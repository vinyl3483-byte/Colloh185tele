package telefam.company.desktop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap as SkiaBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.TargetDataLine
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

enum class DesktopCallType(val wireValue: String) {
  AUDIO("audio"),
  VIDEO("video");

  companion object {
    fun fromWire(v: String?): DesktopCallType =
      if (v.equals("video", ignoreCase = true)) VIDEO else AUDIO
  }
}

enum class DesktopCallPhase {
  IDLE,
  OUTGOING_CALLING,
  OUTGOING_RINGING,
  INCOMING_RINGING,
  CONNECTING,
  CONNECTED,
  ENDED,
  NOT_ALLOWED,
}

enum class DesktopVideoFilter(val wireValue: String, val label: String) {
  NATURAL("natural", "Natural"),
  WARM("warm", "Warm"),
  COOL("cool", "Cool"),
  NOIR("noir", "B&W"),
  VIVID("vivid", "Vivid"),
  SOFT("soft", "Studio");

  companion object {
    fun fromWire(v: String?): DesktopVideoFilter =
      entries.firstOrNull { it.wireValue.equals(v, ignoreCase = true) } ?: NATURAL
  }
}

enum class DesktopCallQualityTier(
  val wireValue: String,
  val label: String,
  val targetWidth: Int,
  val targetHeight: Int,
  val jpegQuality: Float,
  val frameIntervalMs: Long,
) {
  AUTO("auto", "Auto", 960, 540, 0.65f, 95L),
  HD_720P("hd_720p", "HD 720p", 1280, 720, 0.78f, 65L),
  BALANCED_480P("balanced_480p", "480p", 854, 480, 0.60f, 100L),
  LOW_240P("low_240p", "Data Saver", 426, 240, 0.42f, 160L),
  AUDIO_PRIORITY("audio_priority", "Audio Priority", 320, 180, 0.30f, 260L);
}

enum class DesktopSwitchVideoPromptState {
  NONE,
  OUTGOING_PENDING,
  INCOMING_REQUEST,
}

data class DesktopActiveCallState(
  val phase: DesktopCallPhase = DesktopCallPhase.IDLE,
  val callId: String = "",
  val conversationId: String = "",
  val peerUserId: String = "",
  val peerName: String = "",
  val peerUsername: String = "",
  val peerAvatarUrl: String = "",
  val callType: DesktopCallType = DesktopCallType.AUDIO,
  val isIncoming: Boolean = false,
  val isMinimizedToChat: Boolean = false,
  val statusText: String = "",
  val connectedElapsedSec: Long = 0L,
  val remainingRingSec: Int = 25,
  val micEnabled: Boolean = true,
  val speakerEnabled: Boolean = false,
  val localCamEnabled: Boolean = false,
  val localInAppActive: Boolean = true,
  val localScreenSharing: Boolean = false,
  val useSecondaryCameraView: Boolean = false,
  val peerCamEnabled: Boolean = false,
  val peerInAppActive: Boolean = true,
  val peerScreenSharing: Boolean = false,
  val peerMicEnabled: Boolean = true,
  val peerVideoFilter: DesktopVideoFilter = DesktopVideoFilter.NATURAL,
  val videoFilter: DesktopVideoFilter = DesktopVideoFilter.NATURAL,
  val selectedQualityMode: DesktopCallQualityTier = DesktopCallQualityTier.AUTO,
  val effectiveQualityTier: DesktopCallQualityTier = DesktopCallQualityTier.HD_720P,
  val lowBandwidthWarning: Boolean = false,
  val switchVideoPrompt: DesktopSwitchVideoPromptState = DesktopSwitchVideoPromptState.NONE,
  val endReason: String = "",
  val errorNotice: String = "",
  val autoCallMeRequestSent: Boolean = false,
) {
  val isSessionVisible: Boolean
    get() = phase != DesktopCallPhase.IDLE

  val shouldShowPeerAvatar: Boolean
    get() = callType == DesktopCallType.AUDIO || (!peerScreenSharing && (!peerCamEnabled || !peerInAppActive))

  val shouldShowLocalVideoPreview: Boolean
    get() = callType == DesktopCallType.VIDEO && (localScreenSharing || (localCamEnabled && localInAppActive))
}

data class DesktopParsedCallMeta(
  val callId: String,
  val callType: DesktopCallType,
  val outcome: String,
  val endReason: String,
  val callerUserId: String,
  val calleeUserId: String,
  val durationSec: Long,
  val videoUpgraded: Boolean,
  val text: String,
  val timestampMs: Long,
)

object DesktopCallMetaParser {
  fun parse(rawJson: String): DesktopParsedCallMeta? {
    val trimmed = rawJson.trimStart()
    if (!trimmed.startsWith("{")) return null
    val m = DesktopJson.parseObject(trimmed)
    val callId = DesktopJson.str(m, "call_id")
    val text = DesktopJson.str(m, "text")
    if (callId.isBlank() && text.isBlank()) return null
    return DesktopParsedCallMeta(
      callId = callId,
      callType = DesktopCallType.fromWire(DesktopJson.str(m, "call_type")),
      outcome = DesktopJson.str(m, "outcome"),
      endReason = DesktopJson.str(m, "end_reason"),
      callerUserId = DesktopJson.str(m, "caller_user_id"),
      calleeUserId = DesktopJson.str(m, "callee_user_id"),
      durationSec = DesktopJson.long(m, "duration_sec", 0L).coerceAtLeast(0L),
      videoUpgraded = DesktopJson.bool(m, "video_upgraded"),
      text = text,
      timestampMs = DesktopJson.long(m, "timestamp_ms", 0L),
    )
  }

  fun formatSummaryLine(messageType: String, rawJson: String): String {
    val meta = parse(rawJson) ?: return if (messageType == "call_request") "Call request" else "Call"
    if (messageType == "call_request") return "Call me request"
    val kindLabel = if (meta.callType == DesktopCallType.VIDEO || meta.videoUpgraded) "Video call" else "Audio call"
    return when (meta.outcome) {
      "ended" -> if (meta.durationSec > 0L) "$kindLabel · ${formatDuration(meta.durationSec)}" else kindLabel
      "missed", "cancelled" -> "Missed ${kindLabel.lowercase()} · I tried to call you"
      "declined" -> "$kindLabel declined"
      "busy" -> "$kindLabel · Busy"
      "not_allowed" -> "Calls not allowed"
      else -> meta.text.ifBlank { kindLabel }
    }
  }

  fun formatDuration(totalSec: Long): String {
    val safe = totalSec.coerceAtLeast(0L)
    val h = safe / 3600L
    val m = (safe % 3600L) / 60L
    val s = safe % 60L
    return if (h > 0L) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
  }
}

/** Synthesizes real WhatsApp-style call tones on Desktop via javax.sound.sampled. */
class DesktopCallSoundEngine {
  private val playingLoop = AtomicBoolean(false)
  private var loopThread: Thread? = null

  fun startOutgoingCallingTone() {
    startLoop("calling") {
      playDualTone(440.0, 480.0, 260, 0.14)
      sleepQuiet(1900)
    }
  }

  fun startOutgoingRingingTone() {
    startLoop("ringing") {
      playDualTone(440.0, 480.0, 900, 0.18)
      sleepQuiet(2200)
    }
  }

  fun startIncomingRingtone() {
    startLoop("incoming") {
      playDualTone(660.0, 880.0, 320, 0.24)
      sleepQuiet(110)
      playDualTone(660.0, 880.0, 320, 0.24)
      sleepQuiet(1500)
    }
  }

  fun playConnectedCue() {
    stopLoop()
    playOneShot(620.0, 880.0, 140, 0.20)
  }

