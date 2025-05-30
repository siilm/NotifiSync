package com.deaworks.notifisync.ui.pages

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AccessControlPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearch by remember { mutableStateOf(false) }  // 控制搜索框显示
    var searchQuery by remember { mutableStateOf("") }    // 搜索关键词

    var apps by remember { mutableStateOf(emptyList<AppInfo>()) }  // 应用列表数据
    var loadingFlag by remember { mutableStateOf(true) } // 正在加载列表数据
    var selectedPkgs by remember { mutableStateOf(emptySet<String>()) }  // 已选包名集合
    var isBlackList by remember { mutableStateOf(false) }  // 当前是否为黑名单模式
    var showSystemApps by remember { mutableStateOf(false) }  // 是否显示系统应用
    var expanded by remember { mutableStateOf(false) }  // 下拉菜单展开状态

    val context = LocalContext.current
    val scope = rememberCoroutineScope()  // 协程作用域

    LaunchedEffect(Unit, showSystemApps, searchQuery) {
        loadingFlag = true // 开始加载
        loadData(context).let { data ->
            isBlackList = data.blackList
            selectedPkgs = data.pkgNames.toSet()
        }
        apps = loadApps(context, showSystemApps).filter { app ->
            searchQuery.isBlank() ||
                    app.name.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
        }

        loadingFlag = false // 结束加载
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AnimatedContent(
                targetState = showSearch,
                transitionSpec = {
                    if (targetState) {
                        // 进入搜索模式：从右滑入
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width / 2 } + fadeOut())
                    } else {
                        // 退出搜索模式：向左滑出
                        (slideInHorizontally { width -> -width / 2 } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                label = "SearchBarTransition"
            ) { searchShowFlag ->
                if (searchShowFlag) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            IconButton(
                                onClick = { showSearch = false },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "返回",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            TextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    scope.launch {
                                        apps = loadApps(context, showSystemApps)
                                            .filter { app ->
                                                searchQuery.isBlank() ||
                                                        app.name.contains(searchQuery, true) ||
                                                        app.packageName.contains(searchQuery, true)
                                            }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("搜索应用...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "清除",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    TopAppBar(
                        title = { Text("应用访问控制") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showSearch = true }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "搜索",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(Icons.Default.MoreVert, "更多选项")
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = isBlackList,
                                                    onCheckedChange = null
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text("黑名单模式")
                                            }
                                        },
                                        onClick = {
                                            isBlackList = !isBlackList
                                            scope.launch {
                                                saveData(
                                                    context,
                                                    isBlackList,
                                                    selectedPkgs
                                                )
                                            }
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = showSystemApps,
                                                    onCheckedChange = null
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text("显示系统应用")
                                            }
                                        },
                                        onClick = {
                                            showSystemApps = !showSystemApps
                                            scope.launch {
                                                apps = loadApps(context, showSystemApps)
                                            }
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("全选") },
                                        onClick = {
                                            selectedPkgs = apps.map { it.packageName }.toSet()
                                            scope.launch {
                                                saveData(
                                                    context,
                                                    isBlackList,
                                                    selectedPkgs
                                                )
                                            }
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("反选") },
                                        onClick = {
                                            selectedPkgs = selectedPkgs.let { current ->
                                                apps.map { it.packageName }.toSet() - current
                                            }
                                            scope.launch {
                                                saveData(
                                                    context,
                                                    isBlackList,
                                                    selectedPkgs
                                                )
                                            }
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(apps) { app ->
                AppItem(
                    app = app,
                    isSelected = app.packageName in selectedPkgs,
                    onToggle = { pkg, selected ->
                        selectedPkgs = selectedPkgs.toMutableSet().apply {
                            if (selected) add(pkg) else remove(pkg)
                        }
                        scope.launch { saveData(context, isBlackList, selectedPkgs) }
                    }
                )
            }
        }

        if (loadingFlag) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

    }
}

/**
 * Drawable转Painter的可组合函数
 */
@Composable
fun rememberDrawablePainter(drawable: Drawable): Painter {
    val bitmap = remember(drawable) {
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    }
    return remember(bitmap) {
        BitmapPainter(bitmap.asImageBitmap())
    }
}


/**
 * 应用列表项组件
 * @param app 应用信息
 * @param isSelected 是否已选中
 * @param onToggle 切换选中状态回调
 */
@Composable
private fun AppItem(
    app: AppInfo,
    isSelected: Boolean,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(app.packageName, !isSelected) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 应用图标
        Image(
            painter = rememberDrawablePainter(drawable = app.icon),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(16.dp))
        // 应用名称和包名
        Column(modifier = Modifier.weight(1f)) {
            Text(app.name, fontWeight = FontWeight.Bold)
            Text(
                app.packageName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        // 选择复选框
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle(app.packageName, it) }
        )
    }
}

