# Rave — Habit + Walk/Run Tracker (Android)

A native Android app that combines a habit tracker with a Strava-style GPS walk/run tracker,
plus friends, shared streaks, competitions and invites.

> The app name is defined **only** in `app/src/main/res/values/strings.xml` (`app_name`).
> Change it there.

## Tech stack

Kotlin · Jetpack Compose + Material 3 · MVVM (ui / domain / data) · single activity +
type-safe Compose Navigation · Hilt · Room (offline-first) · Coroutines + Flow ·
Preferences DataStore. Later phases add Google Maps Compose, Fused Location (foreground
service), Activity Recognition, WorkManager, Firebase (Auth, Firestore, FCM), Glance widget,
CameraX + ML Kit.

- Min SDK 26, target/compile SDK 36, JDK 17+.
- Font: [Outfit](https://github.com/Outfitio/Outfit-Fonts), bundled in `app/src/main/res/font`
  (SIL Open Font License, see `licenses/OUTFIT_OFL.txt`).

## Project layout

```
app/src/main/java/com/rave/app/
├── RaveApplication.kt / MainActivity.kt / MainViewModel.kt
├── di/                 Hilt modules (Room, DataStore)
├── data/
│   ├── local/          Room database, entities, DAOs, converters
│   └── preferences/    DataStore-backed user settings
├── domain/
│   ├── model/          Plain Kotlin models & enums
│   └── util/           Pure logic (unit-tested)
└── ui/
    ├── theme/          Colour tokens, typography, shapes (the only place with colour literals)
    ├── components/     Shared composables (background, pill buttons, empty state…)
    ├── navigation/     Routes, NavHost, floating bottom bar
    └── home/ routes/ friends/ profile/ tracking/   Feature screens
```

**Theming rule:** composables never use colour literals. Use `MaterialTheme.colorScheme.*`
or `RaveTheme.colors.*` (extra tokens: background gradient, habit tiles, chart colours,
nav bar, accent card).

## Setup

1. Install Android Studio (latest stable) with Android SDK 36.
2. Clone the repo and open the root folder in Android Studio.
3. Copy `local.properties.example` to `local.properties` (git-ignored) and set:
   - `sdk.dir` — Android Studio usually writes this for you.
   - `MAPS_API_KEY` — needed from Phase 3 (see below). It can be left empty until then.
4. Run the `app` configuration, or from a terminal:
   ```bash
   ./gradlew :app:assembleDebug      # build
   ./gradlew :app:testDebugUnitTest  # unit tests
   ```

### Google Maps API key (Phase 3+)

1. In the [Google Cloud Console](https://console.cloud.google.com/), create or choose a project
   and enable **Maps SDK for Android**.
2. Create an API key and restrict it to Android apps with package `com.rave.app` and your
   debug/release SHA-1 fingerprints (see below).
3. Put it in `local.properties` as `MAPS_API_KEY=...`. The build injects it into the manifest
   (`${MAPS_API_KEY}`) and `BuildConfig.MAPS_API_KEY`. Never commit the key.

### Firebase (Phase 8+)

1. Create a Firebase project and add an Android app with package `com.rave.app`.
2. Download `google-services.json` into `app/` (it is git-ignored).
3. Enable **Authentication** (Google and Email/Password), **Cloud Firestore** and
   **Cloud Messaging**.
4. Add your SHA-1 (and SHA-256) fingerprints in the Firebase project settings. Google Sign-In
   needs them.

### Getting SHA-1 fingerprints

```bash
./gradlew signingReport
# or for the default debug keystore:
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### App Links for invites (Phase 9+)

Invite links use `https://<your-domain>/invite/{code}`. To verify them, host
`https://<your-domain>/.well-known/assetlinks.json` with your package name and SHA-256
fingerprints. If you don't have a domain, the app falls back to invite codes plus share text.
Full steps will be added in Phase 9.

## Build phases

| # | Phase | Status |
|---|-------|--------|
| 1 | Project setup: Gradle, Hilt, Room, Navigation, theme (light/dark + Outfit), floating bottom nav, placeholder screens | ✅ |
| 2 | Habits: models, Home (ring, calendar strip, routine list), add/edit, all habit types, streaks + freezes, reminders | ⏳ |
| 3 | Tracking: permissions, foreground service, live map + polyline, stats sheet, pause/finish/lock, process-death restore, Summary | ⏳ |
| 4 | Routes & Progress: history + filters, detail, PB detection, insights charts, heatmap | ⏳ |
| 5 | Auto-tick, auto-detect walks, battery saver, privacy zones | ⏳ |
| 6 | Share cards (4 templates incl. transparent sticker), share intents, privacy trimming | ⏳ |
| 7 | Profile & settings: badges, stats, all settings, data export | ⏳ |
| 8 | Firebase: optional auth, guest migration, profile, invite codes | ⏳ |
| 9 | Friends & invites: requests, link/code/QR, contacts match, deep links, feed, cheers, nudges | ⏳ |
| 10 | Shared streaks | ⏳ |
| 11 | Competitions: create, types, modes, live leaderboard, notifications, podium, anti-cheat | ⏳ |
| 12 | Glance widget, polish, empty states, tests, README | ⏳ |
