package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.JackApplication
import com.example.data.action.ActionPlanner
import com.example.data.action.PhoneActionExecutor
import com.example.data.ai.GeminiClient
import com.example.data.ai.GeminiResult
import com.example.data.local.ChatMessageEntity
import com.example.data.local.UserMemoryEntity
import com.example.model.ActionPlan
import com.example.model.ActionStatus
import com.example.model.AssistantState
import com.example.model.ChatMessage
import com.example.model.MessageRole
import com.example.model.PhoneAction
import com.example.model.RiskLevel
import com.example.service.JackAssistantService
import com.example.voice.JackSpeechRecognizer
import com.example.voice.JackTextToSpeech
import com.example.voice.SpeechEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class JackAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JackApplication
    private val database = app.database
    private val preferences = app.preferences

    private val actionPlanner = ActionPlanner()
    private val actionExecutor = PhoneActionExecutor(application)

    private val speechRecognizer = JackSpeechRecognizer(application)
    private var textToSpeech: JackTextToSpeech? = null

    private var activeJob: Job? = null

    // State
    private val _assistantState = MutableStateFlow(AssistantState.READY)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _pendingActionPlan = MutableStateFlow<ActionPlan?>(null)
    val pendingActionPlan: StateFlow<ActionPlan?> = _pendingActionPlan.asStateFlow()

    private val _currentInputText = MutableStateFlow("")
    val currentInputText: StateFlow<String> = _currentInputText.asStateFlow()

    private val _statusNoticeBn = MutableStateFlow<String?>(null)
    val statusNoticeBn: StateFlow<String?> = _statusNoticeBn.asStateFlow()

    // Preferences
    val isJackOn = preferences.isJackOn.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val autoSpeak = preferences.autoSpeak.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val speechRate = preferences.speechRate.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    val speechPitch = preferences.speechPitch.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    val customApiKey = preferences.customApiKey.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val selectedModel = preferences.selectedModel.stateIn(viewModelScope, SharingStarted.Eagerly, "gemini-3.5-flash")
    val hasCompletedOnboarding = preferences.hasCompletedOnboarding.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isDarkTheme = preferences.isDarkTheme.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // Chat Messages Flow
    val messages: StateFlow<List<ChatMessage>> = database.chatDao().getAllMessages()
        .map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    role = if (entity.role == "user") MessageRole.USER else MessageRole.ASSISTANT,
                    text = entity.text,
                    timestamp = entity.timestamp,
                    isError = entity.isError
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Memories Flow
    val memories: StateFlow<List<UserMemoryEntity>> = database.memoryDao().getAllMemories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val geminiClient = GeminiClient(
        apiKeyProvider = { customApiKey.value },
        modelProvider = { selectedModel.value }
    )

    init {
        // Initialize Text to Speech
        textToSpeech = JackTextToSpeech(application) { success ->
            if (!success) {
                _statusNoticeBn.value = "বাংলা ভয়েস আউটপুট ডিভাইস সমর্থন করেনি।"
            }
        }

        // Listen to Speech Recognizer events
        viewModelScope.launch {
            speechRecognizer.speechEvents.collect { event ->
                when (event) {
                    is SpeechEvent.Ready -> {
                        _assistantState.value = AssistantState.LISTENING
                    }
                    is SpeechEvent.RmsChanged -> {
                        _audioLevel.value = event.normalizedRms
                    }
                    is SpeechEvent.Result -> {
                        _assistantState.value = AssistantState.THINKING
                        processUserInput(event.text)
                    }
                    is SpeechEvent.Error -> {
                        if (isJackOn.value) {
                            _assistantState.value = AssistantState.ERROR
                            _statusNoticeBn.value = event.messageBn
                        }
                    }
                    is SpeechEvent.EndOfSpeech -> {
                        _audioLevel.value = 0f
                    }
                    null -> {}
                    else -> {}
                }
            }
        }

        // Initialize state based on Jack ON/OFF preference
        viewModelScope.launch {
            preferences.isJackOn.collect { on ->
                if (!on) {
                    turnJackOff()
                } else if (_assistantState.value == AssistantState.OFF) {
                    _assistantState.value = AssistantState.READY
                }
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        _currentInputText.value = newText
    }

    fun toggleJackPower(turnOn: Boolean) {
        viewModelScope.launch {
            preferences.setJackOn(turnOn)
            if (turnOn) {
                _assistantState.value = AssistantState.READY
                _statusNoticeBn.value = "JACK অনলাইন হয়েছে।"
                speakFeedback("সিস্টেম অনলাইন। আমি প্রস্তুত স্যার।")
            } else {
                turnJackOff()
            }
        }
    }

    private fun turnJackOff() {
        activeJob?.cancel()
        speechRecognizer.stopListening()
        textToSpeech?.stop()
        _audioLevel.value = 0f
        _pendingActionPlan.value = null
        _assistantState.value = AssistantState.OFF
        _statusNoticeBn.value = "JACK নিষ্ক্রিয় (OFF) করা হয়েছে।"
        JackAssistantService.stopService(getApplication())
    }

    fun startListening() {
        if (!isJackOn.value) {
            _statusNoticeBn.value = "সহকারী বন্ধ রয়েছে। আগে JACK চালু করুন।"
            return
        }
        textToSpeech?.stop()
        _statusNoticeBn.value = null
        _assistantState.value = AssistantState.LISTENING
        speechRecognizer.startListening()
    }

    fun stopListening() {
        speechRecognizer.stopListening()
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.READY
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        if (_assistantState.value == AssistantState.SPEAKING) {
            _assistantState.value = AssistantState.READY
        }
    }

    fun cancelActiveOperation() {
        activeJob?.cancel()
        speechRecognizer.stopListening()
        textToSpeech?.stop()
        _pendingActionPlan.value = null
        _assistantState.value = if (isJackOn.value) AssistantState.READY else AssistantState.OFF
        _statusNoticeBn.value = "অপারেশন বাতিল করা হয়েছে।"
    }

    fun sendTextMessage() {
        val text = _currentInputText.value.trim()
        if (text.isBlank() || !isJackOn.value) return
        _currentInputText.value = ""
        processUserInput(text)
    }

    fun processUserInput(input: String) {
        if (!isJackOn.value) return

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            val userMsgId = UUID.randomUUID().toString()
            val userEntity = ChatMessageEntity(
                id = userMsgId,
                role = "user",
                text = input,
                timestamp = System.currentTimeMillis()
            )
            database.chatDao().insertMessage(userEntity)

            _assistantState.value = AssistantState.THINKING

            // 1. Check local intent first
            val localIntentAction = actionPlanner.parseBengaliIntent(input)
            if (localIntentAction != null) {
                handlePlannedAction(localIntentAction, contextReply = null)
                return@launch
            }

            // 2. Query Gemini
            val recentEntities = database.chatDao().getRecentMessages()
            val conversationHistory = recentEntities.reversed().map {
                Pair(if (it.role == "user") MessageRole.USER else MessageRole.ASSISTANT, it.text)
            }
            val activeMemories = database.memoryDao().getMemoriesSnapshot().map {
                Pair(it.key, it.value)
            }

            when (val result = geminiClient.generateAssistantResponse(conversationHistory, activeMemories)) {
                is GeminiResult.Success -> {
                    val assistantMsgId = UUID.randomUUID().toString()
                    val assistantEntity = ChatMessageEntity(
                        id = assistantMsgId,
                        role = "assistant",
                        text = result.replyText,
                        timestamp = System.currentTimeMillis(),
                        isError = false
                    )
                    database.chatDao().insertMessage(assistantEntity)

                    if (result.functionCallAction != null) {
                        handlePlannedAction(result.functionCallAction, contextReply = result.replyText)
                    } else {
                        speakFeedback(result.replyText)
                    }
                }
                is GeminiResult.Error -> {
                    _assistantState.value = AssistantState.ERROR
                    _statusNoticeBn.value = result.messageBn

                    val errorEntity = ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        role = "assistant",
                        text = result.messageBn,
                        timestamp = System.currentTimeMillis(),
                        isError = true
                    )
                    database.chatDao().insertMessage(errorEntity)
                    speakFeedback(result.messageBn)
                }
            }
        }
    }

    private fun handlePlannedAction(action: PhoneAction, contextReply: String?) {
        val plan = actionPlanner.planAction(action)

        if (action is PhoneAction.SaveMemory) {
            viewModelScope.launch {
                database.memoryDao().insertMemory(
                    UserMemoryEntity(key = action.key, value = action.value)
                )
                val reply = "তথ্য মনে রাখা হয়েছে: ${action.key} = ${action.value}"
                insertAssistantMessage(reply)
                speakFeedback(reply)
            }
            return
        }

        if (plan.requiresConfirmation) {
            _pendingActionPlan.value = plan
            _assistantState.value = AssistantState.NEEDS_CONFIRMATION
            val confirmPrompt = plan.explanationBn
            speakFeedback(confirmPrompt)
        } else {
            executeActionDirectly(plan, contextReply)
        }
    }

    fun confirmPendingAction() {
        val plan = _pendingActionPlan.value ?: return
        _pendingActionPlan.value = null
        executeActionDirectly(plan, null)
    }

    fun rejectPendingAction() {
        val plan = _pendingActionPlan.value ?: return
        _pendingActionPlan.value = null
        plan.status = ActionStatus.REJECTED
        _assistantState.value = AssistantState.READY
        val cancelMsg = "কাজটি বাতিল করা হয়েছে।"
        insertAssistantMessage(cancelMsg)
        speakFeedback(cancelMsg)
    }

    private fun executeActionDirectly(plan: ActionPlan, contextReply: String?) {
        _assistantState.value = AssistantState.EXECUTING
        viewModelScope.launch {
            val result = actionExecutor.execute(plan.action)
            plan.status = if (result.isSuccess) ActionStatus.EXECUTED else ActionStatus.FAILED
            plan.resultMessageBn = result.messageBn

            val feedbackText = buildString {
                if (!contextReply.isNullOrBlank()) {
                    append(contextReply)
                    append("\n\n")
                }
                append(result.messageBn)
            }

            insertAssistantMessage(feedbackText, isError = !result.isSuccess)
            speakFeedback(result.messageBn)
        }
    }

    private fun insertAssistantMessage(text: String, isError: Boolean = false) {
        viewModelScope.launch {
            val entity = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                role = "assistant",
                text = text,
                timestamp = System.currentTimeMillis(),
                isError = isError
            )
            database.chatDao().insertMessage(entity)
        }
    }

    private fun speakFeedback(text: String) {
        if (!isJackOn.value) return
        if (autoSpeak.value && text.isNotBlank()) {
            _assistantState.value = AssistantState.SPEAKING
            textToSpeech?.speak(
                text = text,
                speechRate = speechRate.value,
                speechPitch = speechPitch.value,
                onDone = {
                    if (_assistantState.value == AssistantState.SPEAKING) {
                        _assistantState.value = AssistantState.READY
                    }
                }
            )
        } else {
            _assistantState.value = AssistantState.READY
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            database.chatDao().clearHistory()
            _statusNoticeBn.value = "কথোপকথনের ইতিহাস মুছে ফেলা হয়েছে।"
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            database.memoryDao().deleteMemory(id)
        }
    }

    fun addManualMemory(key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        viewModelScope.launch {
            database.memoryDao().insertMemory(UserMemoryEntity(key = key.trim(), value = value.trim()))
        }
    }

    fun updateCustomApiKey(key: String) {
        viewModelScope.launch {
            preferences.setCustomApiKey(key)
        }
    }

    fun updateSelectedModel(model: String) {
        viewModelScope.launch {
            preferences.setSelectedModel(model)
        }
    }

    fun updateSpeechRate(rate: Float) {
        viewModelScope.launch {
            preferences.setSpeechRate(rate)
        }
    }

    fun updateSpeechPitch(pitch: Float) {
        viewModelScope.launch {
            preferences.setSpeechPitch(pitch)
        }
    }

    fun updateAutoSpeak(auto: Boolean) {
        viewModelScope.launch {
            preferences.setAutoSpeak(auto)
        }
    }

    fun updateDarkTheme(dark: Boolean) {
        viewModelScope.launch {
            preferences.setDarkTheme(dark)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferences.setOnboardingDone(true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.stopListening()
        textToSpeech?.shutdown()
    }
}
