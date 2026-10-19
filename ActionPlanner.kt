package com.example.data.action

import com.example.model.ActionPlan
import com.example.model.ActionStatus
import com.example.model.PhoneAction
import com.example.model.RiskLevel
import java.util.UUID

class ActionPlanner {

    fun planAction(action: PhoneAction): ActionPlan {
        val id = UUID.randomUUID().toString()
        val requiresConfirmation = action.riskLevel == RiskLevel.HIGH || action.riskLevel == RiskLevel.MEDIUM

        val explanation = when (action) {
            is PhoneAction.DialNumber -> {
                "আপনি কি ${if (action.contactName.isNotBlank()) action.contactName + " (" + action.phoneNumber + ")" else action.phoneNumber} নম্বরে কল ডায়াল করতে চান?"
            }
            is PhoneAction.DraftSms -> {
                "আপনি কি ${action.phoneNumber} নম্বরে এই বার্তাটি (\"${action.message}\") ড্রাফট করতে চান?"
            }
            is PhoneAction.SetTimer -> {
                "${action.minutes} মিনিটের জন্য একটি টাইমার সেট করা হবে।"
            }
            is PhoneAction.SetAlarm -> {
                String.format("%02d:%02d সময়ে অ্যালার্ম সেট করা হবে।", action.hour, action.minute)
            }
            is PhoneAction.OpenApp -> {
                "${action.appName} অ্যাপ ওপেন করা হচ্ছে..."
            }
            is PhoneAction.OpenSettings -> {
                "${action.getSummaryBn()} খোলা হচ্ছে..."
            }
            is PhoneAction.WebSearch -> {
                "\"${action.query}\" ইন্টারনেটে অনুসন্ধান করা হচ্ছে..."
            }
            is PhoneAction.OpenWebsite -> {
                "${action.url} ব্রাউজারে খোলা হচ্ছে..."
            }
            is PhoneAction.SaveMemory -> {
                "তথ্য সংরক্ষণ করা হচ্ছে: ${action.key}"
            }
            is PhoneAction.GetDeviceInfo -> {
                "ডিভাইসের তথ্য যাচাই করা হচ্ছে..."
            }
        }

        return ActionPlan(
            id = id,
            action = action,
            riskLevel = action.riskLevel,
            explanationBn = explanation,
            requiresConfirmation = requiresConfirmation,
            status = if (requiresConfirmation) ActionStatus.PENDING else ActionStatus.CONFIRMED
        )
    }

    /**
     * Converts Bengali digits (০-৯) to standard Arabic/English digits (0-9).
     */
    fun normalizeBengaliDigits(input: String): String {
        val bnDigits = "০১২৩৪৫৬৭৮৯"
        val enDigits = "0123456789"
        var result = input
        for (i in bnDigits.indices) {
            result = result.replace(bnDigits[i], enDigits[i])
        }
        return result
    }

