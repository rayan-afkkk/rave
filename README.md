# Rave — Habit + Walk/Run Tracker (Android)

A native Android app that combines a habit tracker with a Strava-style GPS walk/run tracker,
plus friends, shared streaks, competitions and invites.

> The app name is defined **only** in `app/src/main/res/values/strings.xml` (`app_name`).
> Change it there.

## Features

- **Habits**: checkbox, counter (+1 on the card), timed (built-in timer), and activity-linked
  (auto-completes from a tracked walk or run). Schedules: every day, specific weekdays, or X times
  per week. Reminders, swipe right to complete, swipe left to skip. Yesterday stays editable
  for 24 h.
- **Streaks & freezes**: you earn 1 freeze for every 7-day streak (max 3). A freeze is used
  automatically when you miss a day. Streaks are replayed from the full log history, so they
  stay deterministic.
- **Live GPS tracking**: foreground service (`location` type), live orange route line, pulsing
  position dot, GPS quality chip, pocket lock, auto-pause, voice splits, battery-saver GPS, and
  restore after process death. Bad fixes are filtered (accuracy > 25 m, impossible jumps).
- **Summary & history**: route map, splits, PBs (fastest 1K/5K, longest), habit auto-tick
  banner, favourites, repeat a favourite route with a guide line, filters, weekly summary.
- **Set tracker (strength workouts)**: built-in exercise library plus custom exercises (weight × reps,
  reps only, or timed). Sets prefill from your last session, a rest timer starts when you tick a set,
  warm-up sets are supported, and kg/lb follows your units setting. PRs are tracked (heaviest,
  estimated 1RM by Epley, best set volume, most reps, longest hold), along with history and
  personal records. A "Workout" habit type ticks itself off. Sessions are saved live, so they
  survive the app being killed.
- **Insights**: weekly pill bars, monthly heatmap, km chart (weekly/monthly), points, best streaks.
- **Share cards**: 4 templates (warm, dark, minimal, transparent sticker), Instagram Story,
  WhatsApp, save to gallery, system share. Privacy trimming is applied automatically (first/last
  200 m plus privacy zones).
- **Auto-detect walks** with the Activity Recognition API. Low-power location is used only
  during a detected walk, then you get a "Looks like you walked 1.2 km — log it?" notification.
- **Profile & settings**: level, points, badges, units, body stats, goals, privacy zones,
  per-category notifications, visibility, JSON/GPX export, delete account.
- **Optional cloud** (Firebase): Google or email sign-in, guest data migration and backup,
  friends, invite link/code/QR, contact matching (hashed), activity feed with cheers, nudges,
  shared streaks, and competitions (5 types; duel, group or team modes; live leaderboard; podium
  plus a shareable result card; anti-cheat).
- **Animated onboarding**: five motion-graphics story pages that replay as you reach them (habit
  checks ticking, a route drawing itself, a podium rising with confetti, a barbell lifting), with
  parallax swiping and a stretching page indicator. Then a setup page that explains permissions,
  then Google sign-in (email and guest stay available).
- **Home screen widget** (Glance): small (streak + ring) and medium (tap-to-tick + Start walk).

## Tech stack

Kotlin · Jetpack Compose + Material 3 · MVVM (ui / domain / data) · single activity +
type-safe Compose Navigation · Hilt · Room (offline-first) · Coroutines + Flow · DataStore ·
Google Maps Compose · Fused Location · Activity Recognition · WorkManager · Firebase Auth /
Firestore / FCM · Credential Manager (Google Sign-In) · Glance · CameraX + ML Kit · ZXing.

