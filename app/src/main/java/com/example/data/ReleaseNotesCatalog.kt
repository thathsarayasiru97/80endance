package com.example.data

import com.example.BuildConfig

data class ReleaseNoteEntry(
    val version: String,
    val headline: String,
    val whatsNew: List<String>,
    val bugFixes: List<String>
)

/**
 * Dynamic release notes resolver for the post-update "What's New" modal.
 *
 * Resolution priority:
 * 1. Live GitHub Release notes matching the installed version tag (if available).
 * 2. Build-time bundled notes from /RELEASE_NOTES.md (or CI customReleaseNotes).
 * 3. Concise built-in fallback.
 */
object ReleaseNotesCatalog {

    private const val DEFAULT_HEADLINE = "Here's what's new in this update."

    private val fallbackEntry = ReleaseNoteEntry(
        version = BuildConfig.VERSION_NAME.ifBlank { "1.7" },
        headline = DEFAULT_HEADLINE,
        whatsNew = listOf(
            "Slidable Thin-Film Liquid-Glass Controls & Morphing Theme Toggle: Slide and scrub a thin-film iridescent liquid-glass bubble with squash-and-stretch physics across the bottom navigation bar, segmented switchers, \"What-If\" droplet slider, 80% gauge lens, and interactive soap-bubble playground, plus a morphing Sun-to-Moon theme toggle.",
            "Smooth Calendar Month Swiping: Swipe left or right directly on the calendar grid and use the quick \"Jump to Today\" pill.",
            "Dynamic Greeting & Context Banner: Home header adapts to time of day, remaining school days this week, Poya breaks, and active streaks.",
            "A/L Batch Countdown Milestones: Celebratory banners when your batch hits 300d, 200d, 100d, 50d, and 30d to A/Ls.",
            "Scheduled Notices: Upcoming announcements and reminders can now be queued to appear automatically on time."
        ),
        bugFixes = listOf(
            "Eliminated attendance double-counting across imported baselines, locked periods, and school holidays.",
            "Swipe catch-up deck now only auto-appears on app launch instead of every time you switch back to Home.",
            "Improved release notes and general stability fixes."
        )
    )

    val latest: ReleaseNoteEntry
        get() = forVersion(BuildConfig.VERSION_NAME)

    fun forVersion(
        installedVersion: String,
        remoteMarkdown: String? = null
    ): ReleaseNoteEntry {
        val cleanVersion = installedVersion.trim()
            .removePrefix("v")
            .removePrefix("V")
            .ifBlank { BuildConfig.VERSION_NAME.ifBlank { "1.7" } }

        remoteMarkdown?.let { md ->
            parseMarkdown(version = cleanVersion, markdown = md)?.let { return it }
        }

        val bundled = BuildConfig.BUNDLED_RELEASE_NOTES
        if (bundled.isNotBlank()) {
            parseMarkdown(version = cleanVersion, markdown = bundled)?.let { return it }
        }

        return fallbackEntry.copy(version = cleanVersion)
    }

    /**
     * Parses concise Markdown release notes (from RELEASE_NOTES.md or GitHub Release body)
     * into structured [ReleaseNoteEntry] sections, stripping release boilerplate such as
     * "How to Install" instructions, app version headers, and GitHub changelog footers.
     */
    fun parseMarkdown(
        version: String,
        markdown: String,
        maxNewItems: Int = 6,
        maxFixItems: Int = 4
    ): ReleaseNoteEntry? {
        val cleanVersion = version.trim().removePrefix("v").removePrefix("V").ifBlank { "1.7" }
        val lines = markdown.lines()

        var section = Section.NONE
        val whatsNew = mutableListOf<String>()
        val bugFixes = mutableListOf<String>()
        var customHeadline: String? = null

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            // Section headings (## / ###)
            if (trimmed.startsWith("#")) {
                val headingText = trimmed.trimStart('#').trim()
                val lower = headingText.lowercase()
                section = when {
                    lower.contains("how to install") ||
                        lower.contains("installation") ||
                        lower.contains("full changelog") ||
                        lower.contains("checksum") -> Section.IGNORE
                    lower.contains("bug") ||
                        lower.contains("fix") ||
                        lower.contains("polish") -> Section.BUG_FIXES
                    lower.contains("what's new") ||
                        lower.contains("whats new") ||
                        lower.contains("new") ||
                        lower.contains("feature") ||
                        lower.contains("highlight") ||
                        lower.contains("change") ||
                        lower.contains("update") -> Section.WHATS_NEW
                    // Top-level release title like "## 80endance v1.7" — skip as metadata
                    lower.startsWith("80endance") -> Section.META
                    else -> Section.WHATS_NEW
                }
                continue
            }

            if (section == Section.IGNORE) continue

            val isBullet = trimmed.startsWith("- ") ||
                trimmed.startsWith("* ") ||
                trimmed.startsWith("• ") ||
                trimmed.matches(Regex("^\\d+\\.\\s+.*"))

            val normalizedText = if (isBullet) {
                cleanMarkdownInline(trimmed.replaceFirst(Regex("^([-*•]|\\d+\\.)\\s+"), ""))
            } else {
                cleanMarkdownInline(trimmed)
            }
            if (normalizedText.isEmpty()) continue

            val lowerNormalized = normalizedText.lowercase()
            // Ignore GitHub Release auto-generated boilerplate lines
            if (lowerNormalized.startsWith("app version:") ||
                lowerNormalized.startsWith("full changelog:") ||
                lowerNormalized.startsWith("new contributors")
            ) {
                continue
            }

            if (isBullet) {
                when (section) {
                    Section.BUG_FIXES -> if (bugFixes.size < maxFixItems) bugFixes.add(normalizedText)
                    Section.META, Section.IGNORE -> Unit
                    Section.NONE, Section.WHATS_NEW -> if (whatsNew.size < maxNewItems) whatsNew.add(normalizedText)
                }
            } else if (section == Section.NONE || section == Section.META) {
                if (customHeadline == null && normalizedText.length in 10..120) {
                    customHeadline = normalizedText
                }
            } else {
                // Plain short paragraph inside a section (e.g., user typed quick plain text in workflow_dispatch)
                when (section) {
                    Section.BUG_FIXES -> if (bugFixes.size < maxFixItems) bugFixes.add(normalizedText)
                    Section.WHATS_NEW -> if (whatsNew.size < maxNewItems) whatsNew.add(normalizedText)
                    Section.NONE, Section.META, Section.IGNORE -> Unit
                }
            }
        }

        if (whatsNew.isEmpty() && bugFixes.isEmpty()) return null

        return ReleaseNoteEntry(
            version = cleanVersion,
            headline = customHeadline ?: DEFAULT_HEADLINE,
            whatsNew = whatsNew,
            bugFixes = bugFixes
        )
    }

    private fun cleanMarkdownInline(text: String): String {
        return text
            .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1") // [label](url) -> label
            .replace("**", "")
            .replace("__", "")
            .replace("`", "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private enum class Section {
        NONE,
        META,
        WHATS_NEW,
        BUG_FIXES,
        IGNORE
    }
}
