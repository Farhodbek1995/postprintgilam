package uz.carpet.washer.pos.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import uz.carpet.washer.pos.data.db.AppDatabase
import uz.carpet.washer.pos.data.db.CarpetDao
import uz.carpet.washer.pos.data.db.DailyStats
import uz.carpet.washer.pos.data.db.OrderDao
import uz.carpet.washer.pos.data.model.Carpet
import uz.carpet.washer.pos.data.model.Order
import uz.carpet.washer.pos.data.model.OrderWithCarpets
import java.io.File

class OrderRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val orderDao: OrderDao = db.orderDao()
    private val carpetDao: CarpetDao = db.carpetDao()
    private val gson = Gson()

    // Barcha buyurtmalar (Flow - real vaqtda yangilanib turadi)
    fun getAllOrders(): Flow<List<Order>> = orderDao.getAllOrders()

    fun searchOrders(query: String): Flow<List<Order>> = orderDao.searchOrders(query)

    fun getOrdersByDateRange(start: Long, end: Long): Flow<List<Order>> =
        orderDao.getOrdersByDateRange(start, end)

    fun getPendingDeliveryOrders(): Flow<List<Order>> = orderDao.getPendingDeliveryOrders()
    fun getFullyPaidOrders(): Flow<List<Order>> = orderDao.getFullyPaidOrders()
    fun getTodayIncome(todayStart: Long): Flow<Long> = orderDao.getTodayIncome(todayStart)
    fun getTodayOrderCount(todayStart: Long): Flow<Int> = orderDao.getTodayOrderCount(todayStart)
    fun getWeeklyStats(weekStart: Long): Flow<List<DailyStats>> = orderDao.getWeeklyStats(weekStart)

    // Buyurtma + uning gilamlari
    suspend fun getOrderWithCarpets(orderId: Long): OrderWithCarpets? {
        val order = orderDao.getOrderById(orderId) ?: return null
        val carpets = carpetDao.getCarpetsByOrderId(orderId)
        return OrderWithCarpets(order, carpets)
    }

    // Yangi buyurtma saqlash
    suspend fun saveOrder(order: Order, carpets: List<Carpet>): Long {
        val orderId = orderDao.insertOrder(order)
        val carpetsWithId = carpets.map { it.copy(orderId = orderId) }
        carpetDao.insertCarpets(carpetsWithId)
        return orderId
    }

    // Tahrirlash
    suspend fun updateOrder(order: Order, carpets: List<Carpet>) {
        orderDao.updateOrder(order)
        carpetDao.deleteCarpetsByOrderId(order.id)
        carpetDao.insertCarpets(carpets.map { it.copy(orderId = order.id) })
    }

    // Arxivlash (Soft Delete)
    suspend fun archiveOrder(orderId: Long) = orderDao.softDeleteOrder(orderId)

    // Tiklash
    suspend fun restoreOrder(orderId: Long) = orderDao.restoreOrder(orderId)

    // ===== JSON EXPORT =====
    suspend fun exportToJson(): File {
        val orders = orderDao.getAllOrdersForExport()
        val exportData = orders.map { order ->
            val carpets = carpetDao.getCarpetsByOrderId(order.id)
            OrderWithCarpets(order, carpets)
        }
        val json = gson.toJson(exportData)
        val file = File(context.getExternalFilesDir(null), "carpet_pos_backup_${System.currentTimeMillis()}.json")
        file.writeText(json)
        return file
    }

    // ===== JSON IMPORT =====
    suspend fun importFromJson(jsonContent: String): Int {
        val type = object : TypeToken<List<OrderWithCarpets>>() {}.type
        val importedData: List<OrderWithCarpets> = gson.fromJson(jsonContent, type)
        var count = 0
        for (data in importedData) {
            val newOrderId = orderDao.insertOrder(data.order.copy(id = 0))
            carpetDao.insertCarpets(data.carpets.map { it.copy(id = 0, orderId = newOrderId) })
            count++
        }
        return count
    }
}
