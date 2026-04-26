package com.hoshi.reader.core.dictionary

import android.content.Context
import com.hoshi.reader.core.storage.JsonStore
import com.hoshi.reader.data.model.DictionaryConfig
import com.hoshi.reader.data.model.DictionaryEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictionaryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val baseDir: File = File(context.filesDir, "Dictionaries")
    private val termDir: File = File(baseDir, "Term")
    private val freqDir: File = File(baseDir, "Frequency")
    private val pitchDir: File = File(baseDir, "Pitch")

    private val configStore: JsonStore<DictionaryConfig> = JsonStore(
        file = File(baseDir, "config.json"),
        serializer = DictionaryConfig.serializer(),
        default = { DictionaryConfig() }
    )

    private val mutex = Mutex()
    private var loaded = false

    init {
        termDir.mkdirs()
        freqDir.mkdirs()
        pitchDir.mkdirs()
    }

    suspend fun importDictionary(zipPath: String): ImportResult = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "dict_import_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        try {
            val result = HoshiDicts.importDictionary(zipPath, tempDir.absolutePath)
            if (!result.success) return@withContext result

            val indexFile = File(tempDir, "index.json")
            val dictName = if (indexFile.exists()) {
                try {
                    val index = Json.decodeFromString<com.hoshi.reader.data.model.DictionaryIndex>(indexFile.readText())
                    index.title
                } catch (_: Exception) {
                    tempDir.name
                }
            } else {
                tempDir.name
            }

            val targetDir = detectDictType(tempDir, dictName)
            if (targetDir.exists()) targetDir.deleteRecursively()
            tempDir.copyRecursively(targetDir, overwrite = true)

            configStore.update { config ->
                val type = detectType(tempDir)
                when (type) {
                    DictType.TERM -> config.copy(
                        termOrder = config.termOrder + DictionaryEntry(dictName)
                    )
                    DictType.FREQUENCY -> config.copy(
                        frequencyOrder = config.frequencyOrder + DictionaryEntry(dictName)
                    )
                    DictType.PITCH -> config.copy(
                        pitchOrder = config.pitchOrder + DictionaryEntry(dictName)
                    )
                }
            }

            rebuildQueryInternal()
            result
        } finally {
            tempDir.deleteRecursively()
        }
    }

    suspend fun getConfig(): DictionaryConfig = configStore.read()

    suspend fun updateConfig(config: DictionaryConfig) {
        configStore.write(config)
        rebuildQueryInternal()
    }

    suspend fun deleteDictionary(name: String, type: DictType) {
        val dir = when (type) {
            DictType.TERM -> File(termDir, name)
            DictType.FREQUENCY -> File(freqDir, name)
            DictType.PITCH -> File(pitchDir, name)
        }
        dir.deleteRecursively()

        configStore.update { config ->
            when (type) {
                DictType.TERM -> config.copy(
                    termOrder = config.termOrder.filter { it.name != name }
                )
                DictType.FREQUENCY -> config.copy(
                    frequencyOrder = config.frequencyOrder.filter { it.name != name }
                )
                DictType.PITCH -> config.copy(
                    pitchOrder = config.pitchOrder.filter { it.name != name }
                )
            }
        }
        rebuildQueryInternal()
    }

    suspend fun ensureLoaded() = mutex.withLock {
        if (!loaded) {
            rebuildQueryInternal()
            loaded = true
        }
    }

    fun lookup(text: String, maxResults: Int = 16): Array<LookupResult> {
        return HoshiDicts.lookup(HoshiDicts.lookupObject, text, maxResults)
    }

    fun getStyles(): Array<DictionaryStyle> {
        return HoshiDicts.getStyles(HoshiDicts.lookupObject)
    }

    fun getMediaFile(dictName: String, mediaPath: String): ByteArray? {
        return HoshiDicts.getMediaFile(HoshiDicts.lookupObject, dictName, mediaPath)
    }

    private suspend fun rebuildQueryInternal() {
        val config = configStore.read()

        val termPaths = config.termOrder
            .filter { it.enabled }
            .map { File(termDir, it.name).absolutePath }
            .filter { File(it).exists() }
            .toTypedArray()

        val freqPaths = config.frequencyOrder
            .filter { it.enabled }
            .map { File(freqDir, it.name).absolutePath }
            .filter { File(it).exists() }
            .toTypedArray()

        val pitchPaths = config.pitchOrder
            .filter { it.enabled }
            .map { File(pitchDir, it.name).absolutePath }
            .filter { File(it).exists() }
            .toTypedArray()

        withContext(Dispatchers.IO) {
            HoshiDicts.rebuildQuery(HoshiDicts.lookupObject, termPaths, freqPaths, pitchPaths)
        }
    }

    private fun detectDictType(dir: File, name: String): File {
        val type = detectType(dir)
        return when (type) {
            DictType.TERM -> File(termDir, name)
            DictType.FREQUENCY -> File(freqDir, name)
            DictType.PITCH -> File(pitchDir, name)
        }
    }

    private fun detectType(dir: File): DictType {
        val indexFile = File(dir, "index.json")
        if (indexFile.exists()) {
            val text = indexFile.readText()
            if (text.contains("\"freq\"") || text.contains("frequency")) return DictType.FREQUENCY
            if (text.contains("\"pitch\"")) return DictType.PITCH
        }
        if (dir.listFiles()?.any { it.name.startsWith("term_meta_bank") } == true) {
            if (dir.listFiles()?.none { it.name.startsWith("term_bank") } == true) {
                return DictType.FREQUENCY
            }
        }
        return DictType.TERM
    }

    enum class DictType { TERM, FREQUENCY, PITCH }
}
