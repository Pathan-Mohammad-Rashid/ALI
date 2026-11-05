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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ali.agent.ui.AliViewModel
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
fun ModelCenterScreen(viewModel: AliViewModel) {
    val activeModelName by viewModel.llamaBridge.loadedModelName.collectAsState()
    val activeMetadata by viewModel.llamaBridge.loadedModelMetadata.collectAsState()
    val downloadStatus by viewModel.modelDownloadManager.downloadStatus.collectAsState()
    val catalog = viewModel.modelDownloadManager.catalog

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AliDeepBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Hybrid Engine Card
        item {
            Surface(
                color = AliSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, AliCyan, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = AliCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Active On-Device Brain Engine",
                            color = AliCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeModelName,
                        color = AliTextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = activeMetadata,
                        color = AliEmerald,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ALI's built-in Autonomous Cognitive Engine runs out-of-the-box with zero download required. You can also download 100% free open-weights GGUF neural models below for expanded offline tensor inference.",
                        color = AliTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Download progress bar if active
        if (downloadStatus.isDownloading) {
            item {
                Surface(
                    color = AliSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = downloadStatus.statusText,
                            color = AliTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadStatus.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = AliCyan,
                            trackColor = AliDeepBg
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Free Unlimited Open-Weights GGUF Models",
                color = AliTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        items(catalog, key = { it.id }) { item ->
            val isDownloaded = viewModel.modelDownloadManager.isModelDownloaded(item)
            Surface(
                color = AliSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isDownloaded) AliEmerald.copy(alpha = 0.5f) else AliSurfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.displayName,
                                color = AliTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${item.parameterCount} Params • ${item.quantization} • ~${item.approxSizeMb} MB",
                                color = AliViolet,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (isDownloaded) {
                            IconButton(onClick = { viewModel.deleteDownloadedModel(item) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AliRose)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.description,
                        color = AliTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.downloadAndActivateModel(item) },
                        enabled = !downloadStatus.isDownloading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDownloaded) AliEmerald else AliCyan,
                            contentColor = AliDeepBg
                        ),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDownloaded) "Activate Offline GGUF" else "Download Free (${item.approxSizeMb} MB)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
