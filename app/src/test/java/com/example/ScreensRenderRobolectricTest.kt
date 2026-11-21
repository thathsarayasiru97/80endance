package com.example

import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStatus
import com.example.data.CalendarEvent
import com.example.data.CalendarEventType
import com.example.data.UserProfile
import com.example.system.AppUpdateChecker
import com.example.system.AppUpdateInfo
import com.example.ui.admin.AdminNoSchoolCard
import com.example.ui.calendar.CalendarScreen
import com.example.ui.components.DailyCheckInDialog
import com.example.ui.components.LiquidGlassBackdropHost
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
}
