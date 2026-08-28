package com.example.qatarprayertimes.ui

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.qatarprayertimes.data.AppLocale
import com.example.qatarprayertimes.ui.theme.Accent
import com.example.qatarprayertimes.ui.theme.Background
import com.example.qatarprayertimes.ui.theme.Card
import com.example.qatarprayertimes.ui.theme.Foreground
import com.example.qatarprayertimes.ui.theme.Muted
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val DOHA_LATITUDE = 25.2854
private const val DOHA_LONGITUDE = 51.5310
private const val ALIGNMENT_ENTER_DEGREES = 5f
private const val ALIGNMENT_EXIT_DEGREES = 8f
private const val HEADING_FILTER = 0.18f

private data class QiblaLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
)

private class HeadingSmoother {
    private var initialized = false
    private var continuousHeading = 0f

    fun update(rawHeading: Float): Float {
        if (!initialized) {
            initialized = true
            continuousHeading = rawHeading
        } else {
            continuousHeading += shortestAngleDelta(continuousHeading, rawHeading) * HEADING_FILTER
        }
        return continuousHeading
    }
}

@SuppressLint("MissingPermission")
@Composable
fun QiblaCompassScreen(locale: AppLocale) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val rotationVector = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val magnetometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }
    val hasCompass = rotationVector != null || (accelerometer != null && magnetometer != null)

    var headingTarget by remember { mutableFloatStateOf(0f) }
    var hasHeading by remember { mutableStateOf(false) }
    var sensorAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE) }
    var currentLocation by remember { mutableStateOf<QiblaLocation?>(null) }
    var magneticDeclination by remember {
        mutableFloatStateOf(declinationAt(DOHA_LATITUDE, DOHA_LONGITUDE))
    }
    var qiblaBearing by remember {
        mutableFloatStateOf(calculateQiblaBearing(DOHA_LATITUDE, DOHA_LONGITUDE))
    }
    var locationRefreshKey by remember { mutableIntStateOf(0) }
    var isResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var isAligned by remember { mutableStateOf(false) }
    val headingSmoother = remember { HeadingSmoother() }

    val animatedHeading by animateFloatAsState(
        targetValue = headingTarget,
        animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing),
        label = "qibla_heading",
    )
    val heading = normalizeAngle(animatedHeading)
    val turnDegrees = shortestAngleDelta(heading, qiblaBearing)
    val absoluteTurn = abs(turnDegrees)
    val hasLocationPermission = hasLocationPermission(context)
    val usesCurrentLocation = currentLocation != null
    val compassNeedsCalibration = sensorAccuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM

    LaunchedEffect(absoluteTurn, hasHeading) {
        isAligned = hasHeading && if (isAligned) {
            absoluteTurn < ALIGNMENT_EXIT_DEGREES
        } else {
            absoluteTurn <= ALIGNMENT_ENTER_DEGREES
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = event == Lifecycle.Event.ON_RESUME ||
                (event != Lifecycle.Event.ON_PAUSE &&
                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val applyLocation: (Location) -> Unit = { location ->
        currentLocation = QiblaLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy.takeIf { location.hasAccuracy() },
        )
        qiblaBearing = calculateQiblaBearing(location.latitude, location.longitude)
        magneticDeclination = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            location.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
        ).declination
    }

    val sensorEventListener = remember(
        context,
        rotationVector,
        accelerometer,
        magnetometer,
        headingSmoother,
    ) {
        object : SensorEventListener {
            private val accelerometerReading = FloatArray(3)
            private val magnetometerReading = FloatArray(3)
            private val rotationMatrix = FloatArray(9)
            private val remappedMatrix = FloatArray(9)
            private val orientation = FloatArray(3)
            private var hasAccelerometerReading = false
            private var hasMagnetometerReading = false

            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        updateHeading(rotationMatrix)
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        event.values.copyInto(accelerometerReading)
                        hasAccelerometerReading = true
                        updateFallbackHeading()
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        event.values.copyInto(magnetometerReading)
                        hasMagnetometerReading = true
                        updateFallbackHeading()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR ||
                    sensor?.type == Sensor.TYPE_MAGNETIC_FIELD
                ) {
                    sensorAccuracy = accuracy
                }
            }

            private fun updateFallbackHeading() {
                if (rotationVector == null && hasAccelerometerReading && hasMagnetometerReading &&
                    SensorManager.getRotationMatrix(
                        rotationMatrix,
                        null,
                        accelerometerReading,
                        magnetometerReading,
                    )
                ) {
                    updateHeading(rotationMatrix)
                }
            }

            private fun updateHeading(matrix: FloatArray) {
                val (axisX, axisY) = displayAxes(displayRotation(context))
                if (!SensorManager.remapCoordinateSystem(matrix, axisX, axisY, remappedMatrix)) return
                SensorManager.getOrientation(remappedMatrix, orientation)

                val magneticHeading = normalizeAngle(Math.toDegrees(orientation[0].toDouble()).toFloat())
                val trueHeading = normalizeAngle(magneticHeading + magneticDeclination)
                headingTarget = headingSmoother.update(trueHeading)
                hasHeading = true
            }
        }
    }

    val locationListener = remember {
        object : LocationListener {
            override fun onLocationChanged(location: Location) = applyLocation(location)

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }
    }

    DisposableEffect(
        sensorManager,
        locationManager,
        rotationVector,
        accelerometer,
        magnetometer,
        hasLocationPermission,
        isResumed,
        locationRefreshKey,
    ) {
        if (!isResumed) {
            onDispose { }
        } else {
            if (rotationVector != null) {
                sensorManager.registerListener(
                    sensorEventListener,
                    rotationVector,
                    SensorManager.SENSOR_DELAY_GAME,
                )
            } else if (accelerometer != null && magnetometer != null) {
                sensorManager.registerListener(
                    sensorEventListener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_GAME,
                )
                sensorManager.registerListener(
                    sensorEventListener,
                    magnetometer,
                    SensorManager.SENSOR_DELAY_GAME,
                )
            }

            if (hasLocationPermission) {
                runCatching {
                    bestKnownLocation(locationManager)?.let(applyLocation)
                    listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                        .filter(locationManager::isProviderEnabled)
                        .forEach { provider ->
                            locationManager.requestLocationUpdates(
                                provider,
                                10_000L,
                                10f,
                                locationListener,
                            )
                        }
                }
            }

            onDispose {
                sensorManager.unregisterListener(sensorEventListener)
                runCatching { locationManager.removeUpdates(locationListener) }
            }
        }
    }

    val qiblaLabel = if (locale == AppLocale.AR) "Qibla" else "Qibla"
    val directionLabel = when {
        !hasCompass -> "Compass unavailable"
        !hasHeading -> "Finding your heading…"
        isAligned -> "Facing Qibla"
        turnDegrees > 0f -> "Turn ${absoluteTurn.roundToInt()}° right"
        else -> "Turn ${absoluteTurn.roundToInt()}° left"
    }
    val directionDetail = when {
        !hasCompass -> "Your device does not have the sensors needed for a compass."
        !hasHeading -> "Hold your phone flat while the compass starts."
        isAligned -> "You are within ${ALIGNMENT_ENTER_DEGREES.roundToInt()}° of the Qibla direction."
        else -> "Keep the top of your phone pointing toward the gold marker."
    }
    val locationLabel = when {
        usesCurrentLocation -> currentLocation?.accuracyMeters?.let { "Current location · ±${it.roundToInt()} m" }
            ?: "Current location"
        hasLocationPermission -> "Finding your location…"
        else -> "Doha estimate · location permission needed"
    }
    val pulseTransition = rememberInfiniteTransition(label = "qibla_alignment_pulse")
    val alignmentPulse by pulseTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "qibla_alignment_alpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = qiblaLabel,
                color = Foreground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Use the compass to align the top of your phone toward Makkah.",
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(Card)
                .border(1.dp, Muted.copy(alpha = 0.14f), CircleShape)
                .padding(start = 14.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (usesCurrentLocation) Accent else Muted),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = locationLabel,
                color = if (usesCurrentLocation) Foreground else Muted,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { locationRefreshKey++ }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh location",
                    tint = Accent,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Card)
                .border(1.dp, Accent.copy(alpha = 0.16f), RoundedCornerShape(28.dp))
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                CompassDial(
                    heading = heading,
                    qiblaBearing = qiblaBearing,
                    isAligned = isAligned,
                    alignmentPulse = alignmentPulse,
                    modifier = Modifier.size(312.dp),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Qibla bearing",
                    color = Muted,
                    fontSize = 13.sp,
                )
                Text(
                    text = "${qiblaBearing.roundToInt()}° from true north",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(if (isAligned) Accent.copy(alpha = 0.15f) else Card)
                .border(
                    1.dp,
                    if (isAligned) Accent.copy(alpha = 0.48f) else Muted.copy(alpha = 0.16f),
                    RoundedCornerShape(22.dp),
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = directionLabel,
                color = if (isAligned) Accent else Foreground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = directionDetail,
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
            )
        }

        if (compassNeedsCalibration && hasCompass) {
            GuidanceCard(
                title = "Calibrate your compass",
                body = "Move your phone in a figure-eight, keep it flat, and stay away from metal or magnets.",
            )
        } else {
            GuidanceCard(
                title = "For the best result",
                body = "Hold your phone flat and turn slowly until the gold marker reaches the top of the dial.",
            )
        }
    }
}

