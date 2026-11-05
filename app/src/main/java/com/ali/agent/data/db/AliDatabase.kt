package com.ali.agent.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ChatMessageEntity::class,
        UserMemoryEntity::class,
        KnowledgeNodeEntity::class,
        CuriosityTopicEntity::class,
        PrivacyAuditEntity::class,
        MobileTaskEntity::class,
        QuickNoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AliDatabase : RoomDatabase() {
    abstract fun aliDao(): AliDao

    companion object {
        @Volatile
        private var INSTANCE: AliDatabase? = null

        fun getInstance(context: Context): AliDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AliDatabase::class.java,
                    "ali_private_brain.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.aliDao()?.let { dao ->
                                    seedInitialMemoryAndKnowledge(dao)
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialMemoryAndKnowledge(dao: AliDao) {
            dao.insertMessage(
                ChatMessageEntity(
                    role = "ali",
                    content = "Hey there! I'm ALI — your Advanced Learning Interface. I live right here on your phone, completely offline and private. We can talk naturally about anything on your mind, organize your day, set alarms or reminders, or you can teach me new things. And whenever you flip on my Private Curiosity switch, I'll anonymously research topics you mention online—stripping away anything personal so your life stays 100% yours. What's on your mind today?",
                    innerThought = "Initializing local cognitive workspace -> Verifying Privacy Firewall (Strict Offline Default) -> Ready to connect with user.",
                    detectedEmotion = "Warm & Curious",
                    learnedSource = "On-Device Core"
                )
            )

            // Seed initial foundational offline knowledge nodes
            val starterKnowledge = listOf(
                KnowledgeNodeEntity(
                    topic = "ALI Architecture & Privacy Promise",
                    summary = "ALI (Advanced Learning Interface) is a 100% free, unlimited, on-device thinking AI companion. All personal memories, conversations, tasks, and notes are stored strictly inside an on-device SQLite database with cloud backups disabled. When internet learning is permitted, ALI's Privacy Sanitizer strips all names, pronouns, and personal identifiers before looking up public encyclopedic facts.",
                    keywords = "ali, advanced learning interface, privacy, offline ai, security, how do you work",
                    sourceType = "CORE_KNOWLEDGE"
                ),
                KnowledgeNodeEntity(
                    topic = "Neuroplasticity & Human Learning",
                    summary = "Neuroplasticity is the brain's ability to reorganize itself by forming new neural connections throughout life. Spaced repetition, active recall, deep sleep, and connecting new ideas to emotional experiences dramatically accelerate long-term memory consolidation.",
                    keywords = "brain, learning, memory, neuroplasticity, study, focus, habits",
                    sourceType = "CORE_KNOWLEDGE"
                ),
                KnowledgeNodeEntity(
                    topic = "Pomodoro & Deep Work Flow",
                    summary = "Human attention peaks in 45-to-90-minute ultradian cycles. Pairing focused sprints without notification interrupts followed by 10-minute physical rest resets dopamine and prevents cognitive fatigue.",
                    keywords = "productivity, focus, pomodoro, time management, work, study, procrastination",
                    sourceType = "CORE_KNOWLEDGE"
                ),
                KnowledgeNodeEntity(
                    topic = "On-Device GGUF Quantized Models",
                    summary = "GGUF (GPT-Generated Unified Format) allows billion-parameter language models like Llama 3.2 and Qwen 2.5 to run directly on mobile ARM64 CPUs/NPUs using 4-bit quantization without needing any cloud server or subscription.",
                    keywords = "gguf, llama, qwen, local llm, quantization, offline model",
                    sourceType = "CORE_KNOWLEDGE"
                )
            )
            starterKnowledge.forEach { dao.upsertKnowledgeNode(it) }
        }
    }
}
