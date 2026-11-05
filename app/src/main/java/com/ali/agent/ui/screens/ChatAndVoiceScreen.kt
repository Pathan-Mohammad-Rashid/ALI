package com.ali.agent.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ali.agent.data.db.ChatMessageEntity
import com.ali.agent.ui.AliViewModel
import com.ali.agent.ui.theme.AliAmber
import com.ali.agent.ui.theme.AliCyan
import com.ali.agent.ui.theme.AliDeepBg
import com.ali.agent.ui.theme.AliEmerald
import com.ali.agent.ui.theme.AliSurface
import com.ali.agent.ui.theme.AliSurfaceVariant
import com.ali.agent.ui.theme.AliTextPrimary
import com.ali.agent.ui.theme.AliTextSecondary
import com.ali.agent.ui.theme.AliViolet

@Composable
fun ChatAndVoiceScreen(viewModel: AliViewModel) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val partialSpeech by viewModel.voiceManager.livePartialTranscript.collectAsState()
    val isOnlineAllowed by viewModel.isOnlineLearningEnabled.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.voiceManager.startListening(preferOffline = !isOnlineAllowed)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "Give me my daily briefing",
        "Set an alarm for 6:45 AM",
        "Remind me to call Mom at 7pm",
        "What do you remember about me?",
        "Explain neuroplasticity",
        "Calculate 18% of 3450"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AliDeepBg)
    ) {
        // Live Neural Voice & Cognitive Status Banner
        NeuralPresenceHeader(
            isListening = isListening,
            isSpeaking = isSpeaking,
            isThinking = isThinking,
            partialTranscript = partialSpeech,
            onStopSpeaking = { viewModel.voiceManager.stopSpeaking() }
        )

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubbleItem(
                    message = msg,
                    onSpeakAgain = {
                        viewModel.voiceManager.speakHumanLike(msg.content, msg.detectedEmotion)
                    }
                )
            }
        }

        // Quick Interaction Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AliSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier
                        .border(1.dp, AliCyan.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .clickable { viewModel.sendMessage(prompt) }
                ) {
                    Text(
                        text = prompt,
                        color = AliTextPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Voice & Text Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AliSurface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing Microphone Button
            val pulseTransition = rememberInfiniteTransition(label = "micPulse")
            val micScale by pulseTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isListening) 1.16f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "micScale"
            )

            Box(
                modifier = Modifier
                    .scale(micScale)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isListening) Brush.linearGradient(listOf(AliAmber, AliViolet))
                        else Brush.linearGradient(listOf(AliCyan.copy(alpha = 0.25f), AliViolet.copy(alpha = 0.35f)))
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isListening) AliAmber else AliCyan,
                        shape = CircleShape
                    )
                    .clickable {
                        if (isListening) {
                            viewModel.voiceManager.stopListening()
                        } else {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                viewModel.voiceManager.startListening(preferOffline = !isOnlineAllowed)
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Speak to ALI",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Talk to ALI naturally, teach a fact, or ask anything...",
                        color = AliTextSecondary,
                        fontSize = 13.sp
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AliCyan,
                    unfocusedBorderColor = AliSurfaceVariant,
                    focusedContainerColor = AliDeepBg,
                    unfocusedContainerColor = AliDeepBg,
                    focusedTextColor = AliTextPrimary,
                    unfocusedTextColor = AliTextPrimary
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(AliCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = AliDeepBg
                )
            }
        }
    }
}

@Composable
private fun NeuralPresenceHeader(
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean,
    partialTranscript: String,
    onStopSpeaking: () -> Unit
) {
    val activeStatus = when {
        isListening -> "Listening to your voice... ${partialTranscript.ifBlank { "" }}"
        isThinking -> "ALI is thinking & checking local memory graph..."
        isSpeaking -> "ALI is speaking with human prosody (Tap to mute)"
        else -> "Human Cognitive Mode Active • 100% Free & Unlimited"
    }

    val accentColor = when {
        isListening -> AliAmber
        isThinking -> AliViolet
        isSpeaking -> AliCyan
        else -> AliEmerald
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AliSurface.copy(alpha = 0.9f))
            .clickable(enabled = isSpeaking) { onStopSpeaking() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = activeStatus,
            color = AliTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessageEntity,
    onSpeakAgain: () -> Unit
) {
    val isUser = message.role == "user"
    var showInnerThought by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AliCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "ALI -• ${message.detectedEmotion}",
                    color = AliCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                message.learnedSource?.let { src ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = AliViolet.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = src,
                            color = Color(0xFFD8B4FE),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) AliViolet.copy(alpha = 0.35f) else AliSurface,
            modifier = Modifier
                .widthIn(max = 330.dp)
                .border(
                    width = 1.dp,
                    color = if (isUser) AliViolet.copy(alpha = 0.6f) else AliCyan.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.content,
                    color = AliTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                if (!isUser && message.innerThought.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showInnerThought = !showInnerThought },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = AliTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showInnerThought) "Hide thinking trace" else "View ALI's inner thought",
                                color = AliTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Speak",
                                tint = AliCyan,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onSpeakAgain() }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (showInnerThought) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = AliTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showInnerThought) {
                        Surface(
                            color = AliDeepBg,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = message.innerThought,
                                color = AliEmerald,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