@Composable
private fun CompassDial(
    heading: Float,
    qiblaBearing: Float,
    isAligned: Boolean,
    alignmentPulse: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        val outerRadius = radius - 5.dp.toPx()
        val dialRadius = radius - 31.dp.toPx()
        val qiblaRelativeAngle = normalizeAngle(qiblaBearing - heading)

        if (isAligned) {
            drawCircle(
                color = Accent.copy(alpha = alignmentPulse),
                radius = outerRadius,
            )
        }

        drawCircle(color = Background, radius = outerRadius)
        drawCircle(
            color = Muted.copy(alpha = 0.26f),
            radius = outerRadius,
            style = Stroke(width = 1.5.dp.toPx()),
        )
        drawCircle(
            color = Accent.copy(alpha = 0.26f),
            radius = dialRadius,
            style = Stroke(width = 1.dp.toPx()),
        )

        for (worldAngle in 0 until 360 step 15) {
            val relativeAngle = normalizeAngle(worldAngle - heading)
            val isMajor = worldAngle % 30 == 0
            val outer = pointOnCircle(center, outerRadius - 11.dp.toPx(), relativeAngle)
            val inner = pointOnCircle(
                center,
                outerRadius - if (isMajor) 24.dp.toPx() else 18.dp.toPx(),
                relativeAngle,
            )
            drawLine(
                color = if (worldAngle % 90 == 0) Foreground.copy(alpha = 0.72f) else Muted.copy(alpha = 0.38f),
                start = outer,
                end = inner,
                strokeWidth = if (isMajor) 1.8.dp.toPx() else 1.dp.toPx(),
            )
        }

        listOf(
            "N" to 0f,
            "E" to 90f,
            "S" to 180f,
            "W" to 270f,
        ).forEach { (label, worldAngle) ->
            val relativeAngle = normalizeAngle(worldAngle - heading)
            val point = pointOnCircle(center, outerRadius - 42.dp.toPx(), relativeAngle)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                point.x,
                point.y - (compassTextPaint.ascent() + compassTextPaint.descent()) / 2f,
                compassTextPaint,
            )
        }

        val targetTip = pointOnCircle(center, dialRadius + 2.dp.toPx(), qiblaRelativeAngle)
        val targetBase = pointOnCircle(center, 35.dp.toPx(), qiblaRelativeAngle)
        drawLine(
            color = Accent,
            start = targetBase,
            end = targetTip,
            strokeWidth = 8.dp.toPx(),
        )
        val headLeft = pointOnCircle(center, dialRadius - 21.dp.toPx(), qiblaRelativeAngle - 10f)
        val headRight = pointOnCircle(center, dialRadius - 21.dp.toPx(), qiblaRelativeAngle + 10f)
        drawLine(color = Accent, start = targetTip, end = headLeft, strokeWidth = 8.dp.toPx())
        drawLine(color = Accent, start = targetTip, end = headRight, strokeWidth = 8.dp.toPx())
        drawCircle(color = Accent, radius = 7.dp.toPx(), center = targetTip)

        // The fixed top marker represents the direction the phone is pointing.
        val topMarker = pointOnCircle(center, outerRadius - 4.dp.toPx(), 0f)
        drawCircle(color = Foreground, radius = 5.dp.toPx(), center = topMarker)
        drawCircle(color = Background, radius = 2.dp.toPx(), center = topMarker)
    }
}

