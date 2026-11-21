# 80endance

[![Android](https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-2C3E7B)](https://developer.android.com/compose)

**80endance** is an offline-first Android attendance tracker built for **Royal College, Colombo** GCE A/L students (Grade 12 & Grade 13) to track daily attendance against the **80% GCE A/L exam eligibility** requirement.

---

## Features

- **80% Eligibility Dashboard**
  - Animated eligibility gauge (**Current Term / Overall**), safe-skip & recovery-day math, spotlight A/L countdown, personal attendance streak, and live student count pulse.
  - Pinned updates slideshow always on top, followed by targeted announcements and feedback resolution alerts.
- **Daily Check-In, Baseline Import & Period Locking**
  - Daily reminder with quick **Yes / No** actions and in-app check-in sheet (Present with subject tags, Absent, Medical, School Event, or No School).
  - **Import Attendance Calculator** (percentage or days attended up to a date) and **Custom Time-Period Locking** (log days missed, days attended, or % for any date range and lock it on the calendar to avoid duplicate counts).
- **Sri Lankan Holiday & School Calendar**
  - Pre-loaded with Sri Lankan Full Moon Poya and public holidays, separate from school events, exam-break no-school days, and audience-targeted class/stream notes.
- **Community & Personal Trends & Reports**
  - Slidable **Everyone** (default) and **My Stats** cards with **All Time** and **Monthly** views, monthly bar charts, Mon–Fri weekday patterns, term breakdowns, and one-tap summary sharing.
- **Dynamic Resizable Home Screen Widget**
  - Adaptive 2×2 to 4×4 widget showing live eligibility progress, **This Week's Mon–Fri attendance strip**, A/L countdown, next holiday, 1-tap **Present / Absent** buttons, and manual refresh.
- **Background Notifications & In-App Updates**
  - Background notification delivery for targeted admin broadcasts, automatic GitHub Releases update checking, and What's New release popups.
- **Admin Panel**
  - Live online counter, student directory with last-seen timestamps and installed app versions, multi-audience broadcast composer, bug/suggestion inbox, calendar event editor, and editable stream/class/subject catalogs.
- **Offline-First Sync & Guided Setup**
  - Google Sign-In, stream-aware class and subject pickers, automatic A/L exam year mapping, and offline Firestore caching with automatic background sync.

---

## Download & Install

Pre-built APKs are published in the **[Releases](../../releases)** tab:

1. Open the latest release on your Android phone (**Android 7.0 / API 24+**).
2. Download the `.apk` file under **Assets** and install it.

---

## Tech Stack

- **Language & UI:** Kotlin, Jetpack Compose, Material Design 3
- **Architecture:** MVVM (`ViewModel`, `StateFlow`, Kotlin Coroutines)
- **Backend & Sync:** Firebase Auth (Google Sign-In via Credential Manager) & Cloud Firestore with persistent offline cache
- **System & Widgets:** Jetpack DataStore, `AppWidgetProvider`, `AlarmManager`, `JobScheduler`

---

## Building & Automated Releases

**Prerequisites:** JDK 21, Android SDK (API 36.1), Gradle 9.3.1, and `app/google-services.json` for `com.aistudio.eightyendance.rcxkmv`.

```bash
# Debug APK
gradle :app:assembleDebug

# Release APK
KEYSTORE_PATH=/path/to/my-upload-key.jks STORE_PASSWORD=... KEY_PASSWORD=... \
gradle :app:assembleRelease
```

### GitHub Actions Workflows
- **Official Releases (`.github/workflows/release-apk.yml`)** — Runs on version tags (e.g., `v1.6`) or manual dispatch from the **Actions** tab and publishes `80endance-v<version>.apk` with release notes to **Releases**.
- **Development Builds (`.github/workflows/dev-build.yml`)** — Runs on every push to `main` / `master` / `dev` and uploads `80endance-dev` APKs as workflow artifacts (`com.aistudio.eightyendance.rcxkmv.dev` for side-by-side installs).

> Configure `RELEASE_KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` (plus optional `GOOGLE_SERVICES_JSON_BASE64`) in repository **Actions Secrets** for release-signed builds. Register your release SHA-1/SHA-256 in Firebase Console for Google Sign-In.

---

*Developed by Ataya.*
