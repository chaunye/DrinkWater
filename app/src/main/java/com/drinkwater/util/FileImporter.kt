package com.drinkwater.util

import android.content.Context
import android.net.Uri
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.InputStreamReader

object FileImporter {

    data class ImportedWord(val word: String, val definition: String)

    /**
     * Import words from Excel (.xls, .xlsx) or CSV/TXT file.
     * Excel: expects 2 columns - word, definition
     * CSV/TXT: each line is "word\tdefinition" or "word definition" or just "word"
     */
    fun importWords(context: Context, uri: Uri, fileName: String): List<ImportedWord> {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "xls", "xlsx" -> importExcel(context, uri)
            "csv", "txt" -> importText(context, uri)
            else -> importText(context, uri)
        }
    }

    private fun importExcel(context: Context, uri: Uri): List<ImportedWord> {
        val words = mutableListOf<ImportedWord>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            for (row in sheet) {
                if (row.rowNum == 0) continue // skip header
                val word = row.getCell(0)?.toString()?.trim() ?: continue
                val definition = row.getCell(1)?.toString()?.trim() ?: ""
                if (word.isNotBlank()) {
                    words.add(ImportedWord(word, definition))
                }
            }
            workbook.close()
        }
        return words
    }

    private fun importText(context: Context, uri: Uri): List<ImportedWord> {
        val words = mutableListOf<ImportedWord>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isNotBlank()) {
                    val parts = trimmed.split("\t", "  ", limit = 2)
                    val word = parts[0].trim()
                    val definition = if (parts.size > 1) parts[1].trim() else ""
                    if (word.isNotBlank()) {
                        words.add(ImportedWord(word, definition))
                    }
                }
            }
        }
        return words
    }

    /**
     * Import reminders from text content (memo/clipboard).
     * Each non-blank line becomes a reminder.
     */
    fun importReminders(text: String): List<String> {
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length >= 2 }
    }
}
