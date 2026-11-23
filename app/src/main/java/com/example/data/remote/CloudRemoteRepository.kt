package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudRemoteRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUserId != null && uid != expectedUserId) {
            throw IllegalStateException("Signed-in user changed; refusing cross-user write.")
        }
        return uid
    }

    fun observeDevices(userId: String): Flow<List<CloudDevice>> {
        val path = "users/$userId/devices"
        return db.collection("users")
            .document(userId)
            .collection("devices")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudDevice::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeRemotePlaylists(userId: String): Flow<List<CloudRemotePlaylist>> {
        val path = "users/$userId/remote_playlists"
        return db.collection("users")
            .document(userId)
            .collection("remote_playlists")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudRemotePlaylist::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun registerOrHeartbeatDevice(
        userId: String,
        deviceMac: String,
        deviceKey: String,
        deviceName: String,
        isTv: Boolean
    ): Result<Unit> {
        val sanitizedId = deviceMac.replace(":", "").replace(" ", "").lowercase()
            .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .take(64)
        val docPath = "users/$userId/devices/$sanitizedId"
        return try {
            val uid = requireUserId(userId)
            val cleanMac = deviceMac.trim().take(32)
            val cleanKey = deviceKey.trim().take(16)
            val cleanName = deviceName.trim().ifBlank { if (isTv) "Android TV" else "Android Phone" }.take(80)
            require(sanitizedId.isNotBlank() && cleanMac.length >= 5 && cleanKey.length >= 4) {
                "Invalid device identity"
            }

            val platform = if (isTv) "TV" else "PHONE"
            val docRef = db.collection("users").document(uid)
                .collection("devices").document(sanitizedId)

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "deviceMac" to cleanMac,
                        "deviceKey" to cleanKey,
                        "deviceName" to cleanName,
                        "platform" to platform,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "deviceMac" to cleanMac,
                        "deviceKey" to cleanKey,
                        "deviceName" to cleanName,
                        "platform" to platform,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushRemoteXtreamPlaylist(
        userId: String,
        targetMac: String,
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Result<Unit> {
        var docPath = "users/$userId/remote_playlists"
        return try {
            val uid = requireUserId(userId)
            val cleanTarget = targetMac.trim().ifBlank { "ALL" }.take(32)
            val cleanName = name.trim().ifBlank { "Remote Xtream" }.take(100)
            val cleanServer = serverUrl.trim().take(512)
            val cleanUser = username.trim().take(128)
            val cleanPass = password.trim().take(128)
            require(cleanTarget.length >= 3) { "Target MAC is invalid" }
            require(cleanServer.isNotBlank() && cleanUser.isNotBlank() && cleanPass.isNotBlank()) {
                "Server URL, username, and password are required"
            }

            val docRef = db.collection("users").document(uid)
                .collection("remote_playlists").document()
            docPath = docRef.path

            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "targetMac" to cleanTarget,
                "name" to cleanName,
                "playlistType" to "XTREAM",
                "serverUrl" to cleanServer,
                "username" to cleanUser,
                "password" to cleanPass,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushRemoteM3uPlaylist(
        userId: String,
        targetMac: String,
        name: String,
        m3uUrl: String,
        epgUrl: String
    ): Result<Unit> {
        var docPath = "users/$userId/remote_playlists"
        return try {
            val uid = requireUserId(userId)
            val cleanTarget = targetMac.trim().ifBlank { "ALL" }.take(32)
            val cleanName = name.trim().ifBlank { "Remote M3U" }.take(100)
            val cleanM3u = m3uUrl.trim().take(2048)
            val cleanEpg = epgUrl.trim().take(2048).takeIf { it.isNotBlank() }
            require(cleanTarget.length >= 3) { "Target MAC is invalid" }
            require(cleanM3u.isNotBlank()) { "M3U URL is required" }

            val docRef = db.collection("users").document(uid)
                .collection("remote_playlists").document()
            docPath = docRef.path

            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "targetMac" to cleanTarget,
                "name" to cleanName,
                "playlistType" to "M3U",
                "m3uUrl" to cleanM3u,
                "epgUrl" to cleanEpg,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteRemotePlaylist(userId: String, playlistId: String): Result<Unit> {
        val docPath = "users/$userId/remote_playlists/$playlistId"
        return try {
            val uid = requireUserId(userId)
            require(playlistId.isNotBlank()) { "Playlist ID is required" }
            db.collection("users").document(uid)
                .collection("remote_playlists").document(playlistId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeDevice(userId: String, deviceId: String): Result<Unit> {
        val docPath = "users/$userId/devices/$deviceId"
        return try {
            val uid = requireUserId(userId)
            require(deviceId.isNotBlank()) { "Device ID is required" }
            db.collection("users").document(uid)
                .collection("devices").document(deviceId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
