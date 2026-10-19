package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AssistantState
import com.example.model.ChatMessage
import com.example.model.MessageRole
import com.example.ui.components.ActionConfirmationCard
import com.example.ui.components.AudioWaveform
import com.example.ui.components.JackOrb
import com.example.ui.components.StateIndicatorPill
import com.example.ui.theme.JackCyanNeon
import com.example.ui.theme.JackGreenReady
import com.example.ui.theme.JackRedAlert
import com.example.ui.theme.JackSpaceDark
import com.example.ui.theme.JackSurfaceCard
import com.example.ui.theme.JackSurfaceElevated
import com.example.ui.theme.JackTextMuted
import com.example.ui.theme.JackTextPrimary
import com.example.ui.theme.JackTextSecondary
import com.example.viewmodel.JackAssistantViewModel

@Composable
fun MainAssistantScreen(
    viewModel: JackAssistantViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assistantState by viewModel.assistantState.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val isJackOn by viewModel.isJackOn.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val pendingAction by viewModel.pendingActionPlan.collectAsState()
    val inputText by viewModel.currentInputText.collectAsState()
    val statusNotice by viewModel.statusNoticeBn.collectAsState()

    val listState = rememberLazyListState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val samplePrompts = listOf(
        "আজকের আবহাওয়া কেমন?",
        "ইউটিউব খোলো",
        "৫ মিনিটের টাইমার সেট করো",
        "ওয়াইফাই সেটিংস",
        "ডিভাইস ব্যাটারি চেক করো"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JackSpaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- Top Bar ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(JackSurfaceElevated)
                        .border(1.dp, JackCyanNeon.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "JACK AI",
                        tint = if (isJackOn) JackGreenReady else JackTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "JACK AI",
                        color = JackTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isJackOn) "অনলাইন" else "বন্ধ",
                        color = if (isJackOn) JackGreenReady else JackTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Master Power Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(JackSurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isJackOn) "ON" else "OFF",
                        color = if (isJackOn) JackCyanNeon else JackTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Switch(
                        checked = isJackOn,
                        onCheckedChange = { viewModel.toggleJackPower(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JackCyanNeon,
                            checkedTrackColor = JackSurfaceCard,
                            uncheckedThumbColor = JackTextMuted,
                            uncheckedTrackColor = JackSurfaceElevated
                        ),
                        modifier = Modifier.testTag("master_power_switch")
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onNavigateToHistory,
                    modifier = Modifier.testTag("history_nav_button")
                ) {
                    Icon(imageVector = Icons.Default.History, contentDescription = "ইতিহাস", tint = JackCyanNeon)
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.testTag("settings_nav_button")
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "সেটিংস", tint = JackCyanNeon)
                }
            }
        }

        // --- Notice Banner (if any) ---
        AnimatedVisibility(visible = !statusNotice.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(JackSurfaceCard)
                    .border(1.dp, JackCyanNeon.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = statusNotice.orEmpty(),
                    color = JackCyanNeon,
                    fontSize = 12.sp
                )
            }
        }

        // --- Central Visualizer / Orb Area ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                JackOrb(
                    state = assistantState,
                    audioLevel = audioLevel,
                    size = 180.dp,
                    onClick = {
                        if (!isJackOn) {
                            viewModel.toggleJackPower(true)
                        } else if (assistantState == AssistantState.LISTENING) {
                            viewModel.stopListening()
                        } else if (assistantState == AssistantState.SPEAKING) {
                            viewModel.stopSpeaking()
                        } else {
                            val permissionCheck = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            )
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                viewModel.startListening()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                StateIndicatorPill(state = assistantState)

                Spacer(modifier = Modifier.height(4.dp))

                AudioWaveform(
                    audioLevel = audioLevel,
                    isActive = assistantState == AssistantState.LISTENING || assistantState == AssistantState.SPEAKING
                )
            }
        }

        // --- Confirmation Dialog (if pending) ---
        AnimatedVisibility(visible = pendingAction != null) {
            pendingAction?.let { plan ->
                ActionConfirmationCard(
                    plan = plan,
                    onConfirm = { viewModel.confirmPendingAction() },
                    onReject = { viewModel.rejectPendingAction() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // --- Transcript / Chat Messages List ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "কোনো বার্তা নেই। কথা বলতে নিচে মাইক ট্যাপ করুন অথবা প্রশ্ন টাইপ করুন।",
                            color = JackTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }
            }
        }

        // --- Quick Prompts ---
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(samplePrompts) { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(JackSurfaceElevated)
                        .border(1.dp, JackCyanNeon.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .clickable { viewModel.processUserInput(prompt) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = prompt,
                        color = JackCyanNeon,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // --- Bottom Action & Input Bar ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)
        ) {
            // Stop / Cancel button when active
            if (assistantState == AssistantState.SPEAKING) {
                IconButton(
                    onClick = { viewModel.stopSpeaking() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(JackRedAlert.copy(alpha = 0.2f))
                        .border(1.dp, JackRedAlert, CircleShape)
                        .testTag("stop_speaking_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "কথা বলা থামান", tint = JackRedAlert)
                }
                Spacer(modifier = Modifier.width(8.dp))
            } else if (assistantState == AssistantState.THINKING || assistantState == AssistantState.EXECUTING) {
                IconButton(
                    onClick = { viewModel.cancelActiveOperation() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(JackRedAlert.copy(alpha = 0.2f))
                        .border(1.dp, JackRedAlert, CircleShape)
                        .testTag("cancel_action_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "বাতিল", tint = JackRedAlert)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Text Input
            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.onInputTextChanged(it) },
                placeholder = {
                    Text(
                        text = if (isJackOn) "নির্দেশ লিখুন..." else "আগে JACK চালু করুন...",
                        color = JackTextMuted,
                        fontSize = 14.sp
                    )
                },
                enabled = isJackOn,
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JackCyanNeon,
                    unfocusedBorderColor = JackSurfaceElevated,
                    focusedTextColor = JackTextPrimary,
                    unfocusedTextColor = JackTextPrimary,
                    focusedContainerColor = JackSurfaceCard,
                    unfocusedContainerColor = JackSurfaceCard
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Mic or Send Button
            if (inputText.isNotBlank()) {
                IconButton(
                    onClick = { viewModel.sendTextMessage() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(JackCyanNeon)
                        .testTag("send_message_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "পাঠান", tint = Color.Black)
                }
            } else {
                IconButton(
                    onClick = {
                        val permissionCheck = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        )
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            if (assistantState == AssistantState.LISTENING) {
                                viewModel.stopListening()
                            } else {
                                viewModel.startListening()
                            }
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (assistantState == AssistantState.LISTENING) JackCyanNeon else JackSurfaceElevated)
                        .border(
                            1.dp,
                            if (assistantState == AssistantState.LISTENING) JackCyanNeon else JackCyanNeon.copy(alpha = 0.4f),
                            CircleShape
                        )
                        .testTag("mic_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "ভয়েস ইনপুট",
                        tint = if (assistantState == AssistantState.LISTENING) Color.Black else JackCyanNeon
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val bubbleColor = if (isUser) JackSurfaceElevated else JackSurfaceCard
    val borderColor = if (message.isError) {
        JackRedAlert
    } else if (isUser) {
        JackCyanNeon.copy(alpha = 0.3f)
    } else {
        JackCyanNeon.copy(alpha = 0.15f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier
                .border(
                    1.dp,
                    borderColor,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(vertical = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) "আপনি" else "JACK",
                    color = if (isUser) JackTextSecondary else JackCyanNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    color = JackTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                if (message.sources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "তথ্যসূত্র:",
                        color = JackCyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    message.sources.forEach { source ->
                        Text(
                            text = "• ${source.title}",
                            color = JackTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
