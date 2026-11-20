package telefam.company.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.telefam.company.app.R
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamPureWhite

@Composable
fun TelefamPhotoCropperDialog(
  bitmap: Bitmap,
  onCancel: () -> Unit,
  onConfirmCrop: (centerX: Float, centerY: Float, zoomScale: Float) -> Unit,
) {
  var zoom by remember { mutableFloatStateOf(1.1f) }
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }
  val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

  Dialog(onDismissRequest = onCancel) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = stringResource(R.string.profile_crop_title),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .pointerInput(Unit) {
              detectDragGestures { change, dragAmount ->
                change.consume()
                val maxPan = 180f * zoom
                offsetX = (offsetX + dragAmount.x).coerceIn(-maxPan, maxPan)
                offsetY = (offsetY + dragAmount.y).coerceIn(-maxPan, maxPan)
              }
            },
          contentAlignment = Alignment.Center,
        ) {
          Image(
            bitmap = imageBitmap,
            contentDescription = stringResource(R.string.cd_upload_photo),
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer {
                scaleX = zoom
                scaleY = zoom
                translationX = offsetX
                translationY = offsetY
              },
          )

          Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.44f
            val maskPath = Path().apply {
              fillType = PathFillType.EvenOdd
              addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
              addOval(
                androidx.compose.ui.geometry.Rect(
                  center.x - radius,
                  center.y - radius,
                  center.x + radius,
                  center.y + radius,
                ),
              )
            }
            drawPath(path = maskPath, color = Color.Black.copy(alpha = 0.55f))
            drawCircle(
              color = TelefamCrimson,
              radius = radius,
              style = Stroke(width = 2.5.dp.toPx()),
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Slider(
          value = zoom,
          onValueChange = { zoom = it },
          valueRange = 1f..3.5f,
          colors = SliderDefaults.colors(
            thumbColor = TelefamCrimson,
            activeTrackColor = TelefamCrimson,
          ),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(40.dp),
          ) {
            Text(stringResource(R.string.profile_crop_cancel))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = {
              val normX = (0.5f - (offsetX / 800f)).coerceIn(0.1f, 0.9f)
              val normY = (0.5f - (offsetY / 800f)).coerceIn(0.1f, 0.9f)
              onConfirmCrop(normX, normY, zoom)
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = TelefamCrimson,
              contentColor = TelefamPureWhite,
            ),
            modifier = Modifier.height(40.dp),
          ) {
            Text(stringResource(R.string.profile_crop_apply))
          }
        }
      }
    }
  }
}
