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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun BrainAndMemoryScreen(viewModel: AliViewModel) {
    val userMemories by viewModel.userMemories.collectAsState()
    val knowledgeNodes by viewModel.knowledgeNodes.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var newTopic by remember { mutableStateOf("") }
    var newExplanation by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AliDeepBg)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = AliSurface,
            contentColor = AliCyan
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Knowledge Graph (${knowledgeNodes.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Personal Memory (${userMemories.size})") }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Direct Teaching / Memory Card
                Surface(
                    color = AliSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AliCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (selectedTab == 0) "Teach ALI a New Concept (100% Offline)" else "Add a Personal Fact About You",
                            color = AliCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newTopic,
                            onValueChange = { newTopic = it },
                            label = {
                                Text(if (selectedTab == 0) "Topic / Concept Name" else "Memory Category (e.g. Goal, Preference)")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newExplanation,
                            onValueChange = { newExplanation = it },
                            label = {
                                Text(if (selectedTab == 0) "What should ALI know about this?" else "What should ALI remember about you?")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (newTopic.isNotBlank() && newExplanation.isNotBlank()) {
                                    if (selectedTab == 0) {
                                        viewModel.teachAliDirectly(newTopic, newExplanation)
                                    } else {
                                        viewModel.addManualUserMemory(
                                            category = newTopic,
                                            key = "manual_${System.currentTimeMillis()}",
                                            value = newExplanation
                                        )
                                    }
                                    newTopic = ""
                                    newExplanation = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AliCyan, contentColor = AliDeepBg),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Local Brain", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                items(knowledgeNodes, key = { it.id }) { node ->
                    Surface(
                        color = AliSurface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AliSurfaceVariant, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Hub, contentDescription = null, tint = AliCyan)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = node.topic,
                                        color = AliTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Surface(
                                    color = AliEmerald.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = node.sourceType,
                                        color = AliEmerald,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteKnowledgeNode(node) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AliRose)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = node.summary,
                                color = AliTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                items(userMemories, key = { it.id }) { mem ->
                    Surface(
                        color = AliSurface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AliViolet.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.PersonPin, contentDescription = null, tint = AliViolet)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${mem.category} • ${mem.factKey}",
                                        color = AliViolet,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mem.factValue,
                                        color = AliTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteUserMemory(mem) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AliRose)
                            }
                        }
                    }
                }
            }
        }
    }
}
