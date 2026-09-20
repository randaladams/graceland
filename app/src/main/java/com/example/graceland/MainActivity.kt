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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
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

// Graceland, 3764 Elvis Presley Blvd, Memphis, TN
private const val GRACELAND_LAT = 35.0477
private const val GRACELAND_LON = -90.0261
private const val METERS_PER_MILE = 1609.344

// Google's TEST banner ad unit. Replace with your own before release.
private const val BANNER_AD_UNIT = "ca-app-pub-3940256099942544/6300978111"

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

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main area
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { onButtonPressed() },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(140.dp)
            ) {
                Text(
                    text = if (loading) "Locating…" else "How far away is Graceland?",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Box(modifier = Modifier.padding(top = 32.dp), contentAlignment = Alignment.Center) {
                val m = meters
                when {
                    message != null -> Text(message!!, textAlign = TextAlign.Center, fontSize = 18.sp)
                    m != null -> {
                        val value = if (useKm) m / 1000.0 else m / METERS_PER_MILE
                        val unit = if (useKm) "kilometers" else "miles"
                        Text(
                            text = "Graceland, Elvis's home, is\n%,.0f %s\naway".format(value, unit),
                            textAlign = TextAlign.Center,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Unit slider
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Miles", fontWeight = if (!useKm) FontWeight.Bold else FontWeight.Normal)
            Slider(
                value = sliderPos,
                onValueChange = { sliderPos = it },
                onValueChangeFinished = { sliderPos = if (sliderPos >= 0.5f) 1f else 0f },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            Text("Km", fontWeight = if (useKm) FontWeight.Bold else FontWeight.Normal)
        }

        // Upgrade + ad (hidden for Pro users)
        if (!isPro) {
            OutlinedButton(
                onClick = { billing.launchUpgrade(activity) },
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            ) {
                Text("Upgrade to Pro – remove ads")
            }
            AdBanner(modifier = Modifier.fillMaxWidth())
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
