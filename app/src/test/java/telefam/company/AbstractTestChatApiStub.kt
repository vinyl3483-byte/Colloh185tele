package telefam.company

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import telefam.company.data.network.AppVersionResponseDto
import telefam.company.data.network.AuditEventDto
import telefam.company.data.network.AuthSessionResponseDto
import telefam.company.data.network.BatchConversationActionRequestDto
import telefam.company.data.network.ChatConversationsResponseDto
import telefam.company.data.network.ChatGlobalSearchResponseDto
import telefam.company.data.network.ChatMessageSingleResponseDto
import telefam.company.data.network.ChatMessagesListResponseDto
import telefam.company.data.network.ChatPrivacyResponseDto
import telefam.company.data.network.ChatPrivacySettingsDto
import telefam.company.data.network.ChatReceiptRequestDto
import telefam.company.data.network.ContactActionDto
import telefam.company.data.network.ContactSyncBatchRequestDto
import telefam.company.data.network.ContactsPageResponseDto
import telefam.company.data.network.ContentPreferencesDto
import telefam.company.data.network.CreatePostRequestDto
import telefam.company.data.network.CreatePostResponseDto
import telefam.company.data.network.DeviceContextTelemetryDto
import telefam.company.data.network.DeviceContextTelemetryResponseDto
import telefam.company.data.network.E2eeKeyBundleResponseDto
import telefam.company.data.network.E2eeKeyBundleUploadDto
import telefam.company.data.network.EditChatMessageRequestDto
import telefam.company.data.network.EmailCheckResponseDto
import telefam.company.data.network.FaqListResponseDto
import telefam.company.data.network.FeedInteractionBatchRequestDto
import telefam.company.data.network.FeedInteractionRequestDto
import telefam.company.data.network.FeedInteractionResponseDto
import telefam.company.data.network.FeedPageResponseDto
import telefam.company.data.network.FeedSearchResponseDto
import telefam.company.data.network.FollowMutationResponseDto
import telefam.company.data.network.GenericSuccessDto
import telefam.company.data.network.GiflyGreetingResponseDto
import telefam.company.data.network.GiflySearchResponseDto
import telefam.company.data.network.HashtagsSearchResponseDto
import telefam.company.data.network.LegalDocumentsResponseDto
import telefam.company.data.network.LinkPreviewResponseDto
import telefam.company.data.network.LoginApprovalStatusResponseDto
import telefam.company.data.network.LogoutRequestDto
import telefam.company.data.network.ManualSyncRequestDto
import telefam.company.data.network.ManualSyncResponseDto
import telefam.company.data.network.MarkNotificationReadRequestDto
import telefam.company.data.network.MediaUploadResponseDto
import telefam.company.data.network.NotificationSettingsDto
import telefam.company.data.network.NotificationUnreadCountResponseDto
import telefam.company.data.network.NotificationsListResponseDto
import telefam.company.data.network.OAuthDto
import telefam.company.data.network.OpenDirectConversationResponseDto
import telefam.company.data.network.PendingLoginRequestsResponseDto
import telefam.company.data.network.PrivacySettingsDto
import telefam.company.data.network.ProblemReportRequestDto
import telefam.company.data.network.ProblemReportResponseDto
import telefam.company.data.network.ProfileBackgroundAttachDto
import telefam.company.data.network.ProfileBackgroundListResponseDto
import telefam.company.data.network.ProfileBackgroundReorderDto
import telefam.company.data.network.ProfileBackgroundUploadResponseDto
import telefam.company.data.network.ProfileSetupDto
import telefam.company.data.network.ProfileUpdateRequestDto
import telefam.company.data.network.ProfileUpdateResponseDto
import telefam.company.data.network.RecordSecurityFlagRequestDto
import telefam.company.data.network.RefreshTokenDto
import telefam.company.data.network.RegisterPushTokenRequestDto
import telefam.company.data.network.RequestOtpDto
import telefam.company.data.network.RequestOtpResponseDto
import telefam.company.data.network.RespondLoginRequestDto
import telefam.company.data.network.RespondLoginRequestResponseDto
import telefam.company.data.network.RestrictedAccountsResponseDto
import telefam.company.data.network.SaveLinkRequestDto
import telefam.company.data.network.SaveLinkResponseDto
import telefam.company.data.network.SavedLinksListResponseDto
import telefam.company.data.network.SecurityFlagResponseDto
import telefam.company.data.network.SecurityFlagsListResponseDto
import telefam.company.data.network.SendChatMessageRequestDto
import telefam.company.data.network.SettingsBundleResponseDto
import telefam.company.data.network.SongsSearchResponseDto
import telefam.company.data.network.SupportTicketsResponseDto
import telefam.company.data.network.SyncBatchRequestDto
import telefam.company.data.network.SyncBatchResponseDto
import telefam.company.data.network.SyncPreferencesDto
import telefam.company.data.network.SyncSettingsResponseDto
import telefam.company.data.network.SystemStatusResponseDto
import telefam.company.data.network.TelefamApiService
import telefam.company.data.network.UpdateChatThemeRequestDto
import telefam.company.data.network.UpdatePostRequestDto
import telefam.company.data.network.UserBlockResponseDto
import telefam.company.data.network.UserConnectionsResponseDto
import telefam.company.data.network.UserMuteResponseDto
import telefam.company.data.network.UserProfileResponseDto
import telefam.company.data.network.UserReportRequestDto
import telefam.company.data.network.UserReportResponseDto
import telefam.company.data.network.UsernameCheckResponseDto
import telefam.company.data.network.VerifyLinkDto
import telefam.company.data.network.VerifyOtpDto
import telefam.company.data.network.ViewOnceOpenResponseDto
import telefam.company.data.network.VoiceMediaUploadResponseDto

