# Privacy Policy

Last updated: {{UPDATED}}

This policy explains what information the Rave app ("Rave", "we", "us") collects, why, where it is kept and the choices you have. Rave is made by {{DEVELOPER}}. If you have any question, write to {{EMAIL}}.

The short version: most of your data stays on your phone. If you never sign in, nothing about you is stored on our servers. If you sign in to use friends and competitions, only what those features need is synced. We do not sell your data, we do not show ads and we do not use analytics or tracking SDKs.

## 1. Data that stays on your device

These are stored only in the app's private storage on your phone:

- Habits, habit check-ins, streaks, badges and reminders.
- Walks, runs and rides, including the full GPS route, pace, elevation and calories.
- Daily step counts.
- Workouts, sets and weights.
- Water intake, food log, scanned products and meal plans.
- Body details you choose to enter: weight, height, birth year and sex. These are used only to work out calorie and step targets on the device.
- Food preferences: goal, halal, vegetarian, allergies, cuisines and budget.
- Privacy zones and app settings.

Android may include this data in your own Google account backup (Android Auto Backup) or copy it to a new phone when you transfer devices. That backup is controlled by your Google account settings, not by us.

## 2. Data stored in the cloud when you sign in

Signing in is optional. When you sign in with Google or with an email and password, we use Google Firebase (Authentication and Cloud Firestore) to store:

- **Account:** your user ID, email address, display name, username, profile photo link and invite code.
- **Profile summary:** total and weekly distance, current and best streak, weekly points. Signed-in Rave users can look up profiles to find friends. When your visibility is set to Private you are hidden from search and your activities are not shared.
- **Activity summaries:** type, name, start and end time, distance and duration. The detailed GPS points are not uploaded.
- **Friends' feed:** when your visibility is Friends, each finished activity is shared with your friends, including a simplified line of the route. Parts of the route inside your privacy zones are removed first, and the start and end can be hidden as well (Settings → Privacy).
- **Habit backup:** a copy of your habits and check-ins, so they can be restored when you sign in on a new phone, plus a list of the habits you completed on each of the last 7 days. Only you can read these. The habit name of a shared streak or habit competition is visible to its members.
- **Social features:** friends list, friend requests, nudges, people you blocked, invites, shared streaks, competitions you create or join (name, rules, the optional stake text, your score) and your weekly step total for the friends' step league.
- **Notification token:** a Firebase Cloud Messaging token so we can send you friend and competition notifications.

## 3. Features that use other services

- **Barcode lookup:** when you scan a product, its barcode number is sent to Open Food Facts (world.openfoodfacts.org) to get the product details. Nothing else about you is sent.
- **Nutrition label reading:** label photos are read on your phone with Google ML Kit. Only if that fails, the photo of the label is sent to Google's Gemini API (through Firebase AI Logic) to read the numbers. The photo is not stored by us.
- **Maps:** to draw maps, the app downloads map images for the area on screen from a map provider (Mapbox, MapTiler, TomTom or OpenStreetMap). The provider sees which map area is requested and your IP address, like any website. Your route itself is drawn by the app and is not sent to the map provider.
- **Purchases:** Rave Pro is bought through Google Play. Google handles the payment; we only receive whether your subscription is active. We never see your card details.
- **Invites:** when you install Rave from a friend's invite link, the app reads the Google Play install referrer once, only to find the invite code.
- **Sharing:** when you share a card to Instagram, WhatsApp or another app, the image is passed to the app you choose.

## 4. Permissions we ask for

- **Location (while using the app):** to record walks, runs and rides that you start. Location is only used while a walk is being recorded, shown by a notification.
- **Physical activity:** to count steps and, if you turn on "Auto-detect walks", to notice when you start and stop walking. Detected walks are measured with the step counter; Rave never uses your location in the background.
- **Camera:** to scan barcodes, nutrition labels and friend invite QR codes.
- **Notifications and alarms:** for habit reminders and friend activity.

You can withdraw any permission in your phone's settings; the related feature will stop working.

## 5. How we use your information

We use your information only to run the features you use: tracking and showing your progress, syncing your data, friends, feed, competitions and leagues, sending the notifications you turned on, providing Rave Pro and keeping accounts secure. We do not use your data for advertising, we do not sell or rent it, and we do not build profiles of you for third parties.

Where the law requires a legal basis (for example the GDPR), we rely on performing our contract with you (providing the app), your consent (optional permissions such as physical activity and camera, which you can withdraw), and our legitimate interest in keeping the service and its users safe.

## 6. Who we share it with

- **Your friends and competition members:** only what is described in section 2.
- **Service providers** who process data for us: Google (Firebase Authentication, Cloud Firestore, Cloud Messaging, Gemini API, ML Kit, Google Play Billing), Open Food Facts, and the map provider in use. Their own privacy policies apply to the data they receive.
- **When required by law**, or to protect the rights and safety of our users.

**Reports and blocking:** you can block or report any user from their profile. A report contains your user ID, theirs, the reason and any note you add. It is kept for safety review and to enforce our Terms, even after an account is deleted.

Cloud data is stored by Google Firebase and may be processed in the United States or other countries where Google operates. Google provides safeguards such as standard contractual clauses for these transfers.

## 7. How long we keep it

Data on your device is kept until you delete it or uninstall the app. Cloud data is kept while your account exists. When you delete your account, your cloud data is deleted straight away. Copies held in Google's backup systems are removed by Google within their standard backup cycle.

## 8. Your choices and rights

- **See and export:** Settings → Data → Export gives you a file with your habits, activities (with GPX routes), workouts and more.
- **Delete everything:** Settings → Data → Delete account removes your account and all your data from our servers and your phone. If you are not signed in, "Delete all data" clears the app.
- **Privacy controls:** set your visibility to Private, add privacy zones, hide route start and end, and turn notifications on or off.
- **Request by email:** you can also ask us to access, correct or delete your data by writing to {{EMAIL}}. We will reply within 30 days.

Depending on where you live (for example the EU, UK or California), you may have additional rights, such as to object to or restrict processing, data portability, and to complain to your local data protection authority. We do not sell or share personal information for cross-context behavioural advertising.

## 9. Security

Data in transit is encrypted with HTTPS. Cloud data is protected by Firebase security rules so that private data can only be read by you. No method is perfectly secure, but we work to protect your information.

## 10. Children

Rave is not directed at children under 13 (under 16 in the EU/UK) and they should not create an account. If you believe a child has given us personal information, contact us and we will delete it.

## 11. Health information

Rave is a fitness and habit app, not a medical device. Body details, food logs and workouts stay on your device as described above, and are never used for advertising or shared with insurers or data brokers.

## 12. Changes to this policy

If we change this policy, we will update the date at the top and, for important changes, tell you in the app.

## 13. Contact

{{DEVELOPER}}
Email: {{EMAIL}}
