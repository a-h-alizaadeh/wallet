package com.personal.wallet

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

/**
 * تمام عملیات‌هایی که روی موجودی حساب‌ها اثر می‌گذارند اینجا و داخل یک تراکنش دیتابیس انجام می‌شوند
 * تا موجودی حساب‌ها و لیست تراکنش‌ها هیچ‌وقت ناسازگار نشوند.
 */
class WalletRepository(private val db: AppDatabase) {
    private val dao = db.walletDao()

    val accounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
    val categories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val records: Flow<List<RecordEntity>> = dao.getAllRecords()
    val templates: Flow<List<TemplateEntity>> = dao.getAllTemplates()

    // ───── تراکنش‌ها ─────

    /** [old] != null یعنی ویرایش تراکنش: اثر قبلی برگردانده می‌شود و اثر جدید اعمال می‌شود. */
    suspend fun saveRecord(newRec: RecordEntity, old: RecordEntity?) {
        db.withTransaction {
            if (old != null) {
                applyEffect(old, -1)
                applyEffect(newRec, 1)
                dao.updateRecord(newRec)
            } else {
                applyEffect(newRec, 1)
                dao.insertRecord(newRec)
            }
        }
    }

    suspend fun deleteRecord(rec: RecordEntity) {
        db.withTransaction {
            applyEffect(rec, -1)
            dao.deleteRecord(rec)
        }
    }

    private suspend fun applyEffect(rec: RecordEntity, direction: Int) {
        val amount = rec.amount * direction
        when (rec.type) {
            "EXPENSE" -> adjust(rec.accountId, -amount)
            "INCOME" -> adjust(rec.accountId, amount)
            "TRANSFER" -> {
                adjust(rec.accountId, -amount)
                val target = rec.targetAccountId
                if (target != null) adjust(target, amount)
            }
        }
    }

    private suspend fun adjust(accountId: Int, delta: Long) {
        val acc = dao.getAccount(accountId) ?: return
        dao.updateAccount(acc.copy(balance = acc.balance + delta))
    }

    // ───── حساب‌ها ─────

    suspend fun saveAccount(account: AccountEntity) {
        if (account.id == 0) {
            val next = (dao.maxAccountSort() ?: 0) + 1
            dao.insertAccount(account.copy(sortOrder = next))
        } else {
            dao.updateAccount(account)
        }
    }

    suspend fun setBalance(accountId: Int, balance: Long) {
        val acc = dao.getAccount(accountId) ?: return
        dao.updateAccount(acc.copy(balance = balance))
    }

    /** حذف حساب همراه با تمام تراکنش‌هایش؛ اثر انتقال‌ها روی حساب مقابل هم برگردانده می‌شود. */
    suspend fun deleteAccount(account: AccountEntity) {
        db.withTransaction {
            val related = dao.getRecordsOfAccount(account.id)
            related.filter { it.type == "TRANSFER" }.forEach { applyEffect(it, -1) }
            dao.deleteRecordsOfAccount(account.id)
            dao.deleteAccount(account)
        }
    }

    // ───── دسته‌بندی‌ها ─────

    suspend fun saveCategory(category: CategoryEntity, oldName: String?) {
        db.withTransaction {
            if (category.id == 0) {
                dao.insertCategory(category)
            } else {
                dao.updateCategory(category)
                if (oldName != null && oldName != category.name) {
                    dao.renameCategoryInRecords(oldName, category.name)
                    dao.renameCategoryInTemplates(oldName, category.name)
                }
            }
        }
    }

    /** تراکنش‌های قبلی همان نام دسته‌بندی را نگه می‌دارند (فقط آیکون پیش‌فرض می‌گیرند). */
    suspend fun deleteCategory(category: CategoryEntity) {
        db.withTransaction {
            dao.deleteChildCategories(category.id)
            dao.deleteCategory(category)
        }
    }

    // ───── قالب‌ها ─────

    suspend fun saveTemplate(t: TemplateEntity) { dao.insertTemplate(t) }
    suspend fun deleteTemplate(t: TemplateEntity) { dao.deleteTemplate(t) }
}
