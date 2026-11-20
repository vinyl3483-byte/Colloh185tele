package telefam.company.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.telefam.company.app.R
import kotlinx.coroutines.delay
import telefam.company.core.SmartEntity
import telefam.company.core.SmartTextDetector
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.viewmodel.LinkPreviewUiState

data class SmartTextActions(
  val onOpenUrl: (url: String, source: String) -> Unit = { _, _ -> },
  val onPhoneClick: (SmartEntity.PhoneNumber) -> Unit = {},
  val onEmailClick: (SmartEntity.Email) -> Unit = {},
  val onOtpCopy: (code: String) -> Unit = {},
  val onRequestLinkPreview: (url: String) -> Unit = {},
  val onToggleSaveLink: (url: String, source: String) -> Unit = { _, _ -> },
  val linkPreviews: Map<String, LinkPreviewUiState> = emptyMap(),
  val savedLinkUrls: Set<String> = emptySet(),
)

val LocalSmartTextActions = staticCompositionLocalOf { SmartTextActions() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartInteractiveText(
  text: String,
  source: String,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.bodyMedium,
  color: Color = MaterialTheme.colorScheme.onSurface,
  linkColor: Color = TelefamCrimson,
  otpHighlightBg: Color = linkColor.copy(alpha = 0.14f),
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
  textAlign: TextAlign? = null,
  showOtpPills: Boolean = true,
  onTextLayout: (androidx.compose.ui.text.TextLayoutResult) -> Unit = {},
  onPlainClickFallback: (() -> Unit)? = null,
) {
  val actions = LocalSmartTextActions.current
  val analysis = remember(text) { SmartTextDetector.analyze(text) }

  val annotated = remember(analysis, linkColor, otpHighlightBg) {
    buildAnnotatedString {
      var cursor = 0
      for (entity in analysis.entities) {
        if (entity.start > cursor) {
          append(text.substring(cursor, entity.start))
        }
        val linkStyles = TextLinkStyles(
          style = when (entity) {
            is SmartEntity.Url, is SmartEntity.Email -> SpanStyle(
              color = linkColor,
              fontWeight = FontWeight.Medium,
              textDecoration = TextDecoration.Underline,
            )
            is SmartEntity.PhoneNumber -> SpanStyle(
              color = linkColor,
              fontWeight = FontWeight.Medium,
              textDecoration = TextDecoration.Underline,
            )
            is SmartEntity.OtpCode -> SpanStyle(
              color = linkColor,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              background = otpHighlightBg,
            )
          },
        )
        val tag = when (entity) {
          is SmartEntity.Url -> "url:${entity.normalizedUrl}"
          is SmartEntity.Email -> "email:${entity.emailAddress}"
          is SmartEntity.PhoneNumber -> "phone:${entity.dialNumber}"
          is SmartEntity.OtpCode -> "otp:${entity.code}"
        }
        val displaySegment = when (entity) {
          is SmartEntity.PhoneNumber -> entity.formattedDisplay
          else -> text.substring(entity.start, entity.end)
        }
        val startOffset = length
        append(displaySegment)
        val endOffset = length
        addLink(
          clickable = LinkAnnotation.Clickable(
            tag = tag,
            styles = linkStyles,
            linkInteractionListener = {
              when (entity) {
                is SmartEntity.Url -> actions.onOpenUrl(entity.normalizedUrl, source)
                is SmartEntity.Email -> actions.onEmailClick(entity)
                is SmartEntity.PhoneNumber -> actions.onPhoneClick(entity)
                is SmartEntity.OtpCode -> actions.onOtpCopy(entity.code)
              }
            },
          ),
          start = startOffset,
          end = endOffset,
        )
        cursor = entity.end
      }
      if (cursor < text.length) {
        append(text.substring(cursor))
      }
    }
  }

  Column(modifier = modifier) {
    Text(
      text = annotated,
      style = style,
      color = color,
      maxLines = maxLines,
      overflow = overflow,
      textAlign = textAlign,
      onTextLayout = onTextLayout,
      modifier = if (onPlainClickFallback != null && analysis.entities.isEmpty()) {
        Modifier.clickable(onClick = onPlainClickFallback)
      } else {
        Modifier
      },
    )

    if (showOtpPills && analysis.otpCodes.isNotEmpty()) {
      var copiedCode by remember(text) { mutableStateOf<String?>(null) }
      LaunchedEffect(copiedCode) {
        if (copiedCode != null) {
          delay(1_600L)
          copiedCode = null
        }
      }
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 6.dp),
      ) {
        analysis.otpCodes.forEachIndexed { idx, otp ->
          val isCopied = copiedCode == "${idx}_${otp.code}"
          Surface(
            onClick = {
              actions.onOtpCopy(otp.code)
              copiedCode = "${idx}_${otp.code}"
            },
            shape = RoundedCornerShape(10.dp),
            color = if (isCopied) Color(0xFF16A34A).copy(alpha = 0.14f) else linkColor.copy(alpha = 0.12f),
            border = BorderStroke(
              width = 1.dp,
              color = if (isCopied) Color(0xFF16A34A).copy(alpha = 0.45f) else linkColor.copy(alpha = 0.35f),
            ),
            modifier = Modifier
              .defaultMinSize(minHeight = 40.dp)
              .testTag("otp_copy_pill_${otp.code}"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
              AnimatedContent(targetState = isCopied, label = "otp_copy_icon") { copied ->
                Icon(
                  imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                  contentDescription = if (copied) {
                    stringResource(R.string.smart_otp_copied)
                  } else {
                    stringResource(R.string.smart_otp_copy)
                  },
                  tint = if (copied) Color(0xFF16A34A) else linkColor,
                  modifier = Modifier.size(16.dp),
                )
              }
              Spacer(Modifier.width(6.dp))
              Text(
                text = if (isCopied) stringResource(R.string.smart_otp_copied) else otp.code,
                style = MaterialTheme.typography.labelLarge.copy(
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                ),
                color = if (isCopied) Color(0xFF16A34A) else color,
              )
            }
          }
        }
      }
    }
  }
}
