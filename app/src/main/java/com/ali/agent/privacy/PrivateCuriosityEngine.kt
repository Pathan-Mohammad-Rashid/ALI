package com.ali.agent.privacy

import com.ali.agent.data.db.AliDao
import com.ali.agent.data.db.CuriosityTopicEntity
import com.ali.agent.data.db.KnowledgeNodeEntity
import com.ali.agent.data.db.PrivacyAuditEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

data class LearnedTopicResult(
    val learned: Boolean,
    val topic: String,
    val summary: String,
    val sourceLabel: String,
    val queuedForLater: Boolean = false
)

class PrivateCuriosityEngine(
    private val dao: AliDao,
    private val firewall: PrivacyFirewall
) {

    /**
     * Attempts to learn about a topic mentioned by the user.
     * - If Offline Mode is active, queues the sanitized topic in the Curiosity Queue.
     * - If Online Learning is allowed, strips all personal context via PrivacyFirewall,
     *   fetches anonymous public knowledge, logs the Privacy Audit entry, and saves to local DB.
     */
    suspend fun learnFromUserMention(rawUserMessage: String): LearnedTopicResult = withContext(Dispatchers.IO) {
        val sanitization = firewall.sanitizeToAnonymousTopic(rawUserMessage)
        if (!sanitization.isSafeToQuery) {
            return@withContext LearnedTopicResult(
                learned = false,
                topic = "",
                summary = "",
                sourceLabel = "None"
            )
        }

        val topic = sanitization.anonymousTopic
        val strippedSummary = if (sanitization.strippedElements.isEmpty()) {
            "Conversational framing removed"
        } else {
            sanitization.strippedElements.joinToString(", ")
        }

        // Check if we already have this topic in local knowledge graph
        val existing = dao.searchKnowledgeNodes(topic)
        if (existing.isNotEmpty() && existing.first().topic.equals(topic, ignoreCase = true)) {
            val top = existing.first()
            return@withContext LearnedTopicResult(
                learned = true,
                topic = top.topic,
                summary = top.summary,
                sourceLabel = "Local Memory (${top.sourceType})"
            )
        }

        // Check Privacy Firewall state
        if (!firewall.isOnlineLearningEnabled.value) {
            dao.enqueueCuriosityTopic(
                CuriosityTopicEntity(
                    rawTopic = topic,
                    sanitizedQuery = topic,
                    status = "PENDING_INTERNET"
                )
            )
            dao.insertPrivacyAuditLog(
                PrivacyAuditEntity(
                    eventType = "BLOCKED_OFFLINE",
                    strippedPiiSummary = strippedSummary,
                    outboundAnonymousQuery = "NONE (Queued '$topic' for later)",
                    destinationHost = "OFFLINE_AIRGAP"
                )
            )
            return@withContext LearnedTopicResult(
                learned = false,
                topic = topic,
                summary = "",
                sourceLabel = "Queued in Offline Curiosity Queue",
                queuedForLater = true
            )
        }

        // Online Learning is permitted -> Execute anonymous query
        fetchAndStoreAnonymousKnowledge(topic, strippedSummary)
    }

    suspend fun syncPendingCuriosityQueue(): Int = withContext(Dispatchers.IO) {
        if (!firewall.isOnlineLearningEnabled.value) return@withContext 0
        val pending = dao.getPendingCuriosityTopics()
        var learnedCount = 0
        for (item in pending) {
            val res = fetchAndStoreAnonymousKnowledge(
                anonymousTopic = item.sanitizedQuery,
                strippedSummary = "Background Curiosity Queue Sync (Zero Personal Context)"
            )
            if (res.learned) {
                dao.markCuriosityTopicLearned(item.id, System.currentTimeMillis())
                learnedCount++
            }
        }
        learnedCount
    }

    private suspend fun fetchAndStoreAnonymousKnowledge(
        anonymousTopic: String,
        strippedSummary: String
    ): LearnedTopicResult {
        // 1. Try Wikipedia OpenSearch + Summary REST API
        try {
            val wikiResult = queryWikipediaAnonymously(anonymousTopic)
            if (wikiResult != null) {
                val (canonicalTitle, extract, url) = wikiResult
                dao.insertPrivacyAuditLog(
                    PrivacyAuditEntity(
                        eventType = "SANITIZED_QUERY_SENT",
                        strippedPiiSummary = strippedSummary,
                        outboundAnonymousQuery = anonymousTopic,
                        destinationHost = "en.wikipedia.org"
                    )
                )
                dao.upsertKnowledgeNode(
                    KnowledgeNodeEntity(
                        topic = canonicalTitle,
                        summary = extract,
                        keywords = "$anonymousTopic, ${canonicalTitle.lowercase()}",
                        sourceType = "PRIVATE_ONLINE_WIKI",
                        referenceUrl = url
                    )
                )
                return LearnedTopicResult(
                    learned = true,
                    topic = canonicalTitle,
                    summary = extract,
                    sourceLabel = "Anonymous Wikipedia Synthesis"
                )
            }
        } catch (_: Exception) {
            // Fall through to DuckDuckGo Instant Answer
        }

        // 2. Fallback to DuckDuckGo Instant Answer JSON API
        try {
            val ddgResult = queryDuckDuckGoAnonymously(anonymousTopic)
            if (ddgResult != null) {
                val (heading, abstractText, url) = ddgResult
                dao.insertPrivacyAuditLog(
                    PrivacyAuditEntity(
                        eventType = "SANITIZED_QUERY_SENT",
                        strippedPiiSummary = strippedSummary,
                        outboundAnonymousQuery = anonymousTopic,
                        destinationHost = "api.duckduckgo.com"
                    )
                )
                dao.upsertKnowledgeNode(
                    KnowledgeNodeEntity(
                        topic = heading,
                        summary = abstractText,
                        keywords = anonymousTopic.lowercase(),
                        sourceType = "PRIVATE_ONLINE_DDG",
                        referenceUrl = url
                    )
                )
                return LearnedTopicResult(
                    learned = true,
                    topic = heading,
                    summary = abstractText,
                    sourceLabel = "Anonymous DuckDuckGo Instant Graph"
                )
            }
        } catch (_: Exception) {
            // Ignore network failures gracefully
        }

        return LearnedTopicResult(
            learned = false,
            topic = anonymousTopic,
            summary = "",
            sourceLabel = "No public entry found"
        )
    }

    private fun queryWikipediaAnonymously(topic: String): Triple<String, String, String>? {
        val encoded = URLEncoder.encode(topic, "UTF-8")
        val searchUrl = "https://en.wikipedia.org/w/api.php?action=opensearch&search=$encoded&limit=1&namespace=0&format=json"
        val searchReq = Request.Builder().url(searchUrl).get().build()

        var resolvedTitle = topic
        firewall.anonymousHttpClient.newCall(searchReq).execute().use { resp ->
            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string().orEmpty()
                val arr = JSONArray(bodyStr)
                if (arr.length() >= 2) {
                    val titles = arr.getJSONArray(1)
                    if (titles.length() > 0) {
                        resolvedTitle = titles.getString(0)
                    }
                }
            }
        }

        val slug = URLEncoder.encode(resolvedTitle.replace(" ", "_"), "UTF-8")
        val summaryUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/$slug"
        val summaryReq = Request.Builder().url(summaryUrl).get().build()

        firewall.anonymousHttpClient.newCall(summaryReq).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val json = JSONObject(resp.body?.string().orEmpty())
            val extract = json.optString("extract", "").trim()
            val title = json.optString("title", resolvedTitle).trim()
            val pageUrl = json.optJSONObject("content_urls")
                ?.optJSONObject("desktop")
                ?.optString("page", "")
                .orEmpty()
            if (extract.length > 35 && !extract.contains("may refer to:")) {
                return Triple(title, extract, pageUrl)
            }
        }
        return null
    }

    private fun queryDuckDuckGoAnonymously(topic: String): Triple<String, String, String>? {
        val encoded = URLEncoder.encode(topic, "UTF-8")
        val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
        val req = Request.Builder().url(url).get().build()
        firewall.anonymousHttpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val json = JSONObject(resp.body?.string().orEmpty())
            val abstractText = json.optString("AbstractText", "").trim()
            val heading = json.optString("Heading", topic).ifBlank { topic }
            val absUrl = json.optString("AbstractURL", "")
            if (abstractText.length > 30) {
                return Triple(heading, abstractText, absUrl)
            }
            // Check RelatedTopics first item if AbstractText is empty
            val related = json.optJSONArray("RelatedTopics")
            if (related != null && related.length() > 0) {
                val first = related.optJSONObject(0)
                val text = first?.optString("Text", "")?.trim().orEmpty()
                if (text.length > 30) {
                    return Triple(heading, text, first?.optString("FirstURL", "").orEmpty())
                }
            }
        }
        return null
    }
}
