package com.ali.agent.privacy

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

data class SanitizationResult(
    val originalInput: String,
    val anonymousTopic: String,
    val strippedElements: List<String>,
    val isSafeToQuery: Boolean
)

class PrivacyFirewall(context: Context) {

    private val prefs = context.getSharedPreferences("ali_privacy_prefs", Context.MODE_PRIVATE)

    private val _isOnlineLearningEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_ONLINE_LEARNING, false)
    )
    val isOnlineLearningEnabled: StateFlow<Boolean> = _isOnlineLearningEnabled.asStateFlow()

    // Ephemeral, cookie-less HTTP client that only connects to public encyclopedic sources
    val anonymousHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(CookieJar.NO_COOKIES)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request()
            val host = request.url.host.lowercase()
            val isAllowedHost = ALLOWED_PUBLIC_HOSTS.any { host == it || host.endsWith(".$it") }
            if (!isAllowedHost) {
                throw SecurityException("ALI Privacy Firewall blocked unauthorized host: $host")
            }
            val cleanRequest = request.newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", "Mozilla/5.0 (compatible; PublicEncyclopediaReader/1.0)")
                .removeHeader("Cookie")
                .removeHeader("Referer")
                .build()
            chain.proceed(cleanRequest)
        }
        .build()

    fun setOnlineLearningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ONLINE_LEARNING, enabled).apply()
        _isOnlineLearningEnabled.value = enabled
    }

    /**
     * Strips all personal identifiers, conversational context, first-person pronouns,
     * phone numbers, emails, and private context, leaving ONLY an anonymous encyclopedic topic.
     */
    fun sanitizeToAnonymousTopic(rawInput: String): SanitizationResult {
        val stripped = mutableListOf<String>()
        var working = rawInput.trim()

        // 1. Strip emails
        val emailRegex = Regex("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}")
        emailRegex.findAll(working).forEach {
            stripped.add("Email(${it.value})")
        }
        working = emailRegex.replace(working, " ")

        // 2. Strip phone numbers & long numeric IDs
        val phoneRegex = Regex("\\b(?:\\+?\\d{1,3}[ -]?)?\\(?\\d{3,5}\\)?[ -]?\\d{3,5}[ -]?\\d{3,5}\\b")
        phoneRegex.findAll(working).forEach {
            stripped.add("Number(${it.value})")
        }
        working = phoneRegex.replace(working, " ")

        // 3. Strip personal clauses ("my friend X told me", "I work at X", "my name is X")
        val personalClauseRegexes = listOf(
            Regex("(?i)\\b(my\\s+(friend|brother|sister|mom|mother|dad|father|boss|wife|husband|partner|doctor|teacher|colleague|son|daughter)\\s+[a-zA-Z]+)"),
            Regex("(?i)\\b(my\\s+name\\s+is\\s+[a-zA-Z]+)"),
            Regex("(?i)\\b(i\\s+live\\s+in\\s+[a-zA-Z\\s]+)"),
            Regex("(?i)\\b(i\\s+am\\s+feeling\\s+[a-zA-Z]+)")
        )
        for (regex in personalClauseRegexes) {
            regex.findAll(working).forEach {
                stripped.add("PersonalContext(\"${it.value.trim()}\")")
            }
            working = regex.replace(working, " ")
        }

        // 4. Extract target topic from natural inquiry patterns
        val topicExtractionPatterns = listOf(
            Regex("(?i)(?:what\\s+is|what\\s+are|who\\s+is|who\\s+was|tell\\s+me\\s+about|explain|learn\\s+about|search\\s+about|research|do\\s+you\\s+know\\s+about|heard\\s+about|curious\\s+about|meaning\\s+of|how\\s+does|how\\s+do)\\s+([a-zA-Z0-9\\s\\-\\.\\+]{2,45}?)(?:\\s+work|\\s+mean|[\\?\\.!,]|$)"),
            Regex("(?i)(?:about|regarding|topic\\s+of)\\s+([a-zA-Z0-9\\s\\-\\.\\+]{2,40}?)(?:[\\?\\.!,]|$)")
        )

        var extractedCandidate: String? = null
        for (pattern in topicExtractionPatterns) {
            val match = pattern.find(working)
            if (match != null && match.groupValues.size > 1) {
                extractedCandidate = match.groupValues[1].trim()
                break
            }
        }

        val candidate = extractedCandidate ?: working

        // 5. Remove any remaining conversational filler words or first-person words
        val tokens = candidate
            .replace(Regex("[^a-zA-Z0-9\\s\\-\\+]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val cleanTokens = mutableListOf<String>()
        for (token in tokens) {
            val lower = token.lowercase()
            if (lower in PERSONAL_AND_FILLER_STOPWORDS) {
                if (lower in FIRST_PERSON_PRONOUNS) {
                    stripped.add("Pronoun($token)")
                }
            } else {
                cleanTokens.add(token)
            }
        }

        val finalTopic = cleanTokens.take(5).joinToString(" ").trim()
        val isSafe = finalTopic.length >= 2 &&
                !finalTopic.contains("@") &&
                cleanTokens.isNotEmpty()

        return SanitizationResult(
            originalInput = rawInput,
            anonymousTopic = finalTopic,
            strippedElements = stripped.distinct(),
            isSafeToQuery = isSafe
        )
    }

    companion object {
        private const val KEY_ONLINE_LEARNING = "online_learning_enabled"

        val ALLOWED_PUBLIC_HOSTS = setOf(
            "en.wikipedia.org",
            "api.duckduckgo.com",
            "huggingface.co",
            "cdn-lfs.huggingface.co",
            "cdn-lfs-us-1.huggingface.co"
        )

        private val FIRST_PERSON_PRONOUNS = setOf(
            "i", "me", "my", "mine", "myself", "we", "us", "our", "ours"
        )

        private val PERSONAL_AND_FILLER_STOPWORDS = FIRST_PERSON_PRONOUNS + setOf(
            "hey", "hi", "hello", "ali", "please", "can", "could", "would", "you", "your",
            "tell", "explain", "what", "who", "where", "when", "why", "how", "is", "are",
            "was", "were", "the", "a", "an", "about", "today", "yesterday", "said", "told",
            "asked", "think", "know", "learn", "search", "find", "look", "up", "for", "to"
        )
    }
}
