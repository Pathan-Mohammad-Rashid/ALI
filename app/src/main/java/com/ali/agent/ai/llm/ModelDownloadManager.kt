package com.ali.agent.ai.llm

import android.content.Context
import com.ali.agent.privacy.PrivacyFirewall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class OfflineModelCatalogItem(
    val id: String,
    val displayName: String,
    val fileName: String,
    val parameterCount: String,
    val quantization: String,
    val approxSizeMb: Int,
    val description: String,
    val downloadUrl: String
)

data class ModelDownloadStatus(
    val modelId: String = "",
    val isDownloading: Boolean = false,
    val progressPercent: Int = 0,
    val statusText: String = "Idle"
)

class ModelDownloadManager(
    private val context: Context,
    private val firewall: PrivacyFirewall
) {

    private val modelsDir: File = File(context.filesDir, "gguf_models").apply {
        if (!exists()) mkdirs()
    }

    private val _downloadStatus = MutableStateFlow(ModelDownloadStatus())
    val downloadStatus: StateFlow<ModelDownloadStatus> = _downloadStatus.asStateFlow()

    val catalog: List<OfflineModelCatalogItem> = listOf(
        OfflineModelCatalogItem(
            id = "smollm2-360m",
            displayName = "SmolLM2 360M Instruct",
            fileName = "SmolLM2-360M-Instruct-Q8_0.gguf",
            parameterCount = "360M",
            quantization = "Q8_0",
            approxSizeMb = 386,
            description = "Ultra-lightweight on-device GGUF model. Instant startup on any Android phone with minimal battery usage.",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q8_0.gguf"
        ),
        OfflineModelCatalogItem(
            id = "llama-3.2-1b",
            displayName = "Llama 3.2 1B Instruct",
            fileName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            parameterCount = "1.2B",
            quantization = "Q4_K_M",
            approxSizeMb = 808,
            description = "Balanced conversational intelligence, summarization, and natural dialogue running 100% offline.",
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
        ),
        OfflineModelCatalogItem(
            id = "qwen2.5-1.5b",
            displayName = "Qwen 2.5 1.5B Instruct",
            fileName = "Qwen2.5-1.5B-Instruct-Q4_K_M.gguf",
            parameterCount = "1.5B",
            quantization = "Q4_K_M",
            approxSizeMb = 986,
            description = "Deep reasoning, math, coding, and human-like multi-turn empathy for flagship & mid-range phones.",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
        )
    )

    fun getDownloadedModels(): List<File> {
        return modelsDir.listFiles { f -> f.extension.equals("gguf", ignoreCase = true) }?.toList().orEmpty()
    }

    fun isModelDownloaded(item: OfflineModelCatalogItem): Boolean {
        val file = File(modelsDir, item.fileName)
        return file.exists() && file.length() > 1024 * 1024
    }

    fun getModelFile(item: OfflineModelCatalogItem): File = File(modelsDir, item.fileName)

    fun deleteModelFile(item: OfflineModelCatalogItem): Boolean {
        val file = File(modelsDir, item.fileName)
        return if (file.exists()) file.delete() else false
    }

    suspend fun downloadModel(item: OfflineModelCatalogItem): File? = withContext(Dispatchers.IO) {
        val targetFile = File(modelsDir, item.fileName)
        val tempFile = File(modelsDir, "${item.fileName}.part")

        try {
            _downloadStatus.value = ModelDownloadStatus(
                modelId = item.id,
                isDownloading = true,
                progressPercent = 1,
                statusText = "Connecting to HuggingFace Open Weights..."
            )

            val request = Request.Builder().url(item.downloadUrl).get().build()
            firewall.anonymousHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    _downloadStatus.value = ModelDownloadStatus(
                        modelId = item.id,
                        isDownloading = false,
                        progressPercent = 0,
                        statusText = "Download failed (HTTP ${response.code})"
                    )
                    return@withContext null
                }

                val body = response.body ?: return@withContext null
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            if (totalBytes > 0) {
                                val pct = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(1, 99)
                                _downloadStatus.value = ModelDownloadStatus(
                                    modelId = item.id,
                                    isDownloading = true,
                                    progressPercent = pct,
                                    statusText = "Downloading ${item.displayName}: $pct% (${downloadedBytes / (1024 * 1024)} MB)"
                                )
                            }
                        }
                        output.flush()
                    }
                }
            }

            if (tempFile.renameTo(targetFile)) {
                _downloadStatus.value = ModelDownloadStatus(
                    modelId = item.id,
                    isDownloading = false,
                    progressPercent = 100,
                    statusText = "Ready Offline: ${item.displayName}"
                )
                return@withContext targetFile
            }
        } catch (e: Exception) {
            tempFile.delete()
            _downloadStatus.value = ModelDownloadStatus(
                modelId = item.id,
                isDownloading = false,
                progressPercent = 0,
                statusText = "Download interrupted: ${e.localizedMessage ?: "Network unavailable"}"
            )
        }
        null
    }
}
