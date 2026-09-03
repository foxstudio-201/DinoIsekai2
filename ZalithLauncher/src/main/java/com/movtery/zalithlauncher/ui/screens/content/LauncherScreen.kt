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

package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.AccountType
import com.movtery.zalithlauncher.game.account.localLogin
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.main.custom_home.MarkdownBlock
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.removeAndNavigateTo
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

@Composable
fun LauncherScreen(
    backStackViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    onLaunchGame: (Version?) -> Unit,
    onOpenLink: (String) -> Unit,
    onHomePageEvent: (MarkdownBlock.Button.Event) -> Unit,
    onNavigateToStats: () -> Unit = {},
    onNavigateToPlayTimeStats: () -> Unit = {},
    onNavigateToLog: (String) -> Unit = {},
) {
    BaseScreen(
        screenKey = NormalNavKey.LauncherMain,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        DinoHomepage(
            isVisible = isVisible,
            backStackViewModel = backStackViewModel,
            navigateToVersions = navigateToVersions,
            onLaunchGame = onLaunchGame,
            onNavigateToLog = onNavigateToLog,
            onOpenLink = onOpenLink,
        )
    }
}

@Composable
private fun DinoHomepage(
    isVisible: Boolean,
    backStackViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    onLaunchGame: (Version?) -> Unit,
    onNavigateToLog: (String) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val accounts by AccountsManager.accountsFlow.collectAsState()
    val accCurrent by AccountsManager.currentAccountFlow.collectAsState()
    val versions by VersionsManager.versions.collectAsState()
    val currentVersion by VersionsManager.currentVersion.collectAsState()

    var username by remember { mutableStateOf(accCurrent?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val targetVersion = remember(versions) {
        versions.firstOrNull { v ->
            val name = v.getVersionName()
            name.contains("1.20.1") || name.contains("forge") || name.contains("Forge")
        } ?: versions.firstOrNull()
    }

    val boxInteraction = remember { MutableInteractionSource() }
    val isPressed by boxInteraction.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "playScale"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(R.drawable.bg_dino_isekai),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC000000),
                            Color(0x99000000),
                            Color(0xAA000000),
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))

            Image(
                painter = painterResource(R.drawable.dino_isekai_title),
                contentDescription = "Dino Isekai",
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(2.87f)
            )

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChipBadge("Forge 1.20.1", Color(0xFFFACC15), Color(0xFFFACC15))
                ChipBadge("Fantasy", Color(0xFFA78BFA), Color(0xFFA78BFA))
                ChipBadge("Survival", Color(0xFF34D399), Color(0xFF34D399))
                ChipBadge("Realistic", Color(0xFF60A5FA), Color(0xFF60A5FA))
            }

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF34D399))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Online",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399)
                    )
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "· 1.20.1 Forge",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xAAFFFFFF)
                    )
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "0 ms",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF60A5FA)
                    )
                )
            }

            Spacer(Modifier.height(24.dp))

            ModernCard {
                Text(
                    text = "Yêu cầu cấu hình",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xAAFFFFFF),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2a1a4a), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Tối thiểu", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xAAFFFFFF), fontSize = 10.sp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("4GB RAM", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        Text("Intel HD 500+", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xAAFFFFFF)))
                    }
                    Spacer(Modifier.width(32.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF3a2060), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Đề xuất", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA78BFA), fontSize = 10.sp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("12GB RAM", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        Text("RTX 2060+", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xAAFFFFFF)))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            ModernCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3a2060)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_person_outlined),
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { v ->
                            if (v.length <= 16) { username = v; errorMsg = null }
                        },
                        placeholder = { Text("Tên người chơi", color = Color(0x55FFFFFF)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFA78BFA),
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            cursorColor = Color(0xFFA78BFA),
                        ),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            ModernCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3a2060)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(if (showPass) R.drawable.ic_visibility_off_outlined else R.drawable.ic_visibility_outlined),
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        placeholder = { Text("Mật khẩu", color = Color(0x55FFFFFF)) },
                        singleLine = true,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFA78BFA),
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            cursorColor = Color(0xFFA78BFA),
                        ),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(
                            painter = if (showPass) painterResource(R.drawable.ic_visibility_off_outlined)
                                else painterResource(R.drawable.ic_visibility_outlined),
                            contentDescription = if (showPass) "Ẩn" else "Hiện",
                            tint = Color(0xAAFFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            errorMsg?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = msg,
                    color = Color(0xFFFF5252),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                modifier = Modifier
                    .scale(playScale)
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF7C3AED),
                onClick = {
                    if (username.isBlank()) { errorMsg = "Nhập tên người chơi!"; return@Surface }
                    if (password.isBlank()) { errorMsg = "Nhập mật khẩu!"; return@Surface }
                    if (username.length < 3 || username.length > 16) { errorMsg = "Tên 3-16 ký tự"; return@Surface }
                    if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) { errorMsg = "Chỉ dùng a-z, 0-9, _"; return@Surface }
                    errorMsg = null
                    val existing = accounts.find { it.username == username && it.accountType == AccountType.LOCAL.tag }
                    if (existing != null) AccountsManager.setCurrentAccount(existing)
                    else localLogin(username, null)
                    val ver = targetVersion ?: currentVersion
                    if (ver != null && ver.isValid()) onLaunchGame(ver)
                    else errorMsg = "Chưa có phiên bản game"
                },
                interactionSource = boxInteraction,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "PLAY",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 4.sp
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniToolButton(icon = R.drawable.ic_settings_filled, label = "Cài đặt") {
                    backStackViewModel.mainScreen.removeAndNavigateTo(
                        remove = NestedNavKey.Settings::class,
                        screenKey = NestedNavKey.Settings()
                    )
                }
                MiniToolButton(icon = R.drawable.ic_folder_filled, label = "Quản lý") {
                    backStackViewModel.mainScreen.removeAndNavigateTo(
                        remove = NestedNavKey.VersionSettings::class,
                        screenKey = NormalNavKey.VersionsManager
                    )
                }
                MiniToolButton(icon = R.drawable.ic_chat_info, label = "Log") {
                    currentVersion?.let { v ->
                        val logFile = VersionsManager.getLatestLog(v)
                        if (logFile.exists()) onNavigateToLog(logFile.absolutePath)
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ChipBadge(text: String, color: Color, borderColor: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}

@Composable
private fun ModernCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(0.85f),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1e1e30),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun MiniToolButton(
    icon: Int,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1e1e30),
        tonalElevation = 2.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = Color(0xFFA78BFA),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xAAFFFFFF), fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}