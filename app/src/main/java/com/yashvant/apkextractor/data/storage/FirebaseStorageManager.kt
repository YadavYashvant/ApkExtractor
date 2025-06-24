package com.yashvant.apkextractor.data.storage

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageManager @Inject constructor(
    private val authManager: FirebaseAuthManager
) : CloudStorage {
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun uploadFile(file: File, fileName: String): Result<String> = runCatching {
        val userId = authManager.getCurrentUser()?.uid ?: throw IllegalStateException("User not authenticated")

        // First check if the user is properly authenticated
        if (!isUserAuthenticated()) {
            throw SecurityException("User authentication required")
        }

        val storageRef = storage.reference.child("apks/$userId/$fileName")

        try {
            // Upload the file
            val uploadTask = storageRef.putFile(Uri.fromFile(file)).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Save metadata to Firestore
            val metadata = hashMapOf(
                "fileName" to fileName,
                "downloadUrl" to downloadUrl,
                "uploadTime" to System.currentTimeMillis(),
                "size" to file.length()
            )

            firestore.collection("users")
                .document(userId)
                .collection("apks")
                .document(fileName)
                .set(metadata)
                .await()

            downloadUrl
        } catch (e: Exception) {
            throw SecurityException("Failed to upload file: ${e.message}")
        }
    }

    override suspend fun downloadFile(fileId: String, destinationFile: File): Result<File> = runCatching {
        val userId = authManager.getCurrentUser()?.uid ?: throw IllegalStateException("User not authenticated")

        if (!isUserAuthenticated()) {
            throw SecurityException("User authentication required")
        }

        try {
            val storageRef = storage.reference.child("apks/$userId/$fileId")
            storageRef.getFile(destinationFile).await()
            destinationFile
        } catch (e: Exception) {
            throw SecurityException("Failed to download file: ${e.message}")
        }
    }

    override suspend fun listFiles(): Result<List<CloudFile>> = runCatching {
        val userId = authManager.getCurrentUser()?.uid ?: throw IllegalStateException("User not authenticated")

        if (!isUserAuthenticated()) {
            throw SecurityException("User authentication required")
        }

        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("apks")
                .get()
                .await()

            snapshot.documents.map { doc ->
                CloudFile(
                    id = doc.id,
                    name = doc.getString("fileName") ?: "",
                    size = doc.getLong("size") ?: 0L,
                    modifiedTime = doc.getLong("uploadTime") ?: 0L
                )
            }
        } catch (e: Exception) {
            throw SecurityException("Failed to list files: ${e.message}")
        }
    }

    private fun isUserAuthenticated(): Boolean {
        return authManager.getCurrentUser() != null &&
               !authManager.getCurrentUser()!!.isAnonymous
    }
}
