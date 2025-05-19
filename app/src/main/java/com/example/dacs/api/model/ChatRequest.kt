package com.example.dacs.api.model

data class ChatRequest(
    val model: String = "meta-llama/llama-3.1-8b-instruct",
    val messages: List<RequestMessage>,
    val max_tokens: Int = 512
)

data class RequestMessage(
    val role: String,
    val content: String
) 