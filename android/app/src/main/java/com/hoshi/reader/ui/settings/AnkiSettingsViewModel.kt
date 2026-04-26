package com.hoshi.reader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshi.reader.core.anki.AnkiManager
import com.hoshi.reader.data.model.AnkiConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnkiSettingsViewModel @Inject constructor(
    private val ankiManager: AnkiManager
) : ViewModel() {

    private val _config = MutableStateFlow(AnkiConfig())
    val config: StateFlow<AnkiConfig> = _config

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private val _decks = MutableStateFlow<List<String>>(emptyList())
    val decks: StateFlow<List<String>> = _decks

    private val _models = MutableStateFlow<List<String>>(emptyList())
    val models: StateFlow<List<String>> = _models

    private val _fields = MutableStateFlow<List<String>>(emptyList())
    val fields: StateFlow<List<String>> = _fields

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        viewModelScope.launch {
            _config.value = ankiManager.getConfig()
            checkConnection()
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            try {
                _connected.value = ankiManager.isConnected()
                if (_connected.value) {
                    _decks.value = ankiManager.getDeckNames()
                    _models.value = ankiManager.getModelNames()
                    val cfg = _config.value
                    if (cfg.noteType.isNotEmpty()) {
                        _fields.value = ankiManager.getModelFieldNames(cfg.noteType)
                    }
                }
            } catch (e: Exception) {
                _connected.value = false
                _message.value = "Connection failed: ${e.message}"
            }
        }
    }

    fun updateUrl(url: String) {
        viewModelScope.launch {
            val cfg = _config.value.copy(ankiConnectUrl = url)
            _config.value = cfg
            ankiManager.updateConfig(cfg)
            checkConnection()
        }
    }

    fun selectDeck(deck: String) {
        viewModelScope.launch {
            val cfg = _config.value.copy(deck = deck)
            _config.value = cfg
            ankiManager.updateConfig(cfg)
        }
    }

    fun selectModel(model: String) {
        viewModelScope.launch {
            val cfg = _config.value.copy(noteType = model)
            _config.value = cfg
            ankiManager.updateConfig(cfg)
            try {
                _fields.value = ankiManager.getModelFieldNames(model)
            } catch (_: Exception) {}
        }
    }

    fun updateFieldMapping(fieldName: String, template: String) {
        viewModelScope.launch {
            val mappings = _config.value.fieldMappings.toMutableMap()
            if (template.isEmpty()) {
                mappings.remove(fieldName)
            } else {
                mappings[fieldName] = template
            }
            val cfg = _config.value.copy(fieldMappings = mappings)
            _config.value = cfg
            ankiManager.updateConfig(cfg)
        }
    }

    fun updateTags(tagsString: String) {
        viewModelScope.launch {
            val tags = tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val cfg = _config.value.copy(tags = tags)
            _config.value = cfg
            ankiManager.updateConfig(cfg)
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
