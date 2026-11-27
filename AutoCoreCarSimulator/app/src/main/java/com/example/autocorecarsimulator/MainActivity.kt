package com.example.autocorecarsimulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autocorecarsimulator.custom.BatteryMeter
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.example.autocorecarsimulator.custom.RotaryGearSelector
import com.example.autocorecarsimulator.custom.SpeedometerGaugeModern
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.collectAsState
import com.example.autocorecarsimulator.data.VehicleData
import com.example.autocorecarsimulator.data.VehicleRepositoryImpl
import com.example.autocorecarsimulator.data.VehicleUiState
import android.util.Log

private val Background = Color(0xFF0B1020)
private val Panel = Color(0xFF171F33)
private val Blue = Color(0xFF65B9FF)
private val Green = Color(0xFF50D6A0)
private val Muted = Color(0xFF9BAAC2)
private val Amber = Color(0xFFFFC266)
private val Red = Color(0xFFFF7777)
private const val DOOR_INNER_ANGLE = 14f
private const val DOOR_OUTER_ANGLE = 14f

private val DOOR_HORIZONTAL_GAP = -20.dp
private val DOOR_VERTICAL_GAP = 8.dp
private val CentreHexagon = GenericShape { size, _ ->
    moveTo(size.width * 0.22f, 0f)
    lineTo(size.width * 0.78f, 0f)
    lineTo(size.width, size.height * 0.5f)
    lineTo(size.width * 0.78f, size.height)
    lineTo(size.width * 0.22f, size.height)
    lineTo(0f, size.height * 0.5f)
    close()
}

private fun doorPanelShape(
    innerAngle: Float,
    outerAngle: Float,
    leftSide: Boolean,
    upper: Boolean
) = GenericShape { size, _ ->

    val innerOffset =
        size.height * kotlin.math.tan(
            Math.toRadians(innerAngle.toDouble())
        ).toFloat()

    val outerOffset =
        size.height * kotlin.math.tan(
            Math.toRadians(outerAngle.toDouble())
        ).toFloat()

    if (leftSide && upper) {
        moveTo(outerOffset, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width - innerOffset, size.height)
        lineTo(0f, size.height)
    } else if (leftSide && !upper) {
        moveTo(0f, 0f)
        lineTo(size.width - innerOffset, 0f)
        lineTo(size.width, size.height)
        lineTo(outerOffset, size.height)
    } else if (!leftSide && upper) {
        moveTo(0f, 0f)
        lineTo(size.width - outerOffset, 0f)
        lineTo(size.width, size.height)
        lineTo(innerOffset, size.height)
    } else {
        moveTo(innerOffset, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width - outerOffset, size.height)
        lineTo(0f, size.height)
    }

    close()
}

private val LeftUpperDoorShape =
    doorPanelShape(DOOR_INNER_ANGLE, DOOR_OUTER_ANGLE, true, true)

private val LeftLowerDoorShape =
    doorPanelShape(DOOR_INNER_ANGLE, DOOR_OUTER_ANGLE, true, false)

private val RightUpperDoorShape =
    doorPanelShape(DOOR_INNER_ANGLE, DOOR_OUTER_ANGLE, false, true)

private val RightLowerDoorShape =
    doorPanelShape(DOOR_INNER_ANGLE, DOOR_OUTER_ANGLE, false, false)


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Blue,
                    secondary = Green,
                    background = Background,
                    surface = Panel
                )
            ) {
                CarSimulatorScreen()
            }
        }
    }
}

