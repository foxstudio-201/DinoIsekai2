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
import androidx.compose.foundation.border
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.ServerPing
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.AccountType
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersions
import com.movtery.zalithlauncher.game.dinostate.DinoStateSync
import com.movtery.zalithlauncher.game.dinostate.DinoStateType
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.download.game.GameInstaller
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.TitleTaskFlowDialog
import com.movtery.zalithlauncher.ui.screens.removeAndNavigateTo
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import kotlinx.coroutines.launch

private const val FIXED_VERSION_NAME = "1.20.1-Forge"
private const val DINO_SERVER_ADDRESS = "160.250.134.97:3026"

/** Cấu hình auto-join server cho version cố định rồi trả về version đã cấu hình. */
private fun prepareFixedVersion(version: Version): Version {
    runCatching {
        if (version.getVersionConfig().serverIp != DINO_SERVER_ADDRESS) {
            version.getVersionConfig().serverIp = DINO_SERVER_ADDRESS
            version.getVersionConfig().save()
        }
    }.onFailure { e ->
        com.movtery.zalithlauncher.utils.logging.Logger.error("LauncherScreen", "Failed to set server IP", e)
    }
    return version
}

@Composable
fun LauncherScreen(
    backStackViewModel: ScreenBackStackViewModel,
    onLaunchGame: (Version?) -> Unit,
) {
    BaseScreen(
        screenKey = NormalNavKey.LauncherMain,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        DinoHomepage(
            isVisible = isVisible,
            backStackViewModel = backStackViewModel,
            onLaunchGame = onLaunchGame,
        )
    }
}

