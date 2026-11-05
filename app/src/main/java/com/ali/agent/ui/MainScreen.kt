package com.ali.agent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ali.agent.ui.screens.BrainAndMemoryScreen
import com.ali.agent.ui.screens.ChatAndVoiceScreen
import com.ali.agent.ui.screens.DayToDayHubScreen
import com.ali.agent.ui.screens.ModelCenterScreen
import com.ali.agent.ui.screens.PrivacyAndLearningScreen
import com.ali.agent.ui.theme.AliCyan
import com.ali.agent.ui.theme.AliDeepBg
import com.ali.agent.ui.theme.AliEmerald
import com.ali.agent.ui.theme.AliSurface
import com.ali.agent.ui.theme.AliSurfaceVariant
import com.ali.agent.ui.theme.AliTextPrimary
import com.ali.agent.ui.theme.AliTextSecondary
import com.ali.agent.ui.theme.AliViolet

@Composable
fun MainScreen(viewModel: AliViewModel) {
    var currentTab by remember { mutableIntStateOf(0) }
    val isOnlineAllowed by viewModel.isOnlineLearningEnabled.collectAsState()
    val isVoiceOutEnabled by viewModel.voiceManager.voiceOutputEnabled.collectAsState()

    Scaffold(
        topBar = {
            Surface(
                color = AliSurface,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // ALI Branding
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(AliCyan, AliViolet)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ALI",
                                color = AliDeepBg,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ALI -• Advanced Learning Interface",
                                color = AliTextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Human Thinking Companion • Zero Data Sharing",
                                color = AliCyan,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Quick Privacy Firewall Pill
                    Surface(
                        color = if (isOnlineAllowed) AliCyan.copy(alpha = 0.16f) else AliEmerald.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = if (isOnlineAllowed) AliCyan else AliEmerald,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.toggleOnlineLearning(!isOnlineAllowed) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOnlineAllowed) Icons.Default.Public else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isOnlineAllowed) AliCyan else AliEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOnlineAllowed) "Curiosity ON" else "100% Offline",
                                color = if (isOnlineAllowed) AliCyan else AliEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Voice Output Toggle
                    IconButton(onClick = { viewModel.voiceManager.toggleVoiceOutput() }) {
                        Icon(
                            imageVector = if (isVoiceOutEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Voice Output",
                            tint = if (isVoiceOutEnabled) AliCyan else AliTextSecondary
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = AliSurface,
                contentColor = AliTextPrimary
            ) {
                val navItems = listOf(
                    Triple("Talk", Icons.Default.ChatBubbleOutline, 0),
                    Triple("Brain", Icons.Default.Psychology, 1),
                    Triple("Privacy", Icons.Default.Shield, 2),
                    Triple("Day Hub", Icons.Default.Today, 3),
                    Triple("Models", Icons.Default.Memory, 4)
                )
                navItems.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = { currentTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AliDeepBg,
                            selectedTextColor = AliCyan,
                            indicatorColor = AliCyan,
                            unselectedIconColor = AliTextSecondary,
                            unselectedTextColor = AliTextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AliDeepBg)
        ) {
            when (currentTab) {
                0 -> ChatAndVoiceScreen(viewModel)
                1 -> BrainAndMemoryScreen(viewModel)
                2 -> PrivacyAndLearningScreen(viewModel)
                3 -> DayToDayHubScreen(viewModel)
                4 -> ModelCenterScreen(viewModel)
            }
        }
    }
}