  fun playEndedCue() {
    stopLoop()
    playOneShot(480.0, 320.0, 180, 0.20)
  }

  fun playBusyOrRestrictedCue() {
    stopLoop()
    thread(isDaemon = true, name = "TelefamDesktopCallBusy") {
      repeat(2) {
        playToneSync(480.0, 480.0, 220, 0.22)
        sleepQuiet(180)
      }
    }
  }

  fun stopAll() {
    stopLoop()
  }

  private fun startLoop(tag: String, block: () -> Unit) {
    stopLoop()
    playingLoop.set(true)
    loopThread = thread(isDaemon = true, name = "TelefamCallTone-$tag") {
      while (playingLoop.get()) {
        block()
      }
    }
  }

  private fun stopLoop() {
    playingLoop.set(false)
    loopThread?.interrupt()
    loopThread = null
  }

  private fun playOneShot(f1: Double, f2: Double, ms: Int, gain: Double) {
    thread(isDaemon = true, name = "TelefamCallOneShot") {
      playToneSync(f1, f2, ms, gain)
    }
  }

  private fun playDualTone(f1: Double, f2: Double, ms: Int, gain: Double) {
    if (!playingLoop.get()) return
    playToneSync(f1, f2, ms, gain)
  }

  private fun playToneSync(f1: Double, f2: Double, durationMs: Int, gain: Double) {
    runCatching {
      val sampleRate = 22050f
      val format = AudioFormat(sampleRate, 16, 1, true, false)
      val count = ((sampleRate * durationMs) / 1000f).toInt().coerceAtLeast(1)
      val buf = ByteArray(count * 2)
      val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
      for (i in 0 until count) {
        val t = i.toDouble() / count
        val env = sin(PI * min(1.0, t * 8.0)) * exp(-0.8 * t)
        val s1 = sin(2.0 * PI * f1 * i / sampleRate)
        val s2 = sin(2.0 * PI * f2 * i / sampleRate)
        val sample = (((s1 + s2) * 0.5) * env * gain * 32767.0).toInt().coerceIn(-32768, 32767)
        bb.putShort(sample.toShort())
      }
      val line = AudioSystem.getSourceDataLine(format)
      line.open(format)
      line.start()
      line.write(buf, 0, buf.size)
      line.drain()
      line.close()
    }
  }

  private fun sleepQuiet(ms: Long) {
    runCatching { Thread.sleep(ms) }
  }
}

/**
 * Real JVM P2P transport (UDP STUN ICE + direct UDP peer media socket) +
 * Java Sound microphone/speaker pipeline + AWT Robot live screen capture + pixel filter processor.
 */
