package uz.carpet.washer.pos.ui.screens.neworder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.carpet.washer.pos.data.model.Carpet
import uz.carpet.washer.pos.data.model.Order
import uz.carpet.washer.pos.data.repository.OrderRepository
import java.util.concurrent.atomic.AtomicInteger

private val carpetIdGenerator = AtomicInteger(0)

// Gilam kiritish uchun UI steti
data class CarpetInput(
    val id: Int = carpetIdGenerator.incrementAndGet(),
    val type: String = "Gilam",
    val width: String = "",
    val length: String = "",
    val pricePerSqm: String = "15000"
) {
    val widthDouble get() = width.toDoubleOrNull() ?: 0.0
    val lengthDouble get() = length.toDoubleOrNull() ?: 0.0
    val priceDouble get() = pricePerSqm.toLongOrNull() ?: 0L
    val area get() = if (type == "Gilam" || lengthDouble > 0) widthDouble * lengthDouble else widthDouble
    val total get() = (area * priceDouble).toLong()
}

// UI steti — isEditMode qo'shildi
data class NewOrderUiState(
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val carpets: List<CarpetInput> = listOf(CarpetInput()),
    val advanceAmount: String = "0",
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,      // Tahrirlash rejimi
    val editingOrderId: Long? = null,     // Tahrirlanayotgan buyurtma IDsi
    val savedOrderId: Long? = null,
    val error: String? = null
) {
    val totalArea get() = carpets.sumOf { it.area }
    val totalAmount get() = carpets.sumOf { it.total }
    val advance get() = advanceAmount.toLongOrNull() ?: 0L
    val remaining get() = totalAmount - advance

    val isValid get() = customerName.isNotBlank() 
            && customerPhone.trim().startsWith("+998")
            && customerPhone.filter { it.isDigit() }.length == 12
            && carpets.all { it.widthDouble > 0 && it.lengthDouble > 0 && it.priceDouble > 0 }
}

class NewOrderViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repo = OrderRepository(application)

    // Navigation orqali kelgan orderId (tahrirlash uchun, ixtiyoriy)
    private val editOrderId: Long = savedStateHandle.get<Long>("editOrderId") ?: 0L

    private val _uiState = MutableStateFlow(NewOrderUiState())
    val uiState: StateFlow<NewOrderUiState> = _uiState.asStateFlow()

    init {
        // editOrderId mavjud bo'lsa — mavjud buyurtmani yuklash
        if (editOrderId > 0L) {
            loadExistingOrder(editOrderId)
        }
    }

    /**
     * Mavjud buyurtma ma'lumotlarini formaga yuklash (Tahrirlash rejimi)
     */
    private fun loadExistingOrder(orderId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val data = repo.getOrderWithCarpets(orderId)
                if (data != null) {
                    val carpetInputs = data.carpets.map {
                        CarpetInput(
                            type = it.type,
                            width = it.width.toString(),
                            length = it.length.toString(),
                            pricePerSqm = it.pricePerSqm.toString()
                        )
                    }.ifEmpty { listOf(CarpetInput()) }

                    _uiState.update {
                        it.copy(
                            customerName = data.order.customerName,
                            customerPhone = data.order.customerPhone,
                            customerAddress = data.order.customerAddress,
                            carpets = carpetInputs,
                            advanceAmount = data.order.advanceAmount.toString(),
                            isEditMode = true,
                            editingOrderId = orderId,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "Buyurtma topilmadi", isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Yuklashda xatolik: ${e.message}", isLoading = false) }
            }
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(customerName = v) }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(customerPhone = v) }
    fun onAddressChange(v: String) = _uiState.update { it.copy(customerAddress = v) }
    fun onAdvanceChange(v: String) = _uiState.update { it.copy(advanceAmount = v) }

    fun updateCarpet(id: Int, carpet: CarpetInput) {
        _uiState.update { state ->
            state.copy(carpets = state.carpets.map { if (it.id == id) carpet else it })
        }
    }

    fun addCarpet() {
        _uiState.update { it.copy(carpets = it.carpets + CarpetInput()) }
    }

    fun removeCarpet(id: Int) {
        _uiState.update { state ->
            if (state.carpets.size > 1)
                state.copy(carpets = state.carpets.filter { it.id != id })
            else state
        }
    }

    /**
     * Saqlash: yangi buyurtma (saveOrder) yoki mavjudni yangilash (updateOrder)
     */
    fun saveOrder() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(error = "Barcha maydonlarni to'g'ri to'ldiring") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val carpets = state.carpets.map {
                    Carpet(
                        orderId = 0, // Repository saqlashda o'zi ID beradi
                        type = it.type,
                        width = it.widthDouble,
                        length = it.lengthDouble,
                        area = it.area,
                        pricePerSqm = it.priceDouble,
                        totalPrice = it.total
                    )
                }

                if (state.isEditMode && state.editingOrderId != null) {
                    // === TAHRIRLASH ===
                    val existing = repo.getOrderWithCarpets(state.editingOrderId)
                    if (existing == null) {
                        _uiState.update { it.copy(error = "Buyurtma topilmadi, tahrirlash bekor qilindi", isLoading = false) }
                        return@launch
                    }

                    val updatedOrder = existing.order.copy(
                        customerName = state.customerName.trim(),
                        customerPhone = state.customerPhone.trim(),
                        customerAddress = state.customerAddress.trim(),
                        totalArea = state.totalArea,
                        totalAmount = state.totalAmount,
                        advanceAmount = state.advance,
                        remainingAmount = state.remaining
                    )
                    repo.updateOrder(updatedOrder, carpets)
                    _uiState.update { it.copy(savedOrderId = state.editingOrderId, isLoading = false) }
                } else {
                    // === YANGI BUYURTMA ===
                    val order = Order(
                        customerName = state.customerName.trim(),
                        customerPhone = state.customerPhone.trim(),
                        customerAddress = state.customerAddress.trim(),
                        totalArea = state.totalArea,
                        totalAmount = state.totalAmount,
                        advanceAmount = state.advance,
                        remainingAmount = state.remaining
                    )
                    val orderId = repo.saveOrder(order, carpets)
                    _uiState.update { it.copy(savedOrderId = orderId, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Saqlashda xatolik: ${e.message}", isLoading = false) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
