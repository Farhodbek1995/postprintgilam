package uz.carpet.washer.pos.ui.screens.printer

import android.app.Application
import android.hardware.usb.UsbDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uz.carpet.washer.pos.CarpetPosApp
import uz.carpet.washer.pos.printer.EscPosHelper
import uz.carpet.washer.pos.printer.PrintResult
import uz.carpet.washer.pos.printer.PrinterState
import uz.carpet.washer.pos.printer.UsbPrinterManager

class PrinterViewModel(application: Application) : AndroidViewModel(application) {

    // Application darajasidagi YAGONA UsbPrinterManager instance
    // Ekranlar almashinsa ham shu obyekt — printer holati baham ko'riladi
    private val printerManager: UsbPrinterManager =
        (application as CarpetPosApp).printerManager

    val printerState: StateFlow<PrinterState> = printerManager.state

    // Test print natijasini ekranga uzatish uchun
    private val _printMessage = MutableStateFlow<String?>(null)
    val printMessage: StateFlow<String?> = _printMessage

    fun getAvailableDevices(): List<UsbDevice> = printerManager.getAvailableDevices()

    fun connect(device: UsbDevice) {
        viewModelScope.launch {
            printerManager.connect(device)
        }
    }

    fun disconnect() = printerManager.disconnect()

    fun sendTestPrint() {
        viewModelScope.launch {
            val data = EscPosHelper.buildTestReceipt()
            _printMessage.value = when (val result = printerManager.print(data)) {
                is PrintResult.Success -> "✅ Test chopi muvaffaqiyatli!"
                is PrintResult.Failure -> "❌ ${result.reason}"
            }
        }
    }

    fun clearPrintMessage() { _printMessage.value = null }

    // ViewModel o'chirilganda printer ulanishini UZMAYMIZ —
    // singleton saqlanib, boshqa ekranlar ham ishlatadi
    override fun onCleared() {
        super.onCleared()
        // disconnect() CHAQIRILMAYDI — Application.onTerminate() da yopiladi
    }
}
