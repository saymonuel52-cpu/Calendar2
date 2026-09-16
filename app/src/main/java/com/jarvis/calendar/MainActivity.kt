package com.jarvis.calendar

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    
    // Состояния для ИИ
    var isModelReady by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadStatus by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(0L) }
    var chatInstance by remember { mutableStateOf<Chat?>(null) }
    var aiResponse by remember { mutableStateOf("") }
    var userPrompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

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
                        // Анимированный индикатор
                        LinearProgressIndicator(
                            progress = downloadProgress, 
                            modifier = Modifier.fillMaxWidth(), 
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Текстовый статус
                        Text(
                            text = downloadStatus,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        
                        // Время загрузки
                        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
                        Text(
                            text = "⏱ Время загрузки: ${elapsedSeconds}s",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        
                        // Подсказка
                        Text(
                            text = "💡 Размер модели: ~1 ГБ. При медленном интернете это может занять 10-15 минут.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Text(
                            text = "Нажмите кнопку для загрузки модели Qwen 2.5 1.5B",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            isDownloading = true
                            startTime = System.currentTimeMillis()
                            downloadProgress = 0.1f
                            downloadStatus = " Инициализация загрузки..."
                            
                            (context as MainActivity).lifecycleScope.launch {
                                // Симуляция обновления прогресса (так как NobodyWho не даёт callback'ов)
                                launch {
                                    while (isDownloading) {
                                        delay(2000)
                                        if (downloadProgress < 0.9f) {
                                            downloadProgress += 0.05f
                                            downloadStatus = "⬇️ Загрузка модели с Hugging Face...\n${(downloadProgress * 100).toInt()}%"
                                        }
                                    }
                                }
                                
                                try {
                                    downloadStatus = "🔗 Подключение к Hugging Face..."
                                    delay(500)
                                    
                                    downloadStatus = "📥 Скачивание qwen2.5-1.5b-instruct-q4_k_m.gguf..."
                                    chatInstance = Chat.fromPath("hf://Qwen/Qwen2.5-1.5B-Instruct-GGUF/qwen2.5-1.5b-instruct-q4_k_m.gguf")
                                    
                                    downloadProgress = 1.0f
                                    isModelReady = true
                                    isDownloading = false
                                    Toast.makeText(context, "✅ Модель успешно загружена!", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    isDownloading = false
                                    downloadStatus = "❌ Ошибка: ${e.message?.take(100)}"
                                    Toast.makeText(context, "Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }, modifier = Modifier.align(Alignment.End)) {
                            Text("Начать загрузку")
                        }
                    }
                } else {
                    Text("Модель Qwen 2.5 1.5B загружена и работает локально.", color = Color.White.copy(alpha = 0.9f))
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
                                        aiResponse = "Ошибка генерации: ${e.message}"
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                                userPrompt = ""
                            }
                        }, enabled = !isGenerating && chatInstance != null) {
                            Text(if (isGenerating) "⏳" else "🚀")
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