@Composable
private fun CarSimulatorScreen() {

    // Vehicle state
    var speed by remember { mutableFloatStateOf(0f) }
    var battery by remember { mutableFloatStateOf(82f) }
    var temperature by remember { mutableFloatStateOf(22f) }

    var ignitionOn by remember { mutableStateOf(true) }
    var headlightsOn by remember { mutableStateOf(false) }
    var selectedHeadlightBeam by remember { mutableStateOf("LOW") }
    var hvacOn by remember { mutableStateOf(true) }

    // Individual door states
    var frontLeftDoorOpen by remember { mutableStateOf(false) }
    var frontRightDoorOpen by remember { mutableStateOf(false) }
    var rearLeftDoorOpen by remember { mutableStateOf(false) }
    var rearRightDoorOpen by remember { mutableStateOf(false) }

    // Bottom navigation state
    var selectedTab by remember {
        mutableStateOf("dashboard")
    }

    var selectedGear by remember {
        mutableStateOf("P")
    }

    // Switching ignition OFF selects Park.
    LaunchedEffect(ignitionOn) {
        if (!ignitionOn) {
            selectedGear = "P"
            speed = 0f
        }
    }

    // Repository
    val repository = remember {
        VehicleRepositoryImpl()
    }

    val vehicleState by repository.vehicleState.collectAsState()
    val vehicleProperties by repository.vehicleProperties.collectAsState()
    LaunchedEffect(vehicleProperties) {
        Log.d(
            "AutoCoreProperties",
            "Simulator Properties: $vehicleProperties"
        )
    }

    val gatewayStatus = vehicleState.gateway.status
    val gatewayMessage = vehicleState.gateway.message

    // Gateway connection lifecycle
    DisposableEffect(repository) {
        repository.connect()

        onDispose {
            repository.disconnect()
        }
    }

    // Publish vehicle state whenever controls change
    // or the gateway connection is restored.
    LaunchedEffect(
        speed,
        battery,
        temperature,
        ignitionOn,
        headlightsOn,
        selectedHeadlightBeam,
        hvacOn,
        selectedGear,
        frontLeftDoorOpen,
        frontRightDoorOpen,
        rearLeftDoorOpen,
        rearRightDoorOpen,
        gatewayStatus
    ) {
        repository.updateVehicleData(
            VehicleData(
                speed = speed.roundToInt(),
                battery = battery.roundToInt(),
                ignitionOn = ignitionOn,
                headlightsOn = headlightsOn,
                headlightBeam = selectedHeadlightBeam,
                hvacOn = hvacOn,
                temperature = temperature.roundToInt(),
                gear = selectedGear,
                frontLeftDoorOpen = frontLeftDoorOpen,
                frontRightDoorOpen = frontRightDoorOpen,
                rearLeftDoorOpen = rearLeftDoorOpen,
                rearRightDoorOpen = rearRightDoorOpen
            )
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background,
        bottomBar = {
            SimulatorBottomBar(
                selectedTab = selectedTab,
                ignitionOn = ignitionOn,
                onTabSelected = { selectedTab = it },
                onPowerClick = {
                    ignitionOn = !ignitionOn
                }
            )
        }
    ) { innerPadding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Background
        ) {

            when (selectedTab) {

                "dashboard" -> {
                    DashboardScreen(
                        speed = speed,
                        battery = battery,
                        temperature = temperature,
                        ignitionOn = ignitionOn,
                        headlightsOn = headlightsOn,
                        hvacOn = hvacOn,
                        gatewayStatus = gatewayStatus,
                        gatewayMessage = gatewayMessage,
                        onSpeedChange = { speed = it },
                        onBatteryChange = { battery = it },
                        onIgnitionChange = { ignitionOn = it },
                        onHeadlightsChange = { enabled ->
                            headlightsOn = enabled
                            if (!enabled) {
                                selectedHeadlightBeam = "LOW"
                            }
                        },
                        selectedHeadlightBeam = selectedHeadlightBeam,
                        onHeadlightBeamSelected = {
                            selectedHeadlightBeam = it
                        },
                        onHvacChange = { hvacOn = it },
                        onTemperatureChange = { temperature = it },
                        selectedGear = selectedGear,
                        onGearSelected = { requestedGear ->
                            if (ignitionOn && speed <= 0.5f) {
                                selectedGear = requestedGear
                            }
                        }
                    )
                }

                "ivi" -> {
                    IviControlsScreen(
                        frontLeftDoorOpen = frontLeftDoorOpen,
                        frontRightDoorOpen = frontRightDoorOpen,
                        rearLeftDoorOpen = rearLeftDoorOpen,
                        rearRightDoorOpen = rearRightDoorOpen,
                        onFrontLeftChange = {
                            frontLeftDoorOpen = it
                        },
                        onFrontRightChange = {
                            frontRightDoorOpen = it
                        },
                        onRearLeftChange = {
                            rearLeftDoorOpen = it
                        },
                        onRearRightChange = {
                            rearRightDoorOpen = it
                        },
                        onOpenAll = {
                            frontLeftDoorOpen = true
                            frontRightDoorOpen = true
                            rearLeftDoorOpen = true
                            rearRightDoorOpen = true
                        },
                        onCloseAll = {
                            frontLeftDoorOpen = false
                            frontRightDoorOpen = false
                            rearLeftDoorOpen = false
                            rearRightDoorOpen = false
                        }
                    )
                }
            }
        }
    }
}

