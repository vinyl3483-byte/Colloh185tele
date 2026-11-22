package telefam.company

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import telefam.company.ui.screens.TelefamProfileMenuScreen
import telefam.company.ui.theme.TelefamTheme
import telefam.company.ui.theme.ThemeMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h915dp")
class ProfileMenuRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun profileMenuRendersAllEightFeaturesAndGetVerifiedCtaAsComingSoon() {
    val clickedFeatures = mutableListOf<String>()
    var backClicks = 0
    var starsClicks = 0

    composeTestRule.setContent {
      TelefamTheme(themeMode = ThemeMode.LIGHT) {
        TelefamProfileMenuScreen(
          onBack = { backClicks++ },
          onFeatureComingSoon = { feature -> clickedFeatures.add(feature) },
          onOpenStars = { starsClicks++ },
        )
      }
    }

    composeTestRule.onNodeWithTag("profile_menu_screen").assertIsDisplayed()
    composeTestRule.onNodeWithText("Menu").assertIsDisplayed()

    composeTestRule.onNodeWithTag("profile_menu_item_stars")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    assertEquals(1, starsClicks)

    val comingSoonEntries = listOf(
      "dashboard" to "Dashboard",
      "subscription" to "Subscription",
      "monetization" to "Monetization",
      "wallet" to "Wallet",
      "linked_device" to "Linked device",
      "create_campaign" to "Create campaign",
      "get_verified" to "Get Verified",
    )

    comingSoonEntries.forEach { (id, title) ->
      composeTestRule.onNodeWithTag("profile_menu_item_$id")
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
      assertTrue("Expected $title in clicked coming-soon list", clickedFeatures.contains(title))
    }

    composeTestRule.onNodeWithTag("profile_menu_get_verified_cta")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()

    assertEquals(8, clickedFeatures.size)
    assertEquals("Get Verified", clickedFeatures.last())

    composeTestRule.onNodeWithTag("profile_menu_back_button").performClick()
    assertEquals(1, backClicks)
  }

  @Test
  @Config(qualifiers = "w840dp-h1080dp")
  fun profileMenuAdaptsCleanlyOnTabletAndFoldableWidthsInDarkMode() {
    composeTestRule.setContent {
      TelefamTheme(themeMode = ThemeMode.DARK) {
        TelefamProfileMenuScreen(
          onBack = {},
          onFeatureComingSoon = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("profile_menu_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_menu_item_dashboard").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_menu_item_get_verified").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_menu_get_verified_cta").assertIsDisplayed()
  }
}
