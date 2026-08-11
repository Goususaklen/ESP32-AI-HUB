package com.example.data.model

enum class MessageSender {
    USER, AI, SYSTEM, VOICE
}

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val commandExecuted: String? = null,
    val isLoading: Boolean = false
)
