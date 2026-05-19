package com.drinkwater.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_lists")
data class WordList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val importDate: Long = System.currentTimeMillis()
)
