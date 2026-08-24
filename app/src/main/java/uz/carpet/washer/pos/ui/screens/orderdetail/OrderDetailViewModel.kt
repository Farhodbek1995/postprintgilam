package uz.carpet.washer.pos.ui.screens.orderdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uz.carpet.washer.pos.CarpetPosApp
import uz.carpet.washer.pos.data.model.OrderStatus
import uz.carpet.washer.pos.data.model.OrderWithCarpets
import uz.carpet.washer.pos.data.repository.OrderRepository
import uz.carpet.washer.pos.printer.EscPosHelper
import uz.carpet.washer.pos.printer.PrintResult
import uz.carpet.washer.pos.printer.UsbPrinterManager

class OrderDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repo = OrderRepository(application)

    // Application darajasidagi YAGONA UsbPrinterManager — PrinterViewModel bilan BAHAM KO'RILADI
    // Printer sozlamalar ekranida ulangan bo'lsa, shu yerda ham ulangan ko'rinadi
    private val printerManager: UsbPrinterManager =
        (application as CarpetPosApp).printerManager

    private val orderId: Long = savedStateHandle["orderId"] ?: 0L

    private val _orderData = MutableStateFlow<OrderWithCarpets?>(null)
    val orderData: StateFlow<OrderWithCarpets?> = _orderData

    private val _printMessage = MutableStateFlow<String?>(null)
    val printMessage: StateFlow<String?> = _printMessage

    init {
        loadOrder()
    }

    private fun loadOrder() {
        viewModelScope.launch {
            _orderData.value = repo.getOrderWithCarpets(orderId)
        }
    }

    fun updateStatus(status: OrderStatus) {
        viewModelScope.launch {
            _orderData.value?.let { data ->
                val updated = data.order.copy(status = status)
                repo.updateOrder(updated, data.carpets)
                loadOrder()
            }
        }
    }

    fun payRemaining() {
        viewModelScope.launch {
            _orderData.value?.let { data ->
                if (data.order.remainingAmount > 0 && !data.order.isFullyPaid) {
                    val updated = data.order.copy(isFullyPaid = true)
                    repo.updateOrder(updated, data.carpets)
                    loadOrder()
                }
            }
        }
    }

    fun printReceipt() {
        viewModelScope.launch {
            val data = _orderData.value
            if (data == null) {
                _printMessage.value = "❌ Ma'lumot yuklanmadi"
                return@launch
            }
            val bytes = EscPosHelper.buildOrderReceipt(data.order, data.carpets)
            _printMessage.value = when (val result = printerManager.print(bytes)) {
                is PrintResult.Success -> "✅ Chek muvaffaqiyatli chiqarildi!"
                is PrintResult.Failure -> "❌ ${result.reason}"
            }
        }
    }

    fun clearPrintMessage() { _printMessage.value = null }
}
