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
 * Registers dynamic Android launcher long-press shortcuts using the runtime [Context.getPackageName],
 * guaranteeing shortcuts work on both release and dev builds alongside `res/xml/shortcuts.xml`.
 */
object AppShortcutsHelper {

    fun publishDynamicShortcuts(context: Context) {
        val appCtx = context.applicationContext
        runCatching {
            val shortcuts = listOf(
                buildShortcut(
                    context = appCtx,
                    id = "dyn_mark_present",
                    shortLabel = appCtx.getString(R.string.shortcut_mark_present_short),
                    longLabel = appCtx.getString(R.string.shortcut_mark_present_long),
                    iconRes = android.R.drawable.checkbox_on_background,
                    actionValue = AppConstants.SHORTCUT_MARK_PRESENT,
                    rank = 0
                ),
                buildShortcut(
                    context = appCtx,
                    id = "dyn_mark_absent",
                    shortLabel = appCtx.getString(R.string.shortcut_mark_absent_short),
                    longLabel = appCtx.getString(R.string.shortcut_mark_absent_long),
                    iconRes = android.R.drawable.ic_delete,
                    actionValue = AppConstants.SHORTCUT_MARK_ABSENT,
                    rank = 1
                ),
                buildShortcut(
                    context = appCtx,
                    id = "dyn_open_calendar",
                    shortLabel = appCtx.getString(R.string.shortcut_open_calendar_short),
                    longLabel = appCtx.getString(R.string.shortcut_open_calendar_long),
                    iconRes = android.R.drawable.ic_menu_my_calendar,
                    actionValue = AppConstants.SHORTCUT_OPEN_CALENDAR,
                    rank = 2
                ),
                buildShortcut(
                    context = appCtx,
                    id = "dyn_open_reports",
                    shortLabel = appCtx.getString(R.string.shortcut_open_reports_short),
                    longLabel = appCtx.getString(R.string.shortcut_open_reports_long),
                    iconRes = android.R.drawable.ic_menu_sort_by_size,
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