// --------------------------------------------------
// DASHBOARD TAB
// --------------------------------------------------

@Composable
private fun DashboardScreen(
    speed: Float,
    battery: Float,
    temperature: Float,
    ignitionOn: Boolean,
    headlightsOn: Boolean,
    hvacOn: Boolean,
    gatewayStatus: String,
    gatewayMessage: String,
    onSpeedChange: (Float) -> Unit,
    onBatteryChange: (Float) -> Unit,
    onIgnitionChange: (Boolean) -> Unit,
    onHeadlightsChange: (Boolean) -> Unit,
    selectedHeadlightBeam: String,
    onHeadlightBeamSelected: (String) -> Unit,
    onHvacChange: (Boolean) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    selectedGear: String,
    onGearSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "AUTOCORE",
            color = Blue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )

        Text(
            text = "Car Simulator",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "VEHICLE AC-001  •  CONCEPT EV",
            color = Muted,
            fontSize = 12.sp
        )

        // Vehicle state card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Panel
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "VEHICLE STATE",
                    color = Muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Text(
                    text = "${speed.roundToInt()} km/h",
                    color = Blue,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )

                SpeedometerGaugeModern(
                    speed = speed.roundToInt(),
                    maxSpeed = 220,
                    selectedGear = selectedGear,
                    onSpeedChange = { speed ->
                        onSpeedChange(speed.toFloat())
                    },
                    enabled = ignitionOn
                )

                Text(
                    text = "SPEED CONTROL  •  ${speed.roundToInt()} km/h",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Slider(
                    value = speed,
                    onValueChange = onSpeedChange,
                    valueRange = 0f..220f,
                    enabled = ignitionOn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0 km/h", color = Muted, fontSize = 12.sp)
                    Text("220 km/h", color = Muted, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Rotary Gear Selector
                Text(
                    text = "GEAR SELECTOR",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    RotaryGearSelector(
                        selectedGear = selectedGear,
                        enabled = ignitionOn && speed <= 0.5f,
                        onGearSelected = onGearSelected
                    )
                }

                Text(
                    text = if (speed == 0f) {
                        "Vehicle stationary • Gear selection available"
                    } else {
                        "Stop vehicle to change gear"
                    },
                    color = Muted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "BATTERY STATUS",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                BatteryMeter(
                    percentage = battery.roundToInt()
                )

                Slider(
                    value = battery,
                    onValueChange = onBatteryChange,
                    valueRange = 0f..100f,
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        // Vehicle controls
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Panel
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "VEHICLE CONTROLS",
                    color = Muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                ToggleRow(
                    label = "Ignition",
                    description = if (ignitionOn) {
                        "Vehicle on"
                    } else {
                        "Vehicle off"
                    },
                    checked = ignitionOn,
                    onCheckedChange = onIgnitionChange
                )

                ToggleRow(
                    label = "Headlights",
                    description = if (headlightsOn) {
                        "$selectedHeadlightBeam beam active"
                    } else {
                        "Lights off"
                    },
                    checked = headlightsOn,
                    onCheckedChange = onHeadlightsChange
                )

                if (headlightsOn) {
                    HeadlightBeamSelector(
                        selectedBeam = selectedHeadlightBeam,
                        onBeamSelected = onHeadlightBeamSelected
                    )
                }

                ToggleRow(
                    label = "Climate control",
                    description = if (hvacOn) {
                        "HVAC active"
                    } else {
                        "HVAC off"
                    },
                    checked = hvacOn,
                    onCheckedChange = onHvacChange
                )

                Text(
                    text = "Temperature: ${temperature.roundToInt()}°C",
                    color = Color.White
                )

                Slider(
                    value = temperature,
                    onValueChange = onTemperatureChange,
                    valueRange = 16f..30f,
                    enabled = hvacOn
                )
            }
        }

        // Gateway connection
        val gatewayColor = when (gatewayStatus) {
            "Connected" -> Green
            "Reconnecting", "Connecting", "Connecting…" -> Amber
            else -> Red
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Panel
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "GATEWAY CONNECTION",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(
                                    color = gatewayColor,
                                    shape = CircleShape
                                )
                        )

                        Text(
                            text = gatewayStatus.uppercase(),
                            color = gatewayColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                color = gatewayColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 0.25.dp)
                    ) {
                        Text(
                            text = when (gatewayStatus) {
                                "Connected" -> "LIVE"
                                "Reconnecting" -> "RETRYING"
                                "Connecting", "Connecting…" -> "CONNECTING"
                                else -> "OFFLINE"
                            },
                            color = gatewayColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Text(
                    text = gatewayMessage,
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        }

        Text(
            text = "AutoCore IVI • Car Simulator v0.3",
            color = Muted,
            fontSize = 11.sp
        )
    }
}

// --------------------------------------------------
// IVI TAB - INITIAL PLACEHOLDER
// --------------------------------------------------


@Composable
private fun IviControlsScreen(
    frontLeftDoorOpen: Boolean,
    frontRightDoorOpen: Boolean,
    rearLeftDoorOpen: Boolean,
    rearRightDoorOpen: Boolean,
    onFrontLeftChange: (Boolean) -> Unit,
    onFrontRightChange: (Boolean) -> Unit,
    onRearLeftChange: (Boolean) -> Unit,
    onRearRightChange: (Boolean) -> Unit,
    onOpenAll: () -> Unit,
    onCloseAll: () -> Unit
) {
    fun selectOnlyDoor(door: String) {
        val isAlreadyOpen = when (door) {
            "FL" -> frontLeftDoorOpen
            "FR" -> frontRightDoorOpen
            "RL" -> rearLeftDoorOpen
            "RR" -> rearRightDoorOpen
            else -> false
        }

        onFrontLeftChange(door == "FL" && !isAlreadyOpen)
        onFrontRightChange(door == "FR" && !isAlreadyOpen)
        onRearLeftChange(door == "RL" && !isAlreadyOpen)
        onRearRightChange(door == "RR" && !isAlreadyOpen)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // HEADER
        Text(
            text = "AUTOCORE",
            color = Blue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )

        Text(
            text = "IVI Controls",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "INFOTAINMENT & VEHICLE FEATURES",
            color = Muted,
            fontSize = 12.sp
        )

        // VEHICLE DOOR CONTROL CARD
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Panel
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "VEHICLE CONTROL – DOORS",
                    color = Blue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Control individual doors or all doors at once.",
                    color = Muted,
                    fontSize = 12.sp
                )

                // HEXAGONAL DOOR LAYOUT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(DOOR_HORIZONTAL_GAP)
                ) {

                    // LEFT DOORS
                    Column(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxSize()
                            .offset(x = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(DOOR_VERTICAL_GAP)
                    ) {

                        DoorPolygonButton(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = LeftUpperDoorShape,
                            label = "FL",
                            description = "FRONT LEFT",
                            isOpen = frontLeftDoorOpen,
                            onClick = {
                                selectOnlyDoor("FL")
                            }
                        )

                        DoorPolygonButton(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = LeftLowerDoorShape,
                            label = "RL",
                            description = "REAR LEFT",
                            isOpen = rearLeftDoorOpen,
                            onClick = {
                                selectOnlyDoor("RL")
                            }
                        )
                    }

                    // CENTER HEXAGON
                    val allOpen =
                        frontLeftDoorOpen &&
                                frontRightDoorOpen &&
                                rearLeftDoorOpen &&
                                rearRightDoorOpen

                    val allClosed =
                        !frontLeftDoorOpen &&
                                !frontRightDoorOpen &&
                                !rearLeftDoorOpen &&
                                !rearRightDoorOpen

                    val isHexagonActive = !allClosed
                    val borderColor = if (isHexagonActive) {
                        Color(0xFF19BFFF)
                    } else {
                        Color(0xFF46556B)
                    }

                    val backgroundColor = if (isHexagonActive) {
                        Color(0xFF164D79)
                    } else {
                        Color(0xFF25344B)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1.6f)
                            .fillMaxSize()
                            .clip(CentreHexagon)
                            .background(backgroundColor)
                            .border(
                                width = 1.5.dp,
                                color = borderColor,
                                shape = CentreHexagon
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val openDoorCount = listOf(
                                frontLeftDoorOpen,
                                frontRightDoorOpen,
                                rearLeftDoorOpen,
                                rearRightDoorOpen
                            ).count { it }

                            val vehicleImage = when {
                                openDoorCount == 0 ->
                                    R.drawable.ic_door_all_closed

                                openDoorCount == 4 ->
                                    R.drawable.ic_doors_all_open

                                frontLeftDoorOpen && openDoorCount == 1 ->
                                    R.drawable.ic_door_left_front_open

                                frontRightDoorOpen && openDoorCount == 1 ->
                                    R.drawable.ic_door_right_front_open

                                rearLeftDoorOpen && openDoorCount == 1 ->
                                    R.drawable.ic_door_rear_left_open

                                rearRightDoorOpen && openDoorCount == 1 ->
                                    R.drawable.ic_door_rear_right_open

                                else ->
                                    R.drawable.ic_door_all_closed
                            }

                            // 1. CAR IMAGE
                            Image(
                                painter = painterResource(
                                    id = vehicleImage
                                ),
                                contentDescription = "Vehicle top view",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .graphicsLayer {
                                        scaleX = 1.55f
                                        scaleY = 1.55f
                                    },
                                contentScale = ContentScale.Fit
                            )

                            // 2. ALL DOORS
                            Text(
                                text = "ALL DOORS",
                                color = Muted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            // Calculate overall door status
                            val allOpen =
                                frontLeftDoorOpen &&
                                        frontRightDoorOpen &&
                                        rearLeftDoorOpen &&
                                        rearRightDoorOpen

                            val allClosed =
                                !frontLeftDoorOpen &&
                                        !frontRightDoorOpen &&
                                        !rearLeftDoorOpen &&
                                        !rearRightDoorOpen
                            val doorStatus = when {
                                allOpen -> "OPEN"
                                allClosed -> "CLOSED"
                                else -> "PARTIALLY OPEN"
                            }

                            // 3. OPEN / CLOSED STATUS
                            Text(
                                text = doorStatus,
                                color = when {
                                    allOpen -> Color(0xFF19D99B)
                                    allClosed -> Blue
                                    else -> Color(0xFFFFB547)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 1.dp)
                            )

                            val unsupportedCombination = openDoorCount in 2..3
                            if (unsupportedCombination) {
                                Text(
                                    text = "MULTIPLE DOORS OPEN",
                                    color = Amber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // RIGHT DOORS
                    Column(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxSize()
                            .offset(x = (-9).dp),
                        verticalArrangement = Arrangement.spacedBy(DOOR_VERTICAL_GAP),
                    ) {

                        DoorPolygonButton(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = RightUpperDoorShape,
                            label = "FR",
                            description = "FRONT RIGHT",
                            isOpen = frontRightDoorOpen,
                            onClick = {
                                selectOnlyDoor("FR")
                            }
                        )

                        DoorPolygonButton(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = RightLowerDoorShape,
                            label = "RR",
                            description = "REAR RIGHT",
                            isOpen = rearRightDoorOpen,
                            onClick = {
                                selectOnlyDoor("RR")
                            }
                        )
                    }
                }

                // GLOBAL OPEN / CLOSE CONTROLS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val allOpen =
                        frontLeftDoorOpen &&
                                frontRightDoorOpen &&
                                rearLeftDoorOpen &&
                                rearRightDoorOpen

                    val allClosed =
                        !frontLeftDoorOpen &&
                                !frontRightDoorOpen &&
                                !rearLeftDoorOpen &&
                                !rearRightDoorOpen
                    val doorStatus = when {
                        allOpen -> "OPEN"
                        allClosed -> "CLOSED"
                        else -> "PARTIALLY"
                    }
                    DoorActionButton(
                        label = "OPEN ALL",
                        onClick = onOpenAll,
                        selected = allOpen,
                        modifier = Modifier.weight(1f)
                    )

                    DoorActionButton(
                        label = "CLOSE ALL",
                        onClick = onCloseAll,
                        selected = allClosed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // LIGHTING CONTROLS PLACEHOLDER
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Panel
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text = "COMING NEXT",
                    color = Blue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Lighting Controls",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Indicators, hazard lights, parking lights and lighting states.",
                    color = Muted,
                    fontSize = 14.sp
                )
            }
        }

        Text(
            text = "AutoCore IVI • IVI Control Center",
            color = Muted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun DoorPolygonButton(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    label: String,
    description: String,
    isOpen: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isOpen) {
        Color(0xFF19BFFF)
    } else {
        Color(0xFF46556B)
    }

    val backgroundColor = if (isOpen) {
        Color(0xFF164D79)
    } else {
        Color(0xFF25344B)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.5.dp, borderColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            DoorIcon(
                isOpen = isOpen,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
private fun DoorIcon(
    isOpen: Boolean,
    modifier: Modifier = Modifier
) {
    val iconColor = if (isOpen) {
        Color(0xFF63D9FF)
    } else {
        Color(0xFF65B9FF)
    }

    Canvas(modifier = modifier.size(30.dp)) {
        val w = size.width
        val h = size.height

        val doorPath = Path().apply {
            moveTo(w * 0.22f, h * 0.88f)
            lineTo(w * 0.22f, h * 0.30f)
            quadraticTo(
                w * 0.22f, h * 0.16f,
                w * 0.38f, h * 0.16f
            )
            lineTo(w * 0.72f, h * 0.16f)
            quadraticTo(
                w * 0.84f, h * 0.16f,
                w * 0.84f, h * 0.30f
            )
            lineTo(w * 0.84f, h * 0.88f)
            close()
        }

        drawPath(
            path = doorPath,
            color = iconColor,
            style = Stroke(
                width = 2.2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Door handle
        drawLine(
            color = iconColor,
            start = Offset(w * 0.65f, h * 0.48f),
            end = Offset(w * 0.77f, h * 0.48f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun DoorActionButton(
    label: String,
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val buttonColor = if (selected) {
        Color(0xFF126DC2)
    } else {
        Color(0xFF25344B)
    }

    val textColor = if (selected) {
        Color.White
    } else {
        Muted
    }

    Card(
        modifier = modifier
            .height(54.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = buttonColor
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --------------------------------------------------
// BOTTOM NAVIGATION
// --------------------------------------------------


@Composable
private fun SimulatorBottomBar(
    selectedTab: String,
    ignitionOn: Boolean,
    onTabSelected: (String) -> Unit,
    onPowerClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Panel)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BottomTabItem(
                title = "Dashboard",
                symbol = "▦",
                selected = selectedTab == "dashboard",
                onClick = { onTabSelected("dashboard") }
            )

            BottomTabItem(
                title = "IVI",
                symbol = "▣",
                selected = selectedTab == "ivi",
                onClick = { onTabSelected("ivi") }
            )
        }

        AutomotivePowerButton(
            isOn = ignitionOn,
            onClick = onPowerClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun AutomotivePowerButton(
    isOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glowColor = if (isOn) {
        Color(0xFF35F58A)
    } else {
        Color(0xFFFF5252)
    }

    Box(
        modifier = modifier
            .padding(top = 2.dp)
            .size(76.dp)
            .shadow(14.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFE5E9ED),
                        Color(0xFF687584),
                        Color(0xFF252D38),
                        Color(0xFFB8C0C8)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF111820),
                            Color(0xFF03070D),
                            Color(0xFF202B37)
                        )
                    )
                )
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .size(width = 17.dp, height = 4.dp)
                        .shadow(
                            elevation = if (isOn) 9.dp else 0.dp,
                            shape = RoundedCornerShape(50)
                        )
                        .clip(RoundedCornerShape(50))
                        .background(glowColor)
                )

                Text(
                    text = "ENGINE",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (!isOn) "START" else "STOP",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y=(-4).dp)
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    title: String,
    symbol: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(bottom = 13.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = symbol,
            color = if (selected) Blue else Muted,
            fontSize = 23.sp
        )

        Text(
            text = title,
            color = if (selected) Blue else Muted,
            fontSize = 11.sp,
            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )
    }
}

// --------------------------------------------------
// HEADLIGHT BEAM SELECTOR
// --------------------------------------------------

@Composable
private fun HeadlightBeamSelector(
    selectedBeam: String,
    onBeamSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeadlightBeamOption(
            label = "LOW BEAM",
            beam = "LOW",
            imageRes = R.drawable.ic_headlight_low_beam_transparent,
            selected = selectedBeam == "LOW",
            onClick = { onBeamSelected("LOW") },
            modifier = Modifier.weight(1f)
        )

        HeadlightBeamOption(
            label = "HIGH BEAM",
            beam = "HIGH",
            imageRes = R.drawable.ic_headlight_high_beam_transparent,
            selected = selectedBeam == "HIGH",
            onClick = { onBeamSelected("HIGH") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HeadlightBeamOption(
    label: String,
    beam: String,
    imageRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Blue else Color(0xFF354158),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF22334B) else Color(0xFF111A2B)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "$label headlight icon",
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = label,
                color = if (selected) Blue else Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --------------------------------------------------
// REUSABLE TOGGLE ROW
// --------------------------------------------------

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = description,
                color = Muted,
                fontSize = 12.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
