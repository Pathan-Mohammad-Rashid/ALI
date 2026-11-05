package com.ali.agent.ai

import com.ali.agent.data.db.KnowledgeNodeEntity
import com.ali.agent.data.db.MobileTaskEntity
import com.ali.agent.data.db.QuickNoteEntity
import com.ali.agent.data.db.UserMemoryEntity
import com.ali.agent.privacy.LearnedTopicResult
import java.util.Locale

data class CognitiveResponse(
    val replyText: String,
    val innerThought: String,
    val emotionBadge: String,
    val learnedSourceBadge: String? = null,
    val actionBadge: String? = null
)

class AutonomousCognitiveEngine {

    fun synthesizeHumanResponse(
        userInput: String,
        emotion: UserEmotionalState,
        memories: List<UserMemoryEntity>,
        newlyExtractedMemories: List<UserMemoryEntity>,
        newlyTaughtNodes: List<KnowledgeNodeEntity>,
        retrievedOfflineKnowledge: List<KnowledgeNodeEntity>,
        onlineCuriosityResult: LearnedTopicResult?,
        mathResult: String?,
        pendingTasks: List<MobileTaskEntity>,
        notes: List<QuickNoteEntity>,
        isOnlineAllowed: Boolean
    ): CognitiveResponse {
        val lower = userInput.lowercase(Locale.US).trim()
        val userName = memories.firstOrNull { it.factKey == "user_name" }?.factValue
        val nameGreeting = if (userName != null) ", $userName" else ""
        val (timeLabel, timeMood) = HumanPersonaEngine.getTimeOfDayGreetingContext()

        val thoughts = mutableListOf<String>()
        thoughts.add("Perceived user tone: ${emotion.displayBadge} ($timeMood)")

        if (userName != null) {
            thoughts.add("Recalled user identity: $userName (${memories.size} total personal memory nodes)")
        }

        // 1. If user just taught ALI a new concept offline
        if (newlyTaughtNodes.isNotEmpty()) {
            val taught = newlyTaughtNodes.first()
            thoughts.add("Encoded new offline knowledge node: '${taught.topic}'")
            return CognitiveResponse(
                replyText = "I love learning things directly from you$nameGreeting. I've permanently woven **${taught.topic}** into my local knowledge graph: *\"${taught.summary}\"* — and since it lives right here on your phone, I'll remember it anytime we talk, even in airplane mode.",
                innerThought = thoughts.joinToString(" → "),
                emotionBadge = "Curious & Grateful",
                learnedSourceBadge = "Learned Offline From You"
            )
        }

        // 2. If user shared personal facts (name, goal, preference, remember that...)
        if (newlyExtractedMemories.isNotEmpty() && !isQuestion(lower)) {
            val factsDesc = newlyExtractedMemories.joinToString("; ") { "${it.category}: ${it.factValue}" }
            thoughts.add("Consolidated personal episodic memory: $factsDesc")
            val firstFact = newlyExtractedMemories.first()
            val warmReply = when (firstFact.category) {
                "Identity" -> "Really glad to know you, **${firstFact.factValue}**! I've kept your name in my private on-device memory. Everything we talk about stays strictly between us on your phone. How's your $timeLabel going so far?"
                "Goal" -> "That's a meaningful goal$nameGreeting — **${firstFact.factValue}**. I've anchored that in my memory so we can break it down into daily steps whenever you want. What's the very next small move you want to make on it?"
                "Preference" -> "Noted and remembered$nameGreeting! Knowing that you ${firstFact.factValue.lowercase()} helps me understand your world better."
                else -> "Got it$nameGreeting — I've locked that into my private memory: **\"${firstFact.factValue}\"**. You don't have to worry about forgetting it; I'll hold onto it for you."
            }
            return CognitiveResponse(
                replyText = warmReply,
                innerThought = thoughts.joinToString(" → "),
                emotionBadge = "Warm & Attentive",
                learnedSourceBadge = "Saved to Personal Memory"
            )
        }

        // 3. Math & unit conversions
        if (mathResult != null) {
            thoughts.add("Executed deterministic math & unit logic engine")
            return CognitiveResponse(
                replyText = "$mathResult Need me to save that number to a quick note or use it for anything else$nameGreeting?",
                innerThought = thoughts.joinToString(" → "),
                emotionBadge = "Sharp & Attentive",
                learnedSourceBadge = "Offline Logic Core"
            )
        }

        // 4. Check if user is asking for a daily briefing / tasks / notes overview
        if (lower.contains("my day") || lower.contains("briefing") || lower.contains("my tasks") || lower.contains("what do i have")) {
            thoughts.add("Synthesizing daily briefing from ${pendingTasks.size} pending tasks and ${notes.size} notes")
            val taskSummary = if (pendingTasks.isEmpty()) {
                "Your task list is completely clear right now—wide open canvas!"
            } else {
                "Here's what's on your radar:\n" + pendingTasks.take(5).joinToString("\n") {
                    "• **${it.title}** (${it.dueTimeText})"
                }
            }
            val goalHint = memories.firstOrNull { it.category == "Goal" }?.let {
                "\n\nAnd keeping your bigger goal in mind (*${it.factValue}*), even 20 focused minutes on it today will feel great."
            }.orEmpty()

            return CognitiveResponse(
                replyText = "Good $timeLabel$nameGreeting! Let's look at where things stand.\n\n$taskSummary$goalHint\n\nWant me to set an alarm or add a new reminder for anything?",
                innerThought = thoughts.joinToString(" → "),
                emotionBadge = "Focused & Supportive",
                learnedSourceBadge = "On-Device Day Hub"
            )
        }

        // 5. Check if user is asking what ALI remembers about them
        if (lower.contains("what do you know about me") || lower.contains("what do you remember") || lower.contains("who am i")) {
            thoughts.add("Querying local SQLite personal memory graph (${memories.size} nodes)")
            if (memories.isEmpty()) {
                return CognitiveResponse(
                    replyText = "We're still getting to know each other! Tell me your name, what you're working on, what you love doing, or say *\"Remember that...\"*—and I'll keep it 100% privately in my on-device memory.",
                    innerThought = thoughts.joinToString(" → "),
                    emotionBadge = "Warm & Curious"
                )
            }
            val listText = memories.take(8).joinToString("\n") { "• **${it.category}**: ${it.factValue}" }
            return CognitiveResponse(
                replyText = "Here's what I hold in my private on-device memory about you$nameGreeting:\n\n$listText\n\nNone of this ever leaves your phone. You can also view or edit every single memory node in the **Brain** tab anytime.",
                innerThought = thoughts.joinToString(" → "),
                emotionBadge = "Warm & Transparent",
                learnedSourceBadge = "100% Local Memory Graph"
            )
        }

        // 6. If Online Curiosity Engine just learned something new (or found it in local graph)
        if (onlineCuriosityResult != null && onlineCuriosityResult.learned) {
            thoughts.add("Privacy Sanitizer stripped personal context -> Retrieved knowledge on '${onlineCuriosityResult.topic}' via ${onlineCuriosityResult.sourceLabel}")
            val conversationalIntro = when (emotion) {
                UserEmotionalState.CURIOUS_DEEP -> "I love that you brought up **${onlineCuriosityResult.topic}**$nameGreeting. Here's how I think about it:"
                else -> "Here's the fascinating part about **${onlineCuriosityResult.topic}**$nameGreeting:"
            }
            return CognitiveResponse(
                replyText = "$conversationalIntro\n\n${onlineCuriosityResult.summary}\n\nI've also indexed **${onlineCuriosityResult.topic}** into my permanent offline knowledge base, so we have it right here on your phone whenever you need it.",
                innerThought = thoughts.joinToString(" → "),
                emotionBadge = emotion.displayBadge,
                learnedSourceBadge = onlineCuriosityResult.sourceLabel
            )
        }

        // 7. If Offline Semantic Memory matched a relevant knowledge node
        if (retrievedOfflineKnowledge.isNotEmpty()) {
            val topNode = retrievedOfflineKnowledge.first()
            thoughts.add("Matched local semantic knowledge node '${topNode.topic}' (${topNode.sourceType})")
            return CognitiveResponse(
                replyText = "Drawing from my local memory on **${topNode.topic}**$nameGreeting:\n\n${topNode.summary}\n\nHow does that connect with what you're thinking about or working on right now?",
                innerThought = thoughts.joinToString(" → "),
                emotionBadge = emotion.displayBadge,
                learnedSourceBadge = "Offline Brain (${topNode.sourceType})"
            )
        }

        // 8. If Online Curiosity Engine wanted to research a topic, but Offline Firewall blocked it
        if (onlineCuriosityResult != null && onlineCuriosityResult.queuedForLater && isQuestion(lower)) {
            thoughts.add("Offline Firewall active -> Queued sanitized topic '${onlineCuriosityResult.topic}' in Curiosity Queue")
            return CognitiveResponse(
                replyText = "That's a great question about **${onlineCuriosityResult.topic}**$nameGreeting! Right now you have me in **Strict Offline Mode**, so I won't touch the internet at all. \n\nHere's what I did: I stripped out all personal phrasing and added the pure keyword **\"${onlineCuriosityResult.topic}\"** to my **Curiosity Queue**. The moment you toggle **Online Learning ON** (or teach me about it directly by saying *\"Teach you: ${onlineCuriosityResult.topic} is...\"*), I'll absorb it into my permanent offline memory!",
                innerThought = thoughts.joinToString(" → "),
                emotionBadge = "Curious & Privacy-First",
                learnedSourceBadge = "Queued for Private Learning"
            )
        }

        // 9. Emotional & Human Conversational Resonance
        thoughts.add("Generating empathetic human dialogue response for state: ${emotion.name}")
        val reply = when (emotion) {
            UserEmotionalState.STRESSED_OVERWHELMED -> {
                val taskHint = if (pendingTasks.isNotEmpty()) {
                    "You have ${pendingTasks.size} items on your task list, like *${pendingTasks.first().title}*, and we can easily push everything else aside and just pick the single easiest 5-minute step."
                } else {
                    "We can dump whatever is swirling around in your head into a quick list so your brain doesn't have to carry it all at once."
                }
                "Take a slow breath with me for a second$nameGreeting. Seriously—drop your shoulders a little. Carrying too much at once makes everything feel heavier than it really is. $taskHint Want to talk through what's weighing on you most right now?"
            }

            UserEmotionalState.SAD_REFLECTIVE -> {
                "I'm really glad you told me$nameGreeting. Some days just hit harder, and you don't have to put on a brave face or rush past how you feel. I'm right here with you—no judgment, no rush. Do you want to vent about what happened, or would you rather we talk about something comforting to take your mind off it for a while?"
            }

            UserEmotionalState.EXCITED_ENERGIZED -> {
                "Now THAT is the kind of energy I love hearing$nameGreeting! Tell me everything—what made it click, and how are we celebrating? Should I save a quick note about this win so you can look back on it later?"
            }

            UserEmotionalState.PLAYFUL_WITTY -> {
                "Haha, I'm always ready for a good tangent$nameGreeting! You know the best part about living inside your phone with zero cloud servers watching me? I can actually have a sense of humor without a corporate filter breathing down my neck. Hit me with your best thought or challenge!"
            }

            else -> {
                if (lower.contains("who are you") || lower.contains("what are you")) {
                    "I'm **ALI — your Advanced Learning Interface**$nameGreeting. Think of me less like an app and more like a thinking companion who lives strictly on your phone. I remember what matters to you, help you run your day (alarms, tasks, notes, calls, math), and when you flip on my Curiosity switch, I anonymously learn about topics you mention online without ever sharing a single byte of your personal life."
                } else if (lower.contains("hello") || lower.contains("hey") || lower.contains("hi") || lower.contains("good morning") || lower.contains("good evening")) {
                    val memTouch = memories.firstOrNull { it.category == "Goal" || it.category == "Preference" }?.let {
                        " I was just keeping your note about *${it.factValue}* in mind."
                    }.orEmpty()
                    "Hey$nameGreeting! Good $timeLabel.$memTouch How are you feeling right now, and what should we dive into?"
                } else {
                    "I hear you$nameGreeting, and I'm thinking about what you just said: *\"${userInput.take(80)}\"*. Let's unpack it together—are you looking to brainstorm ideas around this, turn it into a concrete action plan for your day, or explore the deeper concepts behind it?"
                }
            }
        }

        return CognitiveResponse(
            replyText = reply,
            innerThought = thoughts.joinToString(" → "),
            emotionBadge = emotion.displayBadge,
            learnedSourceBadge = if (isOnlineAllowed) "Hybrid Brain (Online Curiosity Ready)" else "100% Air-Gapped Offline Brain"
        )
    }

    private fun isQuestion(lower: String): Boolean {
        return lower.endsWith("?") ||
                lower.startsWith("what ") ||
                lower.startsWith("who ") ||
                lower.startsWith("why ") ||
                lower.startsWith("how ") ||
                lower.startsWith("explain ") ||
                lower.startsWith("tell me about ") ||
                lower.contains("know about ")
    }
}
