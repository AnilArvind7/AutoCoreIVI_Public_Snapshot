package com.example.autocoreividashboard

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.example.autocoreividashboard.custom.AnalogClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.autocoreividashboard.data.VehicleRepositoryImpl
import androidx.compose.runtime.collectAsState
import com.example.autocoreividashboard.data.VehicleUiState
import androidx.compose.ui.platform.LocalContext
import com.example.autocoreividashboard.custom.SpeedometerGauge3
import com.example.autocoreividashboard.service.VehiclePropertyServiceClient
import com.example.autocoreividashboard.vhal.MockVhalController
import com.example.autocoreividashboard.vhal.VhalInitializer
import com.example.autocoreividashboard.vhal.VhalPropertyEventListener
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.autocoreividashboard.data.VehiclePropertyStore
import com.example.autocoreividashboard.data.VehiclePropertyKey
import com.example.autocoreividashboard.data.source.VehicleDataSourceType
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import com.example.autocoreividashboard.vhal.NativeVhalTest
import com.example.autocoreividashboard.vhal.VhalRuntime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

private val Background = Color(0xFF050914)
private val Panel = Color(0xFF101B30)
private val Blue = Color(0xFF168CFF)
private val LightBlue = Color(0xFF55B8FF)
private val Green = Color(0xFF39E69B)
private val Amber = Color(0xFFFFB547)
private val Red = Color(0xFFFF555F)
private val Muted = Color(0xFF899AB5)
private val White = Color(0xFFF3F7FF)

class MainActivity : ComponentActivity() {
    private lateinit var mockVhalController: MockVhalController
    private lateinit var vhalEventListener: VhalPropertyEventListener

    private val carPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.forEach { (permission, granted) ->
                Log.i(
                    "AutoCoreAAOS",
                    "$permission granted: $granted"
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requiredPermissions = arrayOf(
            "android.car.permission.CAR_SPEED",
            "android.car.permission.CAR_ENERGY"
        )

        val missingPermissions = requiredPermissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            carPermissionLauncher.launch(missingPermissions.toTypedArray())
        }

        Log.d(
            "AutoCoreAAOS",
            "CAR_SPEED permission: ${
                checkSelfPermission("android.car.permission.CAR_SPEED")
            }"
        )

