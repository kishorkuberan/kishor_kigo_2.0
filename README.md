# KIGO CHAT V1.0

**Connect Freely. Chat Your Way.**

KIGO CHAT is a real-time Android messaging application built with Kotlin, Jetpack Compose (Material Design 3), and Firebase Firestore / Authentication.

---

## Architecture & Implemented Features

### 1. Authentication & Security Gate
- **Google Sign-In via Credential Manager**: Seamless account selection and session persistence (`GetSignInWithGoogleOption` + `GetGoogleIdOption`).
- **Email Verification Gate**: Unverified accounts are prevented from accessing messaging/group features until verified.
- **Session Persistence, Logout & Account Deletion**: Re-authenticates/clears Credential Manager state, deletes user profile & private subcollection records, and releases `@username` reservations.

### 2. Unique KIGO ID & User Discovery
- **Atomic KIGO-XXXXXX Minting**: Every user receives a unique permanent `KIGO-XXXXXX` identifier generated inside a Firestore transaction (`users/{uid}`, `kigoIds/{kigoId}`, `usernames/{username}`).
- **PII Isolation**: Sensitive account info (email) is stored in `users/{uid}/private/account` and never exposed during KIGO ID or `@username` searches.
- **Deep Link Support**: Shareable `kigochat://connect?id=KIGO-XXXXXX` links open straight to User Search.

### 3. Real-Time Messaging & End-to-End Encryption (E2EE)
- **E2EE Direct Messages**: `KigoCryptoManager` uses **ECDH P-256** (`secp256r1`) key agreement + **HKDF-SHA256** key derivation + **AES-256-GCM** authenticated encryption. Direct text messages display an **E2EE · ECDH+AES-GCM** badge, clearly distinguished from unencrypted standard sync (group metadata, profiles, media previews).
- **Rich Messaging**: Emojis, reply-to, forward, edit, delete (tombstone), emoji reactions, delivery & read receipts, live typing indicators, unread counters, conversation archiving, and paginated message history.
- **Media, Documents, Voice & Location**:
  - Zero-permission **Android Photo Picker** (`PickVisualMedia`) + CameraX/FileProvider capture + PDF/TXT/DOC attachment picker (`OpenDocument`), compressed/validated up to 420 KB for inline Firestore sync and downloadable to `Downloads/KigoChat`.
  - **Voice Studio**: `RECORD_AUDIO` permission request, live recording timer, pause/resume playback, discard, and send to any chat (`MediaRecorder` + `MediaPlayer`).
  - **Location Sharing**: Explicit consent modal prior to requesting `ACCESS_COARSE_LOCATION` via Fused Location Provider.

### 4. Social Graph & Groups
- Search users by exact `KIGO-XXXXXX` ID or `@username`.
- Friend requests (send, accept, reject, cancel with duplicate prevention via deterministic document IDs).
- Remove friends, block/unblock users, and submit abuse/support reports.
- Group creation, member management, admin promotion/demotion, leave group, and private/approval controls.

### 5. Customisation Studio (Live Preview + Firestore Sync)
- **Themes**: Light, Dark, and true AMOLED Black.
- **Appearance**: 5 Accent presets, custom chat bubble colors, 4 Font styles (`Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`, `Serif`), 4 font sizes, Modern/Classic/Compact message bubble layouts, Docked/Floating Pill navigation, Profile themes, Chat wallpapers, and Notification sound choices.
- Automatically synced to `userSettings/{uid}` in Firestore and restored on login.

---

## Firebase Provisioning & Schema

- **Firebase Project**: `gen-lang-client-0870993560`
- **Firestore Database ID**: Configured in `app/src/main/res/values/firebase_applet_config.xml` (`ai-studio-android-kigochat-aa390241-e7c4-4171-8338-1082dd015050`)
- **Schema & Rules**: Defined in `/firebase-blueprint.json` and `/firestore.rules`.

### Optional Firebase Console Upgrades
Google Sign-In and Cloud Firestore are provisioned out of the box. To expand into multi-megabyte media buckets, Email/Password sign-in, and push notifications outside the app:
1. **Enable Email/Password Auth**: Firebase Console → Authentication → Sign-in method → Enable *Email/Password* (and configure password reset templates).
2. **Enable Firebase Storage**: Firebase Console → Storage → Create bucket for >420 KB videos/PDFs and lock rules to `request.auth != null`.
3. **Enable Firebase Cloud Messaging (FCM)**: Add `firebase-messaging` dependency and a `FirebaseMessagingService` to deliver background push notifications when the app is closed.
4. **Cloud Functions (Rate Limiting / Fan-out)**: Deploy Cloud Functions for server-side KIGO ID issuance, push fan-out, and abuse rate-limiting.

---

## Building & Testing

### Local Security Rules & Unit Tests (Firebase Emulator)
Ensure Node.js, Firebase CLI, and Java 17+ are available, then run:
```bash
# 1. Start Firestore & Auth emulators
firebase emulators:start --only firestore,auth

# 2. Run JS security rule assertions (in another terminal)
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js

# 3. Run Android Robolectric + Firestore Emulator integration tests
gradle :app:testDebugUnitTest
```

### Build Debug & Signed Release APKs
```bash
# Debug APK
gradle :app:assembleDebug

# Signed Release APK (set KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD env vars)
gradle :app:assembleRelease
```

---

## Publish via GitHub (Free Web Hosting + Automated APK Build)

This repository includes pre-configured GitHub Actions workflows in `.github/workflows/`:

1. **Push from AI Studio**: Click **Save / Push to GitHub** in AI Studio to push this repository to your GitHub account.
2. **Publish Live Web App (GitHub Pages)**:
   - Open your GitHub repository → **Settings** → **Pages**.
   - Under **Build and deployment → Source**, choose **GitHub Actions** (or **Deploy from a branch** → `main` / `/ (root)`).
   - Once deployed, add `your-username.github.io` in **Firebase Console → Authentication → Settings → Authorized domains** and paste your Firebase Web Config (`apiKey`, `appId`) into the **Firebase Setup** button inside `index.html`.
3. **Download Built Android APK**:
   - Open the **Actions** tab in your GitHub repository → select **Build KIGO CHAT Android APK** → download the `KIGO-CHAT-debug-apk` artifact.

