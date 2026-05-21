package com.crescendo.alarm

import android.Manifest
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.crescendo.alarm.service.AlarmService
import com.crescendo.alarm.ui.theme.CrescendoAlarmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class AlarmActivity : ComponentActivity() {
    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.crescendo.alarm.ALARM_DISMISSED") {
                finish()
            }
        }
    }

    private var weatherCondition = mutableStateOf(WeatherCondition.CLEAR)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val filter = IntentFilter("com.crescendo.alarm.ALARM_DISMISSED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dismissReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(dismissReceiver, filter)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val label = intent.getStringExtra("alarm_label") ?: "Alarm"
        
        fetchWeather()

        enableEdgeToEdge()
        setContent {
            CrescendoAlarmTheme {
                AlarmScreen(
                    label = label,
                    weather = weatherCondition.value,
                    onStop = {
                        stopAlarmService()
                        finish()
                    }
                )
            }
        }
    }

    private fun fetchWeather() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val loc = getLastKnownLocation()
                val lat = loc?.latitude ?: 0.0
                val lon = loc?.longitude ?: 0.0
                
                // Open-Meteo API (No key required)
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
                val conn = url.openConnection() as HttpURLConnection
                val data = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(data)
                val weatherCode = json.getJSONObject("current_weather").getInt("weathercode")
                
                // WMO Weather interpretation codes (approximate)
                // 51-67, 80-99 are rain/storm
                val condition = when (weatherCode) {
                    in 51..67, in 80..99 -> WeatherCondition.RAINY
                    in 1..3 -> WeatherCondition.CLOUDY
                    else -> WeatherCondition.CLEAR
                }
                
                withContext(Dispatchers.Main) {
                    weatherCondition.value = condition
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getLastKnownLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dismissReceiver)
    }

    private fun stopAlarmService() {
        startService(Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        })
        startService(Intent(this, com.crescendo.alarm.service.WakeWindowService::class.java).apply {
            action = com.crescendo.alarm.service.WakeWindowService.ACTION_STOP
        })
    }
}

enum class WeatherCondition { CLEAR, RAINY, CLOUDY }

data class AtmosphericTheme(
    val topColor: Color,
    val bottomColor: Color,
    val accentColor: Color,
    val particleColor: Color,
    val condition: WeatherCondition
)

fun getAtmosphericTheme(hour: Int, condition: WeatherCondition): AtmosphericTheme {
    return when (hour) {
        in 5..7 -> AtmosphericTheme( // Dawn
            topColor = Color(0xFFFF9A9E),
            bottomColor = Color(0xFFFAD0C4),
            accentColor = Color(0xFFFFE1E1),
            particleColor = Color.White.copy(alpha = 0.6f),
            condition = condition
        )
        in 8..16 -> AtmosphericTheme( // Day
            topColor = if (condition == WeatherCondition.RAINY) Color(0xFF606c88) else Color(0xFF00B4DB),
            bottomColor = if (condition == WeatherCondition.RAINY) Color(0xFF3f4c6b) else Color(0xFF0083B0),
            accentColor = Color(0xFFE0F7FA),
            particleColor = Color.White.copy(alpha = 0.5f),
            condition = condition
        )
        in 17..18 -> AtmosphericTheme( // Golden Hour
            topColor = Color(0xFFF09819),
            bottomColor = Color(0xFFEDDE5D),
            accentColor = Color(0xFFFFF9C4),
            particleColor = Color.White.copy(alpha = 0.7f),
            condition = condition
        )
        in 19..20 -> AtmosphericTheme( // Dusk
            topColor = Color(0xFF4B6CB7),
            bottomColor = Color(0xFF182848),
            accentColor = Color(0xFFE1F5FE),
            particleColor = Color.White.copy(alpha = 0.4f),
            condition = condition
        )
        else -> AtmosphericTheme( // Night
            topColor = Color(0xFF0F2027),
            bottomColor = Color(0xFF203A43),
            accentColor = Color(0xFFB0BEC5),
            particleColor = Color.White.copy(alpha = 0.3f),
            condition = condition
        )
    }
}

