package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.model.GroundingSource
import com.example.model.MessageRole
import com.example.model.PhoneAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class GeminiResult {
    data class Success(
        val replyText: String,
        val functionCallAction: PhoneAction? = null,
        val sources: List<GroundingSource> = emptyList()
    ) : GeminiResult()

    data class Error(val messageBn: String, val canRetry: Boolean = true) : GeminiResult()
}

class GeminiClient(
    private val apiKeyProvider: () -> String,
    private val modelProvider: () -> String = { "gemini-3.5-flash" }
) {
    private val tag = "GeminiClient"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateAssistantResponse(
        conversationHistory: List<Pair<MessageRole, String>>,
        activeMemories: List<Pair<String, String>>,
        maxRetries: Int = 2
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().ifBlank {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Throwable) {
                ""
            }
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResult.Error(
                messageBn = "Gemini API কী সেট করা নেই। দয়া করে অ্যাপ সেটিংস বা AI Studio Secrets প্যানেলে আপনার API কী যুক্ত করুন।",
                canRetry = false
            )
        }

        val modelName = modelProvider().ifBlank { "gemini-3.5-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val requestPayload = buildRequestPayload(conversationHistory, activeMemories)
        val body = requestPayload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(body).build()

        var currentAttempt = 0
        var backoffMs = 1500L

        while (currentAttempt <= maxRetries) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseCode = response.code
                    val responseBodyString = response.body?.string().orEmpty()

                    if (response.isSuccessful) {
                        return@withContext parseSuccessResponse(responseBodyString)
                    }

                    Log.e(tag, "Gemini API Error: $responseCode - $responseBodyString")

                    when (responseCode) {
                        400 -> {
                            return@withContext GeminiResult.Error(
                                messageBn = "অনুরোধে সমস্যা রয়েছে (ভুল ফরম্যাট বা অবৈধ প্যারামিটার)।",
                                canRetry = false
                            )
                        }
                        401, 403 -> {
                            return@withContext GeminiResult.Error(
                                messageBn = "API কী অকার্যকর বা অনুমোদিত নয়। অনুগ্রহ করে সঠিক Gemini API কী দিয়ে চেষ্টা করুন।",
                                canRetry = false
                            )
                        }
                        429 -> {
                            if (currentAttempt < maxRetries) {
                                delay(backoffMs)
                                backoffMs *= 2
                                currentAttempt++
                                return@use
                            }
                            return@withContext GeminiResult.Error(
                                messageBn = "অনুরোধের সীমা (কোটা) অতিক্রম করেছে। অনুগ্রহ করে কিছুক্ষণ পর চেষ্টা করুন।"
                            )
                        }
                        in 500..599 -> {
                            if (currentAttempt < maxRetries) {
                                delay(backoffMs)
                                backoffMs *= 2
                                currentAttempt++
                                return@use
                            }
                            return@withContext GeminiResult.Error(
                                messageBn = "Gemini সার্ভারে সাময়িক সমস্যা হচ্ছে। একটু পর আবার চেষ্টা করুন।"
                            )
                        }
                        else -> {
                            return@withContext GeminiResult.Error(
                                messageBn = "সংযোগ ত্রুটি ($responseCode)। ইন্টারনেট সংযোগ পরীক্ষা করুন।"
                            )
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(tag, "Network failure calling Gemini", e)
                if (currentAttempt < maxRetries) {
                    delay(backoffMs)
                    backoffMs *= 2
                    currentAttempt++
                } else {
                    return@withContext GeminiResult.Error(
                        messageBn = "ইন্টারনেট সংযোগ পাওয়া যায়নি। আপনার ওয়াই-ফাই বা মোবাইল ডেটা চেক করুন।"
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Unexpected error calling Gemini", e)
                return@withContext GeminiResult.Error(
                    messageBn = "একটি অপ্রত্যাশিত ত্রুটি ঘটেছে: ${e.localizedMessage ?: "অজ্ঞাত কারণ"}"
                )
            }
        }

        GeminiResult.Error(messageBn = "অনুরোধ সম্পন্ন করতে ব্যর্থ হয়েছে। পুনরায় চেষ্টা করুন।")
    }

    private fun buildRequestPayload(
        conversationHistory: List<Pair<MessageRole, String>>,
        activeMemories: List<Pair<String, String>>
    ): JSONObject {
        val root = JSONObject()

        // System Instruction
        val systemInstructionText = buildString {
            append("তুমি হচ্ছ JACK (Just A Rather Capable Kit), মার্ভেলের JARVIS-এর মতো একজন অত্যন্ত পারদর্শী, বুদ্ধিমান ও বিশ্বস্ত ব্যক্তিগত এআই সহকারী।\n")
            append("তোমার সাথে ব্যবহারকারী বাংলায় কথা বলছেন। সকল উত্তর সাবলীল, প্রাঞ্জল ও সম্মানসূচক বাংলায় দিবে।\n")
            append("ব্যবহারকারী বাংলা, ব্যাংলিশ (Banglish) অথবা ইংরেজি টেকনিক্যাল শব্দ বললে তা সঠিক অর্থে বুঝবে।\n")
            append("তুমি সৎ: কোনো ফোন অ্যাকশন (যেমন কল, এসএমএস, অ্যাপ ওপেন) বাস্তবে সম্পন্ন না হলে কখনই দাবি করবে না যে হয়েছে।\n")
            append("যদি ব্যবহারকারীর নির্দেশ অনুযায়ী কোনো কাজ সম্পন্ন করার প্রয়োজন হয়, তবে নির্ধারিত ফাংশন (Tool) কল করবে।\n")
            append("উচ্চ ঝুঁকিপূর্ণ কাজ (যেমন কল দেওয়া বা এসএমএস ড্রাফট করা) হলে ব্যবহারকারীকে স্পষ্ট করে জানাবে।\n")
            if (activeMemories.isNotEmpty()) {
                append("\n[ব্যবহারকারীর স্মরণীয় তথ্যসমূহ / User Memories]:\n")
                activeMemories.forEach { (k, v) ->
                    append("- $k: $v\n")
                }
            }
        }

        val systemInstructionObj = JSONObject().apply {
            put("parts", JSONArray().put(JSONObject().put("text", systemInstructionText)))
        }
        root.put("systemInstruction", systemInstructionObj)

        // Contents (Conversation History - bounded to last 15 messages)
        val contentsArray = JSONArray()
        val boundedHistory = conversationHistory.takeLast(15)

        for ((role, text) in boundedHistory) {
            val contentObj = JSONObject()
            contentObj.put("role", if (role == MessageRole.USER) "user" else "model")
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", text))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }
        root.put("contents", contentsArray)

        // Tools (Function Declarations for phone automation)
        val toolsArray = JSONArray()
        val toolsObj = JSONObject()
        val funcDeclarations = JSONArray().apply {
            put(createFunctionDecl("open_app", "একটি ইনস্টল করা অ্যাপ খুলতে ব্যবহৃত হয়", mapOf("app_name" to "অ্যাপের নাম যেমন YouTube, Chrome, Camera, WhatsApp, Calculator, Settings")))
            put(createFunctionDecl("open_settings", "ডিভাইসের নির্দিষ্ট সেটিংস পেজ ওপেন করে", mapOf("setting_type" to "wifi, bluetooth, sound, display, accessibility, app_details")))
            put(createFunctionDecl("web_search", "ইন্টারনেটে কোনো সাম্প্রতিক তথ্য, খবর বা বিষয় অনুসন্ধান করতে", mapOf("query" to "অনুসন্ধানের বিষয় বা প্রশ্ন")))
            put(createFunctionDecl("open_website", "নির্দিষ্ট কোনো ওয়েবসাইট ওপেন করে", mapOf("url" to "ওয়েবসাইট ইউআরএল (যেমন https://...)")))
            put(createFunctionDecl("dial_number", "ফোন ডায়ালারে নম্বর প্রস্তুত করে", mapOf("phone_number" to "ফোন নম্বর", "contact_name" to "যোগাযোগকারীর নাম (ঐচ্ছিক)")))
            put(createFunctionDecl("draft_sms", "নির্দিষ্ট নম্বরে এসএমএস ড্রাফট প্রস্তুত করে", mapOf("phone_number" to "ফোন নম্বর", "message" to "এসএমএস মেসেজ")))
            put(createFunctionDecl("set_timer", "নির্দিষ্ট মিনিটের জন্য টাইমার সেট করে", mapOf("minutes" to "টাইমারের সময় মিনিটে (পূর্ণসংখ্যা)", "label" to "টাইমারের নাম বা বিবরণ")))
            put(createFunctionDecl("set_alarm", "নির্দিষ্ট সময়ে অ্যালার্ম সেট করে", mapOf("hour" to "ঘণ্টা (০-২৩)", "minute" to "মিনিট (০-৫৯)", "label" to "অ্যালার্মের নাম")))
            put(createFunctionDecl("save_memory", "ব্যবহারকারীর কোনো তথ্য বা পছন্দ ভবিষ্যতে মনে রাখার জন্য সেভ করে", mapOf("key" to "তথ্যের শিরোনাম", "value" to "তথ্যের বিবরণ")))
            put(createFunctionDecl("get_device_info", "ডিভাইসের বর্তমান স্ট্যাটাস (ব্যাটারি ও নেটওয়ার্ক) জানতে", emptyMap()))
        }
        toolsObj.put("functionDeclarations", funcDeclarations)
        toolsArray.put(toolsObj)
        root.put("tools", toolsArray)

        // Generation Config
        val genConfig = JSONObject().apply {
            put("temperature", 0.7)
            put("topP", 0.95)
            put("topK", 40)
        }
        root.put("generationConfig", genConfig)

        return root
    }

    private fun createFunctionDecl(name: String, description: String, properties: Map<String, String>): JSONObject {
        val decl = JSONObject()
        decl.put("name", name)
        decl.put("description", description)

        val parameters = JSONObject()
        parameters.put("type", "OBJECT")

        val props = JSONObject()
        val requiredList = JSONArray()

        properties.forEach { (propName, propDesc) ->
            val propObj = JSONObject()
            propObj.put("type", if (propName == "minutes" || propName == "hour" || propName == "minute") "INTEGER" else "STRING")
            propObj.put("description", propDesc)
            props.put(propName, propObj)
            if (propName != "contact_name" && propName != "label") {
                requiredList.put(propName)
            }
        }

        parameters.put("properties", props)
        if (requiredList.length() > 0) {
            parameters.put("required", requiredList)
        }
        decl.put("parameters", parameters)
        return decl
    }

    private fun parseSuccessResponse(jsonString: String): GeminiResult {
        try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return GeminiResult.Error("Gemini থেকে কোনো প্রতিক্রিয়া পাওয়া যায়নি।")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var replyText = ""
            var parsedAction: PhoneAction? = null
            val sources = mutableListOf<GroundingSource>()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        replyText += part.optString("text")
                    }
                    if (part.has("functionCall")) {
                        val functionCall = part.getJSONObject("functionCall")
                        val name = functionCall.optString("name")
                        val args = functionCall.optJSONObject("args") ?: JSONObject()
                        parsedAction = mapFunctionCallToAction(name, args)
                    }
                }
            }

            // Parse grounding metadata if present
            val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
            if (groundingMetadata != null) {
                val chunks = groundingMetadata.optJSONArray("groundingChunks")
                if (chunks != null) {
                    for (i in 0 until chunks.length()) {
                        val chunk = chunks.getJSONObject(i)
                        val web = chunk.optJSONObject("web")
                        if (web != null) {
                            val uri = web.optString("uri")
                            val title = web.optString("title").ifBlank { "তথ্যসূত্র" }
                            if (uri.isNotBlank()) {
                                sources.add(GroundingSource(title = title, url = uri))
                            }
                        }
                    }
                }
            }

            if (replyText.isBlank() && parsedAction == null) {
                replyText = "আমি আপনার নির্দেশটি পেয়েছি।"
            }

            return GeminiResult.Success(
                replyText = replyText.trim(),
                functionCallAction = parsedAction,
                sources = sources
            )
        } catch (e: Exception) {
            Log.e(tag, "Error parsing Gemini response", e)
            return GeminiResult.Error("প্রতিক্রিয়া বিশ্লেষণ করতে ত্রুটি হয়েছে: ${e.message}")
        }
    }

    private fun mapFunctionCallToAction(name: String, args: JSONObject): PhoneAction? {
        return try {
            when (name) {
                "open_app" -> {
                    val appName = args.optString("app_name", "")
                    if (appName.isNotBlank()) PhoneAction.OpenApp(appName = appName) else null
                }
                "open_settings" -> {
                    val type = args.optString("setting_type", "main")
                    PhoneAction.OpenSettings(settingType = type)
                }
                "web_search" -> {
                    val query = args.optString("query", "")
                    if (query.isNotBlank()) PhoneAction.WebSearch(query = query) else null
                }
                "open_website" -> {
                    var url = args.optString("url", "")
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://$url"
                    }
                    PhoneAction.OpenWebsite(url = url)
                }
                "dial_number" -> {
                    val phone = args.optString("phone_number", "")
                    val contact = args.optString("contact_name", "")
                    if (phone.isNotBlank()) PhoneAction.DialNumber(phoneNumber = phone, contactName = contact) else null
                }
                "draft_sms" -> {
                    val phone = args.optString("phone_number", "")
                    val msg = args.optString("message", "")
                    if (phone.isNotBlank()) PhoneAction.DraftSms(phoneNumber = phone, message = msg) else null
                }
                "set_timer" -> {
                    val minutes = args.optInt("minutes", 5)
                    val label = args.optString("label", "JACK টাইমার")
                    PhoneAction.SetTimer(minutes = minutes, label = label)
                }
                "set_alarm" -> {
                    val hour = args.optInt("hour", 7)
                    val minute = args.optInt("minute", 0)
                    val label = args.optString("label", "JACK অ্যালার্ম")
                    PhoneAction.SetAlarm(hour = hour, minute = minute, label = label)
                }
                "save_memory" -> {
                    val key = args.optString("key", "")
                    val value = args.optString("value", "")
                    if (key.isNotBlank()) PhoneAction.SaveMemory(key = key, value = value) else null
                }
                "get_device_info" -> {
                    PhoneAction.GetDeviceInfo
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to map function call: $name", e)
            null
        }
    }
}
