package dev.rongpi.livecaptions.stt

import android.content.Context
import dev.rongpi.livecaptions.download.ModelDownloader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

open class FakeModelDownloader(
    var dummyProgress: ModelDownloader.DownloadProgress = ModelDownloader.DownloadProgress(100, 100),
    var shouldThrow: Boolean = false
) : ModelDownloader() {
    override fun downloadAndExtractZip(context: Context, url: String, targetDirName: String): Flow<ModelDownloader.DownloadProgress> {
        return if (shouldThrow) {
            flow { throw Exception("Download failed") }
        } else {
            flowOf(dummyProgress)
        }
    }

    override fun downloadFile(context: Context, url: String, targetFileName: String): Flow<ModelDownloader.DownloadProgress> {
        return if (shouldThrow) {
            flow { throw Exception("Download failed") }
        } else {
            flowOf(dummyProgress)
        }
    }
}
