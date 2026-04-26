package com.hoshi.reader.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_config")

enum class ReaderTheme { LIGHT, DARK, SEPIA, CUSTOM }
enum class SortOption { RECENT, TITLE }

@Singleton
class UserConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val FONT_SIZE = intPreferencesKey("font_size")
        val LINE_HEIGHT = doublePreferencesKey("line_height")
        val CHAR_SPACING = doublePreferencesKey("char_spacing")
        val VERTICAL_WRITING = booleanPreferencesKey("vertical_writing")
        val THEME = stringPreferencesKey("theme")
        val SELECTED_FONT = stringPreferencesKey("selected_font")
        val CUSTOM_CSS = stringPreferencesKey("custom_css")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val SHOW_TITLE = booleanPreferencesKey("show_title")
        val TEXT_JUSTIFY = booleanPreferencesKey("text_justify")
        val PAGE_BREAK_AVOID = booleanPreferencesKey("page_break_avoid")
    }

    val fontSize: Flow<Int> = dataStore.data.map { it[FONT_SIZE] ?: 18 }
    val lineHeight: Flow<Double> = dataStore.data.map { it[LINE_HEIGHT] ?: 1.8 }
    val charSpacing: Flow<Double> = dataStore.data.map { it[CHAR_SPACING] ?: 0.0 }
    val verticalWriting: Flow<Boolean> = dataStore.data.map { it[VERTICAL_WRITING] ?: true }
    val theme: Flow<ReaderTheme> = dataStore.data.map {
        try { ReaderTheme.valueOf(it[THEME] ?: "LIGHT") } catch (_: Exception) { ReaderTheme.LIGHT }
    }
    val selectedFont: Flow<String> = dataStore.data.map { it[SELECTED_FONT] ?: "" }
    val customCss: Flow<String> = dataStore.data.map { it[CUSTOM_CSS] ?: "" }
    val sortOption: Flow<SortOption> = dataStore.data.map {
        try { SortOption.valueOf(it[SORT_OPTION] ?: "RECENT") } catch (_: Exception) { SortOption.RECENT }
    }
    val showTitle: Flow<Boolean> = dataStore.data.map { it[SHOW_TITLE] ?: true }
    val textJustify: Flow<Boolean> = dataStore.data.map { it[TEXT_JUSTIFY] ?: false }

    suspend fun setFontSize(size: Int) = dataStore.edit { it[FONT_SIZE] = size }
    suspend fun setLineHeight(height: Double) = dataStore.edit { it[LINE_HEIGHT] = height }
    suspend fun setCharSpacing(spacing: Double) = dataStore.edit { it[CHAR_SPACING] = spacing }
    suspend fun setVerticalWriting(vertical: Boolean) = dataStore.edit { it[VERTICAL_WRITING] = vertical }
    suspend fun setTheme(theme: ReaderTheme) = dataStore.edit { it[THEME] = theme.name }
    suspend fun setSelectedFont(font: String) = dataStore.edit { it[SELECTED_FONT] = font }
    suspend fun setCustomCss(css: String) = dataStore.edit { it[CUSTOM_CSS] = css }
    suspend fun setSortOption(option: SortOption) = dataStore.edit { it[SORT_OPTION] = option.name }
    suspend fun setShowTitle(show: Boolean) = dataStore.edit { it[SHOW_TITLE] = show }
    suspend fun setTextJustify(justify: Boolean) = dataStore.edit { it[TEXT_JUSTIFY] = justify }
}
