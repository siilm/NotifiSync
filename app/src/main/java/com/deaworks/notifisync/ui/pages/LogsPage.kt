package com.deaworks.notifisync.ui.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * 日志查看页面
 * 主要功能：
 * - 查看应用日志
 * - 搜索日志内容
 * - 滚动控制
 * - 清空日志
 * - 自动换行切换
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun LogsPage(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logFile = remember {
        File(context.cacheDir, "app_log.log").apply {
            if (!exists()) createNewFile()
        }
    }

    // 状态管理
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var wrapContent by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // 读取并过滤日志内容
    val logContent by remember(logFile) {
        derivedStateOf { logFile.readText() }
    }
    val filteredContent by remember(logContent, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) logContent
            else logContent.lines()
                .filter { it.contains(searchQuery, ignoreCase = true) }
                .joinToString("\n")
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            // 动态切换搜索框和普通标题栏
            AnimatedContent(
                targetState = showSearch,
                transitionSpec = {
                    // 搜索框滑入滑出动画
                    if (targetState) {
                        (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> -width / 2 } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width / 2 } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> width } + fadeOut())
                    }
                }
            ) { isSearchVisible ->
                if (isSearchVisible) {
                    SearchBar(
                        searchQuery = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onClose = { showSearch = false }
                    )
                } else {
                    DefaultAppBar(
                        onSearchClick = { showSearch = true },
                        onMenuClick = { expanded = true }
                    )
                }
            }
        }
    ) { innerPadding ->
        val coroutineScope = rememberCoroutineScope()

        Box(modifier = Modifier.padding(innerPadding)) {
            if (logContent.isEmpty()) {
                EmptyLogPlaceholder()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    LogContent(
                        content = filteredContent,
                        wrapContent = wrapContent
                    )
                }
            }
        }

        // 清空日志确认对话框
        if (showClearDialog) {
            ClearLogDialog(
                onConfirm = {
                    logFile.writeText("")
                    showClearDialog = false
                },
                onDismiss = { showClearDialog = false }
            )
        }

        // 下拉菜单
        LogsDropdownMenu(
            expanded = expanded,
            onDismiss = { expanded = false },
            actions = listOf(
                LogAction.ScrollToTop(verticalScrollState),
                LogAction.ScrollToBottom(verticalScrollState),
                LogAction.ToggleWordWrap(wrapContent) { wrapContent = it },
                LogAction.ClearLog { showClearDialog = true }
            ),
            coroutineScope = coroutineScope
        )
    }
}

/**
 * 搜索框组件
 */
@Composable
private fun SearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, "关闭搜索")
            }

            TextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索日志...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, "清除搜索")
                }
            }
        }
    }
}

/**
 * 默认应用栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultAppBar(
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    TopAppBar(
        title = { Text("日志", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
//            IconButton(onClick = onBack) {
//                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
//            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, "搜索")
            }

            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .wrapContentSize()
                    .padding(end = 8.dp)
            ) { Icon(Icons.Filled.MoreVert, contentDescription = "更多选项") }

        }
    )
}

/**
 * 空日志占位提示
 */
@Composable
private fun EmptyLogPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "目前没有日志",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

/**
 * 日志内容显示
 */
@Composable
private fun LogContent(
    content: String,
    wrapContent: Boolean
) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .padding(16.dp)
//            .verticalScroll(scrollState)
            .fillMaxWidth(),
        softWrap = wrapContent,
        overflow = if (wrapContent) TextOverflow.Clip else TextOverflow.Visible
    )
}

/**
 * 清空日志确认对话框
 */
@Composable
private fun ClearLogDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        icon = { Icon(Icons.Default.Warning, null) },
        title = { Text("确定要清空日志吗？") },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 日志操作下拉菜单
 */
@Composable
private fun LogsDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    actions: List<LogAction>,
    coroutineScope: CoroutineScope
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(IntrinsicSize.Max),
        offset = DpOffset(x = (-16).dp, y = 0.dp)

    ) {
        actions.forEach { action ->
            when (action) {
                is LogAction.ScrollToTop -> {
                    DropdownMenuItem(
                        text = { Text("滚动到顶部") },
                        onClick = {
                            coroutineScope.launch {
                                action.verticalScrollState.scrollTo(0)
                            }
                            onDismiss()
                        },
//                        leadingIcon = {
//                            Icon(Icons.Default.KeyboardArrowUp, null)
//                        }
                    )
                }

                is LogAction.ScrollToBottom -> {
                    DropdownMenuItem(
                        text = { Text("滚动到底部") },
                        onClick = {
                            coroutineScope.launch {
                                action.verticalScrollState.scrollTo(action.verticalScrollState.maxValue)
                            }
                            onDismiss()
                        },
//                        leadingIcon = {
//                            Icon(Icons.Default.KeyboardArrowDown, null)
//                        }
                    )
                }

                is LogAction.ToggleWordWrap -> {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = action.currentValue,
                                    onCheckedChange = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("自动换行")
                            }
                        },
                        onClick = {
                            action.onToggle(!action.currentValue)
                            onDismiss()
                        }
                    )
                }

                is LogAction.ClearLog -> {
                    DropdownMenuItem(
                        text = { Text("清空日志") },
                        onClick = {
                            action.onClick()
                            onDismiss()
                        },
//                        leadingIcon = {
//                            Icon(Icons.Default.Delete, null)
//                        }
                    )
                }
            }
        }
    }
}

// endregion

// region Models

/**
 * 日志操作密封类
 */
private sealed class LogAction {
    class ScrollToTop(val verticalScrollState: androidx.compose.foundation.ScrollState) : LogAction()
    class ScrollToBottom(val verticalScrollState: androidx.compose.foundation.ScrollState) : LogAction()
    class ToggleWordWrap(
        val currentValue: Boolean,
        val onToggle: (Boolean) -> Unit
    ) : LogAction()

    class ClearLog(val onClick: () -> Unit) : LogAction()
}

// endregion