package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.JackAmberConfirm
import com.example.ui.theme.JackCyanNeon
import com.example.ui.theme.JackGreenReady
import com.example.ui.theme.JackRedAlert
import com.example.ui.theme.JackTextMuted
import com.example.ui.theme.JackVioletNeon

enum class AssistantState(
    val titleBn: String,
    val subtitleBn: String,
    val color: Color
) {
    OFF(
        titleBn = "জ্যাক নিষ্ক্রিয় (OFF)",
        subtitleBn = "সহকারী চালু করতে ট্যাপ করুন",
        color = JackTextMuted
    ),
    READY(
        titleBn = "জ্যাক প্রস্তুত (ONLINE)",
        subtitleBn = "আমি আপনার কথা শোনার জন্য তৈরি",
        color = JackGreenReady
    ),
    LISTENING(
        titleBn = "শুনছি...",
        subtitleBn = "স্পষ্টভাবে আপনার নির্দেশ বলুন",
        color = JackCyanNeon
    ),
    THINKING(
        titleBn = "ভাবছি...",
        subtitleBn = "তথ্য বিশ্লেষণ ও পরিকল্পনা হচ্ছে",
        color = JackVioletNeon
    ),
    SPEAKING(
        titleBn = "উত্তর দিচ্ছি...",
        subtitleBn = "ভয়েস প্লেব্যাক চলছে",
        color = JackCyanNeon
    ),
    EXECUTING(
        titleBn = "সম্পাদন করছি...",
        subtitleBn = "অনুমোদিত কাজ সম্পন্ন হচ্ছে",
        color = JackGreenReady
    ),
    NEEDS_CONFIRMATION(
        titleBn = "অনুমতি প্রয়োজন",
        subtitleBn = "কাজটি সম্পন্ন করতে আপনার সম্মতি দিন",
        color = JackAmberConfirm
    ),
    ERROR(
        titleBn = "ত্রুটি ঘটেছে",
        subtitleBn = "পুনরায় চেষ্টা করুন বা সেটিংস দেখুন",
        color = JackRedAlert
    )
}
