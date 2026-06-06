package com.tradigo.ultra.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isCard: Boolean = false,
    val metricsType: String? = null,
    val metricsDirection: String? = null,
    val metricsTarget: String? = null,
    val metricsConfidence: String? = null
)

@Entity(tableName = "user_config")
data class UserConfigEntity(
    @PrimaryKey val id: String = "current_user",
    val apiKeyEncrypted: String = "",
    val apiSecretEncrypted: String = "",
    val userTier: String = "FREE", // "FREE" or "PREMIUM"
    val xp: Int = 120,
    val streakDays: Int = 4,
    val lastLoginDate: String = "",
    val balanceUsdt: Double = 12480.50
)

@Dao
interface MainDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<<ChatMessageEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
    
    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
    
    @Query("SELECT * FROM user_config WHERE id = 'current_user'")
    suspend fun getUserConfig(): UserConfigEntity?
    
    @Query("SELECT * FROM user_config WHERE id = 'current_user'")
    fun getUserConfigFlow(): Flow<UserConfigEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserConfig(config: UserConfigEntity)
}

@Database(entities = [ChatMessageEntity::class, UserConfigEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): MainDao
}
