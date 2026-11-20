package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.UserProfile
import com.example.ui.calendar.CalendarScreen
import com.example.ui.components.DailyCheckInDialog
import com.example.ui.components.LiquidGlassBackdropHost
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
}
