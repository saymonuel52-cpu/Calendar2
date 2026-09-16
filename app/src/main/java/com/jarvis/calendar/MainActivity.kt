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
import androidx.room.Room
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
    
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("🤖 Джарвис Календарь", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        // Кнопка скачать ИИ
        Button(onClick = { /* TODO: проверка и скачивание */ }, 
               modifier = Modifier.fillMaxWidth(),
               colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC))) {
            Text("📥 Скачать ИИ-помощника")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTabName,
                onValueChange = { newTabName = it },
                label = { Text("Название вкладки") },
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
        
        Text("Мои вкладки:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (tabs.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                Text("📭 Пока нет вкладок\nСоздай первую выше!", 
                     modifier = Modifier.padding(16.dp),
                     color = Color.Gray)
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
                            Text("${tab.icon} ${tab.name}", 
                                 style = MaterialTheme.typography.titleMedium,
                                 color = Color.White)
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    db.dynamicDao().deleteTab(tab.id)
                                }
                            }) {
                                Text("️", fontSize = androidx.compose.ui.unit.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