@Composable
private fun DinoHomepage(
    isVisible: Boolean,
    backStackViewModel: ScreenBackStackViewModel,
    onLaunchGame: (Version?) -> Unit,
) {
    val accounts by AccountsManager.accountsFlow.collectAsState()
    val accCurrent by AccountsManager.currentAccountFlow.collectAsState()
    val currentVersion by VersionsManager.currentVersion.collectAsState()

    var username by remember { mutableStateOf(accCurrent?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var pingResult by remember { mutableStateOf<com.movtery.zalithlauncher.game.PingResult?>(null) }

    var installing by remember { mutableStateOf(false) }
    var installer by remember { mutableStateOf<GameInstaller?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dinoSyncStatus by remember { mutableStateOf<String?>(null) }
    var dinoSyncing by remember { mutableStateOf(false) }
    var dinoSyncPercent by remember { mutableStateOf(0) }
    var dinoSyncMessage by remember { mutableStateOf("") }
    val dinoScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            pingResult = com.movtery.zalithlauncher.game.ServerPing.ping("160.250.134.97", 3026)
            kotlinx.coroutines.delay(8000)
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        dinoScope.launch {
            dinoSyncStatus = null
            var version: Version? = null
            try {
                repeat(3) {
                    VersionsManager.refresh("[Home] dino", FIXED_VERSION_NAME)
                    VersionsManager.waitForRefresh()
                    version = VersionsManager.getVersion(FIXED_VERSION_NAME)
                    if (version != null) return@repeat
                    kotlinx.coroutines.delay(1500)
                }
                val gameDir = version?.getGameDir()
                if (gameDir == null) {
                    dinoSyncStatus = "Chưa cài game, bỏ qua cập nhật dữ liệu"
                    return@launch
                }

                dinoSyncing = true
                var baseDone = false
                val results = mutableListOf<String>()
                DinoStateType.entries.forEach { type ->
                    DinoStateSync.runSync(type, gameDir) { p ->
                        if (p.phase == "done") {
                            dinoSyncStatus = p.message
                            dinoSyncMessage = ""
                            dinoSyncPercent = 0
                            results.add(p.message)
                            if (type == DinoStateType.BASE) baseDone = true
                        } else {
                            if (baseDone) {
                                dinoSyncStatus = results.lastOrNull()
                            }
                            dinoSyncMessage = p.message
                            dinoSyncPercent = p.percent
                        }
                    }
                }
                dinoSyncStatus = results.distinct().joinToString(" · ")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                dinoSyncStatus = "Không thể đồng bộ dữ liệu: ${e.message ?: "lỗi không xác định"}"
            } finally {
                dinoSyncing = false
                dinoSyncMessage = ""
                dinoSyncPercent = 0
            }
        }
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

            Spacer(Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (pingResult?.online == true) Color(0xFF34D399)
                            else Color(0xFFFF5252)
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        pingResult == null -> "Đang ping..."
                        pingResult?.online == true -> "Online"
                        else -> "Offline"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (pingResult?.online == true) Color(0xFF34D399) else Color(0xFFFF5252)
                    )
                )
                if (pingResult?.online == true) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "· 1.20.1 Forge",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xAAFFFFFF)
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${pingResult?.ping ?: 0} ms",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF60A5FA)
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${pingResult?.players ?: 0}/${pingResult?.maxPlayers ?: 0}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xAAFFFFFF)
                        )
                    )
                }
            }

            if (dinoSyncing || dinoSyncStatus != null) {
                Spacer(Modifier.height(10.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (dinoSyncing) {
                        Text(
                            text = "Dữ liệu: $dinoSyncMessage${if (dinoSyncPercent > 0) " $dinoSyncPercent%" else ""}",
                            color = Color(0xFFA78BFA),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                    }
                    dinoSyncStatus?.let { status ->
                        Text(
                            text = status,
                            color = Color(0xFFFACC15),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            ModernCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Yêu cầu cấu hình",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xAAFFFFFF),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x22FFFFFF))
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0x14FFFFFF), RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Tối thiểu", style = MaterialTheme.typography.labelSmall.copy(color = Color(0x99FFFFFF), fontSize = 11.sp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("6GB RAM", style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            Text("Snap 7 Gen 1", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xAAFFFFFF)))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0x2A7C3AED), RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, Color(0x667C3AED)), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Đề xuất", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFC4B5FD), fontSize = 11.sp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("12GB RAM", style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            Text("Snap 8 Gen 3", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xAAFFFFFF)))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

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
                            painter = painterResource(R.drawable.dino_ic_player),
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

            Spacer(Modifier.height(16.dp))

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
                            painter = painterResource(if (showPass) R.drawable.dino_ic_eye_off else R.drawable.dino_ic_eye),
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
                            painter = if (showPass) painterResource(R.drawable.dino_ic_eye_off)
                                else painterResource(R.drawable.dino_ic_eye),
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

            Spacer(Modifier.height(24.dp))

            Surface(
                modifier = Modifier
                    .scale(playScale)
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF7C3AED),
                onClick = {
                    if (installing) return@Surface
                    if (username.isBlank()) { errorMsg = "Nhập tên người chơi!"; return@Surface }
                    if (password.isBlank()) { errorMsg = "Nhập mật khẩu!"; return@Surface }
                    if (username.length < 3 || username.length > 16) { errorMsg = "Tên 3-16 ký tự"; return@Surface }
                    if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) { errorMsg = "Chỉ dùng a-z, 0-9, _"; return@Surface }
                    errorMsg = null
                    val existing = accounts.find { it.username == username && it.accountType == AccountType.LOCAL.tag }
                    if (existing != null) {
                        AccountsManager.setCurrentAccount(existing)
                    }
                    val ver = VersionsManager.getVersion(FIXED_VERSION_NAME)
                    if (ver != null && ver.isValid()) {
                        onLaunchGame(prepareFixedVersion(ver))
                    } else {
                        errorMsg = null
                        installing = true
                        scope.launch {
                            try {
                                if (existing == null) {
                                    val acc = com.movtery.zalithlauncher.game.account.Account(
                                        username = username,
                                        accountType = AccountType.LOCAL.tag
                                    )
                                    AccountsManager.suspendSaveAccount(acc)
                                }
                                val forgeList = ForgeVersions.fetchForgeList("1.20.1")
                                val forge = forgeList?.firstOrNull { it.isRecommended }
                                    ?: forgeList?.firstOrNull()
                                if (forge == null) {
                                    errorMsg = "Không tìm thấy Forge cho 1.20.1"
                                    installing = false
                                    return@launch
                                }
                                val info = GameDownloadInfo(
                                    gameVersion = "1.20.1",
                                    customVersionName = FIXED_VERSION_NAME,
                                    overwrite = false,
                                    forge = forge
                                )
                                val inst = GameInstaller(context, info, scope)
                                installer = inst
                                inst.installGame(
                                    isRunning = {},
                                    onInstalled = { installedName ->
                                        installing = false
                                        installer = null
                                        scope.launch {
                                            VersionsManager.refresh("[Home] installed", installedName)
                                            VersionsManager.waitForRefresh()
                                            val installed = VersionsManager.getVersion(installedName)
                                            if (installed != null && installed.isValid()) {
                                                VersionsManager.saveVersion(installed)
                                                onLaunchGame(prepareFixedVersion(installed))
                                            } else {
                                                errorMsg = "Đã cài xong nhưng không mở được game"
                                            }
                                        }
                                    },
                                    onError = { th ->
                                        installing = false
                                        installer = null
                                        errorMsg = th.message ?: "Lỗi cài đặt game"
                                    },
                                    onGameAlreadyInstalled = {
                                        installing = false
                                        installer = null
                                        scope.launch {
                                            VersionsManager.refresh("[Home] refresh", FIXED_VERSION_NAME)
                                            VersionsManager.waitForRefresh()
                                            val installed = VersionsManager.getVersion(FIXED_VERSION_NAME)
                                            if (installed != null && installed.isValid()) {
                                                VersionsManager.saveVersion(installed)
                                                onLaunchGame(prepareFixedVersion(installed))
                                            } else {
                                                errorMsg = "Game đã tồn tại nhưng không mở được"
                                            }
                                        }
                                    }
                                )
                            } catch (th: Throwable) {
                                installing = false
                                errorMsg = th.message ?: "Lỗi cài đặt game"
                            }
                        }
                    }
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

            Spacer(Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniToolButton(icon = R.drawable.dino_ic_settings, label = "Cài đặt") {
                    backStackViewModel.mainScreen.removeAndNavigateTo(
                        remove = NestedNavKey.Settings::class,
                        screenKey = NestedNavKey.Settings()
                    )
                }
                MiniToolButton(icon = R.drawable.ic_videocam_filled, label = "Video") {
                    backStackViewModel.mainScreen.removeAndNavigateTo(
                        remove = NestedNavKey.Settings::class,
                        screenKey = NormalNavKey.Recordings
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }

        installer?.let { inst ->
            val tasks by inst.tasksFlow.collectAsStateWithLifecycle()
            if (installing || tasks.isNotEmpty()) {
                TitleTaskFlowDialog(
                    title = "Đang cài đặt Minecraft 1.20.1 + Forge",
                    tasks = tasks,
                    onCancel = {
                        inst.cancelInstall()
                        installing = false
                    }
                )
            }
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
        modifier = Modifier.fillMaxWidth(0.9f),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1e1e30),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Box(modifier = Modifier.padding(18.dp)) { content() }
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = Color(0xFFA78BFA),
                modifier = Modifier.size(22.dp)
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