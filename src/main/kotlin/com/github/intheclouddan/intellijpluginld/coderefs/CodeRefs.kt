package com.github.intheclouddan.intellijpluginld.coderefs

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.util.download.DownloadableFileDescription
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.Decompressor
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths


class CodeRefs {
    val codeRefsVerion = "2.1.0"
    val codeRefsPath = PathManager.getPluginsPath() + "/intellij-plugin-ld/bin/coderefs/"
    val codeRefsFullPath = "$codeRefsPath$codeRefsVerion/ld-find-code-refs"

    fun downloadCodeRefs(project: Project? = null): DownloadResult {
        val result = ProgressManager.getInstance().runProcessWithProgressSynchronously(ThrowableComputable<DownloadResult, Nothing> {
            downloadCodeRefsSynchronously()
        }, "Download LaunchDarkly CodeRefs", true, project)

        when (result) {
            is DownloadResult.Ok -> {
                Notifications.Bus.notify(Notification(
                        LAUNCHDARKLY_CODEREFS_ID,
                        "LaunchDarkly",
                        "Code References successfully downloaded",
                        NotificationType.INFORMATION
                ))
            }
            DownloadResult.Failed -> {
                Notifications.Bus.notify(Notification(
                        LAUNCHDARKLY_CODEREFS_ID,
                        "LaunchDarkly",
                        "Code References",
                        NotificationType.ERROR
                ))
            }
        }

        return result
    }

    private fun downloadCodeRefsSynchronously(): DownloadResult {
        val downloadUrl = codeRefsUrl
        return try {
            val crDir = downloadAndUnarchive(downloadUrl.toString())
            DownloadResult.Ok(crDir)
        } catch (e: IOException) {
            println("Can't download Code References: $e")
            DownloadResult.Failed
        }
    }

    @Throws(IOException::class)
    private fun downloadAndUnarchive(crUrl: String): File {
        val service = DownloadableFileService.getInstance()
        val downloadDesc = service.createFileDescription(crUrl)
        val downloader = service.createDownloader(listOf(downloadDesc), "Code References downloading")
        val downloadDirectory = downloadPath().toFile()
        val downloadResults = downloader.download(downloadDirectory)
        val pluginPath = File("$codeRefsPath$codeRefsVerion")
        pluginPath.mkdir()
        for (result in downloadResults) {
            val archiveFile = result.first
            Unarchiver.unarchive(archiveFile, pluginPath)
            archiveFile.delete()
        }
        return pluginPath
    }

    private fun DownloadableFileService.createFileDescription(url: String): DownloadableFileDescription {
        val fileName = url.substringAfterLast("/")
        return createFileDescription(url, fileName)
    }

    companion object {
        const val LAUNCHDARKLY_CODEREFS_ID = "LaunchDarkly Code References"

        private fun downloadPath(): Path = Paths.get(PathManager.getTempPath())
    }

    private enum class Unarchiver {
        ZIP {
            override val extension: String = "zip"
            override fun createDecompressor(file: File): Decompressor = Decompressor.Zip(file)
        },
        TAR {
            override val extension: String = "tar.gz"
            override fun createDecompressor(file: File): Decompressor = Decompressor.Tar(file)
        };

        protected abstract val extension: String
        protected abstract fun createDecompressor(file: File): Decompressor

        companion object {
            @Throws(IOException::class)
            fun unarchive(archivePath: File, dst: File) {
                val unarchiver = values().find { archivePath.name.endsWith(it.extension) }
                        ?: error("Unexpected archive type: $archivePath")
                unarchiver.createDecompressor(archivePath).extract(dst)
            }
        }
    }

    private val codeRefsUrl: URL
        get() {
            return when {
                SystemInfo.isMac -> URL("https://github.com/launchdarkly/ld-find-code-refs/releases/download/${codeRefsVerion}/ld-find-code-refs_${codeRefsVerion}_darwin_amd64.tar.gz")
                SystemInfo.isLinux -> URL("https://github.com/launchdarkly/ld-find-code-refs/releases/download/${codeRefsVerion}/ld-find-code-refs_${codeRefsVerion}_linux_amd64.tar.gz")
                SystemInfo.isWindows -> {
                    if (SystemInfo.is64Bit) {
                        URL("https://github.com/launchdarkly/ld-find-code-refs/releases/download/${codeRefsVerion}/ld-find-code-refs_${codeRefsVerion}_windows_amd64.tar.gz")
                    } else {
                        URL("https://github.com/launchdarkly/ld-find-code-refs/releases/download/${codeRefsVerion}/ld-find-code-refs_${codeRefsVerion}_windows_386.tar.gz")
                    }
                }
                else -> return URL("")
            }
        }

    sealed class DownloadResult {
        class Ok(val crDir: File) : DownloadResult()
        object Failed : DownloadResult()
    }

}