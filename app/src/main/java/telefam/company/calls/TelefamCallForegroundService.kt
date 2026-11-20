package telefam.company.calls

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import telefam.company.notifications.TelefamNotificationManager

/**
 * Android 14+ compliant typed Foreground Service for active P2P voice/video calls and screen sharing.
 * Dynamically requests only the FGS types whose runtime permission/consent has already been granted.
 */
class TelefamCallForegroundService : Service() {

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
      return START_NOT_STICKY
    }

    val callId = intent?.getStringExtra(EXTRA_CALL_ID).orEmpty()
    val convId = intent?.getStringExtra(EXTRA_CONVERSATION_ID).orEmpty()
    val peerId = intent?.getStringExtra(EXTRA_PEER_ID).orEmpty()
    val peerName = intent?.getStringExtra(EXTRA_PEER_NAME).orEmpty()
    val isVideo = intent?.getBooleanExtra(EXTRA_IS_VIDEO, false) == true
    val isScreenSharing = intent?.getBooleanExtra(EXTRA_IS_SCREEN_SHARING, false) == true

    val notification = TelefamNotificationManager.buildOngoingCallNotification(
      context = this,
      callId = callId,
      conversationId = convId,
      peerUserId = peerId,
      peerName = peerName,
      isVideo = isVideo,
      isScreenSharing = isScreenSharing,
    )

    val fgsTypes = computeForegroundServiceTypes(isVideo = isVideo, isScreenSharing = isScreenSharing)
    runCatching {
      ServiceCompat.startForeground(
        this,
        TelefamNotificationManager.ONGOING_CALL_NOTIF_ID,
        notification,
        fgsTypes,
      )
    }.onFailure {
      // Fallback to basic microphone or default FGS if a specific permission is temporarily missing
      runCatching {
        startForeground(TelefamNotificationManager.ONGOING_CALL_NOTIF_ID, notification)
      }
    }

    return START_NOT_STICKY
  }

  private fun computeForegroundServiceTypes(isVideo: Boolean, isScreenSharing: Boolean): Int {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
    var mask = 0
    val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
      PackageManager.PERMISSION_GRANTED
    if (hasMic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      mask = mask or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
    }
    val hasCam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
      PackageManager.PERMISSION_GRANTED
    if (isVideo && hasCam && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      mask = mask or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
    }
    if (isScreenSharing) {
      mask = mask or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
    }
    return mask
  }

  companion object {
    private const val ACTION_START_OR_UPDATE = "telefam.company.calls.ACTION_START_FGS"
    private const val ACTION_STOP = "telefam.company.calls.ACTION_STOP_FGS"
    private const val EXTRA_CALL_ID = "extra_call_id"
    private const val EXTRA_CONVERSATION_ID = "extra_conv_id"
    private const val EXTRA_PEER_ID = "extra_peer_id"
    private const val EXTRA_PEER_NAME = "extra_peer_name"
    private const val EXTRA_IS_VIDEO = "extra_is_video"
    private const val EXTRA_IS_SCREEN_SHARING = "extra_is_screen_sharing"

    fun startOrUpdate(
      context: Context,
      callId: String,
      conversationId: String,
      peerUserId: String,
      peerName: String,
      isVideo: Boolean,
      isScreenSharing: Boolean,
    ) {
      val intent = Intent(context, TelefamCallForegroundService::class.java).apply {
        action = ACTION_START_OR_UPDATE
        putExtra(EXTRA_CALL_ID, callId)
        putExtra(EXTRA_CONVERSATION_ID, conversationId)
        putExtra(EXTRA_PEER_ID, peerUserId)
        putExtra(EXTRA_PEER_NAME, peerName)
        putExtra(EXTRA_IS_VIDEO, isVideo)
        putExtra(EXTRA_IS_SCREEN_SHARING, isScreenSharing)
      }
      runCatching {
        ContextCompat.startForegroundService(context, intent)
      }
    }

    fun stop(context: Context) {
      val intent = Intent(context, TelefamCallForegroundService::class.java).apply {
        action = ACTION_STOP
      }
      runCatching {
        context.stopService(intent)
      }
    }
  }
}
