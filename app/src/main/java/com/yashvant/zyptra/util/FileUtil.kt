package com.yashvant.zyptra.util

import android.content.Context
import com.yashvant.zyptra.data.model.AppInfo
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileUtil {
    fun createBackupZip(context: Context, appInfo: AppInfo): File {
        val backupDir = File(context.getExternalFilesDir(null), Constants.BACKUP_FOLDER_NAME)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        val tempDir = File(backupDir, Constants.TEMP_FOLDER_NAME)
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
        tempDir.mkdirs()

        // Copy APK
        val apkFile = File(appInfo.apkPath)
        val apkCopy = File(tempDir, "base.apk")
        apkFile.copyTo(apkCopy)

        // Create zip file
        val zipFile = File(backupDir, "${appInfo.packageName}${Constants.ZIP_EXTENSION}")
        if (zipFile.exists()) {
            zipFile.delete()
        }

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { out ->
            zipFile(out, tempDir, "")
        }

        // Clean up temp directory
        tempDir.deleteRecursively()

        return zipFile
    }

    private fun zipFile(zipOut: ZipOutputStream, fileToZip: File, fileName: String) {
        when {
            fileToZip.isHidden -> return
            fileToZip.isDirectory -> {
                val zipEntry = ZipEntry(
                    if (fileName.isEmpty()) "" else "$fileName/"
                )
                zipOut.putNextEntry(zipEntry)
                zipOut.closeEntry()

                fileToZip.listFiles()?.forEach { childFile ->
                    zipFile(
                        zipOut,
                        childFile,
                        if (fileName.isEmpty()) childFile.name
                        else "$fileName/${childFile.name}"
                    )
                }
                return
            }
            else -> {
                FileInputStream(fileToZip).use { fi ->
                    BufferedInputStream(fi).use { origin ->
                        val entry = ZipEntry(fileName)
                        zipOut.putNextEntry(entry)
                        origin.copyTo(zipOut, 1024)
                    }
                }
            }
        }
    }
} 