package telefam.company.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.telefam.company.app.R
import telefam.company.data.network.LivenessStepTelemetryDto
import telefam.company.ui.components.TelefamVerifiedBadgeIcon
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamCrimsonBright
import telefam.company.verification.TelefamVerificationCatalog
import telefam.company.verification.TelefamVerificationUiState
import telefam.company.verification.VerificationTierKind
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

@Composable
fun VerificationSelfieGuidanceScreen(
  state: TelefamVerificationUiState,
  onBack: () -> Unit,
  onStartCamera: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val pageBg = if (isDark) Color(0xFF0E1015) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color(0xFFFFFFFF)
  val border = if (isDark) Color(0xFF2B303C) else Color(0xFFE5E8EF)
  val primaryText = if (isDark) Color(0xFFF4F6FA) else Color(0xFF141821)
  val mutedText = if (isDark) Color(0xFF9BA4B5) else Color(0xFF667085)
  val crimson = if (isDark) TelefamCrimsonBright else TelefamCrimson

  Scaffold(
    containerColor = pageBg,
    topBar = {
      VerificationCrimsonHeader(
        title = stringResource(R.string.ver_liveness_title),
        subtitle = "Step 1 of 2 · Anti-spoof verification",
        onBack = onBack,
      )
    },
  ) { inner ->
    Box(modifier = Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.TopCenter) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 660.dp).testTag("verification_selfie_guidance_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        item(key = "face_oval_guide") {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF101824),
            border = BorderStroke(1.2.dp, Color(0xFF254B63)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Canvas(modifier = Modifier.size(140.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawOval(
                  color = Color(0xFF28D17C),
                  topLeft = Offset(cx - 44f, cy - 56f),
                  size = Size(88f, 112f),
                  style = Stroke(width = 4f),
                )
                drawCircle(Color(0xFF94A3B8), radius = 20f, center = Offset(cx, cy - 12f))
                drawArc(
                  color = Color(0xFF94A3B8),
                  startAngle = 180f,
                  sweepAngle = 180f,
                  useCenter = true,
                  topLeft = Offset(cx - 32f, cy + 12f),
                  size = Size(64f, 44f),
                )
              }
              Text(
                text = "Guided live selfie movements",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
              )
              Text(
                text = "Follow the server-issued sequence: Left -> Right -> Up -> Left -> Right -> Center. When your live score reaches 100%, auto-capture triggers immediately.",
                color = Color(0xFFB8C4D4),
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
              )
            }
          }
        }

        item(key = "selfie_rules") {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Before you begin", color = primaryText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
              listOf(
                "Keep your face well lit and inside the oval guide.",
                "Remove sunglasses, hats, or face coverings so both eyes remain visible.",
                "Screen recording, casting, and virtual displays are strictly blocked.",
                "Captured frames are encrypted in transit and deleted after verification.",
              ).forEach { line ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                  Text(line, color = mutedText, fontSize = 12.5.sp, lineHeight = 17.sp)
                }
              }
            }
          }
        }

        item(key = "start_btn") {
          Button(
            onClick = onStartCamera,
            enabled = !state.isSubmittingLiveness && !state.screenCaptureBlocked,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = crimson, contentColor = Color.White),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .navigationBarsPadding()
              .testTag("verification_selfie_start_camera"),
          ) {
            if (state.isSubmittingLiveness) {
              CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
              Text(stringResource(R.string.ver_liveness_start), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
fun VerificationSelfieLivenessScreen(
  state: TelefamVerificationUiState,
  onBack: () -> Unit,
  onProgress: (stepIdx: Int, scorePct: Int, faceVisible: Boolean, lightOk: Boolean, prompt: String) -> Unit,
  onAutoCaptured: (jpegBytes: ByteArray, steps: List<LivenessStepTelemetryDto>) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
    )
  }
  val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    hasCameraPermission = granted
  }
  LaunchedEffect(Unit) {
    if (!hasCameraPermission) permLauncher.launch(Manifest.permission.CAMERA)
  }

  val sequence = state.livenessSequence.ifEmpty { listOf("left", "right", "up", "left", "right", "center") }
  val recordedSteps = remember(state.livenessChallengeId) { mutableStateListOf<LivenessStepTelemetryDto>() }
  val captureTriggered = remember(state.livenessChallengeId) { AtomicBoolean(false) }
  var leftYawSign by remember(state.livenessChallengeId) { mutableStateOf<Int?>(null) }
  var lastStepTimestamp by remember(state.livenessChallengeId) { mutableStateOf(0L) }

  Scaffold(
    containerColor = Color(0xFF0A0D12),
    topBar = {
      VerificationCrimsonHeader(
        title = stringResource(R.string.ver_liveness_title),
        subtitle = "Score ${state.currentLivenessScore}% · Step ${(state.currentLivenessStepIndex + 1).coerceAtMost(sequence.size)} of ${sequence.size}",
        onBack = onBack,
      )
    },
  ) { inner ->
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxSize()
        .padding(inner)
        .navigationBarsPadding()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (state.screenCaptureBlocked) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF7F1D1D),
          modifier = Modifier.fillMaxWidth().testTag("verification_screen_capture_blocked"),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.ver_liveness_blocked), color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF151B26),
        border = BorderStroke(1.dp, Color(0xFF283244)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = state.liveMovementPrompt,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
          LinearProgressIndicator(
            progress = { state.currentLivenessScore / 100f },
            color = if (state.currentLivenessScore >= 100) Color(0xFF28D17C) else TelefamCrimsonBright,
            trackColor = Color(0xFF283244),
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).testTag("verification_liveness_score"),
          )
          Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
              text = if (state.liveFaceVisible) "Face visible" else "Position face in oval",
              color = if (state.liveFaceVisible) Color(0xFF39D98A) else Color(0xFFF59E0B),
              fontSize = 11.5.sp,
            )
            Text(
              text = if (state.liveLightOk) "Lighting OK" else "Too dark · Add light",
              color = if (state.liveLightOk) Color(0xFF39D98A) else Color(0xFFF87171),
              fontSize = 11.5.sp,
            )
          }
        }
      }

      if (!hasCameraPermission) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
          Text("Camera access is required for live selfie verification.", color = Color.White, textAlign = TextAlign.Center)
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = Color.White),
          ) {
            Text("Grant Camera Permission")
          }
        }
      } else {
        Box(
          modifier = Modifier
            .weight(1f)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black),
        ) {
          val executor = remember { Executors.newSingleThreadExecutor() }
          val faceDetector = remember {
            FaceDetection.getClient(
              FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build(),
            )
          }
          val imageCapture = remember {
            ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
          }

          DisposableEffect(Unit) {
            onDispose {
              runCatching { faceDetector.close() }
              runCatching { executor.shutdown() }
            }
          }

          AndroidView(
            factory = { ctx ->
              val previewView = PreviewView(ctx)
              val future = ProcessCameraProvider.getInstance(ctx)
              future.addListener({
                val provider = runCatching { future.get() }.getOrNull() ?: return@addListener
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                  .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                  .build()
                analysis.setAnalyzer(executor) { proxy ->
                  if (state.screenCaptureBlocked || captureTriggered.get()) {
                    proxy.close()
                    return@setAnalyzer
                  }
                  val luma = computeYPlaneMeanLuma(proxy)
                  @Suppress("UnsafeOptInUsageError")
                  val mediaImage = proxy.image
                  if (mediaImage == null) {
                    proxy.close()
                    return@setAnalyzer
                  }
                  val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                  faceDetector.process(input)
                    .addOnSuccessListener { faces ->
                      val face = faces.firstOrNull()
                      val lightOk = luma >= 35.0
                      if (face == null || !lightOk) {
                        val idx = recordedSteps.size.coerceAtMost(sequence.lastIndex)
                        onProgress(
                          idx,
                          (recordedSteps.size * 100) / sequence.size,
                          face != null,
                          lightOk,
                          if (!lightOk) "Increase lighting on your face" else "Face not visible in oval",
                        )
                        return@addOnSuccessListener
                      }
                      val yaw = face.headEulerAngleY.toDouble()
                      val pitch = face.headEulerAngleX.toDouble()
                      val leftOpen = (face.leftEyeOpenProbability ?: 0.85f).toDouble().coerceIn(0.0, 1.0)
                      val rightOpen = (face.rightEyeOpenProbability ?: 0.85f).toDouble().coerceIn(0.0, 1.0)
                      val eyesOk = leftOpen >= 0.40 && rightOpen >= 0.40
                      val curIdx = recordedSteps.size
                      if (curIdx < sequence.size) {
                        val expected = sequence[curIdx]
                        val nowMs = System.currentTimeMillis()
                        val gapOk = curIdx == 0 || (nowMs - lastStepTimestamp >= 420L)
                        val poseMatched = eyesOk && gapOk && when (expected) {
                          "left" -> {
                            val sign = if (yaw >= 0.0) 1 else -1
                            abs(yaw) >= 12.0 && (leftYawSign == null || leftYawSign == sign)
                          }
                          "right" -> {
                            val sign = if (yaw >= 0.0) 1 else -1
                            abs(yaw) >= 12.0 && (leftYawSign == null || leftYawSign != sign)
                          }
                          "up" -> pitch >= 8.5
                          else -> abs(yaw) <= 9.5 && abs(pitch) <= 9.5
                        }
                        if (poseMatched) {
                          if (expected == "left" && leftYawSign == null) {
                            leftYawSign = if (yaw >= 0.0) 1 else -1
                          }
                          recordedSteps.add(
                            LivenessStepTelemetryDto(
                              step = expected,
                              yaw = yaw,
                              pitch = pitch,
                              leftEyeOpen = leftOpen,
                              rightEyeOpen = rightOpen,
                              luma = luma,
                              capturedAtMs = nowMs,
                            ),
                          )
                          lastStepTimestamp = nowMs
                        }
                        val nextIdx = recordedSteps.size.coerceAtMost(sequence.lastIndex)
                        val pct = (recordedSteps.size * 100) / sequence.size
                        val prompt = if (recordedSteps.size >= sequence.size) {
                          "100% Live Match — Auto-capturing..."
                        } else if (!eyesOk) {
                          "Keep both eyes clearly visible"
                        } else {
                          TelefamVerificationCatalog.stepDirectionLabel(sequence[nextIdx])
                        }
                        onProgress(nextIdx, pct, true, lightOk, prompt)

                        if (recordedSteps.size >= sequence.size && captureTriggered.compareAndSet(false, true)) {
                          val stepsCopy = recordedSteps.toList()
                          imageCapture.takePicture(
                            ContextCompat.getMainExecutor(ctx),
                            object : ImageCapture.OnImageCapturedCallback() {
                              override fun onCaptureSuccess(captured: ImageProxy) {
                                val bytes = imageProxyToJpegBytes(captured, maxDim = 960)
                                captured.close()
                                if (bytes.size >= 6000) {
                                  onAutoCaptured(bytes, stepsCopy)
                                } else {
                                  captureTriggered.set(false)
                                }
                              }

                              override fun onError(exception: ImageCaptureException) {
                                captureTriggered.set(false)
                              }
                            },
                          )
                        }
                      }
                    }
                    .addOnCompleteListener { proxy.close() }
                }
                runCatching {
                  provider.unbindAll()
                  provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageCapture,
                    analysis,
                  )
                }
              }, ContextCompat.getMainExecutor(ctx))
              previewView
            },
            modifier = Modifier.fillMaxSize(),
          )

          Canvas(modifier = Modifier.fillMaxSize()) {
            val ow = size.width * 0.66f
            val oh = size.height * 0.60f
            drawOval(
              color = if (state.liveFaceVisible) Color(0xFF28D17C) else Color.White.copy(alpha = 0.72f),
              topLeft = Offset((size.width - ow) / 2f, (size.height - oh) / 2f),
              size = Size(ow, oh),
              style = Stroke(width = 6f),
            )
          }

          if (state.isSubmittingLiveness) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.52f)),
            ) {
              CircularProgressIndicator(color = Color.White)
            }
          }
        }
      }
    }
  }
}

