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
import androidx.room.Room
import com.jarvis.calendar.ai.LocalModel
import com.jarvis.calendar.data.*
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
    
    val localModel = remember { LocalModel(context) }
    var isModelReady by remember { mutableStateOf(localModel.isModelDownloaded()) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var aiResponse by remember { mutableStateOf("") }
    var userQuestion by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("🤖 Джарвис Календарь", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        
        // Кнопка скачать ИИ или статус
        if (!isModelReady) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(" Скачать ИИ-помощника", 
                         style = MaterialTheme.typography.titleMedium,
                         color = Color.White)
                    Text("Локальная модель (~1 ГБ) для работы без интернета", 
                         style = MaterialTheme.typography.bodySmall,
                         color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (isDownloading) {
                        LinearProgressIndicator(
                            progress = downloadProgress / 100f,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White
                        )
                        Text("Загрузка: $downloadProgress%", 
                             color = Color.White,
                             fontSize = 12.sp)
                    } else {
                        Button(onClick = {
                            isDownloading = true
                            coroutineScope.launch {
                                val result = localModel.downloadModel { progress ->
                                    downloadProgress = progress
                                }
                                isDownloading = false
                                isModelReady = result.isSuccess
                                Toast.makeText(context, 
                                    result.getOrElse { "Ошибка: ${it.message}" }, 
                                    Toast.LENGTH_LONG).show()
                            }
                        }, modifier = Modifier.align(Alignment.End)) {
                            Text("Начать загрузку")
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✅ ИИ готов к работе", 
                         style = MaterialTheme.typography.titleMedium,
                         color = Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Чат с ИИ (только если модель готова)
        if (isModelReady) {
            Card(modifier = Modifier.fillMaxWidth(), 
                 colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(" Спроси Джарвиса:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = userQuestion,
                            onValueChange = { userQuestion = it },
                            label = { Text("Вопрос...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (userQuestion.isNotBlank()) {
                                coroutineScope.launch {
                                    aiResponse = "Думаю..."
                                    aiResponse = localModel.generate(userQuestion)
                                }
                                userQuestion = ""
                            }
                        }, enabled = aiResponse != "Думаю...") {
                            Text("🚀")
                        }
                    }
                    
                    if (aiResponse.isNotBlank() && aiResponse != "Думаю...") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(aiResponse, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Создание вкладки
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTabName,
                onValueChange = { newTabName = it },
                label = { Text("Новая вкладка") },
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
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Мои вкладки (${tabs.size}):", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn {
            items(tabs) { tab ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(android.graphics.Color.parseColor(tab.color)))
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${tab.icon} ${tab.name}",
                             style = MaterialTheme.typography.titleMedium,
                             color = Color.White)
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
