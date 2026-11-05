package com.ali.agent.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" or "ali"
    val content: String,
    val innerThought: String = "",
    val detectedEmotion: String = "Thoughtful",
    val actionExecuted: String? = null,
    val learnedSource: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_memories",
    indices = [Index(value = ["factKey"], unique = true)]
)
data class UserMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // Identity, Preference, Goal, Habit, Relationship, LifeContext
    val factKey: String,
    val factValue: String,
    val confidence: Float = 0.95f,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReferencedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "knowledge_nodes",
    indices = [Index(value = ["topic"], unique = true)]
)
data class KnowledgeNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,
    val summary: String,
    val keywords: String,
    val sourceType: String, // USER_TAUGHT_OFFLINE, PRIVATE_ONLINE_WIKI, PRIVATE_ONLINE_DDG, CORE_KNOWLEDGE
    val referenceUrl: String = "",
    val learnedAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 1
)

@Entity(
    tableName = "curiosity_queue",
    indices = [Index(value = ["sanitizedQuery"], unique = true)]
)
data class CuriosityTopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawTopic: String,
    val sanitizedQuery: String,
    val status: String = "PENDING_INTERNET", // PENDING_INTERNET, LEARNED
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

@Entity(tableName = "privacy_audit_logs")
data class PrivacyAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // SANITIZED_QUERY_SENT, BLOCKED_OFFLINE, PII_SCRUBBED
    val strippedPiiSummary: String,
    val outboundAnonymousQuery: String,
    val destinationHost: String
)

@Entity(tableName = "mobile_tasks")
data class MobileTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueTimeText: String = "Today",
    val priority: String = "NORMAL",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quick_notes")
data class QuickNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val tags: String = "general",
    val updatedAt: Long = System.currentTimeMillis()
)