@Composable
fun VerificationIdLiveCaptureScreen(
  state: TelefamVerificationUiState,
  onBack: () -> Unit,
  onReadiness: (Boolean, Boolean, Boolean, Boolean, Boolean, Int) -> Unit,
  onAutoCaptured: (ByteArray, String, Double, Double, Double, Boolean) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
    )
  }
  val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    hasCameraPermission = granted
  }
  LaunchedEffect(Unit) {
    if (!hasCameraPermission) permLauncher.launch(Manifest.permission.CAMERA)
  }
  val docTriggered = remember(state.documentChallengeId) { AtomicBoolean(false) }

  Scaffold(
    containerColor = Color(0xFF0A0D12),
    topBar = {
      VerificationCrimsonHeader(
        title = state.selectedDocumentType.label,
        subtitle = "Auto-captures at 100% readiness",
        onBack = onBack,
      )
    },
  ) { inner ->
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxSize().padding(inner).navigationBarsPadding().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF151B26),
        border = BorderStroke(1.dp, Color(0xFF283244)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Scan readiness: ${state.docQualityReadinessPct}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(if (state.docOcrPreviewReady) "Text readable" else "Hold ID steady", color = Color(0xFF39D98A), fontSize = 12.sp)
          }
          LinearProgressIndicator(
            progress = { state.docQualityReadinessPct / 100f },
            color = if (state.docQualityReadinessPct >= 100) Color(0xFF28D17C) else TelefamCrimsonBright,
            trackColor = Color(0xFF283244),
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
          )
          Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            QualityChip("Corners", state.docCornersReady)
            QualityChip("Sharp", state.docSharpReady)
            QualityChip("Light", state.docLightingReady)
            QualityChip("No glare", state.docGlareFreeReady)
          }
        }
      }

      if (!hasCameraPermission) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
          Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }, shape = CircleShape) {
            Text("Grant Camera Permission")
          }
        }
      } else {
        Box(
          modifier = Modifier
            .weight(1f)
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black),
        ) {
          val executor = remember { Executors.newSingleThreadExecutor() }
          val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
          val imageCapture = remember {
            ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
          }

          DisposableEffect(Unit) {
            onDispose {
              runCatching { textRecognizer.close() }
              runCatching { executor.shutdown() }
            }
          }

          AndroidView(
            factory = { ctx ->
              val previewView = PreviewView(ctx)
              val future = ProcessCameraProvider.getInstance(ctx)
              future.addListener({
                val provider = runCatching { future.get() }.getOrNull() ?: return@addListener
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                  .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                  .build()
                analysis.setAnalyzer(executor) { proxy ->
                  if (state.screenCaptureBlocked || docTriggered.get()) {
                    proxy.close()
                    return@setAnalyzer
                  }
                  val metrics = computeDocumentFrameMetrics(proxy)
                  @Suppress("UnsafeOptInUsageError")
                  val mediaImage = proxy.image
                  if (mediaImage == null) {
                    proxy.close()
                    return@setAnalyzer
                  }
                  val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                  textRecognizer.process(input)
                    .addOnSuccessListener { visionText ->
                      val ocrText = visionText.text.orEmpty().trim()
                      val ocrBlocks = visionText.textBlocks
                      val cornersOk = ocrBlocks.size >= 2 && ocrText.length >= 16
                      val sharpOk = metrics.sharpness >= 0.35
                      val lightOk = metrics.luma >= 40.0
                      val glareOk = metrics.glareRatio <= 0.35
                      val ocrReady = ocrText.length >= 18
                      val pct = listOf(cornersOk, sharpOk, lightOk, glareOk, ocrReady).count { it } * 20
                      onReadiness(cornersOk, sharpOk, lightOk, glareOk, ocrReady, pct)

                      if (pct == 100 && docTriggered.compareAndSet(false, true)) {
                        imageCapture.takePicture(
                          ContextCompat.getMainExecutor(ctx),
                          object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(captured: ImageProxy) {
                              val bytes = imageProxyToJpegBytes(captured, maxDim = 1280)
                              captured.close()
                              if (bytes.size >= 10000) {
                                onAutoCaptured(bytes, ocrText, metrics.sharpness, metrics.luma, metrics.glareRatio, cornersOk)
                              } else {
                                docTriggered.set(false)
                              }
                            }

                            override fun onError(exception: ImageCaptureException) {
                              docTriggered.set(false)
                            }
                          },
                        )
                      }
                    }
                    .addOnCompleteListener { proxy.close() }
                }
                runCatching {
                  provider.unbindAll()
                  provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    analysis,
                  )
                }
              }, ContextCompat.getMainExecutor(ctx))
              previewView
            },
            modifier = Modifier.fillMaxSize(),
          )

          Canvas(modifier = Modifier.fillMaxSize()) {
            val fw = size.width * 0.86f
            val fh = fw * 0.63f
            drawRoundRect(
              color = if (state.docQualityReadinessPct >= 100) Color(0xFF28D17C) else Color.White.copy(alpha = 0.82f),
              topLeft = Offset((size.width - fw) / 2f, (size.height - fh) / 2f),
              size = Size(fw, fh),
              cornerRadius = CornerRadius(24f, 24f),
              style = Stroke(width = 6f),
            )
          }

          if (state.isSubmittingDocument) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.56f)),
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Extracting OCR & purging temporary scan...", color = Color.White, fontSize = 13.sp)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QualityChip(label: String, ready: Boolean) {
  Surface(
    shape = CircleShape,
    color = if (ready) Color(0xFF14532D) else Color(0xFF1E293B),
  ) {
    Text(
      text = label,
      color = if (ready) Color(0xFF86EFAC) else Color(0xFF94A3B8),
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun VerificationStatusReviewScreen(
  state: TelefamVerificationUiState,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  onResumeSteps: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val pageBg = if (isDark) Color(0xFF0E1015) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color(0xFFFFFFFF)
  val border = if (isDark) Color(0xFF2B303C) else Color(0xFFE5E8EF)
  val primaryText = if (isDark) Color(0xFFF4F6FA) else Color(0xFF141821)
  val mutedText = if (isDark) Color(0xFF9BA4B5) else Color(0xFF667085)
  val st = state.status
  val isVerified = st.reviewStatus == "verified" && st.verifiedTier != "none" && st.verifiedTier.isNotBlank()

  Scaffold(
    containerColor = pageBg,
    topBar = {
      VerificationCrimsonHeader(
        title = if (isVerified) stringResource(R.string.ver_verified_title) else stringResource(R.string.ver_under_review_title),
        subtitle = "Telefam Trust & Safety",
        onBack = onBack,
      )
    },
  ) { inner ->
    Box(modifier = Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.TopCenter) {
      Column(
        modifier = Modifier
          .widthIn(max = 660.dp)
          .fillMaxWidth()
          .padding(16.dp)
          .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = cardBg,
          border = BorderStroke(1.dp, border),
          modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isVerified) "verification_unlocked_badge_card" else "verification_under_review_card"),
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            if (isVerified) {
              TelefamVerifiedBadgeIcon(tier = st.verifiedTier, size = 56.dp, isDarkTheme = isDark)
              Text(
                text = "${VerificationTierKind.fromWire(st.verifiedTier).headerTitle} Active",
                color = primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
              )
              Text(
                text = "Your verified badge is unlocked across your Telefam profile, feeds, and chats.",
                color = mutedText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
              )
            } else {
              Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(46.dp))
              Text(stringResource(R.string.ver_under_review_title), color = primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
              Text(
                text = stringResource(R.string.ver_under_review_sub),
                color = mutedText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
              )
            }

            if (st.extractedFullName.isNotBlank() || st.docLast4.isNotBlank()) {
              Surface(shape = RoundedCornerShape(12.dp), color = pageBg, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  if (st.extractedFullName.isNotBlank()) {
                    Text("Verified Name: ${st.extractedFullName}", color = primaryText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                  }
                  if (st.docLast4.isNotBlank()) {
                    Text("Document Ending: ****${st.docLast4}", color = mutedText, fontSize = 12.sp)
                  }
                  if (st.docExpiryIso.isNotBlank()) {
                    Text("Expiry: ${st.docExpiryIso}", color = mutedText, fontSize = 12.sp)
                  }
                }
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
              Text(
                text = if (st.documentsPurged) {
                  "Uploaded verification images have been permanently deleted."
                } else {
                  stringResource(R.string.ver_doc_purge_notice)
                },
                color = Color(0xFF10B981),
                fontSize = 11.5.sp,
              )
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(
            onClick = onRefresh,
            shape = CircleShape,
            modifier = Modifier.weight(1f).height(48.dp).testTag("verification_refresh_status"),
          ) {
            Text("Refresh Status")
          }
          Button(
            onClick = onResumeSteps,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = Color.White),
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Text(if (isVerified) "Explore Tiers" else "View Tiers")
          }
        }
      }
    }
  }
}

