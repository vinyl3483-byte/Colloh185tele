package telefam.company.chat

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * WhatsApp-inspired click-free synthesized sound effects (outgoing sent droplet, incoming dual-tone chime,
 * and subtle tactile typing tick) plus low-bandwidth AAC voice note recording & playback.
 * Respects muted conversations, notification master-pause, and quiet hours.
 */
object ChatSoundEffects {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  fun playSentPop(enabled: Boolean = true) {
    if (!enabled) return
    scope.launch {
      // Smooth upward droplet sweep + soft octave harmonic (WhatsApp outgoing send style)
      playSamples(
        generateToneSamples(
          durationMs = 54,
          startHz = 520.0,
          endHz = 1160.0,
          peak = 0.24,
          decay = 6.2,
          harmonicMix = 0.22,
        ),
      )
    }
  }

  fun playIncomingChime(enabled: Boolean = true) {
    if (!enabled) return
    scope.launch {
      val s1 = generateToneSamples(durationMs = 74, startHz = 659.25, endHz = 678.0, peak = 0.22, decay = 5.4, harmonicMix = 0.18)
      val gap = ShortArray(260) // ~11ms natural interval
      val s2 = generateToneSamples(durationMs = 108, startHz = 987.77, endHz = 987.77, peak = 0.20, decay = 4.6, harmonicMix = 0.16)
      playSamples(s1 + gap + s2)
    }
  }

  fun playTypingTick(enabled: Boolean = true) {
    if (!enabled) return
    scope.launch {
      playSamples(generateToneSamples(durationMs = 15, startHz = 1320.0, endHz = 840.0, peak = 0.065, decay = 13.0, harmonicMix = 0.08))
    }
  }

  private fun generateToneSamples(
    durationMs: Int,
    startHz: Double,
    endHz: Double,
    peak: Double,
    decay: Double,
    harmonicMix: Double = 0.15,
  ): ShortArray {
    val sampleRate = 24_000
    val count = ((sampleRate * durationMs) / 1000).coerceAtLeast(1)
    val fadeInSamples = (sampleRate * 5 / 1000).coerceAtLeast(1)
    val fadeOutSamples = (sampleRate * 10 / 1000).coerceAtLeast(1)
    val out = ShortArray(count)
    var phase = 0.0
    for (i in 0 until count) {
      val t = i.toDouble() / count.toDouble()
      val freq = startHz + (endHz - startHz) * t
      val attack = if (i < fadeInSamples) 0.5 * (1.0 - cos(PI * i / fadeInSamples)) else 1.0
      val release = if (i > count - fadeOutSamples) 0.5 * (1.0 - cos(PI * (count - i) / fadeOutSamples)) else 1.0
      val env = peak * attack * release * exp(-decay * t)
      phase += 2.0 * PI * freq / sampleRate
      val wave = sin(phase) + harmonicMix * sin(phase * 2.0)
      val norm = wave / (1.0 + harmonicMix)
      val sample = (norm * env * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
      out[i] = sample.toShort()
    }
    return out
  }

  private fun playSamples(samples: ShortArray) {
    runCatching {
      val sampleRate = 24_000
      val minBuf = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
      ).coerceAtLeast(samples.size * 2)
      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build(),
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build(),
        )
        .setBufferSizeInBytes(minBuf)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()
      track.write(samples, 0, samples.size)
      track.play()
      Thread.sleep(((samples.size * 1000L) / sampleRate.toLong()) + 45L)
      track.stop()
      track.release()
    }
  }
}

data class VoiceRecordingState(
  val isRecording: Boolean = false,
  val elapsedMs: Long = 0L,
  val amplitude: Float = 0f,
  val recordedFilePath: String = "",
  val recordedDurationMs: Long = 0L,
)

data class VoicePlaybackState(
  val activeMessageId: String = "",
  val isPlaying: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
)

/**
 * Hardware MediaRecorder & MediaPlayer controller for compressed 1-on-1 voice notes and View-Once playback.
 */
