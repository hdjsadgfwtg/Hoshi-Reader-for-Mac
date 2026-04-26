package com.hoshi.reader.ui.popup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshi.reader.core.dictionary.DictionaryManager
import com.hoshi.reader.core.dictionary.LookupResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PopupViewModel @Inject constructor(
    private val dictionaryManager: DictionaryManager
) : ViewModel() {

    private val _lookupResults = MutableStateFlow<Array<LookupResult>>(emptyArray())
    val lookupResults: StateFlow<Array<LookupResult>> = _lookupResults

    private val _styles = MutableStateFlow<String>("")
    val styles: StateFlow<String> = _styles

    fun lookup(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dictionaryManager.ensureLoaded()
            val results = dictionaryManager.lookup(text)
            _lookupResults.value = results

            val dictStyles = dictionaryManager.getStyles()
            val stylesJson = buildString {
                append("{")
                dictStyles.forEachIndexed { i, style ->
                    if (i > 0) append(",")
                    append("\"")
                    append(style.dictName.replace("\"", "\\\""))
                    append("\":\"")
                    append(style.styles.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"))
                    append("\"")
                }
                append("}")
            }
            _styles.value = stylesJson
        }
    }

    fun getMediaFile(dictName: String, mediaPath: String): ByteArray? {
        return dictionaryManager.getMediaFile(dictName, mediaPath)
    }
}
