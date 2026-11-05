package com.ali.agent.ai

import com.ali.agent.ai.llm.LlamaBridge
import com.ali.agent.data.db.AliDao
import com.ali.agent.privacy.LearnedTopicResult
import com.ali.agent.privacy.PrivacyFirewall
import com.ali.agent.privacy.PrivateCuriosityEngine
import com.ali.agent.system.MobileActionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CognitiveOrchestrator(
    private val dao: AliDao,
    private val memoryEngine: SemanticMemoryEngine,
    private val curiosityEngine: PrivateCuriosityEngine,
    private val firewall: PrivacyFirewall,
    private val actionExecutor: MobileActionExecutor,
    private val llamaBridge: LlamaBridge,
    private val autonomousEngine: AutonomousCognitiveEngine = AutonomousCognitiveEngine()
) {

    suspend fun processUserTurn(userInput: String): CognitiveResponse = withContext(Dispatchers.IO) {
        val emotion = HumanPersonaEngine.detectEmotionAndTone(userInput)

        // Step 1: Extract and store any new personal memories or offline teachings from user's speech/text
        val extraction = memoryEngine.extractAndStoreMemoriesFromInput(userInput)

        // Step 2: Check if user requested a day-to-day mobile phone action (Alarm, Timer, Task, Note, SMS, App Launch)
        val actionOutcome = actionExecutor.detectAndExecute(userInput)
        if (actionOutcome != null && actionOutcome.executed) {
            return@withContext CognitiveResponse(
                replyText = actionOutcome.humanConfirmation,
                innerThought = "Detected mobile intent -> Executed ${actionOutcome.actionBadge} locally on device",
                emotionBadge = "Helpful & Proactive",
                learnedSourceBadge = "Device Action Engine",
                actionBadge = actionOutcome.actionBadge
            )
        }

        // Step 3: Check deterministic math / unit conversion
        val mathResult = MathAndLogicEngine.tryEvaluate(userInput)

        // Step 4: Retrieve relevant local memories and offline knowledge nodes
        val allUserMemories = dao.getAllUserMemories()
        val offlineNodes = memoryEngine.retrieveRelevantKnowledge(userInput, topK = 3)
        val pendingTasks = dao.getPendingTasks()
        val notes = dao.getAllNotes()

        // Step 5: If user is asking about a topic or mentioning an unfamiliar concept, invoke PrivateCuriosityEngine
        val shouldCheckCuriosity = mathResult == null &&
                extraction.extractedKnowledgeNodes.isEmpty() &&
                (offlineNodes.isEmpty() || firewall.isOnlineLearningEnabled.value) &&
                looksLikeTopicInquiry(userInput)

        val curiosityResult: LearnedTopicResult? = if (shouldCheckCuriosity) {
            curiosityEngine.learnFromUserMention(userInput)
        } else {
            null
        }

        // Step 6: If a native GGUF model is loaded and llama.cpp backend is linked, try native generation first
        if (llamaBridge.isNativeModelLoaded.value) {
            val sysPrompt = HumanPersonaEngine.buildGgufSystemPrompt(
                memories = allUserMemories,
                knowledge = offlineNodes,
                pendingTasks = pendingTasks,
                emotion = emotion
            )
            val fullPrompt = "<|im_start|>system\n$sysPrompt<|im_end|>\n<|im_start|>user\n$userInput<|im_end|>\n<|im_start|>assistant\n"
            val nativeOut = llamaBridge.tryNativeCompletion(fullPrompt)
            if (!nativeOut.isNullOrBlank()) {
                return@withContext CognitiveResponse(
                    replyText = nativeOut.trim(),
                    innerThought = "Generated via on-device GGUF model (${llamaBridge.loadedModelName.value}) with ${allUserMemories.size} memory nodes & ${offlineNodes.size} RAG context nodes",
                    emotionBadge = emotion.displayBadge,
                    learnedSourceBadge = "On-Device GGUF (${llamaBridge.loadedModelName.value})"
                )
            }
        }

        // Step 7: Synthesize deeply human response via AutonomousCognitiveEngine
        autonomousEngine.synthesizeHumanResponse(
            userInput = userInput,
            emotion = emotion,
            memories = allUserMemories,
            newlyExtractedMemories = extraction.extractedUserFacts,
            newlyTaughtNodes = extraction.extractedKnowledgeNodes,
            retrievedOfflineKnowledge = offlineNodes,
            onlineCuriosityResult = curiosityResult,
            mathResult = mathResult,
            pendingTasks = pendingTasks,
            notes = notes,
            isOnlineAllowed = firewall.isOnlineLearningEnabled.value
        )
    }

    private fun looksLikeTopicInquiry(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("what is") ||
                lower.contains("what are") ||
                lower.contains("who is") ||
                lower.contains("who was") ||
                lower.contains("tell me about") ||
                lower.contains("explain") ||
                lower.contains("how does") ||
                lower.contains("how do") ||
                lower.contains("learn about") ||
                lower.contains("search about") ||
                lower.contains("research") ||
                lower.contains("curious about") ||
                lower.contains("heard about") ||
                lower.endsWith("?")
    }
}