abstract class AbstractTestChatApiStub : TelefamApiService {
  override suspend fun checkEmail(
    deviceFingerprint: String,
    email: String,
  ): Response<EmailCheckResponseDto> = Response.success(EmailCheckResponseDto(success = true, exists = false))

  override suspend fun requestOtp(
    deviceFingerprint: String,
    ipFingerprint: String,
    body: RequestOtpDto,
  ): Response<RequestOtpResponseDto> = Response.success(RequestOtpResponseDto(success = true))

  override suspend fun verifyOtp(
    deviceFingerprint: String,
    ipFingerprint: String,
    body: VerifyOtpDto,
  ): Response<AuthSessionResponseDto> = Response.success(AuthSessionResponseDto(success = true))

  override suspend fun verifyLink(
    deviceFingerprint: String,
    ipFingerprint: String,
    body: VerifyLinkDto,
  ): Response<AuthSessionResponseDto> = Response.success(AuthSessionResponseDto(success = true))

  override suspend fun refreshToken(
    deviceFingerprint: String,
    body: RefreshTokenDto,
  ): Response<AuthSessionResponseDto> = Response.success(AuthSessionResponseDto(success = true))

  override suspend fun verifyGoogleOAuth(
    deviceFingerprint: String,
    ipFingerprint: String,
    body: OAuthDto,
  ): Response<AuthSessionResponseDto> = Response.success(AuthSessionResponseDto(success = true))

  override suspend fun getPendingLoginRequests(
    bearer: String,
    deviceFingerprint: String,
  ): Response<PendingLoginRequestsResponseDto> = Response.success(PendingLoginRequestsResponseDto(success = true))

  override suspend fun getLoginRequestStatus(
    deviceFingerprint: String,
    pollToken: String,
    requestId: String,
  ): Response<LoginApprovalStatusResponseDto> = Response.success(LoginApprovalStatusResponseDto(success = true))

  override suspend fun respondToLoginRequest(
    bearer: String,
    deviceFingerprint: String,
    requestId: String,
    body: RespondLoginRequestDto,
  ): Response<RespondLoginRequestResponseDto> = Response.success(RespondLoginRequestResponseDto(success = true))

