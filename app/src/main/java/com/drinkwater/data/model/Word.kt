package com.drinkwater.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = WordList::class,
            parentColumns = ["id"],
            childColumns = ["wordListId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("wordListId")]
)
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordListId: Long,
    val word: String,
    val definition: String
)
