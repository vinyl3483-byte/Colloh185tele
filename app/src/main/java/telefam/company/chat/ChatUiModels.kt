package telefam.company.chat

import telefam.company.data.local.ChatConversationEntity
import telefam.company.data.local.ChatMessageEntity
import telefam.company.data.local.ChatSecurityFlagEntity
import telefam.company.data.network.GiflyMediaItemDto
import telefam.company.data.repository.ChatSearchBundleResult

data class ViewOnceActiveOverlayState(
  val messageId: String,
  val conversationId: String,
  val mediaSource: String,
  val durationMs: Long,
  val ttlSec: Int,
  val remainingSec: Int,
  val startedAtMs: Long,
)

data class UndoChatBatchState(
  val action: String,
  val label: String,
  val snapshot: List<ChatConversationEntity>,
)

data class TelefamChatUiState(
  val activeConversationId: String? = null,
  val conversations: List<ChatConversationEntity> = emptyList(),
  val activeMessages: List<ChatMessageEntity> = emptyList(),
  val activeSecurityFlags: List<ChatSecurityFlagEntity> = emptyList(),
  val typingModesByConversation: Map<String, String> = emptyMap(),
  val greetingSticker: GiflyMediaItemDto? = null,
  val isLoadingMessages: Boolean = false,
  val composerText: String = "",
  val editingMessageId: String? = null,
  val forwardingMessage: ChatMessageEntity? = null,
  val unsendingMessageIds: Set<String> = emptySet(),
  val voiceRecording: VoiceRecordingState = VoiceRecordingState(),
  val voicePlayback: VoicePlaybackState = VoicePlaybackState(),
  val voicePreviewViewOnce: Boolean = false,
  val voicePreviewTtlSec: Int = 10,
  val activeViewOnceOverlay: ViewOnceActiveOverlayState? = null,
  val giflyPickerOpen: Boolean = false,
  val giflyActiveTab: String = "stickers",
  val giflyQuery: String = "",
  val giflyItems: List<GiflyMediaItemDto> = emptyList(),
  val giflyPage: Int = 1,
  val giflyHasNext: Boolean = false,
  val giflyIsLoading: Boolean = false,
  val giflyIsLoadingMore: Boolean = false,
  val giflyPreviewItem: GiflyMediaItemDto? = null,
  val isMultiSelectMode: Boolean = false,
  val selectedConversationIds: Set<String> = emptySet(),
  val undoBatchState: UndoChatBatchState? = null,
  val isHomeSearchOpen: Boolean = false,
  val homeSearchQuery: String = "",
  val homeSearchResults: ChatSearchBundleResult = ChatSearchBundleResult(emptyList(), emptyList(), emptyList()),
  val isSearchingHomeChats: Boolean = false,
  val sharedMediaTab: String? = null,
  val toastMessage: String? = null,
) {
  val activeConversation: ChatConversationEntity?
    get() = activeConversationId?.let { id -> conversations.firstOrNull { it.conversationId == id } }

  val totalUnreadCount: Int
    get() = conversations.sumOf { it.unreadCount }
}
