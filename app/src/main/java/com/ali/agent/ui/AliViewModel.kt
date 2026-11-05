package com.ali.agent.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ali.agent.ai.CognitiveOrchestrator
import com.ali.agent.ai.SemanticMemoryEngine
import com.ali.agent.ai.llm.LlamaBridge
import com.ali.agent.ai.llm.ModelDownloadManager
import com.ali.agent.ai.llm.OfflineModelCatalogItem
import com.ali.agent.data.db.AliDatabase
import com.ali.agent.data.db.ChatMessageEntity
import com.ali.agent.data.db.CuriosityTopicEntity
import com.ali.agent.data.db.KnowledgeNodeEntity
import com.ali.agent.data.db.MobileTaskEntity
import com.ali.agent.data.db.QuickNoteEntity
import com.ali.agent.data.db.UserMemoryEntity
import com.ali.agent.privacy.PrivacyFirewall
import com.ali.agent.privacy.PrivateCuriosityEngine
import com.ali.agent.privacy.SanitizationResult
import com.ali.agent.system.AliVoiceManager
import com.ali.agent.system.MobileActionExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AliViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AliDatabase.getInstance(application)
    private val dao = db.aliDao()

    val privacyFirewall = PrivacyFirewall(application)
    val llamaBridge = LlamaBridge()
    val modelDownloadManager = ModelDownloadManager(application, privacyFirewall)

    private val memoryEngine = SemanticMemoryEngine(dao)
    private val curiosityEngine = PrivateCuriosityEngine(dao, privacyFirewall)
    private val actionExecutor = MobileActionExecutor(application, dao)

    private val orchestrator = CognitiveOrchestrator(
        dao = dao,
        memoryEngine = memoryEngine,
        curiosityEngine = curiosityEngine,
        firewall = privacyFirewall,
        actionExecutor = actionExecutor,
        llamaBridge = llamaBridge
    )

    val voiceManager = AliVoiceManager(application) { spokenText ->
        sendMessage(spokenText)
    }

    // Reactive Database Flows
    val messages = dao.observeMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userMemories = dao.observeUserMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knowledgeNodes = dao.observeKnowledgeNodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val curiosityQueue = dao.observeCuriosityQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privacyAuditLogs = dao.observePrivacyAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks = dao.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes = dao.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    val isOnlineLearningEnabled = privacyFirewall.isOnlineLearningEnabled

    fun sendMessage(rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            dao.insertMessage(
                ChatMessageEntity(
                    role = "user",
                    content = trimmed
                )
            )
            _isThinking.value = true
            try {
                val response = orchestrator.processUserTurn(trimmed)
                dao.insertMessage(
                    ChatMessageEntity(
                        role = "ali",
                        content = response.replyText,
                        innerThought = response.innerThought,
                        detectedEmotion = response.emotionBadge,
                        actionExecuted = response.actionBadge,
                        learnedSource = response.learnedSourceBadge
                    )
                )
                voiceManager.speakHumanLike(response.replyText, response.emotionBadge)
            } finally {
                _isThinking.value = false
            }
        }
    }

    fun toggleOnlineLearning(enabled: Boolean) {
        privacyFirewall.setOnlineLearningEnabled(enabled)
        if (enabled) {
            viewModelScope.launch {
                val syncedCount = curiosityEngine.syncPendingCuriosityQueue()
                if (syncedCount > 0) {
                    dao.insertMessage(
                        ChatMessageEntity(
                            role = "ali",
                            content = "Now that you allowed me private online access, I just anonymously researched **$syncedCount queued topic(s)** from my Curiosity Queue and saved their summaries permanently into my offline knowledge base!",
                            innerThought = "Synced $syncedCount pending curiosity topics via PrivacyFirewall -> Zero personal data transmitted",
                            detectedEmotion = "Excited & Celebratory",
                            learnedSource = "Curiosity Queue Auto-Sync"
                        )
                    )
                }
            }
        }
    }

    fun syncCuriosityQueueNow() {
        viewModelScope.launch {
            curiosityEngine.syncPendingCuriosityQueue()
        }
    }

    fun previewSanitizer(sampleInput: String): SanitizationResult {
        return privacyFirewall.sanitizeToAnonymousTopic(sampleInput)
    }

    fun teachAliDirectly(topic: String, explanation: String) {
        if (topic.isBlank() || explanation.isBlank()) return
        viewModelScope.launch {
            dao.upsertKnowledgeNode(
                KnowledgeNodeEntity(
                    topic = topic.trim(),
                    summary = explanation.trim(),
                    keywords = topic.lowercase().trim(),
                    sourceType = "USER_TAUGHT_OFFLINE"
                )
            )
        }
    }

    fun addManualUserMemory(category: String, key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        viewModelScope.launch {
            dao.upsertUserMemory(
                UserMemoryEntity(
                    category = category,
                    factKey = key.trim(),
                    factValue = value.trim()
                )
            )
        }
    }

    fun deleteUserMemory(memory: UserMemoryEntity) {
        viewModelScope.launch {
            dao.deleteUserMemory(memory)
        }
    }

    fun deleteKnowledgeNode(node: KnowledgeNodeEntity) {
        viewModelScope.launch {
            dao.deleteKnowledgeNode(node)
        }
    }

    fun deleteCuriosityItem(item: CuriosityTopicEntity) {
        viewModelScope.launch {
            dao.deleteCuriosityTopic(item)
        }
    }

    fun addTask(title: String, dueTime: String, priority: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.insertTask(
                MobileTaskEntity(
                    title = title.trim(),
                    dueTimeText = dueTime.ifBlank { "Today" },
                    priority = priority
                )
            )
        }
    }

    fun toggleTaskCompleted(task: MobileTaskEntity) {
        viewModelScope.launch {
            dao.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: MobileTaskEntity) {
        viewModelScope.launch {
            dao.deleteTask(task)
        }
    }

    fun addNote(title: String, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            dao.insertNote(
                QuickNoteEntity(
                    title = title.ifBlank { "Quick Thought" }.trim(),
                    body = body.trim()
                )
            )
        }
    }

    fun deleteNote(note: QuickNoteEntity) {
        viewModelScope.launch {
            dao.deleteNote(note)
        }
    }

    fun downloadAndActivateModel(item: OfflineModelCatalogItem) {
        viewModelScope.launch {
            val file = if (modelDownloadManager.isModelDownloaded(item)) {
                modelDownloadManager.getModelFile(item)
            } else {
                modelDownloadManager.downloadModel(item)
            }
            if (file != null && file.exists()) {
                llamaBridge.loadGgufModel(file)
            }
        }
    }

    fun deleteDownloadedModel(item: OfflineModelCatalogItem) {
        viewModelScope.launch {
            llamaBridge.unloadModel()
            modelDownloadManager.deleteModelFile(item)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
