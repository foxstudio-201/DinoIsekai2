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

package com.movtery.zalithlauncher.ui.screens.content.elements

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.path.GamePath
import com.movtery.zalithlauncher.game.path.GamePathManager
import com.movtery.zalithlauncher.game.version.installed.cleanup.CleanFailedException
import com.movtery.zalithlauncher.game.version.installed.cleanup.GameAssetCleaner
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SimpleEditDialog
import com.movtery.zalithlauncher.ui.components.TextRailItem
import com.movtery.zalithlauncher.ui.components.fadeEdge
import com.movtery.zalithlauncher.ui.components.verticalScrollWithBar
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.utils.string.isNotEmptyOrBlank
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel

private const val TAG = "VersionsManageElements"

sealed interface GamePathOperation {
    data object None: GamePathOperation
    data object PathExists: GamePathOperation
    data class AddNewPath(val path: String): GamePathOperation
    data class RenamePath(val item: GamePath): GamePathOperation
    data class DeletePath(val item: GamePath): GamePathOperation
}

sealed interface CleanupOperation {
    data object None : CleanupOperation
    /** 清理前的提醒 */
    data object Tip : CleanupOperation
    /** 开始清理 */
    data object Clean : CleanupOperation
    /** 清理失败 */
    data class Error(val error: Throwable) : CleanupOperation
    /** 成功清除 */
    data class Success(val count: Int, val size: String) : CleanupOperation
}

enum class VersionCategory(val textRes: Int) {
    /** 全部 */
    ALL(R.string.generic_all),
    /** 原版 */
    VANILLA(R.string.versions_manage_category_vanilla),
    /** 带有模组加载器 */
    MODLOADER(R.string.versions_manage_category_modloader)
}

@Composable
fun GamePathItemLayout(
    item: GamePath,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {},
    enabled: Boolean = true,
) {
    val notDefault = item.id != GamePathManager.DEFAULT_ID

    NavigationDrawerItem(
        modifier = modifier,
        colors = NavigationDrawerItemDefaults.colors(),
        label = {
            Column(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 4.dp)
                    .alpha(if (enabled) 1f else DisabledAlpha)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE).weight(1f),
                        text = if (notDefault) item.title else stringResource(R.string.versions_manage_game_path_default),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    text = item.path,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        },
        badge = {
            var menuExpanded by remember { mutableStateOf(false) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = { menuExpanded = !menuExpanded }
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.ic_more_horiz),
                        contentDescription = stringResource(R.string.generic_more),
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = 3.dp,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        enabled = notDefault,
                        text = { Text(text = stringResource(R.string.generic_rename)) },
                        leadingIcon = {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(R.drawable.ic_edit_filled),
                                contentDescription = stringResource(R.string.generic_rename)
                            )
                        },
                        onClick = {
                            onRename()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        enabled = notDefault,
                        text = { Text(text = stringResource(R.string.generic_delete)) },
                        leadingIcon = {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(R.drawable.ic_delete_filled),
                                contentDescription = stringResource(R.string.generic_delete)
                            )
                        },
                        onClick = {
                            onDelete()
                            menuExpanded = false
                        }
                    )
                }
            }
        },
        selected = selected,
        onClick = {
            if (enabled) onClick()
        }
    )
}

@Composable
fun GamePathOperation(
    gamePathOperation: GamePathOperation,
    changeState: (GamePathOperation) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    fun doRunCatching(block: () -> Unit) {
        runCatching {
            block()
        }.onFailure { e ->
            submitError(
                ErrorViewModel.ThrowableMessage(
                    title = androidText(R.string.versions_manage_game_path_error_title),
                    message = androidText(e.getMessageOrToString())
                )
            )
        }
    }
    when (gamePathOperation) {
        is GamePathOperation.None -> {}
        is GamePathOperation.AddNewPath -> {
            NameEditPathDialog(
                onDismissRequest = { changeState(GamePathOperation.None) },
                onConfirm = { value ->
                    if (GamePathManager.containsPath(gamePathOperation.path)) {
                        changeState(GamePathOperation.PathExists)
                    } else {
                        doRunCatching {
                            GamePathManager.addNewPath(title = value, path = gamePathOperation.path)
                        }
                        changeState(GamePathOperation.None)
                    }
                }
            )
        }
        is GamePathOperation.RenamePath -> {
            NameEditPathDialog(
                initValue = gamePathOperation.item.title,
                onDismissRequest = { changeState(GamePathOperation.None) },
                onConfirm = { value ->
                    doRunCatching {
                        GamePathManager.modifyTitle(gamePathOperation.item, value)
                    }
                    changeState(GamePathOperation.None)
                }
            )
        }
        is GamePathOperation.DeletePath -> {
            SimpleAlertDialog(
                title = stringResource(R.string.versions_manage_game_path_delete_title),
                text = stringResource(R.string.versions_manage_game_path_delete_message),
                onDismiss = { changeState(GamePathOperation.None) },
                onConfirm = {
                    doRunCatching {
                        GamePathManager.removePath(gamePathOperation.item)
                    }
                    changeState(GamePathOperation.None)
                }
            )
        }
        is GamePathOperation.PathExists -> {
            SimpleAlertDialog(
                title = stringResource(R.string.versions_manage_game_path_exists_title),
                text = stringResource(R.string.versions_manage_game_path_exists_message),
                onDismiss = { changeState(GamePathOperation.None) }
            )
        }
    }
}

