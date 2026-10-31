package com.personal.wallet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

// ───────────────────────────── بازه‌های زمانی ─────────────────────────────

enum class Period(val short: String, val days: Int, val label: String) {
    D7("7D", 7, "۷ روز گذشته"),
    D30("30D", 30, "۳۰ روز گذشته"),
    W12("12W", 84, "۱۲ هفته گذشته"),
    M6("6M", 182, "۶ ماه گذشته"),
    Y1("1Y", 365, "۱ سال گذشته")
}

/** شروع روزِ «daysAgo» روز قبل (۰ = امروز) */
fun dayStart(daysAgo: Int, now: Long = System.currentTimeMillis()): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = now
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    c.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return c.timeInMillis
}

// ───────────────────────────── اثر تراکنش روی مجموعه‌ای از حساب‌ها ─────────────────────────────

fun recordDelta(rec: RecordEntity, ids: Set<Int>): Long {
    return when (rec.type) {
        "INCOME" -> if (rec.accountId in ids) rec.amount else 0L
        "EXPENSE" -> if (rec.accountId in ids) -rec.amount else 0L
        else -> {
            var d = 0L
            if (rec.accountId in ids) d -= rec.amount
            val t = rec.targetAccountId
            if (t != null && t in ids) d += rec.amount
            d
        }
    }
}

fun recordTouches(rec: RecordEntity, ids: Set<Int>): Boolean {
    if (rec.accountId in ids) return true
    val t = rec.targetAccountId
    return t != null && t in ids
}

/** موجودی پایان هر روز در «days» روز اخیر (آخرین نقطه = موجودی همین الان) */
fun balanceSeries(
    currentBalance: Long,
    records: List<RecordEntity>,
    ids: Set<Int>,
    days: Int,
    now: Long = System.currentTimeMillis()
): List<Long> {
    val relevant = records.filter { recordDelta(it, ids) != 0L }
    val out = ArrayList<Long>(days)
    for (p in 0 until days) {
        val daysAgo = days - 1 - p
        val boundary = if (daysAgo == 0) now else dayStart(daysAgo - 1, now)
        var b = currentBalance
        for (r in relevant) {
            if (r.timestamp > boundary) b -= recordDelta(r, ids)
        }
        out.add(b)
    }
    return out
}

// ───────────────────────────── داشبورد صفحه اصلی ─────────────────────────────

data class DashboardData(
    val balance: Long,
    val balanceFraction: Float,
    val cashflow: Long,
    val cashFraction: Float,
    val income: Long,
    val expense: Long,
    val topCategories: List<Pair<String, Long>>
)

fun computeDashboard(
    accounts: List<AccountEntity>,
    records: List<RecordEntity>,
    ids: Set<Int>,
    period: Period,
    now: Long = System.currentTimeMillis()
): DashboardData {
    val balance = accounts.filter { it.id in ids }.sumOf { it.balance }
    val series = balanceSeries(balance, records, ids, period.days, now)
    val start = series.firstOrNull() ?: balance
    val change = if (start == 0L) 0.0 else (balance - start).toDouble() / abs(start.toDouble())
    val balanceFraction = (0.5 + change.coerceIn(-0.5, 0.5)).toFloat()

    val from = dayStart(period.days - 1, now)
    val inPeriod = records.filter { it.timestamp >= from && it.accountId in ids }
    val income = inPeriod.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expense = inPeriod.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val net = income - expense
    val total = income + expense
    val cashFraction = if (total > 0L) (0.5 + 0.5 * net.toDouble() / total.toDouble()).toFloat() else 0.5f

    val top = inPeriod.filter { it.type == "EXPENSE" }
        .groupBy { it.categoryName }
        .map { (k, v) -> k to v.sumOf { it.amount } }
        .sortedByDescending { it.second }
        .take(5)

    return DashboardData(balance, balanceFraction, net, cashFraction, income, expense, top)
}

// ───────────────────────────── ماشین‌حساب ─────────────────────────────

fun evaluateExpression(raw: String): Double {
    val s = raw.trim().trimEnd('+', '-', '*', '/', '×', '÷', '.')
    if (s.isEmpty()) return 0.0
    val nums = ArrayList<Double>()
    val ops = ArrayList<Char>()
    var cur = StringBuilder()
    for (ch in s) {
        when (ch) {
            '+', '-', '*', '/', '×', '÷' -> {
                nums.add(cur.toString().toDoubleOrNull() ?: 0.0)
                cur = StringBuilder()
                ops.add(
                    when (ch) {
                        '×' -> '*'
                        '÷' -> '/'
                        else -> ch
                    }
                )
            }
            else -> cur.append(ch)
        }
    }
    nums.add(cur.toString().toDoubleOrNull() ?: 0.0)

    val n2 = ArrayList<Double>()
    val o2 = ArrayList<Char>()
    n2.add(nums[0])
    for (i in ops.indices) {
        val op = ops[i]
        val next = nums[i + 1]
        if (op == '*') {
            n2[n2.lastIndex] = n2[n2.lastIndex] * next
        } else if (op == '/') {
            if (next != 0.0) n2[n2.lastIndex] = n2[n2.lastIndex] / next
        } else {
            o2.add(op)
            n2.add(next)
        }
    }
    var r = n2[0]
    for (i in o2.indices) {
        r = if (o2[i] == '+') r + n2[i + 1] else r - n2[i + 1]
    }
    return r
}

