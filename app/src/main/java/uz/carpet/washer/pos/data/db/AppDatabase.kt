package uz.carpet.washer.pos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import uz.carpet.washer.pos.data.model.Carpet
import uz.carpet.washer.pos.data.model.Order
import uz.carpet.washer.pos.data.model.OrderStatus

// ===== OrderStatus uchun TypeConverter =====
class Converters {
    @TypeConverter
    fun fromOrderStatus(status: OrderStatus): String = status.name

    @TypeConverter
    fun toOrderStatus(name: String): OrderStatus =
        runCatching { OrderStatus.valueOf(name) }.getOrDefault(OrderStatus.RECEIVED)
}

// ===== MIGRATSIYALAR =====
// Qoida: har safar sxema o'zgarganda (version ++) yangi MIGRATION qo'shiladi.
// Eski ma'lumotlar HECH QACHON yo'qotilmaydi.
//
// Namuna — kelajakda qo'shiladi:
//
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE orders ADD COLUMN isFullyPaid INTEGER NOT NULL DEFAULT 0")
    }
}

// Barcha migratsiyalar ro'yxati
val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_1_2
)

// ===== Room Database =====
@Database(
    entities = [Order::class, Carpet::class],
    version = 2,
    exportSchema = true   // true — sxema JSON ga eksport qilinadi (migratsiya tarixini saqlaydi)
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun orderDao(): OrderDao
    abstract fun carpetDao(): CarpetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "carpet_pos.db"
                )
                    // fallbackToDestructiveMigration() O'CHIRILDI —
                    // endi sxema o'zgarganda ma'lumotlar yo'qolmaydi.
                    // Yangi migratsiya yozmay version ni oshirsangiz,
                    // Room IllegalStateException tashlaydi (bu to'g'ri xatti-harakat!).
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
