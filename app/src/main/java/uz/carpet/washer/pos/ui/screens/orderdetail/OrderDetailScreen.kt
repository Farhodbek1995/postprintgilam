package uz.carpet.washer.pos.ui.screens.orderdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import uz.carpet.washer.pos.data.model.Carpet
import uz.carpet.washer.pos.data.model.Order
import uz.carpet.washer.pos.data.model.OrderStatus
import uz.carpet.washer.pos.printer.PrintResult
import uz.carpet.washer.pos.ui.screens.dashboard.OrderStatusChip
import uz.carpet.washer.pos.ui.screens.dashboard.formatMoney
import uz.carpet.washer.pos.ui.screens.neworder.SectionCard
import uz.carpet.washer.pos.ui.screens.neworder.SummaryRow
import uz.carpet.washer.pos.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Long,
    onBack: () -> Unit,
    vm: OrderDetailViewModel = viewModel()
) {
    val orderData by vm.orderData.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showStatusDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val printMessage by vm.printMessage.collectAsState()

    // printMessage o'zgarganda snackbar ko'rsatish (suspend funksiya)
    LaunchedEffect(printMessage) {
        printMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            vm.clearPrintMessage()
        }
    }

    Scaffold(
        containerColor = BackgroundApp,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Buyurtma №%06d".format(orderId),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Orqaga") }
                },
                actions = {
                    IconButton(onClick = { showStatusDialog = true }) {
                        Icon(Icons.Rounded.EditNote, "Status", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        }
    ) { padding ->
        orderData?.let { data ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mijoz va holat
                SectionCard(title = "Mijoz Ma'lumotlari", icon = Icons.Rounded.Person) {
                    InfoRow(Icons.Rounded.Person, "Ism", data.order.customerName)
                    Spacer(Modifier.height(8.dp))
                    InfoRow(Icons.Rounded.Phone, "Telefon", data.order.customerPhone)
                    if (data.order.customerAddress.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        InfoRow(Icons.Rounded.LocationOn, "Manzil", data.order.customerAddress)
                    }
                    Spacer(Modifier.height(8.dp))
                    InfoRow(Icons.Rounded.Schedule, "Sana", dateFormat.format(Date(data.order.orderDate)))
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Holat: ", fontSize = 13.sp, color = TextSecondary)
                        Spacer(Modifier.width(8.dp))
                        OrderStatusChip(data.order.status)
                    }
                }

                // Gilamlar
                SectionCard(title = "${data.carpets.size} ta Gilam", icon = Icons.Rounded.Layers) {
                    data.carpets.forEachIndexed { i, carpet ->
                        if (i > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Divider)
                        CarpetDetailRow(i + 1, carpet)
                    }
                }

                // Moliyaviy holat
                SectionCard(title = "Hisob-Kitob", icon = Icons.Rounded.Payments) {
                    SummaryRow("Jami m²:", "${"%.2f".format(data.order.totalArea)} m²")
                    Spacer(Modifier.height(6.dp))
                    SummaryRow("Jami summa:", "${formatMoney(data.order.totalAmount)} so'm", isHighlight = true)
                    Spacer(Modifier.height(6.dp))
                    SummaryRow("Avans:", "${formatMoney(data.order.advanceAmount)} so'm")
                    Spacer(Modifier.height(8.dp))
                    if (data.order.isFullyPaid) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                        ) {
                            SummaryRow(
                                "Qoldiq:",
                                "0 so'm (To'liq to'landi)",
                                isHighlight = true,
                                highlightColor = Secondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (data.order.remainingAmount > 0) Color(0xFFFFFBEB) else Color(0xFFECFDF5)
                            )
                        ) {
                            SummaryRow(
                                "Qoldiq:",
                                "${formatMoney(data.order.remainingAmount)} so'm",
                                isHighlight = true,
                                highlightColor = if (data.order.remainingAmount > 0) Warning else Secondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (data.order.remainingAmount > 0 && !data.order.isFullyPaid) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { vm.payRemaining() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Primary))
                        ) {
                            Icon(Icons.Rounded.AttachMoney, null, tint = Primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Qoldiqni to'liq to'lash", color = Primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Chek chiqarish tugmasi
                Button(
                    onClick = { showReceiptDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Rounded.Print, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Chek Ko'rish va Chiqarish", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(16.dp))
            }

            // Status o'zgartirish dialogi
            var showReceiptDialog by remember { mutableStateOf(false) }

            if (showReceiptDialog) {
                AlertDialog(
                    onDismissRequest = { showReceiptDialog = false },
                    title = { Text("Chek", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .background(Color(0xFFFAFAFA))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = uz.carpet.washer.pos.printer.EscPosHelper.buildReceiptText(data.order, data.carpets),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = Color.Black
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showReceiptDialog = false
                                vm.printReceipt()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Icon(Icons.Rounded.Print, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Printerdan chiqarish")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReceiptDialog = false }) {
                            Text("Yopish", color = TextSecondary)
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
            if (showStatusDialog) {
                StatusChangeDialog(
                    currentStatus = data.order.status,
                    onStatusSelected = { vm.updateStatus(it); showStatusDialog = false },
                    onDismiss = { showStatusDialog = false }
                )
            }
        } ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
fun CarpetDetailRow(index: Int, carpet: Carpet) {
    Column {
        Text("${carpet.type} $index", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Primary)
        Spacer(Modifier.height(4.dp))
        val dimText = if (carpet.type == "Gilam" || carpet.length > 0.0) {
            "${"%.2f".format(carpet.width)} × ${"%.2f".format(carpet.length)} = ${"%.2f".format(carpet.area)} m²"
        } else {
            "${"%.2f".format(carpet.width)} dona/m"
        }
        Text(dimText, fontSize = 14.sp, color = TextPrimary)
        Text(
            "Narx: ${formatMoney(carpet.pricePerSqm)} so'm   →   ${formatMoney(carpet.totalPrice)} so'm",
            fontSize = 13.sp, color = TextSecondary
        )
    }
}

@Composable
fun StatusChangeDialog(
    currentStatus: OrderStatus,
    onStatusSelected: (OrderStatus) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Holatni o'zgartirish", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OrderStatus.values().forEach { status ->
                    Card(
                        onClick = { onStatusSelected(status) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (status == currentStatus) PrimaryLight else BackgroundApp
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(status.label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            if (status == currentStatus) {
                                Icon(Icons.Rounded.Check, null, tint = Primary, modifier = Modifier.size(18.dp))
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