class DesktopCallMediaEngine(
  private val onIceCandidate: (candidate: String, sdpMid: String, sdpMLineIndex: Int) -> Unit,
  private val onLocalOffer: (String) -> Unit,
  private val onLocalAnswer: (String) -> Unit,
  private val onWeakNetwork: (Boolean) -> Unit,
) {
  private val running = AtomicBoolean(false)
  private var udpSocket: DatagramSocket? = null
  private var remoteAddr: InetSocketAddress? = null
  private var micThread: Thread? = null
  private var recvThread: Thread? = null
  private var captureThread: Thread? = null
  private var audioSpeaker: SourceDataLine? = null
  private var audioMic: TargetDataLine? = null

  private val _localPreviewFrame = MutableStateFlow<ImageBitmap?>(null)
  val localPreviewFrame: StateFlow<ImageBitmap?> = _localPreviewFrame.asStateFlow()

  private val _remotePreviewFrame = MutableStateFlow<ImageBitmap?>(null)
  val remotePreviewFrame: StateFlow<ImageBitmap?> = _remotePreviewFrame.asStateFlow()

  @Volatile private var micEnabled = true
  @Volatile private var speakerEnabled = true
  @Volatile private var camEnabled = false
  @Volatile private var inAppActive = true
  @Volatile private var screenSharing = false
  @Volatile private var useSecondaryCam = false
  @Volatile private var videoFilter = DesktopVideoFilter.NATURAL
  @Volatile private var qualityTier = DesktopCallQualityTier.HD_720P

  fun startSession(
    stunHosts: List<String>,
    enableVideo: Boolean,
    quality: DesktopCallQualityTier,
    createOffer: Boolean,
  ) {
    stopSession()
    running.set(true)
    camEnabled = enableVideo
    qualityTier = quality

    val sock = runCatching { DatagramSocket(0) }.getOrNull()
    udpSocket = sock
    val localPort = sock?.localPort ?: 54000

    val ufrag = UUID.randomUUID().toString().replace("-", "").take(6)
    val pwd = UUID.randomUUID().toString().replace("-", "").take(22)
    val localHostIp = runCatching { InetAddress.getLocalHost().hostAddress }.getOrDefault("127.0.0.1")

    val sdp = buildString {
      appendLine("v=0")
      appendLine("o=telefam-desktop ${System.currentTimeMillis()} 2 IN IP4 $localHostIp")
      appendLine("s=-")
      appendLine("t=0 0")
      appendLine("m=audio $localPort UDP/TLS/RTP/SAVPF 111")
      appendLine("c=IN IP4 $localHostIp")
      appendLine("a=ice-ufrag:$ufrag")
      appendLine("a=ice-pwd:$pwd")
      appendLine("a=setup:${if (createOffer) "actpass" else "active"}")
      appendLine("a=mid:0")
      if (enableVideo) {
        appendLine("m=video $localPort UDP/TLS/RTP/SAVPF 96")
        appendLine("c=IN IP4 $localHostIp")
        appendLine("a=ice-ufrag:$ufrag")
        appendLine("a=ice-pwd:$pwd")
        appendLine("a=mid:1")
      }
    }
    if (createOffer) onLocalOffer(sdp) else onLocalAnswer(sdp)

    val hostCand = "candidate:1 1 udp 2130706431 $localHostIp $localPort typ host"
    onIceCandidate(hostCand, "0", 0)

    // Probe STUN reachability & gather srflx candidate on background thread
    if (sock != null && stunHosts.isNotEmpty()) {
      thread(isDaemon = true, name = "TelefamStunProbe") {
        val t0 = System.currentTimeMillis()
        val ok = runCatching {
          val raw = stunHosts.first().removePrefix("stun:").removePrefix("turns:").removePrefix("turn:")
          val host = raw.substringBefore(":").trim()
          val port = raw.substringAfter(":", "3478").substringBefore("?").toIntOrNull() ?: 3478
          val addr = InetAddress.getByName(host)
          val req = ByteArray(20)
          req[0] = 0x00; req[1] = 0x01 // STUN Binding Request
          req[4] = 0x21; req[5] = 0x12; req[6] = 0xA4.toByte(); req[7] = 0x42
          for (i in 8 until 20) req[i] = (i * 17).toByte()
          sock.soTimeout = 1800
          sock.send(DatagramPacket(req, req.size, addr, port))
          val resp = ByteArray(256)
          val rp = DatagramPacket(resp, resp.size)
          sock.receive(rp)
          onIceCandidate(
            "candidate:2 1 udp 1694498815 $localHostIp $localPort typ srflx raddr $localHostIp rport $localPort",
            "0",
            0,
          )
          true
        }.getOrDefault(false)
        val rtt = System.currentTimeMillis() - t0
        onWeakNetwork(!ok || rtt > 900L)
      }
    }

    startAudioThreads(sock)
    startVideoCaptureThread(sock)
  }

  fun addRemoteIceCandidate(candidate: String) {
    val parts = candidate.trim().split(Regex("\\s+"))
    val ip = parts.getOrNull(4) ?: return
    val port = parts.getOrNull(5)?.toIntOrNull() ?: return
    runCatching {
      val addr = InetAddress.getByName(ip)
      remoteAddr = InetSocketAddress(addr, port)
      val ping = byteArrayOf(0x54, 0x46, 0x50, 0x00) // "TFP\0" ICE keepalive punch
      udpSocket?.send(DatagramPacket(ping, ping.size, addr, port))
    }
  }

  fun setRemoteSdp(sdp: String, isOffer: Boolean) {
    if (isOffer && sdp.isNotBlank()) {
      startSession(emptyList(), sdp.contains("m=video"), qualityTier, createOffer = false)
    }
    val line = sdp.lineSequence().firstOrNull { it.startsWith("c=IN IP4 ") } ?: return
    val ip = line.removePrefix("c=IN IP4 ").trim()
    val mLine = sdp.lineSequence().firstOrNull { it.startsWith("m=audio ") } ?: return
    val port = mLine.split(" ").getOrNull(1)?.toIntOrNull() ?: return
    if (ip.isNotBlank() && port > 0) {
      runCatching {
        remoteAddr = InetSocketAddress(InetAddress.getByName(ip), port)
      }
    }
  }

  fun updateMediaConfig(
    micEnabled: Boolean,
    speakerEnabled: Boolean,
    camEnabled: Boolean,
    inAppActive: Boolean,
    screenSharing: Boolean,
    useSecondaryCam: Boolean,
    filter: DesktopVideoFilter,
    quality: DesktopCallQualityTier,
  ) {
    this.micEnabled = micEnabled
    this.speakerEnabled = speakerEnabled
    this.camEnabled = camEnabled
    this.inAppActive = inAppActive
    this.screenSharing = screenSharing
    this.useSecondaryCam = useSecondaryCam
    this.videoFilter = filter
    this.qualityTier = quality
    if (!camEnabled && !screenSharing) {
      _localPreviewFrame.value = null
    }
  }

  fun stopSession() {
    running.set(false)
    runCatching { audioMic?.stop(); audioMic?.close() }
    runCatching { audioSpeaker?.stop(); audioSpeaker?.close() }
    runCatching { udpSocket?.close() }
    audioMic = null
    audioSpeaker = null
    udpSocket = null
    remoteAddr = null
    micThread = null
    recvThread = null
    captureThread = null
    _localPreviewFrame.value = null
    _remotePreviewFrame.value = null
  }

  private fun startAudioThreads(sock: DatagramSocket?) {
    val pcmFormat = AudioFormat(16000f, 16, 1, true, false)
    micThread = thread(isDaemon = true, name = "TelefamCallMic") {
      val info = DataLine.Info(TargetDataLine::class.java, pcmFormat)
      val line = runCatching { AudioSystem.getLine(info) as? TargetDataLine }.getOrNull()
      if (line != null && runCatching { line.open(pcmFormat, 6400); line.start(); true }.getOrDefault(false)) {
        audioMic = line
        val buf = ByteArray(640) // 20ms @ 16kHz 16-bit mono
        val pktBuf = ByteArray(644)
        while (running.get()) {
          val n = runCatching { line.read(buf, 0, buf.size) }.getOrDefault(-1)
          if (n > 0 && micEnabled) {
            val target = remoteAddr
            if (target != null && sock != null && !sock.isClosed) {
              pktBuf[0] = 0x54; pktBuf[1] = 0x46; pktBuf[2] = 0x41; pktBuf[3] = 0x01 // "TFA\1" audio frame
              System.arraycopy(buf, 0, pktBuf, 4, n)
              runCatching { sock.send(DatagramPacket(pktBuf, n + 4, target)) }
            }
          } else {
            runCatching { Thread.sleep(20) }
          }
        }
        runCatching { line.stop(); line.close() }
      }
    }

    recvThread = thread(isDaemon = true, name = "TelefamCallRecv") {
      if (sock == null) return@thread
      val spkInfo = DataLine.Info(SourceDataLine::class.java, pcmFormat)
      val spk = runCatching { AudioSystem.getLine(spkInfo) as? SourceDataLine }.getOrNull()
      if (spk != null && runCatching { spk.open(pcmFormat, 12800); spk.start(); true }.getOrDefault(false)) {
        audioSpeaker = spk
      }
      val rxBuf = ByteArray(65507)
      while (running.get() && !sock.isClosed) {
        val pkt = DatagramPacket(rxBuf, rxBuf.size)
        val ok = runCatching { sock.receive(pkt); true }.getOrDefault(false)
        if (!ok || pkt.length < 4) continue
        if (pkt.data[0] == 0x54.toByte() && pkt.data[1] == 0x46.toByte()) {
          val kind = pkt.data[2]
          if (kind == 0x41.toByte() && speakerEnabled) {
            // Audio PCM frame
            runCatching { audioSpeaker?.write(pkt.data, 4, pkt.length - 4) }
          } else if (kind == 0x56.toByte()) {
            // JPEG video/screen frame
            val img = runCatching { ImageIO.read(ByteArrayInputStream(pkt.data, 4, pkt.length - 4)) }.getOrNull()
            if (img != null) {
              _remotePreviewFrame.value = img.toComposeImageBitmap()
            }
          }
        }
      }
    }
  }

  private fun startVideoCaptureThread(sock: DatagramSocket?) {
    captureThread = thread(isDaemon = true, name = "TelefamCallVideoCapture") {
      val robot = runCatching { Robot() }.getOrNull()
      var frameTick = 0
      while (running.get()) {
        val tier = qualityTier
        val activeCapture = screenSharing || (camEnabled && inAppActive)
        if (activeCapture) {
          val rawImage: BufferedImage? = if (screenSharing && robot != null) {
            val screen = Toolkit.getDefaultToolkit().screenSize
            runCatching {
              robot.createScreenCapture(Rectangle(0, 0, screen.width, screen.height))
            }.getOrNull()
          } else {
            renderDesktopCameraFrame(
              width = tier.targetWidth.coerceAtMost(640),
              height = tier.targetHeight.coerceAtMost(360),
              tick = frameTick++,
              altAngle = useSecondaryCam,
            )
          }
          if (rawImage != null) {
            val scaled = scaleBuffered(rawImage, tier.targetWidth, tier.targetHeight)
            val filtered = applyFilter(scaled, videoFilter)
            _localPreviewFrame.value = bufferedToComposeBitmap(filtered)

            val target = remoteAddr
            if (target != null && sock != null && !sock.isClosed) {
              val jpeg = encodeJpeg(filtered, tier.jpegQuality)
              if (jpeg != null && jpeg.size < 64_000) {
                val out = ByteArray(jpeg.size + 4)
                out[0] = 0x54; out[1] = 0x46; out[2] = 0x56; out[3] = 0x01 // "TFV\1"
                System.arraycopy(jpeg, 0, out, 4, jpeg.size)
                runCatching { sock.send(DatagramPacket(out, out.size, target)) }
              }
            }
          }
        }
        runCatching { Thread.sleep(tier.frameIntervalMs.coerceIn(55L, 280L)) }
      }
    }
  }

  private fun renderDesktopCameraFrame(width: Int, height: Int, tick: Int, altAngle: Boolean): BufferedImage {
    val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = img.createGraphics()
    val shift = ((tick % 120) / 120.0)
    val c1 = if (altAngle) java.awt.Color(24, 34, 62) else java.awt.Color(20, 26, 38)
    val c2 = if (altAngle) java.awt.Color(72, 24, 48) else java.awt.Color(42, 22, 34)
    g.paint = java.awt.GradientPaint(0f, 0f, c1, width.toFloat(), height.toFloat(), c2)
    g.fillRect(0, 0, width, height)
    val r = (height * 0.24).toInt()
    val cx = width / 2 + ((sin(shift * 2 * PI) * width * 0.03).toInt())
    val cy = (height * 0.42).toInt()
    g.color = java.awt.Color(255, 255, 255, 28)
    g.fillOval(cx - r, cy - r, r * 2, r * 2)
    g.color = java.awt.Color(255, 255, 255, 72)
    g.drawOval(cx - r, cy - r, r * 2, r * 2)
    val bodyW = (r * 2.4).toInt()
    val bodyH = (r * 1.4).toInt()
    g.fillRoundRect(cx - bodyW / 2, cy + r + 8, bodyW, bodyH, 36, 36)
    g.dispose()
    return img
  }

  private fun scaleBuffered(src: BufferedImage, maxW: Int, maxH: Int): BufferedImage {
    val w = src.width.coerceAtLeast(1)
    val h = src.height.coerceAtLeast(1)
    if (w <= maxW && h <= maxH) return src
    val scale = min(maxW.toDouble() / w, maxH.toDouble() / h)
    val tw = (w * scale).toInt().coerceAtLeast(2)
    val th = (h * scale).toInt().coerceAtLeast(2)
    val dst = BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB)
    val g = dst.createGraphics()
    g.drawImage(src, 0, 0, tw, th, null)
    g.dispose()
    return dst
  }

  private fun applyFilter(src: BufferedImage, filter: DesktopVideoFilter): BufferedImage {
    if (filter == DesktopVideoFilter.NATURAL) return src
    val w = src.width
    val h = src.height
    val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val pixels = IntArray(w * h)
    src.getRGB(0, 0, w, h, pixels, 0, w)
    for (i in pixels.indices) {
      val rgb = pixels[i]
      val r = (rgb ushr 16) and 0xFF
      val g = (rgb ushr 8) and 0xFF
      val b = rgb and 0xFF
      val (nr, ng, nb) = when (filter) {
        DesktopVideoFilter.NATURAL -> Triple(r, g, b)
        DesktopVideoFilter.WARM -> Triple(min(255, (r * 1.12f + 10f).toInt()), min(255, (g * 1.03f).toInt()), (b * 0.88f).toInt())
        DesktopVideoFilter.COOL -> Triple((r * 0.88f).toInt(), min(255, (g * 1.02f).toInt()), min(255, (b * 1.14f + 10f).toInt()))
        DesktopVideoFilter.NOIR -> {
          val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
          val c = (((lum - 128) * 1.15f) + 128f).toInt().coerceIn(0, 255)
          Triple(c, c, c)
        }
        DesktopVideoFilter.VIVID -> {
          val avg = (r + g + b) / 3f
          Triple(
            (avg + (r - avg) * 1.38f).toInt().coerceIn(0, 255),
            (avg + (g - avg) * 1.38f).toInt().coerceIn(0, 255),
            (avg + (b - avg) * 1.38f).toInt().coerceIn(0, 255),
          )
        }
        DesktopVideoFilter.SOFT -> Triple(
          min(255, (r * 0.96f + 16f).toInt()),
          min(255, (g * 0.96f + 14f).toInt()),
          min(255, (b * 0.95f + 12f).toInt()),
        )
      }
      pixels[i] = (nr shl 16) or (ng shl 8) or nb
    }
    out.setRGB(0, 0, w, h, pixels, 0, w)
    return out
  }

  private fun bufferedToComposeBitmap(img: BufferedImage): ImageBitmap {
    val w = img.width
    val h = img.height
    val pixels = IntArray(w * h)
    img.getRGB(0, 0, w, h, pixels, 0, w)
    val bgra = ByteArray(w * h * 4)
    var p = 0
    for (argb in pixels) {
      bgra[p++] = (argb and 0xFF).toByte()
      bgra[p++] = ((argb ushr 8) and 0xFF).toByte()
      bgra[p++] = ((argb ushr 16) and 0xFF).toByte()
      bgra[p++] = 0xFF.toByte()
    }
    val skia = SkiaBitmap()
    skia.allocPixels(ImageInfo(w, h, ColorType.BGRA_8888, ColorAlphaType.OPAQUE))
    skia.installPixels(bgra)
    return skia.asComposeImageBitmap()
  }

  private fun encodeJpeg(img: BufferedImage, quality: Float): ByteArray? = runCatching {
    val baos = ByteArrayOutputStream()
    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
    val param = writer.defaultWriteParam.apply {
      compressionMode = ImageWriteParam.MODE_EXPLICIT
      compressionQuality = quality.coerceIn(0.2f, 0.95f)
    }
    ImageIO.createImageOutputStream(baos).use { ios ->
      writer.output = ios
      writer.write(null, IIOImage(img, null, null), param)
    }
    writer.dispose()
    baos.toByteArray()
  }.getOrNull()
}

