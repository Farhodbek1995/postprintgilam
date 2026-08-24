package uz.carpet.washer.pos.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ===== BUYURTMA HOLATLARI =====
enum class OrderStatus(val label: String) {
    RECEIVED("Qabul qilingan"),
    WASHING("Yuvilmoqda"),
    READY("Tayyor"),
    DELIVERED("Topshirilgan")
}

// ===== BUYURTMA JADVALI =====
@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["orderDate"]),           // Sana bo'yicha tezkor qidiruv
        Index(value = ["customerPhone"])        // Telefon bo'yicha tezkor qidiruv
    ]
)
data class Order(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderDate: Long = System.currentTimeMillis(),  // Unix timestamp
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String = "",
    val totalArea: Double = 0.0,     // Jami m²
    val totalAmount: Long = 0,       // Jami summa (so'm)
    val advanceAmount: Long = 0,     // Avans (so'm)
    val remainingAmount: Long = 0,   // Qoldiq (so'm)
    val status: OrderStatus = OrderStatus.RECEIVED,
    val deletedAt: Long? = null      // Soft delete - arxivlash uchun
)

// ===== GILAM JADVALI =====
@Entity(
    tableName = "carpets",
    foreignKeys = [
        ForeignKey(
            entity = Order::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE   // Buyurtma o'chirilsa, gilami ham o'chadi
        )
    ],
    indices = [Index(value = ["orderId"])]
)
data class Carpet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: Long,
    val width: Double,          // Eni (m)
    val length: Double,         // Bo'yi (m)
    val area: Double,           // m² = eni × bo'yi
    val pricePerSqm: Long,      // 1 m² narxi (so'm)
    val totalPrice: Long        // Summa = area × pricePerSqm
)

// ===== BUYURTMA + GILAMLARI BIRGALIKDA (Room Relation) =====
data class OrderWithCarpets(
    val order: Order,
    val carpets: List<Carpet>
)
