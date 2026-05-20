package com.crescendo.alarm.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crescendo.alarm.data.Alarm
import com.crescendo.alarm.data.SleepSchedule
import com.crescendo.alarm.data.Task
import com.crescendo.alarm.viewmodel.AlarmViewModel

private val BgDark       = Color(0xFF0A0A1A)
private val CardEnabled  = Color(0xFF16213E)
private val AccentBlue   = Color(0xFF63B3ED)
private val AccentPurple = Color(0xFF9F7AEA)
private val AccentGreen  = Color(0xFF68D391)
private val AccentRed    = Color(0xFFFC8181)
private val AccentOrange = Color(0xFFF6AD55)
private val TextMuted    = Color(0xFF8899AA)

@Composable
fun HomeScreen(
    viewModel: AlarmViewModel,
    onAddAlarm: () -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    onOpenSleep: () -> Unit
) {
    val alarms  by viewModel.alarms.collectAsState()
    val sleep   by viewModel.sleepSchedule.collectAsState()
    val ringing by viewModel.isRinging.collectAsState()
    var tab     by remember { mutableIntStateOf(0) }
    var showNameDialog by remember { mutableStateOf(false) }

    // Only show dialog if we have data and the name is blank
    LaunchedEffect(sleep.userName) {
        if (sleep.userName.isNotBlank()) {
            showNameDialog = false
        } else if (sleep.id != -1) { 
            // We use id != -1 as a signal that the real data (or default row) is loaded
            showNameDialog = true
        }
    }

    if (showNameDialog) {
        NameSetupDialog(onSave = {
            viewModel.setUserName(it)
            showNameDialog = false
        })
    }

    Scaffold(containerColor = BgDark, floatingActionButton = {
        if (tab == 0) FloatingActionButton(onClick = onAddAlarm,
            containerColor = AccentBlue, contentColor = Color.White, shape = CircleShape) {
            Icon(Icons.Default.Add, "Add")
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))

            if (ringing) {
                Button(
                    onClick = { viewModel.stopRinging() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("🔔 STOP ALARM", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
            }

            // Tab bar
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111122)).padding(4.dp)) {
                listOf("⏰ Alarms", "🌙 Sleep", "✅ Tasks", "ℹ️ Info").forEachIndexed { idx, title ->
                    val sel = tab == idx
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (sel) Color(0x2663B3ED) else Color.Transparent)
                        .clickable { tab = idx }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center) {
                        Text(title,
                            color = if (sel) AccentBlue else TextMuted,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            when (tab) {
                0 -> AlarmsTab(alarms, viewModel, onEditAlarm)
                1 -> SleepSummaryTab(sleep, onOpenSleep)
                2 -> TasksTab(viewModel)
                3 -> AboutTab(
                    sleep,
                    onEditName = { viewModel.setUserName(it) },
                    onUpdateVoice = { p, r -> viewModel.setVoiceSettings(p, r) },
                    onPreview = { viewModel.previewVoice() }
                )
            }
        }
    }
}

// ── Alarms Tab ─────────────────────────────────────────────────────────────

@Composable
private fun AlarmsTab(alarms: List<Alarm>, viewModel: AlarmViewModel, onEditAlarm: (Alarm) -> Unit) {
    val next = alarms.filter { it.enabled }.minByOrNull { it.hour * 60 + it.minute }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (next != null) {
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color(0x1463B3ED)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📈", fontSize = 18.sp)
                    Text("Next: ", color = TextMuted, fontSize = 13.sp)
                    Text(next.formattedTime(), color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("· ${next.label}", color = TextMuted, fontSize = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        item {
            Text("My Alarms", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }
        if (alarms.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                    Text("No alarms yet. Tap + to add one.", color = TextMuted, fontSize = 14.sp)
                }
            }
        }
        items(alarms, key = { it.id }) { alarm ->
            AlarmCard(alarm, onToggle = { viewModel.toggleAlarm(alarm) }, onClick = { onEditAlarm(alarm) })
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun AlarmCard(alarm: Alarm, onToggle: () -> Unit, onClick: () -> Unit) {
    val bg by animateColorAsState(if (alarm.enabled) CardEnabled else Color(0xFF111122), label = "bg")
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg)
        .clickable(onClick = onClick).padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(alarm.label.uppercase(),
                    color = if (alarm.enabled) AccentBlue else TextMuted,
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(alarm.formattedTime(),
                    color = if (alarm.enabled) Color.White else Color(0xFF667788),
                    fontSize = 42.sp, fontWeight = FontWeight.Light, lineHeight = 44.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val dl = listOf("S","M","T","W","T","F","S")
                    alarm.daysAsList().forEachIndexed { i, on ->
                        Box(Modifier.size(26.dp).clip(CircleShape)
                            .background(if (on) AccentBlue else Color(0x1AFFFFFF)),
                            contentAlignment = Alignment.Center) {
                            Text(dl[i], color = if (on) Color.White else TextMuted,
                                fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = alarm.enabled, onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = AccentBlue,
                        uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF334455)))
                if (alarm.crescendoEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📈", fontSize = 12.sp)
                        Text(alarm.soundName,
                            color = if (alarm.enabled) AccentBlue else TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ── Sleep Summary Tab ──────────────────────────────────────────────────────

@Composable
private fun SleepSummaryTab(s: SleepSchedule, onOpenSleep: () -> Unit) {
    val sleepShort = s.isSleepShort()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // Main sleep card
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF0F3460), Color(0xFF533483))))
                .clickable(onClick = onOpenSleep).padding(20.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("SLEEP SCHEDULE", color = Color(0x80FFFFFF), fontSize = 11.sp,
                            letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape)
                                .background(if (s.enabled) AccentGreen else Color(0xFF334455)))
                            Text(if (s.enabled) "Active" else "Off",
                                color = if (s.enabled) AccentGreen else TextMuted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Column {
                            Text("🌙 Bedtime", color = Color(0x80FFFFFF), fontSize = 12.sp)
                            Text(s.formattedBedtime(), color = Color.White,
                                fontSize = 28.sp, fontWeight = FontWeight.Light)
                        }
                        Column(Modifier.align(Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            val durColor = if (sleepShort) AccentRed else AccentGreen
                            Text(s.sleepDurationLabel(), color = durColor,
                                fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("sleep", color = Color(0x60FFFFFF), fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("☀️ Wake up", color = Color(0x80FFFFFF), fontSize = 12.sp)
                            Text(s.formattedWakeTime(), color = Color.White,
                                fontSize = 28.sp, fontWeight = FontWeight.Light)
                        }
                    }

                    // Short sleep warning
                    if (sleepShort) {
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(AccentRed.copy(0.2f)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⚠️", fontSize = 14.sp)
                            Text("Less than 8 hours — tap to adjust",
                                color = AccentRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Tap to edit →", color = Color(0x55FFFFFF), fontSize = 11.sp)
                }
            }
        }

        // Summary chips row 1
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryChip("🌫️", "Wind Down",
                    if (s.windDownMinutes == 0) "Off" else "${s.windDownMinutes}m",
                    AccentPurple, Modifier.weight(1f))
                SummaryChip("🌅", "Wake Window",
                    if (s.wakeWindowMinutes == 0) "Off" else "${s.wakeWindowMinutes}m",
                    AccentGreen, Modifier.weight(1f))
            }
        }

        // Summary chips row 2
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryChip("🔔", "Bedtime Alert",
                    if (s.bedtimeReminderEnabled) "On" else "Off",
                    AccentOrange, Modifier.weight(1f))
                SummaryChip("📳", "Haptics",
                    if (s.wakeHaptics) "On" else "Off",
                    AccentBlue, Modifier.weight(1f))
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SummaryChip(icon: String, label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(color.copy(0.1f)).padding(14.dp)) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text(label, color = TextMuted, fontSize = 11.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Tasks Tab ─────────────────────────────────────────────────────────────

@Composable
private fun TasksTab(viewModel: AlarmViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Today's Tasks", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, null, tint = AccentBlue)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tasks for today", color = TextMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(tasks) { task ->
                    TaskItem(task, onToggle = { viewModel.toggleTask(task) }, onDelete = { viewModel.deleteTask(task) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(onDismiss = { showAddDialog = false }) { title, time ->
            viewModel.saveTask(Task(title = title, date = java.time.LocalDate.now().toString(), time = time))
            showAddDialog = false
        }
    }
}

@Composable
private fun TaskItem(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111122))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.completed,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = AccentBlue, uncheckedColor = TextMuted)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                task.title,
                color = if (task.completed) TextMuted else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(task.time, color = TextMuted, fontSize = 12.sp)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, null, tint = AccentRed.copy(0.7f), modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var hour by remember { mutableIntStateOf(12) }
    var minute by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111122),
        title = { Text("New Task", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What needs to be done?", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x0AFFFFFF),
                        unfocusedContainerColor = Color(0x0AFFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Time:", color = TextMuted)
                    // Simplified time selection
                    NumberDrumSmall(hour, 0, 23) { hour = it }
                    Text(":", color = Color.White)
                    NumberDrumSmall(minute, 0, 59) { minute = it }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank()) onSave(title, "%02d:%02d".format(hour, minute)) }) {
                Text("Save", color = AccentBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}

@Composable
private fun NumberDrumSmall(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "%02d".format(value),
            color = Color.White,
            modifier = Modifier
                .clickable { onChange(if (value >= max) min else value + 1) }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(Color(0x1AFFFFFF), RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun VoiceSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onPreview: () -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Text("%.1f".format(value), color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onPreview,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = AccentBlue,
                activeTrackColor = AccentBlue,
                inactiveTrackColor = Color(0xFF334455)
            )
        )
    }
}

@Composable
private fun NameSetupDialog(onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { },
        containerColor = Color(0xFF111122),
        title = { Text("Welcome to AuraWake", color = Color.White) },
        text = {
            Column {
                Text("What should I call you?", color = TextMuted)
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Enter your name", color = Color(0x40FFFFFF)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x0AFFFFFF),
                        unfocusedContainerColor = Color(0x0AFFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name) }) {
                Text("Let's Start", color = AccentBlue)
            }
        }
    )
}

// ── About Tab ──────────────────────────────────────────────────────────────

@Composable
private fun AboutTab(
    sleep: SleepSchedule,
    onEditName: (String) -> Unit,
    onUpdateVoice: (Float, Float) -> Unit,
    onPreview: () -> Unit
) {
    var showEditName by remember { mutableStateOf(false) }
    val userName = sleep.userName

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF111122))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("AuraWake", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Crescendo Alarm", color = AccentBlue, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0x15FFFFFF))
                Spacer(Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Good morning,", color = TextMuted, fontSize = 12.sp)
                        Text(userName.ifBlank { "User" }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showEditName = true }) {
                        Text("✏️", fontSize = 14.sp)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Text("Developed by", color = TextMuted, fontSize = 12.sp)
                Text("glsaikiran", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Version 1.0.0", color = TextMuted, fontSize = 12.sp)
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF111122))
                    .padding(20.dp)
            ) {
                Text("Voice Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Adjust how your morning briefing sounds", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(20.dp))

                VoiceSlider("Pitch", sleep.ttsPitch, 0.5f..1.5f, 
                    onChange = { onUpdateVoice(it, sleep.ttsRate) },
                    onPreview = onPreview
                )
                Spacer(Modifier.height(16.dp))
                VoiceSlider("Speed", sleep.ttsRate, 0.5f..1.5f,
                    onChange = { onUpdateVoice(sleep.ttsPitch, it) },
                    onPreview = onPreview
                )
                
                Spacer(Modifier.height(12.dp))
                Text("Tip: Lower pitch sounds more masculine, higher pitch sounds more feminine.", 
                    color = Color(0x8063B3ED), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF111122))
                    .padding(20.dp)
            ) {
                Text("Licensing & Terms", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text(
                    "This software is open for use and modification. However, proper credit must " +
                    "be given to the original developer, glsaikiran, in all distributions or " +
                    "substantial portions of the software.\n\n" +
                    "Copyright (c) 2026 glsaikiran\n\n" +
                    "Permission is hereby granted, free of charge, to any person obtaining a copy " +
                    "of this software and associated documentation files to use, copy, modify, and " +
                    "merge, subject to the condition that credit is clearly attributed to glsaikiran.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text("Made with ❤️ for better mornings", color = TextMuted, fontSize = 11.sp)
            }
        }
    }

    if (showEditName) {
        var newName by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showEditName = false },
            containerColor = Color(0xFF111122),
            title = { Text("Edit Name", color = Color.White) },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x0AFFFFFF),
                        unfocusedContainerColor = Color(0x0AFFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { onEditName(newName); showEditName = false }) {
                    Text("Save", color = AccentBlue)
                }
            }
        )
    }
}
