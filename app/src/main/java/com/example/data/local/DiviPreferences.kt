package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random

private val Context.diviDataStore: DataStore<Preferences> by preferencesDataStore(name = "divi_settings")

data class PlayerSettings(
    val streamFormat: StreamFormat = StreamFormat.HLS,
    val bufferProfile: BufferProfile = BufferProfile.BALANCED,
    val defaultAspectRatio: AspectRatioMode = AspectRatioMode.FIT,
    val userAgent: String = DEFAULT_USER_AGENT,
    val subtitleScale: Float = 1.0f,
    val autoPlayLivePreview: Boolean = true,
    val parentalPin: String = "",
    val deviceMac: String = "",
    val deviceKey: String = ""
) {
    companion object {
        const val DEFAULT_USER_AGENT = "IPTVSmartersPro/3.1 (Linux; Android 13)"
    }
}

class DiviPreferences(private val context: Context) {

    private object Keys {
        val STREAM_FORMAT = stringPreferencesKey("stream_format")
        val BUFFER_PROFILE = stringPreferencesKey("buffer_profile")
        val ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
        val USER_AGENT = stringPreferencesKey("user_agent")
        val SUBTITLE_SCALE = floatPreferencesKey("subtitle_scale")
        val AUTO_PLAY_PREVIEW = booleanPreferencesKey("auto_play_preview")
        val PARENTAL_PIN = stringPreferencesKey("parental_pin")
        val DEVICE_MAC = stringPreferencesKey("device_mac")
        val DEVICE_KEY = stringPreferencesKey("device_key")
    }

    val settingsFlow: Flow<PlayerSettings> = context.diviDataStore.data.map { prefs ->
        PlayerSettings(
            streamFormat = prefs[Keys.STREAM_FORMAT]?.let { name ->
                StreamFormat.entries.firstOrNull { it.name == name }
            } ?: StreamFormat.HLS,
            bufferProfile = prefs[Keys.BUFFER_PROFILE]?.let { name ->
                BufferProfile.entries.firstOrNull { it.name == name }
            } ?: BufferProfile.BALANCED,
            defaultAspectRatio = prefs[Keys.ASPECT_RATIO]?.let { name ->
                AspectRatioMode.entries.firstOrNull { it.name == name }
            } ?: AspectRatioMode.FIT,
            userAgent = prefs[Keys.USER_AGENT]?.takeIf { it.isNotBlank() }
                ?: PlayerSettings.DEFAULT_USER_AGENT,
            subtitleScale = prefs[Keys.SUBTITLE_SCALE] ?: 1.0f,
            autoPlayLivePreview = prefs[Keys.AUTO_PLAY_PREVIEW] ?: true,
            parentalPin = prefs[Keys.PARENTAL_PIN] ?: "",
            deviceMac = prefs[Keys.DEVICE_MAC] ?: "",
            deviceKey = prefs[Keys.DEVICE_KEY] ?: ""
        )
    }

    suspend fun ensureDeviceIdentity() {
        val prefs = context.diviDataStore.data.first()
        if (prefs[Keys.DEVICE_MAC].isNullOrBlank() || prefs[Keys.DEVICE_KEY].isNullOrBlank()) {
            val mac = generateVirtualMac()
            val key = (100000..999999).random().toString()
            context.diviDataStore.edit { edit ->
                if (edit[Keys.DEVICE_MAC].isNullOrBlank()) edit[Keys.DEVICE_MAC] = mac
                if (edit[Keys.DEVICE_KEY].isNullOrBlank()) edit[Keys.DEVICE_KEY] = key
            }
        }
    }

    suspend fun setStreamFormat(format: StreamFormat) {
        context.diviDataStore.edit { it[Keys.STREAM_FORMAT] = format.name }
    }

    suspend fun setBufferProfile(profile: BufferProfile) {
        context.diviDataStore.edit { it[Keys.BUFFER_PROFILE] = profile.name }
    }

    suspend fun setDefaultAspectRatio(mode: AspectRatioMode) {
        context.diviDataStore.edit { it[Keys.ASPECT_RATIO] = mode.name }
    }

    suspend fun setUserAgent(userAgent: String) {
        context.diviDataStore.edit {
            it[Keys.USER_AGENT] = userAgent.trim().ifBlank { PlayerSettings.DEFAULT_USER_AGENT }
        }
    }

    suspend fun setSubtitleScale(scale: Float) {
        context.diviDataStore.edit { it[Keys.SUBTITLE_SCALE] = scale.coerceIn(0.75f, 1.5f) }
    }

    suspend fun setAutoPlayPreview(enabled: Boolean) {
        context.diviDataStore.edit { it[Keys.AUTO_PLAY_PREVIEW] = enabled }
    }

    suspend fun setParentalPin(pin: String) {
        context.diviDataStore.edit { it[Keys.PARENTAL_PIN] = pin.trim() }
    }

    private fun generateVirtualMac(): String {
        val bytes = ByteArray(6) { Random.nextInt(0, 256).toByte() }
        // Locally administered unicast MAC prefix (02:xx:...)
        bytes[0] = ((bytes[0].toInt() and 0xFE) or 0x02).toByte()
        return bytes.joinToString(":") { b -> "%02x".format(b.toInt() and 0xFF) }
    }
}
