package com.hoshi.reader.ui.dictionary

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshi.reader.core.dictionary.DictionaryManager
import com.hoshi.reader.data.model.DictionaryConfig
import com.hoshi.reader.data.model.DictionaryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DictionaryViewModel @Inject constructor(
    private val dictionaryManager: DictionaryManager
) : ViewModel() {

    private val _config = MutableStateFlow(DictionaryConfig())
    val config: StateFlow<DictionaryConfig> = _config

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _config.value = dictionaryManager.getConfig()
        }
    }

    fun importDictionary(zipPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _importing.value = true
            try {
                val result = dictionaryManager.importDictionary(zipPath)
                _importMessage.value = if (result.success) {
                    "Imported: ${result.termCount} terms, ${result.metaCount} meta, ${result.mediaCount} media"
                } else {
                    "Import failed"
                }
                loadConfig()
            } catch (e: Exception) {
                _importMessage.value = "Error: ${e.message}"
            } finally {
                _importing.value = false
            }
        }
    }

    fun toggleDictionary(name: String, type: DictionaryManager.DictType, enabled: Boolean) {
        viewModelScope.launch {
            val current = _config.value
            val updated = when (type) {
                DictionaryManager.DictType.TERM -> current.copy(
                    termOrder = current.termOrder.map { if (it.name == name) it.copy(enabled = enabled) else it }
                )
                DictionaryManager.DictType.FREQUENCY -> current.copy(
                    frequencyOrder = current.frequencyOrder.map { if (it.name == name) it.copy(enabled = enabled) else it }
                )
                DictionaryManager.DictType.PITCH -> current.copy(
                    pitchOrder = current.pitchOrder.map { if (it.name == name) it.copy(enabled = enabled) else it }
                )
            }
            dictionaryManager.updateConfig(updated)
            _config.value = updated
        }
    }

    fun deleteDictionary(name: String, type: DictionaryManager.DictType) {
        viewModelScope.launch {
            dictionaryManager.deleteDictionary(name, type)
            loadConfig()
        }
    }

    fun clearMessage() {
        _importMessage.value = null
    }
}
