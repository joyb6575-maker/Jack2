package com.example.model

enum class RiskLevel(val labelBn: String) {
    LOW("স্বাভাবিক"),
    MEDIUM("সতর্কতা"),
    HIGH("সংবেদনশীল")
}

sealed class PhoneAction {
    abstract val riskLevel: RiskLevel
    abstract fun getSummaryBn(): String

    data class OpenApp(
        val appName: String,
        val packageName: String? = null
    ) : PhoneAction() {
        override val riskLevel = RiskLevel.LOW
        override fun getSummaryBn() = "অ্যাপ ওপেন করা: $appName"
    }

    data class OpenSettings(
        val settingType: String // "wifi", "bluetooth", "sound", "display", "accessibility", "app_details", "main"
    ) : PhoneAction() {
        override val riskLevel = RiskLevel.LOW
        override fun getSummaryBn() = when (settingType.lowercase()) {
            "wifi" -> "ওয়াই-ফাই সেটিংস খোলা"
            "bluetooth" -> "ব্লুটুথ সেটিংস খোলা"
            "sound", "volume" -> "সাউন্ড সেটিংস খোলা"
            "display" -> "ডিসপ্লে সেটিংস খোলা"
            "accessibility" -> "অ্যাক্সেসিবিলিটি সেটিংস খোলা"
            else -> "ডিভাইস সেটিংস খোলা"
        }
    }

    data class WebSearch(
        val query: String
    ) : PhoneAction() {
        override val riskLevel = RiskLevel.LOW
        override fun getSummaryBn() = "ইন্টারনেটে অনুসন্ধান: \"$query\""
    }

    data class OpenWebsite(
        val url: String
    ) : PhoneAction() {
        override val riskLevel = RiskLevel.LOW
        override fun getSummaryBn() = "ওয়েবসাইট ভিজিট: $url"
    }

    data class DialNumber(
        val phoneNumber: String,
        val contactName: String = ""
    ) : PhoneAction() {
        override val riskLevel = RiskLevel.HIGH
        override fun getSummaryBn() = if (contactName.isNotBlank()) {
            "$contactName ($phoneNumber)-এ কল ডায়াল করা"
        } else {
            "$phoneNumber নম্বরে কল ডায়াল করা"
        }
    }

    data class DraftSms(
        val phoneNumber: String,
        val message: String
    ) : PhoneAction() {
        override val riskLevel = RiskLevel.HIGH
        override fun getSummaryBn() = "$phoneNumber নম্বরে এসএমএস ড্রাফট তৈরি: \"$message\""
    }

    data class SetTimer(
        val minutes: Int,
        val label: String = "JACK টাইমার"
    ) : PhoneAction() {
        override val riskLevel = RiskLevel.MEDIUM
        override fun getSummaryBn() = "$minutes মিনিটের জন্য টাইমার সেট করা ($label)"
    }

    data class SetAlarm(
        val hour: Int,
        val minute: Int,
        val label: String = "JACK অ্যালার্ম"
    ) : PhoneAction() {
        override val riskLevel = RiskLevel.MEDIUM
        override fun getSummaryBn() = String.format("%02d:%02d সময়ে অ্যালার্ম সেট করা (%s)", hour, minute, label)
    }

    data class SaveMemory(
        val key: String,
        val value: String
    ) : PhoneAction() {
        override val riskLevel = RiskLevel.LOW
        override fun getSummaryBn() = "তথ্য মনে রাখা: $key -> $value"
    }

    object GetDeviceInfo : PhoneAction() {
        override val riskLevel = RiskLevel.LOW
        override fun getSummaryBn() = "ডিভাইসের তথ্য (ব্যাটারি, নেটওয়ার্ক, মেমরি) সংগ্রহ করা"
    }
}
