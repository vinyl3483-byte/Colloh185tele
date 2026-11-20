package telefam.company.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.telefam.company.app.R
import telefam.company.app.TelefamMainActivity
import telefam.company.core.NotificationSettingsState
import java.util.Calendar

object TelefamNotificationManager {
  const val CHANNEL_MESSAGES = "telefam_messages"
  const val CHANNEL_SOCIAL = "telefam_social"
  const val CHANNEL_CALLS = "telefam_calls"
  const val CHANNEL_CALLS_ONGOING = "telefam_calls_ongoing"

  const val KEY_DIRECT_REPLY_TEXT = "telefam_reply_text"

  const val EXTRA_NOTIF_ROUTE = "telefam_extra_notif_route"
  const val EXTRA_NOTIFICATION_ID = "telefam_extra_notification_id"
  const val EXTRA_CONVERSATION_ID = "telefam_extra_conversation_id"
  const val EXTRA_PEER_USER_ID = "telefam_extra_peer_user_id"
  const val EXTRA_MESSAGE_ID = "telefam_extra_message_id"
  const val EXTRA_POST_ID = "telefam_extra_post_id"
  const val EXTRA_TARGET_USER_ID = "telefam_extra_target_user_id"
  const val EXTRA_CALL_ID = "telefam_extra_call_id"
  const val EXTRA_CALL_TYPE = "telefam_extra_call_type"
  const val EXTRA_CALLER_NAME = "telefam_extra_caller_name"
  const val EXTRA_CALLER_USERNAME = "telefam_extra_caller_username"
  const val EXTRA_CALLER_AVATAR_URL = "telefam_extra_caller_avatar_url"
  const val EXTRA_CALL_EXPIRES_AT_MS = "telefam_extra_call_expires_at_ms"
  const val EXTRA_CALL_AUTO_ACCEPT = "telefam_extra_call_auto_accept"

  const val ONGOING_CALL_NOTIF_ID = 94001

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val messagesChannel = NotificationChannel(
      CHANNEL_MESSAGES,
      context.getString(R.string.notif_channel_messages),
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = context.getString(R.string.notif_channel_messages_desc)
      enableVibration(true)
      setShowBadge(true)
    }

    val socialChannel = NotificationChannel(
      CHANNEL_SOCIAL,
      context.getString(R.string.notif_channel_social),
      NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
      description = context.getString(R.string.notif_channel_social_desc)
      setShowBadge(true)
    }

    val callsChannel = NotificationChannel(
      CHANNEL_CALLS,
      context.getString(R.string.notif_channel_calls),
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = context.getString(R.string.notif_channel_calls_desc)
      enableVibration(true)
      lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
    }

    val ongoingCallsChannel = NotificationChannel(
      CHANNEL_CALLS_ONGOING,
      context.getString(R.string.notif_channel_calls),
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = context.getString(R.string.notif_channel_calls_desc)
      setSound(null, null)
      enableVibration(false)
    }

