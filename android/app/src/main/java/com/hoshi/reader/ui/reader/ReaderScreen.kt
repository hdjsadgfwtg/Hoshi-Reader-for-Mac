package com.hoshi.reader.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val metadata by viewModel.metadata.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val currentProgress by viewModel.currentProgress.collectAsState()
    val chapterHref by viewModel.chapterHref.collectAsState()

    val fontSize by viewModel.userConfig.fontSize.collectAsState(initial = 18)
    val lineHeight by viewModel.userConfig.lineHeight.collectAsState(initial = 1.8)
    val verticalWriting by viewModel.userConfig.verticalWriting.collectAsState(initial = true)
    val theme by viewModel.userConfig.theme.collectAsState(initial = com.hoshi.reader.core.storage.ReaderTheme.LIGHT)
    val customCss by viewModel.userConfig.customCss.collectAsState(initial = "")

    var showToolbar by remember { mutableStateOf(false) }
    var showChapterList by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }
    var webView by remember { mutableStateOf<ReaderWebView?>(null) }

    val contentDir = viewModel.contentDir

    if (metadata == null || contentDir == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            if (showToolbar) {
                TopAppBar(
                    title = { Text(metadata?.title ?: "", maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showChapterList = true }) {
                            Icon(Icons.Default.List, contentDescription = "Chapters")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    ReaderWebView(ctx, contentDir!!, object : ReaderWebView.Callbacks {
                        override fun onRestoreCompleted() {}

                        override fun onTextSelected(json: String) {
                            try {
                                val obj = org.json.JSONObject(json)
                                val text = obj.optString("text", "")
                                if (text.isNotEmpty()) {
                                    selectedText = text
                                    showPopup = true
                                }
                            } catch (_: Exception) {}
                        }

                        override fun onSelectionState(hasSelection: Boolean) {}

                        override fun onPageChanged(direction: String) {
                            when (direction) {
                                "forward" -> webView?.paginate("forward")
                                "backward" -> webView?.paginate("backward")
                                "next_chapter" -> viewModel.nextChapter()
                                "prev_chapter" -> viewModel.previousChapter()
                            }
                        }
                    }).also { wv ->
                        webView = wv
                    }
                },
                update = { _ -> }
            )
        }
    }

    LaunchedEffect(chapterHref) {
        chapterHref?.let { href ->
            webView?.loadChapter(href)
        }
    }

    LaunchedEffect(chapterHref, fontSize, lineHeight, verticalWriting, theme, customCss) {
        if (chapterHref != null) {
            // small delay to let page load
            kotlinx.coroutines.delay(300)
            webView?.applyStyles(fontSize, lineHeight, verticalWriting, theme.name, customCss)
            kotlinx.coroutines.delay(200)
            webView?.restoreProgress(currentProgress)
        }
    }

    if (showPopup && selectedText.isNotEmpty()) {
        com.hoshi.reader.ui.popup.PopupSheet(
            selectedText = selectedText,
            onDismiss = { showPopup = false },
            onMineEntry = { /* Phase 3: Anki integration */ }
        )
    }

    if (showChapterList) {
        ModalBottomSheet(
            onDismissRequest = { showChapterList = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ChapterListSheet(
                chapters = chapters,
                currentChapter = currentChapter,
                onChapterClick = { index ->
                    viewModel.navigateToChapter(index)
                    showChapterList = false
                }
            )
        }
    }
}

@Composable
private fun ChapterListSheet(
    chapters: List<com.hoshi.reader.data.model.ChapterInfo>,
    currentChapter: Int,
    onChapterClick: (Int) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(chapters.size) { index ->
            val chapter = chapters[index]
            val isCurrent = index == currentChapter
            androidx.compose.material3.ListItem(
                headlineContent = {
                    Text(
                        text = chapter.title ?: "Chapter ${index + 1}",
                        color = if (isCurrent)
                            androidx.compose.material3.MaterialTheme.colorScheme.primary
                        else
                            androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )
                },
                trailingContent = {
                    if (chapter.characterCount > 0) {
                        Text(
                            text = "${chapter.characterCount} chars",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                },
                modifier = Modifier.clickable { onChapterClick(index) },
                colors = androidx.compose.material3.ListItemDefaults.colors(
                    containerColor = if (isCurrent)
                        androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        androidx.compose.material3.MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}
