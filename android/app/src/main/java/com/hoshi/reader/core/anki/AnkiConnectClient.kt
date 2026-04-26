package com.hoshi.reader.core.anki

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnkiConnectClient @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    var baseUrl: String = "http://127.0.0.1:8765"

    private suspend fun invoke(action: String, params: JsonObject? = null): JsonElement {
        val body = buildJsonObject {
            put("action", action)
            put("version", 6)
            if (params != null) {
                put("params", params)
            }
        }

        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(baseUrl)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            val result = json.parseToJsonElement(responseBody).jsonObject

            val error = result["error"]
            if (error != null && error !is JsonNull) {
                throw AnkiConnectException(error.jsonPrimitive.content)
            }

            result["result"] ?: JsonNull
        }
    }

    suspend fun isConnected(): Boolean {
        return try {
            invoke("version")
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deckNames(): List<String> {
        val result = invoke("deckNames")
        return result.jsonArray.map { it.jsonPrimitive.content }
    }

    suspend fun modelNames(): List<String> {
        val result = invoke("modelNames")
        return result.jsonArray.map { it.jsonPrimitive.content }
    }

    suspend fun modelFieldNames(modelName: String): List<String> {
        val params = buildJsonObject { put("modelName", modelName) }
        val result = invoke("modelFieldNames", params)
        return result.jsonArray.map { it.jsonPrimitive.content }
    }

    suspend fun addNote(
        deckName: String,
        modelName: String,
        fields: Map<String, String>,
        tags: List<String> = emptyList(),
        audio: List<AnkiMedia> = emptyList(),
        picture: List<AnkiMedia> = emptyList(),
        allowDuplicate: Boolean = false,
        duplicateScope: String = "collection"
    ): Long {
        val params = buildJsonObject {
            put("note", buildJsonObject {
                put("deckName", deckName)
                put("modelName", modelName)
                put("fields", JsonObject(fields.mapValues { JsonPrimitive(it.value) }))
                put("tags", JsonArray(tags.map { JsonPrimitive(it) }))

                if (audio.isNotEmpty()) {
                    put("audio", JsonArray(audio.map { it.toJson() }))
                }
                if (picture.isNotEmpty()) {
                    put("picture", JsonArray(picture.map { it.toJson() }))
                }

                if (allowDuplicate) {
                    put("options", buildJsonObject {
                        put("allowDuplicate", true)
                        put("duplicateScope", duplicateScope)
                    })
                }
            })
        }

        val result = invoke("addNote", params)
        return result.jsonPrimitive.content.toLong()
    }

    suspend fun canAddNotesWithErrorDetail(
        notes: List<NoteParams>
    ): List<CanAddResult> {
        val params = buildJsonObject {
            put("notes", JsonArray(notes.map { note ->
                buildJsonObject {
                    put("deckName", note.deckName)
                    put("modelName", note.modelName)
                    put("fields", JsonObject(note.fields.mapValues { JsonPrimitive(it.value) }))
                }
            }))
        }

        val result = invoke("canAddNotesWithErrorDetail", params)
        return result.jsonArray.map { item ->
            val obj = item.jsonObject
            CanAddResult(
                canAdd = obj["canAdd"]?.jsonPrimitive?.boolean ?: false,
                error = obj["error"]?.let { if (it is JsonNull) null else it.jsonPrimitive.content }
            )
        }
    }

    suspend fun findNotes(query: String): List<Long> {
        val params = buildJsonObject { put("query", query) }
        val result = invoke("findNotes", params)
        return result.jsonArray.map { it.jsonPrimitive.content.toLong() }
    }

    suspend fun storeMediaFile(filename: String, data: String): String {
        val params = buildJsonObject {
            put("filename", filename)
            put("data", data)
        }
        val result = invoke("storeMediaFile", params)
        return result.jsonPrimitive.content
    }
}

class AnkiConnectException(message: String) : Exception(message)

data class AnkiMedia(
    val url: String? = null,
    val data: String? = null,
    val filename: String,
    val fields: List<String> = emptyList()
) {
    fun toJson(): JsonObject = buildJsonObject {
        url?.let { put("url", it) }
        data?.let { put("data", it) }
        put("filename", filename)
        put("fields", JsonArray(fields.map { JsonPrimitive(it) }))
    }
}

data class NoteParams(
    val deckName: String,
    val modelName: String,
    val fields: Map<String, String>
)

data class CanAddResult(
    val canAdd: Boolean,
    val error: String?
)
