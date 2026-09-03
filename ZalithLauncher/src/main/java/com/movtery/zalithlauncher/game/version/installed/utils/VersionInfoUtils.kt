/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
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

package com.movtery.zalithlauncher.game.version.installed.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.version.installed.VersionInfo
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.string.isNotEmptyOrBlank
import java.io.File

private const val TAG = "VersionInfoUtils"

private const val VERSION_PATTERN = """(\d+\.\d+\.\d+|\d{2}w\d{2}[a-z])"""

// "1.20.2-forge-48.1.0"                -> 1.20.2
// "1.21.3-forge-53.0.23"               -> 1.21.3
private val FORGE_REGEX = """$VERSION_PATTERN-forge""".toRegex()
// "1.7.10-Forge10.13.4.1614-1.7.10"    -> 1.7.10
// "1.7.2-10.12.2.1161-mc172"           -> 1.7.2
private val FORGE_OLD_REGEX = """^$VERSION_PATTERN-(Forge[\d.]*)-mc\d+""".toRegex()

private val LOADER_DETECTORS = listOf<(String) -> String?>(
    { id ->
        FORGE_REGEX.find(id)?.groupValues?.get(1)
    },
    { id ->
        FORGE_OLD_REGEX.find(id)?.groupValues?.get(1)
    }
)

/**
 * 在版本的json文件中，找到版本信息
 * @return 版本号、ModLoader信息
 */
fun parseJsonToVersionInfo(jsonFile: File): VersionInfo? {
    return runCatching {
        val jsonObject = JsonParser.parseString(jsonFile.readText()).asJsonObject
        val quickPlay = runCatching {
            ensureQuickPlay(jsonObject)
        }.getOrElse { e ->
            Logger.warning(TAG, "Failed to parse Quick Play", e)
            VersionInfo.QuickPlay(
                hasQuickPlaysSupport = false,
                isQuickPlaySingleplayer = false
            )
        }
        val (versionId, loaderInfo) = detectMinecraftAndLoader(jsonObject)
        VersionInfo(versionId, quickPlay, loaderInfo)
    }.getOrElse {
        Logger.error(TAG, "Error parsing version json", it)
        null
    }
}

/**
 * 确认是否可以使用 Quick Play
 */
private fun ensureQuickPlay(versionJson: JsonObject): VersionInfo.QuickPlay {
    var hasQuickPlaysSupport = false
    var isQuickPlaySingleplayer = false

    versionJson.getAsJsonObject("arguments")?.getAsJsonArray("game")?.forEach outerFor@{ element ->
        if (!element.isJsonObject) return@outerFor

        element.asJsonObject.getAsJsonArray("rules")?.forEach innerFor@{ rule ->
            if (!rule.isJsonObject) return@innerFor

            val features = rule.asJsonObject.getAsJsonObject("features") ?: return@innerFor

            hasQuickPlaysSupport = hasQuickPlaysSupport || features.get("has_quick_plays_support")?.asBoolean ?: false
            isQuickPlaySingleplayer = isQuickPlaySingleplayer || features.get("is_quick_play_singleplayer")?.asBoolean ?: false

            if (hasQuickPlaysSupport && isQuickPlaySingleplayer) {
                return@ensureQuickPlay VersionInfo.QuickPlay(
                    hasQuickPlaysSupport = true,
                    isQuickPlaySingleplayer = true
                )
            }
        }
    }

    return VersionInfo.QuickPlay(
        hasQuickPlaysSupport = hasQuickPlaysSupport,
        isQuickPlaySingleplayer = isQuickPlaySingleplayer
    )
}

private fun detectMinecraftAndLoader(versionJson: JsonObject): Pair<String, VersionInfo.LoaderInfo?> {
    val mcVersion = extractMinecraftVersion(versionJson)
    val loaderInfo = detectModLoader(versionJson)
    return mcVersion to loaderInfo
}

private fun extractMinecraftVersion(json: JsonObject): String {
    //尝试识别HMCL版本
    if (json.has("patches") && json.get("patches").isJsonArray) {
        val patches = json.getAsJsonArray("patches")
        if (patches.size() > 0) {
            val minecraft = patches[0].asJsonObject
            if (minecraft.has("version")) {
                return minecraft.get("version").asString
            }
        }
    }

    //尝试识别PCL导出的整合包给的版本
    //PCL顺手加的 [按住 W 开始思索]
    if (json.has("clientVersion") && json.get("clientVersion").isJsonPrimitive) {
        val clientVersion = json.get("clientVersion").asString
        if (clientVersion.isNotEmptyOrBlank()) return clientVersion
    }

    //尝试从 LaunchFor (ZL安装的版本) 获取信息
    json.getAsJsonObject("launchFor")
        ?.getAsJsonArray("infos")
        ?.firstOrNull { it.asJsonObject["name"]?.asString == "Minecraft" }
        ?.asJsonObject
        ?.getAsJsonPrimitive("version")
        ?.asString
        ?.let {
            return it
        }

    //从minecraft库中获取
    json.getAsJsonArray("libraries")?.forEach { lib ->
        val (group, artifact, version) = lib.asJsonObject["name"].asString.split(":").let {
            Triple(it[0], it[1], it.getOrNull(2) ?: "")
        }
        if (group == "net.minecraft" && (artifact == "client" || artifact == "server")) {
            return version
        }
    }

    val id = json["id"].asString
    return if (json.has("inheritsFrom")) json["inheritsFrom"].asString
    //尝试从ID中解析MC版本
    else LOADER_DETECTORS.firstNotNullOfOrNull { it(id) } ?: id
}

/**
 * 通过库判断ModLoader信息：ModLoader名称、版本
 * @param versionJson 版本json对象
 */
private fun detectModLoader(versionJson: JsonObject): VersionInfo.LoaderInfo? {
    versionJson.getAsJsonArray("libraries")?.forEach { libElement ->
        val lib = libElement.asJsonObject
        val (group, artifact, version) = lib.get("name").asString.split(":").let {
            Triple(it[0], it[1], it.getOrNull(2) ?: "")
        }

        //Forge
        if (group == "net.minecraftforge" && (artifact == "forge" || artifact == "fmlloader")) {
            val forgeVersion = when {
                //新版：1.21.4-54.0.26                 -> 54.0.26
                version.count { it == '-' } == 1 -> version.substringAfterLast('-')
                //旧版：1.7.10-10.13.4.1614-1.7.10     -> 10.13.4.1614
                //旧版：1.7.2-10.12.2.1161-mc172       -> 10.12.2.1161
                version.count { it == '-' } >= 2 -> version.split("-").let { parts ->
                    when {
                        parts.size >= 3 && parts.last().startsWith("mc") -> parts[1]
                        parts.size >= 3 && parts[0] == parts.last() -> parts[1]
                        else -> version
                    }
                }
                else -> version
            }
            return VersionInfo.LoaderInfo(ModLoader.FORGE, forgeVersion)
        }
    }

    return null
}