fun formatResult(v: Double): String {
    val rounded = Math.round(v)
    return if (rounded < 0) "0" else rounded.toString()
}

/** نمایش خوانا: جداکننده هزارگان و علامت‌های ریاضی */
fun prettyExpression(e: String): String {
    val grouped = Regex("(\\d+)(\\.\\d*)?").replace(e) { m ->
        val intPart = m.groupValues[1].toLongOrNull()
        val frac = m.groupValues[2]
        val g = if (intPart != null) NumberFormat.getNumberInstance(Locale.US).format(intPart) else m.groupValues[1]
        g + frac
    }
    return grouped.replace("*", "×").replace("/", "÷")
}

// ───────────────────────────── پیش‌نویس تراکنش (برای افزودن/ویرایش) ─────────────────────────────

class RecordDraft(
    val editing: RecordEntity?,
    type: String,
    accountId: Int,
    targetAccountId: Int?,
    categoryName: String,
    amount: Long
) {
    var type by mutableStateOf(type)
    var accountId by mutableStateOf(accountId)
    var targetAccountId by mutableStateOf(targetAccountId)
    var categoryName by mutableStateOf(categoryName)
    var expression by mutableStateOf(if (amount > 0L) amount.toString() else "0")
    var note by mutableStateOf("")
    var payee by mutableStateOf("")
    var labels by mutableStateOf<List<String>>(emptyList())
    var paymentType by mutableStateOf("Cash")
    var status by mutableStateOf("Cleared")
    var warranty by mutableStateOf("None")
    var place by mutableStateOf("")
    var timestamp by mutableStateOf(System.currentTimeMillis())

    fun amountValue(): Long = Math.round(evaluateExpression(expression)).coerceAtLeast(0L)

    fun toEntity(): RecordEntity = RecordEntity(
        id = editing?.id ?: 0,
        amount = amountValue(),
        type = type,
        accountId = accountId,
        targetAccountId = if (type == "TRANSFER") targetAccountId else null,
        categoryName = if (type == "TRANSFER") TRANSFER_CATEGORY else categoryName,
        note = note.trim(),
        timestamp = timestamp,
        payee = payee.trim(),
        labels = labels.joinToString(","),
        paymentType = paymentType,
        status = status,
        warranty = warranty,
        place = place.trim()
    )
}

const val TRANSFER_CATEGORY = "انتقال بین‌بانکی"

fun draftFromRecord(rec: RecordEntity): RecordDraft {
    val d = RecordDraft(rec, rec.type, rec.accountId, rec.targetAccountId, rec.categoryName, rec.amount)
    d.note = rec.note
    d.payee = rec.payee
    d.labels = rec.labels.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    d.paymentType = rec.paymentType
    d.status = rec.status
    d.warranty = rec.warranty
    d.place = rec.place
    d.timestamp = rec.timestamp
    return d
}

fun draftFromTemplate(t: TemplateEntity): RecordDraft {
    val d = RecordDraft(null, t.type, t.accountId, t.targetAccountId, t.categoryName, t.amount)
    d.note = t.note
    return d
}

fun newDraft(
    type: String,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    preferredAccountId: Int?
): RecordDraft {
    val from = accounts.firstOrNull { it.id == preferredAccountId } ?: accounts.firstOrNull()
    val target = accounts.firstOrNull { it.id != from?.id }
    return RecordDraft(
        editing = null,
        type = type,
        accountId = from?.id ?: 0,
        targetAccountId = target?.id,
        categoryName = categories.firstOrNull { it.parentId == null }?.name ?: "عمومی",
        amount = 0L
    )
}

fun TemplateEntity.summary(accounts: List<AccountEntity>): String {
    val acc = accounts.firstOrNull { it.id == accountId }?.name ?: ""
    return "$categoryName • $acc • ${formatIrr(amount)}"
}

// ───────────────────────────── خروجی CSV ─────────────────────────────

fun buildCsv(records: List<RecordEntity>, accounts: List<AccountEntity>): String {
    fun esc(s: String): String = "\"" + s.replace("\"", "\"\"") + "\""
    val sb = StringBuilder()
    sb.append("date,time,type,account,target_account,category,amount,note,payee,labels,payment_type,status,place\n")
    for (r in records.sortedBy { it.timestamp }) {
        val acc = accounts.firstOrNull { it.id == r.accountId }?.name ?: ""
        val tgt = accounts.firstOrNull { it.id == r.targetAccountId }?.name ?: ""
        sb.append(
            listOf(
                formatDate(r.timestamp), formatTime(r.timestamp), r.type, acc, tgt, r.categoryName,
                r.amount.toString(), r.note, r.payee, r.labels, r.paymentType, r.status, r.place
            ).joinToString(",") { esc(it) }
        ).append("\n")
    }
    return sb.toString()
}
