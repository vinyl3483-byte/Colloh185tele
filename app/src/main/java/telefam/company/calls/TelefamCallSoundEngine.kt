package telefam.company.calls

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Real WhatsApp-style call audio routing and synthesized call cues:
 * - Calling pulse (while awaiting peer reachability)
 * - Double ringback tone (when peer is online & ringing)
 * - System ringtone / harmonic alert for incoming calls
 * - Connect chime, upgrade-to-video chime, and hangup/busy/restricted tones
 */
class TelefamCallSoundEngine(
  private val context: Context,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  private var toneJob: Job? = null
  private var ringtonePlayer: MediaPlayer? = null
  private var focusRequest: AudioFocusRequest? = null

  fun configureAudioRouting(isVideoCall: Boolean, speakerEnabled: Boolean) {
    val am = audioManager ?: return
    runCatching {
      requestAudioFocus(am)
      am.mode = AudioManager.MODE_IN_COMMUNICATION
      val wantSpeaker = speakerEnabled || isVideoCall
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val targetType = if (wantSpeaker) {
          AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
          AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        }
        val target = am.availableCommunicationDevices.firstOrNull { it.type == targetType }
        if (target != null) {
          am.setCommunicationDevice(target)
        } else {
          @Suppress("DEPRECATION")
          am.isSpeakerphoneOn = wantSpeaker
        }
      } else {
        @Suppress("DEPRECATION")
        am.isSpeakerphoneOn = wantSpeaker
      }
    }
  }

  fun setSpeakerEnabled(enabled: Boolean) {
    val am = audioManager ?: return
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val targetType = if (enabled) {
          AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
          AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        }
        val target = am.availableCommunicationDevices.firstOrNull { it.type == targetType }
        if (target != null) {
          am.setCommunicationDevice(target)
          return
        }
      }
      @Suppress("DEPRECATION")
      am.isSpeakerphoneOn = enabled
    }
  }

  fun startOutgoingCallingTone() {
    stopLoopingTones()
    toneJob = scope.launch {
      while (isActive) {
        playDualToneMs(440.0, 480.0, durationMs = 280, amplitude = 0.14)
        delay(1_400L)
      }
    }
  }

  fun startOutgoingRingingTone() {
    stopLoopingTones()
    toneJob = scope.launch {
      while (isActive) {
        playDualToneMs(440.0, 480.0, durationMs = 450, amplitude = 0.18)
        delay(200L)
        playDualToneMs(440.0, 480.0, durationMs = 450, amplitude = 0.18)
        delay(2_100L)
      }
    }
  }

  fun startIncomingRingtone() {
    stopLoopingTones()
    runCatching {
      val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
      if (uri != null) {
        val mp = MediaPlayer().apply {
          setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build(),
          )
          setDataSource(context, uri)
          isLooping = true
          prepare()
          start()
        }
        ringtonePlayer = mp
        return
      }
    }
    // Synthesized pleasant dual-chime fallback if system ringtone URI is unavailable
    toneJob = scope.launch {
      while (isActive) {
        playDualToneMs(523.25, 659.25, durationMs = 360, amplitude = 0.25)
        delay(140L)
        playDualToneMs(659.25, 783.99, durationMs = 420, amplitude = 0.25)
        delay(1_600L)
      }
    }
  }

  fun playConnectedCue() {
    stopLoopingTones()
    scope.launch {
      playDualToneMs(523.25, 659.25, durationMs = 110, amplitude = 0.18)
      playDualToneMs(659.25, 783.99, durationMs = 150, amplitude = 0.18)
    }
  }

  fun playEndedCue() {
    stopLoopingTones()
    scope.launch {
      playDualToneMs(480.0, 380.0, durationMs = 120, amplitude = 0.18)
      delay(70L)
      playDualToneMs(360.0, 280.0, durationMs = 180, amplitude = 0.16)
    }
  }

  fun playBusyOrRestrictedCue() {
    stopLoopingTones()
    scope.launch {
      repeat(3) {
        playDualToneMs(480.0, 620.0, durationMs = 190, amplitude = 0.20)
        delay(180L)
      }
    }
  }

  fun stopLoopingTones() {
    toneJob?.cancel()
    toneJob = null
    runCatching {
      ringtonePlayer?.stop()
      ringtonePlayer?.release()
    }
    ringtonePlayer = null
  }

  fun releaseAll() {
    stopLoopingTones()
    val am = audioManager ?: return
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        am.clearCommunicationDevice()
      } else {
        @Suppress("DEPRECATION")
        am.isSpeakerphoneOn = false
      }
      am.mode = AudioManager.MODE_NORMAL
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        focusRequest?.let { am.abandonAudioFocusRequest(it) }
      }
    }
    focusRequest = null
  }

  private fun requestAudioFocus(am: AudioManager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build(),
        )
        .build()
      focusRequest = req
      am.requestAudioFocus(req)
    }
  }

  private fun playDualToneMs(freqA: Double, freqB: Double, durationMs: Int, amplitude: Double) {
    runCatching {
      val sampleRate = 16_000
      val totalSamples = (sampleRate * durationMs) / 1000
      if (totalSamples <= 0) return
      val pcm = ShortArray(totalSamples)
      val fadeSamples = (sampleRate * 0.015).toInt().coerceAtLeast(1)
      for (i in 0 until totalSamples) {
        val env = when {
          i < fadeSamples -> i.toDouble() / fadeSamples
          i > totalSamples - fadeSamples -> (totalSamples - i).toDouble() / fadeSamples
          else -> 1.0
        }
        val t = i.toDouble() / sampleRate
        val sample = 0.5 * (sin(2.0 * PI * freqA * t) + sin(2.0 * PI * freqB * t))
        pcm[i] = (sample * amplitude * env * Short.MAX_VALUE).toInt()
          .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
          .toShort()
      }
      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build(),
        )
        .setAudioFormat(
          android.media.AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
            .build(),
        )
        .setBufferSizeInBytes(pcm.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()
      track.write(pcm, 0, pcm.size)
      track.play()
      Thread.sleep(durationMs.toLong() + 20L)
      track.stop()
      track.release()
    }
  }
}
