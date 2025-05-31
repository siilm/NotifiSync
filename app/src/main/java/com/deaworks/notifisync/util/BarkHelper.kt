package com.deaworks.notifisync.util

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.InternalAPI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class BarkNotification(
    val title: String,
    val body: String,
)

enum class AesMode(val value: String, val requiresIv: Boolean) {
    CBC_128("AES-128-CBC", true),
    ECB_128("AES-128-ECB", false),
    CBC_256("AES-256-CBC", true),
    ECB_256("AES-256-ECB", false);
}

const val TAG = "BarkHelper"

@Serializable
data class BarkEncryptedRequest(
    val ciphertext: String,
    val iv: String? = null,
//    val mode: String
)

object BarkHelper {
    suspend fun createNotification(
        context: Context,
        packageName: String,
        title: String,
        body: String
    ) {
        try {
            val notification = BarkNotification(
                title = title,
                body = "$body \n来自:$packageName",
            )

            File(context.filesDir, "connect_config.json").let { file ->
                Json.parseToJsonElement(file.readText()).jsonObject.forEach { (configName, value) ->
                    value.jsonObject["address"]?.jsonPrimitive?.content?.let { address ->
                        if (value.jsonObject["enable"]?.jsonPrimitive?.content.toBoolean()) {
                            return
                        }
                        if (value.jsonObject.containsKey("encrypt")) {
                            sendCipherMsg(
                                address = address,
                                notification = notification,
                                key = getConfigValue(context, configName, "encryptKey"),
                                mode = when (getConfigValue(context, configName, "encrypt")) {
                                    "AES-128-CBC" -> AesMode.CBC_128
                                    "AES-128-ECB" -> AesMode.ECB_128
                                    "AES-256-CBC" -> AesMode.CBC_256
                                    "AES-256-ECB" -> AesMode.ECB_256
                                    else -> AesMode.ECB_128
                                },
                                iv = getConfigValue(context, configName, "encryptIv"),
                                context = context
                            )
                        } else {
                            sendPlainMsg(address, notification, context)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LogSaver.e(TAG, "操作失败: ${e.stackTraceToString()}", context)
        }
    }
}


private suspend fun sendPlainMsg(
    address: String,
    notification: BarkNotification,
    context: Context
) {
    HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }.use { client ->
        try {
            client.post(address) {
                contentType(ContentType.Application.Json)
                setBody(notification)
            }.also {
                LogSaver.i(TAG, "明文发送成功: ${it.status}\n", context)
            }
        } catch (e: Exception) {
            LogSaver.e(TAG, "明文发送失败\n", context, e)
        }
    }
}

@OptIn(InternalAPI::class)
private suspend fun sendCipherMsg(
    address: String,
    notification: BarkNotification,
    key: String,
    mode: AesMode,
    iv: String?,
    context: Context
) {
    HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }.use { client ->
        try {
            val request = BarkEncryptedRequest(
                ciphertext = encryptAes(
                    plaintext = Json.encodeToString(notification),
                    key = key,
                    mode = mode,
                    iv = iv
                ),
                iv = iv?.take(16),
//                mode = mode.value
            )

            client.post(address) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.also {
                LogSaver.i(TAG, "${mode.value}加密发送成功: ${it.status}${it.content}\n", context)
            }
        } catch (e: Exception) {
            LogSaver.e(TAG, "加密发送失败\n", context, e)
        }
    }
}

private fun encryptAes(plaintext: String, key: String, mode: AesMode, iv: String?): String {
    val cipher = Cipher.getInstance(
        when (mode) {
            AesMode.CBC_128, AesMode.CBC_256 -> "AES/CBC/PKCS5Padding"
            AesMode.ECB_128, AesMode.ECB_256 -> "AES/ECB/PKCS5Padding"
        }
    ).apply {
        val keySize = when {
            mode.name.contains("256") -> 32
            else -> 16
        }
        val validKey = key.padEnd(keySize, '0').take(keySize)

        init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(validKey.toByteArray(), "AES"),
            iv?.let { IvParameterSpec(it.take(16).toByteArray()) }
        )
    }

    return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.toByteArray()))
}