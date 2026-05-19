package com.drinkwater.data.db

import androidx.room.*
import com.drinkwater.data.model.WordList
import kotlinx.coroutines.flow.Flow

@Dao
interface WordListDao {
    @Query("SELECT * FROM word_lists ORDER BY importDate DESC")
    fun getAll(): Flow<List<WordList>>

    @Insert
    suspend fun insert(wordList: WordList): Long

    @Delete
    suspend fun delete(wordList: WordList)

    @Query("DELETE FROM word_lists WHERE id = :id")
    suspend fun deleteById(id: Long)
}
