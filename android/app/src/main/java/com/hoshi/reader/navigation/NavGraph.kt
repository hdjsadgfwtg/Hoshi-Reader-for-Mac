package com.hoshi.reader.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hoshi.reader.core.storage.BackupManager
import com.hoshi.reader.core.storage.UserConfig
import com.hoshi.reader.ui.bookshelf.BookshelfScreen
import com.hoshi.reader.ui.dictionary.DictionaryScreen
import com.hoshi.reader.ui.reader.ReaderScreen
import com.hoshi.reader.ui.settings.AnkiSettingsScreen
import com.hoshi.reader.ui.settings.AppearanceScreen
import com.hoshi.reader.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.StateFlow

object Routes {
    const val BOOKSHELF = "bookshelf"
    const val READER = "reader/{bookId}"
    const val SETTINGS = "settings"
    const val DICTIONARIES = "dictionaries"
    const val ANKI_SETTINGS = "anki_settings"
    const val APPEARANCE = "appearance"

    fun reader(bookId: String) = "reader/$bookId"
}

@Composable
fun NavGraph(
    userConfig: UserConfig,
    backupManager: BackupManager,
    pendingImportUri: StateFlow<Uri?>? = null,
    onImportUriConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val importUri = pendingImportUri?.collectAsState()?.value

    NavHost(
        navController = navController,
        startDestination = Routes.BOOKSHELF
    ) {
        composable(Routes.BOOKSHELF) {
            BookshelfScreen(
                onBookClick = { bookId ->
                    navController.navigate(Routes.reader(bookId))
                },
                onAnkiSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
                pendingImportUri = importUri,
                onImportUriConsumed = onImportUriConsumed
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onDictionaries = { navController.navigate(Routes.DICTIONARIES) },
                onAnki = { navController.navigate(Routes.ANKI_SETTINGS) },
                onAppearance = { navController.navigate(Routes.APPEARANCE) },
                backupManager = backupManager
            )
        }

        composable(Routes.DICTIONARIES) {
            DictionaryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ANKI_SETTINGS) {
            AnkiSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.APPEARANCE) {
            AppearanceScreen(
                userConfig = userConfig,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
            ReaderScreen(
                bookId = bookId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
