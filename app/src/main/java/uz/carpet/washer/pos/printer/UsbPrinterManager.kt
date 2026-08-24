package uz.carpet.washer.pos.printer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.resume

private const val USB_PERMISSION_ACTION = "uz.carpet.washer.pos.USB_PERMISSION"
private const val TIMEOUT_MS = 10_000L  // 10 soniya timeout

// ===== PRINTER ULANISH HOLATI =====
sealed class PrinterState {
    object Disconnected : PrinterState()
    object Connecting : PrinterState()
    data class Connected(val deviceName: String) : PrinterState()
    data class Error(val message: String) : PrinterState()
}

// ===== CHOP ETISH NATIJASI =====
sealed class PrintResult {
    object Success : PrintResult()
    data class Failure(val reason: String) : PrintResult()
}

class UsbPrinterManager(private val context: Context) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _state = MutableStateFlow<PrinterState>(PrinterState.Disconnected)
    val state: StateFlow<PrinterState> = _state

    // Navbat: Thread-Safe Queue (har xil threadlardan bir vaqtda kirish uchun)
    private val printQueue = ConcurrentLinkedQueue<ByteArray>()

    @Volatile
    private var activeConnection: UsbPrinterConnection? = null

    // ===== USB QURILMALAR RO'YXATI =====
    fun getAvailableDevices(): List<UsbDevice> = usbManager.deviceList.values.toList()

    // ===== QURILMAGA ULANISH (10 sek timeout bilan) =====
    suspend fun connect(device: UsbDevice): Boolean {
        _state.value = PrinterState.Connecting

        val result = withTimeoutOrNull(TIMEOUT_MS) {
            requestPermissionAndConnect(device)
        }

        if (result == null || result == false) {
            _state.value = PrinterState.Error("Ulanishda xatolik: Timeout yoki ruxsat berilmadi")
            return false
        }
        return result
    }

    /**
     * USB ruxsat oqimi:
     *
     * 1. Ruxsat allaqachon berilgan bo'lsa — tryConnect() ni Dispatchers.IO da chaqiradi.
     *    Bu withTimeoutOrNull uchun haqiqiy suspend nuqtasi bo'ladi.
     *
     * 2. Ruxsat yo'q bo'lsa — suspendCancellableCoroutine orqali to'xtatiladi.
     *    BroadcastReceiver dinamik ro'yxatdan o'tadi. Javob kelgach, tryConnect() ni
     *    Dispatchers.IO da launch qiladi va natijani continuation.resume() orqali qaytaradi.
     *    invokeOnCancellation: timeout yoki cancel bo'lsa Receiver unregister qilinadi.
     */
    private suspend fun requestPermissionAndConnect(device: UsbDevice): Boolean {
        // Ruxsat allaqachon bor — withContext(IO) ga o'tkazish (suspend nuqtasi yaratish)
        // Shunda withTimeoutOrNull bu kodni BEKOR QILA OLADI.
        if (usbManager.hasPermission(device)) {
            return tryConnect(device)
        }

        // Ruxsat yo'q — suspendCancellableCoroutine orqali kutish
        return suspendCancellableCoroutine { continuation ->

            val permissionReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.action != USB_PERMISSION_ACTION) return

                    // Receiver'ni darhol unregister qilish (ikki marta chaqirilmasin)
                    try { context.unregisterReceiver(this) } catch (_: Exception) {}

                    val grantedDevice: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)

                    if (permissionGranted && grantedDevice != null) {
                        // tryConnect() ni Dispatchers.IO da ishlatish —
                        // continuation hali aktiv bo'lsa bajariladi
                        CoroutineScope(Dispatchers.IO).launch {
                            val connected = tryConnect(grantedDevice)
                            if (continuation.isActive) continuation.resume(connected)
                        }
                    } else {
                        _state.value = PrinterState.Error("USB ruxsat berilmadi")
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
            }

            val intentFilter = IntentFilter(USB_PERMISSION_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(permissionReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(permissionReceiver, intentFilter)
            }

            // Timeout yoki cancel — Receiver tozalanadi (memory leak yo'q)
            continuation.invokeOnCancellation {
                try { context.unregisterReceiver(permissionReceiver) } catch (_: Exception) {}
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                device.deviceId,
                Intent(USB_PERMISSION_ACTION),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            usbManager.requestPermission(device, pendingIntent)
        }
    }

    /**
     * Haqiqiy USB ulanish logikasi.
     * withContext(Dispatchers.IO) — bu funksiyani SUSPEND qiladi.
     * Shunday qilib withTimeoutOrNull uni timeout bo'lganda bekor qila oladi.
     */
    private suspend fun tryConnect(device: UsbDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = usbManager.openDevice(device) ?: run {
                _state.value = PrinterState.Error("Qurilmani ochib bo'lmadi")
                return@withContext false
            }

            val usbInterface = device.getInterface(0)
            if (!connection.claimInterface(usbInterface, true)) {
                connection.close()
                _state.value = PrinterState.Error("Interface ruxsati olinmadi")
                return@withContext false
            }

            // OUT endpoint topish (printerga ma'lumot yuborish uchun)
            var outEndpoint = usbInterface.getEndpoint(0)
            for (i in 0 until usbInterface.endpointCount) {
                val ep = usbInterface.getEndpoint(i)
                if (ep.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT) {
                    outEndpoint = ep
                    break
                }
            }

            activeConnection = UsbPrinterConnection(connection, usbInterface, outEndpoint)
            _state.value = PrinterState.Connected(device.productName ?: device.deviceName)

            // Navbatdagi cheklarni yuborish
            flushQueue()
            true  // withContext blokidan qaytarish

        } catch (e: Exception) {
            _state.value = PrinterState.Error("Xatolik: ${e.message}")
            false
        }
    }

    // ===== CHOP ETISH =====
    suspend fun print(data: ByteArray): PrintResult = withContext(Dispatchers.IO) {
        val conn = activeConnection
        if (conn != null) {
            try {
                // bulkTransfer bloklovchi — IO thread da bajarilishi shart
                val sent = conn.connection.bulkTransfer(conn.endpoint, data, data.size, 5000)
                if (sent >= 0) PrintResult.Success
                else {
                    printQueue.add(data)
                    PrintResult.Failure("Printer ma'lumot qabul qilmadi, navbatga qo'shildi")
                }
            } catch (e: Exception) {
                printQueue.add(data)
                PrintResult.Failure("Xatolik: ${e.message}")
            }
        } else {
            printQueue.add(data)
            PrintResult.Failure("Printer ulanmagan. Chek navbatga qo'shildi.")
        }
    }

    // Navbatdagi cheklarni yuborish (printer ulanganda)
    private suspend fun flushQueue() = withContext(Dispatchers.IO) {
        val conn = activeConnection ?: return@withContext
        while (true) {
            val data = printQueue.poll() ?: break // queue bo'sh bo'lsa to'xtaydi
            conn.connection.bulkTransfer(conn.endpoint, data, data.size, 3000)
        }
    }

    // ===== ULANISHNI UZISH =====
    fun disconnect() {
        activeConnection?.let {
            it.connection.releaseInterface(it.usbInterface)
            it.connection.close()
        }
        activeConnection = null
        _state.value = PrinterState.Disconnected
    }

    val queueSize: Int get() = printQueue.size
}

// Yordamchi data klass
data class UsbPrinterConnection(
    val connection: android.hardware.usb.UsbDeviceConnection,
    val usbInterface: android.hardware.usb.UsbInterface,
    val endpoint: android.hardware.usb.UsbEndpoint
)
