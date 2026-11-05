package com.ali.agent.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AliDao {

    // --- Chat Messages ---
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun observeMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()

    // --- User Personal Memories ---
    @Query("SELECT * FROM user_memories ORDER BY lastReferencedAt DESC")
    fun observeUserMemories(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memories ORDER BY lastReferencedAt DESC")
    suspend fun getAllUserMemories(): List<UserMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserMemory(memory: UserMemoryEntity)

    @Delete
    suspend fun deleteUserMemory(memory: UserMemoryEntity)

    // --- Knowledge Graph Nodes ---
    @Query("SELECT * FROM knowledge_nodes ORDER BY learnedAt DESC")
    fun observeKnowledgeNodes(): Flow<List<KnowledgeNodeEntity>>

    @Query("SELECT * FROM knowledge_nodes")
    suspend fun getAllKnowledgeNodes(): List<KnowledgeNodeEntity>

    @Query("SELECT * FROM knowledge_nodes WHERE LOWER(topic) LIKE '%' || LOWER(:query) || '%' OR LOWER(keywords) LIKE '%' || LOWER(:query) || '%' OR LOWER(summary) LIKE '%' || LOWER(:query) || '%' ORDER BY accessCount DESC LIMIT 5")
    suspend fun searchKnowledgeNodes(query: String): List<KnowledgeNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKnowledgeNode(node: KnowledgeNodeEntity)

    @Delete
    suspend fun deleteKnowledgeNode(node: KnowledgeNodeEntity)

    // --- Curiosity Queue (Topics to learn when Online Mode is enabled) ---
    @Query("SELECT * FROM curiosity_queue ORDER BY createdAt DESC")
    fun observeCuriosityQueue(): Flow<List<CuriosityTopicEntity>>

    @Query("SELECT * FROM curiosity_queue WHERE status = 'PENDING_INTERNET' ORDER BY createdAt ASC")
    suspend fun getPendingCuriosityTopics(): List<CuriosityTopicEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueueCuriosityTopic(topic: CuriosityTopicEntity)

    @Query("UPDATE curiosity_queue SET status = 'LEARNED', resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun markCuriosityTopicLearned(id: Long, resolvedAt: Long)

    @Delete
    suspend fun deleteCuriosityTopic(topic: CuriosityTopicEntity)

    // --- Privacy Audit Logs ---
    @Query("SELECT * FROM privacy_audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun observePrivacyAuditLogs(): Flow<List<PrivacyAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrivacyAuditLog(log: PrivacyAuditEntity)

    @Query("DELETE FROM privacy_audit_logs")
    suspend fun clearPrivacyAuditLogs()

    // --- Mobile Day-to-Day Tasks ---
    @Query("SELECT * FROM mobile_tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun observeTasks(): Flow<List<MobileTaskEntity>>

    @Query("SELECT * FROM mobile_tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    suspend fun getPendingTasks(): List<MobileTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: MobileTaskEntity): Long

    @Update
    suspend fun updateTask(task: MobileTaskEntity)

    @Delete
    suspend fun deleteTask(task: MobileTaskEntity)

    // --- Quick Notes ---
    @Query("SELECT * FROM quick_notes ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<QuickNoteEntity>>

    @Query("SELECT * FROM quick_notes ORDER BY updatedAt DESC")
    suspend fun getAllNotes(): List<QuickNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: QuickNoteEntity): Long

    @Delete
    suspend fun deleteNote(note: QuickNoteEntity)
}
