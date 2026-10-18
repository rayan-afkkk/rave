# Play Console checklist (legal & privacy)

Fill these in before publishing. Answers below match what the app actually does.

## Before release
- [ ] Replace `legal_contact_email` and `legal_developer_name` in `app/src/main/res/values/strings.xml` (and the same text in `docs/legal/*.html`).
- [ ] Host `docs/legal/privacy-policy.html`, `terms.html` and `delete-account.html` on a public URL (the repo is private, so e.g. a free public GitHub Pages repo, Google Sites or your website).
- [ ] Deploy the updated `firestore.rules` (`firebase deploy --only firestore:rules`), otherwise account deletion can't remove friend requests, nudges, league entries and shared streaks.

## App content → Privacy policy
Paste the hosted privacy-policy URL.

## App content → Data safety
- Data encrypted in transit: **Yes**. Users can request deletion: **Yes** (in-app + delete-account URL).
- **Location → Precise location:** collected, shared: No. Purpose: App functionality. Optional. (Routes stay on the device; a simplified, privacy-zone-trimmed route is shown to friends you choose.)
- **Personal info → Name, Email address, User IDs:** collected (only when signed in), not shared. Purpose: Account management, App functionality.
- **Personal info → Phone number:** collected (hashed, optional), not shared. Purpose: App functionality (friend finding).
- **Health and fitness → Fitness info:** collected (activity summaries, steps total, habit backup, when signed in), not shared. Purpose: App functionality.
- **Photos:** nutrition label photos may be sent to Google Gemini when on-device reading fails, processed ephemerally, not stored. Mark "Photos – collected, processed ephemerally, App functionality".
- **App activity → Other user-generated content:** competition names/stakes, habit names in shared streaks. App functionality.
- **Device or other IDs:** Firebase Cloud Messaging token. App functionality.
- **Contacts:** NOT collected (only one-way hashes are compared; raw contacts never leave the device). Note this in the permission declaration.
- **Financial info → Purchase history:** handled by Google Play; the app does not collect it.
- No data is used for advertising or analytics. No data is sold.

## App content → other declarations
- **Ads:** No ads.
- **Target audience:** 13+ (not designed for children).
- **Health apps:** declare as a fitness / activity tracking app; not a medical device.
- **Location permissions:** background location is only used by "Auto-detect walks". Upload a short video showing the in-app disclosure dialog → enabling the setting → the system prompt.
- **Foreground service (location):** type `location`, used for recording workouts the user starts.
- **Exact alarms:** habit reminders the user sets (alarm-clock style).
- **Activity recognition, camera, contacts:** explained in the app before each request.
- **Account deletion:** Yes, in-app (Settings → Data → Delete account) + the hosted delete-account URL.
- **Financial features / subscriptions:** the paywall shows price, renewal terms, and links to Terms and Privacy Policy.
