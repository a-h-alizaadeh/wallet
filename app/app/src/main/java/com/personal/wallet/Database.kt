package com.personal.wallet

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// ───────────────────────────── Entities ─────────────────────────────

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String = "Cash",
    val balance: Long,
    val colorHex: Long,
    val sortOrder: Int = 0
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    @ColumnInfo(defaultValue = "4279060385") val colorHex: Long = 0xFF0D47A1,
    @ColumnInfo(defaultValue = "'category'") val iconKey: String = "category",
    val parentId: Int? = null
)

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Long,
    val type: String, // EXPENSE, INCOME, TRANSFER
    val accountId: Int,
    val targetAccountId: Int? = null,
    val categoryName: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val payee: String = "",
    @ColumnInfo(defaultValue = "''") val labels: String = "",
    @ColumnInfo(defaultValue = "'Cash'") val paymentType: String = "Cash",
    @ColumnInfo(defaultValue = "'Cleared'") val status: String = "Cleared",
    @ColumnInfo(defaultValue = "'None'") val warranty: String = "None",
    @ColumnInfo(defaultValue = "''") val place: String = ""
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String,
    val amount: Long,
    val accountId: Int,
    val targetAccountId: Int? = null,
    val categoryName: String,
    val note: String = ""
)

// ───────────────────────────── DAO ─────────────────────────────

@Dao
interface WalletDao {
    // accounts
    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccount(id: Int): AccountEntity?

