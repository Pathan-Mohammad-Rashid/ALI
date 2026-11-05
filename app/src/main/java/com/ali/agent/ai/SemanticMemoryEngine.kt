package com.ali.agent.ai

import com.ali.agent.data.db.AliDao
import com.ali.agent.data.db.KnowledgeNodeEntity
import com.ali.agent.data.db.UserMemoryEntity
import kotlin.math.sqrt

data class MemoryExtractionOutcome(
    val extractedUserFacts: List<UserMemoryEntity>,
    val extractedKnowledgeNodes: List<KnowledgeNodeEntity>
)

class SemanticMemoryEngine(private val dao: AliDao) {

    /**
     * Listens to every user message and organically learns personal facts,
     * preferences, habits, goals, and explicit teachings—storing them 100% locally.
     */
    suspend fun extractAndStoreMemoriesFromInput(userText: String): MemoryExtractionOutcome {
        val text = userText.trim()
        val newFacts = mutableListOf<UserMemoryEntity>()
        val newKnowledge = mutableListOf<KnowledgeNodeEntity>()

        // 1. Name extraction
        val nameRegex = Regex("(?i)\\b(?:my\\s+name\\s+is|call\\s+me|i\\s+am\\s+named)\\s+([A-Z][a-zA-Z]{1,22})")
        nameRegex.find(text)?.let { match ->
            val name = match.groupValues[1].trim()
            if (name.lowercase() !in setOf("feeling", "happy", "sad", "tired", "going", "working", "here")) {
                val mem = UserMemoryEntity(
                    category = "Identity",
                    factKey = "user_name",
                    factValue = name
                )
                dao.upsertUserMemory(mem)
                newFacts.add(mem)
            }
        }

        // 2. Location / Study / Work extraction
        val workRegex = Regex("(?i)\\b(?:i\\s+(?:study|work|live)\\s+(?:at|in))\\s+([^.,!?]{3,45})")
        workRegex.find(text)?.let { match ->
            val place = match.groupValues[1].trim()
            val mem = UserMemoryEntity(
                category = "LifeContext",
                factKey = "place_or_institution",
                factValue = place
            )
            dao.upsertUserMemory(mem)
            newFacts.add(mem)
        }

        // 3. Preferences (likes / loves / favorite)
        val likeRegex = Regex("(?i)\\b(?:i\\s+(?:really\\s+)?(?:love|like|enjoy|prefer)|my\\s+favorite\\s+[a-z]+\\s+is)\\s+([^.,!?]{3,55})")
        likeRegex.find(text)?.let { match ->
            val pref = match.groupValues[1].trim()
            val key = "pref_" + pref.lowercase().replace(Regex("[^a-z0-9]"), "_").take(24)
            val mem = UserMemoryEntity(
                category = "Preference",
                factKey = key,
                factValue = "Loves/prefers $pref"
            )
            dao.upsertUserMemory(mem)
            newFacts.add(mem)
        }

        // 4. Goals & Ambitions
        val goalRegex = Regex("(?i)\\b(?:my\\s+goal\\s+is\\s+to|i\\s+want\\s+to\\s+achieve|i'm\\s+working\\s+towards|i\\s+plan\\s+to)\\s+([^.,!?]{4,65})")
        goalRegex.find(text)?.let { match ->
            val goal = match.groupValues[1].trim()
            val key = "goal_" + goal.lowercase().replace(Regex("[^a-z0-9]"), "_").take(24)
            val mem = UserMemoryEntity(
                category = "Goal",
                factKey = key,
                factValue = goal
            )
            dao.upsertUserMemory(mem)
            newFacts.add(mem)
        }

        // 5. Explicit "Remember that..." or "Keep in mind..."
        val rememberRegex = Regex("(?i)(?:remember\\s+that|keep\\s+in\\s+mind\\s+that|don't\\s+forget\\s+that|note\\s+that)\\s+([^.!?]{4,120})")
        rememberRegex.find(text)?.let { match ->
            val fact = match.groupValues[1].trim()
            val key = "remember_" + fact.lowercase().replace(Regex("[^a-z0-9]"), "_").take(28)
            val mem = UserMemoryEntity(
                category = "Memory",
                factKey = key,
                factValue = fact
            )
            dao.upsertUserMemory(mem)
            newFacts.add(mem)
        }

        // 6. Direct knowledge teaching ("X means Y", "Teach you: X is Y", "Fact: X is Y")
        val teachRegex = Regex("(?i)(?:teach\\s+you:?|learn\\s+this:?|fact:?|did\\s+you\\s+know\\s+that)\\s*([a-zA-Z0-9\\s\\-]{2,40})\\s+(?:is|means|refers\\s+to|are)\\s+([^.!?]{6,180})")
        teachRegex.find(text)?.let { match ->
            val topic = match.groupValues[1].trim()
            val explanation = match.groupValues[2].trim()
            val node = KnowledgeNodeEntity(
                topic = topic,
                summary = "$topic is $explanation.",
                keywords = topic.lowercase(),
                sourceType = "USER_TAUGHT_OFFLINE"
            )
            dao.upsertKnowledgeNode(node)
            newKnowledge.add(node)
        }

        return MemoryExtractionOutcome(newFacts, newKnowledge)
    }

    /**
     * Computes semantic similarity using TF-weighted cosine similarity over character n-grams & tokens
     * to retrieve the most relevant knowledge nodes completely offline.
     */
    suspend fun retrieveRelevantKnowledge(query: String, topK: Int = 3): List<KnowledgeNodeEntity> {
        val allNodes = dao.getAllKnowledgeNodes()
        if (allNodes.isEmpty()) return emptyList()

        val queryVec = textToVector(query)
        if (queryVec.isEmpty()) return emptyList()

        return allNodes
            .map { node ->
                val docText = "${node.topic} ${node.topic} ${node.keywords} ${node.summary}"
                val score = cosineSimilarity(queryVec, textToVector(docText))
                node to score
            }
            .filter { it.second > 0.14 }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    private fun textToVector(text: String): Map<String, Double> {
        val tokens = text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in STOPWORDS }

        val counts = mutableMapOf<String, Double>()
        for (token in tokens) {
            counts[token] = (counts[token] ?: 0.0) + 1.0
            if (token.length >= 4) {
                val prefix = token.take(4)
                counts[prefix] = (counts[prefix] ?: 0.0) + 0.5
            }
        }
        return counts
    }

    private fun cosineSimilarity(a: Map<String, Double>, b: Map<String, Double>): Double {
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for ((k, v) in a) {
            normA += v * v
            val bv = b[k]
            if (bv != null) {
                dot += v * bv
            }
        }
        for (v in b.values) {
            normB += v * v
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0.0) 0.0 else dot / denom
    }

    companion object {
        private val STOPWORDS = setOf(
            "the", "and", "for", "that", "this", "with", "you", "are", "was", "were",
            "have", "has", "had", "what", "when", "where", "who", "why", "how", "can",
            "could", "would", "should", "tell", "about", "from", "into", "your"
        )
    }
}
