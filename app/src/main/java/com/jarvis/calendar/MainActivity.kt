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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

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
                    text = if (isModelReady) "✅ ИИ готов к работе" else "📥 Скачать ИИ-модель (~1 ГБ)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (!isModelReady) {
                    if (isDownloading) {
                        LinearProgressIndicator(progress = downloadProgress, modifier = Modifier.fillMaxWidth(), color = Color.White)
                        Text("Загрузка модели с Hugging Face...", color = Color.White)
                    } else {
                        Button(onClick = {
                            isDownloading = true
                            // Используем lifecycleScope Activity для безопасной работы с корутинами
                            (context as MainActivity).lifecycleScope.launch {
                                try {
                                    // NobodyWho умеет сам скачивать модель по hf:// ссылке!
                                    // Используем легкую Qwen 1.5B для быстрой работы на телефоне
                                    chatInstance = Chat.fromPath("hf://Qwen/Qwen2.5-1.5B-Instruct-GGUF/qwen2.5-1.5b-instruct-q4_k_m.gguf")
                                    isModelReady = true
                                    isDownloading = false
                                    Toast.makeText(context, "Модель успешно загружена и инициализирована!", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    isDownloading = false
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
                                        // Потоковая генерация токенов в реальном времени!
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
