package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.ui.screens.HeaderSection
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class HeaderSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun headerSection_showsConsistencyQuote_whenProgressNot100Percent() {
        composeTestRule.setContent {
            MyApplicationTheme {
                HeaderSection(
                    completionPercent = 0.5f,
                    completedCount = 1,
                    totalHabits = 2,
                    selectedDate = "2026-08-15"
                )
            }
        }

        composeTestRule.onNodeWithText("Consistency beats intensity.", substring = true).assertIsDisplayed()
    }

    @Test
    fun headerSection_changesQuoteToClickMe_whenProgressIs100Percent() {
        composeTestRule.setContent {
            MyApplicationTheme {
                HeaderSection(
                    completionPercent = 1.0f,
                    completedCount = 2,
                    totalHabits = 2,
                    selectedDate = "2026-08-15"
                )
            }
        }

        composeTestRule.onNodeWithTag("hold_me_dialog_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("Click me!", substring = true).assertIsDisplayed()
    }
}
