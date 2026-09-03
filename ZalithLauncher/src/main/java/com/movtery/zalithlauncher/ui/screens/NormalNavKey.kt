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
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.screens.content.FirstLoginMenu
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * 普通的屏幕
 */
sealed interface NormalNavKey : TitledNavKey {
    @Contextual override val title: AndroidStringText?
        get() = null

    /** 解压依赖内容屏幕（启动屏幕） */
    @Serializable data object UnpackDeps: NormalNavKey
    /** 启动器主页屏幕 */
    @Serializable data object LauncherMain : NormalNavKey
    /** 账号管理屏幕 */
    @Serializable data class AccountManager(
        val loginMenu: FirstLoginMenu = FirstLoginMenu.NONE
    ) : NormalNavKey {
        @Contextual override val title: AndroidStringText = androidText(R.string.page_title_account_list)
    }
    /** 自定义主页编辑器屏幕 */
    @Serializable data object HomePageEditor : NormalNavKey {
        @Contextual override val title: AndroidStringText = androidText(R.string.page_title_home_page_editor)
    }
    /** Web屏幕 */
    @Serializable data class WebScreen(val url: String) : NormalNavKey
    /** 文件选择屏幕 */
    @Serializable data class FileSelector(
        val startPath: String,
        val selectFile: Boolean,
        val saveKey: TitledNavKey,
        val onSelected: (path: String) -> Unit
    ) : NormalNavKey {
        @Contextual override val title: AndroidStringText = androidText(R.string.page_title_select_files)
    }
    /** 内置文件管理器屏幕 */
    @Serializable data class BuiltInFileManager(
        val startPath: String? = null
    ) : NormalNavKey {
        @Contextual override val title: AndroidStringText = androidText(R.string.page_title_file_manager)
    }
    /** 文件编辑器屏幕 */
    @Serializable data class FileEditor(
        val filePath: String
    ) : NormalNavKey {
        @Contextual override val title: AndroidStringText = androidText(R.string.page_title_file_editor)
    }

    /** 设置嵌套子屏幕 */
    sealed interface Settings : NormalNavKey {
        /** 渲染器设置屏幕 */
        @Serializable data object Renderer : Settings {
            @Contextual override val title: AndroidStringText = androidText(R.string.settings_tab_renderer)
        }
        /** Turnip 驱动下载屏幕 */
        @Serializable data object TurnipDrivers : Settings {
            @Contextual override val title: AndroidStringText = androidText(R.string.settings_renderer_download_turnip)
        }
        /** 游戏设置屏幕 */
        @Serializable data object Game : Settings {
            @Contextual override val title: AndroidStringText = androidText(R.string.settings_tab_game)
        }
        /** 控制设置屏幕 */
        @Serializable data object Control : Settings {
            @Contextual override val title: AndroidStringText = androidText(R.string.settings_tab_control)
        }
        /** 手柄设置屏幕 */
        @Serializable data object Gamepad : Settings {
            @Contextual override val title: AndroidStringText = androidText(R.string.settings_tab_gamepad)
        }
        /** 启动器设置屏幕 */
        @Serializable data object Launcher : Settings {
            @Contextual override val title: AndroidStringText = androidText(R.string.settings_tab_launcher)
        }
        /** Java管理屏幕 */
        @Serializable data object JavaManager : Settings {
            @Contextual override val title: AndroidStringText = androidText(R.string.settings_tab_java_manage)
        }
        /** 控制管理屏幕 */
        @Serializable data object ControlManager : Settings {
            @Contextual override val title: AndroidStringText = androidText(R.string.settings_tab_control_manage)
        }
        /** 关于屏幕 */
        @Serializable data object AboutInfo : Settings {
            @Contextual override val title: AndroidStringText = androidText(R.string.settings_tab_info_about)
        }
    }

    /** 协议展示屏幕 */
    @Serializable data class License(
        val raw: Int
    ): NormalNavKey

    /** 披风浏览屏幕 */
    @Serializable data class CapeGallery(
        val accountUUID: String
    ): NormalNavKey {
        @Contextual override val title: AndroidStringText = androidText(R.string.account_capes_labynet_title)
    }

    /** 录像管理屏幕 */
    @Serializable data object Recordings : NormalNavKey {
        @Contextual override val title: AndroidStringText = androidText(R.string.page_title_recordings)
    }
}