    nm.createNotificationChannel(messagesChannel)
    nm.createNotificationChannel(socialChannel)
    nm.createNotificationChannel(callsChannel)
    nm.createNotificationChannel(ongoingCallsChannel)
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
  }

  fun fetchAndRegisterFcmTokenIfAvailable(
    context: Context,
    onToken: (String) -> Unit,
  ) {
    runCatching {
      if (FirebaseApp.getApps(context).isEmpty()) {
        // FirebaseApp is only auto-initialized when a valid google-services.json is bundled.
        return
      }
      FirebaseMessaging.getInstance().token
        .addOnSuccessListener { token ->
          val clean = token?.trim().orEmpty()
          if (clean.isNotBlank()) {
            onToken(clean)
          }
        }
    }
  }

  fun isQuietHoursActive(
    settings: NotificationSettingsState,
    nowMinutesOfDay: Int = currentMinutesOfDay(),
  ): Boolean {
    if (!settings.quietHoursEnabled) return false
    val start = ((settings.quietStartMinutes % 1440) + 1440) % 1440
    val end = ((settings.quietEndMinutes % 1440) + 1440) % 1440
    if (start == end) return false
    return if (start < end) {
      nowMinutesOfDay in start until end
    } else {
      nowMinutesOfDay >= start || nowMinutesOfDay < end
    }
  }

  fun shouldDeliverPushNotification(
    settings: NotificationSettingsState,
    kind: String,
    nowMinutesOfDay: Int = currentMinutesOfDay(),
  ): Boolean {
    if (settings.masterPause || !settings.channelPush) return false
    if (isQuietHoursActive(settings, nowMinutesOfDay)) return false
    return when (kind.lowercase()) {
      "chat_message" -> settings.typeMessages
      "follow" -> settings.typeFollowers
      "like", "mention", "comment" -> settings.typeComments
      else -> true
    }
  }

  @SuppressLint("MissingPermission")
  fun showChatMessageNotification(
    context: Context,
    conversationId: String,
    messageId: String,
    peerUserId: String,
    senderDisplayName: String,
    previewText: String,
    isViewOnce: Boolean,
    canReply: Boolean,
    timestampMs: Long = System.currentTimeMillis(),
  ) {
    ensureChannels(context)
    if (!hasNotificationPermission(context)) return

    val safeSender = senderDisplayName.ifBlank { context.getString(R.string.notif_default_sender) }
    val safePreview = if (isViewOnce) {
      context.getString(R.string.notif_view_once_voice_body)
    } else {
      previewText.ifBlank { context.getString(R.string.notif_encrypted_message_body) }
    }

    val tapIntent = Intent(context, TelefamMainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_NOTIF_ROUTE, "chat")
      putExtra(EXTRA_CONVERSATION_ID, conversationId)
      putExtra(EXTRA_PEER_USER_ID, peerUserId)
      putExtra(EXTRA_MESSAGE_ID, messageId)
    }
    val notifId = notificationIdForConversation(conversationId)
    val contentPendingIntent = PendingIntent.getActivity(
      context,
      notifId,
      tapIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val markReadIntent = Intent(context, TelefamNotificationActionReceiver::class.java).apply {
      action = TelefamNotificationActionReceiver.ACTION_MARK_AS_READ
      putExtra(EXTRA_CONVERSATION_ID, conversationId)
      putExtra(EXTRA_PEER_USER_ID, peerUserId)
      putExtra(EXTRA_MESSAGE_ID, messageId)
    }
    val markReadPendingIntent = PendingIntent.getBroadcast(
      context,
      notifId + 10_000,
      markReadIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val senderPerson = Person.Builder()
      .setName(safeSender)
      .setKey(peerUserId.ifBlank { conversationId })
      .build()

    val messagingStyle = NotificationCompat.MessagingStyle(senderPerson)
      .addMessage(safePreview, timestampMs.takeIf { it > 0L } ?: System.currentTimeMillis(), senderPerson)

    val publicSummary = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
      .setSmallIcon(R.drawable.ic_notif_telefam)
      .setContentTitle(context.getString(R.string.app_name))
      .setContentText(context.getString(R.string.notif_new_encrypted_message))
      .build()

    val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
      .setSmallIcon(R.drawable.ic_notif_telefam)
      .setColor(ContextCompat.getColor(context, R.color.notif_crimson))
      .setContentTitle(safeSender)
      .setContentText(safePreview)
      .setStyle(messagingStyle)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
      .setPublicVersion(publicSummary)
      .setAutoCancel(true)
      .setOnlyAlertOnce(false)
      .setWhen(timestampMs.takeIf { it > 0L } ?: System.currentTimeMillis())
      .setContentIntent(contentPendingIntent)
      .addAction(
        NotificationCompat.Action.Builder(
          0,
          context.getString(R.string.notif_action_mark_read),
          markReadPendingIntent,
        ).setShowsUserInterface(false).build(),
      )

    // Strictly forbid inline direct reply on view-once voice messages
    if (!isViewOnce && canReply && conversationId.isNotBlank() && peerUserId.isNotBlank()) {
      val remoteInput = RemoteInput.Builder(KEY_DIRECT_REPLY_TEXT)
        .setLabel(context.getString(R.string.notif_reply_hint))
        .build()

      val replyIntent = Intent(context, TelefamNotificationActionReceiver::class.java).apply {
        action = TelefamNotificationActionReceiver.ACTION_DIRECT_REPLY
        putExtra(EXTRA_CONVERSATION_ID, conversationId)
        putExtra(EXTRA_PEER_USER_ID, peerUserId)
        putExtra(EXTRA_MESSAGE_ID, messageId)
      }
      val replyFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
      } else {
        PendingIntent.FLAG_UPDATE_CURRENT
      }
      val replyPendingIntent = PendingIntent.getBroadcast(
        context,
        notifId + 20_000,
        replyIntent,
        replyFlags,
      )
      val replyAction = NotificationCompat.Action.Builder(
        0,
        context.getString(R.string.notif_action_reply),
        replyPendingIntent,
      )
        .addRemoteInput(remoteInput)
        .setAllowGeneratedReplies(false)
        .build()
      builder.addAction(replyAction)
    }

    runCatching {
      NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }
  }

  @SuppressLint("MissingPermission")
  fun showSocialNotification(
    context: Context,
    notificationId: String,
    kind: String,
    title: String,
    body: String,
    actorUserId: String,
    postId: String = "",
    conversationId: String = "",
    timestampMs: Long = System.currentTimeMillis(),
  ) {
    ensureChannels(context)
    if (!hasNotificationPermission(context)) return

    val route = when {
      conversationId.isNotBlank() -> "chat"
      postId.isNotBlank() -> "post"
      actorUserId.isNotBlank() -> "profile"
      else -> "inbox"
    }

    val notifIntId = notificationIdForKey("social_${notificationId.ifBlank { "${kind}_${actorUserId}_$postId" }}")
    val tapIntent = Intent(context, TelefamMainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_NOTIF_ROUTE, route)
      putExtra(EXTRA_NOTIFICATION_ID, notificationId)
      putExtra(EXTRA_CONVERSATION_ID, conversationId)
      putExtra(EXTRA_PEER_USER_ID, actorUserId)
      putExtra(EXTRA_TARGET_USER_ID, actorUserId)
      putExtra(EXTRA_POST_ID, postId)
    }
    val contentPendingIntent = PendingIntent.getActivity(
      context,
      notifIntId,
      tapIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val markReadIntent = Intent(context, TelefamNotificationActionReceiver::class.java).apply {
      action = TelefamNotificationActionReceiver.ACTION_MARK_NOTIFICATION_READ
      putExtra(EXTRA_NOTIFICATION_ID, notificationId)
      putExtra(EXTRA_CONVERSATION_ID, conversationId)
    }
    val markReadPendingIntent = PendingIntent.getBroadcast(
      context,
      notifIntId + 30_000,
      markReadIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val builder = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
      .setSmallIcon(R.drawable.ic_notif_telefam)
      .setColor(ContextCompat.getColor(context, R.color.notif_crimson))
      .setContentTitle(title.ifBlank { context.getString(R.string.app_name) })
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setCategory(NotificationCompat.CATEGORY_SOCIAL)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .setWhen(timestampMs.takeIf { it > 0L } ?: System.currentTimeMillis())
      .setContentIntent(contentPendingIntent)

    if (notificationId.isNotBlank()) {
      builder.addAction(
        NotificationCompat.Action.Builder(
          0,
          context.getString(R.string.notif_action_mark_read),
          markReadPendingIntent,
        ).setShowsUserInterface(false).build(),
      )
    }

    runCatching {
      NotificationManagerCompat.from(context).notify(notifIntId, builder.build())
    }
  }

  @SuppressLint("MissingPermission")
  fun showIncomingCallNotification(
    context: Context,
    callId: String,
    conversationId: String,
    callType: String,
    callerUserId: String,
    callerName: String,
    callerUsername: String,
    callerAvatarUrl: String = "",
    expiresAtMs: Long = 0L,
  ) {
    ensureChannels(context)
    if (!hasNotificationPermission(context) || callId.isBlank()) return

    val safeCallerName = callerName.ifBlank {
      callerUsername.takeIf { it.isNotBlank() }?.let { "@$it" }
        ?: context.getString(R.string.notif_default_sender)
    }
    val isVideo = callType.equals("video", ignoreCase = true)
    val contentSubtitle = if (isVideo) {
      context.getString(R.string.call_incoming_video)
    } else {
      context.getString(R.string.call_incoming_audio)
    }
    val notifId = notificationIdForCall(callId)

    val fullScreenIntent = Intent(context, TelefamMainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_NOTIF_ROUTE, "incoming_call")
      putExtra(EXTRA_CALL_ID, callId)
      putExtra(EXTRA_CONVERSATION_ID, conversationId)
      putExtra(EXTRA_PEER_USER_ID, callerUserId)
      putExtra(EXTRA_CALL_TYPE, if (isVideo) "video" else "audio")
      putExtra(EXTRA_CALLER_NAME, safeCallerName)
      putExtra(EXTRA_CALLER_USERNAME, callerUsername)
      putExtra(EXTRA_CALLER_AVATAR_URL, callerAvatarUrl)
      putExtra(EXTRA_CALL_EXPIRES_AT_MS, expiresAtMs)
      putExtra(EXTRA_CALL_AUTO_ACCEPT, false)
    }
    val fullScreenPendingIntent = PendingIntent.getActivity(
      context,
      notifId,
      fullScreenIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val acceptIntent = Intent(context, TelefamMainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_NOTIF_ROUTE, "incoming_call")
      putExtra(EXTRA_CALL_ID, callId)
      putExtra(EXTRA_CONVERSATION_ID, conversationId)
      putExtra(EXTRA_PEER_USER_ID, callerUserId)
      putExtra(EXTRA_CALL_TYPE, if (isVideo) "video" else "audio")
      putExtra(EXTRA_CALLER_NAME, safeCallerName)
      putExtra(EXTRA_CALLER_USERNAME, callerUsername)
      putExtra(EXTRA_CALLER_AVATAR_URL, callerAvatarUrl)
      putExtra(EXTRA_CALL_EXPIRES_AT_MS, expiresAtMs)
      putExtra(EXTRA_CALL_AUTO_ACCEPT, true)
    }
    val acceptPendingIntent = PendingIntent.getActivity(
      context,
      notifId + 40_000,
      acceptIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val declineIntent = Intent(context, TelefamNotificationActionReceiver::class.java).apply {
      action = TelefamNotificationActionReceiver.ACTION_DECLINE_CALL
      putExtra(EXTRA_CALL_ID, callId)
      putExtra(EXTRA_CONVERSATION_ID, conversationId)
    }
    val declinePendingIntent = PendingIntent.getBroadcast(
      context,
      notifId + 50_000,
      declineIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val callerPerson = Person.Builder()
      .setName(safeCallerName)
      .setKey(callerUserId.ifBlank { callId })
      .setImportant(true)
      .build()

    val callStyle = NotificationCompat.CallStyle.forIncomingCall(
      callerPerson,
      declinePendingIntent,
      acceptPendingIntent,
    ).setIsVideo(isVideo)

    val builder = NotificationCompat.Builder(context, CHANNEL_CALLS)
      .setSmallIcon(R.drawable.ic_notif_telefam)
      .setColor(ContextCompat.getColor(context, R.color.notif_crimson))
      .setContentTitle(safeCallerName)
      .setContentText(contentSubtitle)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setAutoCancel(false)
      .setTimeoutAfter(26_000L)
      .setContentIntent(fullScreenPendingIntent)
      .setFullScreenIntent(fullScreenPendingIntent, true)
      .setStyle(callStyle)

    runCatching {
      NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }
  }

  fun buildOngoingCallNotification(
    context: Context,
    callId: String,
    conversationId: String,
    peerUserId: String,
    peerName: String,
    isVideo: Boolean,
    isScreenSharing: Boolean,
  ): android.app.Notification {
    ensureChannels(context)
    val safeName = peerName.ifBlank { context.getString(R.string.app_name) }
    val statusText = when {
      isScreenSharing -> context.getString(R.string.call_action_share_screen)
      isVideo -> context.getString(R.string.call_action_video)
      else -> context.getString(R.string.call_action_audio)
    }

    val openCallIntent = Intent(context, TelefamMainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_NOTIF_ROUTE, "active_call")
      putExtra(EXTRA_CALL_ID, callId)
      putExtra(EXTRA_CONVERSATION_ID, conversationId)
      putExtra(EXTRA_PEER_USER_ID, peerUserId)
    }
    val openCallPi = PendingIntent.getActivity(
      context,
      ONGOING_CALL_NOTIF_ID,
      openCallIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val hangUpIntent = Intent(context, TelefamNotificationActionReceiver::class.java).apply {
      action = TelefamNotificationActionReceiver.ACTION_HANGUP_CALL
      putExtra(EXTRA_CALL_ID, callId)
    }
    val hangUpPi = PendingIntent.getBroadcast(
      context,
      ONGOING_CALL_NOTIF_ID + 1,
      hangUpIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val peerPerson = Person.Builder()
      .setName(safeName)
      .setKey(peerUserId.ifBlank { callId.ifBlank { "active_call" } })
      .build()

    val style = NotificationCompat.CallStyle.forOngoingCall(peerPerson, hangUpPi)
      .setIsVideo(isVideo)

    return NotificationCompat.Builder(context, CHANNEL_CALLS_ONGOING)
      .setSmallIcon(R.drawable.ic_notif_telefam)
      .setColor(ContextCompat.getColor(context, R.color.notif_crimson))
      .setContentTitle(safeName)
      .setContentText(statusText)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setOngoing(true)
      .setSilent(true)
      .setUsesChronometer(true)
      .setContentIntent(openCallPi)
      .setStyle(style)
      .build()
  }

  fun cancelCallNotification(context: Context, callId: String) {
    if (callId.isBlank()) return
    runCatching {
      NotificationManagerCompat.from(context).cancel(notificationIdForCall(callId))
    }
  }

  fun cancelConversationNotification(context: Context, conversationId: String) {
    if (conversationId.isBlank()) return
    runCatching {
      NotificationManagerCompat.from(context).cancel(notificationIdForConversation(conversationId))
    }
  }

  fun cancelSocialNotification(context: Context, notificationId: String) {
    if (notificationId.isBlank()) return
    runCatching {
      NotificationManagerCompat.from(context).cancel(notificationIdForKey("social_$notificationId"))
    }
  }

  fun cancelAllNotifications(context: Context) {
    runCatching {
      NotificationManagerCompat.from(context).cancelAll()
    }
  }

  fun notificationIdForConversation(conversationId: String): Int =
    notificationIdForKey("conv_$conversationId")

  fun notificationIdForCall(callId: String): Int =
    notificationIdForKey("call_$callId")

  private fun notificationIdForKey(key: String): Int =
    (key.hashCode() and 0x0FFF_FFFF).coerceAtLeast(100)

  private fun currentMinutesOfDay(): Int {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
  }
}
