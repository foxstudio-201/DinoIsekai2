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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersions
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.game.version.installed.VersionsManager.isVersionExists
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedColumn
import com.movtery.zalithlauncher.ui.components.SimpleTextInputField
import com.movtery.zalithlauncher.ui.components.verticalScrollWithBar
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.CommonVersionInfoLayout
import com.movtery.zalithlauncher.ui.screens.content.elements.isFilenameInvalid
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private class AddonsViewModel(
    private val gameVersion: String,
) : ViewModel() {
    val addonList = AddonList()
    val currentAddon = CurrentAddon()
    var refreshIcon by mutableStateOf(false)
        private set

    fun refreshIcon() {
        refreshIcon = !refreshIcon
    }

    fun reloadForge() = launchAddonReload(
        { currentAddon.forgeState = it },
        { ForgeVersions.fetchForgeList(gameVersion) },
        { addonList.forgeList = it }
    )

    private fun <T> launchAddonReload(
        updateState: (AddonState) -> Unit,
        fetch: suspend () -> T?,
        onSuccess: (T?) -> Unit
    ) {
        viewModelScope.launch {
            runWithState(updateState, fetch).also(onSuccess)
        }
    }

    init {
        reloadForge()
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}

/**
 * 下载游戏页面（选择附加内容）
 * @param refreshErrorCheck 刷新版本名称错误检查
 */
@Composable
fun DownloadGameWithAddonScreen(
    mainScreenKey: TitledNavKey?,
    downloadScreenKey: TitledNavKey?,
    downloadGameScreenKey: TitledNavKey?,
    key: NormalNavKey.DownloadGame.Addons,
    refreshErrorCheck: Any? = null,
    onInstall: (GameDownloadInfo) -> Unit = {}
) {
    val viewModel = viewModel(
        key = key.toString()
    ) {
        AddonsViewModel(key.gameVersion)
    }

    BaseScreen(
        listOf(
            Pair(NestedNavKey.Download::class.java, mainScreenKey),
            Pair(NestedNavKey.DownloadGame::class.java, downloadScreenKey),
            Pair(NormalNavKey.DownloadGame.Addons::class.java, downloadGameScreenKey)
        )
    ) { isVisible ->
        val yOffset by swapAnimateDpAsState(
            targetValue = (-40).dp,
            swapIn = isVisible
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
        ) {
            ScreenHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                itemContainerColor = cardColor(),
                itemContentColor = onCardColor(),
                gameVersion = key.gameVersion,
                currentAddon = viewModel.currentAddon,
                refreshIcon = viewModel.refreshIcon,
                refreshErrorCheck = refreshErrorCheck,
                onInstall = { customVersionName ->
                    onInstall(
                        GameDownloadInfo(
                            gameVersion = key.gameVersion,
                            customVersionName = customVersionName,
                            overwrite = isVersionExists(customVersionName, true),
                            forge = viewModel.currentAddon.forgeVersion.value
                        )
                    )
                }
            )

            AnimatedColumn(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .verticalScrollWithBar(state = rememberScrollState()),
                isVisible = isVisible
            ) { scope ->
                Spacer(Modifier)

                AnimatedItem(scope) { yOffset ->
                    ForgeList(
                        modifier = Modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                        currentAddon = viewModel.currentAddon,
                        onValueChanged = { viewModel.refreshIcon() },
                        addonList = viewModel.addonList
                    ) { viewModel.reloadForge() }
                }

                Spacer(Modifier)
            }
        }
    }
}

