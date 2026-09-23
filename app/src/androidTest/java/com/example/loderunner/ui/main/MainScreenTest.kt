package com.example.loderunner.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.loderunner.MainNavigation
import com.example.loderunner.theme.LodeRunnerTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI test for Lode Runner Main Navigation. */
class MainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        composeTestRule.setContent {
            LodeRunnerTheme {
                MainNavigation()
            }
        }
    }

    @Test
    fun titleAndButtonsExist() {
        composeTestRule.onNodeWithText("★ LODE RUNNER ★").assertIsDisplayed()
        composeTestRule.onNodeWithText("START GAME (LEVEL 1)").assertIsDisplayed()
    }
}
