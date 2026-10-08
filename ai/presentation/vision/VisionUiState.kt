package com.mhss.app.ai.presentation.vision

import com.mhss.app.network.NetworkResult

data class VisionUiState(
    val loading: Boolean = false,
    val resultText: String? = null,
    val error: NetworkResult.Failure? = null
)

