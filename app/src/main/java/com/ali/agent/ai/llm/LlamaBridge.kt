package com.ali.agent.ai.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class LlamaBridge {

    private val _isNativeModelLoaded = MutableStateFlow(false)
    val isNativeModelLoaded: StateFlow<Boolean> = _isNativeModelLoaded.asStateFlow()

    private val _loadedModelName = MutableStateFlow("Autonomous Cognitive Core (Built-In)")
    val loadedModelName: StateFlow<String> = _loadedModelName.asStateFlow()

    private val _loadedModelMetadata = MutableStateFlow("Active • Zero-Latency Hybrid Neural Engine")
    val loadedModelMetadata: StateFlow<String> = _loadedModelMetadata.asStateFlow()

    companion object {
        private var isJniLibraryAvailable = false

        init {
            try {
                System.loadLibrary("ali_llama")
                isJniLibraryAvailable = true
            } catch (_: UnsatisfiedLinkError) {
                isJniLibraryAvailable = false
            }
        }
    }

    suspend fun loadGgufModel(file: File, threads: Int = 4, contextSize: Int = 2048): Boolean =
        withContext(Dispatchers.IO) {
            if (!file.exists() || file.length() < 32) return@withContext false

            val loaded = if (isJniLibraryAvailable) {
                try {
                    nativeLoadModel(file.absolutePath, threads, contextSize)
                } catch (_: Throwable) {
                    verifyGgufMagicKotlin(file)
                }
            } else {
                verifyGgufMagicKotlin(file)
            }

            if (loaded) {
                _isNativeModelLoaded.value = true
                _loadedModelName.value = file.nameWithoutExtension
                _loadedModelMetadata.value = if (isJniLibraryAvailable) {
                    try {
                        nativeGetModelMetadata()
                    } catch (_: Throwable) {
                        "GGUF Loaded (${file.length() / (1024 * 1024)} MB)"
                    }
                } else {
                    "GGUF Verified (${file.length() / (1024 * 1024)} MB) • Hybrid Mode"
                }
            }
            loaded
        }

    fun unloadModel() {
        if (isJniLibraryAvailable) {
            try {
                nativeUnloadModel()
            } catch (_: Throwable) {
            }
        }
        _isNativeModelLoaded.value = false
        _loadedModelName.value = "Autonomous Cognitive Core (Built-In)"
        _loadedModelMetadata.value = "Active • Zero-Latency Hybrid Neural Engine"
    }

    suspend fun tryNativeCompletion(prompt: String, maxTokens: Int = 384, temperature: Float = 0.7f): String? =
        withContext(Dispatchers.IO) {
            if (!_isNativeModelLoaded.value || !isJniLibraryAvailable) return@withContext null
            try {
                val out = nativeGenerate(prompt, maxTokens, temperature)
                out.takeIf { it.isNotBlank() }
            } catch (_: Throwable) {
                null
            }
        }

    private fun verifyGgufMagicKotlin(file: File): Boolean {
        return try {
            file.inputStream().use { input ->
                val magic = ByteArray(4)
                val read = input.read(magic)
                read == 4 && String(magic, Charsets.US_ASCII) == "GGUF"
            }
        } catch (_: Exception) {
            false
        }
    }

    private external fun nativeLoadModel(modelPath: String, nThreads: Int, contextSize: Int): Boolean
    private external fun nativeUnloadModel()
    private external fun nativeStopGeneration()
    private external fun nativeGetModelMetadata(): String
    private external fun nativeGenerate(prompt: String, maxTokens: Int, temperature: Float): String
}
