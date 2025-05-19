package com.example.dacs.api.service

import com.example.dacs.api.model.ChatRequest
import com.example.dacs.api.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ChatService {
    @POST("v3/openai/chat/completions")
    suspend fun chat(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): ChatResponse
} 