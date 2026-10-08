package com.mhss.app.domain.repository

import com.mhss.app.network.NetworkResult
import com.mhss.app.domain.model.AiMessage

interface AiApi {

    suspend fun sendPrompt(
        baseUrl: String,
        prompt: String,
        model: String
    ): NetworkResult<String>

    suspend fun sendMessage(
        baseUrl: String,
        messages: List<AiMessage>,
        model: String
    ): NetworkResult<AiMessage>

    suspend fun sendVisionPrompt(
        baseUrl: String,
        model: String,
        prompt: String,
        imageBase64: String
    ): NetworkResult<String>
}
