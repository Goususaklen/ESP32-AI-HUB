package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "USER", "AI", "SYSTEM", "VOICE"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val commandExecuted: String? = null
)
