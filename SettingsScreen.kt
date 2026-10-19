package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.BuildConfig
import com.example.ui.theme.JackAmberConfirm
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
fun SettingsScreen(
    viewModel: JackAssistantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isJackOn by viewModel.isJackOn.collectAsState()
    val autoSpeak by viewModel.autoSpeak.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val speechPitch by viewModel.speechPitch.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    var tempApiKey by remember(customApiKey) { mutableStateOf(customApiKey) }

    val micGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Refreshes state automatically */ }

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "ফিরে যান",
                    tint = JackCyanNeon
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "সিস্টেম সেটিংস",
                color = JackTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. Master Jack Power Card ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = JackSurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isJackOn) JackGreenReady.copy(alpha = 0.5f) else JackRedAlert.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (isJackOn) JackGreenReady else JackTextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "JACK মাস্টার সুইচ",
                                color = JackTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isJackOn) "সহকারী সক্রিয় ও যেকোনো কমান্ডের জন্য প্রস্তুত" else "সহকারী সম্পূর্ণ বন্ধ। মাইক ও ব্যাকগ্রাউন্ড কাজ নিষ্ক্রিয়।",
                                color = JackTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = isJackOn,
                        onCheckedChange = { viewModel.toggleJackPower(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JackGreenReady,
                            checkedTrackColor = JackSurfaceElevated,
                            uncheckedThumbColor = JackTextMuted,
                            uncheckedTrackColor = JackSurfaceElevated
                        ),
                        modifier = Modifier.testTag("settings_master_switch")
                    )
                }
            }

            // --- 2. Gemini AI Integration Card ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = JackSurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JackCyanNeon.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = JackCyanNeon, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini AI সংযোগ ও মডেল",
                            color = JackTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val hasBuildKey = try {
                        BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                    } catch (e: Throwable) {
                        false
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(JackSurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (hasBuildKey || customApiKey.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasBuildKey || customApiKey.isNotBlank()) JackGreenReady else JackAmberConfirm,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasBuildKey || customApiKey.isNotBlank()) "Gemini API কী কনফিগার করা রয়েছে" else "API কী পাওয়া যায়নি। নিচে সরাসরি দিন বা Secrets প্যানেলে সেট করুন।",
                                color = JackTextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        label = { Text("কাস্টম Gemini API কী (ঐচ্ছিক)", fontSize = 12.sp) },
                        placeholder = { Text("AIzaSy...", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JackCyanNeon,
                            unfocusedBorderColor = JackSurfaceElevated,
                            focusedTextColor = JackTextPrimary,
                            unfocusedTextColor = JackTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_api_key_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.updateCustomApiKey(tempApiKey) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JackCyanNeon,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("কী সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "নির্বাচিত মডেল: $selectedModel",
                        color = JackCyanNeon,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.updateSelectedModel("gemini-3.5-flash") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selectedModel == "gemini-3.5-flash") JackCyanNeon else JackTextSecondary
                            )
                        ) {
                            Text("gemini-3.5-flash (দ্রুত)")
                        }
                        OutlinedButton(
                            onClick = { viewModel.updateSelectedModel("gemini-3.1-pro-preview") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selectedModel == "gemini-3.1-pro-preview") JackCyanNeon else JackTextSecondary
                            )
                        ) {
                            Text("gemini-3.1-pro")
                        }
                    }
                }
            }

            // --- 3. Voice & Audio Settings ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = JackSurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JackCyanNeon.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = JackCyanNeon, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ভয়েস ও অডিও স্পিচ",
                            color = JackTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("স্বয়ংক্রিয় ভয়েস উত্তর", color = JackTextPrimary, fontSize = 14.sp)
                            Text("উত্তরের সময় মুখে বাংলা অডিও বাজাবে", color = JackTextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = autoSpeak,
                            onCheckedChange = { viewModel.updateAutoSpeak(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = JackCyanNeon)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("কথা বলার গতি (Speed): ${String.format("%.1fx", speechRate)}", color = JackTextPrimary, fontSize = 13.sp)
                    Slider(
                        value = speechRate,
                        onValueChange = { viewModel.updateSpeechRate(it) },
                        valueRange = 0.7f..1.5f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = JackCyanNeon, activeTrackColor = JackCyanNeon)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("ভয়েস পিচ (Pitch): ${String.format("%.1f", speechPitch)}", color = JackTextPrimary, fontSize = 13.sp)
                    Slider(
                        value = speechPitch,
                        onValueChange = { viewModel.updateSpeechPitch(it) },
                        valueRange = 0.7f..1.4f,
                        steps = 7,
                        colors = SliderDefaults.colors(thumbColor = JackCyanNeon, activeTrackColor = JackCyanNeon)
                    )
                }
            }

            // --- 4. Wake Word Status & Honest Documentation ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = JackSurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JackCyanNeon.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = JackCyanNeon, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ওয়েক-ওয়ার্ড (Wake Word: 'Jack')",
                            color = JackTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(JackSurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "সততা ও অপ্টিমাইজেশন: ডিভাইস ব্যাটারি ও গোপনীয়তা রক্ষার জন্য সাধারণ পুশ-টু-টক (Push-to-Talk) মোড ডিফল্ট রাখা হয়েছে। অফলাইন 'Jack' সার্বক্ষণিক ডিটেকশন প্রপ্রাইটারি বাইনারি মডেল লাইসেন্স ব্যতীত অ্যান্ড্রয়েড সাধারণ স্পিচ লুপে চালালে অতিরিক্ত চার্জ ও হিট তৈরি হয়。",
                            color = JackTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // --- 5. Permissions & Privacy Dashboard ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = JackSurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JackCyanNeon.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = JackCyanNeon, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "অনুমতি ও গোপনীয়তা ড্যাশবোর্ড",
                            color = JackTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mic Permission
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (micGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (micGranted) JackGreenReady else JackRedAlert,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("মাইক্রোফোন অনুমতি", color = JackTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("বাংলা কথা শোনার জন্য প্রয়োজনীয়", color = JackTextSecondary, fontSize = 11.sp)
                            }
                        }

                        if (!micGranted) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = JackCyanNeon, contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("অনুমতি দিন", fontSize = 11.sp)
                            }
                        } else {
                            Text("অনুমোদিত", color = JackGreenReady, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // App System Settings Shortcut
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JackCyanNeon),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("অ্যান্ড্রয়েড সিস্টেম পারমিশন সেটিংস খুলুন")
                    }
                }
            }

            // --- 6. Supported Phone Capabilities ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = JackSurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JackCyanNeon.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, tint = JackCyanNeon, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "সমর্থিত ফোন অটোমেশন নির্দেশাবলী",
                            color = JackTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val features = listOf(
                        "• ইনস্টল করা অ্যাপ ওপেন করা (যেমন: ইউটিউব, ক্যামেরা, ক্রোম)",
                        "• ওয়াই-ফাই, ব্লুটুথ ও সাউন্ড সেটিংস পেজ খোলা",
                        "• ইন্টারনেটে যেকোনো বিষয় অনুসন্ধান (Web Search)",
                        "• টাইমার ও অ্যালার্ম সেট করা",
                        "• ফোন ডায়ালারে নম্বর প্রস্তুত করা (কলের পূর্বে নিশ্চিতকরণ)",
                        "• এসএমএস ড্রাফট প্রস্তুত করা (পাঠানোর পূর্বে নিশ্চিতকরণ)",
                        "• ডিভাইস ব্যাটারি ও নেটওয়ার্ক স্ট্যাটাস চেক",
                        "• ব্যক্তিগত তথ্য ও পছন্দ স্মৃতিতে সংরক্ষণ করা"
                    )

                    features.forEach { feature ->
                        Text(
                            text = feature,
                            color = JackTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
