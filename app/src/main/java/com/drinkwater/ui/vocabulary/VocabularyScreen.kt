package com.drinkwater.ui.vocabulary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drinkwater.DrinkWaterApp
import com.drinkwater.data.model.Word
import com.drinkwater.data.model.WordList
import com.drinkwater.util.FileImporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as DrinkWaterApp).database
    val scope = rememberCoroutineScope()

    val wordLists by db.wordListDao().getAll().collectAsState(initial = emptyList())
    var selectedList by remember { mutableStateOf<WordList?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val fileName = getFileName(context, uri) ?: "unknown"
        scope.launch {
            val imported = FileImporter.importWords(context, uri, fileName)
            if (imported.isNotEmpty()) {
                val listId = db.wordListDao().insert(WordList(name = fileName.substringBeforeLast('.')))
                db.wordDao().insertAll(imported.map {
                    Word(wordListId = listId, word = it.word, definition = it.definition)
                })
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("生词本") },
            actions = {
                IconButton(onClick = {
                    filePicker.launch(arrayOf(
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "text/csv",
                        "text/plain",
                        "application/octet-stream"
                    ))
                }) {
                    Icon(Icons.Default.FileUpload, contentDescription = "导入")
                }
            }
        )

        if (selectedList != null) {
            WordListDetail(
                wordList = selectedList!!,
                db = db,
                onBack = { selectedList = null }
            )
        } else if (wordLists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.LightGray, size = 64.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("还没有导入词表", color = Color.Gray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("支持 Excel / CSV / TXT 格式", color = Color.LightGray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(wordLists, key = { it.id }) { wordList ->
                    WordListCard(
                        wordList = wordList,
                        wordCount = 0, // TODO: count
                        onClick = { selectedList = wordList },
                        onDelete = {
                            scope.launch { db.wordListDao().delete(wordList) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WordListCard(
    wordList: WordList,
    wordCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(wordList.name, fontWeight = FontWeight.Medium)
                Text("${java.text.SimpleDateFormat("yyyy-MM-dd").format(wordList.importDate)} 导入", fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListDetail(wordList: WordList, db: com.drinkwater.data.db.AppDatabase, onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val words by if (searchQuery.isBlank()) {
        db.wordDao().getByList(wordList.id).collectAsState(initial = emptyList())
    } else {
        db.wordDao().search(wordList.id, searchQuery).collectAsState(initial = emptyList())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(wordList.name) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜索单词...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(words, key = { it.id }) { word ->
                WordItem(word = word)
            }
        }
    }
}

@Composable
fun WordItem(word: Word) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(word.word, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (word.definition.isNotBlank()) {
                    Text(word.definition, fontSize = 13.sp, color = Color.Gray)
                }
            }
        }
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) it.getString(nameIndex) else null
        } else null
    }
}
