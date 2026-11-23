package telefam.company.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.app.R
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamCrimsonBright
import telefam.company.verification.IdGuidanceExampleItem
import telefam.company.verification.TelefamVerificationCatalog
import telefam.company.verification.TelefamVerificationUiState
import telefam.company.verification.VerificationDocumentType

@Composable
fun VerificationIdGuidanceAndSelectScreen(
  state: TelefamVerificationUiState,
  onBack: () -> Unit,
  onSelectDocType: (VerificationDocumentType) -> Unit,
  onStartIdScan: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val pageBg = if (isDark) Color(0xFF0E1015) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color(0xFFFFFFFF)
  val border = if (isDark) Color(0xFF2B303C) else Color(0xFFE5E8EF)
  val primaryText = if (isDark) Color(0xFFF4F6FA) else Color(0xFF141821)
  val mutedText = if (isDark) Color(0xFF9BA4B5) else Color(0xFF667085)
  val crimson = if (isDark) TelefamCrimsonBright else TelefamCrimson
  val docOptions = TelefamVerificationCatalog.documentOptionsForTier(state.selectedTier)

  Scaffold(
    containerColor = pageBg,
    topBar = {
      VerificationCrimsonHeader(
        title = stringResource(R.string.ver_doc_select_title),
        subtitle = state.selectedTier.headerTitle,
        onBack = onBack,
      )
    },
  ) { inner ->
    Box(modifier = Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.TopCenter) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp).testTag("verification_doc_guidance_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Document type chooser
        item(key = "doc_types") {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Choose your identification document",
                color = primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.5.sp,
              )
              docOptions.forEach { doc ->
                val selected = state.selectedDocumentType == doc
                Surface(
                  onClick = { onSelectDocType(doc) },
                  shape = RoundedCornerShape(12.dp),
                  color = if (selected) crimson.copy(alpha = if (isDark) 0.16f else 0.08f) else pageBg,
                  border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) crimson else border),
                  modifier = Modifier.fillMaxWidth().testTag("verification_doc_select_${doc.wire}"),
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(doc.label, color = primaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                      Text(doc.subtitle, color = mutedText, fontSize = 11.5.sp)
                    }
                    RadioButton(
                      selected = selected,
                      onClick = { onSelectDocType(doc) },
                      colors = RadioButtonDefaults.colors(selectedColor = crimson, unselectedColor = mutedText),
                    )
                  }
                }
              }
            }
          }
        }

        // Screenshot 4 Correct ID Section
        item(key = "correct_id_panel") {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0E1B1A),
            border = BorderStroke(1.2.dp, Color(0xFF1B6E52)),
            modifier = Modifier.fillMaxWidth().testTag("verification_correct_id_guidance"),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(
                text = stringResource(R.string.ver_doc_guidance_title),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
              )
              Text(
                text = stringResource(R.string.ver_doc_guidance_sub),
                color = Color(0xFFB5C5C2),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
              )
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(0.95f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF28D17C), modifier = Modifier.size(18.dp))
                    Text("Correct ID", color = Color(0xFF28D17C), fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                  }
                  TelefamVerificationCatalog.correctIdRules.forEach { rule ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                      Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF28D17C), modifier = Modifier.size(13.dp))
                      Text(rule, color = Color(0xFFE5EFEA), fontSize = 11.sp)
                    }
                  }
                }
                Box(modifier = Modifier.weight(1.25f), contentAlignment = Alignment.Center) {
                  MiniIdCardVectorIllustration(exampleKey = "valid", modifier = Modifier.fillMaxWidth().height(110.dp))
                }
              }
              Text(
                text = "Auto capture starts automatically at 100% readiness.",
                color = Color(0xFF39D98A),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }

        // Screenshot 4 Incorrect ID 8-example grid
        item(key = "incorrect_id_panel") {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF151822),
            border = BorderStroke(1.dp, Color(0xFF3A2429)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFFE53935))) {
                  Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                }
                Text("Incorrect ID — Avoid these", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
              }
              val pairs = TelefamVerificationCatalog.incorrectIdExamples.chunked(2)
              pairs.forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                  rowItems.forEach { ex ->
                    IncorrectExampleTile(item = ex, modifier = Modifier.weight(1f))
                  }
                }
              }
            }
          }
        }

        // Explicit secure deletion notice + Start Capture CTA
        item(key = "purge_and_start") {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.navigationBarsPadding()) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (isDark) Color(0xFF14221D) else Color(0xFFECFDF5),
              border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
              modifier = Modifier.fillMaxWidth().testTag("verification_secure_delete_notice"),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = stringResource(R.string.ver_doc_purge_notice),
                  color = if (isDark) Color(0xFFD1FAE5) else Color(0xFF065F46),
                  fontSize = 12.sp,
                  lineHeight = 16.5.sp,
                )
              }
            }

            Button(
              onClick = onStartIdScan,
              enabled = !state.isSubmittingDocument,
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(containerColor = crimson, contentColor = Color.White),
              modifier = Modifier.fillMaxWidth().height(52.dp).testTag("verification_doc_start_camera"),
            ) {
              if (state.isSubmittingDocument) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
              } else {
                Text(stringResource(R.string.ver_doc_start_scan), fontWeight = FontWeight.Bold, fontSize = 15.sp)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun IncorrectExampleTile(item: IdGuidanceExampleItem, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = Color(0xFF1D2230),
    border = BorderStroke(1.dp, Color(0xFF33394A)),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      MiniIdCardVectorIllustration(exampleKey = item.key, modifier = Modifier.fillMaxWidth().height(58.dp))
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(0xFFE53935))) {
          Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
        }
        Text(item.label, color = Color(0xFFF3F4F6), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
      }
    }
  }
}

