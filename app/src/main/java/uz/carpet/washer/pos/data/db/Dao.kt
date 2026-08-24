package uz.carpet.washer.pos.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import uz.carpet.washer.pos.data.model.Carpet
import uz.carpet.washer.pos.data.model.Order
import uz.carpet.washer.pos.data.model.OrderStatus

// ===== ORDER DAO =====
@Dao
interface OrderDao {

    // Barcha faol buyurtmalar (o'chirilmaganlar)
    @Query("""
        SELECT * FROM orders 
        WHERE deletedAt IS NULL 
        ORDER BY orderDate DESC
    """)
    fun getAllOrders(): Flow<List<Order>>

    // Qidiruv: ism yoki telefon bo'yicha
    @Query("""
        SELECT * FROM orders 
        WHERE deletedAt IS NULL AND (
            customerName LIKE '%' || :query || '%' OR
            customerPhone LIKE '%' || :query || '%' OR
            CAST(id AS TEXT) LIKE '%' || :query || '%'
        )
        ORDER BY orderDate DESC
    """)
    fun searchOrders(query: String): Flow<List<Order>>

    // Sana oralig'ida filter
    @Query("""
        SELECT * FROM orders 
        WHERE deletedAt IS NULL AND orderDate BETWEEN :startDate AND :endDate
        ORDER BY orderDate DESC
    """)
    fun getOrdersByDateRange(startDate: Long, endDate: Long): Flow<List<Order>>

    // Kechiktirilgan buyurtmalar (tayyor bo'lgan, topshirilmagan)
    @Query("""
        SELECT * FROM orders 
        WHERE deletedAt IS NULL AND status = 'READY'
        ORDER BY orderDate ASC
    """)
    fun getPendingDeliveryOrders(): Flow<List<Order>>

    // Qoldiq 0 bo'lganlar (to'liq to'langan)
    @Query("""
        SELECT * FROM orders 
        WHERE deletedAt IS NULL AND (remainingAmount = 0 OR isFullyPaid = 1)
        ORDER BY orderDate DESC
    """)
    fun getFullyPaidOrders(): Flow<List<Order>>

    // Arxivlangan buyurtmalar
    @Query("""
        SELECT * FROM orders 
        WHERE deletedAt IS NOT NULL
        ORDER BY orderDate DESC
    """)
    fun getArchivedOrders(): Flow<List<Order>>

    // Bitta buyurtmani olish
    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrderById(id: Long): Order?

    // Bugungi statistika
    @Query("""
        SELECT COALESCE(SUM(totalAmount), 0) FROM orders 
        WHERE deletedAt IS NULL AND orderDate >= :todayStart
    """)
    fun getTodayIncome(todayStart: Long): Flow<Long>

    @Query("""
        SELECT COUNT(*) FROM orders 
        WHERE deletedAt IS NULL AND orderDate >= :todayStart
    """)
    fun getTodayOrderCount(todayStart: Long): Flow<Int>

    // Haftalik statistika (grafik uchun)
    @Query("""
        SELECT 
            CAST(STRFTIME('%s', DATE(orderDate / 1000, 'unixepoch', 'localtime')) AS INTEGER) * 1000 AS orderDate, 
            SUM(totalAmount) as totalAmount 
        FROM orders 
        WHERE deletedAt IS NULL AND orderDate >= :weekStart
        GROUP BY DATE(orderDate / 1000, 'unixepoch', 'localtime')
        ORDER BY orderDate
    """)
    fun getWeeklyStats(weekStart: Long): Flow<List<DailyStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    // Soft delete - arxivlash (haqiqiy o'chirish emas)
    @Query("UPDATE orders SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteOrder(id: Long, timestamp: Long = System.currentTimeMillis())

    // Arxivdan tiklash
    @Query("UPDATE orders SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreOrder(id: Long)

    // Export uchun barcha ma'lumotlar (faqat faol)
    @Query("SELECT * FROM orders WHERE deletedAt IS NULL ORDER BY orderDate DESC")
    suspend fun getAllOrdersForExport(): List<Order>
}

// Haftalik statistika uchun yordamchi klass
data class DailyStats(
    val orderDate: Long,
    val totalAmount: Long
)

// ===== CARPET DAO =====
@Dao
interface CarpetDao {

    @Query("SELECT * FROM carpets WHERE orderId = :orderId")
    suspend fun getCarpetsByOrderId(orderId: Long): List<Carpet>

    @Query("SELECT * FROM carpets WHERE orderId = :orderId")
    fun getCarpetsByOrderIdFlow(orderId: Long): Flow<List<Carpet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarpet(carpet: Carpet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarpets(carpets: List<Carpet>)

    @Query("DELETE FROM carpets WHERE orderId = :orderId")
    suspend fun deleteCarpetsByOrderId(orderId: Long)
}