        Log.d(
            "AutoCoreAAOS",
            "CAR_ENERGY permission: ${
                checkSelfPermission("android.car.permission.CAR_ENERGY")
            }"
        )

        lifecycleScope.launch {
            VehiclePropertyStore.manager.properties.collect { properties ->
                Log.i(
                    "AutoCoreAAOS",
                    """
                    Ignition: ${properties[VehiclePropertyKey.IGNITION_STATE]?.value}
                    Gear: ${properties[VehiclePropertyKey.GEAR_SELECTION]?.value}
                    Parking Brake: ${properties[VehiclePropertyKey.PARKING_BRAKE_ON]?.value}
                    """.trimIndent()
                )
            }
        }

        val vhalInitialized = VhalInitializer.initialize()

        if (!vhalInitialized) {
            Log.e("AutoCoreVHAL", "VHAL initialization failed")
        }
        mockVhalController = VhalRuntime.controller
        NativeVhalTest.testNativeReads()

        vhalEventListener = VhalPropertyEventListener { event ->
            Log.d(
                "AutoCoreVHALListener",
                "RECEIVED EVENT: ID=${event.propertyId} | " +
                        "AREA=${event.areaId} | VALUE=${event.value}"
            )
        }

        Log.d("AutoCoreMainActivity", "MainActivity onCreate")

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Blue,
                    secondary = Green,
                    background = Background,
                    surface = Panel
                )
            ) {
                DashboardPager()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        Log.d("AutoCoreVHAL", "Starting Mock VHAL")

        mockVhalController.registerListener(vhalEventListener)
        mockVhalController.start()
    }

    override fun onStop() {
        Log.d("AutoCoreVHAL", "Stopping Mock VHAL")

        mockVhalController.unregisterListener(vhalEventListener)
        mockVhalController.stop()

        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@Composable
private fun DashboardPager() {
    val context = LocalContext.current.applicationContext

    var selectedSource by remember {
        mutableStateOf(VehicleDataSourceType.SIMULATION)
    }

    val repository = remember(selectedSource) {
        VehicleRepositoryImpl(
            context = context,
            sourceType = selectedSource
        )
    }

    val vehicleState by repository.vehicleState.collectAsState()

    val serviceClient = remember {
        VehiclePropertyServiceClient()
    }

    val propertyCallback = remember(serviceClient) {
        object : IVehiclePropertyCallback.Stub() {
            override fun onPropertyChanged(
                propertyName: String,
                value: String
            ) {
                Log.d(
                    "AutoCoreBinder",
                    "Property changed: $propertyName = $value"
                )
            }
        }
    }

    DisposableEffect(repository) {
        repository.connect()
        onDispose {
            repository.disconnect()
        }
    }

    DisposableEffect(serviceClient) {
        serviceClient.bind(context) {
            Log.d("AutoCoreBinder", "Binder connected")
            serviceClient.registerCallback(propertyCallback)
        }

        onDispose {
            serviceClient.unregisterCallback(propertyCallback)
            serviceClient.unbind(context)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> InstrumentClusterScreen(
                    vehicleState = vehicleState,
                    sourceType = selectedSource,
                    onSourceSelected = { selectedSource = it }
                )
                1 -> InfotainmentHomeScreen(vehicleState)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(
                            width = if (pagerState.currentPage == index) 22.dp else 7.dp,
                            height = 7.dp
                        )
                        .background(
                            color = if (pagerState.currentPage == index)
                                LightBlue else Muted,
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}

@Composable
private fun InfotainmentHomeScreen(vehicleState: VehicleUiState) {

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {

            // Top information bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "AUTOCORE",
                        color = LightBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )

                    Text(
                        "INFOTAINMENT",
                        color = Muted,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (vehicleState.gatewayStatus == "Connected")
                                    Green else Amber,
                                RoundedCornerShape(50)
                            )
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        vehicleState.gatewayStatus.uppercase(),
                        color = if (vehicleState.gatewayStatus == "Connected")
                            Green else Amber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main three-panel layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // LEFT: Navigation rail
                Column(
                    modifier = Modifier
                        .weight(0.15f)
                        .fillMaxHeight()
                        .background(Panel, RoundedCornerShape(16.dp))
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    Text("⌂", color = LightBlue, fontSize = 26.sp)
                    Text("♫", color = Muted, fontSize = 24.sp)
                    Text("◉", color = Muted, fontSize = 24.sp)
                    Text("⚙", color = Muted, fontSize = 24.sp)
                }

                // LEFT CONTENT: Clock, weather and media
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .background(Panel, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {

                    // Analog clock
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnalogClock(
                            modifier = Modifier.width(225.dp)
                        )
                    }
                }

                // CENTER: Vehicle visualization and live status
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                        .background(Panel, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        vehicleState.gear,
                        color = when (vehicleState.gear) {
                            "P" -> White
                            "R" -> Red
                            "N" -> Green
                            "D" -> LightBlue
                            else -> White
                        },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Light
                    )

                    val openDoorCount = listOf(
                        vehicleState.frontLeftDoorOpen,
                        vehicleState.frontRightDoorOpen,
                        vehicleState.rearLeftDoorOpen,
                        vehicleState.rearRightDoorOpen
                    ).count { it }

                    val vehicleImage = when {
                        openDoorCount == 0 ->
                            R.drawable.ic_door_all_closed_transparent

                        openDoorCount == 4 ->
                            R.drawable.ic_doors_all_open_transparent

                        openDoorCount == 1 && vehicleState.frontLeftDoorOpen ->
                            R.drawable.ic_door_left_front_open_transparent

                        openDoorCount == 1 && vehicleState.frontRightDoorOpen ->
                            R.drawable.ic_door_right_front_open_transparent

                        openDoorCount == 1 && vehicleState.rearLeftDoorOpen ->
                            R.drawable.ic_door_rear_left_open_transparent

                        openDoorCount == 1 && vehicleState.rearRightDoorOpen ->
                            R.drawable.ic_door_rear_right_open_transparent

                        else ->
                            R.drawable.ic_door_all_closed_transparent
                    }
                    // Vehicle image gets the maximum available space
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = vehicleImage),
                            contentDescription = "AutoCore vehicle top view",
                            modifier = Modifier
                                .fillMaxHeight()
                                .graphicsLayer {
                                    scaleX = 2f
                                    scaleY = 2f
                                },
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Vehicle systems
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (vehicleState.headlightsOn) "LIGHTS ON" else "LIGHTS OFF",
                            color = if (vehicleState.headlightsOn) Amber else Muted,
                            fontSize = 9.sp
                        )

                        Text("•", color = Muted)

                        Text(
                            if (vehicleState.hvacOn) "AC ON" else "AC OFF",
                            color = if (vehicleState.hvacOn) LightBlue else Muted,
                            fontSize = 9.sp
                        )
                    }
                }

                // RIGHT: Navigation panel
                Column(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                        .background(Panel, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {

                    Text(
                        "NAVIGATION",
                        color = LightBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF071426))
                    ) {
                        Image(
                            painter = painterResource(
                                id = R.drawable.autocore_ivi_dark_navigation_map
                            ),
                            contentDescription = "AutoCore navigation map",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Bottom climate control strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel, RoundedCornerShape(14.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    if (vehicleState.hvacOn) "❄ CLIMATE ON" else "❄ CLIMATE OFF",
                    color = if (vehicleState.hvacOn) LightBlue else Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    vehicleState.temperature?.let { "$it°C" } ?: "--°C",
                    color = White,
                    fontSize = 16.sp
                )

                Text(
                    "FAN  AUTO",
                    color = Muted,
                    fontSize = 12.sp
                )

                Text(
                    if (vehicleState.ignitionOn) "READY" else "OFF",
                    color = if (vehicleState.ignitionOn) Green else Red,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun InstrumentClusterScreen(
    vehicleState: VehicleUiState,
    sourceType: VehicleDataSourceType,
    onSourceSelected: (VehicleDataSourceType) -> Unit
) {

    val gatewayStatus = vehicleState.gatewayStatus
    val gatewayMessage = vehicleState.gatewayMessage
    val speed = vehicleState.speed
    val battery = vehicleState.battery
    val ignitionOn = vehicleState.ignitionOn
    val headlightsOn = vehicleState.headlightsOn
    val headlightBeam = vehicleState.headlightBeam
    val hvacOn = vehicleState.hvacOn
    val temperature = vehicleState.temperature
    val gear = vehicleState.gear

    val frontLeftDoorOpen = vehicleState.frontLeftDoorOpen
    val frontRightDoorOpen = vehicleState.frontRightDoorOpen
    val rearLeftDoorOpen = vehicleState.rearLeftDoorOpen
    val rearRightDoorOpen = vehicleState.rearRightDoorOpen

    val anyDoorOpen =
        frontLeftDoorOpen ||
                frontRightDoorOpen ||
                rearLeftDoorOpen ||
                rearRightDoorOpen

    val doorAjarWarning = anyDoorOpen && (speed ?: 0) > 5

    val panelAccent = if (anyDoorOpen) {
        Color(0xFFFF4655)
    } else {
        Color(0xFF19BFFF)
    }

    val openDoorImage = when {
        frontRightDoorOpen -> R.drawable.ic_vehicle_front_right_open_warning
        frontLeftDoorOpen -> R.drawable.ic_vehicle_front_left_open_warning
        rearRightDoorOpen -> R.drawable.ic_vehicle_rear_right_open_warning
        rearLeftDoorOpen -> R.drawable.ic_vehicle_rear_left_open_warning
        else -> null
    }

    val openDoorLabel = when {
        frontRightDoorOpen -> "FRONT RIGHT DOOR OPEN"
        frontLeftDoorOpen -> "FRONT LEFT DOOR OPEN"
        rearRightDoorOpen -> "REAR RIGHT DOOR OPEN"
        rearLeftDoorOpen -> "REAR LEFT DOOR OPEN"
        else -> ""
    }

    val gearColor = when (gear) {
        "P" -> White
        "R" -> Red
        "N" -> Green
        "D" -> LightBlue
        else -> White
    }

    val gearDescription = when (gear) {
        "P" -> "PARK MODE"
        "R" -> "REVERSE MODE"
        "N" -> "NEUTRAL MODE"
        "D" -> "DRIVE MODE"
        else -> "UNKNOWN MODE"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {

            // Top information bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "AUTOCORE",
                        color = LightBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        modifier = Modifier.offset(y = (-12).dp)
                    )

                    Text(
                        "DIGITAL INSTRUMENT CLUSTER",
                        color = Muted,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.offset(y = (-16).dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (gatewayStatus == "Connected") Green else Amber,
                                        CircleShape
                                    )
                            )

                            Text(
                                text = gatewayStatus.uppercase(),
                                color = if (gatewayStatus == "Connected") Green else Amber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (gatewayStatus == "Reconnecting") {
                            Text(
                                text = gatewayMessage,
                                color = Amber.copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Main instrument cluster
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // LEFT: Battery gauge
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularGauge(
                        value = (battery ?: 0).toFloat(),
                        maximum = 100f,
                        label = "% POWER",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )

                    Column(
                        modifier = Modifier.offset(y = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            battery?.toString() ?: "--",
                            color = Green,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Light
                        )

                        Text(
                            "% CHARGE",
                            color = LightBlue,
                            fontSize = 9.sp,
                            modifier = Modifier.offset(y = (-8).dp)
                        )
                    }
                }

                // CENTER: Rectangular information panel
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .offset(y = (-20).dp)
                        .background(
                            color = panelAccent.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 0.05.dp,
                            color = Panel,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(
                            horizontal = 12.dp,
                            vertical = 12.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    if (anyDoorOpen) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            openDoorImage?.let { imageRes ->
                                Image(
                                    painter = painterResource(id = imageRes),
                                    contentDescription = openDoorLabel,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .offset(y = 8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Text(
                                text = if (doorAjarWarning) "DOOR AJAR" else openDoorLabel,
                                color = Red,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 2.sp
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {

                            // Vehicle identification
                            Text(
                                "VEHICLE AC-001",
                                color = Muted,
                                fontSize = 10.sp,
                                letterSpacing = 2.sp
                            )

                            // Vehicle readiness
                            Text(
                                if (ignitionOn) "READY" else "OFF",
                                color = if (ignitionOn) Green else Red,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Gear mode indicator
                            Row(
                                modifier = Modifier
                                    .background(
                                        color = gearColor.copy(alpha = 0.07f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = gearColor.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 4.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = gear,
                                    color = gearColor,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(42.dp)
                                        .background(gearColor.copy(alpha = 0.7f))
                                )

                                Column {
                                    Text(
                                        text = gearDescription,
                                        color = gearColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = when (gear) {
                                            "P" -> "Vehicle Parked"
                                            "R" -> "Reverse Selected"
                                            "N" -> "Transmission Neutral"
                                            "D" -> "Vehicle in Drive"
                                            else -> "Unknown Gear"
                                        },
                                        color = Muted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Estimated range
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "ESTIMATED RANGE",
                                    color = Muted,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    battery?.let { "${it * 4} km" } ?: "--",
                                    color = White,
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Odometer
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "ODOMETER",
                                    color = Muted,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    "48470 km",
                                    color = White,
                                    fontSize = 17.sp
                                )
                            }

                            // Gateway status
                            /*StatusIndicator(
                                label = sourceType.name,
                                active = gatewayStatus == "Connected"
                            )*/
                            StatusIndicator(
                                label = sourceType.name,
                                sourceType = sourceType,
                                active = gatewayStatus == "Connected"
                            )
                        }
                    }
                }

                // RIGHT: Speedometer gauge
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    SpeedometerGauge3(
                        speed = speed ?: 0,
                        maxSpeed = 220,
                        onSpeedChange = {},
                        enabled = false,
                        showDoorWarning = anyDoorOpen,
                        doorAjarWarning = anyDoorOpen && (speed ?: 0) > 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )

                    Column(
                        modifier = Modifier.offset(y = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            speed?.toString() ?: "--",
                            color = White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Light
                        )

                        Text(
                            "km/h",
                            color = LightBlue,
                            fontSize = 11.sp,
                            modifier = Modifier.offset(y = (-4).dp)
                        )
                    }
                }
            }

            // Bottom vehicle status strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        Panel,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VehicleSourceSelector(
                    selectedSource = sourceType,
                    onSourceSelected = onSourceSelected
                )

                // PARK
                Column(
                    modifier = Modifier.weight(0.75f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (vehicleState.parkingBrakeOn) Red else Muted,
                                shape = RoundedCornerShape(50)
                            )
                    )

                    Text(
                        "PARK",
                        color = if (vehicleState.parkingBrakeOn) White else Muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                VerticalDividerLine()

                // HEADLIGHT
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.5.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Image(
                            painter = painterResource(
                                id = if (headlightBeam == "HIGH") {
                                    R.drawable.ic_headlight_high_icon
                                } else {
                                    R.drawable.ic_headlight_low_icon
                                }
                            ),
                            contentDescription = "$headlightBeam beam headlight",
                            modifier = Modifier.size(44.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = if (!headlightsOn) {
                                ColorFilter.tint(Muted)
                            } else {
                                null
                            }
                        )

                        Text(
                            text = "HEADLIGHT",
                            color = if (headlightsOn) White else Muted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.offset(y = (-10).dp)
                        )
                    }
                }

                VerticalDividerLine()

                // CLIMATE
                Column(
                    modifier = Modifier.weight(0.8f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.5.dp)
                ) {
                    Text(
                        "AC",
                        color = if (hvacOn) Green else Muted,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "CLIMATE",
                        color = Muted,
                        fontSize = 9.sp
                    )
                }

                VerticalDividerLine()

                // TEMPERATURE
                Column(
                    modifier = Modifier.weight(0.8f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.5.dp)
                ) {
                    Text(
                        "°C",
                        color = Green,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        temperature?.let { "${it}°C" } ?: "--°C",
                        color = White,
                        fontSize = 9.sp
                    )
                }

                VerticalDividerLine()

                // GEAR
                Row(
                    modifier = Modifier.weight(2f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        "GEAR",
                        color = White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        gear,
                        color = gearColor,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )

                    GearSegmentSelector(
                        selectedGear = gear,
                        selectedColor = gearColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CircularGauge(
    value: Float,
    maximum: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    val progress = (value / maximum).coerceIn(0f, 1f)

    Canvas(modifier = modifier) {

        val diameter = size.minDimension
        val radius = diameter * 0.43f
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        val strokeWidth = diameter * 0.025f

        // Outer background ring
        drawArc(
            color = Color(0xFF25364F),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(
                centerX - radius,
                centerY - radius
            ),
            size = androidx.compose.ui.geometry.Size(
                radius * 2,
                radius * 2
            ),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )

        // Active blue ring
        drawArc(
            color = Blue,
            startAngle = 135f,
            sweepAngle = 270f * progress,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(
                centerX - radius,
                centerY - radius
            ),
            size = androidx.compose.ui.geometry.Size(
                radius * 2,
                radius * 2
            ),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )

        // Inner illuminated ring
        drawCircle(
            color = Color(0xFF164A91),
            radius = radius * 0.70f,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            style = Stroke(diameter * 0.009f)
        )

        // Tick marks and labels
        val nativeCanvas = drawContext.canvas.nativeCanvas

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = diameter * 0.045f
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.NORMAL
            )
        }

        val tickCount = 10

        for (i in 0..tickCount) {
            val angle = Math.toRadians(135.0 + (270.0 / tickCount) * i)

            val startRadius = radius * 0.88f
            val endRadius = radius * 0.96f

            val x1 = centerX + cos(angle).toFloat() * startRadius
            val y1 = centerY + sin(angle).toFloat() * startRadius
            val x2 = centerX + cos(angle).toFloat() * endRadius
            val y2 = centerY + sin(angle).toFloat() * endRadius

            drawLine(
                color = LightBlue,
                start = androidx.compose.ui.geometry.Offset(x1, y1),
                end = androidx.compose.ui.geometry.Offset(x2, y2),
                strokeWidth = diameter * 0.007f
            )

            val labelRadius = radius * 0.77f
            val labelX = centerX + cos(angle).toFloat() * labelRadius
            val labelY = centerY + sin(angle).toFloat() * labelRadius

            val tickValue = (maximum * i / tickCount).toInt()

            nativeCanvas.drawText(
                tickValue.toString(),
                labelX,
                labelY + diameter * 0.015f,
                paint
            )
        }

        // Needle
        val needleAngle = Math.toRadians(135.0 + 270.0 * progress)
        val needleLength = radius * 0.66f

        val needleX =
            centerX + cos(needleAngle).toFloat() * needleLength
        val needleY =
            centerY + sin(needleAngle).toFloat() * needleLength

        drawLine(
            color = White,
            start = androidx.compose.ui.geometry.Offset(centerX, centerY),
            end = androidx.compose.ui.geometry.Offset(needleX, needleY),
            strokeWidth = diameter * 0.012f,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = Blue,
            radius = diameter * 0.035f,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY)
        )

        drawCircle(
            color = White,
            radius = diameter * 0.014f,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY)
        )
    }
}

@Composable
private fun VerticalDividerLine() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color(0xFF40516D))
    )
}


@Composable
private fun GearSegmentSelector(
    selectedGear: String,
    selectedColor: Color
) {
    Row(
        modifier = Modifier
            .width(208.dp)
            .height(40.dp)
            .border(
                width = 1.dp,
                color = Color(0xFF52627B),
                shape = RoundedCornerShape(9.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        listOf("P", "R", "N", "D").forEachIndexed { index, gear ->

            val selected = selectedGear == gear

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(1.dp)
                    .then(
                        if (selected) {
                            Modifier
                                .background(
                                    selectedColor.copy(alpha = 0.12f),
                                    RoundedCornerShape(2.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = selectedColor,
                                    shape = RoundedCornerShape(7.dp)
                                )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gear,
                    color = if (selected) selectedColor else Muted,
                    fontSize = 17.sp,
                    fontWeight = if (selected)
                        FontWeight.Bold
                    else
                        FontWeight.Medium
                )
            }

            if (index < 3) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(25.dp)
                        .background(Color(0xFF40516D))
                )
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    label: String,
    sourceType: VehicleDataSourceType,
    active: Boolean
) {
    val sourceColor = sourceStatusColor(sourceType)

    Box(
        modifier = Modifier
            .widthIn(min = 90.dp)
            .height(30.dp)
            .background(
                color = sourceColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) sourceColor else Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun VehicleSourceSelector(
    selectedSource: VehicleDataSourceType,
    onSourceSelected: (VehicleDataSourceType) -> Unit
) {
    var dialogVisible by remember { mutableStateOf(false) }

    Image(
        painter = painterResource(id = R.drawable.ic_power_button),
        contentDescription = "Select vehicle data source",
        modifier = Modifier
            .size(64.dp)
            .clickable {
                dialogVisible = true
            }
    )

    if (dialogVisible) {
        AlertDialog(
            onDismissRequest = { dialogVisible = false },
            title = { Text("Select vehicle source") },
            text = {
                Column {
                    VehicleDataSourceType.entries.forEach { source ->
                        TextButton(
                            onClick = {
                                onSourceSelected(source)
                                dialogVisible = false
                            }
                        ) {
                            Text(
                                text = if (source == selectedSource) {
                                    "✓ ${source.name}"
                                } else {
                                    source.name
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogVisible = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun sourceStatusColor(source: VehicleDataSourceType): Color {
    return when (source) {
        VehicleDataSourceType.NATIVE -> Color(0xFF69F0AE)
        VehicleDataSourceType.AAOS -> Color(0xFFFFE082)
        VehicleDataSourceType.SIMULATION -> Color(0xFF80D8FF)
    }
}
