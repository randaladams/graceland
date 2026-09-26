package com.adamselite.gracelandmiles

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.DisposableEffect
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
data class ElvisPlace(val label: String, val lat: Double, val lon: Double, val isFree: Boolean)

val ELVIS_PLACES = listOf(
    ElvisPlace("Graceland", 35.045944, -90.022944, isFree = true),
    ElvisPlace("Sun Studio", 35.139247, -90.037678, isFree = false),
    ElvisPlace("His Birthplace, Tupelo", 34.259971, -88.679963, isFree = false)
)
private const val METERS_PER_MILE = 1609.344

// Google's TEST banner ad unit. Replace with your own before release.
private const val BANNER_AD_UNIT = "ca-app-pub-3940256099942544/6300978111"

// Google's TEST full-screen (interstitial) ad unit. Replace with your own before release.
private const val INTERSTITIAL_AD_UNIT = "ca-app-pub-3940256099942544/1033173712"
private const val AD_EVERY_N_LOOKUPS = 4
private const val MIN_MS_BETWEEN_ADS = 60_000L

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
            // Cap the phone's "font size" setting so big text can't break the layout
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, minOf(density.fontScale, 1.1f))
            ) {
                MaterialTheme {
                    GracelandScreen(billing, this, isDebugBuild = BuildConfig.DEBUG)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun GracelandScreen(billing: BillingManager, activity: Activity, isDebugBuild: Boolean = false) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        billing.connect()
    }
    val realIsPro by billing.isPro.collectAsState()

    // Debug-only override so a developer can flip Pro on/off without a real purchase.
    var debugProOverride by remember { mutableStateOf<Boolean?>(null) }
    val isPro = if (isDebugBuild) (debugProOverride ?: realIsPro) else realIsPro

    val prefs = remember { context.getSharedPreferences("graceland", Context.MODE_PRIVATE) }
    var sliderPos by remember { mutableFloatStateOf(if (prefs.getBoolean("use_km", false)) 1f else 0f) } // 0 = miles, 1 = km
    val useKm = sliderPos >= 0.5f
    var meters by remember { mutableStateOf<Double?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    // Full-screen ad: every 4th lookup (free version only), shown between the tap and the result
    // Full-screen ad: every 4th lookup (free version only), shown between the tap and the result
    val prefs = remember { context.getSharedPreferences("graceland", Context.MODE_PRIVATE) }
    var interstitial by remember { mutableStateOf<InterstitialAd?>(null) }
    var adActive by remember { mutableStateOf(false) }
    var pendingMeters by remember { mutableStateOf<Double?>(null) }
    var lastAdMs by remember { mutableLongStateOf(0L) }

    fun loadInterstitial() {
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null }
            }
        )
    }

    fun maybeShowInterstitial() {
        val taps = prefs.getInt("taps", 0) + 1
        prefs.edit().putInt("taps", taps).apply()
        val ad = interstitial
        val now = System.currentTimeMillis()
        if (!isPro && taps % AD_EVERY_N_LOOKUPS == 0 && ad != null && now - lastAdMs > MIN_MS_BETWEEN_ADS) {
            lastAdMs = now
            adActive = true
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitial = null
                    adActive = false
                    loadInterstitial()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitial = null
                    adActive = false
                    loadInterstitial()
                }
            }
            ad.show(activity)
        }
    }

    LaunchedEffect(isPro) { if (!isPro) loadInterstitial() }

    // Reveal the result only when we have it AND any ad has been closed
    LaunchedEffect(pendingMeters, adActive) {
        val m = pendingMeters
        if (m != null && !adActive) {
            meters = m
            pendingMeters = null
            loading = false
        }
    }

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
    val fadeUi = peeking
    val uiAlpha by animateFloatAsState(if (fadeUi) 0f else 1f, tween(150), label = "ui")
    BackHandler(enabled = !isPro && !showExit) { showExit = true }
    BackHandler(enabled = showExit) { }   // ignore Back while the exit screen counts down
    LaunchedEffect(isPro) { if (isPro) showExit = false }   // bought Pro: close the exit screen

    // Which Elvis place we're measuring to. Free users are locked to Graceland.
    var selectedPlace by remember { mutableStateOf(ELVIS_PLACES[0]) }
    LaunchedEffect(isPro) { if (!isPro) selectedPlace = ELVIS_PLACES[0] }

    fun fetchDistance() {
        loading = true
        message = null
        maybeShowInterstitial()
        fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc == null) {
                    loading = false
                    message = "Couldn't get your location. Is location turned on?"
                } else {
                    val out = FloatArray(1)
                    Location.distanceBetween(
                        loc.latitude, loc.longitude, selectedPlace.lat, selectedPlace.lon, out
                    )
                    pendingMeters = out[0].toDouble()
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

    fun resetResult() {
        meters = null
        message = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TimeBackground(hour, showScrim = !fadeUi)

        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clocks (two centered lines)
            ClockBar(
                elvis = gracelandTime,
                local = localTime,
                modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp).alpha(uiAlpha)
            )

            // Hold-to-peek button (+ a debug-only Pro switch, far left)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDebugBuild) {
                    TextButton(onClick = { debugProOverride = !isPro }) {
                        Text(
                            if (isPro) "DEBUG: Pro ON" else "DEBUG: Pro OFF",
                            color = if (isPro) Gold1 else Color.White,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Box(Modifier)
                }
                PeekButton(onPeek = { peeking = it })
            }

            // Place picker (Pro feature): choose which Elvis site to measure to
            if (isPro) {
                PlacePicker(
                    selected = selectedPlace,
                    onSelect = { selectedPlace = it; meters = null; message = null },
                    modifier = Modifier.padding(top = 4.dp).alpha(uiAlpha)
                )
            }

            // Main area
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .alpha(uiAlpha),
                contentAlignment = Alignment.Center
            ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val m = meters
                when {
                    m == null && message == null -> {
                        ElvisButton(place = selectedPlace, loading = loading, onClick = { onButtonPressed() })
                    }
                    message != null -> {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { resetResult() }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(message!!, color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center)
                            Text(
                                "Tap to try again",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                m != null -> {
                    // Below ~0.1 mi / 160 m, whole miles/km round to "0" and read as broken.
                    // Drop to feet or meters instead so a nearby result is still meaningful.
                    val useSmallUnit = m < 160.0
                    val value: Double
                    val unit: String
                    if (useSmallUnit) {
                        value = if (useKm) m else m * 3.28084 // meters, or feet
                        unit = if (useKm) "meters" else "feet"
                    } else {
                        value = if (useKm) m / 1000.0 else m / METERS_PER_MILE
                        unit = if (useKm) "kilometers" else "miles"
                    }
                    ResultWithCompass(
                        place = selectedPlace,
                        valueText = "%,.0f".format(value),
                        unit = unit,
                        onTap = { resetResult() }
                    )
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
                    onValueChangeFinished = {
                        sliderPos = if (sliderPos >= 0.5f) 1f else 0f
                        prefs.edit().putBoolean("use_km", sliderPos >= 0.5f).apply()
                    },
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
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp).alpha(uiAlpha)
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
fun ElvisButton(place: ElvisPlace, loading: Boolean, onClick: () -> Unit) {
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
            .height(140.dp)
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
                    place.label.uppercase() + "?",
                    color = Ink,
                    fontSize = if (place.label.length > 10) 26.sp else 34.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center
                )
                Text(
                    "♪ Tap to find out ♪",
                    color = Ink, fontSize = 14.sp, fontFamily = FontFamily.Serif
                )
            }
        }
    }
}

