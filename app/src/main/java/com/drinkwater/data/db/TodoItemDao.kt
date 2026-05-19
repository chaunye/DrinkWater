package com.drinkwater.data.db

import androidx.room.*
import com.drinkwater.data.model.TodoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoItemDao {
    @Query("SELECT * FROM todo_items ORDER BY isCompleted ASC, createdAt DESC")
    fun getAll(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getUncompleted(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE isCompleted = 0 AND applyToAll = 1")
    suspend fun getGlobalUncompleted(): List<TodoItem>

    @Query("SELECT * FROM todo_items WHERE isCompleted = 0")
    suspend fun getAllUncompleted(): List<TodoItem>

    @Query("SELECT * FROM todo_items WHERE id = :id")
    suspend fun getById(id: Long): TodoItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TodoItem): Long

    @Update
    suspend fun update(item: TodoItem)

    @Delete
    suspend fun delete(item: TodoItem)

    @Query("DELETE FROM todo_items WHERE isCompleted = 1")
    suspend fun deleteCompleted()
}
