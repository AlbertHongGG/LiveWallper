package com.livewallpaper.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.livewallpaper.app.domain.model.ScalingMode
import com.livewallpaper.app.domain.model.WallpaperConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.wallpaperDataStore: DataStore<Preferences> by preferencesDataStore(name = "wallpaper_settings")

class WallpaperPreferencesDataSource(
    private val context: Context
) {
    companion object {
        private val KEY_VIDEO_FILE_PATH = stringPreferencesKey("video_file_path")
        private val KEY_SCALING_MODE = stringPreferencesKey("scaling_mode")
        private val KEY_ZOOM_SCALE = floatPreferencesKey("zoom_scale")
        private val KEY_PAN_OFFSET_X = floatPreferencesKey("pan_offset_x")
        private val KEY_PAN_OFFSET_Y = floatPreferencesKey("pan_offset_y")
        private val KEY_DIM_ALPHA = floatPreferencesKey("dim_alpha")
        private val KEY_IS_MUTED = booleanPreferencesKey("is_muted")
        private val KEY_VOLUME = floatPreferencesKey("volume")
        private val KEY_PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        private val KEY_ENABLE_TRIM = booleanPreferencesKey("enable_trim")
        private val KEY_TRIM_START_MS = longPreferencesKey("trim_start_ms")
        private val KEY_TRIM_END_MS = longPreferencesKey("trim_end_ms")
        private val KEY_DOUBLE_TAP_TO_PAUSE = booleanPreferencesKey("double_tap_to_pause")
        private val KEY_POWER_SAVE_DELAY_SECONDS = intPreferencesKey("power_save_delay_seconds")
        private val KEY_UPDATED_AT = longPreferencesKey("updated_at")
    }

    val configFlow: Flow<WallpaperConfig> = context.wallpaperDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val path = prefs[KEY_VIDEO_FILE_PATH].orEmpty()
            val scalingModeStr = prefs[KEY_SCALING_MODE] ?: ScalingMode.FILL.name
            val scalingMode = try {
                ScalingMode.valueOf(scalingModeStr)
            } catch (_: Exception) {
                ScalingMode.FILL
            }
            val zoom = prefs[KEY_ZOOM_SCALE] ?: 1.0f
            val panX = prefs[KEY_PAN_OFFSET_X] ?: 0f
            val panY = prefs[KEY_PAN_OFFSET_Y] ?: 0f
            val dim = prefs[KEY_DIM_ALPHA] ?: 0f
            val isMuted = prefs[KEY_IS_MUTED] ?: true
            val volume = prefs[KEY_VOLUME] ?: 0.5f
            val speed = prefs[KEY_PLAYBACK_SPEED] ?: 1.0f
            val enableTrim = prefs[KEY_ENABLE_TRIM] ?: false
            val trimStart = prefs[KEY_TRIM_START_MS] ?: 0L
            val trimEnd = prefs[KEY_TRIM_END_MS] ?: 0L
            val doubleTap = prefs[KEY_DOUBLE_TAP_TO_PAUSE] ?: true
            val powerSave = prefs[KEY_POWER_SAVE_DELAY_SECONDS] ?: 3
            val updatedAt = prefs[KEY_UPDATED_AT] ?: 0L

            WallpaperConfig(
                videoFilePath = path,
                scalingMode = scalingMode,
                zoomScale = zoom,
                panOffsetX = panX,
                panOffsetY = panY,
                dimAlpha = dim,
                isMuted = isMuted,
                volume = volume,
                playbackSpeed = speed,
                enableTrim = enableTrim,
                trimStartMs = trimStart,
                trimEndMs = trimEnd,
                doubleTapToPause = doubleTap,
                powerSaveDelaySeconds = powerSave,
                updatedAtMillis = updatedAt
            )
        }

    suspend fun getCurrentConfig(): WallpaperConfig {
        return configFlow.first()
    }

    suspend fun updateConfig(transform: (WallpaperConfig) -> WallpaperConfig) {
        val current = getCurrentConfig()
        val updated = transform(current)
        saveConfig(updated)
    }

    suspend fun saveConfig(config: WallpaperConfig) {
        context.wallpaperDataStore.edit { prefs ->
            prefs[KEY_VIDEO_FILE_PATH] = config.videoFilePath
            prefs[KEY_SCALING_MODE] = config.scalingMode.name
            prefs[KEY_ZOOM_SCALE] = config.zoomScale
            prefs[KEY_PAN_OFFSET_X] = config.panOffsetX
            prefs[KEY_PAN_OFFSET_Y] = config.panOffsetY
            prefs[KEY_DIM_ALPHA] = config.dimAlpha
            prefs[KEY_IS_MUTED] = config.isMuted
            prefs[KEY_VOLUME] = config.volume
            prefs[KEY_PLAYBACK_SPEED] = config.playbackSpeed
            prefs[KEY_ENABLE_TRIM] = config.enableTrim
            prefs[KEY_TRIM_START_MS] = config.trimStartMs
            prefs[KEY_TRIM_END_MS] = config.trimEndMs
            prefs[KEY_DOUBLE_TAP_TO_PAUSE] = config.doubleTapToPause
            prefs[KEY_POWER_SAVE_DELAY_SECONDS] = config.powerSaveDelaySeconds
            prefs[KEY_UPDATED_AT] = if (config.updatedAtMillis > 0L) config.updatedAtMillis else System.currentTimeMillis()
        }
    }
}
