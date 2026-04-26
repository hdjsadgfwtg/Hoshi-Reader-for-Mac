package com.hoshi.reader.ui.dictionary

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hoshi.reader.core.dictionary.DictionaryManager
import com.hoshi.reader.data.model.DictionaryEntry
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    onBack: () -> Unit,
    viewModel: DictionaryViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()
    val importing by viewModel.importing.collectAsState()
    val importMessage by viewModel.importMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val tempFile = File(context.cacheDir, "import_dict.zip")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        viewModel.importDictionary(tempFile.absolutePath)
    }

    LaunchedEffect(importMessage) {
        importMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dictionaries") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                launcher.launch(arrayOf("application/zip", "application/octet-stream"))
            }) {
                if (importing) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimaryContainer)
                } else {
                    Icon(Icons.Default.Add, contentDescription = "Import Dictionary")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (config.termOrder.isNotEmpty()) {
                item {
                    SectionHeader("Term Dictionaries")
                }
                items(config.termOrder) { entry ->
                    DictionaryItem(
                        entry = entry,
                        onToggle = { viewModel.toggleDictionary(entry.name, DictionaryManager.DictType.TERM, it) },
                        onDelete = { viewModel.deleteDictionary(entry.name, DictionaryManager.DictType.TERM) }
                    )
                }
            }

            if (config.frequencyOrder.isNotEmpty()) {
                item {
                    SectionHeader("Frequency Dictionaries")
                }
                items(config.frequencyOrder) { entry ->
                    DictionaryItem(
                        entry = entry,
                        onToggle = { viewModel.toggleDictionary(entry.name, DictionaryManager.DictType.FREQUENCY, it) },
                        onDelete = { viewModel.deleteDictionary(entry.name, DictionaryManager.DictType.FREQUENCY) }
                    )
                }
            }

            if (config.pitchOrder.isNotEmpty()) {
                item {
                    SectionHeader("Pitch Accent Dictionaries")
                }
                items(config.pitchOrder) { entry ->
                    DictionaryItem(
                        entry = entry,
                        onToggle = { viewModel.toggleDictionary(entry.name, DictionaryManager.DictType.PITCH, it) },
                        onDelete = { viewModel.deleteDictionary(entry.name, DictionaryManager.DictType.PITCH) }
                    )
                }
            }

            if (config.termOrder.isEmpty() && config.frequencyOrder.isEmpty() && config.pitchOrder.isEmpty()) {
                item {
                    Text(
                        text = "No dictionaries imported.\nTap + to import a Yomitan dictionary ZIP.",
                        modifier = Modifier.padding(32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun DictionaryItem(
    entry: DictionaryEntry,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = entry.enabled,
            onCheckedChange = onToggle
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
