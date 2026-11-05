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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.text.style.TextDecoration
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
fun DayToDayHubScreen(viewModel: AliViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val notes by viewModel.notes.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var titleInput by remember { mutableStateOf("") }
    var detailInput by remember { mutableStateOf("") }

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
                text = { Text("Smart Tasks (${tasks.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Quick Notes (${notes.size})") }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick Phone Action Triggers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.sendMessage("Set an alarm for 7:00 AM") },
                        colors = ButtonDefaults.buttonColors(containerColor = AliSurfaceVariant, contentColor = AliCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("7:00 AM Alarm", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { viewModel.sendMessage("Set a timer for 25 minutes") },
                        colors = ButtonDefaults.buttonColors(containerColor = AliSurfaceVariant, contentColor = AliViolet),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("25m Pomodoro", fontSize = 12.sp)
                    }
                }
            }

            // Add Task / Note Card
            item {
                Surface(
                    color = AliSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AliCyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (selectedTab == 0) "New Day-to-Day Task / Reminder" else "Capture Private Voice/Text Note",
                            color = AliCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = {
                                Text(if (selectedTab == 0) "What needs to get done?" else "Note Title")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = detailInput,
                            onValueChange = { detailInput = it },
                            label = {
                                Text(if (selectedTab == 0) "When? (e.g. Today 6:00 PM, Tomorrow)" else "Note Content")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (selectedTab == 0 && titleInput.isNotBlank()) {
                                    viewModel.addTask(titleInput, detailInput, "NORMAL")
                                    titleInput = ""
                                    detailInput = ""
                                } else if (selectedTab == 1 && (titleInput.isNotBlank() || detailInput.isNotBlank())) {
                                    viewModel.addNote(titleInput, detailInput.ifBlank { titleInput })
                                    titleInput = ""
                                    detailInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AliCyan, contentColor = AliDeepBg),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (selectedTab == 0) "Add Task" else "Save Note", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                items(tasks, key = { it.id }) { task ->
                    Surface(
                        color = AliSurface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(onClick = { viewModel.toggleTaskCompleted(task) }) {
                                    Icon(
                                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Complete",
                                        tint = if (task.isCompleted) AliEmerald else AliCyan
                                    )
                                }
                                Column {
                                    Text(
                                        text = task.title,
                                        color = if (task.isCompleted) AliTextSecondary else AliTextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    Text(
                                        text = "${task.dueTimeText} • ${task.priority}",
                                        color = if (task.priority == "HIGH") AliAmber else AliTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteTask(task) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AliRose)
                            }
                        }
                    }
                }
            } else {
                items(notes, key = { it.id }) { note ->
                    Surface(
                        color = AliSurface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AliViolet.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.NoteAlt, contentDescription = null, tint = AliViolet)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = note.title,
                                        color = AliTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteNote(note) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AliRose)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.body,
                                color = AliTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
