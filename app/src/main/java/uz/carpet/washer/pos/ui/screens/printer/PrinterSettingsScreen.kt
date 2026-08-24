package uz.carpet.washer.pos.ui.screens.printer

import android.hardware.usb.UsbDevice
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import uz.carpet.washer.pos.printer.PrintResult
import uz.carpet.washer.pos.printer.PrinterState
import uz.carpet.washer.pos.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsScreen(
    onBack: () -> Unit,
    vm: PrinterViewModel = viewModel()
) {
    val printerState by vm.printerState.collectAsState()
    var devices by remember { mutableStateOf<List<UsbDevice>>(emptyList()) }
    var showDeviceDialog by remember { mutableStateOf(false) }
    
    val printMessage by vm.printMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(printMessage) {
        printMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearPrintMessage()
        }
    }

    Scaffold(
        containerColor = BackgroundApp,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Printer Sozlamalari", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Orqaga") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Printer holat kartasi
            item {
                PrinterStatusCard(
                    state = printerState,
                    onDisconnect = { vm.disconnect() }
                )
            }

            // Ulanish tugmasi
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("USB Qurilmalar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Type-C OTG kabel orqali E200L printerni ulang",
                            fontSize = 13.sp, color = TextSecondary
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                devices = vm.getAvailableDevices()
                                showDeviceDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Icon(Icons.Rounded.Usb, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("USB Qurilmalarni Ko'rish", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Test Print tugmasi
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Test Chopi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Printerga test sahifasini yuborib ulanishni tekshiring",
                            fontSize = 13.sp, color = TextSecondary
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { vm.sendTestPrint() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(),
                            enabled = printerState is PrinterState.Connected
                        ) {
                            Icon(Icons.Rounded.Print, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Test Print Yuborish", fontWeight = FontWeight.SemiBold)
                        }

                        if (printerState !is PrinterState.Connected) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "⚠️ Avval printerga ulanib oling",
                                fontSize = 12.sp, color = Warning
                            )
                        }
                    }
                }
            }
        }
    }

    // USB Qurilmalar Dialog
    if (showDeviceDialog) {
        UsbDevicesDialog(
            devices = devices,
            onDeviceSelected = { device ->
                showDeviceDialog = false
                vm.connect(device)
            },
            onDismiss = { showDeviceDialog = false }
        )
    }
}

@Composable
fun PrinterStatusCard(state: PrinterState, onDisconnect: () -> Unit) {
    val (statusColor, statusText, statusIcon) = when (state) {
        is PrinterState.Connected -> Triple(Secondary, "Ulangan: ${state.deviceName}", Icons.Rounded.CheckCircle)
        is PrinterState.Connecting -> Triple(Warning, "Ulanmoqda...", Icons.Rounded.Sync)
        is PrinterState.Error -> Triple(Danger, state.message, Icons.Rounded.Error)
        is PrinterState.Disconnected -> Triple(Color(0xFF94A3B8), "Printer ulanmagan", Icons.Rounded.UsbOff)
    }

    val animatedColor by animateColorAsState(targetValue = statusColor)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rang indikatori (🔴/🟢)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(animatedColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(statusIcon, null, tint = animatedColor, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Printer holati", fontSize = 13.sp, color = TextSecondary)
                Text(statusText, fontWeight = FontWeight.Bold, color = animatedColor, fontSize = 15.sp)
            }
            if (state is PrinterState.Connected) {
                IconButton(onClick = onDisconnect) {
                    Icon(Icons.Rounded.LinkOff, "Uzish", tint = Danger)
                }
            }
        }
    }
}

@Composable
fun UsbDevicesDialog(
    devices: List<UsbDevice>,
    onDeviceSelected: (UsbDevice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("USB Qurilmalar", fontWeight = FontWeight.Bold)
        },
        text = {
            if (devices.isEmpty()) {
                Text("USB qurilma topilmadi. OTG kabelni tekshiring.", color = TextSecondary)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Qurilmani tanlang:", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    devices.forEach { device ->
                        Card(
                            onClick = { onDeviceSelected(device) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundApp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Usb, null, tint = Primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        device.productName ?: "Noma'lum qurilma",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "VendorID: ${device.vendorId} | ProductID: ${device.productId}",
                                        fontSize = 11.sp, color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Yopish") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
