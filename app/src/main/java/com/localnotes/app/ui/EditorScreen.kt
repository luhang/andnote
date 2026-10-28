package com.localnotes.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class EditorSnapshot(
    val title: String,
    val content: String,
    val pinned: Boolean
)

private val EditorSnapshotSaver = listSaver<EditorSnapshot, Any>(
    save = { listOf(it.title, it.content, it.pinned) },
    restore = {
        EditorSnapshot(
            title = it[0] as String,
            content = it[1] as String,
            pinned = it[2] as Boolean
        )
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: Long?,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var loaded by rememberSaveable { mutableStateOf(noteId == null) }
    var currentId by remember { mutableStateOf(noteId) }
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var pinned by rememberSaveable { mutableStateOf(false) }

    var baseline by rememberSaveable(stateSaver = EditorSnapshotSaver) {
        mutableStateOf(EditorSnapshot("", "", false))
    }
    var savedAt by remember { mutableStateOf<Long?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // 打开已有笔记时加载一次（旋转屏幕后不重复加载，避免覆盖未保存的输入）
    LaunchedEffect(noteId) {
        if (loaded) return@LaunchedEffect
        if (noteId != null) {
            viewModel.loadNote(noteId)?.let { note ->
                title = note.title
                content = note.content
                pinned = note.pinned
                savedAt = note.updatedAt
                baseline = EditorSnapshot(note.title, note.content, note.pinned)
            }
        }
        loaded = true
    }

    // 停止输入 600ms 自动保存
    val snapshot = EditorSnapshot(title, content, pinned)
    LaunchedEffect(snapshot, loaded) {
        if (!loaded) return@LaunchedEffect
        if (snapshot == baseline) return@LaunchedEffect
        delay(600)
        val id = viewModel.upsertNote(currentId, snapshot.title, snapshot.content, snapshot.pinned)
        if (id != null) {
            currentId = id
            baseline = snapshot
            savedAt = System.currentTimeMillis()
        }
    }

    val saveAndExit: () -> Unit = {
        scope.launch {
            if (loaded && snapshot != baseline) {
                viewModel.upsertNote(currentId, snapshot.title, snapshot.content, snapshot.pinned)
                    ?.let { currentId = it }
            }
            onBack()
        }
    }

    BackHandler { saveAndExit() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (noteId == null) "新建笔记" else "编辑笔记",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = saveAndExit) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回并保存"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { pinned = !pinned }) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = if (pinned) "取消置顶" else "置顶",
                            tint = if (pinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多操作")
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("删除", color = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    menuOpen = false
                                    confirmDelete = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it.take(120) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("标题", style = MaterialTheme.typography.headlineSmall)
                },
                textStyle = MaterialTheme.typography.headlineSmall,
                singleLine = true,
                colors = transparentFieldColors()
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            TextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = {
                    Text("开始输入…", style = MaterialTheme.typography.bodyLarge)
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = transparentFieldColors()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${content.length} 字",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = savedAt?.let { "已保存 ${formatClock(it)}" } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这条笔记？") },
            text = { Text("删除后不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        scope.launch {
                            currentId?.let { viewModel.deleteNote(it) }
                            onBack()
                        }
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent
)
