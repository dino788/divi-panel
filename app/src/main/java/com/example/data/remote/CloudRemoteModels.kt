package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CloudDevice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceMac: String = "",
    val deviceKey: String = "",
    val deviceName: String = "",
    val platform: String = "PHONE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudRemotePlaylist(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetMac: String = "ALL",
    val name: String = "",
    val playlistType: String = "XTREAM",
    val m3uUrl: String = "",
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val epgUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun matchesDevice(deviceMac: String): Boolean {
        val target = targetMac.trim()
        if (target.equals("ALL", ignoreCase = true)) return true
        return deviceMac.isNotBlank() && target.equals(deviceMac.trim(), ignoreCase = true)
    }
}
