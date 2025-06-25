package com.yashvant.zyptra.data.storage

import java.io.File

interface CloudStorage {
    suspend fun uploadFile(file: File, fileName: String): Result<String>
    suspend fun downloadFile(fileId: String, destinationFile: File): Result<File>
    suspend fun listFiles(): Result<List<CloudFile>>
}

data class CloudFile(
    val id: String,
    val name: String,
    val size: Long,
    val modifiedTime: Long
) 