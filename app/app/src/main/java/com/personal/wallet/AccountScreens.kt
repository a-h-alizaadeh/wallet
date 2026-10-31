package com.personal.wallet

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// ───────────────────────────── جزئیات حساب ─────────────────────────────

@Composable
fun AccountDetailScreen(
    account: AccountEntity,
    records: List<RecordEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    templates: List<TemplateEntity>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSetBalance: (Long) -> Unit,
    onOpenRecord: (RecordEntity) -> Unit,
    onAllRecords: () -> Unit,
    onNewRecord: () -> Unit,
    onTransfer: () -> Unit,
    onTemplate: (TemplateEntity) -> Unit
) {
    var showBalanceDialog by remember { mutableStateOf(false) }
    val ids = remember(account.id) { setOf(account.id) }
    val series = remember(account, records) { balanceSeries(account.balance, records, ids, 30) }
    val start = series.firstOrNull() ?: account.balance
    val pct = if (start == 0L) 0.0 else (account.balance - start).toDouble() / abs(start.toDouble()) * 100.0
    val mine = remember(records, account.id) { records.filter { recordTouches(it, ids) }.take(10) }

    Box(Modifier.fillMaxSize().background(WColors.Page)) {
        Column(Modifier.fillMaxSize()) {
            WalletAppBar(
                title = "جزئیات حساب",
                navIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavClick = onBack,
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, null, tint = Color.White) }
                }
            )
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                item {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircleIcon(
                                icon = accountIcon(account.type),
                                color = Color(account.colorHex),
                                size = 60.dp,
                                iconSize = 34.dp,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(Modifier.width(24.dp))
                            Column {
                                Text(account.name, color = Color.White, fontSize = 16.sp)
                                Text(labelOf(AccountTypes, account.type), color = WColors.Muted, fontSize = 14.sp)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                            Column(Modifier.weight(1f)) {
                                Text("امروز", color = WColors.Muted, fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(formatIrr(account.balance), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color(0xFF2A3A5C))
                                            .clickable { showBalanceDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Edit, null, tint = WColors.Blue, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("۳۰ روز اخیر", color = WColors.Muted, fontSize = 12.sp)
                                val pctText = (if (pct > 0) "+" else "") + String.format(java.util.Locale.US, "%.0f", pct) + "%"
                                Text(pctText, color = Color.White, fontSize = 20.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        LineChart(series, Modifier.fillMaxWidth().height(170.dp))
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val now = System.currentTimeMillis()
                            listOf(29, 20, 10, 0).forEach { d ->
                                Text(
                                    if (d == 0) "امروز" else formatShortDate(dayStart(d, now)),
                                    color = WColors.Muted, fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(8.dp).background(WColors.Section))
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("آخرین تراکنش‌ها", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        Text("آخرین تراکنش‌هایی که ثبت کرده‌ام", color = WColors.Muted, fontSize = 13.sp)
                    }
                    Hairline()
                }
                if (mine.isEmpty()) {
                    item {
                        Text("هنوز تراکنشی برای این حساب ثبت نشده است.", color = WColors.Muted, fontSize = 14.sp, modifier = Modifier.padding(24.dp))
                    }
                } else {
                    items(mine, key = { it.id }) { rec ->
                        RecordRow(rec, accounts, categories) { onOpenRecord(rec) }
                        Hairline(Modifier.padding(horizontal = 16.dp))
                    }
                    item {
                        TextButton(onClick = onAllRecords, modifier = Modifier.padding(8.dp)) {
                            Text("مشاهده همه تراکنش‌ها", color = WColors.Blue)
                        }
                    }
                }
            }
        }
        ExpandableFab(
            actions = buildList {
                templates.take(2).reversed().forEach { t ->
                    add(FabAction("قالب: ${t.name}", Icons.Filled.ContentCopy) { onTemplate(t) })
                }
                add(FabAction("انتقال", Icons.Filled.SwapHoriz, onTransfer))
                add(FabAction("تراکنش جدید", Icons.Filled.Edit, onNewRecord))
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    if (showBalanceDialog) {
        TextInputDialog(
            title = "اصلاح موجودی",
            label = "موجودی جدید (ریال)",
            initial = account.balance.toString(),
            keyboardType = KeyboardType.Number,
            onConfirm = { text ->
                val v = text.filter { it.isDigit() || it == '-' }.toLongOrNull()
                if (v != null) onSetBalance(v)
                showBalanceDialog = false
            },
            onDismiss = { showBalanceDialog = false }
        )
    }
}

// ───────────────────────────── فهرست حساب‌ها (مدیریت یا انتخاب) ─────────────────────────────

/** [onPick] != null → حالت انتخاب (برای تراکنش)؛ در غیر این صورت حالت مدیریت */
@Composable
fun AccountsScreen(
    title: String,
    accounts: List<AccountEntity>,
    onBack: () -> Unit,
    onPick: ((AccountEntity) -> Unit)?,
    onEdit: (AccountEntity) -> Unit,
    onAdd: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(WColors.Page)) {
        Column(Modifier.fillMaxSize()) {
            WalletAppBar(title = title, navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(accounts, key = { it.id }) { acc ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { if (onPick != null) onPick(acc) else onEdit(acc) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircleIcon(accountIcon(acc.type), Color(acc.colorHex), size = 40.dp, iconSize = 24.dp, shape = RoundedCornerShape(6.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(acc.name, color = Color.White, fontSize = 15.sp)
                            Text(labelOf(AccountTypes, acc.type), color = WColors.Muted, fontSize = 13.sp)
                        }
                        if (onPick != null) {
                            IconButton(onClick = { onEdit(acc) }) { Icon(Icons.Filled.Edit, null, tint = WColors.Muted) }
                        } else {
                            Text(formatIrr(acc.balance), color = Color(0xFFE0E0E0), fontSize = 13.sp)
                        }
                    }
                    Hairline(Modifier.padding(start = 72.dp))
                }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            containerColor = WColors.Blue,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Filled.Add, null) }
    }
}

// ───────────────────────────── ویرایش / ساخت حساب ─────────────────────────────

@Composable
fun AccountEditScreen(
    existing: AccountEntity?,
    onBack: () -> Unit,
    onSave: (AccountEntity) -> Unit,
    onDelete: (AccountEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: "Cash") }
    var balanceText by remember { mutableStateOf(existing?.balance?.toString() ?: "0") }
    var color by remember { mutableStateOf(existing?.colorHex ?: AccountPalette[1]) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        WalletAppBar(
            title = if (existing == null) "حساب جدید" else "ویرایش حساب",
            navIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavClick = onBack,
            actions = {
                if (existing != null) {
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, null, tint = Color.White) }
                }
                IconButton(onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "نام حساب را وارد کنید", Toast.LENGTH_SHORT).show()
                    } else {
                        val bal = balanceText.filter { it.isDigit() || it == '-' }.toLongOrNull() ?: 0L
                        onSave(
                            AccountEntity(
                                id = existing?.id ?: 0,
                                name = name.trim(),
                                type = type,
                                balance = bal,
                                colorHex = color,
                                sortOrder = existing?.sortOrder ?: 0
                            )
                        )
                    }
                }) { Icon(Icons.Filled.Check, null, tint = Color.White) }
            }
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                CircleIcon(accountIcon(type), Color(color), size = 72.dp, iconSize = 40.dp, shape = RoundedCornerShape(12.dp))
            }
            UnderlineField("نام حساب", name, { name = it }, placeholder = "مثلاً ملی")
            Column(Modifier.padding(horizontal = 16.dp)) {
                DropdownField("نوع حساب", labelOf(AccountTypes, type), AccountTypes, { type = it })
            }
            UnderlineField(
                label = if (existing == null) "موجودی اولیه (ریال)" else "موجودی فعلی (ریال)",
                value = balanceText,
                onValueChange = { balanceText = it },
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(12.dp))
            Text("رنگ", color = WColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            Column(Modifier.padding(horizontal = 16.dp)) { ColorPicker(AccountPalette, color) { color = it } }
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "حذف حساب",
            text = "حساب «${existing.name}» همراه با تمام تراکنش‌هایش حذف می‌شود. این کار قابل بازگشت نیست.",
            confirmText = "حذف",
            onConfirm = {
                confirmDelete = false
                onDelete(existing)
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
