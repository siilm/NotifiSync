package com.deaworks.notifisync.ui.pages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.deaworks.notifisync.ui.theme.NotifiSyncTheme
import com.deaworks.notifisync.util.ConnectionConfigManager.saveConnection
import com.deaworks.notifisync.util.showToast
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionConfig(
    var connectionName: String = "",
    var connectionType: String = "Bark",
    var address: String = "",
    var encryptKey: String = "",
    var encryptIv: String = "",
)

data class ComponentsData(

    val dropMenuSelection: List<String> = listOf(
        "AES-128-CBC",
        "AES-128-ECB",
        "AES-256-CBC",
        "AES-256-ECB"
    ),
    var dropMenuFlag: Boolean = false,

    var switchToggled: Boolean = true,
    var encryptFlag: Boolean = true,
    val encryptRegex: List<String> = listOf(
        "^[A-Za-z0-9]{16}\$",
        "^[A-Za-z0-9]{32}\$",
    ),
    var isAES128: Boolean = true,
)

@Composable
fun ConnectConfigPage(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var errorFlag = false
    var connectionConfig by remember { mutableStateOf(ConnectionConfig()) }
    var componentsData by remember { mutableStateOf(ComponentsData()) }
    var selectedOption by remember { mutableStateOf(componentsData.dropMenuSelection[0]) }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Dialog(onDismissRequest = { }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "新建连接", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = connectionConfig.connectionName,
                        onValueChange = {
                            connectionConfig = connectionConfig.copy(connectionName = it)
                        },
                        label = { Text("连接名称") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = connectionConfig.address,
                        onValueChange = { connectionConfig = connectionConfig.copy(address = it) },
                        label = { Text("Bark服务地址") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    Row(
                        Modifier
                            .padding(0.dp)
                            .fillMaxWidth(),
                        Arrangement.Start,
                        Alignment.CenterVertically
                    )
                    {
                        Text("启用加密", modifier = Modifier.padding(4.dp))
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = componentsData.switchToggled,
                            onCheckedChange = { active ->
                                componentsData = componentsData.copy(switchToggled = active)
                                componentsData.encryptFlag = if (!active) {
                                    //当关闭开关时清除所有数据并提示
                                    connectionConfig.encryptKey = ""
                                    connectionConfig.encryptIv = ""
                                    "建议加密，请在Bark内配置".showToast(context)
                                    if (!componentsData.switchToggled) {
                                        //下拉菜单回归选项
                                        selectedOption =
                                            componentsData.dropMenuSelection[0]
                                    }
                                    false //关闭加密配置
                                } else {
                                    connectionConfig.encryptKey = ""
                                    connectionConfig.encryptIv = ""
                                    true //启用加密配置
                                }
                            },
                        )
                    }
                    //加密配置
                    if (componentsData.encryptFlag) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            Arrangement.Center,
                            Alignment.Start
                        ) {
                            Box {
                                //下拉菜单触发按钮
                                OutlinedButton(
                                    onClick = {
                                        componentsData = componentsData.copy(dropMenuFlag = true)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(10)
                                ) {
                                    Text(selectedOption)
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                }
                                //下拉菜单
                                DropdownMenu(
                                    expanded = componentsData.dropMenuFlag,
                                    onDismissRequest = {
                                        componentsData = componentsData.copy(dropMenuFlag = false)
                                    }
                                ) {
                                    componentsData.dropMenuSelection.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                selectedOption = option
                                                componentsData = componentsData.copy(
                                                    isAES128 = option.startsWith("AES-128"),
                                                    dropMenuFlag = false
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            //填写格式错误信息
                            val isError by remember { mutableStateOf(arrayOf(false, false)) }
                            var errorMessage by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = connectionConfig.encryptKey,
                                onValueChange = { inputValue ->
                                    connectionConfig = connectionConfig.copy(encryptKey = inputValue)
                                    isError[0] = !inputValue.matches(
                                        Regex(
                                            if (componentsData.isAES128) {
                                                //选择为AES128加密时
                                                "^[A-Za-z0-9]{16}\$"
                                            } else {
                                                //选择为非AES128加密时
                                                "^[A-Za-z0-9]{32}\$"
                                            }
                                        )
                                    )
                                    //显示的错误信息
                                    errorMessage =
                                        if (isError[0] && componentsData.isAES128
                                        ) {
                                            errorFlag = true
                                            "长度应为16位的英文/数字组成"
                                        } else {
                                            errorFlag = true
                                            "长度应为32位的英文/数字组成"
                                        }
                                },
                                label = { Text("Key 值") },
                                isError = isError[0],
                                modifier = Modifier
                                    .fillMaxWidth(),
                                singleLine = true
                            )
                            if (isError[0]) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                            OutlinedTextField(
                                value = connectionConfig.encryptIv,
                                onValueChange = { inputValue ->
                                    connectionConfig = connectionConfig.copy(encryptIv = inputValue)
                                    isError[1] = !inputValue.matches(
                                        Regex(
                                            if (componentsData.isAES128) {
                                                //选择为AES128加密时
                                                "^[A-Za-z0-9]{16}\$"
                                            } else {
                                                //选择为非AES128加密时
                                                "^[A-Za-z0-9]{32}\$"
                                            }
                                        )
                                    )
                                    //显示的错误信息
                                    errorMessage = if (isError[1] && componentsData.isAES128
                                    ) {
                                        errorFlag = true
                                        "长度应为16位的英文/数字组成"
                                    } else {
                                        errorFlag = true
                                        "长度应为32位的英文/数字组成"
                                    }
                                },
                                label = { Text("偏移值") },
                                isError = isError[1],
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                            if (isError[1]) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }

                        }
                    }


                    //对话框最末
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = {
                            //取消按钮
                            onDismiss()
                        }) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            //确认按钮
                            saveConnection(
//                                "Bark",
                                connectionConfig.connectionType,
                                connectionConfig.connectionName,
                                arrayOf(componentsData.encryptFlag.toString(), selectedOption),
                                context,
                                connectionConfig
                            )
                            onDismiss()
                        }) {
                            Text("确认")
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewConnectPage() {
    NotifiSyncTheme {
        ConnectConfigPage(onDismiss = { Log.v("ConfigPreview", "预览") })
    }
}