  override suspend fun syncDeviceContext(
    bearer: String,
    deviceFingerprint: String,
    body: DeviceContextTelemetryDto,
  ): Response<DeviceContextTelemetryResponseDto> = Response.success(DeviceContextTelemetryResponseDto(success = true))

  override suspend fun checkUsername(username: String): Response<UsernameCheckResponseDto> =
    Response.success(UsernameCheckResponseDto(available = true))

  override suspend fun uploadAvatar(
    bearer: String,
    deviceFingerprint: String,
    cropCenterX: Float,
    cropCenterY: Float,
    cropScale: Float,
    imageBody: RequestBody,
  ): Response<MediaUploadResponseDto> = Response.success(MediaUploadResponseDto(success = true))

  override suspend fun uploadProfileBackground(
    bearer: String,
    deviceFingerprint: String,
    imageBody: RequestBody,
  ): Response<ProfileBackgroundUploadResponseDto> = Response.success(ProfileBackgroundUploadResponseDto(success = true))

  override suspend fun uploadPostPhoto(
    bearer: String,
    deviceFingerprint: String,
    imageBody: RequestBody,
  ): Response<MediaUploadResponseDto> = Response.success(MediaUploadResponseDto(success = true))

  override suspend fun uploadPostVideo(
    bearer: String,
    deviceFingerprint: String,
    frameHashesCsv: String,
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    width: Int,
    height: Int,
    videoPart: MultipartBody.Part,
    thumbnailPart: MultipartBody.Part?,
  ): Response<MediaUploadResponseDto> = Response.success(MediaUploadResponseDto(success = true))

  override suspend fun searchSongs(
    bearer: String,
    deviceFingerprint: String,
    query: String,
  ): Response<SongsSearchResponseDto> = Response.success(SongsSearchResponseDto(success = true))

  override suspend fun searchHashtags(
    bearer: String,
    deviceFingerprint: String,
    query: String,
  ): Response<HashtagsSearchResponseDto> = Response.success(HashtagsSearchResponseDto(success = true))

  override suspend fun createPost(
    bearer: String,
    deviceFingerprint: String,
    body: CreatePostRequestDto,
  ): Response<CreatePostResponseDto> = Response.success(CreatePostResponseDto(success = true))

  override suspend fun updatePost(
    bearer: String,
    deviceFingerprint: String,
    postId: String,
    body: UpdatePostRequestDto,
  ): Response<CreatePostResponseDto> = Response.success(CreatePostResponseDto(success = true))

