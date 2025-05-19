package com.example.dacs.viewmodel.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dacs.api.client.ApiClient
import com.example.dacs.api.model.ChatRequest
import com.example.dacs.api.model.RequestMessage
import com.example.dacs.model.AIChatMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AIChatViewModel @Inject constructor() : ViewModel() {
    private val _messages = MutableStateFlow<List<AIChatMessage>>(emptyList())
    val messages: StateFlow<List<AIChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val db = Firebase.database
    private val currentUserId = Firebase.auth.currentUser?.uid

    init {
        loadChatHistory()
    }

    private fun loadChatHistory() {
        if (currentUserId == null) return

        viewModelScope.launch {
            try {
                val snapshot = db.getReference("ai_chats")
                    .child(currentUserId)
                    .get()
                    .await()

                val chatHistory = mutableListOf<AIChatMessage>()
                snapshot.children.forEach { child ->
                    val message = child.getValue(AIChatMessage::class.java)
                    message?.let { chatHistory.add(it) }
                }
                _messages.value = chatHistory.sortedBy { it.timestamp }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun sendMessage(content: String) {
        if (currentUserId == null) return

        val userMessage = AIChatMessage(
            content = content,
            isUser = true
        )

        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Save user message
                val messageRef = db.getReference("ai_chats")
                    .child(currentUserId)
                    .push()
                messageRef.setValue(userMessage).await()

                // Call Novita AI API
                val request = ChatRequest(
                    messages = listOf(
                        RequestMessage("system", "Act like you are a helpful assistant."),
                        RequestMessage("user", content)
                    )
                )

                val response = ApiClient.chatService.chat(
                    apiKey = "Bearer ${ApiClient.API_KEY}",
                    request = request
                )

                // Save AI response
                val aiResponse = AIChatMessage(
                    content = response.choices.firstOrNull()?.message?.content ?: "Sorry, I couldn't process that.",
                    isUser = false
                )

                db.getReference("ai_chats")
                    .child(currentUserId)
                    .push()
                    .setValue(aiResponse)
                    .await()

                // Update messages
                _messages.value = _messages.value + listOf(userMessage, aiResponse)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
} 