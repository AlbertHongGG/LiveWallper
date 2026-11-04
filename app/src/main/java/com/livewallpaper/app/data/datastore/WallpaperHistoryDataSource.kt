package com.livewallpaper.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.livewallpaper.app.domain.model.WallpaperHistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

val Context.wallpaperHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "wallpaper_history")

class WallpaperHistoryDataSource(
    private val context: Context
) {
    companion object {
        private val KEY_HISTORY_JSON = stringPreferencesKey("history_items_json")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    val historyFlow: Flow<List<WallpaperHistoryItem>> = context.wallpaperHistoryDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val jsonString = prefs[KEY_HISTORY_JSON].orEmpty()
            if (jsonString.isBlank()) {
                emptyList()
            } else {
                try {
                    json.decodeFromString<List<WallpaperHistoryItem>>(jsonString)
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

    suspend fun getHistoryItems(): List<WallpaperHistoryItem> {
        return historyFlow.first()
    }

    suspend fun saveHistoryItems(items: List<WallpaperHistoryItem>) {
        val jsonString = json.encodeToString(items)
        context.wallpaperHistoryDataStore.edit { prefs ->
            prefs[KEY_HISTORY_JSON] = jsonString
        }
    }
}
