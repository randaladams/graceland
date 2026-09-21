package com.example.graceland

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

// Graceland, 3764 Elvis Presley Blvd, Memphis, TN
private const val GRACELAND_LAT = 35.0477
private const val GRACELAND_LON = -90.0261
private const val METERS_PER_MILE = 1609.344

// Google's TEST banner ad unit. Replace with your own before release.
private const val BANNER_AD_UNIT = "ca-app-pub-3940256099942544/6300978111"

// Palette: Vegas-era gold, midnight blue, ruby
private val Gold1 = Color(0xFFFFE27A)
private val Gold2 = Color(0xFFD4A017)
private val Gold3 = Color(0xFF8A6508)
private val NightTop = Color(0xFF0B1030)
private val NightMid = Color(0xFF3A0B2E)
private val Ruby = Color(0xFFC2185B)
private val Ground = Color(0xFF0A0616)
private val Ink = Color(0xFF2A0620)

class MainActivity : ComponentActivity() {
    private lateinit var billing: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        billing = BillingManager(this)
        setContent {
            MaterialTheme {
                GracelandScreen(billing, this)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun GracelandScreen(billing: BillingManager, activity: Activity) {
    val context = LocalContext.current
    val isPro by billing.isPro.collectAsState()

    var sliderPos by remember { mutableFloatStateOf(0f) } // 0 = miles, 1 = km
    val useKm = sliderPos >= 0.5f
    var meters by remember { mutableStateOf<Double?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Clock: ticks every second
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }
    val gracelandTime = formatTime(nowMs, TimeZone.getTimeZone("America/Chicago"), useKm)
    val localTime = formatTime(nowMs, TimeZone.getDefault(), useKm)
    val hour = Calendar.getInstance().apply { timeInMillis = nowMs }.get(Calendar.HOUR_OF_DAY)

    fun fetchDistance() {
        loading = true
        message = null
        fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { loc ->
                loading = false
                if (loc == null) {
                    message = "Couldn't get your location. Is location turned on?"
                } else {
                    val out = FloatArray(1)
                    Location.distanceBetween(
                        loc.latitude, loc.longitude, GRACELAND_LAT, GRACELAND_LON, out
                    )
                    meters = out[0].toDouble()
                }
            }
            .addOnFailureListener {
                loading = false
                message = "Couldn't get your location."
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) fetchDistance()
        else message = "Location permission is needed to measure the distance."
    }

    fun onButtonPressed() {
        val granted = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (granted) fetchDistance()
        else permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TimeBackground(hour)

        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clocks
            Column(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                ClockLine("Elvis Time:", gracelandTime)
                Box(Modifier.height(4.dp))
                ClockLine("Local Time:", localTime)
            }

            // Main area
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ElvisButton(loading = loading, onClick = { onButtonPressed() })

                val m = meters
                if (message != null || m != null) {
                    Column(
                        modifier = Modifier
                            .padding(top = 28.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (message != null) {
                            Text(message!!, color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center)
                        } else if (m != null) {
                            val value = if (useKm) m / 1000.0 else m / METERS_PER_MILE
                            val unit = if (useKm) "kilometers" else "miles"
                            Text("Graceland, Elvis's home, is", color = Color.White, fontSize = 18.sp)
                            Text(
                                "%,.0f".format(value),
                                color = Gold1,
                                fontSize = 60.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Serif
                            )
                            Text("$unit away", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Unit slider
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Imperial", color = Color.White, fontWeight = if (!useKm) FontWeight.Bold else FontWeight.Normal)
                Slider(
                    value = sliderPos,
                    onValueChange = { sliderPos = it },
                    onValueChangeFinished = { sliderPos = if (sliderPos >= 0.5f) 1f else 0f },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Gold1,
                        activeTrackColor = Gold2,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )
                Text("Metric", color = Color.White, fontWeight = if (useKm) FontWeight.Bold else FontWeight.Normal)
            }

            Text(
                if (useKm) "Kilometers · 24-hour · DD/MM/YYYY" else "Miles · 12-hour · MM/DD/YYYY",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )

            // Upgrade + ad (hidden for Pro users)
            if (!isPro) {
                OutlinedButton(
                    onClick = { billing.launchUpgrade(activity) },
                    border = BorderStroke(1.5.dp, Gold1),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold1),
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                ) {
                    Text("★ Upgrade to Pro – remove ads")
                }
                AdBanner(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Gold jumpsuit-style button with rhinestone trim and a gentle pulse. */
@Composable
fun ElvisButton(loading: Boolean, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    val shape = RoundedCornerShape(36.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .scale(if (loading) 1f else scale)
            .shadow(16.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Gold1, Gold2, Gold3)))
            .clickable(enabled = !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Rhinestone trim
        Canvas(Modifier.fillMaxSize()) {
            val inset = 10.dp.toPx()
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        inset, inset, size.width - inset, size.height - inset,
                        CornerRadius(28.dp.toPx())
                    )
                )
            }
            val measure = PathMeasure()
            measure.setPath(path, false)
            val step = 15.dp.toPx()
            var d = 0f
            var i = 0
            while (d < measure.length) {
                val p = measure.getPosition(d)
                val color = if (i % 2 == 0) Color.White else Ruby
                drawCircle(color, radius = 3.4.dp.toPx(), center = p)
                d += step
                i++
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) {
                Text(
                    "LOCATING…",
                    color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic
                )
            } else {
                Text(
                    "★ HOW FAR AWAY IS ★",
                    color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif, letterSpacing = 2.sp
                )
                Text(
                    "GRACELAND?",
                    color = Ink, fontSize = 42.sp, fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic
                )
                Text(
                    "♪ Tap to find out ♪",
                    color = Ink, fontSize = 14.sp, fontFamily = FontFamily.Serif
                )
            }
        }
    }
}

/** Formats a date and time. Imperial = MM/DD/YYYY 12-hour, Metric = DD/MM/YYYY 24-hour. */
fun formatTime(ms: Long, zone: TimeZone, metric: Boolean): String {
    val pattern = if (metric) "dd/MM/yyyy HH:mm" else "MM/dd/yyyy h:mm a"
    return SimpleDateFormat(pattern, Locale.US).apply { timeZone = zone }.format(Date(ms))
}

@Composable
fun ClockLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = Gold1,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 13.sp,
            modifier = Modifier.width(96.dp)
        )
        Text(value, color = Color.White, fontSize = 14.sp)
    }
}

/**
 * Photo background chosen by the phone's local hour:
 *  5-10 morning1 | 11-15 day1 | 16-19 day2 | 20-23 night1 | 0-4 night2
 */
@Composable
fun TimeBackground(hour: Int) {
    val res = when (hour) {
        in 5..10 -> R.drawable.morning1
        in 11..15 -> R.drawable.day1
        in 16..19 -> R.drawable.day2
        in 20..23 -> R.drawable.night1
        else -> R.drawable.night2
    }
    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = res, animationSpec = tween(1200), label = "bg") { r ->
            Image(
                painter = painterResource(r),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        // Dark gradient so text stays readable over any photo
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.10f),
                        Color.Black.copy(alpha = 0.30f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                )
            )
        )
    }
}

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BANNER_AD_UNIT
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
