package telefam.company.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.telefam.company.app.R
import telefam.company.core.SmartTextDetector
import telefam.company.ui.theme.TelefamCrimson
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TelefamInAppBrowserScreen(
  initialUrl: String,
  titleHint: String,
  isSaved: Boolean,
  savedUrls: Set<String> = emptySet(),
  onToggleSave: (currentUrl: String, currentTitle: String) -> Unit,
  onClose: () -> Unit,
) {
  val context = LocalContext.current
  val safeInitialUrl = remember(initialUrl) {
    SmartTextDetector.normalizeSafeHttpUrl(initialUrl) ?: "about:blank"
  }

  var currentUrl by remember(safeInitialUrl) { mutableStateOf(safeInitialUrl) }
  var pageTitle by remember(safeInitialUrl) { mutableStateOf(titleHint.ifBlank { SmartTextDetector.extractCleanDomain(safeInitialUrl) }) }
  var progress by remember { mutableIntStateOf(0) }
  var isLoading by remember { mutableStateOf(true) }
  var isRefreshing by remember { mutableStateOf(false) }
  var canGoBack by remember { mutableStateOf(false) }
  var canGoForward by remember { mutableStateOf(false) }
  var hasError by remember { mutableStateOf(false) }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  val isHttps = currentUrl.startsWith("https://", ignoreCase = true)
  val effectiveIsSaved = remember(isSaved, currentUrl, savedUrls) {
    val norm = SmartTextDetector.normalizeSafeHttpUrl(currentUrl)
    if (savedUrls.isNotEmpty() && norm != null) norm in savedUrls else isSaved
  }
  val displayDomain = remember(currentUrl) {
    SmartTextDetector.extractCleanDomain(currentUrl).ifBlank { currentUrl }
  }

  // Android system back button steps back through WebView history before closing the browser
  BackHandler(enabled = true) {
    val wv = webViewRef
    if (wv != null && wv.canGoBack()) {
      wv.goBack()
    } else {
      onClose()
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      webViewRef?.apply {
        stopLoading()
        destroy()
      }
      webViewRef = null
    }
  }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier
      .fillMaxSize()
      .testTag("telefam_in_app_browser"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(),
    ) {
      // Top Modern Browser Toolbar
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 4.dp, vertical = 6.dp),
          ) {
            IconButton(
              onClick = onClose,
              modifier = Modifier
                .size(48.dp)
                .testTag("browser_close_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.browser_close),
              )
            }

            // Title + HTTPS domain pill
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              ) {
                Icon(
                  imageVector = if (isHttps) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                  contentDescription = if (isHttps) stringResource(R.string.browser_secure_https) else displayDomain,
                  tint = if (isHttps) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = pageTitle.ifBlank { displayDomain },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = displayDomain,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }

            IconButton(
              onClick = { onToggleSave(currentUrl, pageTitle) },
              modifier = Modifier
                .size(48.dp)
                .testTag("browser_save_button"),
            ) {
              Icon(
                imageVector = if (effectiveIsSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                contentDescription = if (effectiveIsSaved) stringResource(R.string.browser_saved) else stringResource(R.string.browser_save_link),
                tint = if (effectiveIsSaved) TelefamCrimson else MaterialTheme.colorScheme.onSurface,
              )
            }

            IconButton(
              onClick = {
                val safeExternal = SmartTextDetector.normalizeSafeHttpUrl(currentUrl)
                if (safeExternal != null) {
                  runCatching {
                    context.startActivity(
                      Intent(Intent.ACTION_VIEW, Uri.parse(safeExternal))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                  }
                }
              },
              modifier = Modifier
                .size(48.dp)
                .testTag("browser_external_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = stringResource(R.string.browser_open_external),
              )
            }
          }

          // Animated loading progress bar
          if (isLoading && progress in 1..99) {
            LinearProgressIndicator(
              progress = { progress / 100f },
              color = TelefamCrimson,
              trackColor = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .testTag("browser_progress_bar"),
            )
          } else if (isLoading) {
            LinearProgressIndicator(
              color = TelefamCrimson,
              trackColor = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .testTag("browser_progress_bar"),
            )
          } else {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          }
        }
      }

      // Main WebView / Loading Overlay / Error Surface with Pull-To-Refresh
      PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
          isRefreshing = true
          hasError = false
          webViewRef?.reload()
        },
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          if (!hasError && safeInitialUrl != "about:blank") {
            AndroidView(
              factory = { ctx ->
                WebView(ctx).apply {
                  settings.javaScriptEnabled = true
                  settings.domStorageEnabled = true
                  settings.allowFileAccess = false
                  settings.allowContentAccess = false
                  settings.setSupportMultipleWindows(false)
                  settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                  settings.loadWithOverviewMode = true
                  settings.useWideViewPort = true
                  settings.builtInZoomControls = true
                  settings.displayZoomControls = false

                  webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                      progress = newProgress
                      isLoading = newProgress < 100
                      if (newProgress >= 100) {
                        isRefreshing = false
                      }
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                      if (!title.isNullOrBlank()) {
                        pageTitle = title.trim()
                      }
                    }
                  }

                  webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                      view: WebView?,
                      request: WebResourceRequest?,
                    ): Boolean {
                      val next = request?.url?.toString().orEmpty()
                      val validated = SmartTextDetector.normalizeSafeHttpUrl(next)
                      if (validated == null) {
                        return true // Block javascript:, file:, intent:, private IPs
                      }
                      currentUrl = validated
                      return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                      hasError = false
                      isLoading = true
                      if (!url.isNullOrBlank()) {
                        SmartTextDetector.normalizeSafeHttpUrl(url)?.let { currentUrl = it }
                      }
                      canGoBack = view?.canGoBack() == true
                      canGoForward = view?.canGoForward() == true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                      isLoading = false
                      isRefreshing = false
                      if (!url.isNullOrBlank()) {
                        SmartTextDetector.normalizeSafeHttpUrl(url)?.let { currentUrl = it }
                      }
                      val t = view?.title?.trim().orEmpty()
                      if (t.isNotBlank()) pageTitle = t
                      canGoBack = view?.canGoBack() == true
                      canGoForward = view?.canGoForward() == true
                    }

                    override fun onReceivedError(
                      view: WebView?,
                      request: WebResourceRequest?,
                      error: WebResourceError?,
                    ) {
                      if (request?.isForMainFrame == true) {
                        isLoading = false
                        isRefreshing = false
                        hasError = true
                      }
                    }
                  }
                  loadUrl(safeInitialUrl)
                  webViewRef = this
                }
              },
              update = { view ->
                webViewRef = view
                canGoBack = view.canGoBack()
                canGoForward = view.canGoForward()
              },
              modifier = Modifier.fillMaxSize(),
            )
          }

          // Error view with Retry
          if (hasError) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .testTag("browser_error_view"),
            ) {
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.widthIn(max = 420.dp),
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.padding(24.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = stringResource(R.string.browser_error_title),
                    tint = TelefamCrimson,
                    modifier = Modifier.size(44.dp),
                  )
                  Spacer(Modifier.height(12.dp))
                  Text(
                    text = stringResource(R.string.browser_error_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Spacer(Modifier.height(4.dp))
                  Text(
                    text = displayDomain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Spacer(Modifier.height(18.dp))
                  Button(
                    onClick = {
                      hasError = false
                      isLoading = true
                      webViewRef?.reload()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson),
                    modifier = Modifier.testTag("browser_retry_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Refresh,
                      contentDescription = stringResource(R.string.browser_retry),
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.browser_retry))
                  }
                }
              }
            }
          }

          // Kinetic breaking-and-reconnecting "Telefam Browser" loading badge
          Box(
            modifier = Modifier
              .align(Alignment.TopCenter)
              .padding(top = 18.dp),
          ) {
            androidx.compose.animation.AnimatedVisibility(
              visible = isLoading && !hasError && progress < 75,
              enter = fadeIn(),
              exit = fadeOut(),
            ) {
              TelefamBrowserReconnectAnimation()
            }
          }
        }
      }

      // Bottom Navigation Controls Bar (Back / Forward / Reload)
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
          ) {
            IconButton(
              onClick = { webViewRef?.goBack() },
              enabled = canGoBack,
              modifier = Modifier
                .size(48.dp)
                .testTag("browser_back_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.browser_back),
              )
            }
            IconButton(
              onClick = { webViewRef?.goForward() },
              enabled = canGoForward,
              modifier = Modifier
                .size(48.dp)
                .testTag("browser_forward_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = stringResource(R.string.browser_forward),
              )
            }
            IconButton(
              onClick = {
                if (isLoading) {
                  webViewRef?.stopLoading()
                  isLoading = false
                } else {
                  hasError = false
                  isLoading = true
                  webViewRef?.reload()
                }
              },
              modifier = Modifier
                .size(48.dp)
                .testTag("browser_reload_button"),
            ) {
              Icon(
                imageVector = if (isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.browser_reload),
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Animated "Telefam Browser" kinetic loader where letters/words break apart and smoothly reconnect.
 */
@Composable
fun TelefamBrowserReconnectAnimation(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "telefam_browser_reconnect")
  val phase by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "break_phase",
  )

  // At phase=0 -> tightly connected; at phase=1 -> broken apart; Smooth reverse reconnects
  val telefamLetters = listOf("T", "e", "l", "e", "f", "a", "m")
  val browserLetters = listOf("B", "r", "o", "w", "s", "e", "r")

  Surface(
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
    shadowElevation = 6.dp,
    border = BorderStroke(1.dp, TelefamCrimson.copy(alpha = 0.35f)),
    modifier = modifier.testTag("telefam_browser_loading_anim"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = TelefamCrimson.copy(alpha = 0.14f),
        modifier = Modifier.size(28.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.Public,
            contentDescription = stringResource(R.string.browser_brand_title),
            tint = TelefamCrimson,
            modifier = Modifier.size(16.dp),
          )
        }
      }

      Spacer(Modifier.width((6 + phase * 10).dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        telefamLetters.forEachIndexed { idx, ch ->
          val centerOffset = idx - 3f
          val dx = centerOffset * phase * 2.2f
          val dy = sin(idx * 1.1 + phase * 3.14).toFloat() * phase * 4f
          Text(
            text = ch,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TelefamCrimson,
            modifier = Modifier
              .offset(x = dx.dp, y = dy.dp)
              .alpha(1f - phase * 0.25f),
          )
        }
      }

      Spacer(Modifier.width((6 + phase * 14).dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        browserLetters.forEachIndexed { idx, ch ->
          val centerOffset = idx - 3f
          val dx = centerOffset * phase * 2.2f
          val dy = -sin(idx * 1.1 + phase * 3.14).toFloat() * phase * 4f
          Text(
            text = ch,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
              .offset(x = dx.dp, y = dy.dp)
              .alpha(1f - phase * 0.25f),
          )
        }
      }
    }
  }
}
