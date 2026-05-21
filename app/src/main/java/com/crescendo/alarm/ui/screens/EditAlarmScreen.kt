package com.crescendo.alarm.ui.screens

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crescendo.alarm.data.Alarm
import com.crescendo.alarm.data.WakeSoundItem
import com.crescendo.alarm.viewmodel.AlarmViewModel
import org.json.JSONArray
import org.json.JSONObject

private val BgDark    = Color(0xFF0A0A1A)
private val CardBg    = Color(0xFF111122)
private val AccentBlue = Color(0xFF63B3ED)
private val TextMuted  = Color(0xFF8899AA)
private val DangerRed  = Color(0xFFE24B4A)

private val RAMP_OPTIONS = listOf(0, 15, 30, 45, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmScreen(alarmId: Int, viewModel: AlarmViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val alarms  by viewModel.alarms.collectAsState()
    val existing = remember(alarmId, alarms) { alarms.find { it.id == alarmId } }

    var hour       by remember { mutableStateOf(existing?.hour    ?: 7) }
    var minute     by remember { mutableStateOf(existing?.minute  ?: 0) }
    var label      by remember { mutableStateOf(existing?.label   ?: "New Alarm") }
    var days       by remember { mutableStateOf(existing?.daysAsList() ?: List(7) { it in 1..5 }) }
    var crescendo  by remember { mutableStateOf(existing?.crescendoEnabled ?: true) }
    var rampMin    by remember {
        mutableStateOf(
            when (val old = existing?.rampSeconds ?: 0) {
                in 0..30    -> 0
                in 31..1200 -> 15
                in 1201..2400 -> 30
                in 2401..3600 -> 45
                else -> 60
            }
        )
    }
    var soundsJson by remember { mutableStateOf(existing?.soundsJson ?: "[]") }
    var vibration  by remember { mutableStateOf(existing?.vibration ?: false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTimePicker   by remember { mutableStateOf(false) }
    var editingSoundIndex by remember { mutableStateOf(-1) }

    val sounds = remember(soundsJson) {
        val list = mutableListOf<WakeSoundItem>()
        try {
            val arr = JSONArray(soundsJson)
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
        soundsJson = arr.toString()
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

    Scaffold(containerColor = BgDark) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Text(if (alarmId == 0) "New Alarm" else "Edit Alarm",
                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = {
                    val alarm = Alarm(
                        id = existing?.id ?: 0,
                        label = label, hour = hour, minute = minute,
                        days  = days.joinToString(","),
                        crescendoEnabled = crescendo,
                        rampSeconds = rampMin * 60,
                        soundName = if (sounds.isNotEmpty()) sounds[0].name else "Helix",
                        soundsJson = soundsJson,
                        vibration = vibration,
                        enabled = existing?.enabled ?: true
                    )
                    viewModel.saveAlarm(alarm); onBack()
                }) { Text("Save", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            }

            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(CardBg)
                        .clickable { showTimePicker = true }
                        .padding(24.dp),
                        contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center) {
                            val displayHour = when {
                                hour == 0 -> 12
                                hour > 12 -> hour - 12
                                else -> hour
                            }
                            Text("%02d".format(displayHour), color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
                            Text(":", color = Color(0x55FFFFFF), fontSize = 64.sp,
                                fontWeight = FontWeight.Light, modifier = Modifier.padding(horizontal = 8.dp))
                            Text("%02d".format(minute), color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("AM", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    color = if (hour < 12) AccentBlue else Color(0x44FFFFFF))
                                Spacer(Modifier.height(4.dp))
                                Text("PM", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    color = if (hour >= 12) AccentBlue else Color(0x44FFFFFF))
                            }
                        }
                    }
                }

                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(CardBg).padding(14.dp)) {
                        Text("Label", color = TextMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        TextField(value = label, onValueChange = { label = it },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor        = Color.White,
                                unfocusedTextColor      = Color.White,
                                focusedIndicatorColor   = AccentBlue,
                                unfocusedIndicatorColor = Color.Transparent),
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                }

                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(CardBg).padding(14.dp)) {
                        Text("Repeat", color = TextMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat").forEachIndexed { i, d ->
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                    .background(if (days[i]) AccentBlue else Color(0x1AFFFFFF))
                                    .clickable { days = days.toMutableList().also { it[i] = !it[i] } }
                                    .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center) {
                                    Text(d,
                                        color = if (days[i]) Color.White else TextMuted,
                                        fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                item {
                    Text("ALARM OPTIONS", color = TextMuted, fontSize = 12.sp,
                        letterSpacing = 1.sp, modifier = Modifier.padding(top = 8.dp))
                }

                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(CardBg).padding(14.dp)) {

                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Crescendo Mode", color = Color.White,
                                    fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text("Volume ramps up gradually", color = TextMuted, fontSize = 12.sp)
                            }
                            Switch(checked = crescendo, onCheckedChange = { crescendo = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue))
                        }

                        if (crescendo) {
                            Spacer(Modifier.height(16.dp))
                            Divider(color = Color(0x15FFFFFF))
                            Spacer(Modifier.height(14.dp))

                            Text("Ramp Duration", color = TextMuted, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (rampMin == 0) "Instant (no ramp)" else "$rampMin minutes ramp",
                                color = AccentBlue, fontSize = 12.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RAMP_OPTIONS.forEach { opt ->
                                    val sel = rampMin == opt
                                    Box(Modifier.clip(RoundedCornerShape(20.dp))
                                        .background(if (sel) AccentBlue else Color(0x1AFFFFFF))
                                        .clickable { rampMin = opt }
                                        .padding(horizontal = 14.dp, vertical = 9.dp)) {
                                        Text(
                                            if (opt == 0) "Off" else "${opt}m",
                                            color = if (sel) Color.White else TextMuted,
                                            fontSize = 13.sp,
                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            if (rampMin > 0) {
                                Spacer(Modifier.height(14.dp))
                                Text("Volume ramp preview", color = TextMuted, fontSize = 12.sp)
                                Spacer(Modifier.height(6.dp))
                                CrescendoBarViz()
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("Silent", color = TextMuted, fontSize = 10.sp)
                                    Text("$rampMin min", color = TextMuted, fontSize = 10.sp)
                                    Text("Full volume", color = TextMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(CardBg).padding(14.dp)) {
                        Text("ALARM SOUNDS", color = TextMuted, fontSize = 12.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(12.dp))

                        sounds.forEachIndexed { index, item ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Color(0x0AFFFFFF)).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, color = Color.White, fontSize = 13.sp, maxLines = 1)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${item.duration} min", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                    Icon(Icons.Default.Delete, null, tint = DangerRed.copy(0.7f), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

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
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Sound", fontSize = 12.sp)
                        }

                        if (sounds.isEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Using default system alarm", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(CardBg).padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Vibration", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Switch(checked = vibration, onCheckedChange = { vibration = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue))
                    }
                }

                if (alarmId != 0) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x1FE24B4A), contentColor = DangerRed),
                            shape = RoundedCornerShape(14.dp)) {
                            Text("Delete Alarm", modifier = Modifier.padding(vertical = 4.dp),
                                fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = Color(0xFF111122),
            title = { Text("Set Time", color = Color.White, fontWeight = FontWeight.SemiBold) },
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
                TextButton(onClick = { 
                    hour = state.hour
                    minute = state.minute
                    showTimePicker = false 
                }) {
                    Text("Set", color = AccentBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false },
            containerColor = Color(0xFF111122),
            title = { Text("Delete Alarm", color = Color.White) },
            text  = { Text("This alarm will be permanently deleted.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { existing?.let { viewModel.deleteAlarm(it) }; onBack() }) {
                    Text("Delete", color = DangerRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = AccentBlue) }
            }
        )
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
                            colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue)
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
                }) { Text("OK", color = AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { editingSoundIndex = -1 }) { Text("Cancel", color = TextMuted) }
            }
        )
    }
}

@Composable
private fun CrescendoBarViz() {
    Canvas(Modifier.fillMaxWidth().height(40.dp)) {
        val barCount = 40
        val spacing = size.width / barCount
        val brush = Brush.horizontalGradient(listOf(AccentBlue.copy(0.2f), AccentBlue))
        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            val h = 4.dp.toPx() + (size.height - 4.dp.toPx()) * progress * progress
            drawRect(
                brush = brush,
                topLeft = androidx.compose.ui.geometry.Offset(i * spacing + 2.dp.toPx(), size.height - h),
                size = androidx.compose.ui.geometry.Size(spacing - 4.dp.toPx(), h)
            )
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
