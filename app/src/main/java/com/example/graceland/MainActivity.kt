package com.example.graceland

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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

    // Exit screen: shown when Back is pressed (free version only)
    var showExit by remember { mutableStateOf(false) }

    // "Hold to peek": while the eye button is held, all text/controls fade out
    var peeking by remember { mutableStateOf(false) }
    val uiAlpha by animateFloatAsState(if (peeking) 0f else 1f, tween(150), label = "ui")
    BackHandler(enabled = !isPro && !showExit) { showExit = true }
    BackHandler(enabled = showExit) { activity.finish() }   // Back again = leave now
    LaunchedEffect(isPro) { if (isPro) showExit = false }   // bought Pro: close the exit screen

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
        TimeBackground(hour, showScrim = !peeking)

        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clocks (one compact line)
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, start = 8.dp, end = 8.dp)
                    .alpha(uiAlpha)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                ClockBar(gracelandTime, localTime, resetKey = useKm)
            }

            // Hold-to-peek button
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                PeekButton(onPeek = { peeking = it })
            }

            // Main area
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp).alpha(uiAlpha),
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).alpha(uiAlpha),
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
                fontSize = 12.sp,
                modifier = Modifier.alpha(uiAlpha)
            )

            // Upgrade + ad (hidden for Pro users)
            if (!isPro) {
                OutlinedButton(
                    onClick = { billing.launchUpgrade(activity) },
                    border = BorderStroke(1.5.dp, Gold1),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold1),
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp).alpha(uiAlpha)
                ) {
                    Text("★ Upgrade to Pro – remove ads")
                }
                AdBanner(modifier = Modifier.fillMaxWidth())
            }
        }

        if (showExit) {
            ExitOverlay(
                backgroundRes = backgroundFor(hour),
                lifecycleOwner = activity as LifecycleOwner,
                onUpgrade = { billing.launchUpgrade(activity) },
                onStay = { showExit = false },
                onExit = { activity.finish() }
            )
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

/** Both clocks on one line. Text shrinks automatically if the screen is narrow. */
@Composable
fun ClockBar(elvis: String, local: String, resetKey: Boolean) {
    var size by remember(resetKey) { mutableStateOf(12f) }
    val label = SpanStyle(color = Gold1, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
    val value = SpanStyle(color = Color.White)
    val text = buildAnnotatedString {
        withStyle(label) { append("Elvis Time: ") }
        withStyle(value) { append(elvis) }
        append("   ")
        withStyle(label) { append("Local Time: ") }
        withStyle(value) { append(local) }
    }
    Text(
        text = text,
        fontSize = size.sp,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { if (it.hasVisualOverflow && size > 8f) size -= 0.5f }
    )
}

/** Small eye button. All text hides for as long as a finger is held on it. */
@Composable
fun PeekButton(onPeek: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPeek(true)
                        try { tryAwaitRelease() } finally { onPeek(false) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val eye = Path().apply {
                moveTo(0f, h / 2)
                quadraticBezierTo(w / 2, -h * 0.35f, w, h / 2)
                quadraticBezierTo(w / 2, h * 1.35f, 0f, h / 2)
                close()
            }
            drawPath(eye, Color.White, style = Stroke(width = 2.dp.toPx()))
            drawCircle(Color.White, radius = h * 0.17f, center = center)
        }
    }
}

fun backgroundFor(hour: Int): Int = when (hour) {
    in 5..10 -> R.drawable.morning1
    in 11..15 -> R.drawable.day1
    in 16..19 -> R.drawable.day2
    in 20..23 -> R.drawable.night1
    else -> R.drawable.night2
}

/**
 * Photo background chosen by the phone's local hour:
 *  5-10 morning1 | 11-15 day1 | 16-19 day2 | 20-23 night1 | 0-4 night2
 */
@Composable
fun TimeBackground(hour: Int, showScrim: Boolean = true) {
    val res = backgroundFor(hour)
    val scrimAlpha by animateFloatAsState(if (showScrim) 1f else 0f, tween(150), label = "scrim")
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
            Modifier.fillMaxSize().alpha(scrimAlpha).background(
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

/**
 * Shown when the user presses Back. Counts down from 5 on an "upgrade" button,
 * then closes the app. The countdown pauses while another screen (like the
 * Google Play purchase dialog) is on top, so it can't close the app mid-purchase.
 */
@Composable
fun ExitOverlay(
    backgroundRes: Int,
    lifecycleOwner: LifecycleOwner,
    onUpgrade: () -> Unit,
    onStay: () -> Unit,
    onExit: () -> Unit
) {
    var seconds by remember { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (seconds > 0) {
                delay(1000)
                seconds--
            }
            onExit()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // swallow taps so nothing underneath can be pressed
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))

        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Leaving already?",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )
            Text(
                "Go Pro and get rid of the ads.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            Button(
                onClick = onUpgrade,
                shape = RoundedCornerShape(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold2, contentColor = Ink),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.fillMaxWidth().height(130.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "★ GET THE FULL VERSION ★",
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        "$seconds",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            TextButton(onClick = onStay, modifier = Modifier.padding(top = 16.dp)) {
                Text("Stay in the app", color = Color.White, fontSize = 16.sp)
            }

            // TODO: add a "Rate us" link to the Play Store listing here later
        }
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