@Composable
private fun ScreenHeader(
    modifier: Modifier = Modifier,
    itemContainerColor: Color,
    itemContentColor: Color,
    gameVersion: String,
    currentAddon: CurrentAddon,
    refreshIcon: Any? = null,
    refreshErrorCheck: Any? = null,
    onInstall: (String) -> Unit = {}
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(8.dp))

            VersionIconPreview(
                modifier = Modifier.size(28.dp),
                currentAddon = currentAddon,
                refreshIcon = refreshIcon
            )

            var nameValue by remember { mutableStateOf(gameVersion) }
            //用户是否对版本名称进行过编辑
            var editedByUser by remember { mutableStateOf(false) }

            AutoChangeVersionName(
                gameVersion = gameVersion,
                currentAddon = currentAddon,
                editedByUser = editedByUser,
                changeValue = {
                    nameValue = it
                }
            )

            val emptyError = stringResource(R.string.generic_cannot_empty)
            val overwriteMessage = stringResource(R.string.download_game_version_overwrite, nameValue)

            val filenameInvalidMessage = key(nameValue) {
                isFilenameInvalid(nameValue)
            }
            val isVersionOverwrite = remember(nameValue) {
                //如果目标版本存在，则使用覆盖安装的方式进行安装
                isVersionExists(nameValue, true)
            }

            val isError = remember(nameValue, refreshErrorCheck) {
                nameValue.isEmpty() || filenameInvalidMessage != null
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .animateContentSize(animationSpec = getAnimateTween())
            ) {
                SimpleTextInputField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 4.dp),
                    value = nameValue,
                    onValueChange = {
                        nameValue = it
                        if (!editedByUser) {
                            //用户已经对版本名称进行了编辑
                            editedByUser = true
                        }
                    },
                    color = itemContainerColor,
                    contentColor = itemContentColor,
                    singleLine = true,
                    hint = {
                        Text(
                            text = stringResource(R.string.download_game_version_name),
                            style = TextStyle(color = itemContentColor).copy(fontSize = 12.sp)
                        )
                    }
                )

                if (isError || isVersionOverwrite) {
                    val message = if (isError) {
                        filenameInvalidMessage ?: emptyError
                    } else {
                        overwriteMessage
                    }

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        text = message,
                        color = if (isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            val versions by VersionsManager.versions.collectAsStateWithLifecycle()

            if (versions.isNotEmpty()) {
                Row {
                    //不使用viewModel存储，防止版本刷新这里状态不同步
                    var showMenu by remember { mutableStateOf(false) }
                    //选择要覆盖安装的版本
                    IconButton(
                        onClick = {
                            showMenu = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (showMenu) {
                                    R.drawable.ic_menu_open
                                } else {
                                    R.drawable.ic_menu
                                }
                            ),
                            contentDescription = stringResource(R.string.download_game_version_overwrite_select)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        //一个提醒用的Text
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            text = stringResource(R.string.download_game_version_overwrite_select_subtitle),
                            style = MaterialTheme.typography.labelLarge
                        )

                        versions.forEach { version ->
                            DropdownMenuItem(
                                text = {
                                    CommonVersionInfoLayout(
                                        modifier = Modifier.weight(1f),
                                        version = version
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    //直接更新当前编辑的名称
                                    nameValue = version.getVersionName()
                                    editedByUser = true //也算是用户编辑了，不过目的是防止选择加载器被覆盖
                                }
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = {
                    if (!isError) {
                        onInstall(nameValue)
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_download_2_filled),
                    contentDescription = stringResource(R.string.download_install)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun VersionIconPreview(
    modifier: Modifier = Modifier,
    currentAddon: CurrentAddon,
    refreshIcon: Any? = null
) {
    val iconRes = remember(refreshIcon) {
        when {
            currentAddon.forgeVersion.value != null -> R.drawable.img_anvil
            else -> R.drawable.img_minecraft
        }
    }

    Image(
        modifier = modifier,
        painter = painterResource(id = iconRes),
        contentDescription = null
    )
}

/**
 * 根据当前已选择的Addon，自动修改版本名称
 * @param editedByUser 版本名称是否已被用户修改，如果用户已经修改过版本名称，则阻止自动修改
 */
@Composable
private fun AutoChangeVersionName(
    gameVersion: String,
    currentAddon: CurrentAddon,
    editedByUser: Boolean,
    changeValue: (String) -> Unit = {}
) {
    LaunchedEffect(
        currentAddon.forgeVersion.value,
    ) {
        if (editedByUser) return@LaunchedEffect

        fun formatModloader(name: String, version: String) = "$name $version"
        fun formatForge(forge: ForgeVersion) = formatModloader(ModLoader.FORGE.displayName, forge.versionName)

        val modloaderValue = buildString {
            with(currentAddon) {
                when {
                    forgeVersion.value != null -> append(formatForge(forgeVersion.value!!))
                    else -> {
                        changeValue(gameVersion)
                        return@LaunchedEffect
                    }
                }
            }
        }

        changeValue("$gameVersion $modloaderValue")
    }
}