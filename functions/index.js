/**
 * Rave Cloud Functions (Firebase Functions v2, Node 20).
 *
 *  - Push notifications (FCM) for nudges, friend requests, competition invites,
 *    overtakes, "24 h left" and "competition ended".
 *  - validateParticipant: re-computes distance-based competition values from the
 *    participant's synced activity summaries (users/{uid}/activities) inside the
 *    competition window, applying the same anti-cheat rules as the app
 *    (GPS-tracked only, runs <= 25 km/h, walks <= 12 km/h). The client value is
 *    overwritten if it doesn't match.
 *
 * TODO: habit-based competition types (ACTIVE_DAYS, HABITS_COMPLETED, LONGEST_STREAK)
 * are currently trusted from the client (users/{uid}/days is uploaded for a future
 * server-side check).
 */
const { onDocumentCreated, onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

const MAX_RUN_KMH = 25;
const MAX_WALK_KMH = 12;
const DAY_MS = 24 * 60 * 60 * 1000;

/** Sends a data message to every registered device of a user; prunes dead tokens. */
async function pushToUser(uid, data) {
  const tokensSnap = await db.collection("users").doc(uid).collection("tokens").get();
  const tokens = tokensSnap.docs.map((d) => d.id);
  if (tokens.length === 0) return;
  const res = await admin.messaging().sendEachForMulticast({
    tokens,
    data: Object.fromEntries(Object.entries(data).map(([k, v]) => [k, String(v ?? "")])),
    android: { priority: "high" },
  });
  const dead = [];
  res.responses.forEach((r, i) => {
    const code = r.error && r.error.code;
    if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") {
      dead.push(tokens[i]);
    }
  });
  await Promise.all(dead.map((t) => db.collection("users").doc(uid).collection("tokens").doc(t).delete()));
}

// ---- Social pushes ----

exports.onNudge = onDocumentCreated("nudges/{id}", async (event) => {
  const n = event.data && event.data.data();
  if (!n) return;
  await pushToUser(n.to, {
    type: n.reason === "shared_streak" ? "shared_streak" : "nudge",
    title: `${n.fromName || "A friend"} nudged you 👋`,
    body: n.reason === "shared_streak" ? "Your shared streak is waiting on you." : "Time to move!",
    targetId: n.targetId || "",
  });
});

exports.onFriendRequest = onDocumentCreated("friendRequests/{id}", async (event) => {
  const r = event.data && event.data.data();
  if (!r || r.status !== "PENDING") return;
  await pushToUser(r.to, {
    type: "friend_request",
    title: "New friend request",
    body: `${r.fromName || "Someone"} wants to be friends`,
    targetId: event.params.id,
  });
});

exports.onCompetitionCreated = onDocumentCreated("competitions/{cid}", async (event) => {
  const c = event.data && event.data.data();
  if (!c) return;
  await Promise.all((c.invited || []).map((uid) => pushToUser(uid, {
    type: "competition",
    title: "Competition invite",
    body: `${c.createdByName || "A friend"} invited you to ${c.name}`,
    targetId: event.params.cid,
  })));
});

// ---- Competition validation + overtakes ----

function speedOk(a) {
  if (!a.movingTimeS || a.movingTimeS <= 0) return true;
  const kmh = (a.distanceM / 1000) / (a.movingTimeS / 3600);
  return kmh <= (a.type === "RUN" ? MAX_RUN_KMH : MAX_WALK_KMH);
}

async function computeDistanceValue(uid, c) {
  const snap = await db.collection("users").doc(uid).collection("activities")
    .where("startTime", ">=", c.startAt)
    .where("startTime", "<", c.endAt)
    .get();
  let km = 0;
  let reachedAt = null;
  const acts = snap.docs.map((d) => d.data())
    .filter((a) => a.gpsTracked !== false && speedOk(a))
    .filter((a) => !c.activityFilter || a.type === c.activityFilter)
    .sort((a, b) => a.startTime - b.startTime);
  for (const a of acts) {
    km += (a.distanceM || 0) / 1000;
    if (c.type === "TARGET_RACE" && reachedAt === null && km >= (c.targetKm || 0)) {
      reachedAt = a.startTime + (a.movingTimeS || 0) * 1000;
    }
  }
  return { km, reachedAt };
}