    /**
     * Local Bengali Natural Language Intent Parser fallback.
     * Ensures JACK can handle core phone tasks even when offline or without API key.
     */
    fun parseBengaliIntent(input: String): PhoneAction? {
        val trimmed = input.trim()
        val normalized = normalizeBengaliDigits(trimmed)
        val lower = normalized.lowercase()

        // 1. Settings intents
        if (lower.contains("wifi") || lower.contains("ওয়াইফাই") || lower.contains("ওয়াইফাই")) {
            return PhoneAction.OpenSettings("wifi")
        }
        if (lower.contains("bluetooth") || lower.contains("ব্লুটুথ")) {
            return PhoneAction.OpenSettings("bluetooth")
        }
        if (lower.contains("sound") || lower.contains("volume") || lower.contains("সাউন্ড") || lower.contains("ভলিউম")) {
            return PhoneAction.OpenSettings("sound")
        }
        if (lower.contains("display") || lower.contains("ডিসপ্লে") || lower.contains("ব্রাইটনেস")) {
            return PhoneAction.OpenSettings("display")
        }
        if (lower.contains("settings") || lower.contains("সেটিংস")) {
            return PhoneAction.OpenSettings("main")
        }

        // 2. Timer intent (e.g., "১০ মিনিট টাইমার", "5 min timer", "টাইমার ৫ মিনিট")
        val timerRegex = Regex("""(\d+)\s*(মিনিট|মিনিটের|min|minute|minutes)""")
        val timerMatch = timerRegex.find(lower)
        if (timerMatch != null && (lower.contains("টাইমার") || lower.contains("timer"))) {
            val minutes = timerMatch.groupValues[1].toIntOrNull() ?: 5
            return PhoneAction.SetTimer(minutes = minutes, label = "JACK টাইমার")
        }

        // 3. Alarm intent (e.g., "অ্যালার্ম ৭ টা ৩০", "alarm 7:30")
        if (lower.contains("অ্যালার্ম") || lower.contains("alarm")) {
            val timeRegex = Regex("""(\d{1,2})[:.\s](\d{2})""")
            val timeMatch = timeRegex.find(lower)
            if (timeMatch != null) {
                val hour = timeMatch.groupValues[1].toIntOrNull() ?: 7
                val min = timeMatch.groupValues[2].toIntOrNull() ?: 0
                return PhoneAction.SetAlarm(hour = hour, minute = min, label = "JACK অ্যালার্ম")
            }
        }

        // 4. Dial / Call intent (e.g., "কল করো 01711...", "ডায়াল করো 01...")
        if (lower.contains("কল") || lower.contains("ডায়াল") || lower.contains("ডায়াল") || lower.contains("call") || lower.contains("dial")) {
            val phoneRegex = Regex("""\+?\d{6,14}""")
            val phoneMatch = phoneRegex.find(normalized)
            if (phoneMatch != null) {
                val number = phoneMatch.value
                return PhoneAction.DialNumber(phoneNumber = number, contactName = "")
            }
        }

        // 5. Open Apps (e.g., "ইউটিউব খোলো", "ক্যামেরা চালু করো", "open youtube", "open camera")
        val appKeywords = listOf(
            Pair(listOf("ইউটিউব", "youtube"), "YouTube"),
            Pair(listOf("ক্যামেরা", "camera"), "Camera"),
            Pair(listOf("হোয়াটসঅ্যাপ", "হোয়াটসঅ্যাপ", "whatsapp"), "WhatsApp"),
            Pair(listOf("ক্যালকুলেটর", "calculator"), "Calculator"),
            Pair(listOf("ক্রোম", "chrome", "ব্রাউজার", "browser"), "Chrome"),
            Pair(listOf("ম্যাপ", "গুগল ম্যাপ", "maps"), "Google Maps"),
            Pair(listOf("প্লে স্টোর", "play store"), "Play Store"),
            Pair(listOf("ঘড়ি", "ঘড়ি", "clock"), "Clock"),
            Pair(listOf("গ্যালারি", "gallery", "ছবি", "photos"), "Gallery")
        )

        for ((keywords, appName) in appKeywords) {
            if (keywords.any { lower.contains(it) }) {
                if (lower.contains("খোলো") || lower.contains("ওপেন") || lower.contains("চালু") || lower.contains("open") || lower.contains("start")) {
                    return PhoneAction.OpenApp(appName = appName)
                }
            }
        }

        // 6. Web Search (e.g., "সার্চ করো ...", "খোঁজো ...", "search ...")
        if (lower.startsWith("সার্চ করো") || lower.startsWith("খোঁজো") || lower.startsWith("অনুসন্ধান করো") || lower.startsWith("search")) {
            val query = trimmed.replace(Regex("^(সার্চ করো|খোঁজো|অনুসন্ধান করো|search)\\s*"), "").trim()
            if (query.isNotBlank()) {
                return PhoneAction.WebSearch(query = query)
            }
        }

        // 7. Save memory (e.g., "মনে রাখো ...")
        if (lower.startsWith("মনে রাখো") || lower.startsWith("মনে রাখ")) {
            val memoryText = trimmed.replace(Regex("^(মনে রাখো|মনে রাখ)\\s*"), "").trim()
            if (memoryText.isNotBlank()) {
                return PhoneAction.SaveMemory(key = "ব্যবহারকারীর নোট", value = memoryText)
            }
        }

        // 8. Device Info
        if (lower.contains("ব্যাটারি") || lower.contains("চার্জ") || lower.contains("battery") || lower.contains("ডিভাইস ইনফো")) {
            return PhoneAction.GetDeviceInfo
        }

        return null
    }
}
