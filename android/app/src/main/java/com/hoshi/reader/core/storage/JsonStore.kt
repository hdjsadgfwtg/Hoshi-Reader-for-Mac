package com.hoshi.reader.core.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

class JsonStore<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
    private val default: () -> T
) {
    private val mutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun read(): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!file.exists()) return@withContext default()
            try {
                json.decodeFromString(serializer, file.readText())
            } catch (_: Exception) {
                default()
            }
        }
    }

    suspend fun write(value: T) = withContext(Dispatchers.IO) {
        mutex.withLock {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(json.encodeToString(serializer, value))
            tmp.renameTo(file)
        }
    }

    suspend fun update(transform: (T) -> T) {
        val current = read()
        write(transform(current))
    }

    fun readBlocking(): T {
        if (!file.exists()) return default()
        return try {
            json.decodeFromString(serializer, file.readText())
        } catch (_: Exception) {
            default()
        }
    }
}
