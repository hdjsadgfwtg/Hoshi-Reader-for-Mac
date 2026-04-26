package com.hoshi.reader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hoshi.reader.core.storage.BackupManager
import com.hoshi.reader.core.storage.UserConfig
import com.hoshi.reader.navigation.NavGraph
import com.hoshi.reader.ui.theme.HoshiReaderTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userConfig: UserConfig
    @Inject lateinit var backupManager: BackupManager

    private val _pendingImportUri = MutableStateFlow<Uri?>(null)
    val pendingImportUri: StateFlow<Uri?> = _pendingImportUri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            HoshiReaderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph(
                        userConfig = userConfig,
                        backupManager = backupManager,
                        pendingImportUri = pendingImportUri,
                        onImportUriConsumed = { consumeImportUri() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    _pendingImportUri.value = uri
                }
            }
            "com.hoshi.reader.IMPORT_FILE" -> {
                intent.getStringExtra("path")?.let { path ->
                    _pendingImportUri.value = Uri.fromFile(java.io.File(path))
                }
            }
        }
    }

    fun consumeImportUri() {
        _pendingImportUri.value = null
    }
}
