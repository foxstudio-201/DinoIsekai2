/*
 * Dino Isekai — Minecraft Launcher for Android
 * Copyright (C) 2025 FoxStudio
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.dinostate

import com.movtery.zalithlauncher.path.DOWNLOAD_OKHTTP_CLIENT
import com.movtery.zalithlauncher.path.GLOBAL_CLIENT
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.upgrade.GithubReleaseApi
import com.movtery.zalithlauncher.utils.file.ensureParentDirectory
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.network.safeBodyAsJson
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.apache.commons.io.FileUtils
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipFile
import kotlin.math.min

private const val TAG = "DinoStateSync"

enum class DinoStateType { BASE, DATA }

data class DinoStateCheckResult(
    val hasUpdate: Boolean,
    val currentTag: String?,
    val latestTag: String?,
    val assetName: String?,
    val assetUrl: String?,
    val assetSize: Long,
    val error: String? = null
)

data class DinoStateProgress(
    val type: DinoStateType,
    val phase: String,
    val percent: Int,
    val message: String
)

object DinoStateSync {
    const val BASE_REPO = "foxstudio-201/dinostatedata"
    const val DATA_REPO = "foxstudio-201/datadinoisekaiserver"

    fun repoOf(type: DinoStateType): String = when (type) {
        DinoStateType.BASE -> BASE_REPO
        DinoStateType.DATA -> DATA_REPO
    }

    fun markerName(type: DinoStateType): String = when (type) {
        DinoStateType.BASE -> ".dinobase-version"
        DinoStateType.DATA -> ".dinosync-version"
    }

    fun readMarker(type: DinoStateType, gameDir: File): String? {
        val marker = File(gameDir, markerName(type))
        return if (marker.exists() && marker.isFile) marker.readText().trim().takeIf { it.isNotEmpty() } else null
    }

    /**
     * Kiểm tra bản phát hành mới nhất trên GitHub và so sánh với version marker đã lưu.
     */
    suspend fun checkUpdate(type: DinoStateType, gameDir: File): DinoStateCheckResult {
        val currentTag = readMarker(type, gameDir)
        val repo = repoOf(type)
        return try {
            val release: GithubReleaseApi = GLOBAL_CLIENT.get("https://api.github.com/repos/$repo/releases/latest")
                .safeBodyAsJson()
            val latestTag = release.tagName.trim()
            val asset = selectAsset(release.assets)
            if (asset == null) {
                DinoStateCheckResult(
                    hasUpdate = false,
                    currentTag = currentTag,
                    latestTag = latestTag,
                    assetName = null,
                    assetUrl = null,
                    assetSize = 0L
                )
            } else {
                val hasUpdate = latestTag != currentTag
                DinoStateCheckResult(
                    hasUpdate = hasUpdate,
                    currentTag = currentTag,
                    latestTag = latestTag,
                    assetName = asset.name,
                    assetUrl = asset.browserDownloadUrl,
                    assetSize = asset.size
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.warning(TAG, "Check update failed for $repo", e)
            DinoStateCheckResult(
                hasUpdate = false,
                currentTag = currentTag,
                latestTag = null,
                assetName = null,
                assetUrl = null,
                assetSize = 0L,
                error = e.message ?: e.javaClass.simpleName
            )
        }
    }

    private fun selectAsset(assets: List<GithubReleaseApi.Asset>): GithubReleaseApi.Asset? {
        val zipLike = assets.filter { asset ->
            val lower = asset.name.lowercase()
            lower.endsWith(".zip") || lower.endsWith(".rar")
        }
        return zipLike.firstOrNull { it.name.equals("update.zip", ignoreCase = true) }
            ?: zipLike.firstOrNull()
    }

    /**
     * Chạy toàn bộ quy trình đồng bộ: kiểm tra -> tải -> giải nén -> ghi marker.
     * @return true nếu dữ liệu đã được cập nhật hoặc đã mới nhất, false nếu gặp lỗi
     */
    suspend fun runSync(
        type: DinoStateType,
        gameDir: File,
        onProgress: (DinoStateProgress) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val label = typeLabel(type)

        fun progress(phase: String, percent: Int, message: String) =
            onProgress(DinoStateProgress(type, phase, percent, message))

        progress("check", 0, "Đang kiểm tra dữ liệu...")

        val check = checkUpdate(type, gameDir)
        if (check.error != null) {
            progress("done", 100, "Lỗi $label: ${check.error}")
            return@withContext false
        }
        if (!check.hasUpdate) {
            val tag = check.latestTag ?: check.currentTag
            val message = if (check.assetName == null) "$label: không có bản cập nhật mới"
            else "$label: đã cập nhật (${tag ?: ""})".trim()
            progress("done", 100, message)
            return@withContext true
        }

        val assetUrl = check.assetUrl
        if (assetUrl == null) {
            progress("done", 100, "$label: không có bản cập nhật mới")
            return@withContext true
        }

        val tempZip = File(PathManager.DIR_CACHE, "dino_sync_${type.name.lowercase()}.zip")
        val tag = check.latestTag ?: ""
        try {
            progress("download", 0, "Đang tải dữ liệu...")
            downloadAsset(assetUrl, tempZip, check.assetSize) { percent ->
                progress("download", percent, "Đang tải dữ liệu...")
            }

            progress("extract", 0, "Đang giải nén dữ liệu...")
            extractZip(tempZip, gameDir) { percent ->
                progress("extract", percent, "Đang giải nén dữ liệu...")
            }

            writeMarker(type, gameDir, tag)
            progress("done", 100, "$label: đã cập nhật ($tag)")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.warning(TAG, "Sync $label failed", e)
            progress("done", 100, "Lỗi $label: ${e.message ?: e.javaClass.simpleName}")
            false
        } finally {
            FileUtils.deleteQuietly(tempZip)
        }
    }

    private fun writeMarker(type: DinoStateType, gameDir: File, tag: String) {
        runCatching {
            val marker = File(gameDir, markerName(type))
            marker.ensureParentDirectory()
            marker.writeText(tag)
        }.onFailure { e ->
            Logger.error(TAG, "Failed to write marker for $type", e)
        }
    }

    private fun typeLabel(type: DinoStateType): String = when (type) {
        DinoStateType.BASE -> "Dữ liệu gốc"
        DinoStateType.DATA -> "Dữ liệu bổ sung"
    }

    /**
     * Tải file xuống tạm thời, có 1 lần thử lại. Dữ liệu được ghi stream trực tiếp ra file.
     */
    private suspend fun downloadAsset(
        url: String,
        dest: File,
        totalBytes: Long,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val maxAttempts = 2
        var attempt = 0
        while (true) {
            attempt++
            try {
                dest.ensureParentDirectory()
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Dino-Isekai-Launcher")
                    .build()
                DOWNLOAD_OKHTTP_CLIENT.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code} - ${response.message}")
                    }
                    val body = response.body
                    val contentLength = if (totalBytes > 0) totalBytes else body.contentLength()
                    var downloaded = 0L

                    body.byteStream().use { input ->
                        BufferedOutputStream(FileOutputStream(dest)).use { output ->
                            val buffer = ByteArray(65536)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                ensureActive()
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (contentLength > 0) {
                                    val percent = ((downloaded * 100) / contentLength).toInt().coerceIn(0, 100)
                                    onProgress(percent)
                                }
                            }
                        }
                    }

                    if (contentLength > 0 && downloaded != contentLength) {
                        throw IOException("Download incomplete. Expected $contentLength bytes, received $downloaded bytes.")
                    }
                }
                return@withContext
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                FileUtils.deleteQuietly(dest)
                if (attempt >= maxAttempts) throw e
                Logger.debug(TAG, "Download attempt $attempt failed for $url, retrying.")
            }
        }
    }

    /**
     * Giải nén zip vào [gameDir]. Nếu tất cả các entry nằm trong một thư mục gốc chung duy nhất
     * thì bỏ lớp thư mục đó. options.txt được sao lưu trước khi giải nén và khôi phục sau đó.
     */
    private suspend fun extractZip(zipFile: File, gameDir: File, onProgress: (Int) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            gameDir.ensureParentDirectory()

            val optionsBackup = File(PathManager.DIR_CACHE, "dino_sync_options_backup.txt")
            val optionsFile = File(gameDir, "options.txt")
            val hasOptionsBackup = optionsFile.exists() && optionsFile.isFile
            if (hasOptionsBackup) {
                FileUtils.deleteQuietly(optionsBackup)
                optionsFile.copyTo(optionsBackup, overwrite = true)
            }

            try {
                ZipFile(zipFile).use { zip ->
                    val rootDir = detectZipRootDir(zip)
                    val entries = zip.entries().asSequence().filter { !it.isDirectory }.toList()
                    var processed = 0

                    entries.forEach { entry ->
                        ensureActive()
                        val entryName = stripRoot(entry.name, rootDir)
                        if (entryName.isNotEmpty()) {
                            val target = safeResolve(gameDir, entryName)
                            target.ensureParentDirectory()
                            zip.getInputStream(entry).use { input ->
                                BufferedOutputStream(FileOutputStream(target)).use { output ->
                                    val buffer = ByteArray(65536)
                                    var read: Int
                                    while (input.read(buffer).also { read = it } != -1) {
                                        output.write(buffer, 0, read)
                                    }
                                }
                            }
                        }
                        processed++
                        if (entries.size > 0) {
                            onProgress(min((processed * 100) / entries.size, 100))
                        }
                    }
                }

                if (hasOptionsBackup && optionsBackup.exists()) {
                    optionsBackup.copyTo(optionsFile, overwrite = true)
                }
            } finally {
                FileUtils.deleteQuietly(optionsBackup)
            }
        }
    }

    private fun detectZipRootDir(zip: ZipFile): String? {
        val topLevels = LinkedHashSet<String>()
        var hasRootLevelFile = false
        var hasContentBelow = false

        zip.entries().asSequence().forEach { entry ->
            val name = entry.name.trimEnd('/')
            if (name.isEmpty()) return@forEach
            val slashIndex = name.indexOf('/')
            if (slashIndex < 0) {
                if (!entry.isDirectory) hasRootLevelFile = true
            } else {
                topLevels.add(name.substring(0, slashIndex))
                hasContentBelow = true
            }
        }

        return if (topLevels.size == 1 && !hasRootLevelFile && hasContentBelow) topLevels.first() else null
    }

    private fun stripRoot(name: String, rootDir: String?): String {
        if (rootDir == null) return name
        val prefix = rootDir.trimEnd('/')
        return if (name == prefix) "" else name.removePrefix("$prefix/")
    }

    private fun safeResolve(gameDir: File, entryName: String): File {
        val normalizedGameDir = gameDir.absoluteFile.normalize()
        val target = File(normalizedGameDir, entryName).normalize()
        require(target.path.startsWith(normalizedGameDir.path)) { "Unsafe zip entry: $entryName" }
        return target
    }
}
