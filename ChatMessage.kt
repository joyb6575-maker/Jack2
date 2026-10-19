package com.example.model

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class GroundingSource(
    val title: String,
    val url: String
)

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionPlan: ActionPlan? = null,
    val isError: Boolean = false,
    val sources: List<GroundingSource> = emptyList()
)
