package com.jarvis.calendar.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "custom_tabs")
data class CustomTab(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "📋",
    val color: String = "#6200EE",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_fields", foreignKeys = [ForeignKey(entity = CustomTab::class, parentColumns = ["id"], childColumns = ["tabId"], onDelete = ForeignKey.CASCADE)])
data class CustomField(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tabId: Long,
    val name: String,
    val type: String = "text",
    val options: String = "",
    val order: Int = 0
)

@Entity(tableName = "custom_data", foreignKeys = [ForeignKey(entity = CustomTab::class, parentColumns = ["id"], childColumns = ["tabId"], onDelete = ForeignKey.CASCADE)])
data class CustomData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tabId: Long,
    val title: String,
    val valuesJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface DynamicDao {
    @Query("SELECT * FROM custom_tabs ORDER BY createdAt DESC")
    fun allTabs(): Flow<List<CustomTab>>
    
    @Insert
    suspend fun insertTab(tab: CustomTab): Long
    
    @Query("DELETE FROM custom_tabs WHERE id = :id")
    suspend fun deleteTab(id: Long)
    
    @Query("SELECT * FROM custom_fields WHERE tabId = :tabId ORDER BY `order`")
    fun fieldsOf(tabId: Long): Flow<List<CustomField>>
    
    @Insert
    suspend fun insertField(field: CustomField): Long
    
    @Query("SELECT * FROM custom_data WHERE tabId = :tabId ORDER BY createdAt DESC")
    fun dataOf(tabId: Long): Flow<List<CustomData>>
    
    @Insert
    suspend fun insertData(data: CustomData): Long
}

@Database(entities = [CustomTab::class, CustomField::class, CustomData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dynamicDao(): DynamicDao
}
