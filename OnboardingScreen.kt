package com.example.ui.screens

import android.Manifest
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState
import com.example.ui.components.JackOrb
import com.example.ui.theme.JackCyanNeon
import com.example.ui.theme.JackSpaceDark
import com.example.ui.theme.JackSurfaceCard
import com.example.ui.theme.JackSurfaceElevated
import com.example.ui.theme.JackTextPrimary
import com.example.ui.theme.JackTextSecondary
import com.example.viewmodel.JackAssistantViewModel

@Composable
fun OnboardingScreen(
    viewModel: JackAssistantViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.completeOnboarding()
        onComplete()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JackSpaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            JackOrb(
                state = AssistantState.READY,
                audioLevel = 0.5f,
                size = 180.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "স্বাগতম, JACK এআই",
                color = JackTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "JARVIS-অনুপ্রাণিত আপনার নির্ভরযোগ্য বাংলা ব্যক্তিগত এআই সহকারী",
                color = JackCyanNeon,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Highlight Cards
            val highlights = listOf(
                Triple(Icons.Default.Mic, "স্বাভাবিক বাংলা কথোপকথন", "বাংলা, ব্যাংলিশ ও ইংরেজি সহজে বুঝে কথা বলে"),
                Triple(Icons.Default.Security, "নিরাপদ ফোন অটোমেশন", "অনুমতি সাপেক্ষে কল, এসএমএস, অ্যাপ ও সেটিংস নিয়ন্ত্রণ"),
                Triple(Icons.Default.Notifications, "স্মার্ট টাস্ক প্ল্যানার", "সত্যতা যাচাই ছাড়া কোনো কাজের মিথ্যা দাবি করে না")
            )

            highlights.forEach { (icon, title, desc) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(JackSurfaceCard)
                        .border(1.dp, JackCyanNeon.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(JackSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = JackCyanNeon, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = title, color = JackTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = desc, color = JackTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Get Started Button
        Button(
            onClick = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = JackCyanNeon,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("get_started_button")
        ) {
            Text(
                text = "JACK শুরু করুন",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
