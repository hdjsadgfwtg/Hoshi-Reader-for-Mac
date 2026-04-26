package com.hoshi.reader

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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userConfig: UserConfig
    @Inject lateinit var backupManager: BackupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HoshiReaderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph(
                        userConfig = userConfig,
                        backupManager = backupManager
                    )
                }
            }
        }
    }
}