@Composable
fun VersionCategoryItem(
    modifier: Modifier = Modifier,
    value: VersionCategory,
    versionsCount: Int,
    selected: Boolean,
    shape: Shape = MaterialTheme.shapes.large,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.labelMedium,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    TextRailItem(
        modifier = modifier,
        text = {
            Text(
                text = stringResource(value.textRes),
                style = style
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "($versionsCount)",
                style = style
            )
        },
        onClick = onClick,
        selected = selected,
        shape = shape,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
        enabled = enabled
    )
}

@Composable
private fun NameEditPathDialog(
    initValue: String = "",
    onDismissRequest: () -> Unit = {},
    onConfirm: (value: String) -> Unit = {}
) {
    var value by remember { mutableStateOf(initValue) }
    SimpleEditDialog(
        title = stringResource(R.string.versions_manage_game_path_add_new),
        value = value,
        onValueChange = { value = it },
        label = { Text(text = stringResource(R.string.versions_manage_game_path_edit_title)) },
        isError = value.isEmpty(),
        supportingText = {
            if (value.isEmpty()) Text(text = stringResource(R.string.generic_cannot_empty))
        },
        singleLine = true,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            if (value.isNotEmptyOrBlank()) {
                onConfirm(value.trim())
            }
        }
    )
}

@Composable
fun CleanupOperation(
    operation: CleanupOperation,
    changeOperation: (CleanupOperation) -> Unit,
    cleaner: GameAssetCleaner?,
    onClean: () -> Unit,
    onCancel: () -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    when(operation) {
        is CleanupOperation.None -> {}
        is CleanupOperation.Tip -> {
            SimpleAlertDialog(
                title = stringResource(R.string.versions_manage_cleanup),
                text = {
                    Text(stringResource(R.string.versions_manage_cleanup_tip))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.versions_manage_cleanup_warning))
                    Text("../assets/..")
                    //不再清理依赖库，文件并不会太大，也有可能导致其他问题：#617
//                    Text("../libraries/..")
                },
                onConfirm = onClean,
                onCancel = { changeOperation(CleanupOperation.None) }
            )
        }
        is CleanupOperation.Clean -> {
            if (cleaner != null) {
                val tasks = cleaner.tasksFlow.collectAsStateWithLifecycle()
                if (tasks.value.isNotEmpty()) {
                    //清理无用游戏文件流程对话框
                    TitleTaskFlowDialog(
                        title = stringResource(R.string.versions_manage_cleanup),
                        tasks = tasks.value,
                        onCancel = {
                            onCancel()
                            changeOperation(CleanupOperation.None)
                        }
                    )
                }
            }
        }
        is CleanupOperation.Error -> {
            val error = operation.error
            if (error is CleanFailedException) {
                AlertDialog(
                    onDismissRequest = {},
                    title = {
                        Text(
                            text = stringResource(R.string.generic_warning),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fadeEdge(state = scrollState)
                                .verticalScrollWithBar(state = scrollState)
                        ) {
                            Text(stringResource(R.string.versions_manage_cleanup_failed_files))
                            error.files.forEach { file ->
                                Text(file.absolutePath)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { changeOperation(CleanupOperation.None) }) {
                            MarqueeText(text = stringResource(R.string.generic_cancel))
                        }
                    }
                )
            } else {
                changeOperation(CleanupOperation.None)
                submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = androidText(R.string.versions_manage_cleanup_failed),
                        message = androidText(error.getMessageOrToString())
                    )
                )
            }
        }
        is CleanupOperation.Success -> {
            SimpleAlertDialog(
                title = stringResource(R.string.versions_manage_cleanup),
                text = stringResource(R.string.versions_manage_cleanup_success, operation.count, operation.size)
            ) {
                changeOperation(CleanupOperation.None)
            }
        }
    }
}

/**
 * 模组加载器图标展示组件，包装 [Image]
 */
@Composable
fun ModLoaderIcon(
    modloader: ModLoader,
    @DrawableRes
    defaultIcon: Int,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) {
    val icon = getLoaderIconRes(modloader, defaultIcon)
    Image(
        modifier = modifier,
        painter = painterResource(icon),
        contentDescription = null,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
    )
}

private fun getLoaderIconRes(
    loader: ModLoader?,
    @DrawableRes
    defaultIcon: Int = R.drawable.img_minecraft,
): Int {
    return when (loader) {
        ModLoader.FORGE -> R.drawable.img_anvil
        else -> defaultIcon
    }
}