  override suspend fun deletePost(
    bearer: String,
    deviceFingerprint: String,
    postId: String,
  ): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))

  override suspend fun getFeeds(
    bearer: String,
    deviceFingerprint: String,
    tab: String,
    page: Int,
    pageSize: Int,
  ): Response<FeedPageResponseDto> = Response.success(FeedPageResponseDto(success = true))

  override suspend fun searchFeeds(
    bearer: String,
    deviceFingerprint: String,
    query: String,
    shortcut: String,
    page: Int,
    pageSize: Int,
  ): Response<FeedSearchResponseDto> = Response.success(FeedSearchResponseDto(success = true))

  override suspend fun interactFeed(
    bearer: String,
    deviceFingerprint: String,
    body: FeedInteractionRequestDto,
  ): Response<FeedInteractionResponseDto> = Response.success(FeedInteractionResponseDto(success = true))

  override suspend fun interactFeedBatch(
    bearer: String,
    deviceFingerprint: String,
    body: FeedInteractionBatchRequestDto,
  ): Response<SyncBatchResponseDto> = Response.success(SyncBatchResponseDto(success = true))

  override suspend fun setupProfile(
    bearer: String,
    deviceFingerprint: String,
    body: ProfileSetupDto,
  ): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))

  override suspend fun updateProfile(
    bearer: String,
    deviceFingerprint: String,
    body: ProfileUpdateRequestDto,
  ): Response<ProfileUpdateResponseDto> = Response.success(ProfileUpdateResponseDto(success = true))

  override suspend fun attachProfileBackground(
    bearer: String,
    deviceFingerprint: String,
    body: ProfileBackgroundAttachDto,
  ): Response<ProfileBackgroundListResponseDto> = Response.success(ProfileBackgroundListResponseDto(success = true))

  override suspend fun reorderProfileBackgrounds(
    bearer: String,
    deviceFingerprint: String,
    body: ProfileBackgroundReorderDto,
  ): Response<ProfileBackgroundListResponseDto> = Response.success(ProfileBackgroundListResponseDto(success = true))

  override suspend fun deleteProfileBackground(
    bearer: String,
    deviceFingerprint: String,
    mediaId: String,
  ): Response<ProfileBackgroundListResponseDto> = Response.success(ProfileBackgroundListResponseDto(success = true))

  override suspend fun recordAuditEvent(
    bearer: String,
    deviceFingerprint: String,
    body: AuditEventDto,
  ): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))

  override suspend fun syncBatch(
    bearer: String,
    deviceFingerprint: String,
    body: SyncBatchRequestDto,
  ): Response<SyncBatchResponseDto> = Response.success(SyncBatchResponseDto(success = true))

  override suspend fun listContacts(
    bearer: String,
    deviceFingerprint: String,
    filter: String,
    query: String,
    page: Int,
    pageSize: Int,
  ): Response<ContactsPageResponseDto> = Response.success(ContactsPageResponseDto(success = true))

  override suspend fun followContact(
    bearer: String,
    deviceFingerprint: String,
    body: ContactActionDto,
  ): Response<FollowMutationResponseDto> = Response.success(FollowMutationResponseDto(success = true))

  override suspend fun unfollowContact(
    bearer: String,
    deviceFingerprint: String,
    body: ContactActionDto,
  ): Response<FollowMutationResponseDto> = Response.success(FollowMutationResponseDto(success = true))

  override suspend fun syncContactActions(
    bearer: String,
    deviceFingerprint: String,
    body: ContactSyncBatchRequestDto,
  ): Response<SyncBatchResponseDto> = Response.success(SyncBatchResponseDto(success = true))

  override suspend fun getUserProfile(
    bearer: String,
    deviceFingerprint: String,
    userId: String,
  ): Response<UserProfileResponseDto> = Response.success(UserProfileResponseDto(success = true, userId = userId))

  override suspend fun getUserConnections(
    bearer: String,
    deviceFingerprint: String,
    userId: String,
    type: String,
    page: Int,
    pageSize: Int,
  ): Response<UserConnectionsResponseDto> = Response.success(UserConnectionsResponseDto(success = true))

  override suspend fun getUserFollowers(
    bearer: String,
    deviceFingerprint: String,
    userId: String,
    page: Int,
    pageSize: Int,
  ): Response<UserConnectionsResponseDto> = Response.success(UserConnectionsResponseDto(success = true))

  override suspend fun getUserFollowing(
    bearer: String,
    deviceFingerprint: String,
    userId: String,
    page: Int,
    pageSize: Int,
  ): Response<UserConnectionsResponseDto> = Response.success(UserConnectionsResponseDto(success = true))

  override suspend fun blockUser(
    bearer: String,
    deviceFingerprint: String,
    userId: String,
  ): Response<UserBlockResponseDto> = Response.success(UserBlockResponseDto(success = true, targetUserId = userId))

  override suspend fun unblockUser(
    bearer: String,
    deviceFingerprint: String,
    userId: String,
  ): Response<UserBlockResponseDto> = Response.success(UserBlockResponseDto(success = true, targetUserId = userId))

  override suspend fun reportUser(
    bearer: String,
    deviceFingerprint: String,
    userId: String,
    body: UserReportRequestDto,
  ): Response<UserReportResponseDto> = Response.success(UserReportResponseDto(success = true, targetUserId = userId))

  override suspend fun muteUser(
    bearer: String,
    deviceFingerprint: String,
    userId: String,
  ): Response<UserMuteResponseDto> = Response.success(UserMuteResponseDto(success = true, userId = userId))

  override suspend fun unmuteUser(
    bearer: String,
    deviceFingerprint: String,
    userId: String,
  ): Response<UserMuteResponseDto> = Response.success(UserMuteResponseDto(success = true, userId = userId))

  override suspend fun getSettings(
    bearer: String,
    deviceFingerprint: String,
  ): Response<SettingsBundleResponseDto> = Response.success(SettingsBundleResponseDto(success = true))

  override suspend fun updatePrivacySettings(
    bearer: String,
    deviceFingerprint: String,
    body: PrivacySettingsDto,
  ): Response<SettingsBundleResponseDto> = Response.success(SettingsBundleResponseDto(success = true))

  override suspend fun updateNotificationSettings(
    bearer: String,
    deviceFingerprint: String,
    body: NotificationSettingsDto,
  ): Response<SettingsBundleResponseDto> = Response.success(SettingsBundleResponseDto(success = true))

  override suspend fun updateContentPreferences(
    bearer: String,
    deviceFingerprint: String,
    body: ContentPreferencesDto,
  ): Response<SettingsBundleResponseDto> = Response.success(SettingsBundleResponseDto(success = true))

  override suspend fun listBlockedAccounts(
    bearer: String,
    deviceFingerprint: String,
    query: String,
  ): Response<RestrictedAccountsResponseDto> = Response.success(RestrictedAccountsResponseDto(success = true))

  override suspend fun listMutedAccounts(
    bearer: String,
    deviceFingerprint: String,
    query: String,
  ): Response<RestrictedAccountsResponseDto> = Response.success(RestrictedAccountsResponseDto(success = true))

  override suspend fun submitProblemReport(
    bearer: String,
    deviceFingerprint: String,
    body: ProblemReportRequestDto,
  ): Response<ProblemReportResponseDto> = Response.success(ProblemReportResponseDto(success = true))

  override suspend fun listSupportTickets(
    bearer: String,
    deviceFingerprint: String,
  ): Response<SupportTicketsResponseDto> = Response.success(SupportTicketsResponseDto(success = true))

  override suspend fun searchFaqs(
    bearer: String,
    deviceFingerprint: String,
    query: String,
  ): Response<FaqListResponseDto> = Response.success(FaqListResponseDto(success = true))

  override suspend fun getSystemStatus(
    bearer: String,
    deviceFingerprint: String,
  ): Response<SystemStatusResponseDto> = Response.success(SystemStatusResponseDto(success = true))

  override suspend fun getLegalDocuments(
    bearer: String,
    deviceFingerprint: String,
  ): Response<LegalDocumentsResponseDto> = Response.success(LegalDocumentsResponseDto(success = true))

  override suspend fun checkAppVersion(
    bearer: String,
    deviceFingerprint: String,
    version: String,
  ): Response<AppVersionResponseDto> = Response.success(AppVersionResponseDto(success = true))

  override suspend fun logout(
    bearer: String,
    deviceFingerprint: String,
    body: LogoutRequestDto,
  ): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))

  override suspend fun getChatPrivacy(
    bearer: String,
    deviceFingerprint: String,
  ): Response<ChatPrivacyResponseDto> = Response.success(ChatPrivacyResponseDto(success = true))

  override suspend fun updateChatPrivacy(
    bearer: String,
    deviceFingerprint: String,
    body: ChatPrivacySettingsDto,
  ): Response<ChatPrivacyResponseDto> = Response.success(ChatPrivacyResponseDto(success = true))

  override suspend fun updateChatTheme(
    bearer: String,
    deviceFingerprint: String,
    body: UpdateChatThemeRequestDto,
  ): Response<ChatPrivacyResponseDto> = Response.success(ChatPrivacyResponseDto(success = true))

  override suspend fun getSyncSettings(
    bearer: String,
    deviceFingerprint: String,
  ): Response<SyncSettingsResponseDto> = Response.success(SyncSettingsResponseDto(success = true))

  override suspend fun updateSyncSettings(
    bearer: String,
    deviceFingerprint: String,
    body: SyncPreferencesDto,
  ): Response<SyncSettingsResponseDto> = Response.success(SyncSettingsResponseDto(success = true))

  override suspend fun syncNow(
    bearer: String,
    deviceFingerprint: String,
    body: ManualSyncRequestDto,
  ): Response<ManualSyncResponseDto> = Response.success(ManualSyncResponseDto(success = true))

  override suspend fun downloadMedia(url: String): Response<ResponseBody> =
    Response.success(ByteArray(0).toResponseBody("application/octet-stream".toMediaType()))

  override suspend fun uploadE2eeKeys(
    bearer: String,
    deviceFingerprint: String,
    body: E2eeKeyBundleUploadDto,
  ): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))

  override suspend fun fetchPeerE2eeKeys(
    bearer: String,
    deviceFingerprint: String,
    userId: String,
  ): Response<E2eeKeyBundleResponseDto> = Response.success(E2eeKeyBundleResponseDto(success = true, userId = userId))

  override suspend fun listChatConversations(
    bearer: String,
    deviceFingerprint: String,
    archived: Boolean,
  ): Response<ChatConversationsResponseDto> = Response.success(ChatConversationsResponseDto(success = true))

  override suspend fun openDirectConversation(
    bearer: String,
    deviceFingerprint: String,
    userId: String,
  ): Response<OpenDirectConversationResponseDto> = Response.success(OpenDirectConversationResponseDto(success = true))

  override suspend fun batchConversationAction(
    bearer: String,
    deviceFingerprint: String,
    body: BatchConversationActionRequestDto,
  ): Response<ChatConversationsResponseDto> = Response.success(ChatConversationsResponseDto(success = true))

  override suspend fun listChatMessages(
    bearer: String,
    deviceFingerprint: String,
    conversationId: String,
    sinceMs: Long?,
  ): Response<ChatMessagesListResponseDto> = Response.success(ChatMessagesListResponseDto(success = true))

  override suspend fun sendChatMessage(
    bearer: String,
    deviceFingerprint: String,
    conversationId: String,
    body: SendChatMessageRequestDto,
  ): Response<ChatMessageSingleResponseDto> = Response.success(ChatMessageSingleResponseDto(success = true))

  override suspend fun editChatMessage(
    bearer: String,
    deviceFingerprint: String,
    messageId: String,
    body: EditChatMessageRequestDto,
  ): Response<ChatMessageSingleResponseDto> = Response.success(ChatMessageSingleResponseDto(success = true))

  override suspend fun deleteChatMessage(
    bearer: String,
    deviceFingerprint: String,
    messageId: String,
    mode: String,
  ): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))

  override suspend fun sendChatReceipt(
    bearer: String,
    deviceFingerprint: String,
    messageId: String,
    body: ChatReceiptRequestDto,
  ): Response<ChatMessageSingleResponseDto> = Response.success(ChatMessageSingleResponseDto(success = true))

  override suspend fun openViewOnceVoice(
    bearer: String,
    deviceFingerprint: String,
    messageId: String,
  ): Response<ViewOnceOpenResponseDto> = Response.success(ViewOnceOpenResponseDto(success = true))

  override suspend fun consumeViewOnceVoice(
    bearer: String,
    deviceFingerprint: String,
    messageId: String,
  ): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))

  override suspend fun uploadVoiceMedia(
    bearer: String,
    deviceFingerprint: String,
    conversationId: RequestBody,
    durationMs: RequestBody,
    viewOnce: RequestBody,
    file: MultipartBody.Part,
  ): Response<VoiceMediaUploadResponseDto> = Response.success(VoiceMediaUploadResponseDto(success = true))

  override suspend fun recordSecurityFlag(
    bearer: String,
    deviceFingerprint: String,
    conversationId: String,
    body: RecordSecurityFlagRequestDto,
  ): Response<SecurityFlagResponseDto> = Response.success(SecurityFlagResponseDto(success = true))

  override suspend fun listSecurityFlags(
    bearer: String,
    deviceFingerprint: String,
    conversationId: String,
  ): Response<SecurityFlagsListResponseDto> = Response.success(SecurityFlagsListResponseDto(success = true))

  override suspend fun getGreetingSticker(
    bearer: String,
    deviceFingerprint: String,
    peerId: String,
  ): Response<GiflyGreetingResponseDto> = Response.success(GiflyGreetingResponseDto(success = true))

  override suspend fun searchGiflyMedia(
    bearer: String,
    deviceFingerprint: String,
    type: String,
    query: String,
    page: Int,
    perPage: Int,
  ): Response<GiflySearchResponseDto> = Response.success(GiflySearchResponseDto(success = true))

  override suspend fun searchChatsAndPeople(
    bearer: String,
    deviceFingerprint: String,
    query: String,
  ): Response<ChatGlobalSearchResponseDto> = Response.success(ChatGlobalSearchResponseDto(success = true))

  override suspend fun getLinkPreview(
    bearer: String,
    deviceFingerprint: String,
    url: String,
  ): Response<LinkPreviewResponseDto> = Response.success(LinkPreviewResponseDto(success = true))

  override suspend fun listSavedLinks(
    bearer: String,
    deviceFingerprint: String,
    query: String,
  ): Response<SavedLinksListResponseDto> = Response.success(SavedLinksListResponseDto(success = true))

  override suspend fun saveLink(
    bearer: String,
    deviceFingerprint: String,
    body: SaveLinkRequestDto,
  ): Response<SaveLinkResponseDto> = Response.success(SaveLinkResponseDto(success = true))

  override suspend fun deleteSavedLink(
    bearer: String,
    deviceFingerprint: String,
    id: String,
  ): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))

  override suspend fun registerPushToken(
    bearer: String,
    deviceFingerprint: String,
    body: RegisterPushTokenRequestDto,
  ): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))

  override suspend fun unregisterPushToken(
    bearer: String,
    deviceFingerprint: String,
    allDevices: Boolean,
  ): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))

  override suspend fun listNotifications(
    bearer: String,
    deviceFingerprint: String,
    limit: Int,
  ): Response<NotificationsListResponseDto> = Response.success(NotificationsListResponseDto(success = true))

  override suspend fun markNotificationRead(
    bearer: String,
    deviceFingerprint: String,
    body: MarkNotificationReadRequestDto,
  ): Response<NotificationUnreadCountResponseDto> = Response.success(NotificationUnreadCountResponseDto(success = true))

  override suspend fun markAllNotificationsRead(
    bearer: String,
    deviceFingerprint: String,
  ): Response<NotificationUnreadCountResponseDto> = Response.success(NotificationUnreadCountResponseDto(success = true))

  override suspend fun getCallIceConfig(
    bearer: String,
    deviceFingerprint: String,
  ): Response<IceConfigResponseDto> = Response.success(IceConfigResponseDto(success = true))

  override suspend fun startCall(
    bearer: String,
    deviceFingerprint: String,
    body: StartCallRequestDto,
  ): Response<StartCallResponseDto> = Response.success(StartCallResponseDto(success = true, allowed = true))

  override suspend fun getCallState(
    bearer: String,
    deviceFingerprint: String,
    callId: String,
    sinceSeq: Long,
  ): Response<CallStateResponseDto> = Response.success(CallStateResponseDto(success = true))

  override suspend fun sendCallSignal(
    bearer: String,
    deviceFingerprint: String,
    callId: String,
    body: CallSignalRequestDto,
  ): Response<CallActionResponseDto> = Response.success(CallActionResponseDto(success = true))
}
