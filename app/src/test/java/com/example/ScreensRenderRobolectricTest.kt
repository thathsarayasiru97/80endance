package com.example

import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.AcademicCatalog
import com.example.data.AcademicTerm
import com.example.data.AttendanceCalculator
import com.example.data.AttendancePeriodBlock
import com.example.data.AttendancePeriodEntryMode
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStatus
import com.example.data.BroadcastNotification
import com.example.data.CalendarEvent
import com.example.data.CalendarEventType
import com.example.data.DateUtils
import com.example.data.FeedbackReport
import com.example.data.FeedbackStatus
import com.example.data.FeedbackType
import com.example.data.Notice
import com.example.data.NoticeCategory
import com.example.data.ReleaseNotesCatalog
import com.example.data.UserProfile
import com.example.system.AppUpdateChecker
import com.example.system.AppUpdateInfo
import com.example.system.UpdateCheckStatusSummary
import com.example.ui.admin.AdminBroadcastTab
import com.example.ui.admin.AdminCatalogTab
import com.example.ui.admin.AdminNoSchoolCard
import com.example.ui.admin.AdminTermRolloverCard
import com.example.ui.calendar.CalendarScreen
import com.example.ui.components.AttendanceBaselineDialog
import com.example.ui.components.DailyCheckInDialog
import com.example.ui.components.FeedbackReportDialog
import com.example.ui.components.FeedbackWelcomeDialog
import com.example.ui.components.LiquidGlassBackdropHost
import com.example.ui.components.PinnedNoticeSlideshow
import com.example.ui.components.ProfilePhotoDialog
import com.example.ui.components.UpdateAvailableDialog
import com.example.ui.components.WhatsNewDialog
import com.example.ui.home.HomeScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.setup.AcademicSetupScreen
import com.example.ui.theme.EightyEndanceTheme
import com.example.viewmodel.DashboardScopeMode
import com.example.viewmodel.StudentAppState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScreensRenderRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleState = StudentAppState(
        isLoading = false,
        profile = UserProfile(
            id = "u1",
            firstName = "Yasiru",
            lastName = "Perera",
            className = "13M2",
            alStream = "Physical Science",
            subjects = listOf("Combined Mathematics", "Physics", "Chemistry"),
            setupCompleted = true
        )
    )

    @Test
    fun reportsScreenRendersWithoutCrash() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    ReportsScreen(
                        state = sampleState,
                        onToggleCountExcused = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun homeScreenRendersWithoutCrash() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    HomeScreen(
                        state = sampleState,
                        scopeMode = DashboardScopeMode.CURRENT_TERM,
                        onScopeModeChange = {},
                        onOpenCheckIn = {},
                        onSyncClick = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun calendarScreenRendersWithoutCrash() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    CalendarScreen(
                        state = sampleState,
                        onEditDate = {},
                        onDeleteDate = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun settingsScreenRendersWithoutCrash() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    SettingsScreen(
                        state = sampleState,
                        isVerifiedAdmin = true,
                        onEditProfile = {},
                        onUpdatePreferences = { _, _, _, _ -> },
                        onPreviewCheckInPopup = {},
                        onManualSync = {},
                        onOpenAdminPanel = {},
                        onSignOut = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun academicSetupAndCheckInDialogRenderWithoutCrash() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    AcademicSetupScreen(
                        initialDisplayName = "Yasiru Perera",
                        existingProfile = sampleState.profile,
                        isSaving = false,
                        onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
                    )
                    DailyCheckInDialog(
                        dateKey = "2026-11-20",
                        existingRecord = null,
                        subjects = listOf("Combined Mathematics", "Physics", "Chemistry"),
                        isSaving = false,
                        onDismiss = {},
                        onSave = { _, _, _ -> }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun updateAvailableDialogRendersWithoutCrash() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    UpdateAvailableDialog(
                        updateInfo = AppUpdateInfo(
                            currentVersion = "1.0",
                            latestVersion = "v1.1.0",
                            releaseTitle = "80endance v1.1.0",
                            releaseNotes = "- Improved performance\n- Bug fixes",
                            releaseUrl = AppUpdateChecker.GITHUB_LATEST_RELEASE_WEB
                        ),
                        onUpdateNow = {},
                        onDismiss = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("update_available_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("update_now_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("update_later_button").assertIsDisplayed()
    }

    @Test
    fun homeScreenShowsWeekendCardAndMonthTab() {
        val weekendState = sampleState.copy(
            todayIsWeekend = true,
            currentMonthLabel = "November 2026"
        )
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    HomeScreen(
                        state = weekendState,
                        scopeMode = DashboardScopeMode.CURRENT_MONTH,
                        onScopeModeChange = {},
                        onOpenCheckIn = {},
                        onSyncClick = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_profile_avatar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("scope_tab_month").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_weekend_card"))
        composeTestRule.onNodeWithTag("home_weekend_card").assertIsDisplayed()
    }

    @Test
    fun homeScreenShowsAdminNoSchoolCardWhenPublished() {
        val noSchoolState = sampleState.copy(
            todayIsWeekend = false,
            todayNonSchoolEvent = CalendarEvent(
                id = "no_school_2026_11_20",
                title = "Exam Study Leave",
                dateKey = "2026-11-20",
                eventType = CalendarEventType.SCHOOL_HOLIDAY.raw
            )
        )
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    HomeScreen(
                        state = noSchoolState,
                        scopeMode = DashboardScopeMode.CURRENT_TERM,
                        onScopeModeChange = {},
                        onOpenCheckIn = {},
                        onSyncClick = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_no_school_card"))
        composeTestRule.onNodeWithTag("home_no_school_card").assertIsDisplayed()
    }

    @Test
    fun profilePhotoDialogRendersWithoutCrash() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                ProfilePhotoDialog(
                    displayName = "Yasiru Perera",
                    customAvatarBase64 = null,
                    googlePhotoUrl = "https://example.com/photo.jpg",
                    onSaveCustomAvatar = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_photo_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("choose_profile_photo_button").assertIsDisplayed()
    }

    @Test
    fun homeScreenShowsMissedDayCatchUpBanner() {
        val catchUpState = sampleState.copy(
            missedSchoolDays = listOf("2026-11-19", "2026-11-18")
        )
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    HomeScreen(
                        state = catchUpState,
                        scopeMode = DashboardScopeMode.CURRENT_TERM,
                        onScopeModeChange = {},
                        onOpenCheckIn = {},
                        onSyncClick = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_catchup_card"))
        composeTestRule.onNodeWithTag("home_catchup_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("catchup_present_2026-11-19").assertIsDisplayed()
    }

    @Test
    fun reportsScreenShowsWhatIfSimulatorAndPatternsChart() {
        val stateWithRecords = sampleState.copy(
            attendanceRecords = listOf(
                AttendanceRecord(dateKey = "2026-10-05", status = AttendanceStatus.PRESENT.raw),
                AttendanceRecord(dateKey = "2026-10-09", status = AttendanceStatus.ABSENT.raw),
                AttendanceRecord(dateKey = "2026-11-02", status = AttendanceStatus.PRESENT.raw),
                AttendanceRecord(dateKey = "2026-11-03", status = AttendanceStatus.SCHOOL_EVENT.raw)
            )
        )
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    ReportsScreen(state = stateWithRecords)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reports_lazy_column")
            .performScrollToNode(hasTestTag("what_if_simulator_card"))
        composeTestRule.onNodeWithTag("what_if_simulator_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reports_lazy_column")
            .performScrollToNode(hasTestTag("attendance_patterns_card"))
        composeTestRule.onNodeWithTag("attendance_patterns_card").assertIsDisplayed()
    }

    @Test
    fun adminNoSchoolCardRendersAllModesAndTargeting() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.verticalScroll(
                        androidx.compose.foundation.rememberScrollState()
                    )
                ) {
                    AdminNoSchoolCard(
                        isBusy = false,
                        onMarkNoSchoolDays = { _, _, _, _, _ -> }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_no_school_card").assertExists()
        composeTestRule.onNodeWithTag("admin_no_school_mode_selector").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_no_school_custom_reason_chip").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_mark_no_school_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun calendarDisablesFutureDaysAndLocksImportedBaselineDays() {
        val today = DateUtils.todayKey()
        val (curY, curM) = DateUtils.currentYearMonth()
        val daysInMonth = DateUtils.daysInMonth(curY, curM)
        // Find a future weekday in the current month with no calendar events (if one exists)
        val futureWeekdayInMonth = (1..daysInMonth)
            .map { DateUtils.formatDateKey(curY, curM, it) }
            .firstOrNull { it > today && !DateUtils.isWeekend(it) }
        // Find a past/current weekday in the current month to lock
        val lockDayInMonth = (1..daysInMonth)
            .map { DateUtils.formatDateKey(curY, curM, it) }
            .firstOrNull { it <= today && !DateUtils.isWeekend(it) }

        val lockedProfile = sampleState.profile!!.copy(
            priorPresentCount = 16,
            priorAbsentCount = 4,
            baselineStartDate = lockDayInMonth ?: "2026-11-02",
            baselineEndDate = lockDayInMonth ?: "2026-11-10",
            baselinePercent = 80.0
        )
        val lockedState = sampleState.copy(
            profile = lockedProfile,
            calendarEvents = emptyList()
        )

        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    CalendarScreen(
                        state = lockedState,
                        onEditDate = {},
                        onDeleteDate = {},
                        onOpenBaselineImport = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("calendar_baseline_banner").assertIsDisplayed()

        if (futureWeekdayInMonth != null) {
            composeTestRule.onNodeWithTag("calendar_day_$futureWeekdayInMonth")
                .assertHasNoClickAction()
        }
        if (lockDayInMonth != null) {
            composeTestRule.onNodeWithTag("calendar_day_$lockDayInMonth").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("calendar_locked_day_notice").assertIsDisplayed()
        }
    }

    @Test
    fun attendanceBaselineDialogComputesPreviewAndSaves() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                AttendanceBaselineDialog(
                    profile = sampleState.profile,
                    activeTerm = sampleState.activeTerm,
                    terms = sampleState.terms,
                    calendarEvents = emptyList(),
                    existingRecords = emptyList(),
                    isSaving = false,
                    onDismiss = {},
                    onSave = { _, _, _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("baseline_import_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("baseline_percent_input").performTextInput("85")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("baseline_preview_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("baseline_save_button").assertIsDisplayed()
    }

    @Test
    fun pinnedNoticeSlideshowRendersMultipleNoticesIncludingNormalNote() {
        val notices = listOf(
            Notice(
                id = "n1",
                title = "Lab Coat Reminder",
                message = "Bring lab coats for tomorrow's Chemistry practical.",
                category = NoticeCategory.NORMAL_NOTE.raw,
                pinned = true,
                published = true
            ),
            Notice(
                id = "n2",
                title = "Term Test Timetable",
                message = "Term 2 timetable is posted on the main notice board.",
                category = NoticeCategory.EXAM.raw,
                pinned = true,
                published = true
            )
        )
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            EightyEndanceTheme {
                PinnedNoticeSlideshow(
                    pinnedNotices = notices,
                    totalNoticeCount = notices.size,
                    onNoticeClick = {},
                    onViewAllClick = {}
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(100L)
        composeTestRule.onNodeWithTag("pinned_notices_slideshow").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pinned_slide_dot_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pinned_slide_dot_1").assertIsDisplayed()
        // Advance past the 5s auto-advance timer to confirm smooth slideshow transition without crashing
        composeTestRule.mainClock.advanceTimeBy(5600L)
        composeTestRule.onNodeWithTag("pinned_notices_slideshow").assertIsDisplayed()
    }

    @Test
    fun reportsScreenRendersMilestoneBadgesCard() {
        val milestones = AttendanceCalculator.calculateMilestones(
            records = listOf(
                AttendanceRecord(dateKey = "2026-05-04", status = AttendanceStatus.PRESENT.raw)
            ),
            currentStreak = 5,
            termStats = sampleState.currentTermStats,
            overallStats = sampleState.overallStats
        )
        val stateWithMilestones = sampleState.copy(milestones = milestones)
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    ReportsScreen(state = stateWithMilestones)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reports_lazy_column")
            .performScrollToNode(hasTestTag("reports_milestones_card"))
        composeTestRule.onNodeWithTag("reports_milestones_card").assertIsDisplayed()
    }

    @Test
    fun settingsScreenRendersAppearanceSundayPrepAndFeedbackCards() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    SettingsScreen(
                        state = sampleState,
                        isVerifiedAdmin = false,
                        onEditProfile = {},
                        onUpdatePreferences = { _, _, _, _ -> },
                        onPreviewCheckInPopup = {},
                        onManualSync = {},
                        onOpenAdminPanel = {},
                        onSignOut = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_sunday_prep_switch"))
        composeTestRule.onNodeWithTag("settings_sunday_prep_switch").assertIsDisplayed()

        composeTestRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_appearance_card"))
        composeTestRule.onNodeWithTag("settings_appearance_card").assertIsDisplayed()

        composeTestRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_feedback_card"))
        composeTestRule.onNodeWithTag("settings_feedback_card").assertIsDisplayed()
    }

    @Test
    fun feedbackReportDialogRendersAndAcceptsInput() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                FeedbackReportDialog(
                    initialType = FeedbackType.BUG,
                    isSubmitting = false,
                    onDismiss = {},
                    onSubmit = { _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("feedback_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("feedback_title_input").performTextInput("Slideshow lag")
        composeTestRule.onNodeWithTag("feedback_message_input").performTextInput("Happens on home tab")
        composeTestRule.onNodeWithTag("feedback_submit_button").assertIsDisplayed()
    }

    @Test
    fun adminTermRolloverCardRendersActiveAndTargetTermControls() {
        val terms = listOf(
            AcademicTerm(
                id = "term_1_2026",
                name = "Term 1 — 2026",
                startDate = "2026-01-05",
                endDate = "2026-04-09",
                academicYear = 2026,
                active = true
            ),
            AcademicTerm(
                id = "term_2_2026",
                name = "Term 2 — 2026",
                startDate = "2026-04-27",
                endDate = "2026-08-07",
                academicYear = 2026,
                active = false
            )
        )
        composeTestRule.setContent {
            EightyEndanceTheme {
                AdminTermRolloverCard(
                    terms = terms,
                    isBusy = false,
                    onRolloverTerm = { _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_term_rollover_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_rollover_confirm_button").assertIsDisplayed()
    }

    @Test
    fun homeScreenShowsCommunityCountPillAdminOnlinePillAndBroadcastBanner() {
        val stateWithLiveCommunity = sampleState.copy(
            totalAccounts = 128,
            adminOnlineNowCount = 9,
            unreadBroadcasts = listOf(
                BroadcastNotification(
                    id = "bc_1",
                    title = "Special Assembly Tomorrow",
                    message = "All A/L students report to the main hall by 7:20 AM.",
                    targetAll = true,
                    audienceSummary = "Everyone (All Students)"
                )
            )
        )
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    HomeScreen(
                        state = stateWithLiveCommunity,
                        scopeMode = DashboardScopeMode.CURRENT_TERM,
                        onScopeModeChange = {},
                        onOpenCheckIn = {},
                        onSyncClick = {}
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(400L)
        composeTestRule.onNodeWithTag("home_community_count_pill").assertIsDisplayed()
        composeTestRule.onNodeWithText("128").assertIsDisplayed()
        composeTestRule.onNodeWithText("128 students").assertDoesNotExist()
        composeTestRule.onNodeWithTag("admin_online_now_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_broadcast_alert_card").assertIsDisplayed()
    }

    @Test
    fun adminBroadcastTabOpensConfirmDialogOnSend() {
        val students = listOf(
            sampleState.profile!!.copy(id = "u1", userId = "u1"),
            UserProfile(
                id = "u2",
                userId = "u2",
                firstName = "Nimal",
                lastName = "Silva",
                grade = "Grade 12",
                alStream = "Bio Science",
                className = "12B1"
            )
        )
        composeTestRule.setContent {
            EightyEndanceTheme {
                AdminBroadcastTab(
                    students = students,
                    broadcasts = emptyList(),
                    isBusy = false,
                    onSendBroadcast = { _, _, _, _, _, _, _, _, onDone -> onDone() },
                    onDeleteBroadcast = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_broadcast_title_input").performTextInput("Exam Room Change")
        composeTestRule.onNodeWithTag("admin_broadcast_message_input").performTextInput("13M2 moved to Block B.")
        composeTestRule.onNodeWithTag("admin_broadcast_lazy_column")
            .performScrollToNode(hasTestTag("admin_broadcast_send_button"))
        composeTestRule.onNodeWithTag("admin_broadcast_send_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_broadcast_confirm_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_broadcast_confirm_send_button").assertIsDisplayed()
    }

    @Test
    fun settingsScreenDisplaysUpdateStatusBanner() {
        val status = UpdateCheckStatusSummary(
            checkedAtMillis = 1_750_000_000_000L,
            statusLine = "Up to date (latest on GitHub: v1.0)",
            detailLine = "Checked at 10:30 AM",
            hasUpdate = false,
            isError = false
        )
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    SettingsScreen(
                        state = sampleState,
                        isVerifiedAdmin = false,
                        updateStatusSummary = status,
                        onEditProfile = {},
                        onUpdatePreferences = { _, _, _, _ -> },
                        onPreviewCheckInPopup = {},
                        onManualSync = {},
                        onOpenAdminPanel = {},
                        onSignOut = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_update_status_banner"))
        composeTestRule.onNodeWithTag("settings_update_status_banner").assertIsDisplayed()
    }

    @Test
    fun homeScreenShowsHeroAlCountdownAndInAppResolvedFeedbackBanner() {
        val stateWithHeroAndResolved = sampleState.copy(
            daysUntilAlExam = 142,
            resolvedFeedbackAlerts = listOf(
                FeedbackReport(
                    id = "fb_1",
                    userId = "u1",
                    type = FeedbackType.BUG.raw,
                    title = "Test popup on weekends",
                    message = "Doesn't open on Saturday",
                    status = FeedbackStatus.RESOLVED.raw
                )
            )
        )
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    HomeScreen(
                        state = stateWithHeroAndResolved,
                        scopeMode = DashboardScopeMode.CURRENT_TERM,
                        onScopeModeChange = {},
                        onOpenCheckIn = {},
                        onSyncClick = {}
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(300L)
        composeTestRule.onNodeWithTag("home_al_countdown_hero").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_resolved_feedback_banner").assertIsDisplayed()

        // Advance past the 2000ms spotlight hold so the countdown docks smoothly into the compact chip
        composeTestRule.mainClock.advanceTimeBy(2300L)
        composeTestRule.onNodeWithTag("home_al_countdown_chip").assertIsDisplayed()
    }

    @Test
    fun whatsNewAndFeedbackWelcomeDialogsRenderCorrectly() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                WhatsNewDialog(
                    releaseNotes = ReleaseNotesCatalog.forVersion("1.6"),
                    onContinue = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("whats_new_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("whats_new_continue_button").assertIsDisplayed()
    }

    @Test
    fun feedbackWelcomeDialogShowsLocationAndButtons() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                FeedbackWelcomeDialog(
                    isAfterUpdate = true,
                    onGoToSettings = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("feedback_welcome_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("feedback_welcome_go_settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("feedback_welcome_dismiss").assertIsDisplayed()
    }

    @Test
    fun adminCatalogTabRendersSubjectAndClassEditors() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                AdminCatalogTab(
                    catalog = AcademicCatalog.defaultCatalog(),
                    isBusy = false,
                    onSaveCatalog = { _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_catalog_tab").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_catalog_tab")
            .performScrollToNode(hasTestTag("admin_catalog_add_subject_input"))
        composeTestRule.onNodeWithTag("admin_catalog_add_subject_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_catalog_tab")
            .performScrollToNode(hasTestTag("admin_catalog_add_class_input"))
        composeTestRule.onNodeWithTag("admin_catalog_add_class_input").assertIsDisplayed()
    }

    @Test
    fun academicCatalogJsonRoundTripWorksOnAndroid() {
        val updated = AcademicCatalog.defaultCatalog()
            .withAddedSubject("Physical Science", AcademicCatalog.BUCKET_3, "Higher Mathematics")
            .withAddedClass("Grade 12", "Bio Science", "12S7")
        val (streamsJson, classesJson) = updated.toFirestoreJsonPair()
        val restored = AcademicCatalog.fromFirestoreJson(streamsJson, classesJson)
        org.junit.Assert.assertTrue(
            restored.subjectsFor("Physical Science", AcademicCatalog.BUCKET_3).contains("Higher Mathematics")
        )
        org.junit.Assert.assertTrue(
            restored.classesFor("Grade 12", "Bio Science").contains("12S7")
        )
    }

    @Test
    fun adminCanTapAnyFutureDayOnCalendarAndSeeAddEventButton() {
        val today = DateUtils.todayKey()
        val (curY, curM) = DateUtils.currentYearMonth()
        val daysInMonth = DateUtils.daysInMonth(curY, curM)
        val futureDay = (1..daysInMonth)
            .map { DateUtils.formatDateKey(curY, curM, it) }
            .firstOrNull { it > today } ?: today

        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    CalendarScreen(
                        state = sampleState.copy(calendarEvents = emptyList()),
                        onEditDate = {},
                        onDeleteDate = {},
                        isVerifiedAdmin = true,
                        onAdminSaveCalendarEvent = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
                        onAdminDeleteCalendarEvent = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("calendar_day_$futureDay").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("calendar_admin_add_event_button").assertIsDisplayed()
    }

    @Test
    fun settingsScreenRendersBackgroundNotificationsCardAndControls() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    SettingsScreen(
                        state = sampleState.copy(
                            instantBackgroundAlertsEnabled = true,
                            lastBackgroundCheckMillis = 1_750_000_000_000L
                        ),
                        isVerifiedAdmin = false,
                        onEditProfile = {},
                        onUpdatePreferences = { _, _, _, _ -> },
                        onPreviewCheckInPopup = {},
                        onManualSync = {},
                        onOpenAdminPanel = {},
                        onSignOut = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_background_notifications_card"))
        composeTestRule.onNodeWithTag("settings_background_notifications_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_instant_bg_alerts_switch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_check_bg_now_button").assertIsDisplayed()
    }

    @Test
    fun cachedStudentAudienceProfileMatchesTargetedBroadcastInColdProcess() {
        val prefs = com.example.data.LocalReminderPrefs(
            cachedUserId = "student_42",
            cachedGrade = "Grade 12",
            cachedStream = "Bio Science",
            cachedClassName = "12S2",
            cachedSetupCompleted = true
        )
        val cachedProfile = prefs.toCachedUserProfile()
        org.junit.Assert.assertNotNull(cachedProfile)

        val classBroadcast = BroadcastNotification(
            id = "bc_class",
            title = "Lab Cancelled",
            message = "No Biology practical today for 12S2.",
            targetAll = false,
            targetClasses = listOf("12S2")
        )
        val otherClassBroadcast = BroadcastNotification(
            id = "bc_other",
            title = "Commerce Seminar",
            message = "Only for 13C1.",
            targetAll = false,
            targetClasses = listOf("13C1")
        )

        org.junit.Assert.assertTrue(classBroadcast.matchesStudent(cachedProfile, "student_42"))
        org.junit.Assert.assertFalse(otherClassBroadcast.matchesStudent(cachedProfile, "student_42"))

        val incompletePrefs = prefs.copy(cachedSetupCompleted = false)
        org.junit.Assert.assertNull(incompletePrefs.toCachedUserProfile())
    }

    @Test
    fun backgroundListenerNotificationAndSchedulerBuildCleanlyOnSdk36() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        com.example.system.ReminderScheduler.ensureNotificationChannel(context)
        val notification = com.example.system.ReminderScheduler.buildBackgroundListenerNotification(context)
        org.junit.Assert.assertNotNull(notification)
        org.junit.Assert.assertEquals(
            com.example.system.ReminderScheduler.BACKGROUND_SERVICE_CHANNEL_ID,
            notification.channelId
        )
        com.example.system.SyncScheduler.scheduleBackgroundBroadcastAlarm(context, delayMs = 60_000L)
        com.example.system.SyncScheduler.cancelBackgroundBroadcastAlarm(context)
    }

    @Test
    fun developedByAtayaFooterRendersAtBottomOfHomeAndSettings() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    HomeScreen(
                        state = sampleState,
                        scopeMode = DashboardScopeMode.CURRENT_TERM,
                        onScopeModeChange = {},
                        onOpenCheckIn = {},
                        onSyncClick = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("developed_by_ataya_footer"))
        composeTestRule.onNodeWithTag("developed_by_ataya_footer").assertIsDisplayed()
    }

    @Test
    fun settingsScreenRendersDevelopedByAtayaFooterAtBottom() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    SettingsScreen(
                        state = sampleState,
                        isVerifiedAdmin = false,
                        onEditProfile = {},
                        onUpdatePreferences = { _, _, _, _ -> },
                        onPreviewCheckInPopup = {},
                        onManualSync = {},
                        onOpenAdminPanel = {},
                        onSignOut = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("developed_by_ataya_footer"))
        composeTestRule.onNodeWithTag("developed_by_ataya_footer").assertIsDisplayed()
    }

    @Test
    fun calendarSeparatesSchoolUpdatesFromSriLankanHolidaysAndAdminHasExamBreak() {
        val (curY, curM) = DateUtils.currentYearMonth()
        val day10 = DateUtils.formatDateKey(curY, curM, 10)
        val day18 = DateUtils.formatDateKey(curY, curM, 18)

        val events = listOf(
            CalendarEvent(
                id = "ev_study_break",
                title = "Term Test Study Break",
                dateKey = day10,
                eventType = CalendarEventType.STUDY_LEAVE.raw,
                description = "No school between Physics and Chemistry exam days."
            ),
            CalendarEvent(
                id = "ev_poya",
                title = "Full Moon Poya Day",
                dateKey = day18,
                eventType = CalendarEventType.POYA.raw
            )
        )

        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    CalendarScreen(
                        state = sampleState.copy(calendarEvents = events),
                        onEditDate = {},
                        onDeleteDate = {},
                        isVerifiedAdmin = true,
                        onAdminSaveCalendarEvent = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
                        onAdminDeleteCalendarEvent = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("calendar_lazy_column")
            .performScrollToNode(hasTestTag("calendar_school_updates_section"))
        composeTestRule.onNodeWithTag("calendar_school_updates_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("calendar_lazy_column")
            .performScrollToNode(hasTestTag("calendar_sl_holidays_section"))
        composeTestRule.onNodeWithTag("calendar_sl_holidays_section").assertIsDisplayed()

        // Scroll back to calendar grid, tap day 10 and open Admin Add Event dialog to verify Exam Break / Study Leave chip exists
        composeTestRule.onNodeWithTag("calendar_lazy_column")
            .performScrollToNode(hasTestTag("calendar_day_$day10"))
        composeTestRule.onNodeWithTag("calendar_day_$day10").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("calendar_admin_add_event_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_event_type_study_leave").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun setupReplacesJoiningMidTermWithImportAttendanceAndCalculator() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    AcademicSetupScreen(
                        initialDisplayName = "Yasiru Perera",
                        existingProfile = null,
                        isSaving = false,
                        onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Joining Mid-Term? (Optional)").assertDoesNotExist()

        composeTestRule.onNodeWithTag("setup_import_attendance_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("setup_import_attendance_toggle").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("setup_import_mode_calculator").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("setup_calc_days_held_input").performScrollTo().performTextInput("50")
        composeTestRule.onNodeWithTag("setup_calc_days_attended_input").performScrollTo().performTextInput("42")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("setup_calc_result_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Calculated Attendance: 84.0%", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("setup_baseline_lock_notice").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun attendanceBaselineDialogSupportsPercentageCalculatorMode() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    AttendanceBaselineDialog(
                        profile = sampleState.profile,
                        activeTerm = sampleState.activeTerm,
                        terms = sampleState.terms,
                        calendarEvents = emptyList(),
                        existingRecords = emptyList(),
                        isSaving = false,
                        onDismiss = {},
                        onSave = { _, _, _, _, _ -> }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("baseline_mode_calculator").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("baseline_calc_days_held_input").performScrollTo().performTextInput("40")
        composeTestRule.onNodeWithTag("baseline_calc_days_attended_input").performScrollTo().performTextInput("36")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("baseline_preview_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("90.0%", substring = true).assertIsDisplayed()
    }

    @Test
    fun homeScreenRendersPinnedUpdatesAboveNewAnnouncements() {
        val stateWithPinnedAndBroadcast = sampleState.copy(
            daysUntilAlExam = 0,
            pinnedNotices = listOf(
                Notice(
                    id = "pin_1",
                    title = "Pinned Exam Schedule",
                    message = "Term tests start next Monday.",
                    category = NoticeCategory.EXAM.raw,
                    pinned = true,
                    published = true
                )
            ),
            visibleNotices = listOf(
                Notice(
                    id = "pin_1",
                    title = "Pinned Exam Schedule",
                    message = "Term tests start next Monday.",
                    category = NoticeCategory.EXAM.raw,
                    pinned = true,
                    published = true
                )
            ),
            unreadBroadcasts = listOf(
                BroadcastNotification(
                    id = "bc_order_test",
                    title = "New Assembly Update",
                    message = "Report at 7:20 AM.",
                    targetAll = true,
                    audienceSummary = "Everyone"
                )
            )
        )
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    HomeScreen(
                        state = stateWithPinnedAndBroadcast,
                        scopeMode = DashboardScopeMode.CURRENT_TERM,
                        onScopeModeChange = {},
                        onOpenCheckIn = {},
                        onSyncClick = {}
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(400L)
        val pinnedBounds = composeTestRule.onNodeWithText("Pinned Exam Schedule").getBoundsInRoot()
        val broadcastBounds = composeTestRule.onNodeWithTag("home_broadcast_alert_card").getBoundsInRoot()
        org.junit.Assert.assertTrue(
            "Expected Pinned Updates (top=${pinnedBounds.top}) to appear above New Announcements (top=${broadcastBounds.top})",
            pinnedBounds.top < broadcastBounds.top
        )
    }

    @Test
    fun periodAttendanceDialogCalculatesMissedDaysAndShowsLockPreview() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    com.example.ui.components.PeriodAttendanceDialog(
                        profile = sampleState.profile,
                        calendarEvents = emptyList(),
                        existingRecords = emptyList(),
                        existingPeriodBlocks = emptyList(),
                        isSaving = false,
                        onDismiss = {},
                        onSave = { _, _, _, _, _, _, _, _ -> }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("period_mode_missed").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("period_value_input").performScrollTo().performTextInput("1")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("period_preview_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("1 Missed", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("period_save_button").assertIsDisplayed()
    }

    @Test
    fun trendsCardDefaultsToEveryoneAllTimeAndSwitchesToMonthlyAndPersonal() {
        val personalRecords = listOf(
            AttendanceRecord(dateKey = "2026-10-05", status = AttendanceStatus.PRESENT.raw),
            AttendanceRecord(dateKey = "2026-11-02", status = AttendanceStatus.PRESENT.raw),
            AttendanceRecord(dateKey = "2026-11-06", status = AttendanceStatus.ABSENT.raw)
        )
        val summary1 = AttendanceCalculator.buildStudentTrendSummary("u1", personalRecords)
        val summary2 = AttendanceCalculator.buildStudentTrendSummary(
            "u2",
            listOf(
                AttendanceRecord(dateKey = "2026-10-05", status = AttendanceStatus.PRESENT.raw),
                AttendanceRecord(dateKey = "2026-11-02", status = AttendanceStatus.PRESENT.raw),
                AttendanceRecord(dateKey = "2026-11-06", status = AttendanceStatus.PRESENT.raw)
            )
        )
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    com.example.ui.reports.AttendancePatternsAndChartCard(
                        records = personalRecords,
                        thresholdPercent = 80,
                        periodBlocks = emptyList(),
                        communitySummaries = listOf(summary1, summary2)
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        // Default page is Everyone -> All Time
        composeTestRule.onNodeWithTag("trends_page_everyone").assertIsDisplayed()
        composeTestRule.onNodeWithText("Everyone • All-Time Average").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 students").assertIsDisplayed()

        // Switch Everyone to Monthly
        composeTestRule.onNodeWithTag("trends_everyone_monthly").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Everyone • November 2026").assertIsDisplayed()

        // Slide/switch to My Stats tab
        composeTestRule.onNodeWithTag("trends_tab_personal").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("trends_page_personal").assertIsDisplayed()
        composeTestRule.onNodeWithText("My All-Time Average").assertIsDisplayed()

        // Switch My Stats to Monthly
        composeTestRule.onNodeWithTag("trends_personal_monthly").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("My November 2026 Average").assertIsDisplayed()
    }

    @Test
    fun dynamicAttendanceWidgetBuildsCompactStandardAndExpandedViewsCleanly() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val snapshot = com.example.system.WidgetSnapshot(
            percentage = 87.5,
            eligible = true,
            thresholdPercent = 80,
            effectiveAttended = 35,
            totalCountedDays = 40,
            safeSkipsRemaining = 3,
            daysNeededToRecover = 0,
            streak = 6,
            termName = "Term 3 — 2026",
            todayDateKey = "2026-11-19",
            todayStatusLabel = "Present",
            todayNoSchoolTitle = null,
            signedIn = true,
            weekDays = listOf(
                com.example.system.WidgetWeekDay("Mon", "present", false),
                com.example.system.WidgetWeekDay("Tue", "present", false),
                com.example.system.WidgetWeekDay("Wed", "absent", false),
                com.example.system.WidgetWeekDay("Thu", "present", true),
                com.example.system.WidgetWeekDay("Fri", "unlogged", false)
            ),
            daysUntilAlExam = 120,
            alExamYear = 2026,
            nextHolidayTitle = "Il Full Moon Poya",
            nextHolidayDateShort = "Nov 24"
        )
        val compact = com.example.system.AttendanceWidgetProvider.buildCompactViews(context, snapshot)
        val standard = com.example.system.AttendanceWidgetProvider.buildStandardViews(context, snapshot)
        val expanded = com.example.system.AttendanceWidgetProvider.buildExpandedViews(context, snapshot)
        org.junit.Assert.assertNotNull(compact)
        org.junit.Assert.assertNotNull(standard)
        org.junit.Assert.assertNotNull(expanded)
    }

    @Test
    fun communityAndPersonalTrendsAggregateAccuratelyAcrossAllTimeAndMonthly() {
        val student1Records = listOf(
            AttendanceRecord(dateKey = "2026-10-05", status = AttendanceStatus.PRESENT.raw), // Mon
            AttendanceRecord(dateKey = "2026-10-09", status = AttendanceStatus.ABSENT.raw),  // Fri
            AttendanceRecord(dateKey = "2026-11-02", status = AttendanceStatus.PRESENT.raw), // Mon
            AttendanceRecord(dateKey = "2026-11-06", status = AttendanceStatus.PRESENT.raw)  // Fri
        )
        val student2Records = listOf(
            AttendanceRecord(dateKey = "2026-10-05", status = AttendanceStatus.PRESENT.raw), // Mon
            AttendanceRecord(dateKey = "2026-10-09", status = AttendanceStatus.ABSENT.raw),  // Fri
            AttendanceRecord(dateKey = "2026-11-02", status = AttendanceStatus.PRESENT.raw), // Mon
            AttendanceRecord(dateKey = "2026-11-06", status = AttendanceStatus.ABSENT.raw)   // Fri
        )

        val sum1 = AttendanceCalculator.buildStudentTrendSummary("u1", student1Records)
        val sum2 = AttendanceCalculator.buildStudentTrendSummary("u2", student2Records)

        val communityAllTime = AttendanceCalculator.calculateCommunityInsights(
            summaries = listOf(sum1, sum2),
            timeframe = com.example.data.TrendsTimeframe.ALL_TIME,
            currentYearMonth = "2026-11"
        )
        org.junit.Assert.assertEquals(2, communityAllTime.contributorCount)
        org.junit.Assert.assertEquals(5, communityAllTime.totalAttendedDays)
        org.junit.Assert.assertEquals(8, communityAllTime.totalCountedDays)
        org.junit.Assert.assertEquals(62.5, communityAllTime.overallPercent ?: 0.0, 0.05)
        org.junit.Assert.assertEquals("Friday", communityAllTime.mostSkippedWeekday?.fullName)
        org.junit.Assert.assertEquals(3, communityAllTime.mostSkippedWeekday?.absentDays)
        org.junit.Assert.assertEquals("Monday", communityAllTime.bestWeekday?.fullName)

        val communityMonthly = AttendanceCalculator.calculateCommunityInsights(
            summaries = listOf(sum1, sum2),
            timeframe = com.example.data.TrendsTimeframe.MONTHLY,
            currentYearMonth = "2026-11"
        )
        org.junit.Assert.assertEquals(3, communityMonthly.totalAttendedDays)
        org.junit.Assert.assertEquals(4, communityMonthly.totalCountedDays)
        org.junit.Assert.assertEquals(75.0, communityMonthly.overallPercent ?: 0.0, 0.05)
        org.junit.Assert.assertEquals(25.0, communityMonthly.monthOverMonthDelta ?: 0.0, 0.05)
        org.junit.Assert.assertEquals("November 2026", communityMonthly.activeMonthLabel)
    }

    @Test
    fun widgetWeekDayJsonSerializationRoundTripsCleanly() {
        val sampleDays = listOf(
            com.example.system.WidgetWeekDay("Mon", "present", false),
            com.example.system.WidgetWeekDay("Tue", "absent", false),
            com.example.system.WidgetWeekDay("Wed", "locked", false),
            com.example.system.WidgetWeekDay("Thu", "unlogged", true),
            com.example.system.WidgetWeekDay("Fri", "off", false)
        )
        val json = com.example.system.AttendanceWidgetStore.serializeWeekDays(sampleDays)
        val restored = com.example.system.AttendanceWidgetStore.deserializeWeekDays(json)
        org.junit.Assert.assertEquals(5, restored.size)
        org.junit.Assert.assertEquals(sampleDays, restored)
    }

    @Test
    fun homeScreenShowsDangerZoneBannerPrivacyBlurSwipeButtonAndCelebration() {
        val edgeStats = com.example.data.AttendanceStats(
            percentage = 80.0, eligible = true, thresholdPercent = 80,
            presentDays = 8, absentDays = 2, medicalDays = 0, schoolEventDays = 0, holidayDays = 0,
            priorPresentDays = 0, priorAbsentDays = 0,
            effectiveAttended = 8, totalCountedDays = 10, safeSkipsRemaining = 0, daysNeededToRecover = 0
        )
        val homeState = sampleState.copy(
            currentTermStats = edgeStats,
            overallStats = edgeStats,
            isInDangerZone = true,
            missedSchoolDays = listOf("2026-11-18", "2026-11-19")
        )
        val celebration = com.example.data.CelebrationEvent(
            type = com.example.data.CelebrationType.PERFECT_WEEK,
            title = "Perfect 5/5 Week!",
            subtitle = "You attended every school day Mon–Fri.",
            badgeLabel = "5/5"
        )
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    HomeScreen(
                        state = homeState,
                        scopeMode = DashboardScopeMode.CURRENT_TERM,
                        onScopeModeChange = {},
                        onOpenCheckIn = {},
                        onSyncClick = {},
                        celebrationEvent = celebration,
                        onDismissCelebration = {}
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("celebration_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("privacy_blur_toggle"))
        composeTestRule.onNodeWithTag("privacy_blur_toggle").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_danger_zone_banner"))
        composeTestRule.onNodeWithTag("home_danger_zone_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_open_swipe_deck_btn"))
        composeTestRule.onNodeWithTag("home_open_swipe_deck_btn").assertIsDisplayed()
    }

    @Test
    fun calendarScreenSupportsBulkSelectAndPersonalAcademicMarkers() {
        val today = DateUtils.todayKey()
        val stateWithMarker = sampleState.copy(
            personalMarkers = listOf(
                com.example.data.PersonalAcademicMarker(
                    id = "pm1",
                    userId = "u1",
                    dateKey = today,
                    title = "Physics Practical",
                    category = com.example.data.PersonalMarkerCategory.PRACTICAL.raw,
                    note = "Lab 2"
                )
            )
        )
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    CalendarScreen(
                        state = stateWithMarker,
                        onEditDate = {},
                        onDeleteDate = {},
                        onBulkLogAttendance = { _, _, _ -> },
                        onSavePersonalMarker = { _, _, _, _, _ -> },
                        onDeletePersonalMarker = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("calendar_lazy_column")
            .performScrollToNode(hasTestTag("calendar_bulk_select_toggle"))
        composeTestRule.onNodeWithTag("calendar_bulk_select_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("calendar_bulk_action_bar").assertIsDisplayed()

        composeTestRule.onNodeWithTag("calendar_lazy_column")
            .performScrollToNode(hasTestTag("calendar_personal_markers_section"))
        composeTestRule.onNodeWithTag("calendar_personal_markers_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("calendar_personal_marker_pm1").assertIsDisplayed()
    }

    @Test
    fun reportsScreenShowsWrappedParentRegisterToolsAndCohortPulse() {
        val myProf = sampleState.profile!!
        val stateWithCommunity = sampleState.copy(
            communityTrendSummaries = listOf(
                com.example.data.StudentTrendSummary(
                    id = "u1", userId = "u1", totalAttended = 9, totalAbsent = 1,
                    grade = myProf.grade,
                    alStream = myProf.alStream,
                    className = myProf.className
                ),
                com.example.data.StudentTrendSummary(
                    id = "u2", userId = "u2", totalAttended = 8, totalAbsent = 2,
                    grade = myProf.grade,
                    alStream = myProf.alStream,
                    className = "13M9"
                )
            )
        )
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    ReportsScreen(state = stateWithCommunity)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reports_lazy_column")
            .performScrollToNode(hasTestTag("reports_tools_card"))
        composeTestRule.onNodeWithTag("reports_open_wrapped_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reports_open_parent_card_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reports_open_register_verify_btn").assertIsDisplayed()

        composeTestRule.onNodeWithTag("reports_lazy_column")
            .performScrollToNode(hasTestTag("cohort_pulse_section"))
        composeTestRule.onNodeWithTag("cohort_pulse_section").assertIsDisplayed()
    }

    @Test
    fun settingsScreenShowsUnlockPromptDangerNudgeAndRegisterVerify() {
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    SettingsScreen(
                        state = sampleState,
                        isVerifiedAdmin = false,
                        onEditProfile = {},
                        onUpdatePreferences = { _, _, _, _ -> },
                        onPreviewCheckInPopup = {},
                        onManualSync = {},
                        onOpenAdminPanel = {},
                        onSignOut = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_verify_register_button"))
        composeTestRule.onNodeWithTag("settings_verify_register_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_unlock_prompt_switch"))
        composeTestRule.onNodeWithTag("settings_unlock_prompt_switch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_danger_nudge_switch"))
        composeTestRule.onNodeWithTag("settings_danger_nudge_switch").assertIsDisplayed()
    }

    @Test
    fun homeScreenShowsNextDayPollCardWhenEnabledAndOpenAndSupportsVoteSwitching() {
        var latestVote: Boolean? = null
        val pollState = sampleState.copy(
            appSettings = sampleState.appSettings.copy(nextDayPollEnabled = true),
            pollWindow = com.example.data.NextDayPollWindow(
                isOpen = true,
                targetDateKey = "2026-11-23",
                isTomorrowTarget = true,
                isTargetSchoolDay = true
            ),
            nextDayPollSummary = com.example.data.NextDayPollSummary(
                targetDateKey = "2026-11-23",
                totalVotes = 10,
                goingCount = 7,
                notGoingCount = 3,
                goingPercent = 70.0,
                notGoingPercent = 30.0,
                myVoteGoing = true,
                myClassGoingCount = 4,
                myClassTotalCount = 5,
                myClassGoingPercent = 80.0
            )
        )
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    HomeScreen(
                        state = pollState,
                        scopeMode = DashboardScopeMode.CURRENT_TERM,
                        onScopeModeChange = {},
                        onOpenCheckIn = {},
                        onSyncClick = {},
                        onVoteNextDay = { latestVote = it }
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_next_day_poll_card"))
        composeTestRule.onNodeWithTag("home_next_day_poll_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("poll_live_analysis_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("poll_vote_no_btn").performClick()
        org.junit.Assert.assertEquals(false, latestVote)
        composeTestRule.onNodeWithTag("poll_vote_yes_btn").performClick()
        org.junit.Assert.assertEquals(true, latestVote)
    }

    @Test
    fun homeScreenHidesNextDayPollCardWhenAdminToggleIsOff() {
        val pollDisabledState = sampleState.copy(
            appSettings = sampleState.appSettings.copy(nextDayPollEnabled = false),
            pollWindow = com.example.data.NextDayPollWindow(
                isOpen = true,
                targetDateKey = "2026-11-23",
                isTomorrowTarget = true,
                isTargetSchoolDay = true
            )
        )
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    HomeScreen(
                        state = pollDisabledState,
                        scopeMode = DashboardScopeMode.CURRENT_TERM,
                        onScopeModeChange = {},
                        onOpenCheckIn = {},
                        onSyncClick = {}
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_next_day_poll_card").assertDoesNotExist()
    }

    @Test
    fun homeScreenPrivacyBlurMasksPercentAndAbsentCountAndOpensParentSummaryCard() {
        val termStats = com.example.data.AttendanceStats(
            percentage = 80.0, eligible = true, thresholdPercent = 80,
            presentDays = 8, absentDays = 2, medicalDays = 0, schoolEventDays = 0, holidayDays = 0,
            priorPresentDays = 0, priorAbsentDays = 0,
            effectiveAttended = 8, totalCountedDays = 10, safeSkipsRemaining = 0, daysNeededToRecover = 0
        )
        val overallStats = com.example.data.AttendanceStats(
            percentage = 86.0, eligible = true, thresholdPercent = 80,
            presentDays = 43, absentDays = 7, medicalDays = 0, schoolEventDays = 0, holidayDays = 0,
            priorPresentDays = 0, priorAbsentDays = 0,
            effectiveAttended = 43, totalCountedDays = 50, safeSkipsRemaining = 3, daysNeededToRecover = 0
        )
        val todayKey = DateUtils.todayKey()
        val privateNoteText = "Private doctor visit note"
        val todayPrivateRecord = AttendanceRecord(
            dateKey = todayKey,
            status = AttendanceStatus.ABSENT.raw,
            note = privateNoteText
        )
        val homeState = sampleState.copy(
            currentTermStats = termStats,
            overallStats = overallStats,
            attendanceRecords = listOf(todayPrivateRecord),
            todayRecord = todayPrivateRecord
        )
        composeTestRule.setContent {
            EightyEndanceTheme {
                LiquidGlassBackdropHost {
                    HomeScreen(
                        state = homeState,
                        scopeMode = DashboardScopeMode.CURRENT_TERM,
                        onScopeModeChange = {},
                        onOpenCheckIn = {},
                        onSyncClick = {}
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // Unblurred state shows actual attended/counted days and absent count
        composeTestRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("privacy_blur_toggle"))
        composeTestRule.onNodeWithTag("gauge_counts_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("8/10d • 2 absent").assertIsDisplayed()

        // Toggle Privacy Mode ON -> masks %, absent count, and status pill
        composeTestRule.onNodeWithTag("privacy_blur_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("••.•%").assertIsDisplayed()
        composeTestRule.onNodeWithText("•• / •• days • •• absent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hidden").assertIsDisplayed()

        // Toggle Privacy Mode OFF -> reveals actual counts again
        composeTestRule.onNodeWithTag("privacy_blur_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("8/10d • 2 absent").assertIsDisplayed()

        // 1-tap Parent Card button opens Parent-Friendly Clean Summary Card
        composeTestRule.onNodeWithTag("home_parent_summary_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_parent_summary_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("parent_summary_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_summary_term_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_summary_overall_chip").assertIsDisplayed()
        composeTestRule.onNodeWithText("80% Eligible").assertIsDisplayed()
        composeTestRule.onNodeWithText("8 of 10 school days counted").assertIsDisplayed()
        composeTestRule.onNodeWithText(privateNoteText, substring = true).assertDoesNotExist()

        // Switch scope inside Parent Summary Card to Overall G12 & 13
        composeTestRule.onNodeWithTag("parent_summary_overall_chip").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("43 of 50 school days counted").assertIsDisplayed()

        // Scroll to the Share button at the bottom of the dialog
        composeTestRule.onNodeWithTag("share_parent_summary_btn")
            .performScrollTo()
            .assertIsDisplayed()
    }
}





