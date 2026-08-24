package uz.carpet.washer.pos.printer

import uz.carpet.washer.pos.data.model.Carpet
import uz.carpet.washer.pos.data.model.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ESC/POS komandalar yordamida chek matnini bayt massiviga aylantiradigan klass.
 * E200L printer 58mm yoki 80mm qog'oz bilan ishlaydi.
 * 80mm uchun bir qatorda ~48 belgi sig'adi.
 */
object EscPosHelper {

    private const val LINE_WIDTH = 48 // 80mm qog'oz uchun

    // ESC/POS komandalar
    private val ESC = byteArrayOf(0x1B)
    private val GS = byteArrayOf(0x1D)

    val INIT = byteArrayOf(0x1B, 0x40)                    // Printerni qayta boshlash
    val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)     // Markazlashtirish
    val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)       // Chapga
    val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)          // Qalin yozuv yoqish
    val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)         // Qalin yozuv o'chirish
    val FONT_LARGE = byteArrayOf(0x1D, 0x21, 0x11)       // Katta shrift (2x)
    val FONT_NORMAL = byteArrayOf(0x1D, 0x21, 0x00)      // Normal shrift
    val FEED_LINE = byteArrayOf(0x0A)                    // Yangi qator
    val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x41, 0x05) // Qog'ozni kesish

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    // ===== BUYURTMA CHEKI =====
    fun buildOrderReceipt(order: Order, carpets: List<Carpet>): ByteArray {
        val buffer = mutableListOf<Byte>()

        fun append(bytes: ByteArray) = buffer.addAll(bytes.toList())
        fun appendText(text: String) = append(text.toByteArray(Charsets.UTF_8))
        fun newLine() = append(FEED_LINE)
        fun separator() { appendText("-".repeat(LINE_WIDTH)); newLine() }

        // Boshlash
        append(INIT)

        // Sarlavha (markazda, katta, qalin)
        append(ALIGN_CENTER)
        append(BOLD_ON)
        append(FONT_LARGE)
        appendText("GILAM YUVISH"); newLine()
        append(FONT_NORMAL)
        appendText("BUYURTMA №%06d".format(order.id)); newLine()
        append(BOLD_OFF)
        newLine()

        // Sana va vaqt (chapga)
        append(ALIGN_LEFT)
        separator()
        appendText("Sana: ${dateFormat.format(Date(order.orderDate))}"); newLine()
        newLine()

        // Mijoz ma'lumotlari
        append(BOLD_ON)
        appendText("Mijoz: "); append(BOLD_OFF)
        appendText(order.customerName); newLine()

        append(BOLD_ON)
        appendText("Tel: "); append(BOLD_OFF)
        appendText(order.customerPhone); newLine()

        if (order.customerAddress.isNotBlank()) {
            append(BOLD_ON)
            appendText("Manzil: "); append(BOLD_OFF)
            appendText(order.customerAddress); newLine()
        }
        newLine()

        // Gilamlar
        separator()
        for ((index, carpet) in carpets.withIndex()) {
            if (carpets.size > 1) {
                append(BOLD_ON)
                appendText("Gilam ${index + 1}:"); append(BOLD_OFF)
                newLine()
            } else {
                append(BOLD_ON)
                appendText("Gilam:"); append(BOLD_OFF)
                newLine()
            }
            appendText("${"%.2f".format(carpet.width)} x ${"%.2f".format(carpet.length)} = ${"%.2f".format(carpet.area)} m2")
            newLine()
            appendText("Narx: ${formatMoney(carpet.pricePerSqm)} so'm/m2"); newLine()
            appendText(formatRow("Summa:", "${formatMoney(carpet.totalPrice)} so'm")); newLine()
            newLine()
        }

        // Jami
        separator()
        append(BOLD_ON)
        appendText(formatRow("JAMI:", "${formatMoney(order.totalAmount)} so'm")); newLine()
        appendText(formatRow("AVANS:", "${formatMoney(order.advanceAmount)} so'm")); newLine()
        appendText(formatRow("QOLDIQ:", "${formatMoney(order.remainingAmount)} so'm")); newLine()
        append(BOLD_OFF)
        separator()

        // Pastki qism
        newLine()
        append(ALIGN_CENTER)
        appendText("Rahmat!"); newLine()
        newLine()
        newLine()
        newLine()

        // Qog'ozni kesish
        append(CUT_PAPER)

        return buffer.toByteArray()
    }

    // ===== TEST CHEKI =====
    fun buildTestReceipt(): ByteArray {
        val buffer = mutableListOf<Byte>()
        fun append(bytes: ByteArray) = buffer.addAll(bytes.toList())
        fun appendText(text: String) = append(text.toByteArray(Charsets.UTF_8))
        fun newLine() = append(FEED_LINE)

        append(INIT)
        append(ALIGN_CENTER)
        append(BOLD_ON)
        append(FONT_LARGE)
        appendText("TEST PRINT"); newLine()
        append(FONT_NORMAL)
        append(BOLD_OFF)
        appendText("-".repeat(LINE_WIDTH)); newLine()
        appendText("E200L Printer ulandi!"); newLine()
        appendText("Gilam Yuvish POS v1.0"); newLine()
        appendText("-".repeat(LINE_WIDTH)); newLine()
        newLine()
        newLine()
        append(CUT_PAPER)

        return buffer.toByteArray()
    }

    // Pul miqdorini formatlash: 90000 -> "90 000"
    private fun formatMoney(amount: Long): String {
        return String.format("%,d", amount).replace(",", " ")
    }

    // "Ism:           90 000 so'm" - chapda va o'ngda
    private fun formatRow(left: String, right: String): String {
        val spaces = LINE_WIDTH - left.length - right.length
        return if (spaces > 0) "$left${" ".repeat(spaces)}$right" else "$left $right"
    }
}
