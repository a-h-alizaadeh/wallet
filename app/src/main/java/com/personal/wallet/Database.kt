package com.personal.wallet

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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
    val name: String
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
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface WalletDao {
    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAccounts(accounts: List<AccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("SELECT * FROM records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<RecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RecordEntity)
    
    @Delete
    suspend fun deleteRecord(record: RecordEntity)
}

@Database(entities = [AccountEntity::class, CategoryEntity::class, RecordEntity::class], version = 2, exportSchema = false)
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
                .fallbackToDestructiveMigration()
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
                    database.walletDao().insertAllCategories(
                        InitialPersonalData.defaultCategories.map { CategoryEntity(name = it) }
                    )
                }
            }
        }
    }
}

object InitialPersonalData {
    val defaultAccounts = listOf(
        AccountEntity(1, "ملت", "Cash", 6378813L, 0xFFD32F2F, 1),
        AccountEntity(2, "ملی", "Cash", 438479410L, 0xFFFBC02D, 2),
        AccountEntity(3, "تجارت", "Cash", 62093773L, 0xFF1976D2, 3)
    )

    val defaultCategories = listOf(
        "بهداشت دندان",
        "دندانپزشکی",
        "لوازم التحریر",
        "کتاب شخصی",
        "کتاب درسی ملیکا",
        "کتاب زبان",
        "خرج خونه",
        "حقوق",
        "کارت مترو",
        "پول نقد",
        "بسته اینترنت",
        "عمومی"
    )
}