@Composable
private fun GuidanceCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Card)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(text = title, color = Foreground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(text = body, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

private val compassTextPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(243, 240, 232)
    textAlign = android.graphics.Paint.Align.CENTER
    textSize = 12f * android.content.res.Resources.getSystem().displayMetrics.scaledDensity
    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
}

private fun pointOnCircle(center: Offset, radius: Float, angleDegrees: Float): Offset {
    val radians = Math.toRadians((angleDegrees - 90f).toDouble())
    return Offset(
        x = center.x + cos(radians).toFloat() * radius,
        y = center.y + sin(radians).toFloat() * radius,
    )
}

private fun shortestAngleDelta(from: Float, to: Float): Float {
    var difference = normalizeAngle(to) - normalizeAngle(from)
    if (difference > 180f) difference -= 360f
    if (difference < -180f) difference += 360f
    return difference
}

private fun normalizeAngle(angle: Float): Float = ((angle % 360f) + 360f) % 360f

private fun calculateQiblaBearing(latitude: Double, longitude: Double): Float {
    val kaabaLatitude = Math.toRadians(21.4225)
    val kaabaLongitude = Math.toRadians(39.8262)
    val deviceLatitude = Math.toRadians(latitude)
    val deviceLongitude = Math.toRadians(longitude)
    val y = sin(kaabaLongitude - deviceLongitude)
    val x = cos(deviceLatitude) * Math.tan(kaabaLatitude) -
        sin(deviceLatitude) * cos(kaabaLongitude - deviceLongitude)
    return normalizeAngle(Math.toDegrees(Math.atan2(y, x)).toFloat())
}

private fun declinationAt(latitude: Double, longitude: Double): Float = GeomagneticField(
    latitude.toFloat(),
    longitude.toFloat(),
    0f,
    System.currentTimeMillis(),
).declination

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

private fun bestKnownLocation(locationManager: LocationManager): Location? {
    return listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    ).mapNotNull { provider ->
        runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
    }.maxWithOrNull(
        compareBy<Location> { it.time }.thenByDescending { location ->
            if (location.hasAccuracy()) -location.accuracy else Float.NEGATIVE_INFINITY
        },
    )
}

@Suppress("DEPRECATION")
private fun displayRotation(context: Context): Int {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return windowManager.defaultDisplay.rotation
}

private fun displayAxes(rotation: Int): Pair<Int, Int> = when (rotation) {
    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
}
