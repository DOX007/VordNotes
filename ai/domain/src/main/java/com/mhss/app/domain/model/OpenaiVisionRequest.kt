package com.mhss.app.ai.data.model.openai

data class OpenaiVisionRequest(
    val model: String,
    val messages: List<Message>
) {
    data class Message(
        val role: String,
        val content: List<Content>
    )

    data class Content(
        val type: String,
        val text: String? = null,
        val image_url: ImageUrl? = null
    )

    data class ImageUrl(
        val url: String
    )
}