/**
 * 应用信息数据类
 * @property name 应用名称
 * @property packageName 包名
 * @property icon 应用图标
 */
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

/**
 * 持久化数据结构（使用kotlinx.serialization序列化）
 * @property blackList 是否为黑名单模式
 * @property pkgNames 已选包名列表
 */
@Serializable
private data class AccessControlData(
    val blackList: Boolean,
    val pkgNames: List<String>
)

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}


/**
 * 保存数据到文件
 */
private fun saveData(context: Context, blackList: Boolean, pkgNames: Set<String>) {
    try {
        val file = File(context.filesDir, "package_select.json")
        file.writeText(json.encodeToString(AccessControlData(blackList, pkgNames.toList())))
    } catch (e: Exception) {
        // 主存储失败时尝试外部存储
        val externalFile = File(context.getExternalFilesDir(null), "package_select.json")
        externalFile.writeText(json.encodeToString(AccessControlData(blackList, pkgNames.toList())))
    }
}

/**
 * 从文件加载数据
 */
private suspend fun loadData(context: Context): AccessControlData {
    return withContext(Dispatchers.IO) {
        // 尝试从两个位置加载文件
        listOf(
            File(context.filesDir, "package_select.json"),
            File(context.getExternalFilesDir(null), "package_select.json")
        ).firstOrNull { it.exists() }?.let { file ->
            try {
                json.decodeFromString(file.readText())
            } catch (e: Exception) {
                null
            }
        } ?: AccessControlData(false, emptyList())  // 默认值
    }
}

/**
 * 加载设备应用列表
 * @param showSystemApps 是否包含系统应用
 */
@SuppressLint("QueryPermissionsNeeded")
private suspend fun loadApps(context: Context, showSystemApps: Boolean): List<AppInfo> {
    return withContext(Dispatchers.IO) {
        val pm = context.packageManager
        pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.MATCH_UNINSTALLED_PACKAGES)
            .asSequence()
            .filter { it.packageName != context.packageName }  // 排除自身
            .filter {
                // 过滤有启动图标或INTERNET权限的应用
                pm.getLaunchIntentForPackage(it.packageName) != null ||
                        it.requestedPermissions?.contains(Manifest.permission.INTERNET) == true
            }
            .filter { showSystemApps || !it.isSystemApp() }  // 系统应用过滤
            .map { it.toAppInfo(pm) }  // 转换为AppInfo
            .sortedBy { it.name }      // 按名称排序
            .toList()
    }
}


/**
 * 判断是否为系统应用
 */
private fun PackageInfo.isSystemApp(): Boolean {
    return (applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0) ||
            (applicationInfo?.flags?.and(ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
}

/**
 * PackageInfo转AppInfo扩展函数
 */
private fun PackageInfo.toAppInfo(pm: PackageManager): AppInfo {
    return AppInfo(
        name = applicationInfo?.loadLabel(pm).toString(),
        packageName = packageName,
        icon = applicationInfo!!.loadIcon(pm)
    )
}