    @Query("SELECT MAX(sortOrder) FROM accounts")
    suspend fun maxAccountSort(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAccounts(accounts: List<AccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    // categories
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE parentId = :parentId")
    suspend fun deleteChildCategories(parentId: Int)

    @Query("UPDATE categories SET parentId = NULL WHERE parentId = :parentId")
    suspend fun detachChildCategories(parentId: Int)

    @Query("UPDATE records SET categoryName = :newName WHERE categoryName = :oldName")
    suspend fun renameCategoryInRecords(oldName: String, newName: String)

    @Query("UPDATE templates SET categoryName = :newName WHERE categoryName = :oldName")
    suspend fun renameCategoryInTemplates(oldName: String, newName: String)

    // records
    @Query("SELECT * FROM records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE accountId = :accountId OR targetAccountId = :accountId")
    suspend fun getRecordsOfAccount(accountId: Int): List<RecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RecordEntity): Long

    @Update
    suspend fun updateRecord(record: RecordEntity)

    @Delete
    suspend fun deleteRecord(record: RecordEntity)

    @Query("DELETE FROM records WHERE accountId = :accountId OR targetAccountId = :accountId")
    suspend fun deleteRecordsOfAccount(accountId: Int)

    // templates
    @Query("SELECT * FROM templates ORDER BY id DESC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity): Long

    @Delete
    suspend fun deleteTemplate(template: TemplateEntity)
}

// ───────────────────────────── Database ─────────────────────────────

val CategoryPalette = listOf(
    0xFF0D47A1, 0xFF29B6F6, 0xFFFFA726, 0xFF78909C, 0xFFAD1457, 0xFF5C6BC0, 0xFF00BFA5, 0xFFFF6F00,
    0xFFFFB300, 0xFF6A1B9A, 0xFFD32F2F, 0xFF388E3C, 0xFF00838F, 0xFF8D6E63, 0xFF7CB342, 0xFFE91E63,
    0xFF3949AB, 0xFF546E7A
)

val AccountPalette = listOf(
    0xFFD32F2F, 0xFFFFA000, 0xFFFBC02D, 0xFF388E3C, 0xFF00897B, 0xFF0C469E,
    0xFF1976D2, 0xFF5E35B1, 0xFF8E24AA, 0xFFC2185B, 0xFF6D4C41, 0xFF546E7A
)

/**
 * مهاجرت ۲ → ۳: بدون پاک شدن هیچ داده‌ای.
 * ستون‌های جدید به دسته‌بندی‌ها و تراکنش‌ها اضافه می‌شود و جدول قالب‌ها ساخته می‌شود.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN colorHex INTEGER NOT NULL DEFAULT 4279060385")
        db.execSQL("ALTER TABLE categories ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'category'")
        db.execSQL("ALTER TABLE categories ADD COLUMN parentId INTEGER")

        db.execSQL("ALTER TABLE records ADD COLUMN payee TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE records ADD COLUMN labels TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE records ADD COLUMN paymentType TEXT NOT NULL DEFAULT 'Cash'")
        db.execSQL("ALTER TABLE records ADD COLUMN status TEXT NOT NULL DEFAULT 'Cleared'")
        db.execSQL("ALTER TABLE records ADD COLUMN warranty TEXT NOT NULL DEFAULT 'None'")
        db.execSQL("ALTER TABLE records ADD COLUMN place TEXT NOT NULL DEFAULT ''")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `templates` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `type` TEXT NOT NULL, `amount` INTEGER NOT NULL, " +
                "`accountId` INTEGER NOT NULL, `targetAccountId` INTEGER, " +
                "`categoryName` TEXT NOT NULL, `note` TEXT NOT NULL)"
        )

        // رنگ‌های متنوع برای دسته‌بندی‌های موجود
        val cases = CategoryPalette.take(8).mapIndexed { i, c -> "WHEN $i THEN $c" }.joinToString(" ")
        db.execSQL("UPDATE categories SET colorHex = CASE (id % 8) $cases ELSE 4279060385 END")

        // حدس آیکون بر اساس نام دسته‌بندی‌های قبلی
        db.execSQL("UPDATE categories SET iconKey = 'medical' WHERE name LIKE '%دندان%'")
        db.execSQL("UPDATE categories SET iconKey = 'book' WHERE name LIKE '%کتاب%'")
        db.execSQL("UPDATE categories SET iconKey = 'edit' WHERE name LIKE '%التحریر%'")
        db.execSQL("UPDATE categories SET iconKey = 'subway' WHERE name LIKE '%مترو%'")
        db.execSQL("UPDATE categories SET iconKey = 'wifi' WHERE name LIKE '%اینترنت%'")
        db.execSQL("UPDATE categories SET iconKey = 'payments' WHERE name LIKE '%حقوق%'")
        db.execSQL("UPDATE categories SET iconKey = 'home' WHERE name LIKE '%خونه%'")
        db.execSQL("UPDATE categories SET iconKey = 'atm' WHERE name LIKE '%نقد%'")
        db.execSQL("UPDATE categories SET iconKey = 'language' WHERE name LIKE '%زبان%'")
    }
}

@Database(
    entities = [AccountEntity::class, CategoryEntity::class, RecordEntity::class, TemplateEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wallet_secure_v2.db"
                )
                    // عمداً fallbackToDestructiveMigration حذف شده تا هیچ‌وقت داده‌های شما پاک نشود.
                    .addMigrations(MIGRATION_2_3)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    database.walletDao().insertAllAccounts(InitialPersonalData.defaultAccounts)
                    database.walletDao().insertAllCategories(InitialPersonalData.defaultCategories)
                }
            }
        }
    }
}

object InitialPersonalData {
    val defaultAccounts = listOf(
        AccountEntity(1, "ملت", "Cash", 6378813L, 0xFFD32F2F, 1),
        AccountEntity(2, "ملی", "Cash", 438479410L, 0xFFFFA000, 2),
        AccountEntity(3, "تجارت", "Cash", 62093773L, 0xFF0C469E, 3)
    )

    val defaultCategories = listOf(
        CategoryEntity(name = "بهداشت دندان", colorHex = 0xFF0D47A1, iconKey = "medical"),
        CategoryEntity(name = "دندانپزشکی", colorHex = 0xFF29B6F6, iconKey = "medical"),
        CategoryEntity(name = "لوازم التحریر", colorHex = 0xFFFFA726, iconKey = "edit"),
        CategoryEntity(name = "کتاب شخصی", colorHex = 0xFF78909C, iconKey = "book"),
        CategoryEntity(name = "کتاب درسی ملیکا", colorHex = 0xFFAD1457, iconKey = "school"),
        CategoryEntity(name = "کتاب زبان", colorHex = 0xFF5C6BC0, iconKey = "language"),
        CategoryEntity(name = "خرج خونه", colorHex = 0xFF00BFA5, iconKey = "home"),
        CategoryEntity(name = "حقوق", colorHex = 0xFF388E3C, iconKey = "payments"),
        CategoryEntity(name = "کارت مترو", colorHex = 0xFF6A1B9A, iconKey = "subway"),
        CategoryEntity(name = "پول نقد", colorHex = 0xFFFF6F00, iconKey = "atm"),
        CategoryEntity(name = "بسته اینترنت", colorHex = 0xFF00838F, iconKey = "wifi"),
        CategoryEntity(name = "عمومی", colorHex = 0xFF78909C, iconKey = "category")
    )
}
