package com.jarvis.calendar.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalModel(private val context: Context) {
    
    init {
        System.loadLibrary("jarvis-ai")
    }
    
    fun isModelDownloaded(): Boolean {
        val modelFile = File(context.getExternalFilesDir(null), "qwen2.5-1.5b-instruct-q4_k_m.gguf")
        return modelFile.exists() && modelFile.length() > 500_000_000
    }
    
    fun getModelPath(): String {
        return File(context.getExternalFilesDir(null), "qwen2.5-1.5b-instruct-q4_k_m.gguf").absolutePath
    }
    
    suspend fun downloadModel(onProgress: (Int) -> Unit): Result<String> = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.getExternalFilesDir(null), "qwen2.5-1.5b-instruct-q4_k_m.gguf")
            val url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
            
            // Используем OkHttp для скачивания с прогрессом
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                .build()
            
            val request = okhttp3.Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            val totalBytes = response.body?.contentLength() ?: 0
            val inputStream = response.body?.byteStream()
            
            if (inputStream == null) {
                return@withContext Result.failure(Exception("Не удалось получить поток данных"))
            }
            
            modelFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var downloadedBytes = 0L
                var bytesRead: Int
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    
                    val progress = ((downloadedBytes.toDouble() / totalBytes) * 100).toInt()
                    onProgress(progress.coerceIn(0, 100))
                }
            }
            
            Result.success("Модель скачана: ${modelFile.length() / (1024 * 1024)} МБ")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    external fun initModel(modelPath: String): Boolean
    external fun generate(prompt: String): String
    external fun freeModel()
}
