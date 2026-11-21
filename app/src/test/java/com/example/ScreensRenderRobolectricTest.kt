package com.example

import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.AcademicTerm
import com.example.data.AttendanceCalculator
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStatus
import com.example.data.BroadcastNotification
import com.example.data.CalendarEvent
import com.example.data.CalendarEventType
import com.example.data.DateUtils
import com.example.data.FeedbackType
import com.example.data.Notice
import com.example.data.NoticeCategory
import com.example.data.UserProfile
import com.example.system.AppUpdateChecker
import com.example.system.AppUpdateInfo
import com.example.system.UpdateCheckStatusSummary
import com.example.ui.admin.AdminBroadcastTab
import com.example.ui.admin.AdminNoSchoolCard
import com.example.ui.admin.AdminTermRolloverCard
import com.example.ui.calendar.CalendarScreen
import com.example.ui.components.AttendanceBaselineDialog
import com.example.ui.components.DailyCheckInDialog
import com.example.ui.components.FeedbackReportDialog
import com.example.ui.components.LiquidGlassBackdropHost
import com.example.ui.components.PinnedNoticeSlideshow
import com.example.ui.components.ProfilePhotoDialog
import com.example.ui.components.UpdateAvailableDialog
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
                        onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
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
}
