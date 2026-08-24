package uz.carpet.washer.pos.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import uz.carpet.washer.pos.data.model.Order
import uz.carpet.washer.pos.data.model.OrderStatus
import uz.carpet.washer.pos.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNewOrder: () -> Unit,
    onOrderClick: (Long) -> Unit,
    onEditOrder: (Long) -> Unit,
    onPrinterSettings: () -> Unit,
    onStatistics: () -> Unit,
    vm: DashboardViewModel = viewModel()
) {
    val orders by vm.orders.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val activeFilter by vm.activeFilter.collectAsState()
    val todayIncome by vm.todayIncome.collectAsState()
    val todayOrderCount by vm.todayOrderCount.collectAsState()
    val printMessage by vm.printMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Reprint natijasi kelganda snackbar ko'rsatish
    LaunchedEffect(printMessage) {
        printMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            vm.clearPrintMessage()
        }
    }

    Scaffold(
        containerColor = BackgroundApp,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gilam Yuvish POS", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Gilam yuvish fabrikasi", fontSize = 12.sp, color = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface),
                actions = {
                    IconButton(onClick = onStatistics) {
                        Icon(Icons.Rounded.BarChart, "Statistika", tint = Primary)
                    }
                    IconButton(onClick = onPrinterSettings) {
                        Icon(Icons.Rounded.Print, "Printer", tint = Primary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewOrder,
                icon = { Icon(Icons.Rounded.Add, "Yangi") },
                text = { Text("Yangi Buyurtma", fontWeight = FontWeight.SemiBold) },
                containerColor = Primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Statistika kartalari
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Bugungi tushum",
                        value = formatMoney(todayIncome) + " so'm",
                        icon = Icons.Rounded.Payments,
                        gradientColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Buyurtmalar",
                        value = "$todayOrderCount ta",
                        icon = Icons.Rounded.Receipt,
                        gradientColors = listOf(Color(0xFF059669), Color(0xFF047857))
                    )
                }
            }

            // Qidiruv
            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = vm::onSearch
                )
            }

            // Filter chiplar
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DashboardFilter.values()) { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { vm.setFilter(filter) },
                            label = { Text(filter.label, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Buyurtmalar soni
            item {
                Text(
                    "${orders.size} ta buyurtma",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Buyurtmalar ro'yxati
            if (orders.isEmpty()) {
                item { EmptyState() }
            } else {
                items(orders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        onClick = { onOrderClick(order.id) },
                        onEdit = { onEditOrder(order.id) },
                        onReprint = { vm.reprint(order.id) },
                        onArchive = { vm.archiveOrder(order.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) } // FAB uchun joy
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    gradientColors: List<Color>
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(gradientColors))
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(title, color = Color.White.copy(0.85f), fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Ism, telefon yoki №...", color = TextSecondary, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Primary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Clear, null, tint = TextSecondary)
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = Divider,
            unfocusedContainerColor = CardSurface,
            focusedContainerColor = CardSurface
        ),
        singleLine = true
    )
}

@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onReprint: () -> Unit,
    onArchive: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Chap tomon
            Column(modifier = Modifier.weight(1f)) {
                // Buyurtma raqami va ism
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "№%06d".format(order.id),
                        fontSize = 12.sp,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("•", color = TextSecondary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        order.customerName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Phone, null, modifier = Modifier.size(13.dp), tint = TextSecondary)
                    Spacer(Modifier.width(4.dp))
                    Text(order.customerPhone, fontSize = 13.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(13.dp), tint = TextSecondary)
                    Spacer(Modifier.width(4.dp))
                    Text(dateFormat.format(Date(order.orderDate)), fontSize = 12.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(8.dp))
                OrderStatusChip(order.status)
            }

            // O'ng tomon
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatMoney(order.totalAmount) + " so'm",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary
                )
                if (order.remainingAmount > 0) {
                    Text(
                        "Qoldiq: ${formatMoney(order.remainingAmount)}",
                        fontSize = 11.sp,
                        color = Warning
                    )
                } else {
                    Text("To'liq to'langan", fontSize = 11.sp, color = Secondary)
                }
                Spacer(Modifier.height(8.dp))

                // Harakatlar (Print + Ko'proq)
                Row {
                    IconButton(
                        onClick = onReprint,
                        modifier = Modifier
                            .size(36.dp)
                            .background(PrimaryLight, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Print, "Reprint", tint = Primary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(Icons.Rounded.MoreVert, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Tahrirlash") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                onClick = { showMenu = false; onEdit() }  // ← To'g'ri: edit ekraniga
                            )
                            DropdownMenuItem(
                                text = { Text("Arxivlash", color = Danger) },
                                leadingIcon = { Icon(Icons.Rounded.Archive, null, tint = Danger) },
                                onClick = { showMenu = false; onArchive() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus) {
    val (color, bg) = when (status) {
        OrderStatus.RECEIVED -> Pair(Color(0xFF2563EB), Color(0xFFEFF6FF))
        OrderStatus.WASHING -> Pair(Color(0xFFF59E0B), Color(0xFFFFFBEB))
        OrderStatus.READY -> Pair(Color(0xFF10B981), Color(0xFFECFDF5))
        OrderStatus.DELIVERED -> Pair(Color(0xFF64748B), Color(0xFFF1F5F9))
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            status.label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Inbox, null, modifier = Modifier.size(64.dp), tint = Color(0xFFCBD5E1))
            Spacer(Modifier.height(12.dp))
            Text("Buyurtma topilmadi", fontSize = 16.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Text("Yangi buyurtma qo'shing!", fontSize = 13.sp, color = TextSecondary)
        }
    }
}

fun formatMoney(amount: Long): String = String.format("%,d", amount).replace(",", " ")
