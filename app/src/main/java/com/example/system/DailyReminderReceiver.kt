package com.example.system

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppConstants
import com.example.data.AttendanceCalculator
import com.example.data.AttendanceRecord
import com.example.data.AttendanceRepository
import com.example.data.AttendanceStatus
import com.example.data.DateUtils
import com.example.data.LocalPreferencesStore
import com.example.data.NextDaySchoolVote
import com.example.data.SriLankaCalendarDefaults
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val source = intent.getStringExtra(ReminderScheduler.EXTRA_SOURCE) ?: "notification"
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ReminderScheduler.ACTION_DAILY_REMINDER -> handleDailyReminder(context, rescheduleRegular = true)
                    ReminderScheduler.ACTION_UNLOCK_REMINDER -> handleDailyReminder(context, rescheduleRegular = false)
                    ReminderScheduler.ACTION_SNOOZE_1H -> handleSnoozeOneHour(context)
                    ReminderScheduler.ACTION_QUICK_YES -> handleQuickCheckIn(context, AttendanceStatus.PRESENT, source)
                    ReminderScheduler.ACTION_QUICK_MEDICAL -> handleQuickCheckIn(context, AttendanceStatus.MEDICAL, source)
                    ReminderScheduler.ACTION_QUICK_EXCUSED -> handleQuickCheckIn(context, AttendanceStatus.SCHOOL_EVENT, source)
                    ReminderScheduler.ACTION_QUICK_NO -> handleQuickCheckIn(context, AttendanceStatus.ABSENT, source)
                    ReminderScheduler.ACTION_QUICK_UNDO -> handleQuickUndo(
                        context = context,
                        dateKey = intent.getStringExtra(ReminderScheduler.EXTRA_TARGET_DATE) ?: DateUtils.todayKey(),
                        previousStatusRaw = intent.getStringExtra(ReminderScheduler.EXTRA_PREVIOUS_STATUS),
                        previousNote = intent.getStringExtra(ReminderScheduler.EXTRA_PREVIOUS_NOTE)
                    )
                    ReminderScheduler.ACTION_NEXT_DAY_POLL_ALARM -> handleNextDayPollAlarm(context)
                    ReminderScheduler.ACTION_POLL_VOTE_YES -> handleNextDayPollVote(
                        context = context,
                        going = true,
                        extraTargetDate = intent.getStringExtra(ReminderScheduler.EXTRA_POLL_TARGET_DATE)
                    )
                    ReminderScheduler.ACTION_POLL_VOTE_NO -> handleNextDayPollVote(
                        context = context,
                        going = false,
                        extraTargetDate = intent.getStringExtra(ReminderScheduler.EXTRA_POLL_TARGET_DATE)
                    )
                }
            } catch (e: Exception) {
                Log.w("DailyReminderReceiver", "Reminder action error: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleDailyReminder(context: Context, rescheduleRegular: Boolean) {
        val prefs = LocalPreferencesStore(context).getSnapshot()
        // Always reschedule the next school day for regular alarms
        if (rescheduleRegular && prefs.enabled) {
            ReminderScheduler.scheduleDailyReminder(context, true, prefs.hour, prefs.minute)
        }

        val todayKey = DateUtils.todayKey()
        if (DateUtils.isWeekend(todayKey)) {
            return
        }
        val repository = AttendanceRepository(context.applicationContext)
        val uid = Firebase.auth.currentUser?.uid
        if (!uid.isNullOrBlank() && repository.hasLoggedAttendance(uid, todayKey)) {
            return
        }
        val isNonSchoolDay = runCatching {
            repository.isPublishedNonSchoolDay(todayKey)
        }.getOrElse {
            !AttendanceCalculator.isSchoolDay(todayKey, SriLankaCalendarDefaults.defaultEvents)
        }
        if (isNonSchoolDay) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ReminderScheduler.ensureNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AppConstants.EXTRA_OPEN_CHECKIN, true)
        }
        val openPending = PendingIntent.getActivity(
            context,
            8020,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val presentPending = buildQuickActionPendingIntent(
            context = context,
            action = ReminderScheduler.ACTION_QUICK_YES,
            requestCode = 8021
        )
        val absentPending = buildQuickActionPendingIntent(
            context = context,
            action = ReminderScheduler.ACTION_QUICK_NO,
            requestCode = 8022
        )
        val snoozePending = buildQuickActionPendingIntent(
            context = context,
            action = ReminderScheduler.ACTION_SNOOZE_1H,
            requestCode = 8024
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(0, context.getString(R.string.notif_action_present), presentPending)
            .addAction(0, context.getString(R.string.notif_action_absent), absentPending)
            .addAction(0, context.getString(R.string.notif_action_snooze_1h), snoozePending)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(ReminderScheduler.NOTIFICATION_ID, notification)
    }

    private fun handleSnoozeOneHour(context: Context) {
        val appCtx = context.applicationContext
        val manager = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(ReminderScheduler.NOTIFICATION_ID)
        ReminderScheduler.scheduleSnoozeReminder(appCtx, delayMinutes = 60)

        if (manager != null && ReminderScheduler.hasNotificationPermission(appCtx)) {
            val snoozeConfirm = NotificationCompat.Builder(appCtx, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Reminder snoozed for 1 hour")
                .setContentText("We'll remind you to log today's attendance in 60 minutes.")
                .setSilent(true)
                .setTimeoutAfter(4_000L)
                .setAutoCancel(true)
                .build()
            manager.notify(ReminderScheduler.CONFIRMATION_NOTIFICATION_ID, snoozeConfirm)
        }
    }

    private fun buildQuickActionPendingIntent(
        context: Context,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderScheduler.EXTRA_SOURCE, "notification")
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private suspend fun handleQuickCheckIn(
        context: Context,
        status: AttendanceStatus,
        source: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(ReminderScheduler.NOTIFICATION_ID)

        val uid = Firebase.auth.currentUser?.uid ?: return
        val todayKey = DateUtils.todayKey()
        if (DateUtils.isWeekend(todayKey)) return

        val appCtx = context.applicationContext
        val repository = AttendanceRepository(appCtx)

        // Capture real terms, profile, events, and locked period blocks to prevent double-counting or logging locked/non-school days
        val (preProfile, preRecords, preEvents) = repository.getQuickWidgetMetrics(uid, todayKey)
        val (settings, fetchedTerms) = repository.getQuickWidgetSettingsAndTerms()
        val periodBlocks = repository.getQuickWidgetPeriodBlocks(uid)
        val effectiveTerms = fetchedTerms.ifEmpty { SriLankaCalendarDefaults.defaultTerms }
        val effectiveEvents = AttendanceCalculator.filterCalendarEventsForStudent(preEvents, preProfile, uid)
        val schoolStartKey = settings.effectiveSchoolStartDate(preProfile?.alExamYear)

        if (todayKey < schoolStartKey) return
        if (effectiveEvents.any { it.dateKey == todayKey && it.nonSchoolDay }) return
        if (preProfile?.isDateInLockedBaseline(todayKey) == true) return
        if (periodBlocks.any { it.containsDate(todayKey) }) return

        val termId = AttendanceCalculator.resolveTermIdForDate(todayKey, effectiveTerms)
        val previousRecord = preRecords.firstOrNull { it.dateKey == todayKey }
        val previousStatusRaw = previousRecord?.status
        val previousNote = previousRecord?.note

        val result = repository.upsertAttendance(
            targetUserId = uid,
            dateKey = todayKey,
            termId = termId,
            status = status,
            subjectTags = emptyList(),
            note = null,
            markedBy = "student"
        )
        if (result.isSuccess) {
            LocalPreferencesStore(appCtx).markPromptDismissedForDate(todayKey)
            SyncScheduler.scheduleBackgroundSync(appCtx)

            AttendanceWidgetStore.savePendingExternalUndo(
                context = appCtx,
                dateKey = todayKey,
                statusLabel = status.label,
                previousStatusRaw = previousStatusRaw,
                previousNote = previousNote,
                source = source
            )

            val newPercent = refreshWidgetsFromRepository(appCtx, repository, uid, todayKey, status, termId)

            // Provide instant silent confirmation with a 1-tap Undo button
            showQuickConfirmation(
                context = appCtx,
                manager = manager,
                status = status,
                dateKey = todayKey,
                newPercent = newPercent,
                previousStatusRaw = previousStatusRaw,
                previousNote = previousNote
            )
            if (preProfile == null) {
                Log.d("DailyReminderReceiver", "Quick check-in saved without cached profile ($source)")
            }
        }
    }

    private suspend fun handleQuickUndo(
        context: Context,
        dateKey: String,
        previousStatusRaw: String?,
        previousNote: String? = null
    ) {
        val appCtx = context.applicationContext
        val manager = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(ReminderScheduler.CONFIRMATION_NOTIFICATION_ID)
        AttendanceWidgetStore.clearPendingExternalUndo(appCtx)

        val uid = Firebase.auth.currentUser?.uid ?: return
        val repository = AttendanceRepository(appCtx)
        val (_, fetchedTerms) = repository.getQuickWidgetSettingsAndTerms()
        val effectiveTerms = fetchedTerms.ifEmpty { SriLankaCalendarDefaults.defaultTerms }
        val termId = AttendanceCalculator.resolveTermIdForDate(dateKey, effectiveTerms)
        if (!previousStatusRaw.isNullOrBlank()) {
            val prevStatus = AttendanceStatus.fromRaw(previousStatusRaw)
            repository.upsertAttendance(
                targetUserId = uid,
                dateKey = dateKey,
                termId = termId,
                status = prevStatus,
                subjectTags = emptyList(),
                note = previousNote,
                markedBy = "student"
            )
            refreshWidgetsFromRepository(appCtx, repository, uid, dateKey, prevStatus, termId)
        } else {
            repository.deleteAttendance(uid, dateKey)
            refreshWidgetsFromRepository(appCtx, repository, uid, dateKey, null, termId)
        }
        SyncScheduler.scheduleBackgroundSync(appCtx)

        if (manager != null && ReminderScheduler.hasNotificationPermission(appCtx)) {
            val revertedNotif = NotificationCompat.Builder(appCtx, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Check-in undone")
                .setContentText("Reverted ${DateUtils.formatReadableShort(dateKey)}.")
                .setSilent(true)
                .setTimeoutAfter(3_000L)
                .setAutoCancel(true)
                .build()
            manager.notify(ReminderScheduler.CONFIRMATION_NOTIFICATION_ID, revertedNotif)
        }
    }

    private suspend fun refreshWidgetsFromRepository(
        appCtx: Context,
        repository: AttendanceRepository,
        uid: String,
        todayKey: String,
        overrideTodayStatus: AttendanceStatus?,
        termId: String
    ): Double {
        val (profile, records, events) = repository.getQuickWidgetMetrics(uid, todayKey)
        val periodBlocks = repository.getQuickWidgetPeriodBlocks(uid)
        val (settings, fetchedTerms) = repository.getQuickWidgetSettingsAndTerms()
        val effectiveTerms = fetchedTerms.ifEmpty { SriLankaCalendarDefaults.defaultTerms }
        val resolvedTerm = AttendanceCalculator.resolveActiveTerm(effectiveTerms, todayKey)
        val effectiveEvents = AttendanceCalculator.filterCalendarEventsForStudent(events, profile, uid)
        val mergedRecords = if (overrideTodayStatus != null) {
            records.filterNot { it.dateKey == todayKey } +
                AttendanceRecord(userId = uid, dateKey = todayKey, termId = termId, status = overrideTodayStatus.raw)
        } else {
            records.filterNot { it.dateKey == todayKey }
        }
        val lockedRange = profile?.lockedBaselineRange
        val schoolStartKey = settings.effectiveSchoolStartDate(profile?.alExamYear)
        val stats = AttendanceCalculator.calculateStats(
            records = mergedRecords,
            thresholdPercent = resolvedTerm.minEligibilityPercent,
            countExcusedAsPresent = true,
            includePriorBalance = true,
            priorPresent = profile?.priorPresentCount ?: 0,
            priorAbsent = profile?.priorAbsentCount ?: 0,
            termFilter = resolvedTerm,
            lockedRange = lockedRange,
            periodBlocks = periodBlocks,
            calendarEvents = effectiveEvents,
            schoolStartDate = schoolStartKey
        )
        val streak = AttendanceCalculator.calculateCurrentStreak(
            records = mergedRecords,
            calendarEvents = effectiveEvents,
            countExcusedAsPresent = true,
            todayKey = todayKey,
            periodBlocks = periodBlocks
        )
        val weekly = AttendanceCalculator.calculateWeeklySummary(
            records = mergedRecords,
            calendarEvents = effectiveEvents,
            todayKey = todayKey,
            lockedRange = lockedRange,
            periodBlocks = periodBlocks
        )
        val examYear = profile?.alExamYear ?: settings.currentAcademicYear
        val daysToExam = AttendanceCalculator.daysUntilAlExam(examYear, settings, todayKey)
        val nextHoliday = AttendanceCalculator.nextUpcomingPoyaOrHoliday(effectiveEvents, todayKey)

        AttendanceWidgetStore.saveAndRefresh(
            appCtx,
            WidgetSnapshot(
                percentage = stats.percentage,
                eligible = stats.eligible,
                thresholdPercent = stats.thresholdPercent,
                effectiveAttended = stats.effectiveAttended,
                totalCountedDays = stats.totalCountedDays,
                safeSkipsRemaining = stats.safeSkipsRemaining,
                daysNeededToRecover = stats.daysNeededToRecover,
                streak = streak,
                termName = resolvedTerm.name,
                todayDateKey = todayKey,
                todayStatusLabel = overrideTodayStatus?.label,
                todayNoSchoolTitle = null,
                signedIn = true,
                weekDays = AttendanceWidgetStore.toWidgetWeekDays(weekly),
                daysUntilAlExam = daysToExam,
                alExamYear = examYear,
                nextHolidayTitle = nextHoliday?.title,
                nextHolidayDateShort = nextHoliday?.dateKey?.let { DateUtils.formatReadableShort(it) }
            )
        )
        return stats.percentage
    }

    private fun showQuickConfirmation(
        context: Context,
        manager: NotificationManager?,
        status: AttendanceStatus,
        dateKey: String,
        newPercent: Double,
        previousStatusRaw: String?,
        previousNote: String? = null
    ) {
        if (manager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            8025,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val undoIntent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_QUICK_UNDO
            putExtra(ReminderScheduler.EXTRA_TARGET_DATE, dateKey)
            if (!previousStatusRaw.isNullOrBlank()) {
                putExtra(ReminderScheduler.EXTRA_PREVIOUS_STATUS, previousStatusRaw)
            }
            if (!previousNote.isNullOrBlank()) {
                putExtra(ReminderScheduler.EXTRA_PREVIOUS_NOTE, previousNote)
            }
        }
        val undoPending = PendingIntent.getBroadcast(
            context,
            8026,
            undoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val confirmNotif = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Marked ${status.label} (${DateUtils.formatReadableShort(dateKey)})")
            .setContentText("Term attendance updated to ${String.format(java.util.Locale.US, "%.1f%%", newPercent)}. Mistap? Tap Undo.")
            .setSilent(true)
            .setTimeoutAfter(8_000L)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(0, context.getString(R.string.notif_action_undo), undoPending)
            .build()
        manager.notify(ReminderScheduler.CONFIRMATION_NOTIFICATION_ID, confirmNotif)
    }

    private suspend fun handleNextDayPollAlarm(context: Context) {
        val appCtx = context.applicationContext
        val repository = AttendanceRepository(appCtx)
        val (settings, _) = repository.getQuickWidgetSettingsAndTerms()
        LocalPreferencesStore(appCtx).saveCachedNextDayPollEnabled(settings.nextDayPollEnabled)
        ReminderScheduler.scheduleNextDayPollAlarm(appCtx, settings.nextDayPollEnabled)
        if (!settings.nextDayPollEnabled) return

        val uid = Firebase.auth.currentUser?.uid ?: return
        val todayKey = DateUtils.todayKey()
        val (profile, _, events) = repository.getQuickWidgetMetrics(uid, todayKey)
        if (profile?.setupCompleted != true) return

        val (hourNow, _) = DateUtils.currentHourMinute()
        val window = AttendanceCalculator.resolveNextDayPollWindow(
            calendarEvents = events,
            todayKey = todayKey,
            hourOfDay = hourNow
        )
        if (!window.isOpen) return

        val existingVote = repository.getMyNextDayVote(uid, window.targetDateKey)
        if (existingVote != null) return

        ReminderScheduler.showNextDayPollPromptNotification(
            context = appCtx,
            targetDateKey = window.targetDateKey,
            isTomorrowTarget = window.isTomorrowTarget
        )
    }

    private suspend fun handleNextDayPollVote(
        context: Context,
        going: Boolean,
        extraTargetDate: String?
    ) {
        val appCtx = context.applicationContext
        val manager = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(ReminderScheduler.POLL_NOTIFICATION_ID)

        val uid = Firebase.auth.currentUser?.uid ?: return
        val repository = AttendanceRepository(appCtx)
        val (settings, _) = repository.getQuickWidgetSettingsAndTerms()
        LocalPreferencesStore(appCtx).saveCachedNextDayPollEnabled(settings.nextDayPollEnabled)
        if (!settings.nextDayPollEnabled) return

        val todayKey = DateUtils.todayKey()
        val (hourNow, _) = DateUtils.currentHourMinute()
        val (profile, _, events) = repository.getQuickWidgetMetrics(uid, todayKey)
        val window = AttendanceCalculator.resolveNextDayPollWindow(
            calendarEvents = events,
            todayKey = todayKey,
            hourOfDay = hourNow
        )
        if (!window.isOpen || (!extraTargetDate.isNullOrBlank() && extraTargetDate != window.targetDateKey)) {
            if (manager != null && ReminderScheduler.hasNotificationPermission(appCtx)) {
                val closedNotif = NotificationCompat.Builder(appCtx, ReminderScheduler.POLL_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Roll-call closed at 5:00 AM")
                    .setContentText("The 8:00 PM – 5:00 AM voting window has already ended.")
                    .setAutoCancel(true)
                    .setTimeoutAfter(6_000L)
                    .build()
                manager.notify(ReminderScheduler.POLL_RESULT_NOTIFICATION_ID, closedNotif)
            }
            return
        }

        val saveRes = repository.submitNextDayVote(
            userId = uid,
            targetDateKey = window.targetDateKey,
            going = going,
            profile = profile
        )
        if (saveRes.isFailure) return

        val fetchedVotes = repository.getNextDayVotesForDate(window.targetDateKey)
        val myVoteEntry = NextDaySchoolVote(
            id = "${window.targetDateKey}_$uid",
            userId = uid,
            targetDateKey = window.targetDateKey,
            going = going,
            grade = profile?.grade,
            alStream = profile?.alStream,
            className = profile?.className
        )
        val mergedVotes = fetchedVotes.filterNot { it.userId == uid } + myVoteEntry
        val summary = AttendanceCalculator.calculateNextDayPollSummary(
            votes = mergedVotes,
            currentUserId = uid,
            myClassName = profile?.className,
            targetDateKey = window.targetDateKey
        )
        AttendanceWidgetStore.updatePollState(
            context = appCtx,
            pollEnabled = true,
            pollOpen = true,
            targetDateKey = window.targetDateKey,
            myVoteGoing = going,
            classGoingPercent = (summary.myClassGoingPercent ?: summary.goingPercent).toInt(),
            totalVotes = summary.totalVotes
        )
        ReminderScheduler.showNextDayPollAnalysisNotification(
            context = appCtx,
            summary = summary,
            userVotedGoing = going
        )
    }
}