Min SDK 26, compile/target SDK 36, JDK 17+. The font is
[Outfit](https://github.com/Outfitio/Outfit-Fonts), bundled under the SIL OFL (`licenses/OUTFIT_OFL.txt`).

## Project layout

```
app/src/main/java/com/rave/app/
├── RaveApplication.kt / MainActivity.kt / MainViewModel.kt
├── core/            TimeProvider, app scope, deep links (App Links, install referrer)
├── di/              Hilt modules
├── domain/          Pure Kotlin, unit tested
│   ├── model/       Habit, Activity, settings models
│   ├── tracking/    Haversine, GPS filter, splits/PBs, calories, polyline, privacy trimming
│   ├── habit/       Streak + freeze engine, auto-tick, PB detection, points, badges
│   ├── competition/ Ranking, scoring, anti-cheat
│   ├── workout/     Set tracker maths: e1RM, volume, PR detection
│   └── social/      Invite codes, phone hashing
├── data/
│   ├── local/       Room entities, DAOs, mappers
│   ├── preferences/ DataStore settings
│   ├── repository/  Habits, streaks, activities, stats/badges, privacy zones, processing
│   ├── cloud/       Firebase: auth, profiles, friends, invites, feed, shared streaks, competitions
│   └── export/      JSON + GPX export
├── tracking/        TrackingManager (state machine), TrackingService (FGS), voice, auto-detect
├── work/            Reminders, daily streak job, cloud sync
├── notifications/   Channels, FCM service, notification actions
├── widget/          Glance widget
└── ui/              Theme, components and one package per feature
firestore.rules      Security rules · firestore.indexes.json  Composite indexes (no server code; 100% Kotlin)
```

**Theming rule:** composables never use colour literals. Use `MaterialTheme.colorScheme.*` or
`RaveTheme.colors.*` (gradient, habit tiles, chart colours, nav bar, accent card). Literals live
only in `ui/theme/Color.kt`.

## Setup

1. Install the latest stable Android Studio with Android SDK 36.
2. Open the project root in Android Studio. Gradle sync downloads all dependencies.
3. Copy `local.properties.example` to `local.properties` (it's git-ignored) and fill in the values.
4. Run the `app` configuration, or use the terminal:
   ```bash
   ./gradlew :app:assembleDebug       # build
   ./gradlew :app:testDebugUnitTest   # unit tests
   ```

Without any keys the app still runs. Habits, tracking (the route is drawn without map tiles),
history, insights, share cards and the widget all work offline as a guest.

### 1. Google Maps API key

1. In [Google Cloud Console](https://console.cloud.google.com/), enable **Maps SDK for Android**.
2. Create an API key and restrict it to Android apps: package `com.rave.app` plus your SHA-1
   fingerprints (see below).
3. Add `MAPS_API_KEY=...` to `local.properties`. The build injects it into the manifest and
   `BuildConfig`. Never commit it.

### 2. Firebase (friends, competitions, invites, backup)

1. Create a Firebase project and add an Android app with package `com.rave.app`.
2. Download **`google-services.json`** into `app/` (it's git-ignored). The Google Services plugin
   is applied automatically when the file exists.
3. Enable **Authentication** → Google and Email/Password.
4. Create a **Cloud Firestore** database, then deploy the rules and indexes:
   ```bash
   npm i -g firebase-tools && firebase login && firebase use <project-id>
   firebase deploy --only firestore:rules,firestore:indexes
   ```
5. No server code is needed: the project is 100% Kotlin. The app's hourly sync raises its own
   notifications for nudges, friend requests, invitations, overtakes, "24 h left" and "competition
   ended". If you later add your own push sender, `RaveMessagingService` already handles FCM
   data messages (`type`, `title`, `body`, `targetId`).

### 3. SHA-1 for Google Sign-In

Google Sign-In needs your signing certificate fingerprints registered in Firebase
(Project settings → Your apps → Add fingerprint):

```bash
./gradlew signingReport
# or, for the default debug keystore:
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Add both **SHA-1** and **SHA-256** for debug and release, then download `google-services.json`
again. The app uses the Web client ID (`default_web_client_id`) that the plugin generates.

### 4. App Links for invites

Invite links look like `https://<INVITE_DOMAIN>/invite/{code}`, and competitions like
`https://<INVITE_DOMAIN>/competition/{id}`.

1. Set `INVITE_DOMAIN=your.domain` in `local.properties`.
2. Host `https://your.domain/.well-known/assetlinks.json`:
   ```json
   [{
     "relation": ["delegate_permission/common.handle_all_urls"],
     "target": { "namespace": "android_app", "package_name": "com.rave.app",
                 "sha256_cert_fingerprints": ["AA:BB:…"] }
   }]
   ```
3. On the web page for `/invite/{code}`, send people who don't have the app to the Play Store with
   `&referrer=invite_code%3D{code}`. On first launch the app reads the install referrer and opens
   that invite right after sign-up.

**No domain?** Invites still work through the short code (`RAYAN-7K2Q`), the QR code, the pre-filled
share text, and the `rave://invite/{code}` fallback scheme.

### 5. Instagram Story sharing (optional)

Meta requires a Facebook App ID for the Story intent. Add `FACEBOOK_APP_ID=...` to `local.properties`.

## Privacy

- Raw GPS traces never leave the device. The cloud only gets activity summaries and simplified,
  privacy-trimmed polylines (start and end trimmed by 200 m, privacy zones removed).
- Contact matching is opt-in. Numbers are normalised, salted and SHA-256 hashed on the device, and
  only hashes are compared.
- Background location is requested only if you enable auto-detect walks, and only after an
  explanation.

## Tests

`./gradlew :app:testDebugUnitTest` runs JVM unit tests for distance and pace, GPS filtering,
splits and fastest-window PBs, calories, polyline encoding and privacy trimming, the streak and
freeze engine, auto-tick, PB detection, points and badges, competition ranking, scoring and
anti-cheat, workout maths (e1RM, volume, PRs), and invite codes and phone hashing.

## Build phases

| # | Phase | Status |
|---|-------|--------|
| 1 | Project setup, theme (light/dark + Outfit), floating bottom nav | ✅ |
| 2 | Habits: Home ring + week strip + routine list, add/edit, all types, streaks + freezes, reminders | ✅ |
| 3 | Tracking: permissions, foreground service, live map, stats, pause/finish/lock, restore, summary | ✅ |
| 4 | Routes & Progress: history + filters + favourites, detail, PBs, insights, heatmap | ✅ |
| 5 | Auto-tick, auto-detect walks, battery saver, privacy zones | ✅ |
| 6 | Share cards (4 templates incl. transparent sticker), share intents, privacy trimming | ✅ |
| 7 | Profile & settings: badges, stats, all settings, data export | ✅ |
| 8 | Firebase: optional auth, guest migration, profile, invite codes | ✅ |
| 9 | Friends & invites: requests, link/code/QR, contacts, deep links, feed, cheers, nudges | ✅ |
| 10 | Shared streaks | ✅ |
| 11 | Competitions: create, types, modes, invites, live leaderboard, notifications, podium, anti-cheat | ✅ |
| 12 | Glance widget, animations, empty states, tests, README | ✅ |
| + | Set tracker: strength workouts, rest timer, PRs, history, Workout habit type | ✅ |
