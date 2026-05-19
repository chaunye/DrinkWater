package com.drinkwater.data.db

import androidx.room.*
import com.drinkwater.data.model.Word
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE wordListId = :listId ORDER BY id ASC")
    fun getByList(listId: Long): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE wordListId = :listId AND (word LIKE '%' || :query || '%' OR definition LIKE '%' || :query || '%')")
    fun search(listId: Long, query: String): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE word LIKE '%' || :query || '%' OR definition LIKE '%' || :query || '%'")
    fun searchAll(query: String): Flow<List<Word>>

    @Insert
    suspend fun insertAll(words: List<Word>)

    @Update
    suspend fun update(word: Word)

    @Delete
    suspend fun delete(word: Word)
}
