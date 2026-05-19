package com.drinkwater.util

/**
 * Extracts actionable reminders from plain text using local keyword matching.
 * Lines containing keywords like "要", "记得", "别忘了", "需" are prioritized.
 */
object KeywordExtractor {

    private val keywords = listOf(
        "要", "记得", "别忘了", "需要", "必须", "务必", "应该",
        "remember", "todo", "need", "must", "should", "don't forget"
    )

    data class ExtractedReminder(
        val content: String,
        val isHighPriority: Boolean
    )

    fun extract(text: String): List<ExtractedReminder> {
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length >= 2 }
            .map { line ->
                ExtractedReminder(
                    content = line,
                    isHighPriority = keywords.any { kw ->
                        line.contains(kw, ignoreCase = true)
                    }
                )
            }
    }
}
