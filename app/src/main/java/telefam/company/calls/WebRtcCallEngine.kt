package telefam.company.calls

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.CapturerObserver
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.JavaI420Buffer
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import org.webrtc.RtpParameters
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoFrame
import org.webrtc.VideoProcessor
import org.webrtc.VideoSink
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule
import telefam.company.data.network.IceServerDto
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real-time I420 frame filter applied to outgoing local camera VideoSource so both
 * local SurfaceViewRenderer preview and remote peer WebRTC stream receive the chosen filter.
 */
class TelefamVideoFilterProcessor : VideoProcessor {
  @Volatile
  var activeFilter: VideoCallFilter = VideoCallFilter.NATURAL

  @Volatile
  private var targetSink: VideoSink? = null

  override fun onCapturerStarted(success: Boolean) = Unit
  override fun onCapturerStopped() = Unit

  override fun setSink(sink: VideoSink?) {
    targetSink = sink
  }

  override fun onFrameCaptured(frame: VideoFrame) {
    val sink = targetSink ?: return
    val filter = activeFilter
    if (filter == VideoCallFilter.NATURAL) {
      sink.onFrame(frame)
      return
    }
    val i420 = frame.buffer.toI420()
    if (i420 == null) {
      sink.onFrame(frame)
      return
    }
    try {
      val width = i420.width
      val height = i420.height
      val out = JavaI420Buffer.allocate(width, height)
      transformI420(
        filter = filter,
        width = width,
        height = height,
        srcY = i420.dataY,
        srcU = i420.dataU,
        srcV = i420.dataV,
        srcStrideY = i420.strideY,
        srcStrideU = i420.strideU,
        srcStrideV = i420.strideV,
        dstY = out.dataY,
        dstU = out.dataU,
        dstV = out.dataV,
        dstStrideY = out.strideY,
        dstStrideU = out.strideU,
        dstStrideV = out.strideV,
      )
      val filteredFrame = VideoFrame(out, frame.rotation, frame.timestampNs)
      sink.onFrame(filteredFrame)
      filteredFrame.release()
    } finally {
      i420.release()
    }
  }

  private fun transformI420(
    filter: VideoCallFilter,
    width: Int,
    height: Int,
    srcY: ByteBuffer,
    srcU: ByteBuffer,
    srcV: ByteBuffer,
    srcStrideY: Int,
    srcStrideU: Int,
    srcStrideV: Int,
    dstY: ByteBuffer,
    dstU: ByteBuffer,
    dstV: ByteBuffer,
    dstStrideY: Int,
    dstStrideU: Int,
    dstStrideV: Int,
  ) {
    for (row in 0 until height) {
      val sOff = row * srcStrideY
      val dOff = row * dstStrideY
      for (col in 0 until width) {
        val yVal = srcY.get(sOff + col).toInt() and 0xFF
        val outY = when (filter) {
          VideoCallFilter.NOIR -> ((yVal - 128) * 1.15f + 128).toInt().coerceIn(0, 255)
          VideoCallFilter.SOFT -> (yVal * 1.04f + 8f).toInt().coerceIn(0, 255)
          VideoCallFilter.VIVID -> ((yVal - 128) * 1.08f + 131).toInt().coerceIn(0, 255)
          VideoCallFilter.WARM -> (yVal + 4).coerceIn(0, 255)
          VideoCallFilter.COOL -> (yVal - 2).coerceIn(0, 255)
          VideoCallFilter.NATURAL -> yVal
        }
        dstY.put(dOff + col, outY.toByte())
      }
    }

    val chromaW = (width + 1) / 2
    val chromaH = (height + 1) / 2
    for (row in 0 until chromaH) {
      val suOff = row * srcStrideU
      val svOff = row * srcStrideV
      val duOff = row * dstStrideU
      val dvOff = row * dstStrideV
      for (col in 0 until chromaW) {
        val uVal = (srcU.get(suOff + col).toInt() and 0xFF) - 128
        val vVal = (srcV.get(svOff + col).toInt() and 0xFF) - 128
        val (nu, nv) = when (filter) {
          VideoCallFilter.NOIR -> 128 to 128
          VideoCallFilter.WARM -> (128 + uVal - 10) to (128 + vVal + 14)
          VideoCallFilter.COOL -> (128 + uVal + 14) to (128 + vVal - 10)
          VideoCallFilter.VIVID -> (128 + (uVal * 1.32f).toInt()) to (128 + (vVal * 1.32f).toInt())
          VideoCallFilter.SOFT -> (128 + (uVal * 0.85f).toInt()) to (128 + (vVal * 0.85f).toInt())
          VideoCallFilter.NATURAL -> (128 + uVal) to (128 + vVal)
        }
        dstU.put(duOff + col, nu.coerceIn(0, 255).toByte())
        dstV.put(dvOff + col, nv.coerceIn(0, 255).toByte())
      }
    }
  }
}

