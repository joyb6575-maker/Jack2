package com.example.model

enum class ActionStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    EXECUTED,
    FAILED
}

data class ActionPlan(
    val id: String,
    val action: PhoneAction,
    val riskLevel: RiskLevel,
    val explanationBn: String,
    val requiresConfirmation: Boolean,
    var status: ActionStatus = ActionStatus.PENDING,
    var resultMessageBn: String? = null
)
