package com.crescendo.alarm.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crescendo.alarm.data.SleepSchedule
import com.crescendo.alarm.data.WakeSoundItem
import com.crescendo.alarm.viewmodel.AlarmViewModel
import org.json.JSONArray
import org.json.JSONObject
import android.media.RingtoneManager
import android.net.Uri
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.provider.OpenableColumns

private val BgDark       = Color(0xFF0A0A1A)
private val CardBg       = Color(0xFF111122)
private val AccentBlue   = Color(0xFF63B3ED)
private val AccentPurple = Color(0xFF9F7AEA)
private val AccentGreen  = Color(0xFF68D391)
private val AccentOrange = Color(0xFFF6AD55)
private val AccentRed    = Color(0xFFFC8181)
private val TextMuted    = Color(0xFF8899AA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScheduleScreen(viewModel: AlarmViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val saved   by viewModel.sleepSchedule.collectAsState()
    var s       by remember(saved) { mutableStateOf(saved) }

    var showBedPicker  by remember { mutableStateOf(false) }
    var showWakePicker by remember { mutableStateOf(false) }
    var editingSoundIndex by remember { mutableStateOf(-1) }

    // Multi-sound selection logic
    val sounds = remember(s.wakeSoundsJson) {
        val list = mutableListOf<WakeSoundItem>()
        try {
            val arr = JSONArray(s.wakeSoundsJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(WakeSoundItem(o.getString("uri"), o.getString("name"), o.getInt("duration")))
            }
        } catch (e: Exception) {}
        list
    }

    fun updateSounds(newList: List<WakeSoundItem>) {
        val arr = JSONArray()
        newList.forEach {
            val o = JSONObject()
            o.put("uri", it.uri)
            o.put("name", it.name)
            o.put("duration", it.duration)
            arr.put(o)
        }
        s = s.copy(wakeSoundsJson = arr.toString())
    }

    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                val name = ringtone.getTitle(context)
                updateSounds(sounds + WakeSoundItem(uri.toString(), name, 5))
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val name = getFileName(context, uri) ?: "Selected Song"
            updateSounds(sounds + WakeSoundItem(uri.toString(), name, 5))
        }
    }

    val sleepShort = s.isSleepShort()
    val sleepMins  = s.sleepMinutes()
    val sleepH     = sleepMins / 60
    val sleepM     = sleepMins % 60

    Scaffold(containerColor = BgDark) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {

            // ── Top bar ────────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Text("Sleep Schedule", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { viewModel.saveSleepSchedule(s); onBack() }) {
                    Text("Save", color = AccentBlue, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ── Master toggle ──────────────────────────────────────────
                item {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(CardBg).padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Sleep Schedule", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Bedtime, wind-down & gradual wake-up", color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(checked = s.enabled, onCheckedChange = { s = s.copy(enabled = it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue))
                    }
                }

                // ── Bedtime / Wake time pickers ────────────────────────────
                item {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF0F3460), Color(0xFF533483))))
                        .padding(20.dp)) {
                        Column {
                            Text("SCHEDULE", color = Color(0x80FFFFFF), fontSize = 11.sp,
                                letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(14.dp))

                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {

                                // Bedtime tap
                                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                    .clickable { showBedPicker = true }.padding(vertical = 8.dp, horizontal = 2.dp)) {
                                    Text("🌙 Bedtime", color = Color(0x99FFFFFF), fontSize = 10.sp, maxLines = 1)
                                    Spacer(Modifier.height(2.dp))
                                    Text(s.formattedBedtime(), color = Color.White,
                                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Clip)
                                    Text("Tap to edit", color = Color(0x55FFFFFF), fontSize = 9.sp)
                                }

                                // Sleep duration badge
                                Column(Modifier.weight(0.6f).align(Alignment.CenterVertically),
                                    horizontalAlignment = Alignment.CenterHorizontally) {
                                    val durColor = if (sleepShort) AccentRed else AccentGreen
                                    Text(s.sleepDurationLabel(), color = durColor,
                                        fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("sleep", color = Color(0x60FFFFFF), fontSize = 9.sp)
                                }

                                // Wake time tap
                                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                    .clickable { showWakePicker = true }.padding(vertical = 8.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.End) {
                                    Text("☀️ Wake up", color = Color(0x99FFFFFF), fontSize = 10.sp, maxLines = 1)
                                    Spacer(Modifier.height(2.dp))
                                    Text(s.formattedWakeTime(), color = Color.White,
                                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Clip)
                                    Text("Tap to edit", color = Color(0x55FFFFFF), fontSize = 9.sp)
                                }
                            }

                            // ── Sleep warning ──────────────────────────────
                            if (sleepShort) {
                                Spacer(Modifier.height(14.dp))
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(AccentRed.copy(alpha = 0.18f)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("⚠️", fontSize = 16.sp)
                                    Column {
                                        Text("Less than 8 hours of sleep",
                                            color = AccentRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        val need = 480 - sleepMins
                                        val nH = need / 60; val nM = need % 60
                                        val diff = if (nM == 0) "${nH}h earlier" else "${nH}h ${nM}m earlier"
                                        Text("Move bedtime $diff for 8h sleep",
                                            color = AccentRed.copy(0.8f), fontSize = 12.sp)
                                    }
                                }
                            } else {
                                Spacer(Modifier.height(14.dp))
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(AccentGreen.copy(alpha = 0.12f)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("✅", fontSize = 14.sp)
                                    Text("${if (sleepM == 0) "${sleepH}h" else "${sleepH}h ${sleepM}m"} – healthy sleep duration",
                                        color = AccentGreen, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // ── Wind Down ──────────────────────────────────────────────
                item {
                    SleepCard(icon = "🌫️", title = "Wind Down", accent = AccentPurple,
                        subtitle = "Silence notifications before bedtime") {

                        Text("How long before bedtime", color = TextMuted, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        ChipRow(
                            options = listOf(0, 15, 30, 45, 60, 90),
                            selected = s.windDownMinutes,
                            label = { if (it == 0) "Off" else "${it}m" },
                            accent = AccentPurple
                        ) { s = s.copy(windDownMinutes = it) }

                        AnimatedVisibility(s.windDownMinutes > 0) {
                            Column {
                                Spacer(Modifier.height(10.dp))
                                InfoRow("🌫️ Starts at", s.windDownStartTime())
                                Spacer(Modifier.height(10.dp))
                                Text("Apps silenced:", color = TextMuted, fontSize = 12.sp)
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("📱 WhatsApp","📸 Instagram","▶️ YouTube","📧 Gmail")
                                        .forEach { AppChip(it, AccentPurple) }
                                    Spacer(Modifier.width(8.dp))
                                }
                            }
                        }
                    }
                }

                // ── Bedtime Reminder ───────────────────────────────────────
                item {
                    SleepCard(icon = "🔔", title = "Bedtime Reminder", accent = AccentOrange,
                        subtitle = "Voice: \"It's Bedtime, Time to sleep\"") {

                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Enable voice reminder", color = Color.White, fontSize = 14.sp)
                            Switch(checked = s.bedtimeReminderEnabled,
                                onCheckedChange = { s = s.copy(bedtimeReminderEnabled = it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = AccentOrange))
                        }

                        AnimatedVisibility(s.bedtimeReminderEnabled) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                Text("Remind again after", color = TextMuted, fontSize = 13.sp)
                                Spacer(Modifier.height(8.dp))
                                ChipRow(
                                    options = listOf(5, 10, 15, 20, 30),
                                    selected = s.bedtimeSnoozeMinutes,
                                    label = { "${it}m" },
                                    accent = AccentOrange
                                ) { s = s.copy(bedtimeSnoozeMinutes = it) }
                                Spacer(Modifier.height(12.dp))
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(AccentOrange.copy(0.12f)).padding(12.dp)) {
                                    Text(
                                        "🎙️ At ${s.formattedBedtime()} your phone will say:\n" +
                                        "\"It's Bedtime. Time to sleep.\"\n" +
                                        "If ignored, it will repeat every ${s.bedtimeSnoozeMinutes} minutes.",
                                        color = AccentOrange.copy(0.9f), fontSize = 12.sp, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }

                // ── Wake Up Window ─────────────────────────────────────────
                item {
                    SleepCard(icon = "🌅", title = "Wake Up Window", accent = AccentGreen,
                        subtitle = "Gradual alarm starts before your wake time") {

                        Text("Window duration before wake time", color = TextMuted, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        ChipRow(
                            options = listOf(0, 15, 30, 45, 60),
                            selected = s.wakeWindowMinutes,
                            label = { if (it == 0) "Off" else "${it}m" },
                            accent = AccentGreen
                        ) { s = s.copy(wakeWindowMinutes = it) }

                        AnimatedVisibility(s.wakeWindowMinutes > 0) {
                            Column {
                                Spacer(Modifier.height(14.dp))

                                // Timeline
                                WakeWindowTimeline(s)

                                Spacer(Modifier.height(16.dp))

                                // Volume slider
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Max volume", color = TextMuted, fontSize = 13.sp)
                                    Text("${s.wakeVolume}%", color = AccentGreen,
                                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Slider(
                                    value = s.wakeVolume.toFloat(),
                                    onValueChange = { s = s.copy(wakeVolume = it.toInt()) },
                                    valueRange = 10f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentGreen,
                                        activeTrackColor = AccentGreen,
                                        inactiveTrackColor = Color(0xFF334455))
                                )

                                // Haptics
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("Haptics during window", color = Color.White, fontSize = 14.sp)
                                    Switch(checked = s.wakeHaptics,
                                        onCheckedChange = { s = s.copy(wakeHaptics = it) },
                                        colors = SwitchDefaults.colors(checkedTrackColor = AccentGreen))
                                }

                                // Sound
                                Spacer(Modifier.height(16.dp))
                                Text("Wake sounds sequence", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Add multiple sounds for different stages", color = TextMuted, fontSize = 12.sp)
                                Spacer(Modifier.height(8.dp))

                                sounds.forEachIndexed { index, item ->
                                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x0AFFFFFF)).padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.MusicNote, null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(item.name, color = Color.White, fontSize = 13.sp, maxLines = 1)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("${item.duration} min", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Tap to change", color = TextMuted, fontSize = 10.sp,
                                                    modifier = Modifier.clickable {
                                                        editingSoundIndex = index
                                                    })
                                            }
                                        }
                                        IconButton(onClick = {
                                            val newList = sounds.filterIndexed { i, _ -> i != index }
                                            updateSounds(newList)
                                        }) {
                                            Icon(Icons.Default.Delete, null, tint = AccentRed.copy(0.7f), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }

                                Row(Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
                                            }
                                            ringtonePicker.launch(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen)
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Sound", fontSize = 12.sp)
                                    }
                                }

                                if (sounds.isEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("Using default: ${s.wakeSound}", color = AccentGreen.copy(0.7f), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    // ── Time Picker dialogs ────────────────────────────────────────────────
    if (showBedPicker) {
        SleepTimePicker("🌙 Bedtime", s.bedHour, s.bedMinute,
            onConfirm = { h, m -> s = s.copy(bedHour = h, bedMinute = m); showBedPicker = false },
            onDismiss = { showBedPicker = false })
    }
    if (showWakePicker) {
        SleepTimePicker("☀️ Wake Up", s.wakeHour, s.wakeMinute,
            onConfirm = { h, m -> s = s.copy(wakeHour = h, wakeMinute = m); showWakePicker = false },
            onDismiss = { showWakePicker = false })
    }

    if (editingSoundIndex != -1) {
        val sound = sounds[editingSoundIndex]
        var tempDuration by remember { mutableStateOf(sound.duration) }
        
        AlertDialog(
            onDismissRequest = { editingSoundIndex = -1 },
            containerColor = CardBg,
            title = { Text("Set Duration", color = Color.White) },
            text = {
                Column {
                    Text("Duration for ${sound.name}", color = TextMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Slider(
                            value = tempDuration.toFloat(),
                            onValueChange = { tempDuration = it.toInt() },
                            valueRange = 1f..60f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = AccentGreen,
                                activeTrackColor = AccentGreen
                            )
                        )
                        Text("${tempDuration}m", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newList = sounds.toMutableList()
                    newList[editingSoundIndex] = sound.copy(duration = tempDuration)
                    updateSounds(newList)
                    editingSoundIndex = -1
                }) {
                    Text("OK", color = AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSoundIndex = -1 }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

// ── Reusable components ────────────────────────────────────────────────────

@Composable
private fun SleepCard(icon: String, title: String, subtitle: String,
                      accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(CardBg)
        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
        .padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(accent.copy(0.15f)),
                contentAlignment = Alignment.Center) { Text(icon, fontSize = 18.sp) }
            Column {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextMuted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Divider(color = Color(0x15FFFFFF))
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ChipRow(options: List<Int>, selected: Int, label: (Int) -> String,
                    accent: Color, onSelect: (Int) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            val sel = opt == selected
            Box(Modifier.clip(RoundedCornerShape(12.dp))
                .background(if (sel) accent else Color(0x1AFFFFFF))
                .clickable { onSelect(opt) }
                .padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(label(opt),
                    color = if (sel) Color.White else TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun AppChip(label: String, accent: Color) {
    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(accent.copy(0.12f))
        .padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMuted, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun WakeWindowTimeline(s: SleepSchedule) {
    val wMin = s.wakeWindowMinutes
    // Build 4 milestone steps
    val steps = listOf(
        Triple(s.wakeWindowStartTime(),  "🔇", "Very soft — almost silent"),
        Triple(s.wakeWindowMidLabel(1),  "🔈", "Slowly increasing volume"),
        Triple(s.wakeWindowMidLabel(2),  "🔉", "Noticeably louder — pulling from deep sleep"),
        Triple(s.formattedWakeTime(),    "🔊", "Full intensity — target wake time")
    )

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(AccentGreen.copy(0.07f)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)) {

        Text("Wake window: ${wMin}m gradual ramp",
            color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        steps.forEachIndexed { i, (time, vol, desc) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                // Dot + connector line
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val alpha = 0.25f + i * 0.25f
                    Box(Modifier.size(12.dp).clip(CircleShape).background(AccentGreen.copy(alpha)))
                    if (i < steps.lastIndex)
                        Box(Modifier.width(2.dp).height(28.dp).background(Color(0x15FFFFFF)))
                }

                Column(Modifier.weight(1f).padding(bottom = if (i < steps.lastIndex) 4.dp else 0.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(time, color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(vol, fontSize = 13.sp)
                    }
                    Text(desc, color = TextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) result = result?.substring(cut + 1)
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimePicker(title: String, hour: Int, minute: Int,
                             onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111122),
        title = { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold) },
        text = {
            TimePicker(state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Color(0xFF1A1A2E),
                    clockDialSelectedContentColor = Color.White,
                    clockDialUnselectedContentColor = TextMuted,
                    selectorColor = AccentBlue,
                    containerColor = Color(0xFF111122),
                    timeSelectorSelectedContainerColor = AccentBlue,
                    timeSelectorUnselectedContainerColor = Color(0x1AFFFFFF),
                    timeSelectorSelectedContentColor = Color.White,
                    timeSelectorUnselectedContentColor = TextMuted))
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("Set", color = AccentBlue, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}
