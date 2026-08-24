package uz.carpet.washer.pos

import android.app.Application
import uz.carpet.washer.pos.printer.UsbPrinterManager

/**
 * Application darajasida yagona UsbPrinterManager saqlash.
 *
 * Nima uchun Application? Chunki:
 * - Dastur ishga tushganda bir marta yaratiladi, o'chirilganda yo'qoladi.
 * - Ekranlar almashinsa ham bir xil obyekt — printer holati baham ko'riladi.
 * - ViewModel lar o'chirilsa ham printer ulanishi saqlanadi.
 */
class CarpetPosApp : Application() {

    /**
     * lazy — birinchi marta so'ralganda yaratiladi (dastur ishga tushganda emas).
     * Bu Application context mavjud bo'lganda ishlatilishi kafolatlanadi.
     */
    val printerManager: UsbPrinterManager by lazy {
        UsbPrinterManager(applicationContext)
    }

    override fun onTerminate() {
        super.onTerminate()
        printerManager.disconnect()
    }
}
