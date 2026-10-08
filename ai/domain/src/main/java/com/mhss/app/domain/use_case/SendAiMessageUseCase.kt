package com.mhss.app.domain.use_case

import com.mhss.app.domain.repository.AiApi
import com.mhss.app.network.NetworkResult
import com.mhss.app.preferences.domain.model.AiProvider
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.IOException
import com.mhss.app.domain.model.AiMessage
import com.mhss.app.domain.model.AiMessageType
import java.util.UUID

@Single
class SendAiMessageUseCase(
    @Named("openaiApi") private val openai: AiApi,
) {

    /**
     * ===== TEXT / CHAT PROMPT =====
     * Används för:
     * - sammanfattning
     * - stavningskontroll
     * - autoformatering
     * - chat
     */
    suspend operator fun invoke(
        prompt: String,
        model: String,
        provider: AiProvider,
        baseURL: String = ""
    ): NetworkResult<String> {
        return try {
            when (provider) {
                AiProvider.OpenAI -> {
                    val result = openai.sendMessage(
                        baseUrl = baseURL,
                        messages = listOf(
                            AiMessage(
                                content = prompt,
                                type = AiMessageType.USER,
                                time = System.currentTimeMillis(),
                                attachments = emptyList(),
                                id = UUID.randomUUID().toString()
                            )
                        ),
                        model = model
                    )

                    when (result) {
                        is NetworkResult.Success -> {
                            NetworkResult.Success(result.data.content)
                        }

                        NetworkResult.InvalidKey -> {
                            NetworkResult.InvalidKey
                        }

                        NetworkResult.InternetError -> {
                            NetworkResult.InternetError
                        }

                        is NetworkResult.OtherError -> {
                            NetworkResult.OtherError(result.message)
                        }
                    }
                }

                else -> throw IllegalStateException("No AI provider is chosen")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            NetworkResult.InternetError
        } catch (e: Exception) {
            e.printStackTrace()
            NetworkResult.OtherError(e.message)
        }
    }





    /**
     * ===== OPENAI VISION / OCR =====
     * Används för:
     * - bild → text (OCR)
     * - mätvärden (blodtryck, puls, etc.)
     */
    suspend fun sendVisionPrompt(
        prompt: String,
        imageBase64: String,
        model: String,
        baseURL: String
    ): NetworkResult<String> {
        return try {
            openai.sendVisionPrompt(
                baseUrl = baseURL,
                model = model,
                prompt = prompt,
                imageBase64 = imageBase64
            )
        } catch (e: IOException) {
            e.printStackTrace()
            NetworkResult.InternetError
        } catch (e: Exception) {
            e.printStackTrace()
            NetworkResult.OtherError()
        }
    }
}
