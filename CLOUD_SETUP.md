# ChemLink cloud persistence

The Android app now contains a dependency-free Firebase Realtime Database sync layer. It keeps the local cache for offline use and mirrors customer profiles and ads to the cloud when `CHEMLINK_FIREBASE_DB_URL` is configured.

## One-time setup

1. Create a Firebase project.
2. Create a Realtime Database in that project.
3. Copy the database URL (for example `https://YOUR_PROJECT-default-rtdb.firebaseio.com`).
4. In GitHub: **Settings → Secrets and variables → Actions → New repository secret**.
5. Name the secret exactly `CHEMLINK_FIREBASE_DB_URL` and paste the database URL.
6. Run **Actions → Build ChemTrade APK → Run workflow**.

## Important security note

The current Android cloud layer is deliberately dependency-free and is suitable for the private prototype phase. Do **not** expose customer data through public read/write Firebase rules for production. Before opening the app to customers, the next production step is Firebase Phone Authentication + Realtime Database Security Rules, with admin authorization enforced server-side/custom claims. Firebase's security rules are enforced on the server and should be used for authorization.

The 30-day ad lifecycle is enforced by the app: approved ads older than 30 days disappear from the public market and remain available to management as expired/archive records.
