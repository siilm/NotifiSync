package com.deaworks.notifisync.util

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.File

object ConnectionConfigManager {
    private val json = Json { prettyPrint = true }
    private val dataChangeListeners = mutableListOf<() -> Unit>()

    /**
     * 保存连接配置到JSON文件
     */
    internal inline fun <reified T : Any> saveConnection(
        connectionType: String,
        connectionName: String,
        encryptOption: Array<String>,
        context: Context,
        config: T
    ) {
        val file = getConfigFile(context)
        when (connectionType) {
            "Bark" -> handleBarkConnection(
                file,
                connectionName,
                encryptOption,
                config,
                connectionType
            )

            else -> handleOtherConnection()
        }
    }

    /**
     * 删除指定连接配置
     */
    fun deleteConnection(context: Context, connectionName: String): Boolean {
        return try {
            val file = getConfigFile(context)
            val currentData = readConnections(file)
            if (currentData.remove(connectionName) != null) {
                writeConnections(file, currentData)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("ConnectionConfig", "删除失败", e)
            false
        }
    }

    /**
     * 读取所有连接配置
     */
    fun readConnections(context: Context): Map<String, JsonObject> {
        return readConnections(getConfigFile(context))
    }

    /**
     * 更新特定字段
     */
    fun <T> updateConnectionField(
        context: Context,
        connectionName: String,
        fieldName: String,
        newValue: T
    ): Boolean {
        return try {
            val file = getConfigFile(context)
            val currentData = readConnections(file)
            currentData[connectionName]?.let { jsonObj ->
                val updatedFields = jsonObj.toMutableMap().apply {
                    put(
                        fieldName, when (newValue) {
                            is Boolean -> JsonPrimitive(newValue)
                            is String -> JsonPrimitive(newValue)
                            is Int -> JsonPrimitive(newValue)
                            else -> {
                                LogSaver.e(TAG, "更改的格式不受支持", context)
                                throw IllegalArgumentException("不受支持的格式: ${(newValue as Any).javaClass.simpleName}")
                            }
                        }
                    )

                }
                currentData[connectionName] = JsonObject(updatedFields)
                writeConnections(file, currentData)
                true
            } == true
        } catch (e: Exception) {
            Log.e("ConnectionConfig", "更新字段失败", e)
            false
        }
    }
    // ==================== 内部实现 ====================

    private fun getConfigFile(context: Context) = File(context.filesDir, "connect_config.json")

    private fun notifyDataChanged() {
        dataChangeListeners.forEach { it.invoke() }
    }

    private inline fun <reified T> handleBarkConnection(
        file: File,
        connectionName: String,
        encryptOption: Array<String>,
        config: T,
        connectionType: String
    ) {
        saveConnectionConfig(file, connectionName, config, connectionType)
        if (encryptOption[0] == "true") {
            appendEncryptionField(
                file = file,
                connectionName = connectionName,
                encryptionType = encryptOption[1]
            )
        }
    }

    private fun handleOtherConnection() {
        Log.d("ConnectionConfig", "还没做")
        TODO()
    }

    private inline fun <reified T> saveConnectionConfig(
        file: File,
        key: String,
        config: T,
        connectionType: String
    ) {
        val currentData = readConnections(file)
        val jsonElement = json.encodeToJsonElement(config).jsonObject.toMutableMap().apply {
            put("connectionType", JsonPrimitive(connectionType))
            put("enable", JsonPrimitive(true))
        }
        currentData[key] = JsonObject(jsonElement)
        writeConnections(file, currentData)
    }

    private fun appendEncryptionField(
        file: File,
        connectionName: String,
        encryptionType: String
    ) {
        val currentData = readConnections(file)
        currentData[connectionName]?.let { jsonObj ->
            val updatedFields = jsonObj.toMutableMap().apply {
                put("encrypt", JsonPrimitive(encryptionType))
            }
            currentData[connectionName] = JsonObject(updatedFields)
            writeConnections(file, currentData)
        }
    }

    private fun readConnections(file: File): MutableMap<String, JsonObject> {
        return if (file.exists() && file.length() > 0) {
            json.decodeFromString(file.readText())
        } else {
            mutableMapOf()
        }
    }

    private fun writeConnections(file: File, data: Map<String, JsonObject>) {
        try {
            file.writeText(json.encodeToString(data))
        } catch (e: Exception) {
            Log.e("ConnectionConfig", "写入失败", e)
        }
    }
}

