package com.deaworks.notifisync.ui.pages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.deaworks.notifisync.ui.theme.NotifiSyncTheme
import com.deaworks.notifisync.util.ConnectionConfigManager.deleteConnection
import com.deaworks.notifisync.util.ConnectionConfigManager.readConnections
import com.deaworks.notifisync.util.ConnectionConfigManager.updateConnectionField
import com.deaworks.notifisync.util.isAddressAvailable
import com.deaworks.notifisync.util.showToast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("主页")
                },
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val configFile = File(context.filesDir, "connect_config.json")
            if (!configFile.exists() || configFile.readText().isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("目前还没有连接", style = MaterialTheme.typography.titleLarge)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
//                    ConnectionList(readDataFromFile(configFile))
                    var connectData by remember { mutableStateOf(readConnections(context)) }

                    LaunchedEffect(Unit) {
                        // 初始加载数据
                        connectData = readConnections(context)
                    }
                    ConnectionList(
                        connectData = connectData as MutableMap<String, JsonObject>,
                        onConfigUpdated = {
                            connectData = readConnections(context)
                        }
                    )

                }
            }

            ExtendedFloatingActionButton(
                onClick = {
                    showDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, "新建连接按钮")
                Text("新建连接")
            }

            if (showDialog) {
                ConnectConfigPage(
                    onDismiss = { showDialog = false }
                )
            }
        }

    }

}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun ConnectionList(connectData: MutableMap<String, JsonObject>, onConfigUpdated: () -> Unit) {
    val context = LocalContext.current

    LazyColumn {
        itemsIndexed(connectData.toList()) { index, (_, jsonObject) ->
            val isEnable =
                jsonObject["enable"]?.jsonPrimitive?.contentOrNull ?: "Null"
            val connectionName =
                jsonObject["connectionName"]?.jsonPrimitive?.contentOrNull ?: "Null"
            val connectionType =
                jsonObject["connectionType"]?.jsonPrimitive?.contentOrNull ?: "未知错误"
            val isEncrypt = jsonObject["encrypt"]?.jsonPrimitive?.contentOrNull ?: "未加密"

            // 状态管理
            var showMenu by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            var itemState by remember { mutableFloatStateOf(1f) }

            // 长按动画
            val animateScale by animateFloatAsState(
                targetValue = itemState,
                animationSpec = tween(durationMillis = 100)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(animateScale)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                scope.launch {
                                    itemState = 0.95f
                                    delay(100)
                                    itemState = 1f
                                    showMenu = true
                                }
                            }
                        )
                    }
            ) {
                ListItem(
                    headlineContent = {
                        Text(connectionName)
                    },
                    supportingContent = {
                        Text(
                            "状态: ${if (isEnable.toBoolean()) "启用" else "禁用"}, " +
                                    "协议: $connectionType, " +
                                    "加密方式: $isEncrypt"
                        )
                    }
                )

                ConnectionItemMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    onTest = {
                        GlobalScope.launch {
                            isAddressAvailable(
                                jsonObject["address"]?.jsonPrimitive?.contentOrNull ?: "null",
                                context
                            )
                        }
                        "测试连接: $connectionName".showToast(context)
                    },
                    onDelete = {
                        if (deleteConnection(context, connectionName)) {
                            onConfigUpdated()
                        }
                        onConfigUpdated()
                        "已删除: $connectionName".showToast(context)
                    },
                    onEnable = {
                        // 启用逻辑
                        updateConnectionField(
                            context,
                            connectionName = connectionName.toString(),
                            fieldName = "enable",
                            newValue = true,
                        )
                        onConfigUpdated()
                        "已启用: $connectionName".showToast(context)
                    },
                    onDisable = {
                        // 禁用逻辑
                        updateConnectionField(
                            context,
                            connectionName = connectionName.toString(),
                            fieldName = "enable",
                            newValue = false,
                        )
                        onConfigUpdated()
                        "已禁用: $connectionName".showToast(context)
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
fun ConnectionItemMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        DropdownMenuItem(
            text = { Text("启用") },
            onClick = {
                onEnable()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(Icons.Default.Check, contentDescription = "启用")
            }
        )
        DropdownMenuItem(
            text = { Text("禁用") },
            onClick = {
                onDisable()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(Icons.Default.Close, contentDescription = "禁用")
            }
        )
        DropdownMenuItem(
            text = { Text("测试") },
            onClick = {
                onTest()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(Icons.Default.PlayArrow, contentDescription = "测试")
            }
        )
        DropdownMenuItem(
            text = { Text("删除") },
            onClick = {
                onDelete()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
        )
    }
}

@Preview(device = "id:pixel_3a", showBackground = true)
@Composable
fun PreviewDialog() {
    NotifiSyncTheme {
        HomePage()
    }
}