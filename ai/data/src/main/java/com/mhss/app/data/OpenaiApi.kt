package com.mhss.app.data

import com.mhss.app.data.model.openai.OpenaiMessage
import com.mhss.app.data.model.openai.OpenaiMessageRequestBody
import com.mhss.app.data.model.openai.OpenaiResponse
import com.mhss.app.data.model.openai.toAiMessage
import com.mhss.app.data.model.openai.toOpenAiRequestBody
import com.mhss.app.domain.model.AiMessage
import com.mhss.app.domain.repository.AiApi
import com.mhss.app.network.NetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonPrimitive

@Single
@Named("openaiApi")
class OpenaiApi(
    private val client: HttpClient,
    @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) : AiApi {

    // =========================
    // ===== TEXT PROMPT ======
    // =========================

    override suspend fun sendPrompt(
        baseUrl: String,
        apiKey: String,
        prompt: String,
        model: String
    ): NetworkResult<String> = withContext(ioDispatcher) {
        val key = apiKey.trim()
        if (key.isBlank()) {
            return@withContext NetworkResult.InvalidKey
        }

        val response: HttpResponse = client.post(baseUrl.trim()) {
            url { appendPathSegments("chat", "completions") }
            contentType(ContentType.Application.Json)
            bearerAuth(key)
            setBody(
                OpenaiMessageRequestBody(
                    model = model,
                    messages = listOf(
                        OpenaiMessage(
                            role = NetworkConstants.OPENAI_MESSAGE_USER_TYPE,
                            content = JsonPrimitive(prompt)
                        )
                    )
                )
            )
        }

        if (response.status == HttpStatusCode.Unauthorized) {
            return@withContext NetworkResult.InvalidKey
        }
        if (response.status != HttpStatusCode.OK) {
            return@withContext NetworkResult.OtherError(
                "HTTP ${response.status.value}: ${response.bodyAsText()}"
            )
        }

        val result = response.body<OpenaiResponse>()
        return@withContext when {
            result.error != null && result.error.message.contains("API key") ->
                NetworkResult.InvalidKey

            result.error != null ->
                NetworkResult.OtherError(result.error.message)

            else ->
                NetworkResult.Success(
                    result.choices!!.first().message.content.jsonPrimitive.content
                )
        }
    }

    // =========================
    // ===== CHAT MESSAGES =====
    // =========================

    override suspend fun sendMessage(
        baseUrl: String,
        apiKey: String,
        messages: List<AiMessage>,
        model: String
    ): NetworkResult<AiMessage> = withContext(ioDispatcher) {
        val key = apiKey.trim()
        if (key.isBlank()) {
            return@withContext NetworkResult.InvalidKey
        }

        val response: HttpResponse = client.post(baseUrl.trim()) {
            url { appendPathSegments("chat", "completions") }
            contentType(ContentType.Application.Json)
            bearerAuth(key)
            setBody(messages.toOpenAiRequestBody(model))
        }

        if (response.status == HttpStatusCode.Unauthorized) {
            return@withContext NetworkResult.InvalidKey
        }
        if (response.status != HttpStatusCode.OK) {
            return@withContext NetworkResult.OtherError(
                "HTTP ${response.status.value}: ${response.bodyAsText()}"
            )
        }

        val result = response.body<OpenaiResponse>()
        return@withContext when {
            result.error != null && result.error.message.contains("API key") ->
                NetworkResult.InvalidKey

            result.error != null ->
                NetworkResult.OtherError(result.error.message)

            else ->
                NetworkResult.Success(
                    result.choices!!.first().message.toAiMessage()
                )
        }
    }

    // =========================
    // ===== OPENAI VISION =====
    // =========================

    override suspend fun sendVisionPrompt(
        baseUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
        imageBase64: String
    ): NetworkResult<String> = withContext(ioDispatcher) {
        val key = apiKey.trim()
        if (key.isBlank()) {
            return@withContext NetworkResult.InvalidKey
        }

        val visionContent = buildJsonArray {
            add(
                buildJsonObject {
                    put("type", "text")
                    put("text", prompt)
                }
            )
            add(
                buildJsonObject {
                    put("type", "image_url")
                    put(
                        "image_url",
                        buildJsonObject {
                            put("url", "data:image/jpeg;base64,$imageBase64")
                        }
                    )
                }
            )
        }

        val response: HttpResponse = client.post(baseUrl.trim()) {
            url { appendPathSegments("chat", "completions") }
            contentType(ContentType.Application.Json)
            bearerAuth(key)
            setBody(
                OpenaiMessageRequestBody(
                    model = model,
                    messages = listOf(
                        OpenaiMessage(
                            role = NetworkConstants.OPENAI_MESSAGE_USER_TYPE,
                            content = visionContent
                        )
                    )
                )
            )
        }

        if (response.status == HttpStatusCode.Unauthorized) {
            return@withContext NetworkResult.InvalidKey
        }
        if (response.status != HttpStatusCode.OK) {
            return@withContext NetworkResult.OtherError(
                "HTTP ${response.status.value}: ${response.bodyAsText()}"
            )
        }

        val result = response.body<OpenaiResponse>()
        return@withContext when {
            result.error != null && result.error.message.contains("API key") ->
                NetworkResult.InvalidKey

            result.error != null ->
                NetworkResult.OtherError(result.error.message)

            else ->
                NetworkResult.Success(
                    result.choices!!.first().message.content.jsonPrimitive.content
                )
        }
    }
}