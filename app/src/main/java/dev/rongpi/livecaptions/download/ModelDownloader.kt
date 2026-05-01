package dev.rongpi.livecaptions.download

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.BufferedInputStream
import java.util.zip.ZipInputStream

open class ModelDownloader {
    private val client = OkHttpClient()

    data class DownloadProgress(val downloadedBytes: Long, val totalBytes: Long)

    open fun downloadAndExtractZip(context: Context, url: String, targetDirName: String): Flow<DownloadProgress> = flow {
        if (!url.startsWith("https://", ignoreCase = true)) {
            throw SecurityException("Insecure HTTP connections are not allowed for downloading models.")
        }

        val targetDir = File(context.filesDir, targetDirName)
        if (!targetDir.canonicalPath.startsWith(context.filesDir.canonicalPath + File.separator) && targetDir.canonicalPath != context.filesDir.canonicalPath) {
            throw SecurityException("Invalid target directory name: prevents path traversal.")
        }

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Failed to download file: ${response.code}")
        }

        val body = response.body ?: throw Exception("Empty response body")
        val totalBytes = body.contentLength()
        val inputStream: InputStream = body.byteStream()

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val tempZipFile = File(context.filesDir, "$targetDirName.zip")

        // 1. Download to temporary zip file, tracking accurate compressed bytes.
        FileOutputStream(tempZipFile).use { fos ->
            val buffer = ByteArray(8192)
            var downloadedBytes = 0L
            var len: Int
            var lastEmitTime = 0L
            while (inputStream.read(buffer).also { len = it } > 0) {
                fos.write(buffer, 0, len)
                downloadedBytes += len
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastEmitTime >= 100 || downloadedBytes == totalBytes) {
                    emit(DownloadProgress(downloadedBytes, totalBytes))
                    lastEmitTime = currentTime
                }
            }
        }

        // 2. Extract after download finishes
        ZipInputStream(BufferedInputStream(tempZipFile.inputStream())).use { zis ->
            var zipEntry = zis.nextEntry
            val buffer = ByteArray(8192)

            while (zipEntry != null) {
                val newFile = File(targetDir, zipEntry.name)

                // Prevent Zip Slip vulnerability
                if (!newFile.canonicalPath.startsWith(targetDir.canonicalPath + File.separator)) {
                    throw Exception("Entry is outside of the target dir: ${zipEntry.name}")
                }

                if (zipEntry.isDirectory) {
                    if (!newFile.isDirectory && !newFile.mkdirs()) {
                        throw Exception("Failed to create directory $newFile")
                    }
                } else {
                    val parent = newFile.parentFile
                    if (parent != null && !parent.isDirectory && !parent.mkdirs()) {
                        throw Exception("Failed to create directory $parent")
                    }

                    FileOutputStream(newFile).use { fos ->
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                zipEntry = zis.nextEntry
            }
            zis.closeEntry()
        }

        // 3. Clean up the temp zip file
        tempZipFile.delete()
    }.flowOn(Dispatchers.IO)

    open fun downloadFile(context: Context, url: String, targetFileName: String): Flow<DownloadProgress> = flow {
        if (!url.startsWith("https://", ignoreCase = true)) {
            throw SecurityException("Insecure HTTP connections are not allowed for downloading models.")
        }

        val targetFile = File(context.filesDir, targetFileName)
        if (!targetFile.canonicalPath.startsWith(context.filesDir.canonicalPath + File.separator)) {
            throw SecurityException("Invalid target file name: prevents path traversal.")
        }

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Failed to download file: ${response.code}")
        }

        val body = response.body ?: throw Exception("Empty response body")
        val totalBytes = body.contentLength()
        val inputStream: InputStream = body.byteStream()

        val parent = targetFile.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
             throw Exception("Failed to create directory $parent")
        }

        FileOutputStream(targetFile).use { fos ->
            val buffer = ByteArray(8192)
            var downloadedBytes = 0L
            var len: Int
            var lastEmitTime = 0L
            while (inputStream.read(buffer).also { len = it } > 0) {
                fos.write(buffer, 0, len)
                downloadedBytes += len
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastEmitTime >= 100 || downloadedBytes == totalBytes) {
                    emit(DownloadProgress(downloadedBytes, totalBytes))
                    lastEmitTime = currentTime
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
