package com.ali.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ali.agent.ui.AliViewModel
import com.ali.agent.ui.theme.AliAmber
import com.ali.agent.ui.theme.AliCyan
import com.ali.agent.ui.theme.AliDeepBg
import com.ali.agent.ui.theme.AliEmerald
import com.ali.agent.ui.theme.AliRose
import com.ali.agent.ui.theme.AliSurface
import com.ali.agent.ui.theme.AliSurfaceVariant
import com.ali.agent.ui.theme.AliTextPrimary
import com.ali.agent.ui.theme.AliTextSecondary
import com.ali.agent.ui.theme.AliViolet

@Composable
fun PrivacyAndLearningScreen(viewModel: AliViewModel) {
    val isOnlineAllowed by viewModel.isOnlineLearningEnabled.collectAsState()
    val curiosityQueue by viewModel.curiosityQueue.collectAsState()
    val auditLogs by viewModel.privacyAuditLogs.collectAsState()

    var testSentence by remember {
        mutableStateOf("Hey ALI, my brother Rahul (9876543210) told me about Hawking radiation today, what is that?")
    }
    val previewResult = remember(testSentence) {
        viewModel.previewSanitizer(testSentence)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AliDeepBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Master Privacy Firewall Card
        item {
            Surface(
                color = AliSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = if (isOnlineAllowed) AliCyan else AliEmerald,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isOnlineAllowed) Icons.Default.Public else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isOnlineAllowed) AliCyan else AliEmerald
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isOnlineAllowed) "Private Online Curiosity: ON" else "Strict Offline Firewall: LOCKED",
                                    color = AliTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isOnlineAllowed)
                                        "ALI strips all personal context & anonymously learns public encyclopedic facts."
                                    else
                                        "100% air-gapped. Unfamiliar topics are saved to your Curiosity Queue for later.",
                                    color = AliTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = isOnlineAllowed,
                            onCheckedChange = { viewModel.toggleOnlineLearning(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AliDeepBg,
                                checkedTrackColor = AliCyan,
                                uncheckedThumbColor = AliEmerald,
                                uncheckedTrackColor = AliSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // 2. Live PII Sanitizer Tester
        item {
            Surface(
                color = AliSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AliViolet.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = AliViolet)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Zero-Leak Privacy Sanitizer (Live Inspector)",
                            color = AliViolet,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Test how ALI strips names, phone numbers, emails, and personal context before ever touching Wikipedia or DuckDuckGo:",
                        color = AliTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = testSentence,
                        onValueChange = { testSentence = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Sample sentence spoken to ALI") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = AliDeepBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Stripped Personal Data (Kept strictly on phone): ${
                                    if (previewResult.strippedElements.isEmpty()) "Conversational framing"
                                    else previewResult.strippedElements.joinToString(", ")
                                }",
                                color = AliAmber,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Outbound Anonymous Topic Query: \"${previewResult.anonymousTopic}\"",
                                color = AliEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Curiosity Queue (Topics mentioned while offline)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Offline Curiosity Queue (${curiosityQueue.size})",
                    color = AliCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (isOnlineAllowed && curiosityQueue.any { it.status == "PENDING_INTERNET" }) {
                    Button(
                        onClick = { viewModel.syncCuriosityQueueNow() },
                        colors = ButtonDefaults.buttonColors(containerColor = AliCyan, contentColor = AliDeepBg)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Learn All Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        items(curiosityQueue, key = { it.id }) { item ->
            Surface(
                color = AliSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Topic: \"${item.sanitizedQuery}\"",
                            color = AliTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Status: ${item.status}",
                            color = if (item.status == "LEARNED") AliEmerald else AliAmber,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { viewModel.deleteCuriosityItem(item) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = AliRose)
                    }
                }
            }
        }

        // 4. Privacy Audit Log
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = AliEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transparency & Privacy Audit Log (${auditLogs.size})",
                    color = AliEmerald,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        items(auditLogs, key = { it.id }) { log ->
            Surface(
                color = AliSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AliSurfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = log.eventType,
                            color = if (log.eventType == "BLOCKED_OFFLINE") AliAmber else AliEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = log.destinationHost,
                            color = AliTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Outbound Query: ${log.outboundAnonymousQuery}",
                        color = AliTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Scrubbed Locally: ${log.strippedPiiSummary}",
                        color = AliTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
