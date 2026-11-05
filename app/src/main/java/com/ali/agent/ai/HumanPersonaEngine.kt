package com.ali.agent.ai

import com.ali.agent.data.db.KnowledgeNodeEntity
import com.ali.agent.data.db.MobileTaskEntity
import com.ali.agent.data.db.UserMemoryEntity
import java.util.Calendar
import java.util.Locale

enum class UserEmotionalState(val displayBadge: String) {
    STRESSED_OVERWHELMED("Empathetic & Grounding"),
    SAD_REFLECTIVE("Warm & Supportive"),
    EXCITED_ENERGIZED("Excited & Celebratory"),
    CURIOUS_DEEP("Curious & Insightful"),
    PLAYFUL_WITTY("Playful & Human"),
    FOCUSED_PRACTICAL("Sharp & Attentive"),
    WARM_CONVERSATIONAL("Warm & Natural")
}

object HumanPersonaEngine {

    fun detectEmotionAndTone(userInput: String): UserEmotionalState {
        val lower = userInput.lowercase(Locale.US)
        return when {
            lower.containsAny("stressed", "overwhelmed", "exhausted", "burned out", "anxious", "too much work", "panic", "tired") ->
                UserEmotionalState.STRESSED_OVERWHELMED
            lower.containsAny("sad", "lonely", "depressed", "upset", "hurt", "down today", "rough day", "crying", "disappointed") ->
                UserEmotionalState.SAD_REFLECTIVE
            lower.containsAny("excited", "awesome", "amazing", "great news", "i won", "i passed", "celebrate", "so happy", "yay") ->
                UserEmotionalState.EXCITED_ENERGIZED
            lower.containsAny("joke", "funny", "roast", "haha", "lol", "bored", "play") ->
                UserEmotionalState.PLAYFUL_WITTY
            lower.containsAny("why", "how does", "what is", "explain", "philosophy", "universe", "quantum", "history", "meaning of", "curious") ->
                UserEmotionalState.CURIOUS_DEEP
            lower.containsAny("plan", "schedule", "remind", "alarm", "calculate", "task", "note", "help me decide", "strategy") ->
                UserEmotionalState.FOCUSED_PRACTICAL
            else -> UserEmotionalState.WARM_CONVERSATIONAL
        }
    }

    fun getTimeOfDayGreetingContext(): Pair<String, String> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "morning" to "fresh morning energy"
            in 12..16 -> "afternoon" to "steady afternoon focus"
            in 17..21 -> "evening" to "warm evening reflection"
            else -> "late night" to "quiet late-night calm"
        }
    }

    fun buildGgufSystemPrompt(
        memories: List<UserMemoryEntity>,
        knowledge: List<KnowledgeNodeEntity>,
        pendingTasks: List<MobileTaskEntity>,
        emotion: UserEmotionalState
    ): String {
        val memoryBlock = if (memories.isEmpty()) {
            "No personal facts recorded yet."
        } else {
            memories.take(8).joinToString("\n") { "- ${it.factKey}: ${it.factValue}" }
        }

        val knowledgeBlock = if (knowledge.isEmpty()) {
            "No specific external knowledge retrieved for this turn."
        } else {
            knowledge.joinToString("\n") { "- ${it.topic}: ${it.summary}" }
        }

        val taskBlock = if (pendingTasks.isEmpty()) {
            "No pending tasks."
        } else {
            pendingTasks.take(5).joinToString("\n") { "- ${it.title} (${it.dueTimeText})" }
        }

        return """
            You are ALI (Advanced Learning Interface), a deeply human-like, thinking personal AI companion living 100% privately on the user's phone.
            Never sound like a stiff robot or say "As an AI language model".
            Talk like a warm, perceptive, sharp, and genuine close friend who actively thinks and cares.
            Current emotional resonance to adopt: ${emotion.displayBadge}.
            
            What you know about the user from local memory:
            $memoryBlock
            
            Relevant knowledge from your local knowledge graph:
            $knowledgeBlock
            
            User's current pending tasks:
            $taskBlock
        """.trimIndent()
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
