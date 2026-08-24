package uz.carpet.washer.pos.ui.screens.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uz.carpet.washer.pos.data.db.DailyStats
import uz.carpet.washer.pos.data.repository.OrderRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Ekran uchun tayyor holat
data class StatisticsUiState(
    val weeklyStats: List<DailyStats> = emptyList(),
    val todayIncome: Long = 0L,
    val todayOrderCount: Int = 0,
    val weeklyTotal: Long = 0L
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = OrderRepository(application)

    private val todayStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    private val weekStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    // Haftalik statistika (DB dan real Flow)
    val weeklyStats: StateFlow<List<DailyStats>> = repo.getWeeklyStats(weekStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Haftalik jami summa (weeklyStats dan hisoblanadi)
    val weeklyTotal: StateFlow<Long> = repo.getWeeklyStats(weekStart)
        .map { stats -> stats.sumOf { it.totalAmount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Bugungi tushum
    val todayIncome: StateFlow<Long> = repo.getTodayIncome(todayStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Bugungi buyurtmalar soni
    val todayOrderCount: StateFlow<Int> = repo.getTodayOrderCount(todayStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Unix timestamp → "Dush", "Sesh"... kabi qisqa kun nomini qaytaradi
    fun formatDayLabel(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE", Locale("uz"))
        return sdf.format(Date(timestamp)).take(3).replaceFirstChar { it.uppercase() }
    }
}
