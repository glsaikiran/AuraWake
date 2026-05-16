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
    var tab     by remember { mutableIntStateOf(0) }

    Scaffold(containerColor = BgDark, floatingActionButton = {
        if (tab == 0) FloatingActionButton(onClick = onAddAlarm,
            containerColor = AccentBlue, contentColor = Color.White, shape = CircleShape) {
            Icon(Icons.Default.Add, "Add")
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))

            // Tab bar
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111122)).padding(4.dp)) {
                listOf("⏰  Alarms", "🌙  Sleep", "ℹ️  Info").forEachIndexed { idx, title ->
                    val sel = tab == idx
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (sel) Color(0x2663B3ED) else Color.Transparent)
                        .clickable { tab = idx }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center) {
                        Text(title,
                            color = if (sel) AccentBlue else TextMuted,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            when (tab) {
                0 -> AlarmsTab(alarms, viewModel, onEditAlarm)
                1 -> SleepSummaryTab(sleep, onOpenSleep)
                2 -> AboutTab()
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

@Composable
fun CrescendoBarViz(barCount: Int = 20) {
    Row(Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom) {
        repeat(barCount) { i ->
            val pct = (i + 1f) / barCount
            Box(Modifier.weight(1f).height((6 + pct * 42).dp)
                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                .background(AccentBlue.copy(alpha = 0.2f + pct * 0.6f)))
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

// ── About Tab ──────────────────────────────────────────────────────────────

@Composable
private fun AboutTab() {
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
}
