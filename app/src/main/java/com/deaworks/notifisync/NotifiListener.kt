package com.deaworks.notifisync

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.deaworks.notifisync.util.LogSaver
import com.deaworks.notifisync.util.checkPkgInConfig
import com.deaworks.notifisync.util.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import com.deaworks.notifisync.util.BarkHelper
import androidx.core.content.edit
import com.deaworks.notifisync.util.getConfigValue
import com.deaworks.notifisync.util.isConfigEmpty

class NotifiListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotifiListener"
        const val PREFS_NAME = "NotifiListenerPrefs"  // SharedPreferences 文件名
        const val KEY_SERVICE_RUNNING = "service_running"  // 标记是否服务已经启动

        fun startService(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val isServiceRunning = prefs.getBoolean(KEY_SERVICE_RUNNING, false)
            val isConfigEmpty = isConfigEmpty(context)
            val intent = Intent(context, NotifiListener::class.java)

            //如果服务已经在运行，则不再启动
            if (isServiceRunning || isConfigEmpty) {
                LogSaver.i(TAG, "服务已在运行或没有配置连接", context)
                "服务已在运行或没有配置连接".showToast(context)
                return
            }
            //如果服务未运行，则启动并标记服务已运行
            prefs.edit { putBoolean(KEY_SERVICE_RUNNING, true) }
            LogSaver.i(TAG, "正在启动通知监听服务...", context)
            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                LogSaver.e(TAG, "启动服务时发生错误: ${e.message}", context, e)
            }
        }

        fun stopService(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
//            val isServiceRunning = prefs.getBoolean(KEY_SERVICE_RUNNING, false)
//            // 如果服务未运行，直接返回
//            if (!isServiceRunning) {
//                LogSaver.i(TAG, "服务未运行，无需停止")
//                return
//            }

            try {
                //停止前台服务
                val intent = Intent(context, NotifiListener::class.java)
                context.stopService(intent)
                //更新状态标记
                prefs.edit {
                    putBoolean(KEY_SERVICE_RUNNING, false)
                    apply() //同步写入
                }
                LogSaver.i(TAG, "服务已停止", context)
                "关闭按钮点击".showToast(context)
            } catch (e: Exception) {
                LogSaver.e(TAG, "停止服务时发生错误: ${e.message}", context, e)
                "停止服务失败".showToast(context)
            }
        }


        fun checkPermissions(
            context: Context, onPermissionsGranted: () -> Unit
        ): Boolean {
            //通知读取权限
            val hasNotifiReadPermission = Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            ).contains(context.packageName)

            //通知发送权限
            val hasNotifiPostPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            return when {
                !hasNotifiReadPermission -> {
                    "请授予通知读取权限".showToast(context)
                    Log.i(TAG, "请求获取通知权限...")
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

                    ActivityCompat.requestPermissions(
                        context as Activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2
                    )
                    false
                }

                !hasNotifiPostPermission -> {
                    Log.i(TAG, "请求通知发送权限...")
                    ActivityCompat.requestPermissions(
                        context as Activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2
                    )
                    false
                }

                else -> {
                    LogSaver.i(TAG, "权限齐全, 尝试启动", context)
                    onPermissionsGranted()
                    true
                }
            }
        }
    }

    //通知队列
    private val notificationQueue: BlockingQueue<StatusBarNotification> = ArrayBlockingQueue(20)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
            putBoolean(KEY_SERVICE_RUNNING, true)
        }
        return START_STICKY // 系统会尝试重启服务
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        LogSaver.e(TAG, "通知监听服务已断开", applicationContext)

        //清除服务运行标记
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_SERVICE_RUNNING, false) }
        //重新启动服务
        val restartServiceRequest = OneTimeWorkRequest.Builder(ServiceDaemon::class.java).build()
        WorkManager.getInstance(this).enqueue(restartServiceRequest)
    }

    override fun onDestroy() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
            putBoolean(KEY_SERVICE_RUNNING, false)
        }
        LogSaver.i(TAG, "服务被退出", applicationContext)
        super.onDestroy()
    }

    override fun onCreate() {
        super.onCreate()
        "通知监听服务启动".showToast(this)
        //启动一个协程来处理队列中的通知
        praisingNotifications()

        val channelId = "notification_listener_channel"
        val channel = NotificationChannel(
            channelId, "通知监听服务", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "监听服务正运行" }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification = Notification.Builder(this, channelId).setContentTitle("服务正在运行")
            .setContentText("正在监听通知").setSmallIcon(R.drawable.ic_launcher_foreground).build()


        startForeground(1, notification)
    }

    // 启动通知处理协程
    private fun praisingNotifications() {
        coroutineScope.launch {
            while (isActive) {
                val sbn = notificationQueue.take() // 阻塞直到队列中有通知
                withContext(Dispatchers.Main) {
                    // 获取通知信息
                    val extras = sbn.notification.extras
                    val title = extras.getCharSequence("android.title").toString()
                    val text = extras.getCharSequence("android.text").toString()

                    LogSaver.i(
                        TAG,
                        "通知来源: ${sbn.packageName},\n标题: $title, 内容: $text",
                        applicationContext
                    )
                    if (sbn.packageName != "com.deaworks.notifisync") {
                        BarkHelper.createNotification(
                            applicationContext,
                            sbn.packageName,
                            title,
                            text
                        )
                    }
                }
                delay(500)
            }
        }
    }

    //接收到通知时
    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        super.onNotificationPosted(sbn, rankingMap)
        val packageName = sbn?.packageName
        LogSaver.d(TAG, "捕获到通知: $packageName", applicationContext)

        val selectedPackages = File(applicationContext.filesDir, "package_select.json")

        if (getConfigValue(
                applicationContext, "package_select.json", "blacklist"
            ).toBoolean()
        ) {
            /**
             * 跳过未指定应用的通知-白名单模式
             * 当包名不在json内时return
             */
            if (!checkPkgInConfig(selectedPackages, packageName.toString())) {
                LogSaver.i(TAG, "不是白名单成员: $packageName", applicationContext)
                return
            }
        } else {
            /**
             * 跳过指定应用的通知-黑名单模式
             * 当包名在json内时return
             */
            if (checkPkgInConfig(selectedPackages, packageName.toString())) {
                LogSaver.i(TAG, "不是白名单成员: $packageName", applicationContext)
                return
            }

        }

        sbn?.let {
            if (!notificationQueue.offer(it)) {
                LogSaver.e(
                    TAG, "队列已满，无法添加新的通知: ${it.packageName}",
                    applicationContext
                )
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        LogSaver.i(
            TAG, "移除来自: ${sbn.packageName} 的通知\n标题: ${
                sbn.notification.extras.getString(
                    "android.title", ""
                )
            } 被移除", applicationContext
        )
    }
}