class ChatVoiceAudioManager(private val appContext: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private val _recording = MutableStateFlow(VoiceRecordingState())
  val recording: StateFlow<VoiceRecordingState> = _recording.asStateFlow()

  private val _playback = MutableStateFlow(VoicePlaybackState())
  val playback: StateFlow<VoicePlaybackState> = _playback.asStateFlow()

  private var recorder: MediaRecorder? = null
  private var recordFile: File? = null
  private var recordStartMs: Long = 0L
  private var recordTicker: Job? = null

  private var player: MediaPlayer? = null
  private var playTicker: Job? = null

  fun startRecording(): Boolean {
    stopPlayback()
    discardRecording()
    val outDir = File(appContext.cacheDir, "telefam_voice_notes").apply { mkdirs() }
    val outFile = File(outDir, "vn_${System.currentTimeMillis()}.m4a")
    return runCatching {
      val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(appContext)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }
      rec.setAudioSource(MediaRecorder.AudioSource.MIC)
      rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
      rec.setAudioChannels(1)
      // Wideband speech AAC (~3KB/sec) — fast & reliable on weak cellular connections
      rec.setAudioEncodingBitRate(24_000)
      rec.setAudioSamplingRate(24_000)
      rec.setOutputFile(outFile.absolutePath)
      rec.prepare()
      rec.start()
      recorder = rec
      recordFile = outFile
      recordStartMs = System.currentTimeMillis()
      _recording.value = VoiceRecordingState(isRecording = true, elapsedMs = 0L, amplitude = 0.25f)
      recordTicker?.cancel()
      recordTicker = scope.launch {
        while (isActive) {
          val amp = runCatching { (recorder?.maxAmplitude ?: 0) / 32767f }.getOrDefault(0.15f)
          _recording.value = _recording.value.copy(
            elapsedMs = System.currentTimeMillis() - recordStartMs,
            amplitude = amp.coerceIn(0.08f, 1f),
          )
          delay(75L)
        }
      }
      true
    }.getOrElse {
      runCatching { recorder?.release() }
      recorder = null
      outFile.delete()
      _recording.value = VoiceRecordingState()
      false
    }
  }

  fun finishRecording(): VoiceRecordingState {
    recordTicker?.cancel()
    recordTicker = null
    val elapsed = (System.currentTimeMillis() - recordStartMs).coerceAtLeast(350L)
    val file = recordFile
    runCatching {
      recorder?.stop()
    }
    runCatching {
      recorder?.release()
    }
    recorder = null
    val valid = file != null && file.exists() && file.length() > 128L
    val state = if (valid && file != null) {
      VoiceRecordingState(
        isRecording = false,
        elapsedMs = elapsed,
        recordedFilePath = file.absolutePath,
        recordedDurationMs = elapsed,
      )
    } else {
      file?.delete()
      VoiceRecordingState()
    }
    _recording.value = state
    return state
  }

  fun discardRecording() {
    recordTicker?.cancel()
    recordTicker = null
    runCatching { recorder?.stop() }
    runCatching { recorder?.release() }
    recorder = null
    recordFile?.delete()
    recordFile = null
    _recording.value = VoiceRecordingState()
  }

  fun playFileOrUrl(
    messageId: String,
    source: String,
    authHeaders: Map<String, String> = emptyMap(),
    onCompleted: () -> Unit = {},
  ) {
    if (_playback.value.activeMessageId == messageId && _playback.value.isPlaying) {
      stopPlayback()
      return
    }
    stopPlayback()
    runCatching {
      val mp = MediaPlayer()
      if (source.startsWith("http://") || source.startsWith("https://")) {
        mp.setDataSource(appContext, android.net.Uri.parse(source), authHeaders)
      } else {
        mp.setDataSource(source)
      }
      mp.setOnPreparedListener { readyPlayer ->
        val total = readyPlayer.duration.toLong().coerceAtLeast(0L)
        readyPlayer.start()
        _playback.value = VoicePlaybackState(
          activeMessageId = messageId,
          isPlaying = true,
          positionMs = 0L,
          durationMs = total,
        )
        playTicker?.cancel()
        playTicker = scope.launch {
          while (isActive && player === mp && mp.isPlaying) {
            _playback.value = _playback.value.copy(
              positionMs = mp.currentPosition.toLong().coerceAtLeast(0L),
            )
            delay(80L)
          }
        }
      }
      mp.setOnCompletionListener {
        stopPlayback()
        onCompleted()
      }
      mp.setOnErrorListener { _, _, _ ->
        stopPlayback()
        true
      }
      player = mp
      mp.prepareAsync()
    }.onFailure {
      stopPlayback()
    }
  }

  fun stopPlayback() {
    playTicker?.cancel()
    playTicker = null
    runCatching { player?.stop() }
    runCatching { player?.release() }
    player = null
    _playback.value = VoicePlaybackState()
  }
}
