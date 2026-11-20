# 80endance

[![Android](https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-2C3E7B)](https://developer.android.com/compose)

**80endance** is an offline-capable Android attendance tracker built for **Royal College, Colombo** GCE A/L students (Grade 12 & Grade 13) to track daily attendance against the **80% GCE A/L exam eligibility** requirement.

---

## Features

- **80% Exam Eligibility Dashboard**
  - Animated circular eligibility gauge with a **Current Term / Overall (G12 & G13)** toggle.
  - Real-time safe-skip and recovery-day calculations so you always know where you stand.
  - A/L exam year countdown badge, personal attendance streak (bridging weekends, Poya days, and school holidays), and a Mon–Fri weekly summary card.
- **Daily Check-In Reminder & Quick Popup**
  - Customizable daily notification reminder with quick **Yes / No** actions.
  - Fast in-app **"Did you attend school today?"** check-in sheet:
    - **Yes:** Logs Present with optional tags for your three A/L subjects and notes.
    - **No:** Choose from **Absent**, **Medical Excused**, **School Event**, or **No School / Holiday** with an optional note.
- **Pinned School & Class Notices**
  - Auto-advancing 5-second notice slideshow on the dashboard for school-wide, stream-specific, and class-specific announcements.
- **Sri Lankan Poya & School Term Calendar**
  - Monthly attendance calendar pre-loaded with Sri Lankan Full Moon Poya days and school term dates.
  - Tap any date to view or update your attendance entry.
- **Term Reports & Export**
  - Term-by-term attendance breakdown, subject attendance counts, and one-tap summary sharing via the Android Share Sheet.
- **Offline-First & Background Sync**
  - Works seamlessly without an internet connection using an on-device Firestore cache and automatically syncs pending entries in the background once connectivity returns.
- **Personalized A/L Setup**
  - Sign in with Google and configure your name, optional nickname, Grade, A/L Stream, Class, A/L Exam Year, three subjects, preferred reminder time, and optional mid-term starting attendance balance.

---

## Download & Install

Pre-built APKs are published automatically in the **[Releases](../../releases)** tab:

1. Open the latest release on your Android phone (**Android 7.0 / API 24+**).
2. Download the `.apk` file under **Assets**.
3. Tap the downloaded file and approve installing from unknown sources if prompted.

---

## Tech Stack

- **Language & UI:** Kotlin, Jetpack Compose, Material Design 3
- **Architecture:** MVVM with `ViewModel`, `StateFlow`, and Kotlin Coroutines
- **Authentication & Sync:** Firebase Authentication (Google Sign-In via Jetpack Credential Manager) & Cloud Firestore with persistent local disk cache
- **Preferences & Scheduling:** Jetpack DataStore, `AlarmManager` (daily check-in reminders), `JobScheduler` (network-aware background sync)

---

## Building Locally

### Prerequisites
- **JDK 21** (compilation targets Java 11 bytecode)
- **Android SDK** with API 36.1 platform tools
- **Gradle 9.3.1** (or Android Studio with Gradle configured)
- A valid `app/google-services.json` configured for package `com.aistudio.eightyendance.rcxkmv`

### Build Commands

```bash
# Build Debug APK
gradle :app:assembleDebug

# Build Release APK (requires signing environment variables)
KEYSTORE_PATH=/path/to/my-upload-key.jks \
STORE_PASSWORD=your_store_password \
KEY_PASSWORD=your_key_password \
gradle :app:assembleRelease
```

---

## Automated Builds & Releases (GitHub Actions)

This repository uses two separate workflows so testing builds stay separate from public releases:

1. **Official Releases — `80endance` (`.github/workflows/release-apk.yml`)**
   - Triggers when you push a version tag (e.g., `v1.0`, `v1.1`) or manually run the workflow from the **Actions** tab.
   - Publishes `80endance-v<version>.apk` directly to the **[Releases](../../releases)** tab.
2. **Development Builds — `80endance-dev` (`.github/workflows/dev-build.yml`)**
   - Triggers automatically on every push to `main` / `master` / `dev`.
   - Uploads `80endance-dev-build.<N>-<sha>.apk` as a **GitHub Actions Artifact** (downloadable under the **Actions** tab) without cluttering **Releases**.
   - Automatically enables side-by-side installation (`com.aistudio.eightyendance.rcxkmv.dev`) alongside the main `80endance` app once `com.aistudio.eightyendance.rcxkmv.dev` is registered in the same Firebase project and included in `app/google-services.json`.

### Release Signing Setup (Optional for Production APKs)

If release signing secrets are not configured yet, builds fall back to the bundled debug keystore so pushes and tags work out of the box.

To publish **release-signed APKs** automatically:

1. Generate an upload keystore (keep the alias as `upload` to match `app/build.gradle.kts`):
   ```bash
   keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
2. Convert the keystore to Base64:
   ```bash
   base64 -w 0 my-upload-key.jks
   ```
3. In your GitHub repository, go to **Settings → Secrets and variables → Actions** and add:
   - `RELEASE_KEYSTORE_BASE64`: Base64 output from Step 2
   - `STORE_PASSWORD`: Your keystore password
   - `KEY_PASSWORD`: Your key password
   - *(Optional)* `GOOGLE_SERVICES_JSON_BASE64`: Base64-encoded `app/google-services.json` if you choose not to commit it directly to the repository
4. Register your release keystore's **SHA-1** and **SHA-256** fingerprints in **Firebase Console → Project Settings → Your Android App** so Google Sign-In works in release builds.
