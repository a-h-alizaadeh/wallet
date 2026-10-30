package com.personal.wallet

import androidx.room.*

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String = "Cash",
    val balance: Long,
    val colorHex: Long,
    val sortOrder: Int = 0
)

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Long,
    val type: String,
    val accountName: String,
    val categoryName: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object InitialPersonalData {
    // حساب‌های شخصی شما
    val defaultAccounts = listOf(
        AccountEntity(1, "ملت", "Cash", 6378813L, 0xFFD32F2F, 1),
        AccountEntity(2, "ملی", "Cash", 438479410L, 0xFFFBC02D, 2),
        AccountEntity(3, "تجارت", "Cash", 62093773L, 0xFF1976D2, 3)
    )

    // برچسب‌های شما
    val defaultLabels = listOf(
        "بهداشت دندان", "دندانپزشکی", "لوازم التحریر", "کتاب شخصی",
        "کتاب درسی ملیکا", "کتاب زبان", "خرج خونه", "حقوق",
        "کارت مترو", "پول نقد", "بسته اینترنت"
    )
}