private data class DocumentFrameMetrics(val sharpness: Double, val luma: Double, val glareRatio: Double)

private fun computeYPlaneMeanLuma(proxy: ImageProxy): Double {
  val plane = proxy.planes.firstOrNull() ?: return 0.0
  val buf = plane.buffer.duplicate()
  buf.rewind()
  var sum = 0L
  var count = 0
  val step = 16
  val rem = buf.remaining()
  var idx = 0
  while (idx < rem) {
    sum += (buf.get(idx).toInt() and 0xFF)
    count++
    idx += step
  }
  return if (count == 0) 0.0 else sum.toDouble() / count
}

private fun computeDocumentFrameMetrics(proxy: ImageProxy): DocumentFrameMetrics {
  val plane = proxy.planes.firstOrNull() ?: return DocumentFrameMetrics(0.0, 0.0, 1.0)
  val buf = plane.buffer.duplicate()
  buf.rewind()
  val w = proxy.width.coerceAtLeast(1)
  val rowStride = plane.rowStride.coerceAtLeast(w)
  val total = buf.remaining()
  var lumaSum = 0L
  var glareCount = 0
  var edgeSum = 0L
  var samples = 0
  var y = 2
  while (y < proxy.height - 2) {
    var x = 2
    val rowOffset = y * rowStride
    while (x < w - 2) {
      val idx = rowOffset + x
      if (idx + 1 < total && idx - 1 >= 0) {
        val c = buf.get(idx).toInt() and 0xFF
        val n = buf.get(idx + 1).toInt() and 0xFF
        lumaSum += c
        if (c >= 246) glareCount++
        edgeSum += abs(c - n)
        samples++
      }
      x += 8
    }
    y += 8
  }
  if (samples == 0) return DocumentFrameMetrics(0.0, 0.0, 1.0)
  val luma = lumaSum.toDouble() / samples
  val glare = glareCount.toDouble() / samples
  val sharpness = ((edgeSum.toDouble() / samples) / 14.0).coerceIn(0.0, 1.0)
  return DocumentFrameMetrics(sharpness = sharpness, luma = luma, glareRatio = glare)
}

private fun imageProxyToJpegBytes(proxy: ImageProxy, maxDim: Int): ByteArray {
  val bitmap = runCatching { proxy.toBitmap() }.getOrNull() ?: return ByteArray(0)
  val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
    val ratio = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height).toFloat()
    Bitmap.createScaledBitmap(
      bitmap,
      (bitmap.width * ratio).toInt().coerceAtLeast(1),
      (bitmap.height * ratio).toInt().coerceAtLeast(1),
      true,
    )
  } else {
    bitmap
  }
  val out = ByteArrayOutputStream()
  scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
  if (scaled !== bitmap) scaled.recycle()
  bitmap.recycle()
  return out.toByteArray()
}
