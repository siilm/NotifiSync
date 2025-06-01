package com.deaworks.notifisync.ui.pages

import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.deaworks.notifisync.NotifiListener
import com.deaworks.notifisync.R
import com.deaworks.notifisync.util.ServiceViewModel
import com.deaworks.notifisync.util.showToast
import kotlinx.coroutines.DelicateCoroutinesApi

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun SettingsPage(navController: NavController) {
    val serviceViewModel: ServiceViewModel = viewModel()
    val isServiceRunning by serviceViewModel.isServiceRunning
    val rotationAngle by animateFloatAsState(
        targetValue = if (isServiceRunning) 180f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("设置")
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = Modifier.padding(innerPadding),
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(32.dp)
                        .clickable {
                            navController.navigate("access_control")
                        },
                ) {
                    Icon(
                        painterResource(R.drawable.baseline_filter_list_24),
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                            .align(Alignment.CenterVertically),
                        text = "应用访问控制",
                        fontSize = 20.sp
                    )
                }
//                Row(
//                    modifier = Modifier
//                        .padding(16.dp)
//                        .fillMaxWidth()
//                        .height(32.dp)
//                        .clickable {
//                            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
//                            prefs.edit { putBoolean(KEY_SERVICE_RUNNING, false) }
//                        },
//                ) {
//                    Icon(
//                        Icons.Default.Refresh,
//                        contentDescription = null,
//                        modifier = Modifier.align(Alignment.CenterVertically)
//                    )
//                    Text(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(start = 16.dp)
//                            .align(Alignment.CenterVertically),
//                        text = "[Debug]重置服务运行标记",
//                        fontSize = 20.sp
//                    )
//                }
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(32.dp)
                        .clickable {
                            navController.navigate("solution_page")
                        },
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                            .align(Alignment.CenterVertically),
                        text = "保活服务帮助",
                        fontSize = 20.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(32.dp)
                        .clickable {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                            .align(Alignment.CenterVertically),
                        text = "[Debug]跳转到权限授予界面",
                        fontSize = 20.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(32.dp)
                        .clickable {
                            "信息展示什么的以后再写吧~".showToast(context)
                            "Code by dea116.".showToast(context)
                        },
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                            .align(Alignment.CenterVertically),
                        text = "关于",
                        fontSize = 20.sp
                    )
                }
            }

            ExtendedFloatingActionButton(
                onClick = {
                    if (context is ComponentActivity) {
                        NotifiListener.checkPermissions(context) {
                            if (isServiceRunning) {
                                //停止服务
//                                NotifiListener.stopService(context)
                                serviceViewModel.updateServiceState(false)
                                "待写".showToast(context)
                            } else {
                                //启动服务
                                NotifiListener.startService(context)
                                serviceViewModel.updateServiceState(true)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),

                icon = {
                    Crossfade(targetState = isServiceRunning, label = "IconAnimation") { running ->
                        if (running) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_stop_24),
                                contentDescription = "停止服务",
                                modifier = Modifier.rotate(rotationAngle)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "启动服务",
                                modifier = Modifier.rotate(rotationAngle)
                            )
                        }
                    }
                },

                text = {
                    Text(if (isServiceRunning) "停止服务" else "启动服务")
                }
            )
        }

    }
}

//@Preview
//@Composable
//fun MainPagePreview() {
//    NotifiSyncTheme {
//        SettingsPage()
//    }
//}