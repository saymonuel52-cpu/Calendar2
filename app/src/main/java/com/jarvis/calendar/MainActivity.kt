package com.jarvis.calendar

import android.os.Bundle
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import com.jarvis.calendar.ai.LocalAI
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
    
    var aiStatus by remember { mutableStateOf("checking") }
    var aiResponse by remember { mutableStateOf("") }
    var userQuestion by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        aiStatus = if (LocalAI.isServerRunning()) "online" else "offline"
    }
    
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("🤖 Джарвис Календарь", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when(aiStatus) {
                    "online" -> Color(0xFF4CAF50)
                    "offline" -> Color(0xFFFF5722)
                    else -> Color(0xFFFFC107)
                }
            )
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when(aiStatus) {
                        "online" -> "✅ ИИ подключен (Termux)"
                        "offline" -> "❌ ИИ не запущен. Запусти в Termux:\n~/llama.cpp/build/bin/llama-server -m ~/llama.cpp/models/qwen2.5-3b-instruct-q4_k_m.gguf -c 2048 --host 127.0.0.1 --port 8080"
                        else -> "⏳ Проверка ИИ..."
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (aiStatus == "online") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = userQuestion,
                    onValueChange = { userQuestion = it },
                    label = { Text("Спроси Джарвиса...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (userQuestion.isNotBlank()) {
                        coroutineScope.launch {
                            aiResponse = "Думаю..."
                            aiResponse = LocalAI.chat(userQuestion)
                        }
                        userQuestion = ""
                    }
                }) {
                    Text("🚀")
                }
            }
            if (aiResponse.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                     colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                    Text(aiResponse, modifier = Modifier.padding(12.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