function rank(rows, type) {
  const sorted = [...rows].sort((a, b) => {
    if (type === "TARGET_RACE") {
      const ra = a.reachedTargetAt ?? Infinity;
      const rb = b.reachedTargetAt ?? Infinity;
      if (ra !== rb) return ra - rb;
    }
    return (b.value || 0) - (a.value || 0);
  });
  const ranks = {};
  sorted.forEach((r, i) => { ranks[r.uid] = i + 1; });
  return ranks;
}

exports.validateParticipant = onDocumentWritten("competitions/{cid}/participants/{uid}", async (event) => {
  const after = event.data && event.data.after && event.data.after.data();
  if (!after) return;
  const { cid, uid } = event.params;
  const compSnap = await db.collection("competitions").doc(cid).get();
  const c = compSnap.data();
  if (!c) return;

  // 1) Server-side value for distance competitions.
  if (c.type === "DISTANCE" || c.type === "TARGET_RACE") {
    const { km, reachedAt } = await computeDistanceValue(uid, c);
    const rounded = Math.round(km * 1000) / 1000;
    if (Math.abs((after.value || 0) - rounded) > 0.01 || (after.reachedTargetAt ?? null) !== reachedAt) {
      logger.info("Correcting participant value", { cid, uid, client: after.value, server: rounded });
      // This write re-triggers the function; the values then match and it stops.
      await event.data.after.ref.set({ value: rounded, reachedTargetAt: reachedAt, validated: true }, { merge: true });
      return;
    }
  }

  // 2) Overtake notifications: compare ranks before/after this participant's change.
  const before = event.data.before && event.data.before.data();
  if (!before || before.value === after.value) return;
  const partsSnap = await db.collection("competitions").doc(cid).collection("participants").get();
  const rowsAfter = partsSnap.docs.map((d) => ({ uid: d.id, ...d.data() }));
  const rowsBefore = rowsAfter.map((r) => (r.uid === uid ? { ...r, value: before.value, reachedTargetAt: before.reachedTargetAt } : r));
  const rb = rank(rowsBefore, c.type);
  const ra = rank(rowsAfter, c.type);
  const overtaken = rowsAfter.filter((r) => r.uid !== uid && rb[r.uid] < rb[uid] && ra[r.uid] > ra[uid]);
  await Promise.all(overtaken.map((r) => pushToUser(r.uid, {
    type: "competition",
    title: `${after.name || "Someone"} overtook you!`,
    body: `Get moving to take back your spot in ${c.name}.`,
    targetId: cid,
  })));
});

// ---- Hourly: 24 h left + ended ----

exports.competitionTimers = onSchedule("every 60 minutes", async () => {
  const now = Date.now();
  const snap = await db.collection("competitions").where("endAt", ">", now - 2 * DAY_MS).get();
  for (const doc of snap.docs) {
    const c = doc.data();
    const members = c.members || [];
    if (!c.notified24h && c.endAt - now <= DAY_MS && c.endAt > now) {
      await doc.ref.update({ notified24h: true });
      await Promise.all(members.map((uid) => pushToUser(uid, {
        type: "competition", title: `24 hours left in ${c.name}`, body: "Last chance to climb the leaderboard.", targetId: doc.id,
      })));
    }
    if (!c.notifiedEnd && c.endAt <= now) {
      await doc.ref.update({ notifiedEnd: true, status: "ENDED" });
      const parts = await doc.ref.collection("participants").get();
      const ranks = rank(parts.docs.map((d) => ({ uid: d.id, ...d.data() })), c.type);
      await Promise.all(members.map((uid) => pushToUser(uid, {
        type: "competition",
        title: `${c.name} has ended`,
        body: ranks[uid] === 1 ? "You won! 🏆 Tap for the podium." : `You finished #${ranks[uid] || "-"}. Tap for the results.`,
        targetId: doc.id,
      })));
    }
  }
});
