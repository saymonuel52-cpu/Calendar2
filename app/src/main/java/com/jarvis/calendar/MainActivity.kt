package com.jarvis.calendar

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.jarvis.calendar.data.*
import kotlinx.coroutines.launch
import java.io.File

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var isModelReady by remember { mutableStateOf(false) }
    var aiResponse by remember { mutableStateOf("") }
    var userPrompt by remember { mutableStateOf("") }

    val modelFile = File(context.getExternalFilesDir(null), "qwen2.5-1.5b-instruct-q4_k_m.gguf")
    
    // Проверяем, скачан ли файл при запуске
    LaunchedEffect(Unit) {
        isModelReady = modelFile.exists() && modelFile.length() > 500_000_000
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("🤖 Джарвис Календарь", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Блок ИИ
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
                        LinearProgressIndicator(progress = progress / 100f, modifier = Modifier.fillMaxWidth(), color = Color.White)
                        Text("Загрузка: $progress%", color = Color.White)
                    } else {
                        Button(onClick = {
                            isDownloading = true
                            coroutineScope.launch {
                                // Здесь будет реальная логика скачивания через OkHttp
                                // Для краткости имитируем процесс, в полной версии добавим OkHttp загрузчик
                                for (i in 1..10) {
                                    progress = i * 10
                                    kotlinx.coroutines.delay(500)
                                }
                                isDownloading = false
                                isModelReady = true
                                Toast.makeText(context, "Модель успешно загружена!", Toast.LENGTH_LONG).show()
                            }
                        }, modifier = Modifier.align(Alignment.End)) {
                            Text("Начать загрузку")
                        }
                    }
                } else {
                    Text("Модель загружена и готова к локальному запуску.", color = Color.White.copy(alpha = 0.9f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        OutlinedTextField(
                            value = userPrompt,
                            onValueChange = { userPrompt = it },
                            label = { Text("Ваш вопрос...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (userPrompt.isNotBlank()) {
                                aiResponse = "Думаю... (Здесь будет вызов dev.ffmpegkit.maintained.llama.android.Llama)"
                                userPrompt = ""
                            }
                        }) {
                            Text("🚀")
                        }
                    }
                    if (aiResponse.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(aiResponse, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Управление вкладками", style = MaterialTheme.typography.titleMedium)
        // ... (здесь остается твой рабочий код создания вкладок, который мы уже отладили)
    }
}
