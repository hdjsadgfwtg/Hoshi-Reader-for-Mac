package com.hoshi.reader.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.hoshi.reader.core.storage.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDictionaries: () -> Unit,
    onAnki: () -> Unit,
    onAppearance: () -> Unit,
    backupManager: BackupManager
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                backupManager.exportBackup(uri)
                snackbarHostState.showSnackbar("Backup exported")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Export failed: ${e.message}")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                backupManager.importBackup(uri)
                snackbarHostState.showSnackbar("Backup restored")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Restore failed: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Dictionaries") },
                    supportingContent = { Text("Import and manage Yomitan dictionaries") },
                    leadingContent = { Icon(Icons.Default.Book, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onDictionaries)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Anki") },
                    supportingContent = { Text("AnkiConnect settings and card templates") },
                    leadingContent = { Icon(Icons.Default.Style, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onAnki)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Appearance") },
                    supportingContent = { Text("Font, theme, writing mode") },
                    leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onAppearance)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Export Backup") },
                    supportingContent = { Text("Save books and settings as ZIP") },
                    leadingContent = { Icon(Icons.Default.Backup, contentDescription = null) },
                    modifier = Modifier.clickable { exportLauncher.launch("hibiki_backup.zip") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Restore Backup") },
                    supportingContent = { Text("Import from backup ZIP") },
                    leadingContent = { Icon(Icons.Default.Restore, contentDescription = null) },
                    modifier = Modifier.clickable {
                        importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                    }
                )
            }
        }
    }
}
