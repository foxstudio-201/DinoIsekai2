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

package com.movtery.zalithlauncher.ui.screens

import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.ui.androidText
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * 嵌套NavDisplay的屏幕
 */
sealed interface NestedNavKey {
    /** 启动屏幕 */
    @Serializable class Splash : BackStackNavKey<TitledNavKey>() {
        init {
            backStack.addIfEmpty(NormalNavKey.UnpackDeps)
        }
    }
    /** 主屏幕 */
    @Serializable class Main : BackStackNavKey<TitledNavKey>() {
        init {
            backStack.addIfEmpty(NormalNavKey.LauncherMain)
        }
    }
    /** 设置屏幕 */
    @Serializable class Settings : BackStackNavKey<TitledNavKey>(androidText(R.string.generic_setting)) {
        init {
            backStack.addIfEmpty(NormalNavKey.Settings.Renderer)
        }
    }
    /** 版本详细设置屏幕 */
    @Serializable
    class VersionSettings(@Contextual val version: Version) : BackStackNavKey<TitledNavKey>(
        androidText(R.string.page_title_version_manage)
    ) {
        init {
            backStack.addIfEmpty(NormalNavKey.Versions.OverView)
        }
    }
}