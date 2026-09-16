package com.jarvis.calendar.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

object LocalAI {
    const val SERVER_URL = "http://:8080"
    
    suspend fun isServerRunning(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$SERVER_URL/health")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            val code = conn.responseCode
            conn.disconnect()
            code == 200
        } catch (e: Exception) { false }
    }
    
    suspend fun chat(userMessage: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("$SERVER_URL/v1/chat/completions")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 120000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            
            val body = JSONObject().apply {
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "system")
                    put("content", "Ты — Джарвис, помощник в календаре. Отвечай кратко на русском.")
                }).put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                }))
                put("max_tokens", 512)
                put("temperature", 0.7)
            }
            
            conn.outputStream.write(body.toString().toByteArray())
            
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = reader.readText()
            reader.close()
            conn.disconnect()
            
            val respJson = JSONObject(response)
            respJson.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        } catch (e: Exception) {
            "Ошибка: ${e.message}. Убедись, что сервер ИИ запущен в Termux."
        }
    }
}