@Composable
private fun MiniIdCardVectorIllustration(exampleKey: String, modifier: Modifier = Modifier) {
  val tilt = if (exampleKey == "aligned") -10f else 0f
  val scaleFactor = when (exampleKey) {
    "too_far" -> 0.52f
    "too_close" -> 1.24f
    else -> 1f
  }
  val cardAlpha = if (exampleKey == "too_dark") 0.32f else if (exampleKey == "blurry") 0.60f else 1f
  Box(contentAlignment = Alignment.Center, modifier = modifier.clip(RoundedCornerShape(8.dp))) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .rotate(tilt)
        .alpha(cardAlpha),
    ) {
      val cw = size.width * 0.88f * scaleFactor
      val ch = size.height * 0.82f * scaleFactor
      val left = if (exampleKey == "corners") size.width * 0.22f else (size.width - cw) / 2f
      val top = (size.height - ch) / 2f

      drawRoundRect(
        color = Color(0xFFE9EEF5),
        topLeft = Offset(left, top),
        size = Size(cw, ch),
        cornerRadius = CornerRadius(8f, 8f),
      )
      // Photo rectangle
      drawRoundRect(
        color = Color(0xFF64748B),
        topLeft = Offset(left + cw * 0.08f, top + ch * 0.24f),
        size = Size(cw * 0.22f, ch * 0.52f),
        cornerRadius = CornerRadius(4f, 4f),
      )
      // Detail bars
      val lineColor = if (exampleKey == "blurry") Color(0xFFAAB5C6) else Color(0xFF334155)
      drawRoundRect(lineColor, topLeft = Offset(left + cw * 0.22f, top + ch * 0.09f), size = Size(cw * 0.56f, ch * 0.09f), cornerRadius = CornerRadius(3f, 3f))
      drawRoundRect(lineColor, topLeft = Offset(left + cw * 0.36f, top + ch * 0.28f), size = Size(cw * 0.42f, ch * 0.08f), cornerRadius = CornerRadius(3f, 3f))
      drawRoundRect(lineColor, topLeft = Offset(left + cw * 0.36f, top + ch * 0.44f), size = Size(cw * 0.50f, ch * 0.08f), cornerRadius = CornerRadius(3f, 3f))
      drawRoundRect(lineColor, topLeft = Offset(left + cw * 0.36f, top + ch * 0.60f), size = Size(cw * 0.34f, ch * 0.08f), cornerRadius = CornerRadius(3f, 3f))

      if (exampleKey == "valid") {
        val bracket = Color(0xFF28D17C)
        val bStroke = 4f
        val bLen = 16f
        // Four green alignment brackets
        drawLine(bracket, Offset(left - 6f, top - 6f), Offset(left - 6f + bLen, top - 6f), strokeWidth = bStroke)
        drawLine(bracket, Offset(left - 6f, top - 6f), Offset(left - 6f, top - 6f + bLen), strokeWidth = bStroke)
        drawLine(bracket, Offset(left + cw + 6f, top - 6f), Offset(left + cw + 6f - bLen, top - 6f), strokeWidth = bStroke)
        drawLine(bracket, Offset(left + cw + 6f, top - 6f), Offset(left + cw + 6f, top - 6f + bLen), strokeWidth = bStroke)
        drawLine(bracket, Offset(left - 6f, top + ch + 6f), Offset(left - 6f + bLen, top + ch + 6f), strokeWidth = bStroke)
        drawLine(bracket, Offset(left - 6f, top + ch + 6f), Offset(left - 6f, top + ch + 6f - bLen), strokeWidth = bStroke)
        drawLine(bracket, Offset(left + cw + 6f, top + ch + 6f), Offset(left + cw + 6f - bLen, top + ch + 6f), strokeWidth = bStroke)
        drawLine(bracket, Offset(left + cw + 6f, top + ch + 6f), Offset(left + cw + 6f, top + ch + 6f - bLen), strokeWidth = bStroke)
      }
      if (exampleKey == "glare") {
        drawCircle(Color.White.copy(alpha = 0.86f), radius = cw * 0.16f, center = Offset(left + cw * 0.68f, top + ch * 0.45f))
      }
      if (exampleKey == "obstructed") {
        drawRoundRect(
          color = Color(0xFFCC9977),
          topLeft = Offset(left + cw * 0.62f, top + ch * 0.32f),
          size = Size(cw * 0.40f, ch * 0.56f),
          cornerRadius = CornerRadius(14f, 14f),
        )
      }
      if (exampleKey == "corners") {
        drawPath(
          Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
          },
          color = Color(0xFFE53935).copy(alpha = 0.55f),
          style = Stroke(width = 3f),
        )
      }
    }
  }
}
