package com.deaworks.notifisync.util

import android.content.Context
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

fun checkPkgInConfig(jsonFile: File, pkgName: String): Boolean {
    if (!jsonFile.exists() || jsonFile.length() == 0L) {
        return false
    }
    return try {
        val jsonContent = jsonFile.readText()
        val jsonObject = Json.parseToJsonElement(jsonContent).jsonObject
        val jsonArray = jsonObject["pkgNames"] as? JsonArray

        jsonArray?.any { it.jsonPrimitive.content == pkgName } == false
    } catch (e: Exception) {
        Log.e(TAG, "解析 JSON 失败: ${e.message}")
        false
    }
}

fun isConfigEmpty(context: Context): Boolean {
    val file = File(context.filesDir, "connect_config.json")
        .readText().isBlank()
    return file
}

suspend fun isAddressAvailable(address: String, context: Context): Boolean {
    val client = HttpClient(CIO)
    try {
        val response = client.get("${address}手动发送测试消息")
        when (response.status) {
            HttpStatusCode.BadRequest -> {
                LogSaver.i("ServerAddressChecker", "服务器地址可用", context)
                return true
            }

            else -> {
                LogSaver.e("ServerAddressChecker", "返回状态码: ${response.status}", context)
                return false
            }
        }
    } catch (e: Exception) {
        LogSaver.e("ServerAddressChecker", "客户端发起请求失败: ${e.message}", context)
        return false
    } finally {
        client.close()
    }
}

fun getConfigValue(context: Context, configName: String, fieldName: String): String {
    val file = File(context.filesDir, "connect_config.json")
    val jsonObject = Json.parseToJsonElement(file.readText()).jsonObject

    val configObj = jsonObject[configName]?.jsonObject ?: return "配置不存在"
    return configObj[fieldName]?.jsonPrimitive?.content ?: "字段不存在"
}