interface WebRtcSignalingCallbacks {
  fun onLocalSdpCreated(type: String, sdp: String)
  fun onLocalIceCandidate(candidate: String, sdpMid: String, sdpMLineIndex: Int)
  fun onIceConnectionStateChanged(state: PeerConnection.IceConnectionState)
  fun onStatsSample(packetLossPct: Float, rttMs: Double)
  fun onScreenShareStoppedBySystem()
}

/**
 * Encapsulates real Stream WebRTC PeerConnection, hardware AEC/NS audio, Camera2 capture,
 * live I420 video filters, MediaProjection screen sharing, adaptive bitrate/resolution,
 * and automatic ICE restarts. Degrades gracefully on non-native JVM unit tests.
 */
class WebRtcCallEngine(
  private val appContext: Context,
  private val callbacks: WebRtcSignalingCallbacks,
) {
  private val initialized = AtomicBoolean(false)
  private val nativeAvailable = AtomicBoolean(true)

  var eglBase: EglBase? = null
    private set
  private var factory: PeerConnectionFactory? = null
  private var peerConnection: PeerConnection? = null
  private var audioSource: AudioSource? = null
  private var localAudioTrack: AudioTrack? = null
  private var videoSource: VideoSource? = null
  private var localVideoTrack: VideoTrack? = null
  private var remoteVideoTrack: VideoTrack? = null

  private var surfaceTextureHelper: SurfaceTextureHelper? = null
  private var cameraCapturer: CameraVideoCapturer? = null
  private var screenCapturer: VideoCapturer? = null
  private val filterProcessor = TelefamVideoFilterProcessor()

  private var isFrontCamera = true
  private var isScreenSharing = false
  private var hasRemoteDescription = false
  private val pendingRemoteIce = CopyOnWriteArrayList<IceCandidate>()
  private var currentQuality = CallQualityTier.HD_720P

  private var localRenderer: SurfaceViewRenderer? = null
  private var remoteRenderer: SurfaceViewRenderer? = null

  fun initEngine(): Boolean {
    if (initialized.get()) return nativeAvailable.get()
    val ok = runCatching {
      PeerConnectionFactory.initialize(
        PeerConnectionFactory.InitializationOptions.builder(appContext)
          .setEnableInternalTracer(false)
          .createInitializationOptions(),
      )
      val egl = EglBase.create()
      eglBase = egl
      val adm = JavaAudioDeviceModule.builder(appContext)
        .setUseHardwareAcousticEchoCanceler(true)
        .setUseHardwareNoiseSuppressor(true)
        .createAudioDeviceModule()
      factory = PeerConnectionFactory.builder()
        .setAudioDeviceModule(adm)
        .setVideoEncoderFactory(DefaultVideoEncoderFactory(egl.eglBaseContext, true, true))
        .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl.eglBaseContext))
        .createPeerConnectionFactory()
      adm.release()
      true
    }.getOrElse {
      nativeAvailable.set(false)
      false
    }
    initialized.set(true)
    return ok
  }

  fun startSession(
    iceServers: List<IceServerDto>,
    enableVideo: Boolean,
    quality: CallQualityTier,
  ) {
    currentQuality = quality
    if (!initEngine()) return
    val pcf = factory ?: return

    val rtcIce = if (iceServers.isEmpty()) {
      listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
    } else {
      iceServers.flatMap { srv ->
        srv.urls.filter { it.isNotBlank() }.map { url ->
          PeerConnection.IceServer.builder(url).apply {
            if (srv.username.isNotBlank()) setUsername(srv.username)
            if (srv.credential.isNotBlank()) setPassword(srv.credential)
          }.createIceServer()
        }
      }
    }

    val rtcConfig = PeerConnection.RTCConfiguration(rtcIce).apply {
      sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
      continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
      iceTransportsType = PeerConnection.IceTransportsType.ALL
    }

    hasRemoteDescription = false
    pendingRemoteIce.clear()

    val audioConstraints = MediaConstraints().apply {
      mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
      mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
      mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
    }
    audioSource = pcf.createAudioSource(audioConstraints)
    localAudioTrack = pcf.createAudioTrack("telefam_audio_0", audioSource)?.apply {
      setEnabled(true)
    }

    val pc = pcf.createPeerConnection(
      rtcConfig,
      object : PeerConnection.Observer {
        override fun onSignalingChange(newState: PeerConnection.SignalingState?) = Unit
        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
          if (newState != null) {
            callbacks.onIceConnectionStateChanged(newState)
          }
        }
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) = Unit
        override fun onIceCandidate(candidate: IceCandidate?) {
          if (candidate != null) {
            callbacks.onLocalIceCandidate(
              candidate.sdp,
              candidate.sdpMid.orEmpty(),
              candidate.sdpMLineIndex,
            )
          }
        }
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
        override fun onAddStream(stream: MediaStream?) {
          val vt = stream?.videoTracks?.firstOrNull() ?: return
          attachRemoteVideoTrack(vt)
        }
        override fun onRemoveStream(stream: MediaStream?) = Unit
        override fun onDataChannel(dataChannel: DataChannel?) = Unit
        override fun onRenegotiationNeeded() = Unit
        override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
          val track = receiver?.track()
          if (track is VideoTrack) {
            attachRemoteVideoTrack(track)
          }
        }
        override fun onTrack(transceiver: RtpTransceiver?) {
          val track = transceiver?.receiver?.track()
          if (track is VideoTrack) {
            attachRemoteVideoTrack(track)
          }
        }
      },
    ) ?: return

    peerConnection = pc
    localAudioTrack?.let { pc.addTrack(it, listOf("telefam_stream")) }

    // Always pre-attach a video track transceiver so live Audio->Video upgrades work smoothly
    prepareVideoTrack(enableImmediately = enableVideo)
    applyQualityParameters(quality)
  }

  private fun prepareVideoTrack(enableImmediately: Boolean) {
    val pcf = factory ?: return
    val egl = eglBase ?: return
    val pc = peerConnection ?: return
    if (videoSource == null) {
      videoSource = pcf.createVideoSource(false).apply {
        setVideoProcessor(filterProcessor)
      }
      surfaceTextureHelper = SurfaceTextureHelper.create("TelefamCaptureThread", egl.eglBaseContext)
      localVideoTrack = pcf.createVideoTrack("telefam_video_0", videoSource)?.apply {
        setEnabled(enableImmediately)
        localRenderer?.let { addSink(it) }
      }
      localVideoTrack?.let { pc.addTrack(it, listOf("telefam_stream")) }
    } else {
      localVideoTrack?.setEnabled(enableImmediately)
    }

    if (enableImmediately && !isScreenSharing) {
      ensureCameraStarted()
    }
  }

  private fun ensureCameraStarted() {
    val vs = videoSource ?: return
    val sth = surfaceTextureHelper ?: return
    if (cameraCapturer == null) {
      val enumerator = Camera2Enumerator(appContext)
      val deviceNames = enumerator.deviceNames
      val preferred = deviceNames.firstOrNull {
        if (isFrontCamera) enumerator.isFrontFacing(it) else enumerator.isBackFacing(it)
      } ?: deviceNames.firstOrNull()
      if (preferred != null) {
        cameraCapturer = enumerator.createCapturer(preferred, null)
        runCatching {
          cameraCapturer?.initialize(sth, appContext, vs.capturerObserver)
          cameraCapturer?.startCapture(
            currentQuality.targetWidth,
            currentQuality.targetHeight,
            currentQuality.targetFps,
          )
        }
      }
    } else {
      runCatching {
        cameraCapturer?.startCapture(
          currentQuality.targetWidth,
          currentQuality.targetHeight,
          currentQuality.targetFps,
        )
      }
    }
  }

  fun createOffer(iceRestart: Boolean = false) {
    val pc = peerConnection ?: return
    val constraints = MediaConstraints().apply {
      mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
      mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
      if (iceRestart) {
        mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
      }
    }
    pc.createOffer(
      object : SimpleSdpObserver() {
        override fun onCreateSuccess(desc: SessionDescription?) {
          if (desc == null) return
          pc.setLocalDescription(
            object : SimpleSdpObserver() {
              override fun onSetSuccess() {
                callbacks.onLocalSdpCreated("offer", desc.description)
              }
            },
            desc,
          )
        }
      },
      constraints,
    )
  }

  fun handleRemoteOfferAndCreateAnswer(offerSdp: String) {
    val pc = peerConnection ?: return
    val sdp = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
    pc.setRemoteDescription(
      object : SimpleSdpObserver() {
        override fun onSetSuccess() {
          hasRemoteDescription = true
          flushPendingIce()
          val answerConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
          }
          pc.createAnswer(
            object : SimpleSdpObserver() {
              override fun onCreateSuccess(answerDesc: SessionDescription?) {
                if (answerDesc == null) return
                pc.setLocalDescription(
                  object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                      callbacks.onLocalSdpCreated("answer", answerDesc.description)
                    }
                  },
                  answerDesc,
                )
              }
            },
            answerConstraints,
          )
        }
      },
      sdp,
    )
  }

  fun handleRemoteAnswer(answerSdp: String) {
    val pc = peerConnection ?: return
    val sdp = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
    pc.setRemoteDescription(
      object : SimpleSdpObserver() {
        override fun onSetSuccess() {
          hasRemoteDescription = true
          flushPendingIce()
        }
      },
      sdp,
    )
  }

  fun addRemoteIceCandidate(candidateSdp: String, sdpMid: String, sdpMLineIndex: Int) {
    if (candidateSdp.isBlank()) return
    val candidate = IceCandidate(sdpMid.ifBlank { "0" }, sdpMLineIndex, candidateSdp)
    if (!hasRemoteDescription) {
      pendingRemoteIce.add(candidate)
    } else {
      runCatching { peerConnection?.addIceCandidate(candidate) }
    }
  }

  private fun flushPendingIce() {
    val pc = peerConnection ?: return
    val list = pendingRemoteIce.toList()
    pendingRemoteIce.clear()
    list.forEach { runCatching { pc.addIceCandidate(it) } }
  }

  fun setMicEnabled(enabled: Boolean) {
    runCatching { localAudioTrack?.setEnabled(enabled) }
  }

  fun setCameraEnabled(enabled: Boolean) {
    if (isScreenSharing) return
    if (enabled) {
      prepareVideoTrack(enableImmediately = true)
    } else {
      runCatching {
        localVideoTrack?.setEnabled(false)
        cameraCapturer?.stopCapture()
      }
    }
  }

  fun flipCamera() {
    if (isScreenSharing) return
    isFrontCamera = !isFrontCamera
    runCatching {
      cameraCapturer?.switchCamera(null)
    }
  }

  fun setVideoFilter(filter: VideoCallFilter) {
    filterProcessor.activeFilter = filter
  }

  fun applyQualityParameters(tier: CallQualityTier) {
    currentQuality = tier
    val pc = peerConnection ?: return
    val videoSender = pc.senders.firstOrNull { it.track()?.kind() == MediaStreamTrack.VIDEO_TRACK_KIND }
    if (videoSender != null) {
      val params = videoSender.parameters
      params.degradationPreference = RtpParameters.DegradationPreference.MAINTAIN_FRAMERATE
      params.encodings.forEach { enc ->
        enc.maxBitrateBps = tier.maxBitrateBps
        enc.maxFramerate = tier.targetFps
        enc.minBitrateBps = 70_000
      }
      runCatching { videoSender.parameters = params }
    }
    if (!isScreenSharing && localVideoTrack?.enabled() == true) {
      runCatching {
        cameraCapturer?.changeCaptureFormat(tier.targetWidth, tier.targetHeight, tier.targetFps)
      }
    }
  }

  fun startScreenShare(permissionResultData: Intent): Boolean {
    val vs = videoSource ?: return false
    val sth = surfaceTextureHelper ?: return false
    runCatching { cameraCapturer?.stopCapture() }

    val created = runCatching {
      ScreenCapturerAndroid(
        permissionResultData,
        object : MediaProjection.Callback() {
          override fun onStop() {
            callbacks.onScreenShareStoppedBySystem()
          }
        },
      )
    }.getOrNull() ?: return false

    screenCapturer = created
    isScreenSharing = true
    localVideoTrack?.setEnabled(true)
    return runCatching {
      created.initialize(sth, appContext, vs.capturerObserver)
      created.startCapture(
        currentQuality.targetWidth.coerceAtLeast(720),
        currentQuality.targetHeight.coerceAtLeast(1280),
        15,
      )
      true
    }.getOrElse {
      isScreenSharing = false
      false
    }
  }

  fun stopScreenShare(resumeCamera: Boolean) {
    if (!isScreenSharing) return
    isScreenSharing = false
    runCatching {
      screenCapturer?.stopCapture()
      screenCapturer?.dispose()
    }
    screenCapturer = null
    if (resumeCamera) {
      ensureCameraStarted()
      localVideoTrack?.setEnabled(true)
    } else {
      localVideoTrack?.setEnabled(false)
    }
  }

  fun pollStatsSample() {
    val pc = peerConnection ?: return
    pc.getStats { report ->
      var packetsLost = 0L
      var packetsSent = 0L
      var rttSec = 0.0
      for (stats in report.statsMap.values) {
        when (stats.type) {
          "outbound-rtp" -> {
            (stats.members["packetsSent"] as? Number)?.let { packetsSent += it.toLong() }
          }
          "remote-inbound-rtp" -> {
            (stats.members["packetsLost"] as? Number)?.let { packetsLost += it.toLong() }
            (stats.members["roundTripTime"] as? Number)?.let { rttSec = maxOf(rttSec, it.toDouble()) }
          }
          "candidate-pair" -> {
            val nominated = stats.members["nominated"] as? Boolean ?: false
            if (nominated) {
              (stats.members["currentRoundTripTime"] as? Number)?.let {
                if (rttSec <= 0.0) rttSec = it.toDouble()
              }
            }
          }
        }
      }
      val total = packetsSent + packetsLost
      val lossPct = if (total > 0L) (packetsLost.toFloat() / total.toFloat()) * 100f else 0f
      callbacks.onStatsSample(lossPct, rttSec * 1000.0)
    }
  }

  fun attachLocalRenderer(renderer: SurfaceViewRenderer?) {
    val egl = eglBase ?: return
    if (localRenderer === renderer) return
    localRenderer?.let { prev ->
      runCatching { localVideoTrack?.removeSink(prev) }
    }
    localRenderer = renderer
    if (renderer != null) {
      runCatching {
        renderer.init(egl.eglBaseContext, null)
        renderer.setMirror(isFrontCamera && !isScreenSharing)
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        renderer.setEnableHardwareScaler(true)
        localVideoTrack?.addSink(renderer)
      }
    }
  }

  fun attachRemoteRenderer(renderer: SurfaceViewRenderer?) {
    val egl = eglBase ?: return
    if (remoteRenderer === renderer) return
    remoteRenderer?.let { prev ->
      runCatching { remoteVideoTrack?.removeSink(prev) }
    }
    remoteRenderer = renderer
    if (renderer != null) {
      runCatching {
        renderer.init(egl.eglBaseContext, null)
        renderer.setMirror(false)
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        renderer.setEnableHardwareScaler(true)
        remoteVideoTrack?.addSink(renderer)
      }
    }
  }

  private fun attachRemoteVideoTrack(track: VideoTrack) {
    remoteVideoTrack = track
    remoteRenderer?.let { renderer ->
      runCatching { track.addSink(renderer) }
    }
  }

  fun disposeSession() {
    isScreenSharing = false
    runCatching { screenCapturer?.stopCapture() }
    runCatching { screenCapturer?.dispose() }
    screenCapturer = null

    runCatching { cameraCapturer?.stopCapture() }
    runCatching { cameraCapturer?.dispose() }
    cameraCapturer = null

    localRenderer?.let { r -> runCatching { localVideoTrack?.removeSink(r) } }
    remoteRenderer?.let { r -> runCatching { remoteVideoTrack?.removeSink(r) } }
    localRenderer = null
    remoteRenderer = null

    runCatching { localVideoTrack?.dispose() }
    localVideoTrack = null
    remoteVideoTrack = null
    runCatching { videoSource?.dispose() }
    videoSource = null

    runCatching { localAudioTrack?.dispose() }
    localAudioTrack = null
    runCatching { audioSource?.dispose() }
    audioSource = null

    runCatching { surfaceTextureHelper?.dispose() }
    surfaceTextureHelper = null

    runCatching { peerConnection?.close() }
    runCatching { peerConnection?.dispose() }
    peerConnection = null
    hasRemoteDescription = false
    pendingRemoteIce.clear()
  }

  fun shutdownAll() {
    disposeSession()
    runCatching { factory?.dispose() }
    factory = null
    runCatching { eglBase?.release() }
    eglBase = null
    initialized.set(false)
  }

  private open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) = Unit
    override fun onSetSuccess() = Unit
    override fun onCreateFailure(error: String?) = Unit
    override fun onSetFailure(error: String?) = Unit
  }
}
