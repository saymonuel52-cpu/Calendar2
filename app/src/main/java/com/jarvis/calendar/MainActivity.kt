package com.jarvis.calendar

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import ai.nobodywho.Chat
import com.jarvis.calendar.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "jarvis_db").build()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(db)
                }
            }
        }
    }
}

@Composable
fun MainScreen(db: AppDatabase) {
    val tabs by db.dynamicDao().allTabs().collectAsState(initial = emptyList())
    var newTabName by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var isModelReady by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadStatus by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var chatInstance by remember { mutableStateOf<Chat?>(null) }
    var aiResponse by remember { mutableStateOf("") }
    var userPrompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    
    // Анимация для индикатора загрузки
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("🤖 Джарвис Календарь", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (isModelReady) Color(0xFF4CAF50) else Color(0xFF2196F3))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isModelReady) "✅ ИИ готов к работе" else "📥 Скачивание ИИ-модели",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (!isModelReady) {
                    if (isDownloading) {
                        // Анимированный индикатор (пока нет реального прогресса)
                        LinearProgressIndicator(
                            progress = animatedProgress, 
                            modifier = Modifier.fillMaxWidth(), 
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Статус
                        Text(downloadStatus, color = Color.White, fontSize = 14.sp)
                        
                        // Лог ошибок
                        if (errorMessage.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336))
                            ) {
                                Text(
                                    "❌ ОШИБКА:\n$errorMessage",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                        
                        // Подсказка
                        Text(
                            " Подождите 5-15 минут. Не закрывайте приложение!",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        
                        // Кнопка отмены
                        Button(
                            onClick = {
                                isDownloading = false
                                downloadStatus = "Загрузка отменена"
                            },
                            modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("Отмена", color = Color(0xFF2196F3))
                        }
                    } else {
                        Text("Нажмите для загрузки модели Qwen 2.5 1.5B (~1 ГБ)", color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            isDownloading = true
                            errorMessage = ""
                            downloadStatus = "🔄 Подготовка..."
                            downloadProgress = 0f
                            
                            Log.d("JarvisAI", "Starting model download...")
                            
                            (context as MainActivity).lifecycleScope.launch {
                                try {
                                    // Устанавливаем таймаут 20 минут
                                    val result = withTimeoutOrNull(20 * 60 * 1000) {
                                        withContext(Dispatchers.IO) {
                                            try {
                                                downloadStatus = "🔗 Подключение к Hugging Face..."
                                                Log.d("JarvisAI", "Connecting to Hugging Face...")
                                                delay(500)
                                                
                                                downloadStatus = "📥 Скачивание модели (это может занять время)..."
                                                Log.d("JarvisAI", "Downloading model...")
                                                
                                                // Пытаемся загрузить модель
                                                val chat = Chat.fromPath("hf://Qwen/Qwen2.5-1.5B-Instruct-GGUF/qwen2.5-1.5b-instruct-q4_k_m.gguf")
                                                
                                                Log.d("JarvisAI", "Model loaded successfully!")
                                                chat
                                            } catch (e: Exception) {
                                                Log.e("JarvisAI", "Error during download: ${e.message}", e)
                                                throw e
                                            }
                                        }
                                    }
                                    
                                    if (result != null) {
                                        chatInstance = result
                                        downloadStatus = "✅ Модель успешно загружена!"
                                        delay(500)
                                        isModelReady = true
                                        Toast.makeText(context, "ИИ готов к работе!", Toast.LENGTH_LONG).show()
                                    } else {
                                        errorMessage = "Превышено время ожидания (20 минут). Проверьте интернет и попробуйте снова."
                                        Log.e("JarvisAI", "Timeout exceeded")
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Ошибка загрузки: ${e.message ?: "Неизвестная ошибка"}\n\nПроверьте:\n1. Интернет-соединение\n2. Доступ к Hugging Face\n3. Достаточно места на телефоне"
                                    Log.e("JarvisAI", "Exception: ${e.message}", e)
                                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isDownloading = false
                                }
                            }
                        }, modifier = Modifier.align(Alignment.End)) {
                            Text("Начать загрузку")
                        }
                    }
                } else {
                    Text("Модель загружена и работает локально.", color = Color.White.copy(alpha = 0.9f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        OutlinedTextField(
                            value = userPrompt,
                            onValueChange = { userPrompt = it },
                            label = { Text("Ваш вопрос...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !isGenerating
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (userPrompt.isNotBlank() && chatInstance != null) {
                                isGenerating = true
                                aiResponse = ""
                                (context as MainActivity).lifecycleScope.launch {
                                    try {
                                        chatInstance!!.ask(userPrompt).asFlow().catch { e ->
                                            aiResponse += "\n[Ошибка: ${e.message}]"
                                        }.collect { token ->
                                            aiResponse += token
                                        }
                                    } catch (e: Exception) {
                                        aiResponse = "Ошибка: ${e.message}"
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                                userPrompt = ""
                            }
                        }, enabled = !isGenerating && chatInstance != null) {
                            Text(if (isGenerating) "⏳" else "")
                        }
                    }
                    if (aiResponse.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(aiResponse, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Мои вкладки (${tabs.size}):", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTabName,
                onValueChange = { newTabName = it },
                label = { Text("Название") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (newTabName.isNotBlank()) {
                    coroutineScope.launch {
                        db.dynamicDao().insertTab(CustomTab(name = newTabName.trim(), icon = "📋"))
                    }
                    newTabName = ""
                }
            }) {
                Text("Создать")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (tabs.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                Text("📭 Пока нет вкладок", modifier = Modifier.padding(16.dp), color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(tabs) { tab ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(android.graphics.Color.parseColor(tab.color)))
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("${tab.icon} ${tab.name}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    db.dynamicDao().deleteTab(tab.id)
                                }
                            }) {
                                Text("🗑️", fontSize = 20.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