/** Horizontal row of chips letting a Pro user pick which Elvis site to measure to. */
@Composable
fun PlacePicker(selected: ElvisPlace, onSelect: (ElvisPlace) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        items(ELVIS_PLACES) { place ->
            val active = place == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) Gold2 else Color.Black.copy(alpha = 0.45f))
                    .clickable { onSelect(place) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    place.label,
                    color = if (active) Ink else Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

/** Bearing in degrees (0 = north, clockwise) from one lat/lon to another. */
fun bearingTo(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Float {
    val lat1 = Math.toRadians(fromLat)
    val lat2 = Math.toRadians(toLat)
    val dLon = Math.toRadians(toLon - fromLon)
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    val deg = Math.toDegrees(atan2(y, x))
    return ((deg + 360) % 360).toFloat()
}

/**
 * Shown once a distance result is ready: the compass (using the phone's location and
 * orientation sensors to point at [place]) together with the distance readout.
 * Tapping it goes back to the button so the user can measure again.
 */
@SuppressLint("MissingPermission")
@Composable
fun ResultWithCompass(place: ElvisPlace, valueText: String, unit: String, onTap: () -> Unit) {
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(0f) }      // which way the phone is facing (0 = north)
    var bearing by remember { mutableStateOf<Float?>(null) } // which way the destination is
    var hasSensor by remember { mutableStateOf(true) }

    // Orientation sensor
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationSensor == null) hasSensor = false
        val listener = object : SensorEventListener {
            val rotMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                SensorManager.getOrientation(rotMatrix, orientation)
                azimuth = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + 360) % 360)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (rotationSensor != null) {
            sm.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sm.unregisterListener(listener) }
    }

    // One-shot location fix — permission is already granted by the time a result exists
    LaunchedEffect(place) {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    bearing = bearingTo(loc.latitude, loc.longitude, place.lat, place.lon)
                }
            }
    }

    val intro = if (place.label == "Graceland")
        "Graceland, Elvis's home, is" else "${place.label} is"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onTap() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(intro, color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center)
        Text(
            valueText,
            color = Gold1,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Serif
        )
        Text(
            "$unit in this direction",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .padding(top = 18.dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Gold1.copy(alpha = 0.6f), style = Stroke(width = 2.dp.toPx()))
            }
            listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f).forEach { (label, deg) ->
                Box(
                    modifier = Modifier
                    .fillMaxSize()
                    .rotate(deg)
                ) {
                    Text(
                        label,
                        color = if (label == "N") Gold1 else Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                            .rotate(-deg)
                    )
                }
            }
            // Arrow, rotated to the destination's bearing relative to the phone's facing
            val b = bearing
            val pointer = (b?.minus(azimuth)) ?: 0f
            Canvas(
                modifier = Modifier
                    .size(86.dp)
                    .rotate(pointer)
            ) {
                val path = Path().apply {
                    moveTo(size.width / 2, 4.dp.toPx())
                    lineTo(size.width * 0.32f, size.height * 0.62f)
                    lineTo(size.width / 2, size.height * 0.48f)
                    lineTo(size.width * 0.68f, size.height * 0.62f)
                    close()
                }
                drawPath(path, if (b != null) Ruby else Color.Gray, style = Fill)
            }
        }

        Text(
            when {
                !hasSensor -> "This phone doesn't have a compass sensor."
                bearing == null -> "Finding your direction…"
                else -> "Turn until the arrow points up"
            },
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp),
            textAlign = TextAlign.Center
        )
        Text(
            "Tap to measure again",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** Formats a date and time. Imperial = MM/DD/YYYY 12-hour, Metric = DD/MM/YYYY 24-hour. */
fun formatTime(ms: Long, zone: TimeZone, metric: Boolean): String {
    val pattern = if (metric) "dd/MM/yyyy HH:mm" else "MM/dd/yyyy h:mm a"
    return SimpleDateFormat(pattern, Locale.US).apply { timeZone = zone }.format(Date(ms))
}

private fun clockLine(label: String, value: String): AnnotatedString {
    val labelStyle = SpanStyle(color = Gold1, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
    val valueStyle = SpanStyle(color = Color.White)
    return buildAnnotatedString {
        withStyle(labelStyle) { append("$label ") }
        withStyle(valueStyle) { append(value) }
    }
}

/** Two centered lines: Elvis Time, then Local Time. */
@Composable
fun ClockBar(elvis: String, local: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(clockLine("Elvis Time:", elvis), fontSize = 12.sp, textAlign = TextAlign.Center)
            Box(Modifier.height(2.dp))
            Text(clockLine("Local Time:", local), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
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
