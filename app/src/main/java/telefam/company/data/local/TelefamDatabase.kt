package telefam.company.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    RateLimitStateEntity::class,
    RateLimitHitEntity::class,
    UserProfileEntity::class,
    MediaUploadEntity::class,
    RefreshTokenRecordEntity::class,
    AuditLogEntity::class,
    FamilyCircleEntity::class,
    FamilyMessageEntity::class,
    OfflineActionEntity::class,
    CachedContactEntity::class,
    ContactSyncActionEntity::class,
    CachedHashtagEntity::class,
    PendingPostUploadEntity::class,
    CachedFeedPostEntity::class,
    PendingFeedInteractionEntity::class,
    CachedUserProfileEntity::class,
    CachedUserConnectionEntity::class,
    CachedSettingsEntity::class,
    CachedRestrictedAccountEntity::class,
    SupportTicketEntity::class,
    PendingSettingsOpEntity::class,
    CachedChatPrivacyEntity::class,
    SignalKeyStateEntity::class,
    ChatConversationEntity::class,
    ChatMessageEntity::class,
    ChatSecurityFlagEntity::class,
    PendingChatOpEntity::class,
    CachedLinkPreviewEntity::class,
    SavedLinkEntity::class,
    CachedNotificationEntity::class,
  ],
  version = 12,
  exportSchema = false,
)
abstract class TelefamDatabase : RoomDatabase() {
  abstract fun telefamDao(): TelefamDao

  companion object {
    @Volatile
    private var INSTANCE: TelefamDatabase? = null

    fun getInstance(context: Context): TelefamDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          TelefamDatabase::class.java,
          "telefam_company.db",
        )
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}