@Composable
fun AlarmScreen(label: String, weather: WeatherCondition, onStop: () -> Unit) {
    var currentTime by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val theme = getAtmosphericTheme(hour, weather)

    val animatedTopColor by animateColorAsState(theme.topColor, animationSpec = tween(2000), label = "top")
    val animatedBottomColor by animateColorAsState(theme.bottomColor, animationSpec = tween(2000), label = "bottom")

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animatedTopColor, animatedBottomColor)))
    ) {
        AtmosphericParticles(theme)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 64.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(theme.accentColor.copy(alpha = 0.6f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )

                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale),
                    tint = Color.White
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = currentTime,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.2f),
                            offset = Offset(4f, 4f),
                            blurRadius = 8f
                        )
                    )
                )
                
                Text(
                    text = label.uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 4.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when(weather) {
                        WeatherCondition.RAINY -> "🌧 RAINY OUTSIDE"
                        WeatherCondition.CLOUDY -> "☁️ CLOUDY SKIES"
                        else -> "☀️ CLEAR WEATHER"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )
            }

            SwipeToStopButton(theme, onStop)
        }
    }
}

@Composable
fun AtmosphericParticles(theme: AtmosphericTheme) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val count = if (theme.condition == WeatherCondition.RAINY) 60 else 20
    val particles = remember(theme.condition) { List(count) { ParticleData(theme.condition) } }

    Box(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val yOffset by infiniteTransition.animateFloat(
                initialValue = particle.startY,
                targetValue = particle.endY,
                animationSpec = infiniteRepeatable(
                    animation = tween(particle.duration, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "y"
            )
            
            val xOffset by infiniteTransition.animateFloat(
                initialValue = particle.startX,
                targetValue = if (theme.condition == WeatherCondition.RAINY) particle.startX + 5f else particle.startX + 30f,
                animationSpec = infiniteRepeatable(
                    animation = tween(particle.duration, easing = LinearEasing),
                    repeatMode = if (theme.condition == WeatherCondition.RAINY) RepeatMode.Restart else RepeatMode.Reverse
                ),
                label = "x"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                if (theme.condition == WeatherCondition.RAINY) {
                    drawLine(
                        color = theme.particleColor.copy(alpha = 0.7f),
                        start = Offset(xOffset * size.width / 100f, yOffset * size.height / 100f),
                        end = Offset((xOffset + 0.5f) * size.width / 100f, (yOffset + 4f) * size.height / 100f),
                        strokeWidth = 3f
                    )
                } else {
                    drawCircle(
                        color = theme.particleColor,
                        radius = particle.size,
                        center = Offset(xOffset * size.width / 100f, yOffset * size.height / 100f)
                    )
                }
            }
        }
    }
}

class ParticleData(condition: WeatherCondition) {
    val startX = (0..100).random().toFloat()
    val startY = if (condition == WeatherCondition.RAINY) (-20..0).random().toFloat() else (0..100).random().toFloat()
    val endY = if (condition == WeatherCondition.RAINY) 110f else startY - 120f
    val size = if (condition == WeatherCondition.RAINY) 2f else (4..12).random().toFloat()
    val duration = if (condition == WeatherCondition.RAINY) (1000..2000).random() else (4000..10000).random()
}

@Composable
fun SwipeToStopButton(theme: AtmosphericTheme, onStop: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val width = 300.dp
    val size = 72.dp
    val swipeLimit = with(LocalDensity.current) { (width - size).toPx() }

    Box(
        modifier = Modifier
            .width(width)
            .height(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "SWIPE TO STOP",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            style = MaterialTheme.typography.labelLarge
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(size)
                .padding(6.dp)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX.value >= swipeLimit * 0.8f) {
                                coroutineScope.launch {
                                    offsetX.animateTo(swipeLimit)
                                    onStop()
                                }
                            } else {
                                coroutineScope.launch {
                                    offsetX.animateTo(0f)
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val newValue = (offsetX.value + dragAmount).coerceIn(0f, swipeLimit)
                                offsetX.snapTo(newValue)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = theme.topColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
