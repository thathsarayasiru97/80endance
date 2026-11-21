package com.example.data

data class ReleaseNoteEntry(
    val version: String,
    val headline: String,
    val whatsNew: List<String>,
    val bugFixes: List<String>
)

/**
 * Structured in-app release notes shown automatically in the "What's New" modal
 * whenever a user updates 80endance to a newer version.
 * Keep in sync with /RELEASE_NOTES.md used by the GitHub Release workflow.
 */
object ReleaseNotesCatalog {
    val latest: ReleaseNoteEntry = ReleaseNoteEntry(
        version = "1.5",
        headline = "Smarter Notifications, Structured Subjects & Calendar Upgrades",
        whatsNew = listOf(
            "Background Notification Delivery: Receive custom school & class announcements even when the app is closed.",
            "Hero A/L Exam Countdown: Opening the app highlights your days left until GCE A/L before smoothly docking into the header.",
            "Structured Subject Buckets & Class Lists: Pick subjects by Stream & Bucket and choose classes matching your Grade & Stream — with custom entry fallback.",
            "Smart Grade-to-Exam-Year Matching: Selecting Grade 12 auto-selects 28 A/L and Grade 13 auto-selects 26 A/L (still editable anytime).",
            "Targeted Multi-Event School Calendar: View multiple events and teaching advisories on any calendar day tailored to your class or stream.",
            "In-App Feedback Resolution Alerts: Get notified right inside the app whenever a bug report or idea you submitted is addressed."
        ),
        bugFixes = listOf(
            "Fixed custom announcements re-triggering device notifications every time the app was reopened.",
            "Fixed the Settings 'Test Daily Check-In Popup' button so it works on weekends too.",
            "Fixed APK version syncing so installed updates always display the true version in About & Update Checker.",
            "Polished motion transitions, micro-animations, and compact pulsing community counter."
        )
    )

    fun forVersion(installedVersion: String): ReleaseNoteEntry {
        val clean = installedVersion.trim().removePrefix("v").removePrefix("V")
        return if (clean.isBlank()) latest else latest.copy(version = clean)
    }
}
