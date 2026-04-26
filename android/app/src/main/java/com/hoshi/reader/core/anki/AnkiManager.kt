package com.hoshi.reader.core.anki

import android.content.Context
import android.util.Base64
import com.hoshi.reader.core.storage.JsonStore
import com.hoshi.reader.data.model.AnkiConfig
import com.hoshi.reader.data.model.AnkiWord
import com.hoshi.reader.data.model.AnkiWordsData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnkiManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ankiConnect: AnkiConnectClient
) {
    private val configStore = JsonStore(
        file = File(context.filesDir, "anki_config.json"),
        serializer = AnkiConfig.serializer(),
        default = { AnkiConfig() }
    )

    private val savedWordsStore = JsonStore(
        file = File(context.filesDir, "anki_words.json"),
        serializer = AnkiWordsData.serializer(),
        default = { AnkiWordsData() }
    )

    private val httpClient = OkHttpClient()

    suspend fun getConfig(): AnkiConfig = configStore.read()

    suspend fun updateConfig(config: AnkiConfig) {
        configStore.write(config)
        ankiConnect.baseUrl = config.ankiConnectUrl
    }

    suspend fun isConnected(): Boolean {
        val config = configStore.read()
        ankiConnect.baseUrl = config.ankiConnectUrl
        return ankiConnect.isConnected()
    }

    suspend fun getDeckNames(): List<String> = ankiConnect.deckNames()

    suspend fun getModelNames(): List<String> = ankiConnect.modelNames()

    suspend fun getModelFieldNames(modelName: String): List<String> =
        ankiConnect.modelFieldNames(modelName)

    suspend fun isDuplicate(expression: String): Boolean {
        val saved = savedWordsStore.read()
        if (saved.words.any { it.expression == expression }) return true

        return try {
            val notes = ankiConnect.findNotes("\"$expression\"")
            notes.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun addNote(
        variables: Map<String, String>,
        dictionaryMedia: List<DictionaryMediaItem> = emptyList()
    ): Boolean {
        val config = configStore.read()
        ankiConnect.baseUrl = config.ankiConnectUrl

        if (config.deck.isEmpty() || config.noteType.isEmpty()) return false

        val fields = mutableMapOf<String, String>()
        var audioField: String? = null
        var audioUrl: String? = null

        for ((fieldName, template) in config.fieldMappings) {
            if (template == "{{audio}}") {
                audioField = fieldName
                audioUrl = variables["audio"]
                fields[fieldName] = ""
            } else {
                fields[fieldName] = Handlebars.render(template, variables)
            }
        }

        for (media in dictionaryMedia) {
            try {
                val data = media.data ?: continue
                val hash = sha1(data)
                val filename = "hibiki_dict_$hash.${media.extension}"
                ankiConnect.storeMediaFile(filename, Base64.encodeToString(data, Base64.NO_WRAP))

                for ((key, value) in fields) {
                    if (value.contains(media.placeholderFilename)) {
                        fields[key] = value.replace(media.placeholderFilename, filename)
                    }
                }
            } catch (_: Exception) {}
        }

        val audioList = mutableListOf<AnkiMedia>()
        if (audioField != null && !audioUrl.isNullOrEmpty()) {
            try {
                val audioData = downloadUrl(audioUrl)
                if (audioData != null) {
                    val hash = sha1(audioData)
                    val filename = "hibiki_audio_$hash.mp3"
                    audioList.add(AnkiMedia(
                        data = Base64.encodeToString(audioData, Base64.NO_WRAP),
                        filename = filename,
                        fields = listOf(audioField)
                    ))
                }
            } catch (_: Exception) {
                if (audioUrl.startsWith("http")) {
                    audioList.add(AnkiMedia(
                        url = audioUrl,
                        filename = "hibiki_audio_${audioUrl.hashCode()}.mp3",
                        fields = listOf(audioField)
                    ))
                }
            }
        }

        val noteId = ankiConnect.addNote(
            deckName = config.deck,
            modelName = config.noteType,
            fields = fields,
            tags = config.tags,
            audio = audioList,
            allowDuplicate = config.allowDuplicates,
            duplicateScope = config.duplicateScope
        )

        val expression = variables["expression"] ?: ""
        if (expression.isNotEmpty()) {
            savedWordsStore.update { data ->
                data.copy(words = data.words + AnkiWord(expression))
            }
        }

        return noteId > 0
    }

    private suspend fun downloadUrl(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            response.body?.bytes()
        } catch (_: Exception) {
            null
        }
    }

    private fun sha1(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1")
        return digest.digest(data).joinToString("") { "%02x".format(it) }.take(12)
    }
}

data class DictionaryMediaItem(
    val dictionary: String,
    val path: String,
    val placeholderFilename: String,
    val extension: String = path.substringAfterLast('.', "bin"),
    val data: ByteArray? = null
)
