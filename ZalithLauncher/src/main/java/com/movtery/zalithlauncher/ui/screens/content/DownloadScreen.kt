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

package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadGameScreen
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

/**
 * 导航至DownloadScreen
 */
fun ScreenBackStackViewModel.navigateToDownload(targetScreen: TitledNavKey? = null) {
    downloadScreen.clearWith(targetScreen ?: downloadGameScreen)
    mainScreen.removeAndNavigateTo(
        removes = clearBeforeNavKeys,
        screenKey = downloadScreen,
        useClassEquality = true
    )
}

@Composable
fun DownloadScreen(
    key: NestedNavKey.Download,
    backScreenViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    BaseScreen(
        screenKey = key,
        currentKey = backScreenViewModel.mainScreen.currentKey,
        useClassEquality = true
    ) { isVisible: Boolean ->
        NavigationUI(
            key = key,
            backScreenViewModel = backScreenViewModel,
            eventViewModel = eventViewModel,
            submitError = submitError,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun NavigationUI(
    key: NestedNavKey.Download,
    backScreenViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val backStack = key.backStack
    val stackTopKey = backStack.lastOrNull()
    LaunchedEffect(stackTopKey) {
        backScreenViewModel.downloadScreen.currentKey = stackTopKey
    }

    if (backStack.isNotEmpty()) {
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NestedNavKey.DownloadGame> { key ->
                    DownloadGameScreen(
                        key = key,
                        mainScreenKey = backScreenViewModel.mainScreen.currentKey,
                        downloadScreenKey = backScreenViewModel.downloadScreen.currentKey,
                        downloadGameScreenKey = backScreenViewModel.downloadGameScreen.currentKey,
                        onCurrentKeyChange = { newKey ->
                            backScreenViewModel.downloadGameScreen.currentKey = newKey
                        },
                        eventViewModel = eventViewModel
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}
