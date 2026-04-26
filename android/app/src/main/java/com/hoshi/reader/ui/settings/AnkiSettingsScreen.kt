package com.hoshi.reader.ui.settings

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnkiSettingsScreen(
    onBack: () -> Unit,
    viewModel: AnkiSettingsViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()
    val connected by viewModel.connected.collectAsState()
    val decks by viewModel.decks.collectAsState()
    val models by viewModel.models.collectAsState()
    val fields by viewModel.fields.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anki Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.checkConnection() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                .padding(horizontal = 16.dp)
        ) {
            item {
                SectionTitle("Connection")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (connected) "Connected" else "Disconnected",
                        color = if (connected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(8.dp))

                var url by remember(config.ankiConnectUrl) { mutableStateOf(config.ankiConnectUrl) }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; viewModel.updateUrl(it) },
                    label = { Text("AnkiConnect URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                SectionTitle("Deck & Note Type")

                DropdownSelector(
                    label = "Deck",
                    selected = config.deck,
                    options = decks,
                    onSelect = { viewModel.selectDeck(it) }
                )
                Spacer(Modifier.height(8.dp))

                DropdownSelector(
                    label = "Note Type",
                    selected = config.noteType,
                    options = models,
                    onSelect = { viewModel.selectModel(it) }
                )
                Spacer(Modifier.height(16.dp))
            }

            if (fields.isNotEmpty()) {
                item {
                    SectionTitle("Field Mappings")
                    Text(
                        text = "Available: {{expression}}, {{reading}}, {{glossary}}, {{audio}}, {{sentence}}, {{frequencies}}, {{pitch-accent-positions}}, etc.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(fields) { fieldName ->
                    var template by remember(fieldName, config.fieldMappings) {
                        mutableStateOf(config.fieldMappings[fieldName] ?: "")
                    }
                    OutlinedTextField(
                        value = template,
                        onValueChange = {
                            template = it
                            viewModel.updateFieldMapping(fieldName, it)
                        },
                        label = { Text(fieldName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        singleLine = true
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }
            }

            item {
                SectionTitle("Tags")
                var tagsText by remember(config.tags) { mutableStateOf(config.tags.joinToString(", ")) }
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = {
                        tagsText = it
                        viewModel.updateTags(it)
                    },
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.ifEmpty { "Select $label" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
