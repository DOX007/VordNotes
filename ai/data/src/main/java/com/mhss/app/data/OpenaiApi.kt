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
import io.ktor.http.ContentType
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

    // ⚠️ OBS: Flytta detta till säkrare hantering senare
    private val aiKey =
        "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"

    // =========================
    // ===== TEXT PROMPT ======
    // =========================

    override suspend fun sendPrompt(
        baseUrl: String,
        prompt: String,
        model: String
    ): NetworkResult<String> = withContext(ioDispatcher) {
        val result = client.post(baseUrl) {
            url { appendPathSegments("chat", "completions") }
            contentType(ContentType.Application.Json)
            bearerAuth(aiKey)
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
        }.body<OpenaiResponse>()

        when {
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
        messages: List<AiMessage>,
        model: String
    ): NetworkResult<AiMessage> = withContext(ioDispatcher) {
        val result = client.post(baseUrl) {
            url { appendPathSegments("chat", "completions") }
            contentType(ContentType.Application.Json)
            bearerAuth(aiKey)
            setBody(messages.toOpenAiRequestBody(model))
        }.body<OpenaiResponse>()

        when {
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
        model: String,
        prompt: String,
        imageBase64: String
    ): NetworkResult<String> = withContext(ioDispatcher) {

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

        val result = client.post(baseUrl) {
            url { appendPathSegments("chat", "completions") }
            contentType(ContentType.Application.Json)
            bearerAuth(aiKey)
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
        }.body<OpenaiResponse>()

        when {
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