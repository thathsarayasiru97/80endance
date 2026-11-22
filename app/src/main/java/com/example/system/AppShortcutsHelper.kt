package com.example.system

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppConstants

/**
 * Ensures Android launcher long-press shortcuts work reliably on both standard and dev builds:
 * - On the standard package (`com.aistudio.eightyendance.rcxkmv`), static `res/xml/shortcuts.xml`
 *   provides all 4 instant actions immediately upon install, so dynamic duplicates are cleared.
 * - On alternative/dev package IDs, publishes the exact same 4 dynamic shortcuts bound to the
 *   runtime [Context.getPackageName].
 */
object AppShortcutsHelper {

    private const val STATIC_SHORTCUTS_PACKAGE = "com.aistudio.eightyendance.rcxkmv"

    fun publishDynamicShortcuts(context: Context) {
        val appCtx = context.applicationContext
        runCatching {
            if (appCtx.packageName == STATIC_SHORTCUTS_PACKAGE) {
                ShortcutManagerCompat.removeAllDynamicShortcuts(appCtx)
                return@runCatching
            }
            val shortcuts = listOf(
                buildShortcut(
                    context = appCtx,
                    id = "dyn_mark_present",
                    shortLabel = appCtx.getString(R.string.shortcut_mark_present_short),
                    longLabel = appCtx.getString(R.string.shortcut_mark_present_long),
                    iconRes = R.drawable.ic_shortcut_present,
                    actionValue = AppConstants.SHORTCUT_MARK_PRESENT,
                    rank = 0
                ),
                buildShortcut(
                    context = appCtx,
                    id = "dyn_mark_absent",
                    shortLabel = appCtx.getString(R.string.shortcut_mark_absent_short),
                    longLabel = appCtx.getString(R.string.shortcut_mark_absent_long),
                    iconRes = R.drawable.ic_shortcut_absent,
                    actionValue = AppConstants.SHORTCUT_MARK_ABSENT,
                    rank = 1
                ),
                buildShortcut(
                    context = appCtx,
                    id = "dyn_open_calendar",
                    shortLabel = appCtx.getString(R.string.shortcut_open_calendar_short),
                    longLabel = appCtx.getString(R.string.shortcut_open_calendar_long),
                    iconRes = R.drawable.ic_shortcut_calendar,
                    actionValue = AppConstants.SHORTCUT_OPEN_CALENDAR,
                    rank = 2
                ),
                buildShortcut(
                    context = appCtx,
                    id = "dyn_open_reports",
                    shortLabel = appCtx.getString(R.string.shortcut_open_reports_short),
                    longLabel = appCtx.getString(R.string.shortcut_open_reports_long),
                    iconRes = R.drawable.ic_shortcut_trends,
                    actionValue = AppConstants.SHORTCUT_OPEN_REPORTS,
                    rank = 3
                )
            )
            ShortcutManagerCompat.setDynamicShortcuts(appCtx, shortcuts)
        }
    }

    private fun buildShortcut(
        context: Context,
        id: String,
        shortLabel: String,
        longLabel: String,
        iconRes: Int,
        actionValue: String,
        rank: Int
    ): ShortcutInfoCompat {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AppConstants.EXTRA_SHORTCUT_ACTION, actionValue)
        }
        return ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(shortLabel)
            .setLongLabel(longLabel)
            .setIcon(IconCompat.createWithResource(context, iconRes))
            .setIntent(intent)
            .setRank(rank)
            .build()
    }
}
