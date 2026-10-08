package com.mhss.app.data.model.openai

import com.mhss.app.data.NetworkConstants
import com.mhss.app.domain.model.AiMessage
import com.mhss.app.domain.systemMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class OpenaiMessageRequestBody(
    val model: String,
    val messages: List<OpenaiMessage>
)

@Serializable
data class OpenaiMessage(
    val role: String,
    val content: JsonElement
)

fun List<AiMessage>.toOpenAiRequestBody(
    model: String,
): OpenaiMessageRequestBody {
    return OpenaiMessageRequestBody(
        model = model,
        messages =
        listOf(
            OpenaiMessage(
                content = JsonPrimitive(systemMessage),
                role = NetworkConstants.OPENAI_MESSAGE_SYSTEM_TYPE
            )
        ) + map {
            OpenaiMessage(
                content = JsonPrimitive(it.content + it.attachmentsText),
                role = it.type.openaiRole
            )
        }
    )
}