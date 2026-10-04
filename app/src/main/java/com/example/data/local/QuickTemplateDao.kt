package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickTemplateDao {
    @Query("SELECT * FROM quick_templates ORDER BY orderIndex ASC, id ASC")
    fun getAllTemplates(): Flow<List<QuickTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: QuickTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<QuickTemplate>)

    @Update
    suspend fun updateTemplate(template: QuickTemplate)

    @Delete
    suspend fun deleteTemplate(template: QuickTemplate)

    @Query("DELETE FROM quick_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM quick_templates")
    suspend fun getTemplateCount(): Int
}
