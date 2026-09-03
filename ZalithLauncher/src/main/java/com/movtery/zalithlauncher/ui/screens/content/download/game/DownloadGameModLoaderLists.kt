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

package com.movtery.zalithlauncher.ui.screens.content.download.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.addons.modloader.ResponseTooShortException
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersion
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.network.toLocal
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

private const val TAG = "ModLoaderLists"

class AddonList {
    var forgeList by mutableStateOf<List<ForgeVersion>?>(null)
}

class CurrentAddon {
    var forgeVersion = mutableStateOf<ForgeVersion?>(null)
    var forgeState by mutableStateOf<AddonState>(AddonState.None)
    var incompatibleWithForge = mutableStateOf<Set<ModLoader>>(emptySet())

    private val allLoaders = listOf(
        LoaderState(ModLoader.FORGE, forgeVersion, incompatibleWithForge)
    )

    private val loaderMap = allLoaders.associateBy { it.loader }

    fun updateIncompatibleState(
        thisLoader: ModLoader,
        addonList: AddonList
    ) {
        val currentVersions = loaderMap.mapValues { it.value.versionState.value }
        loaderMap.values.forEach { targetState ->
            targetState.incompatibleState.value = emptySet()
        }
    }

    private data class LoaderState<T : Any>(
        val loader: ModLoader,
        val versionState: MutableState<T?>,
        val incompatibleState: MutableState<Set<ModLoader>>
    )
}

suspend fun <T> ViewModel.runWithState(
    updateState: (AddonState) -> Unit,
    block: suspend () -> T?
): T? {
    updateState(AddonState.Loading)
    return runCatching {
        block().also {
            updateState(AddonState.None)
        }
    }.onFailure { e ->
        val state = when (e) {
            is ResponseTooShortException -> {
                AddonState.None
            }
            is HttpRequestTimeoutException -> AddonState.Error(androidText(R.string.error_timeout))
            is UnknownHostException, is UnresolvedAddressException -> {
                AddonState.Error(androidText(R.string.error_network_unreachable))
            }
            is ConnectException -> {
                AddonState.Error(androidText(R.string.error_connection_failed))
            }
            is SerializationException -> {
                AddonState.Error(androidText(R.string.error_parse_failed))
            }
            is ResponseException -> AddonState.Error(e.toLocal())
            else -> {
                Logger.error(TAG, "An unknown exception was caught!", e)
                AddonState.Error(
                    androidText(e.localizedMessage ?: e.message ?: e::class.qualifiedName ?: "Unknown error")
                )
            }
        }
        updateState(state)
    }.getOrNull()
}

@Composable
fun ForgeList(
    modifier: Modifier = Modifier,
    currentAddon: CurrentAddon,
    addonList: AddonList,
    error: String? = null,
    onValueChanged: () -> Unit = {},
    onReload: () -> Unit = {}
) {
    var version by currentAddon.forgeVersion
    val incompatibleSet by currentAddon.incompatibleWithForge

    AddonListLayout(
        modifier = modifier,
        state = currentAddon.forgeState,
        title = ModLoader.FORGE.displayName,
        iconPainter = painterResource(R.drawable.img_anvil),
        items = addonList.forgeList,
        current = version,
        incompatibleSet = incompatibleSet,
        checkIncompatible = {
            currentAddon.updateIncompatibleState(ModLoader.FORGE, addonList)
        },
        error = error ?: checkForgeCompatibilityError(addonList.forgeList),
        getItemText = { it.versionName },
        summary = { ForgeVersionSummary(it) },
        onValueChange = { version0 ->
            version = version0
            onValueChanged()
        },
        onReload = onReload
    )
}

@Composable
private fun checkForgeCompatibilityError(
    forgeList: List<ForgeVersion>?
): String? {
    return when {
        forgeList == null -> null
        forgeList.any { forgeVersion -> forgeVersion.category == "universal" || forgeVersion.category == "client" } -> {
            stringResource(R.string.download_game_addon_not_installable)
        }
        else -> null
    }
}