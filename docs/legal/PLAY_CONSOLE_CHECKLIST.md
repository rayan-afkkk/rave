# Play Console checklist — read before you submit

The app itself is set up to pass review. These are the things only you can do in Play Console,
plus the exact answers to give. Wrong or missing declarations are the #1 reason apps get rejected.

## 1. Before you build the release
- [x] Support email set to rayanshahid98@gmail.com (app and `docs/legal/*.html`).
- [ ] Developer name: still "The Rave team" (`legal_developer_name` and `docs/legal/*.html`).
- [ ] Host `docs/legal/privacy-policy.html`, `terms.html` and `delete-account.html` on a public URL
      (repo is private: use a free public GitHub Pages repo, Google Sites or your website).
- [ ] Deploy Firestore rules: `firebase deploy --only firestore:rules` (blocking, reports and account deletion need them).
- [ ] Build the signed **App Bundle (.aab)**: GitHub → Actions → Release AAB → Run workflow (the version code goes up automatically).
- [ ] Test the release build once on a real phone: sign in, track a walk, scan a barcode, buy Pro with a test card, delete a test account.

## 2. App content (Policy → App content)
| Section | Answer |
|---|---|
| Privacy policy | Your hosted privacy-policy URL |
| App access | **"All or some functionality is restricted"** → give a test login: make a spare Gmail account just for reviewers (no 2-step verification) and paste its email + password with: "Friends, competitions and feed need sign-in. Everything else works as a guest. Tap Sign in → Continue with Google and use this account." |
| Ads | No |
| Content rating | Fill the IARC questionnaire honestly: no violence, users can interact (friends/feed), shares location with friends (route maps), no gambling, no purchases of physical goods. Expect **Everyone / PEGI 3** (or Teen if you answer "users can interact" strictly) |
| Target audience | **13+** only (do NOT tick under-13 groups) |
| News app | No |
| Government app | No |
| Financial features | None |
| Health apps | Yes → **Activity & fitness** (and Nutrition). Not a medical device. |
| Data safety | See section 3 |
| Account deletion | Yes → in app: Settings → Data → Delete account. Web link: your hosted delete-account.html |

## 3. Data safety form
- Is data encrypted in transit? **Yes**. Can users request deletion? **Yes**.
- Data collected (all **not shared**, all for **App functionality** unless noted; none for ads/analytics):
  - **Location → Precise location**: collected, optional (simplified routes shown to friends you pick).
  - **Personal info → Name, Email address, User IDs**: collected (only when signed in). Also *Account management*.
  - **Health and fitness → Fitness info**: collected (activity summaries, weekly steps, habit backup) when signed in.
  - **Photos and videos → Photos**: collected, **processed ephemerally** (label photo sent to Google Gemini only if on-device reading fails).
  - **App activity → Other user-generated content**: competition names/dares, habit names in shared streaks, reports you send.
  - **Device or other IDs**: Firebase Cloud Messaging token.
- NOT collected: contacts, phone number, financial info (Google Play handles payments), messages, audio, web history, installed apps.
- Third parties that process data for you (service providers, not "sharing"): Google Firebase, Google Gemini, Open Food Facts (barcode only), map tile provider.

## 4. Permission declarations
- **Foreground service (location)**: declare type *location*, use case "User-initiated location tracking of a workout". Upload a short video: Home → Start walk → notification shows → Finish.
- **Exact alarms**: not needed (app uses SCHEDULE_EXACT_ALARM, which the user grants; it does not use USE_EXACT_ALARM).
- Background location, contacts, SMS, call log, all-files access: **not used** (removed on purpose).

## 5. Store listing
- Name: **Rave** (no extra keywords like "Best", "#1", "Free" in the title).
- Short description example: "Habits, walks, steps and workouts — with friends."
- No competitor names (Strava, Nike…), no "Google" in the title, no fake reviews or misleading screenshots.
- Screenshots must show the real app (use the ones in docs/screenshots).
- Subscription prices in the listing must match Play Console products.

## 6. Monetization
- Subscriptions `rave_pro_monthly` ($3.99), `rave_pro_yearly` ($27.99, optional 7-day trial), in-app product `rave_pro_lifetime` ($69.99).
- The paywall already shows price, renewal, trial terms, Restore, and links to Terms and Privacy.

## 7. First release tips
- Start with **Internal testing** → **Closed testing**. New personal developer accounts must run a closed test with **12+ testers for 14 days** before production.
- Reply to any policy email calmly and fix exactly what they mention; resubmissions are normal.

## Health Connect (smartwatch sync)
- [ ] Play Console → **App content → Health apps / Health Connect**: declare read access to **Steps, Exercise and Distance**.
      Reason: "Show the user's smartwatch steps and walks/runs inside Rave. Read only, never shared or used for ads."
- [ ] Data safety: add **Health and fitness → Fitness info** as collected (from Health Connect), not shared.
- [ ] The privacy policy already has a Health Connect section; make sure the hosted copy is updated.
