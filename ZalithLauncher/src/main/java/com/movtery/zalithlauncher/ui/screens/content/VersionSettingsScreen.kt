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

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.fadeEdge
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryIcon
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryItem
import com.movtery.zalithlauncher.ui.screens.content.versions.ScreenshotsManagerScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.VersionConfigScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.VersionOverViewScreen
import com.movtery.zalithlauncher.ui.screens.navigateOnce
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendToast

@Composable
fun VersionSettingsScreen(
    key: NestedNavKey.VersionSettings,
    backScreenViewModel: ScreenBackStackViewModel,
    backToMainScreen: () -> Unit,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val cBackToMainScreen by rememberUpdatedState(backToMainScreen)
    DisposableEffect(key) {
        val listener = object : suspend () -> Unit {
            override suspend fun invoke() {
                cBackToMainScreen()
            }
        }
        VersionsManager.registerListener(listener)
        onDispose {
            VersionsManager.unregisterListener(listener)
        }
    }

    BaseScreen(
        screenKey = key,
        currentKey = backScreenViewModel.mainScreen.currentKey
    ) { isVisible ->
        Row(modifier = Modifier.fillMaxSize()) {
            TabMenu(
                isVisible = isVisible,
                backStack = key.backStack,
                versionsScreenKey = key.currentKey,
                modifier = Modifier.fillMaxHeight()
            )

            NavigationUI(
                modifier = Modifier.fillMaxHeight(),
                key = key,
                backScreenViewModel = backScreenViewModel,
                versionsScreenKey = key.currentKey,
                onCurrentKeyChange = { newKey ->
                    key.currentKey = newKey
                },
                backToMainScreen = backToMainScreen,
                version = key.version,
                eventViewModel = eventViewModel,
                submitError = submitError
            )
        }
    }
}

private val settingItems = listOf(
    CategoryItem(NormalNavKey.Versions.OverView, { CategoryIcon(R.drawable.ic_dashboard_outlined, R.string.versions_settings_overview) }, R.string.versions_settings_overview),
    CategoryItem(NormalNavKey.Versions.Config, { CategoryIcon(R.drawable.ic_build_outlined, R.string.versions_settings_config) }, R.string.versions_settings_config),
    CategoryItem(NormalNavKey.Versions.ScreenshotsManager, { CategoryIcon(R.drawable.ic_photo_library_outlined, R.string.screenshots_manage) }, R.string.screenshots_manage),
)

@Composable
private fun TabMenu(
    isVisible: Boolean,
    backStack: NavBackStack<TitledNavKey>,
    versionsScreenKey: TitledNavKey?,
    modifier: Modifier = Modifier
) {
    val xOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible,
        isHorizontal = true
    )

    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fadeEdge(scrollState)
            .width(IntrinsicSize.Min)
            .padding(start = 8.dp)
            .offset { IntOffset(x = xOffset.roundToPx(), y = 0) }
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        settingItems.forEach { item ->
            if (item.division) {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth(0.4f)
                        .alpha(0.4f),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            NavigationRailItem(
                selected = versionsScreenKey === item.key,
                onClick = {
                    backStack.navigateOnce(item.key)
                },
                icon = {
                    item.icon()
                },
                label = {
                    Text(
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        text = stringResource(item.textRes),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    key: NestedNavKey.VersionSettings,
    backScreenViewModel: ScreenBackStackViewModel,
    versionsScreenKey: TitledNavKey?,
    onCurrentKeyChange: (TitledNavKey?) -> Unit,
    backToMainScreen: () -> Unit,
    version: Version,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val mainScreenKey = backScreenViewModel.mainScreen.currentKey

    val backStack = key.backStack
    val stackTopKey = backStack.lastOrNull()
    LaunchedEffect(stackTopKey) {
        onCurrentKeyChange(stackTopKey)
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
                entry<NormalNavKey.Versions.OverView> {
                    VersionOverViewScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        backToMainScreen = backToMainScreen,
                        onOpenFolder = { folderPath ->
                            backScreenViewModel.mainScreen.navigateTo(
                                NormalNavKey.BuiltInFileManager(startPath = folderPath)
                            )
                        },
                        version = version,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.Config> {
                    VersionConfigScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        onCheckVulkan = {
                            eventViewModel.sendEvent(
                                EventViewModel.Event.VulkanCheck
                            )
                        },
                        showToast = { text ->
                            eventViewModel.sendToast(text)
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ScreenshotsManager> {
                    ScreenshotsManagerScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}