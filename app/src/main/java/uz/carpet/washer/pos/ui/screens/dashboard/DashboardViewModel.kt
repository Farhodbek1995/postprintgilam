package uz.carpet.washer.pos.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.carpet.washer.pos.CarpetPosApp
import uz.carpet.washer.pos.data.model.Order
import uz.carpet.washer.pos.data.repository.OrderRepository
import uz.carpet.washer.pos.printer.EscPosHelper
import uz.carpet.washer.pos.printer.PrintResult
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = OrderRepository(application)
    private val printerManager = (application as CarpetPosApp).printerManager

    // Reprint natijasini ekranga uzatish uchun
    private val _printMessage = MutableStateFlow<String?>(null)
    val printMessage: StateFlow<String?> = _printMessage

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _activeFilter = MutableStateFlow(DashboardFilter.ALL)
    val activeFilter: StateFlow<DashboardFilter> = _activeFilter

    // Qidiruv va filterni bog'lash
    val orders: StateFlow<List<Order>> = combine(_searchQuery, _activeFilter) { query, filter ->
        Pair(query, filter)
    }.flatMapLatest { (query, filter) ->
        when {
            query.isNotBlank() -> repo.searchOrders(query)
            filter == DashboardFilter.PENDING_DELIVERY -> repo.getPendingDeliveryOrders()
            filter == DashboardFilter.FULLY_PAID -> repo.getFullyPaidOrders()
            else -> repo.getAllOrders()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bugungi statistika
    private val todayStart: Long get() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        return cal.timeInMillis
    }

    val todayIncome: StateFlow<Long> = repo.getTodayIncome(todayStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val todayOrderCount: StateFlow<Int> = repo.getTodayOrderCount(todayStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun onSearch(query: String) { _searchQuery.value = query }
    fun setFilter(filter: DashboardFilter) { _activeFilter.value = filter }

    fun archiveOrder(orderId: Long) {
        viewModelScope.launch { repo.archiveOrder(orderId) }
    }

    /**
     * Buyurtmani DB dan olib, ESC/POS chek formatiga aylantirib printerga yuboradi.
     * Natija _printMessage orqali ekranga uzatiladi (snackbar).
     */
    fun reprint(orderId: Long) {
        viewModelScope.launch {
            val data = repo.getOrderWithCarpets(orderId)
            if (data == null) {
                _printMessage.value = "❌ Buyurtma topilmadi"
                return@launch
            }
            val bytes = EscPosHelper.buildOrderReceipt(data.order, data.carpets)
            _printMessage.value = when (val result = printerManager.print(bytes)) {
                is PrintResult.Success -> "✅ №%06d chek chiqarildi!".format(orderId)
                is PrintResult.Failure -> "❌ ${result.reason}"
            }
        }
    }

    fun clearPrintMessage() { _printMessage.value = null }
}

enum class DashboardFilter(val label: String) {
    ALL("Barchasi"),
    PENDING_DELIVERY("Kechiktirilgan"),
    FULLY_PAID("To'liq to'langan")
}
