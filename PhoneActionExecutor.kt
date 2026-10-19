package com.example.data.action

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import com.example.model.PhoneAction

data class ActionResult(
    val isSuccess: Boolean,
    val messageBn: String
)

class PhoneActionExecutor(private val context: Context) {

    private val tag = "PhoneActionExecutor"

    fun execute(action: PhoneAction): ActionResult {
        return try {
            when (action) {
                is PhoneAction.OpenApp -> executeOpenApp(action)
                is PhoneAction.OpenSettings -> executeOpenSettings(action)
                is PhoneAction.WebSearch -> executeWebSearch(action)
                is PhoneAction.OpenWebsite -> executeOpenWebsite(action)
                is PhoneAction.DialNumber -> executeDialNumber(action)
                is PhoneAction.DraftSms -> executeDraftSms(action)
                is PhoneAction.SetTimer -> executeSetTimer(action)
                is PhoneAction.SetAlarm -> executeSetAlarm(action)
                is PhoneAction.SaveMemory -> ActionResult(true, "তথ্য সফলভাবে সংরক্ষিত হয়েছে।")
                is PhoneAction.GetDeviceInfo -> executeGetDeviceInfo()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to execute action: $action", e)
            ActionResult(
                isSuccess = false,
                messageBn = "কাজটি সম্পন্ন করা সম্ভব হয়নি: ${e.localizedMessage ?: "অনাকাঙ্ক্ষিত সমস্যা"}"
            )
        }
    }

    private fun executeOpenApp(action: PhoneAction.OpenApp): ActionResult {
        val pm = context.packageManager
        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "camera" to "com.android.camera",
            "whatsapp" to "com.whatsapp",
            "maps" to "com.google.android.apps.maps",
            "calculator" to "com.google.android.calculator",
            "clock" to "com.google.android.deskclock",
            "photos" to "com.google.android.apps.photos",
            "play store" to "com.android.vending",
            "settings" to "com.android.settings"
        )

        val targetPackage = action.packageName
            ?: knownPackages[action.appName.lowercase()]

        var launchIntent: Intent? = null
        if (targetPackage != null) {
            launchIntent = pm.getLaunchIntentForPackage(targetPackage)
        }

        if (launchIntent == null) {
            // Search all installed launcher intents
            val queryIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(queryIntent, 0)
            val matchedApp = apps.find {
                it.loadLabel(pm).toString().contains(action.appName, ignoreCase = true) ||
                it.activityInfo.packageName.contains(action.appName, ignoreCase = true)
            }
            if (matchedApp != null) {
                launchIntent = pm.getLaunchIntentForPackage(matchedApp.activityInfo.packageName)
            }
        }

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ActionResult(true, "${action.appName} অ্যাপ খোলা হয়েছে।")
        } else {
            ActionResult(false, "${action.appName} অ্যাপটি ডিভাইসে খুঁজে পাওয়া যায়নি।")
        }
    }

    private fun executeOpenSettings(action: PhoneAction.OpenSettings): ActionResult {
        val intentAction = when (action.settingType.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "app_details" -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        val intent = Intent(intentAction).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intentAction == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }

        context.startActivity(intent)
        return ActionResult(true, "${action.getSummaryBn()} সফলভাবে খোলা হয়েছে।")
    }

    private fun executeWebSearch(action: PhoneAction.WebSearch): ActionResult {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, action.query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult(true, "\"${action.query}\"-এর অনুসন্ধান ব্রাউজারে প্রস্তুত করা হয়েছে।")
        } catch (e: Exception) {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=" + Uri.encode(action.query))
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            ActionResult(true, "\"${action.query}\"-এর ফলাফল খোলা হয়েছে।")
        }
    }

    private fun executeOpenWebsite(action: PhoneAction.OpenWebsite): ActionResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(true, "${action.url} পেজটি ব্রাউজারে খোলা হয়েছে।")
    }

    private fun executeDialNumber(action: PhoneAction.DialNumber): ActionResult {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.phoneNumber}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(true, "${action.phoneNumber} নম্বরটি ফোন ডায়ালারে প্রস্তুত করা হয়েছে।")
    }

    private fun executeDraftSms(action: PhoneAction.DraftSms): ActionResult {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${action.phoneNumber}")).apply {
            putExtra("sms_body", action.message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(true, "${action.phoneNumber} নম্বরে এসএমএস ড্রাফট প্রস্তুত করা হয়েছে।")
    }

    private fun executeSetTimer(action: PhoneAction.SetTimer): ActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, action.minutes * 60)
            putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(true, "${action.minutes} মিনিটের জন্য টাইমার সেট করা হয়েছে।")
    }

    private fun executeSetAlarm(action: PhoneAction.SetAlarm): ActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, action.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(true, String.format("%02d:%02d সময়ে অ্যালার্ম সেট করা হয়েছে।", action.hour, action.minute))
    }

    private fun executeGetDeviceInfo(): ActionResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val status = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } else false

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val netType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "ওয়াই-ফাই (Wi-Fi)"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "মোবাইল ডেটা (Cellular)"
            else -> "সংযোগ বিচ্ছিন্ন"
        }

        val infoBn = "ডিভাইসের স্ট্যাটাস:\n- ব্যাটারি: $batteryPct% ${if (isCharging) "(চার্জ হচ্ছে)" else ""}\n- নেটওয়ার্ক: $netType\n- অ্যান্ড্রয়েড সংস্করণ: ${Build.VERSION.RELEASE}"
        return ActionResult(true, infoBn)
    }
}
