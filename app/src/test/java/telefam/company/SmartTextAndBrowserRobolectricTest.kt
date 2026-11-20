package telefam.company

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import telefam.company.core.SmartEntity
import telefam.company.core.SmartTextDetector
import telefam.company.data.local.CachedLinkPreviewEntity
import telefam.company.data.local.SavedLinkEntity
import telefam.company.data.local.TelefamDatabase
import telefam.company.ui.components.LinkPreviewCard
import telefam.company.ui.components.LocalSmartTextActions
import telefam.company.ui.components.SmartEmailActionSheet
import telefam.company.ui.components.SmartInteractiveText
import telefam.company.ui.components.SmartPhoneActionSheet
import telefam.company.ui.components.SmartTextActions
import telefam.company.ui.screens.SavedLinksListPane
import telefam.company.ui.screens.TelefamBrowserReconnectAnimation
import telefam.company.ui.theme.TelefamTheme
import telefam.company.ui.theme.ThemeMode
import telefam.company.ui.viewmodel.LinkPreviewUiState

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h915dp")
class SmartTextAndBrowserRobolectricTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var db: TelefamDatabase

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, TelefamDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    shadowOf(Looper.getMainLooper()).idle()
    db.close()
  }

  @Test
  fun `url detector finds explicit and schemeless links, trims punctuation, preserves parens, and blocks unsafe schemes`() {
    val msg = "Check google.com, youtube.com/watch?v=1, https://t.me/telefam_app! Also see " +
      "https://en.wikipedia.org/wiki/Kotlin_(programming_language). Ignore photo.jpg and v2.0."
    val analysis = SmartTextDetector.analyze(msg)

    assertEquals(
      listOf(
        "https://google.com",
        "https://youtube.com/watch?v=1",
        "https://t.me/telefam_app",
        "https://en.wikipedia.org/wiki/Kotlin_(programming_language)",
      ),
      analysis.urls.map { it.normalizedUrl },
    )

    assertNull(SmartTextDetector.normalizeSafeHttpUrl("javascript:alert(1)"))
    assertNull(SmartTextDetector.normalizeSafeHttpUrl("file:///etc/passwd"))
    assertNull(SmartTextDetector.normalizeSafeHttpUrl("data:text/html,<script>alert(1)</script>"))
    assertNull(SmartTextDetector.normalizeSafeHttpUrl("vbscript:msgbox(1)"))
    assertNull(SmartTextDetector.normalizeSafeHttpUrl("intent://scan/#Intent;end"))
    assertNull(SmartTextDetector.normalizeSafeHttpUrl("http://127.0.0.1:8080/admin"))
    assertNull(SmartTextDetector.normalizeSafeHttpUrl("http://169.254.169.254/latest/meta-data"))
    assertNull(SmartTextDetector.normalizeSafeHttpUrl("http://localhost:3000"))

    val unsafeText = "Do not open javascript:evil.com/x or file:///sdcard/secret.txt"
    assertTrue(SmartTextDetector.analyze(unsafeText).urls.isEmpty())
  }

  @Test
  fun `email detection takes precedence over domain urls and rejects malformed addresses`() {
    val analysis = SmartTextDetector.analyze("Reach support@telefam.company or ops.team@sub.domain.co.ke, not invalid@@bad")
    val emails = analysis.entities.filterIsInstance<SmartEntity.Email>()
    assertEquals(listOf("support@telefam.company", "ops.team@sub.domain.co.ke"), emails.map { it.emailAddress })
    assertTrue(analysis.urls.isEmpty())
  }

  @Test
  fun `phone detection formats Kenyan and international numbers without mutating source text`() {
    val source = "Call 0712345678 or 0112345678 or +254722111222 or +1 (650) 253-0000."
    val analysis = SmartTextDetector.analyze(source)
    val phones = analysis.entities.filterIsInstance<SmartEntity.PhoneNumber>()

    assertEquals(4, phones.size)
    assertEquals("0712 345 678", phones[0].formattedDisplay)
    assertEquals("0112 345 678", phones[1].formattedDisplay)
    assertEquals("+254 722 111 222", phones[2].formattedDisplay)
    assertEquals("+1 650 253 0000", phones[3].formattedDisplay)
    assertEquals(source, analysis.text)

    // Must not mistake prices or dates for phones
    val noise = SmartTextDetector.analyze("Total KES 150000 on 12/05/2026 and $1200000.")
    assertTrue(noise.entities.filterIsInstance<SmartEntity.PhoneNumber>().isEmpty())
  }

  @Test
  fun `otp detector extracts context-aware codes independently and ignores prices and dates`() {
    val single1 = SmartTextDetector.analyze("Your verification code is 482913")
    val single2 = SmartTextDetector.analyze("OTP: 826401")
    val single3 = SmartTextDetector.analyze("Use 543218 to verify your account")
    val single4 = SmartTextDetector.analyze("Your login code is 123456")

    assertEquals(listOf("482913"), single1.otpCodes.map { it.code })
    assertEquals(listOf("826401"), single2.otpCodes.map { it.code })
    assertEquals(listOf("543218"), single3.otpCodes.map { it.code })
    assertEquals(listOf("123456"), single4.otpCodes.map { it.code })

    val multi = SmartTextDetector.analyze("Login code: 482913 and backup code is 991244. Paid KES 4500 on 2026.")
    assertEquals(listOf("482913", "991244"), multi.otpCodes.map { it.code })
  }

  @Test
  fun `room caches link previews and enforces user-scoped saved links with logout wipe`() = runTest {
    val dao = db.telefamDao()
    dao.upsertCachedLinkPreview(
      CachedLinkPreviewEntity(
        normalizedUrl = "https://telefam.company/guide",
        title = "Telefam Guide",
        description = "Family hub",
        imageUrl = "https://telefam.company/og.png",
        faviconUrl = "https://telefam.company/favicon.ico",
        domain = "telefam.company",
        status = "ok",
        fetchedAtMs = 10_000L,
      ),
    )
    assertEquals("Telefam Guide", dao.getCachedLinkPreview("https://telefam.company/guide")?.title)

    dao.upsertSavedLink(
      SavedLinkEntity(
        id = "sl_1",
        userId = "user_a",
        url = "https://telefam.company/guide",
        normalizedUrl = "https://telefam.company/guide",
        title = "Telefam Guide",
        description = "Family hub",
        imageUrl = "",
        faviconUrl = "",
        domain = "telefam.company",
        source = "chat",
        savedAtMs = 20_000L,
      ),
    )
    dao.upsertSavedLink(
      SavedLinkEntity(
        id = "sl_2",
        userId = "user_b",
        url = "https://kotlinlang.org",
        normalizedUrl = "https://kotlinlang.org",
        title = "Kotlin",
        description = "",
        imageUrl = "",
        faviconUrl = "",
        domain = "kotlinlang.org",
        source = "feed",
        savedAtMs = 21_000L,
      ),
    )

    val userALinks = dao.observeSavedLinks("user_a", "").first()
    assertEquals(1, userALinks.size)
    assertEquals("sl_1", userALinks.first().id)

    dao.clearAllUserScopedData()
    assertTrue(dao.getSavedLinksSnapshot("user_a").isEmpty())
    assertTrue(dao.getSavedLinksSnapshot("user_b").isEmpty())
  }

  @Test
  fun `smart interactive text renders independent otp copy pills with copied feedback`() {
    val copied = mutableListOf<String>()
    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.LIGHT) {
        CompositionLocalProvider(
          LocalSmartTextActions provides SmartTextActions(
            onOtpCopy = { copied += it },
          ),
        ) {
          SmartInteractiveText(
            text = "Login code: 482913 and backup code: 778899",
            source = "chat",
          )
        }
      }
    }

    composeRule.onNodeWithTag("otp_copy_pill_482913").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("otp_copy_pill_778899").assertIsDisplayed().performClick()
    composeRule.waitForIdle()

    assertEquals(listOf("482913", "778899"), copied)
    composeRule.onNodeWithText("Copied").assertIsDisplayed()
  }

  @Test
  fun `phone and email action sheets show all native actions`() {
    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.LIGHT) {
        SmartPhoneActionSheet(
          phone = SmartEntity.PhoneNumber(0, 10, "0712345678", "0712345678", "0712 345 678"),
          onDismiss = {},
          onCopied = {},
        )
      }
    }
    composeRule.onNodeWithText("0712 345 678").assertIsDisplayed()
    composeRule.onNodeWithTag("smart_phone_action_call").assertIsDisplayed()
    composeRule.onNodeWithTag("smart_phone_action_copy").assertIsDisplayed()
    composeRule.onNodeWithTag("smart_phone_action_save_contact").assertIsDisplayed()
    composeRule.onNodeWithTag("smart_phone_action_sms").assertIsDisplayed()
  }

  @Test
  fun `email action sheet renders send and copy actions`() {
    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.LIGHT) {
        SmartEmailActionSheet(
          email = SmartEntity.Email(0, 20, "hello@telefam.company", "hello@telefam.company"),
          onDismiss = {},
          onCopied = {},
        )
      }
    }
    composeRule.onNodeWithText("hello@telefam.company").assertIsDisplayed()
    composeRule.onNodeWithTag("smart_email_action_send").assertIsDisplayed()
    composeRule.onNodeWithTag("smart_email_action_copy").assertIsDisplayed()
  }

  @Test
  fun `link preview card renders skeleton, loaded card, and fallback states`() {
    val loadedUrl = SmartEntity.Url(0, 20, "kotlinlang.org", "https://kotlinlang.org", "kotlinlang.org")
    val fallbackUrl = SmartEntity.Url(0, 18, "example.org", "https://example.org", "example.org")
    val loadingUrl = SmartEntity.Url(0, 18, "pending.org", "https://pending.org", "pending.org")

    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.LIGHT) {
        CompositionLocalProvider(
          LocalSmartTextActions provides SmartTextActions(
            linkPreviews = mapOf(
              loadedUrl.normalizedUrl to LinkPreviewUiState.Loaded(
                CachedLinkPreviewEntity(
                  normalizedUrl = loadedUrl.normalizedUrl,
                  title = "Kotlin Programming Language",
                  description = "Concise multiplatform code",
                  imageUrl = "",
                  faviconUrl = "",
                  domain = "kotlinlang.org",
                  status = "ok",
                  fetchedAtMs = 1_000L,
                ),
              ),
              fallbackUrl.normalizedUrl to LinkPreviewUiState.Loaded(
                CachedLinkPreviewEntity(
                  normalizedUrl = fallbackUrl.normalizedUrl,
                  title = "example.org",
                  description = "",
                  imageUrl = "",
                  faviconUrl = "",
                  domain = "example.org",
                  status = "fallback",
                  fetchedAtMs = 1_000L,
                ),
              ),
              loadingUrl.normalizedUrl to LinkPreviewUiState.Loading(
                normalizedUrl = loadingUrl.normalizedUrl,
                domain = loadingUrl.domain,
              ),
            ),
          ),
        ) {
          androidx.compose.foundation.layout.Column {
            LinkPreviewCard(url = loadedUrl, source = "chat")
            LinkPreviewCard(url = fallbackUrl, source = "chat")
            LinkPreviewCard(url = loadingUrl, source = "chat")
            TelefamBrowserReconnectAnimation()
          }
        }
      }
    }

    composeRule.onNodeWithTag("link_preview_card_kotlinlang.org").assertIsDisplayed()
    composeRule.onNodeWithText("Kotlin Programming Language").assertIsDisplayed()
    composeRule.onNodeWithTag("link_preview_fallback_example.org").assertIsDisplayed()
    composeRule.onNodeWithTag("link_preview_skeleton_pending.org").assertIsDisplayed()
    composeRule.onNodeWithTag("telefam_browser_loading_anim").assertIsDisplayed()
  }

  @Test
  fun `saved links pane displays saved link and forwards search, open, and delete actions`() {
    var typedQuery = ""
    var openedId: String? = null
    var deletedId: String? = null
    val sample = SavedLinkEntity(
      id = "saved_99",
      userId = "u1",
      url = "https://youtube.com",
      normalizedUrl = "https://youtube.com",
      title = "YouTube Video",
      description = "",
      imageUrl = "",
      faviconUrl = "",
      domain = "youtube.com",
      source = "feed",
      savedAtMs = 1_700_000_000_000L,
    )

    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.LIGHT) {
        SavedLinksListPane(
          links = listOf(sample),
          searchQuery = "",
          onSearchQueryChanged = { typedQuery = it },
          onOpenLink = { openedId = it.id },
          onDeleteLink = { deletedId = it },
        )
      }
    }

    composeRule.onNodeWithTag("saved_link_card_saved_99").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("saved_link_delete_saved_99").performClick()
    composeRule.onNodeWithTag("saved_links_search_input").performTextInput("tube")
    composeRule.waitForIdle()

    assertEquals("saved_99", openedId)
    assertEquals("saved_99", deletedId)
    assertEquals("tube", typedQuery)
  }
}