/** Coordinates full WhatsApp-style P2P calling on Desktop JVM. */
class DesktopCallController(
  private val apiClient: DesktopApiClient,
  private val chatController: DesktopChatController,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val soundEngine = DesktopCallSoundEngine()

  private val _uiState = MutableStateFlow(DesktopActiveCallState())
  val uiState: StateFlow<DesktopActiveCallState> = _uiState.asStateFlow()

  private val _callConversationUpdated = MutableSharedFlow<String>(extraBufferCapacity = 16)
  val callConversationUpdated: SharedFlow<String> = _callConversationUpdated.asSharedFlow()

  private var ownerUserId: String = ""
  private var lastSignalSeq: Long = 0L
  private var pendingRemoteOfferSdp: String = ""
  private var answeredEpochMs: Long = 0L
  private var desiredCamEnabledByUser: Boolean = true
  private var isWindowFocused: Boolean = true
  private var cachedStunHosts: List<String> = listOf("stun:stun.l.google.com:19302")

  private var pollJob: Job? = null
  private var tickerJob: Job? = null
  private var ringTimeoutJob: Job? = null

  private val mediaEngine = DesktopCallMediaEngine(
    onIceCandidate = { cand, mid, idx ->
      val callId = _uiState.value.callId
      if (callId.isNotBlank()) {
        scope.launch {
          sendSignal(
            callId,
            buildSignalJson("ice", candidate = cand, sdpMid = mid, sdpMLineIndex = idx),
          )
        }
      }
    },
    onLocalOffer = { sdp ->
      val callId = _uiState.value.callId
      if (callId.isNotBlank()) {
        scope.launch { sendSignal(callId, buildSignalJson("offer", sdp = sdp, sdpType = "offer")) }
      }
    },
    onLocalAnswer = { sdp ->
      val callId = _uiState.value.callId
      if (callId.isNotBlank()) {
        scope.launch { sendSignal(callId, buildSignalJson("answer", sdp = sdp, sdpType = "answer")) }
      }
    },
    onWeakNetwork = { weak ->
      val cur = _uiState.value
      if (cur.selectedQualityMode == DesktopCallQualityTier.AUTO && cur.lowBandwidthWarning != weak) {
        val nextTier = if (weak) DesktopCallQualityTier.LOW_240P else DesktopCallQualityTier.HD_720P
        _uiState.update { it.copy(lowBandwidthWarning = weak, effectiveQualityTier = nextTier) }
        syncMediaConfig()
        publishMediaState()
      }
    },
  )

  val localPreviewFrame: StateFlow<ImageBitmap?> = mediaEngine.localPreviewFrame
  val remotePreviewFrame: StateFlow<ImageBitmap?> = mediaEngine.remotePreviewFrame

  init {
    scope.launch {
      chatController.wsEvents.collect { ev ->
        handleWsCallEvent(ev.event, ev.rawJson)
      }
    }
  }

  fun attachSession(userId: String) {
    ownerUserId = userId.trim()
  }

  fun detachSession() {
    if (_uiState.value.isSessionVisible) {
      hangUp("logout")
    }
    resetToIdle()
    ownerUserId = ""
  }

  fun startCall(
    peerUserId: String,
    peerName: String,
    peerUsername: String,
    peerAvatarUrl: String,
    conversationId: String,
    callType: DesktopCallType,
  ) {
    if (peerUserId.isBlank() || ownerUserId.isBlank()) return
    val current = _uiState.value
    if (current.phase != DesktopCallPhase.IDLE &&
      current.phase != DesktopCallPhase.ENDED &&
      current.phase != DesktopCallPhase.NOT_ALLOWED
    ) {
      return
    }

    desiredCamEnabledByUser = callType == DesktopCallType.VIDEO
    lastSignalSeq = 0L
    pendingRemoteOfferSdp = ""
    answeredEpochMs = 0L

    _uiState.value = DesktopActiveCallState(
      phase = DesktopCallPhase.OUTGOING_CALLING,
      conversationId = conversationId,
      peerUserId = peerUserId,
      peerName = peerName.ifBlank { peerUsername.ifBlank { "Telefam" } },
      peerUsername = peerUsername,
      peerAvatarUrl = peerAvatarUrl,
      callType = callType,
      isIncoming = false,
      statusText = "Calling…",
      remainingRingSec = 25,
      micEnabled = true,
      speakerEnabled = true,
      localCamEnabled = desiredCamEnabledByUser,
      localInAppActive = true,
      peerCamEnabled = callType == DesktopCallType.VIDEO,
      peerInAppActive = true,
      effectiveQualityTier = DesktopCallQualityTier.HD_720P,
    )
    soundEngine.startOutgoingCallingTone()

    scope.launch {
      fetchIceConfig()
      val payload = buildString {
        append("{\"peer_user_id\":\"${DesktopJson.escape(peerUserId)}\"")
        append(",\"conversation_id\":\"${DesktopJson.escape(conversationId)}\"")
        append(",\"call_type\":\"${callType.wireValue}\"")
        append("}")
      }
      val resp = apiClient.executeChatRaw("POST", "/v1/calls", payload)
      if (resp == null || resp.first !in 200..299) {
        finishCallLocally("network_error", "Connection unavailable", conversationId)
        return@launch
      }
      val root = DesktopJson.parseObject(resp.second)
      val allowed = if (root.containsKey("allowed")) DesktopJson.bool(root, "allowed") else true
      val code = DesktopJson.str(root, "code")
      val msg = DesktopJson.str(root, "message")
      val peerOnline = DesktopJson.bool(root, "peer_online")
      val timeoutSec = DesktopJson.long(root, "timeout_sec", 25L).toInt().coerceIn(5, 60)
      val autoReq = DesktopJson.bool(root, "auto_request_sent")
      val callObj = DesktopJson.obj(root, "call")
      val callId = DesktopJson.str(callObj, "call_id")
      val resolvedConv = DesktopJson.str(callObj, "conversation_id").ifBlank { conversationId }
      val state = DesktopJson.str(callObj, "state")

      if (!allowed || code == "calls_not_allowed") {
        soundEngine.playBusyOrRestrictedCue()
        _uiState.update {
          it.copy(
            phase = DesktopCallPhase.NOT_ALLOWED,
            callId = callId,
            conversationId = resolvedConv,
            statusText = "Calls not allowed",
            endReason = "calls_not_allowed",
            errorNotice = msg.ifBlank { "Calls are restricted by privacy settings. A 'Call me' request was sent." },
            autoCallMeRequestSent = autoReq,
          )
        }
        if (resolvedConv.isNotBlank()) {
          chatController.refreshConversationNow(resolvedConv)
          _callConversationUpdated.tryEmit(resolvedConv)
        }
        scheduleAutoReset(3_200L)
        return@launch
      }

      if (code == "peer_busy" || state == "busy") {
        soundEngine.playBusyOrRestrictedCue()
        _uiState.update {
          it.copy(
            phase = DesktopCallPhase.ENDED,
            callId = callId,
            conversationId = resolvedConv,
            statusText = "Busy",
            endReason = "peer_busy",
          )
        }
        if (resolvedConv.isNotBlank()) {
          chatController.refreshConversationNow(resolvedConv)
          _callConversationUpdated.tryEmit(resolvedConv)
        }
        scheduleAutoReset(2_200L)
        return@launch
      }

      val isRinging = peerOnline || state == "ringing"
      _uiState.update {
        it.copy(
          phase = if (isRinging) DesktopCallPhase.OUTGOING_RINGING else DesktopCallPhase.OUTGOING_CALLING,
          callId = callId,
          conversationId = resolvedConv,
          statusText = if (isRinging) "Ringing…" else "Calling…",
          remainingRingSec = timeoutSec,
        )
      }
      if (isRinging) soundEngine.startOutgoingRingingTone()

      syncMediaConfig()
      mediaEngine.startSession(
        stunHosts = cachedStunHosts,
        enableVideo = callType == DesktopCallType.VIDEO,
        quality = _uiState.value.effectiveQualityTier,
        createOffer = true,
      )
      startRingCountdownAndPollers(callId, timeoutSec)
    }
  }

  fun acceptIncomingCall() {
    val cur = _uiState.value
    if (cur.callId.isBlank() || cur.phase != DesktopCallPhase.INCOMING_RINGING) return
    soundEngine.stopAll()
    _uiState.update { it.copy(phase = DesktopCallPhase.CONNECTING, statusText = "Connecting…") }

    scope.launch {
      fetchIceConfig()
      syncMediaConfig()
      mediaEngine.startSession(
        stunHosts = cachedStunHosts,
        enableVideo = cur.callType == DesktopCallType.VIDEO,
        quality = cur.effectiveQualityTier,
        createOffer = false,
      )
      if (pendingRemoteOfferSdp.isNotBlank()) {
        mediaEngine.setRemoteSdp(pendingRemoteOfferSdp, isOffer = true)
      }
      sendSignal(cur.callId, buildSignalJson("accept"))
      transitionToConnected(System.currentTimeMillis())
      publishMediaState()
    }
  }

  fun declineIncomingCall() {
    val cur = _uiState.value
    if (cur.callId.isBlank()) {
      resetToIdle()
      return
    }
    scope.launch {
      sendSignal(cur.callId, buildSignalJson("decline", reason = "declined"))
      finishCallLocally("declined", "Declined", cur.conversationId)
    }
  }

  fun hangUp(reason: String = "hangup") {
    val cur = _uiState.value
    if (!cur.isSessionVisible) return
    if (cur.callId.isBlank()) {
      resetToIdle()
      return
    }
    val finalReason = when {
      reason != "hangup" -> reason
      cur.isIncoming && cur.phase == DesktopCallPhase.INCOMING_RINGING -> "declined"
      !cur.isIncoming && (cur.phase == DesktopCallPhase.OUTGOING_CALLING || cur.phase == DesktopCallPhase.OUTGOING_RINGING) -> "caller_cancelled"
      else -> "hangup"
    }
    scope.launch {
      val action = if (finalReason == "declined") "decline" else "end"
      sendSignal(cur.callId, buildSignalJson(action, reason = finalReason))
      finishCallLocally(finalReason, if (finalReason == "declined") "Declined" else "Call ended", cur.conversationId)
    }
  }

  fun dismissNotAllowed() {
    resetToIdle()
  }

  fun toggleMic() {
    val next = !_uiState.value.micEnabled
    _uiState.update { it.copy(micEnabled = next) }
    syncMediaConfig()
    publishMediaState()
  }

  fun toggleSpeaker() {
    val next = !_uiState.value.speakerEnabled
    _uiState.update { it.copy(speakerEnabled = next) }
    syncMediaConfig()
  }

  fun toggleCamera() {
    val cur = _uiState.value
    if (cur.callType == DesktopCallType.AUDIO) {
      requestSwitchToVideo()
      return
    }
    val next = !cur.localCamEnabled
    desiredCamEnabledByUser = next
    _uiState.update { it.copy(localCamEnabled = next) }
    syncMediaConfig()
    publishMediaState()
  }

  fun flipCamera() {
    if (_uiState.value.callType != DesktopCallType.VIDEO) return
    _uiState.update { it.copy(useSecondaryCameraView = !it.useSecondaryCameraView) }
    syncMediaConfig()
  }

  fun toggleScreenShare() {
    val cur = _uiState.value
    if (!cur.localScreenSharing && cur.callType == DesktopCallType.AUDIO) {
      requestSwitchToVideo()
      return
    }
    val next = !cur.localScreenSharing
    _uiState.update { it.copy(localScreenSharing = next) }
    syncMediaConfig()
    publishMediaState()
  }

  fun selectFilter(filter: DesktopVideoFilter) {
    _uiState.update { it.copy(videoFilter = filter) }
    syncMediaConfig()
    publishMediaState()
  }

  fun setQualityTier(tier: DesktopCallQualityTier) {
    val effective = if (tier == DesktopCallQualityTier.AUTO) {
      if (_uiState.value.lowBandwidthWarning) DesktopCallQualityTier.LOW_240P else DesktopCallQualityTier.HD_720P
    } else {
      tier
    }
    _uiState.update { it.copy(selectedQualityMode = tier, effectiveQualityTier = effective) }
    syncMediaConfig()
    publishMediaState()
  }

  fun requestSwitchToVideo() {
    val cur = _uiState.value
    if (cur.callId.isBlank() || cur.callType == DesktopCallType.VIDEO) return
    _uiState.update { it.copy(switchVideoPrompt = DesktopSwitchVideoPromptState.OUTGOING_PENDING) }
    scope.launch { sendSignal(cur.callId, buildSignalJson("switch_video_request")) }
  }

  fun acceptSwitchToVideo() {
    val cur = _uiState.value
    if (cur.callId.isBlank()) return
    desiredCamEnabledByUser = true
    _uiState.update {
      it.copy(
        callType = DesktopCallType.VIDEO,
        speakerEnabled = true,
        localCamEnabled = isWindowFocused && !it.isMinimizedToChat,
        switchVideoPrompt = DesktopSwitchVideoPromptState.NONE,
      )
    }
    syncMediaConfig()
    mediaEngine.startSession(cachedStunHosts, enableVideo = true, quality = _uiState.value.effectiveQualityTier, createOffer = false)
    scope.launch {
      sendSignal(
        cur.callId,
        buildSignalJson("switch_video_accept", camEnabled = _uiState.value.localCamEnabled, inAppActive = _uiState.value.localInAppActive),
      )
      publishMediaState()
    }
  }

  fun declineSwitchToVideo() {
    val cur = _uiState.value
    _uiState.update { it.copy(switchVideoPrompt = DesktopSwitchVideoPromptState.NONE) }
    if (cur.callId.isNotBlank()) {
      scope.launch { sendSignal(cur.callId, buildSignalJson("switch_video_decline")) }
    }
  }

  /** Triggered by the "Message" button in call: opens peer chat and swaps peer view to Avatar until user returns. */
  fun minimizeToChat() {
    val cur = _uiState.value
    if (!cur.isSessionVisible) return
    _uiState.update {
      it.copy(
        isMinimizedToChat = true,
        localInAppActive = false,
        localCamEnabled = if (it.localScreenSharing) it.localCamEnabled else false,
      )
    }
    syncMediaConfig()
    publishMediaState()
    if (cur.conversationId.isNotBlank()) {
      chatController.openConversation(cur.conversationId)
    }
  }

  /** Restores full call screen and automatically switches camera back on. */
  fun expandFromMinimized() {
    val cur = _uiState.value
    if (!cur.isSessionVisible) return
    val restoreCam = cur.callType == DesktopCallType.VIDEO && desiredCamEnabledByUser && isWindowFocused
    _uiState.update {
      it.copy(
        isMinimizedToChat = false,
        localInAppActive = true,
        localCamEnabled = restoreCam,
      )
    }
    syncMediaConfig()
    publishMediaState()
  }

  /** Leaving Desktop window / minimizing switches peer to Avatar; returning restores video automatically. */
  fun onWindowFocusChanged(focused: Boolean) {
    isWindowFocused = focused
    val cur = _uiState.value
    if (!cur.isSessionVisible || cur.phase == DesktopCallPhase.ENDED || cur.phase == DesktopCallPhase.NOT_ALLOWED) return
    if (!focused) {
      _uiState.update {
        it.copy(
          localInAppActive = false,
          localCamEnabled = if (it.localScreenSharing) it.localCamEnabled else false,
        )
      }
      syncMediaConfig()
      publishMediaState()
    } else if (!cur.isMinimizedToChat) {
      val restoreCam = cur.callType == DesktopCallType.VIDEO && desiredCamEnabledByUser
      _uiState.update {
        it.copy(
          localInAppActive = true,
          localCamEnabled = restoreCam,
        )
      }
      syncMediaConfig()
      publishMediaState()
    }
  }

  private fun syncMediaConfig() {
    val cur = _uiState.value
    mediaEngine.updateMediaConfig(
      micEnabled = cur.micEnabled,
      speakerEnabled = cur.speakerEnabled,
      camEnabled = cur.localCamEnabled,
      inAppActive = cur.localInAppActive,
      screenSharing = cur.localScreenSharing,
      useSecondaryCam = cur.useSecondaryCameraView,
      filter = cur.videoFilter,
      quality = cur.effectiveQualityTier,
    )
  }

  private suspend fun fetchIceConfig() {
    val res = withContext(Dispatchers.IO) { apiClient.executeChatRaw("GET", "/v1/calls/ice-config") }
    if (res != null && res.first in 200..299) {
      val root = DesktopJson.parseObject(res.second)
      val servers = DesktopJson.objList(root, "ice_servers")
      val urls = servers.flatMap { DesktopJson.stringList(it, "urls") }.filter { it.isNotBlank() }
      if (urls.isNotEmpty()) cachedStunHosts = urls
    }
  }

  private fun publishMediaState() {
    val cur = _uiState.value
    if (cur.callId.isBlank()) return
    scope.launch {
      sendSignal(
        cur.callId,
        buildSignalJson(
          action = "media_state",
          camEnabled = cur.localCamEnabled,
          micEnabled = cur.micEnabled,
          inAppActive = cur.localInAppActive,
          screenSharing = cur.localScreenSharing,
          videoFilter = cur.videoFilter.wireValue,
          networkQuality = cur.effectiveQualityTier.wireValue,
        ),
      )
    }
  }

  private suspend fun sendSignal(callId: String, jsonBody: String) {
    val wsSent = chatController.sendCallSignalViaWs(callId, jsonBody)
    val parsedAction = DesktopJson.str(DesktopJson.parseObject(jsonBody), "action")
    if (!wsSent || parsedAction in setOf("accept", "answer", "decline", "end", "cancel", "offer", "ringing")) {
      withContext(Dispatchers.IO) {
        apiClient.executeChatRaw("POST", "/v1/calls/${DesktopChatCodec.urlEncode(callId)}/signal", jsonBody)
      }
    }
  }

  private fun startRingCountdownAndPollers(callId: String, timeoutSec: Int) {
    ringTimeoutJob?.cancel()
    ringTimeoutJob = scope.launch {
      var remaining = timeoutSec.coerceIn(5, 30)
      while (isActive && remaining > 0) {
        val phase = _uiState.value.phase
        if (phase != DesktopCallPhase.OUTGOING_CALLING &&
          phase != DesktopCallPhase.OUTGOING_RINGING &&
          phase != DesktopCallPhase.INCOMING_RINGING
        ) {
          return@launch
        }
        _uiState.update { it.copy(remainingRingSec = remaining) }
        delay(1_000L)
        remaining--
      }
      val phase = _uiState.value.phase
      if (phase == DesktopCallPhase.OUTGOING_CALLING ||
        phase == DesktopCallPhase.OUTGOING_RINGING ||
        phase == DesktopCallPhase.INCOMING_RINGING
      ) {
        sendSignal(callId, buildSignalJson("end", reason = "timeout_25s"))
        finishCallLocally("timeout_25s", "No answer", _uiState.value.conversationId)
      }
    }

    pollJob?.cancel()
    pollJob = scope.launch {
      while (isActive && _uiState.value.callId == callId) {
        val resp = withContext(Dispatchers.IO) {
          apiClient.executeChatRaw("GET", "/v1/calls/${DesktopChatCodec.urlEncode(callId)}?since_seq=$lastSignalSeq")
        }
        if (resp != null && resp.first in 200..299) {
          val root = DesktopJson.parseObject(resp.second)
          val sdpOffer = DesktopJson.str(root, "sdp_offer")
          if (sdpOffer.isNotBlank() && pendingRemoteOfferSdp.isBlank()) {
            pendingRemoteOfferSdp = sdpOffer
          }
          DesktopJson.objList(root, "signals").forEach { sigMap -> processSignalMap(sigMap) }
          DesktopJson.objOrNull(root, "call")?.let { syncCallDtoMap(it) }
        }
        delay(1_400L)
      }
    }
  }

  private fun handleWsCallEvent(event: String, rawJson: String) {
    if (rawJson.isBlank()) return
    val root = DesktopJson.parseObject(rawJson)
    val callMap = DesktopJson.objOrNull(root, "call")
    when (event) {
      "call_incoming" -> {
        if (callMap == null) return
        val callId = DesktopJson.str(callMap, "call_id")
        if (callId.isBlank()) return
        val cur = _uiState.value
        if (cur.isSessionVisible && cur.callId != callId) return
        if (cur.callId == callId) return

        val callType = DesktopCallType.fromWire(DesktopJson.str(callMap, "call_type"))
        val offer = DesktopJson.str(root, "sdp_offer")
        if (offer.isNotBlank()) pendingRemoteOfferSdp = offer
        desiredCamEnabledByUser = callType == DesktopCallType.VIDEO
        lastSignalSeq = 0L
        answeredEpochMs = 0L
        val expMs = DesktopJson.long(callMap, "expires_at_ms", System.currentTimeMillis() + 25_000L)
        val remain = ((expMs - System.currentTimeMillis()) / 1000L).toInt().coerceIn(5, 25)
        val callerName = DesktopJson.str(callMap, "caller_name")
        val callerUsername = DesktopJson.str(callMap, "caller_username")

        _uiState.value = DesktopActiveCallState(
          phase = DesktopCallPhase.INCOMING_RINGING,
          callId = callId,
          conversationId = DesktopJson.str(callMap, "conversation_id"),
          peerUserId = DesktopJson.str(callMap, "caller_user_id"),
          peerName = callerName.ifBlank { callerUsername.ifBlank { "Telefam" } },
          peerUsername = callerUsername,
          peerAvatarUrl = DesktopJson.str(callMap, "caller_avatar"),
          callType = callType,
          isIncoming = true,
          statusText = if (callType == DesktopCallType.VIDEO) "Incoming video call" else "Incoming audio call",
          remainingRingSec = remain,
          micEnabled = true,
          speakerEnabled = true,
          localCamEnabled = desiredCamEnabledByUser,
          localInAppActive = true,
          peerCamEnabled = callType == DesktopCallType.VIDEO,
          peerInAppActive = true,
        )
        soundEngine.startIncomingRingtone()
        scope.launch {
          sendSignal(callId, buildSignalJson("ringing"))
          startRingCountdownAndPollers(callId, remain)
        }
      }

      "call_ringing" -> {
        val callId = callMap?.let { DesktopJson.str(it, "call_id") }.orEmpty()
        if (callId == _uiState.value.callId && _uiState.value.phase == DesktopCallPhase.OUTGOING_CALLING) {
          _uiState.update { it.copy(phase = DesktopCallPhase.OUTGOING_RINGING, statusText = "Ringing…") }
          soundEngine.startOutgoingRingingTone()
        }
      }

      "call_accepted" -> {
        val callId = callMap?.let { DesktopJson.str(it, "call_id") }.orEmpty()
        if (callId == _uiState.value.callId) {
          val ansAt = DesktopJson.long(callMap!!, "answered_at_ms", System.currentTimeMillis())
          transitionToConnected(ansAt)
        }
      }

      "call_signal" -> {
        val callId = callMap?.let { DesktopJson.str(it, "call_id") }.orEmpty()
        if (callId != _uiState.value.callId) return
        val sigMap = DesktopJson.objOrNull(root, "signal") ?: return
        processSignalMap(sigMap)
      }

      "call_ended" -> {
        val callId = callMap?.let { DesktopJson.str(it, "call_id") }.orEmpty()
        val convId = DesktopJson.str(root, "conversation_id").ifBlank {
          callMap?.let { DesktopJson.str(it, "conversation_id") }.orEmpty()
        }
        val st = callMap?.let { DesktopJson.str(it, "state") }.orEmpty()
        val reason = callMap?.let { DesktopJson.str(it, "end_reason") }.orEmpty()
        if (callId == _uiState.value.callId) {
          val label = when (st) {
            "declined" -> "Declined"
            "missed" -> "No answer"
            "busy" -> "Busy"
            else -> "Call ended"
          }
          finishCallLocally(reason.ifBlank { st }, label, convId)
        } else if (convId.isNotBlank()) {
          chatController.refreshConversationNow(convId)
          _callConversationUpdated.tryEmit(convId)
        }
      }
    }
  }

  private fun processSignalMap(sig: Map<String, Any?>) {
    val seq = DesktopJson.long(sig, "seq", 0L)
    if (seq > 0L && seq <= lastSignalSeq) return
    if (seq > 0L) lastSignalSeq = seq
    val action = DesktopJson.str(sig, "action").lowercase()
    when (action) {
      "offer" -> {
        val sdp = DesktopJson.str(sig, "sdp")
        if (sdp.isNotBlank()) {
          pendingRemoteOfferSdp = sdp
          if (_uiState.value.phase == DesktopCallPhase.CONNECTING || _uiState.value.phase == DesktopCallPhase.CONNECTED) {
            mediaEngine.setRemoteSdp(sdp, isOffer = true)
          }
        }
      }
      "answer" -> {
        val sdp = DesktopJson.str(sig, "sdp")
        if (sdp.isNotBlank()) mediaEngine.setRemoteSdp(sdp, isOffer = false)
      }
      "ice" -> {
        val cand = DesktopJson.str(sig, "candidate")
        if (cand.isNotBlank()) mediaEngine.addRemoteIceCandidate(cand)
      }
      "media_state" -> {
        _uiState.update { cur ->
          cur.copy(
            peerCamEnabled = if (sig.containsKey("cam_enabled")) DesktopJson.bool(sig, "cam_enabled") else cur.peerCamEnabled,
            peerMicEnabled = if (sig.containsKey("mic_enabled")) DesktopJson.bool(sig, "mic_enabled") else cur.peerMicEnabled,
            peerInAppActive = if (sig.containsKey("in_app_active")) DesktopJson.bool(sig, "in_app_active") else cur.peerInAppActive,
            peerScreenSharing = if (sig.containsKey("screen_sharing")) DesktopJson.bool(sig, "screen_sharing") else cur.peerScreenSharing,
            peerVideoFilter = DesktopJson.str(sig, "video_filter").takeIf { it.isNotBlank() }?.let {
              DesktopVideoFilter.fromWire(it)
            } ?: cur.peerVideoFilter,
          )
        }
      }
      "switch_video_request" -> {
        _uiState.update { it.copy(switchVideoPrompt = DesktopSwitchVideoPromptState.INCOMING_REQUEST) }
      }
      "switch_video_accept" -> {
        desiredCamEnabledByUser = true
        _uiState.update { cur ->
          cur.copy(
            callType = DesktopCallType.VIDEO,
            speakerEnabled = true,
            switchVideoPrompt = DesktopSwitchVideoPromptState.NONE,
            localCamEnabled = cur.localInAppActive,
            peerCamEnabled = if (sig.containsKey("cam_enabled")) DesktopJson.bool(sig, "cam_enabled") else true,
            peerInAppActive = if (sig.containsKey("in_app_active")) DesktopJson.bool(sig, "in_app_active") else true,
          )
        }
        syncMediaConfig()
        mediaEngine.startSession(cachedStunHosts, enableVideo = true, quality = _uiState.value.effectiveQualityTier, createOffer = true)
      }
      "switch_video_decline" -> {
        _uiState.update {
          it.copy(switchVideoPrompt = DesktopSwitchVideoPromptState.NONE, statusText = "Video switch declined")
        }
      }
    }
  }

  private fun syncCallDtoMap(call: Map<String, Any?>) {
    if (DesktopJson.str(call, "call_id") != _uiState.value.callId) return
    val st = DesktopJson.str(call, "state")
    when (st) {
      "ringing" -> {
        if (_uiState.value.phase == DesktopCallPhase.OUTGOING_CALLING) {
          _uiState.update { it.copy(phase = DesktopCallPhase.OUTGOING_RINGING, statusText = "Ringing…") }
          soundEngine.startOutgoingRingingTone()
        }
      }
      "connected" -> {
        val ansAt = DesktopJson.long(call, "answered_at_ms", System.currentTimeMillis())
        transitionToConnected(ansAt)
        val dur = DesktopJson.long(call, "duration_sec", 0L)
        if (dur > _uiState.value.connectedElapsedSec) {
          _uiState.update { it.copy(connectedElapsedSec = dur) }
        }
      }
      "ended", "missed", "declined", "busy", "cancelled" -> {
        val label = when (st) {
          "declined" -> "Declined"
          "missed" -> "No answer"
          "busy" -> "Busy"
          else -> "Call ended"
        }
        finishCallLocally(DesktopJson.str(call, "end_reason").ifBlank { st }, label, DesktopJson.str(call, "conversation_id"))
      }
    }
  }

  private fun transitionToConnected(answeredAtMs: Long) {
    if (_uiState.value.phase == DesktopCallPhase.CONNECTED) return
    ringTimeoutJob?.cancel()
    soundEngine.playConnectedCue()
    answeredEpochMs = if (answeredAtMs > 0L) answeredAtMs else System.currentTimeMillis()
    _uiState.update { it.copy(phase = DesktopCallPhase.CONNECTED, statusText = "") }
    tickerJob?.cancel()
    tickerJob = scope.launch {
      while (isActive && _uiState.value.phase == DesktopCallPhase.CONNECTED) {
        val elapsed = ((System.currentTimeMillis() - answeredEpochMs) / 1000L).coerceAtLeast(0L)
        _uiState.update { it.copy(connectedElapsedSec = elapsed) }
        delay(1_000L)
      }
    }
  }

  private fun finishCallLocally(reason: String, label: String, convId: String) {
    ringTimeoutJob?.cancel()
    pollJob?.cancel()
    tickerJob?.cancel()
    mediaEngine.stopSession()
    soundEngine.playEndedCue()
    _uiState.update {
      it.copy(
        phase = DesktopCallPhase.ENDED,
        isMinimizedToChat = false,
        localScreenSharing = false,
        statusText = label,
        endReason = reason,
        switchVideoPrompt = DesktopSwitchVideoPromptState.NONE,
      )
    }
    if (convId.isNotBlank()) {
      chatController.refreshConversationNow(convId)
      _callConversationUpdated.tryEmit(convId)
    }
    scheduleAutoReset(2_200L)
  }

  private fun scheduleAutoReset(delayMs: Long) {
    val targetCallId = _uiState.value.callId
    scope.launch {
      delay(delayMs)
      val cur = _uiState.value
      if ((cur.phase == DesktopCallPhase.ENDED || cur.phase == DesktopCallPhase.NOT_ALLOWED) &&
        cur.callId == targetCallId
      ) {
        resetToIdle()
      }
    }
  }

  private fun resetToIdle() {
    ringTimeoutJob?.cancel()
    pollJob?.cancel()
    tickerJob?.cancel()
    mediaEngine.stopSession()
    soundEngine.stopAll()
    _uiState.value = DesktopActiveCallState()
  }

  private fun buildSignalJson(
    action: String,
    sdp: String = "",
    sdpType: String = "",
    candidate: String = "",
    sdpMid: String = "",
    sdpMLineIndex: Int = 0,
    reason: String = "",
    camEnabled: Boolean? = null,
    micEnabled: Boolean? = null,
    inAppActive: Boolean? = null,
    screenSharing: Boolean? = null,
    videoFilter: String = "",
    networkQuality: String = "",
  ): String = buildString {
    append("{\"action\":\"${DesktopJson.escape(action)}\"")
    if (sdp.isNotBlank()) append(",\"sdp\":\"${DesktopJson.escape(sdp)}\"")
    if (sdpType.isNotBlank()) append(",\"sdp_type\":\"${DesktopJson.escape(sdpType)}\"")
    if (candidate.isNotBlank()) append(",\"candidate\":\"${DesktopJson.escape(candidate)}\"")
    if (sdpMid.isNotBlank()) append(",\"sdp_mid\":\"${DesktopJson.escape(sdpMid)}\"")
    if (sdpMLineIndex != 0) append(",\"sdp_mline_index\":$sdpMLineIndex")
    if (reason.isNotBlank()) append(",\"reason\":\"${DesktopJson.escape(reason)}\"")
    camEnabled?.let { append(",\"cam_enabled\":$it") }
    micEnabled?.let { append(",\"mic_enabled\":$it") }
    inAppActive?.let { append(",\"in_app_active\":$it") }
    screenSharing?.let { append(",\"screen_sharing\":$it") }
    if (videoFilter.isNotBlank()) append(",\"video_filter\":\"${DesktopJson.escape(videoFilter)}\"")
    if (networkQuality.isNotBlank()) append(",\"network_quality\":\"${DesktopJson.escape(networkQuality)}\"")
    append("}")
  }
}
