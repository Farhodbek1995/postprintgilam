package uz.carpet.washer.pos.ui.screens.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import uz.carpet.washer.pos.data.db.DailyStats
import uz.carpet.washer.pos.ui.screens.dashboard.formatMoney
import uz.carpet.washer.pos.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    vm: StatisticsViewModel = viewModel()   // ViewModel birinchi marta bu yerda ulanadi
) {
    // Real DB dan kelayotgan ma'lumotlar
    val weeklyStats by vm.weeklyStats.collectAsState()
    val weeklyTotal by vm.weeklyTotal.collectAsState()
    val todayIncome by vm.todayIncome.collectAsState()
    val todayOrderCount by vm.todayOrderCount.collectAsState()

    // Graf uchun maksimal qiymat (barlarning balandligi hisoblanadi)
    val maxValue = weeklyStats.maxOfOrNull { it.totalAmount }?.toFloat() ?: 0f

    Scaffold(
        containerColor = BackgroundApp,
        topBar = {
            TopAppBar(
                title = { Text("Statistika", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Orqaga") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Bugungi statistika kartalari (ikki ustun)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bugungi tushum
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Primary),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Rounded.TrendingUp, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Bugungi tushum", color = Color.White.copy(0.8f), fontSize = 11.sp)
                        Text(
                            "${formatMoney(todayIncome)} so'm",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Bugungi buyurtmalar
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Secondary),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Rounded.Receipt, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Bugungi buyurtma", color = Color.White.copy(0.8f), fontSize = 11.sp)
                        Text(
                            "$todayOrderCount ta",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Haftalik jami karta
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Rounded.BarChart, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Haftalik Jami Tushum", color = Color.White.copy(0.75f), fontSize = 13.sp)
                    Text(
                        "${formatMoney(weeklyTotal)} so'm",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Haftalik bar grafik — DB dan real ma'lumotlar
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Haftalik Ko'rsatkichlar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(20.dp))

                    if (weeklyStats.isEmpty()) {
                        // Ma'lumot yo'q holati
                        Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Bu hafta hali buyurtma yo'q",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        BarChart(
                            stats = weeklyStats,
                            maxValue = maxValue,
                            dayLabel = { vm.formatDayLabel(it) }
                        )
                    }
                }
            }

            // Kunlik tafsilot ro'yxati
            if (weeklyStats.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Kunlik Tafsilot", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(12.dp))
                        weeklyStats.forEach { stat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    vm.formatDayLabel(stat.orderDate),
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${formatMoney(stat.totalAmount)} so'm",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            HorizontalDivider(color = Divider.copy(0.5f))
                        }
                    }
                }
            }
        }
    }
}

// ===== BAR GRAFIK COMPOSABLE =====
@Composable
fun BarChart(
    stats: List<DailyStats>,
    maxValue: Float,
    dayLabel: (Long) -> String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        stats.forEach { stat ->
            val barHeight = if (maxValue > 0) (stat.totalAmount / maxValue) * 100f else 0f
            val isToday = isToday(stat.orderDate)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                // Summa (kichik)
                if (stat.totalAmount > 0) {
                    Text(
                        formatMoney(stat.totalAmount / 1000) + "k",
                        fontSize = 9.sp,
                        color = if (isToday) Primary else TextSecondary
                    )
                    Spacer(Modifier.height(2.dp))
                }

                // Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight.coerceAtLeast(4f).dp)
                    ) {
                        drawRoundRect(
                            color = if (isToday) Primary else Primary.copy(alpha = 0.45f),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    dayLabel(stat.orderDate),
                    fontSize = 11.sp,
                    color = if (isToday) Primary else TextSecondary,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// Berilgan timestamp bugunmi?
private fun isToday(timestamp: Long): Boolean {
    val today = java.util.Calendar.getInstance()
    val check = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    return today.get(java.util.Calendar.DAY_OF_YEAR) == check.get(java.util.Calendar.DAY_OF_YEAR)
            && today.get(java.util.Calendar.YEAR) == check.get(java.util.Calendar.YEAR)
}
