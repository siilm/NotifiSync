package com.deaworks.notifisync

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.deaworks.notifisync.ui.theme.NotifiSyncTheme
import java.io.File
import com.deaworks.notifisync.ui.MainPage

class MainActivity : ComponentActivity() {
    override fun onDestroy() {
        //将运行标记设为否
        applicationContext.getSharedPreferences("NotifiListenerPrefs", MODE_PRIVATE).edit {
            putBoolean("service_running", false)
        }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            NotifiSyncTheme {
                MainPage()
            }
        }
        checkFiles()
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
            private set
    }

    private fun checkFiles() {
        val filesName = arrayOf("connect_config.json", "app_settings.json", "package_select.json")
        for (value in filesName) {
            val file = File(filesDir, value)
            if (!file.exists()) {
                if (value == "package_select.json") {
                    file.createNewFile()
                    file.writeText(
                        "{\n" +
                                "   \"blackList\": false,\n" +
                                "   \"pkgNames\": []\n" +
                                "}"
                    )
                } else {
                    file.createNewFile()
                }
            }
        }

    }
}