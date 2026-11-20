package telefam.company.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import telefam.company.app.TelefamApplication

class TelefamNotificationActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val app = context.applicationContext as? TelefamApplication ?: return
    val repo = app.repository
    val pendingResult = goAsync()

    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        when (intent.action) {
          ACTION_DIRECT_REPLY -> {
            val conversationId = intent.getStringExtra(TelefamNotificationManager.EXTRA_CONVERSATION_ID).orEmpty()
            val peerUserId = intent.getStringExtra(TelefamNotificationManager.EXTRA_PEER_USER_ID).orEmpty()
            val replyText = RemoteInput.getResultsFromIntent(intent)
              ?.getCharSequence(TelefamNotificationManager.KEY_DIRECT_REPLY_TEXT)
              ?.toString()
              ?.trim()
              .orEmpty()

            if (conversationId.isNotBlank() && peerUserId.isNotBlank() && replyText.isNotBlank()) {
              repo.chatRepository.sendQuickReplyFromNotification(
                conversationId = conversationId,
                peerUserId = peerUserId,
                replyText = replyText,
              )
              repo.markConversationNotificationsReadLocal(conversationId)
            }
            TelefamNotificationManager.cancelConversationNotification(context, conversationId)
          }

          ACTION_MARK_AS_READ -> {
            val conversationId = intent.getStringExtra(TelefamNotificationManager.EXTRA_CONVERSATION_ID).orEmpty()
            if (conversationId.isNotBlank()) {
              repo.chatRepository.markConversationReadFromNotification(conversationId)
              repo.markConversationNotificationsReadLocal(conversationId)
              TelefamNotificationManager.cancelConversationNotification(context, conversationId)
            }
          }

          ACTION_MARK_NOTIFICATION_READ -> {
            val notificationId = intent.getStringExtra(TelefamNotificationManager.EXTRA_NOTIFICATION_ID).orEmpty()
            val conversationId = intent.getStringExtra(TelefamNotificationManager.EXTRA_CONVERSATION_ID).orEmpty()
            if (notificationId.isNotBlank()) {
              repo.markNotificationRead(notificationId)
              TelefamNotificationManager.cancelSocialNotification(context, notificationId)
            }
            if (conversationId.isNotBlank()) {
              repo.chatRepository.markConversationReadFromNotification(conversationId)
              TelefamNotificationManager.cancelConversationNotification(context, conversationId)
            }
          }

          ACTION_DECLINE_CALL -> {
            val callId = intent.getStringExtra(TelefamNotificationManager.EXTRA_CALL_ID).orEmpty()
            val conversationId = intent.getStringExtra(TelefamNotificationManager.EXTRA_CONVERSATION_ID).orEmpty()
            if (callId.isNotBlank()) {
              TelefamNotificationManager.cancelCallNotification(context, callId)
              telefam.company.calls.IncomingCallPushBridge.emitCancelled(callId, "declined")
              runCatching {
                repo.chatRepository.sendCallSignal(
                  callId,
                  telefam.company.data.network.CallSignalRequestDto(
                    action = "decline",
                    reason = "declined_from_notification",
                  ),
                )
              }
              if (conversationId.isNotBlank()) {
                runCatching { repo.chatRepository.loadConversationMessages(conversationId, markAsRead = false) }
              }
            }
          }

          ACTION_HANGUP_CALL -> {
            val callId = intent.getStringExtra(TelefamNotificationManager.EXTRA_CALL_ID).orEmpty()
            telefam.company.calls.IncomingCallPushBridge.emitHangup(callId)
            if (callId.isNotBlank()) {
              TelefamNotificationManager.cancelCallNotification(context, callId)
              runCatching {
                repo.chatRepository.sendCallSignal(
                  callId,
                  telefam.company.data.network.CallSignalRequestDto(
                    action = "end",
                    reason = "hangup_from_notification",
                  ),
                )
              }
            }
          }
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    const val ACTION_DIRECT_REPLY = "telefam.company.action.DIRECT_REPLY"
    const val ACTION_MARK_AS_READ = "telefam.company.action.MARK_AS_READ"
    const val ACTION_MARK_NOTIFICATION_READ = "telefam.company.action.MARK_NOTIFICATION_READ"
    const val ACTION_DECLINE_CALL = "telefam.company.action.DECLINE_CALL"
    const val ACTION_HANGUP_CALL = "telefam.company.action.HANGUP_CALL"
  }
}
