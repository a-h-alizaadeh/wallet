package com.personal.wallet

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** صفحه‌های برنامه؛ پیش‌نویس تراکنش در خود صفحه‌ها نگهداری می‌شود تا هنگام رفتن به انتخاب دسته/حساب از بین نرود. */
sealed class Screen {
    object Home : Screen()
    data class AccountDetail(val accountId: Int) : Screen()
    data class Records(val accountIds: Set<Int>) : Screen()
    data class AddRecord(val draft: RecordDraft) : Screen()
    data class RecordDetails(val draft: RecordDraft) : Screen()
    data class CategoryPicker(val draft: RecordDraft) : Screen()
    data class SubCategories(val draft: RecordDraft, val parentId: Int) : Screen()
    data class AccountPicker(val draft: RecordDraft, val forTarget: Boolean) : Screen()
    object Categories : Screen()
    data class CategoryEdit(val categoryId: Int?, val parentId: Int?) : Screen()
    object Accounts : Screen()
    data class AccountEdit(val accountId: Int?) : Screen()
    object Templates : Screen()
    object Spending : Screen()
    object Cashflow : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repo = WalletRepository(database)
        setContent {
            WalletTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = WColors.Page, contentColor = Color.White) {
                    WalletApp(repo)
                }
            }
        }
    }
}

@Composable
fun WalletApp(repo: WalletRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val accounts by repo.accounts.collectAsState(initial = emptyList())
    val categories by repo.categories.collectAsState(initial = emptyList())
    val records by repo.records.collectAsState(initial = emptyList())
    val templates by repo.templates.collectAsState(initial = emptyList())

    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val current = stack.last()

    var rawSelected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var period by remember { mutableStateOf(Period.D30) }

    // انتخاب معتبر: فقط حساب‌هایی که هنوز وجود دارند؛ اگر خالی بود یعنی همه
    val selectedIds: Set<Int> = remember(rawSelected, accounts) {
        val valid = rawSelected.filter { id -> accounts.any { it.id == id } }.toSet()
        if (valid.isEmpty()) accounts.map { it.id }.toSet() else valid
    }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun finishDraft(draft: RecordDraft) {
        val idx = stack.indexOfLast { it is Screen.AddRecord && it.draft === draft }
        if (idx > 0) {
            while (stack.size > idx) stack.removeAt(stack.lastIndex)
        } else {
            pop()
        }
    }

    // ── خروجی CSV (انتخاب محل ذخیره با سیستم فایل اندروید؛ نیازی به مجوز نیست) ──
    var pendingCsv by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(pendingCsv.toByteArray(Charsets.UTF_8)) }
                toast("فایل ذخیره شد")
            } catch (e: Exception) {
                toast("ذخیره فایل ناموفق بود")
            }
        }
    }
    fun export(list: List<RecordEntity>) {
        if (list.isEmpty()) { toast("تراکنشی برای خروجی وجود ندارد"); return }
        pendingCsv = "\uFEFF" + buildCsv(list, accounts)
        exportLauncher.launch("wallet-export.csv")
    }

    fun startDraft(type: String) {
        if (accounts.isEmpty()) { toast("ابتدا یک حساب بسازید"); return }
        val pref = (current as? Screen.AccountDetail)?.accountId ?: selectedIds.singleOrNull()
        push(Screen.AddRecord(newDraft(type, accounts, categories, pref)))
    }
    fun startFromTemplate(t: TemplateEntity) {
        if (accounts.none { it.id == t.accountId }) { toast("حساب این قالب دیگر وجود ندارد"); return }
        push(Screen.AddRecord(draftFromTemplate(t)))
    }
    fun saveDraft(draft: RecordDraft) {
        val rec = draft.toEntity()
        if (accounts.none { it.id == rec.accountId }) { toast("حساب را انتخاب کنید"); return }
        if (rec.amount <= 0L) { toast("مبلغ را وارد کنید"); return }
        if (rec.type == "TRANSFER" && (rec.targetAccountId == null || rec.targetAccountId == rec.accountId)) {
            toast("حساب مبدأ و مقصد باید متفاوت باشند"); return
        }
        scope.launch { repo.saveRecord(rec, draft.editing) }
        finishDraft(draft)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = current is Screen.Home,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = WColors.AppBar, modifier = Modifier.width(290.dp)) {
                Spacer(Modifier.height(28.dp))
                Text("مدیریت مالی شخصی", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                Hairline(Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(8.dp))
                fun go(s: Screen?) {
                    scope.launch { drawerState.close() }
                    if (s != null) { while (stack.size > 1) stack.removeAt(stack.lastIndex); push(s) }
                }
                DrawerItem("خانه", Icons.Filled.Home) { go(null) }
                DrawerItem("تراکنش‌ها", Icons.AutoMirrored.Filled.List) { go(Screen.Records(accounts.map { it.id }.toSet())) }
                DrawerItem("حساب‌ها", Icons.Filled.AccountBalanceWallet) { go(Screen.Accounts) }
                DrawerItem("دسته‌بندی‌ها", Icons.Filled.Category) { go(Screen.Categories) }
                DrawerItem("قالب‌ها", Icons.Filled.ContentCopy) { go(Screen.Templates) }
                DrawerItem("هزینه‌ها", Icons.Filled.PieChart) { go(Screen.Spending) }
                DrawerItem("جریان نقدی", Icons.Filled.BarChart) { go(Screen.Cashflow) }
                DrawerItem("خروجی CSV", Icons.Filled.Share) {
                    scope.launch { drawerState.close() }
                    export(records)
                }
                Spacer(Modifier.weight(1f))
                Hairline()
                Text("نسخه ۲.۰", color = WColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
            }
        }
    ) {
        Box(Modifier.fillMaxSize().background(WColors.Page)) {
            when (val s = current) {
                is Screen.Home -> HomeScreen(
                    accounts = accounts, records = records, categories = categories, templates = templates,
                    selectedIds = selectedIds, period = period, onPeriodChange = { period = it },
                    onSelectOnly = { rawSelected = setOf(it) },
                    onToggle = { id ->
                        val next = if (id in selectedIds) selectedIds - id else selectedIds + id
                        rawSelected = if (next.isEmpty()) setOf(id) else next
                    },
                    onSelectAll = { rawSelected = accounts.map { it.id }.toSet() },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onAddAccount = { push(Screen.AccountEdit(null)) },
                    onOpenAccounts = { push(Screen.Accounts) },
                    onAccountDetail = {
                        val id = selectedIds.singleOrNull()
                        if (id == null) toast("برای دیدن جزئیات، فقط یک حساب را انتخاب کنید") else push(Screen.AccountDetail(id))
                    },
                    onRecords = { push(Screen.Records(selectedIds)) },
                    onNewRecord = { startDraft("EXPENSE") },
                    onTransfer = { startDraft("TRANSFER") },
                    onTemplate = { startFromTemplate(it) },
                    onSpending = { push(Screen.Spending) },
                    onExport = { export(records.filter { recordTouches(it, selectedIds) }) },
                    onCashflow = { push(Screen.Cashflow) },
                    onTemplates = { push(Screen.Templates) },
                    onNotifications = { toast("اعلان جدیدی وجود ندارد") }
                )

                is Screen.AccountDetail -> {
                    val acc = accounts.firstOrNull { it.id == s.accountId }
                    if (acc == null) {
                        LaunchedEffect(Unit) { pop() }
                    } else {
                        AccountDetailScreen(
                            account = acc, records = records, accounts = accounts, categories = categories, templates = templates,
                            onBack = { pop() },
                            onEdit = { push(Screen.AccountEdit(acc.id)) },
                            onSetBalance = { v -> scope.launch { repo.setBalance(acc.id, v) } },
                            onOpenRecord = { push(Screen.AddRecord(draftFromRecord(it))) },
                            onAllRecords = { push(Screen.Records(setOf(acc.id))) },
                            onNewRecord = { startDraft("EXPENSE") },
                            onTransfer = { startDraft("TRANSFER") },
                            onTemplate = { startFromTemplate(it) }
                        )
                    }
                }

                is Screen.Records -> RecordsScreen(
                    accountIds = s.accountIds, accounts = accounts, records = records, categories = categories,
                    onBack = { pop() },
                    onOpenRecord = { push(Screen.AddRecord(draftFromRecord(it))) },
                    onExport = { export(it) },
                    onNewRecord = { startDraft("EXPENSE") },
                    onTransfer = { startDraft("TRANSFER") },
                    templates = templates,
                    onTemplate = { startFromTemplate(it) }
                )

                is Screen.AddRecord -> AddRecordScreen(
                    draft = s.draft, accounts = accounts, templates = templates,
                    onClose = { pop() },
                    onSave = { saveDraft(s.draft) },
                    onDelete = {
                        s.draft.editing?.let { rec -> scope.launch { repo.deleteRecord(rec) } }
                        finishDraft(s.draft)
                    },
                    onOpenDetails = { push(Screen.RecordDetails(s.draft)) },
                    onPickAccount = { push(Screen.AccountPicker(s.draft, false)) },
                    onPickTarget = { push(Screen.AccountPicker(s.draft, true)) },
                    onPickCategory = { push(Screen.CategoryPicker(s.draft)) },
                    onApplyTemplate = { t ->
                        s.draft.type = t.type
                        s.draft.accountId = t.accountId
                        s.draft.targetAccountId = t.targetAccountId
                        s.draft.categoryName = t.categoryName
                        s.draft.expression = t.amount.toString()
                        s.draft.note = t.note
                    }
                )

                is Screen.RecordDetails -> RecordDetailsScreen(
                    draft = s.draft, accounts = accounts,
                    onBack = { pop() },
                    onSave = { saveDraft(s.draft) },
                    onSaveTemplate = { name ->
                        val d = s.draft
                        scope.launch {
                            repo.saveTemplate(
                                TemplateEntity(
                                    name = name, type = d.type, amount = d.amountValue(), accountId = d.accountId,
                                    targetAccountId = d.targetAccountId, categoryName = d.categoryName, note = d.note.trim()
                                )
                            )
                        }
                        toast("قالب ذخیره شد")
                    }
                )

                is Screen.CategoryPicker -> CategoryPickerScreen(
                    categories = categories, records = records,
                    onBack = { pop() },
                    onSettings = { push(Screen.Categories) },
                    onPick = { c -> s.draft.categoryName = c.name; pop() },
                    onOpenSub = { c -> push(Screen.SubCategories(s.draft, c.id)) }
                )

                is Screen.SubCategories -> {
                    val parent = categories.firstOrNull { it.id == s.parentId }
                    if (parent == null) {
                        LaunchedEffect(Unit) { pop() }
                    } else {
                        SubCategoryScreen(
                            parent = parent, categories = categories,
                            onBack = { pop() },
                            onSettings = { push(Screen.Categories) },
                            onPick = { c -> s.draft.categoryName = c.name; pop(); pop() }
                        )
                    }
                }

                is Screen.AccountPicker -> AccountsScreen(
                    title = "حساب", accounts = accounts, onBack = { pop() },
                    onPick = { acc ->
                        if (s.forTarget) s.draft.targetAccountId = acc.id else {
                            s.draft.accountId = acc.id
                            if (s.draft.targetAccountId == acc.id) {
                                s.draft.targetAccountId = accounts.firstOrNull { it.id != acc.id }?.id
                            }
                        }
                        pop()
                    },
                    onEdit = { push(Screen.AccountEdit(it.id)) },
                    onAdd = { push(Screen.AccountEdit(null)) }
                )

                is Screen.Categories -> CategoriesScreen(
                    categories = categories, onBack = { pop() },
                    onEdit = { push(Screen.CategoryEdit(it.id, null)) },
                    onAdd = { push(Screen.CategoryEdit(null, null)) }
                )

                is Screen.CategoryEdit -> CategoryEditScreen(
                    existing = categories.firstOrNull { it.id == s.categoryId },
                    categories = categories, presetParentId = s.parentId,
                    onBack = { pop() },
                    onSave = { cat, oldName -> scope.launch { repo.saveCategory(cat, oldName) }; pop() },
                    onDelete = { cat -> scope.launch { repo.deleteCategory(cat) }; pop() }
                )

                is Screen.Accounts -> AccountsScreen(
                    title = "حساب‌ها", accounts = accounts, onBack = { pop() },
                    onPick = null,
                    onEdit = { push(Screen.AccountEdit(it.id)) },
                    onAdd = { push(Screen.AccountEdit(null)) }
                )

                is Screen.AccountEdit -> AccountEditScreen(
                    existing = accounts.firstOrNull { it.id == s.accountId },
                    onBack = { pop() },
                    onSave = { acc -> scope.launch { repo.saveAccount(acc) }; pop() },
                    onDelete = { acc ->
                        scope.launch { repo.deleteAccount(acc) }
                        // اگر از صفحه جزئیات همین حساب آمده‌ایم، آن را هم ببند
                        pop()
                        if (stack.lastOrNull().let { it is Screen.AccountDetail && it.accountId == acc.id }) pop()
                        rawSelected = rawSelected - acc.id
                    }
                )

                is Screen.Templates -> TemplatesScreen(
                    templates = templates, accounts = accounts, onBack = { pop() },
                    onUse = { startFromTemplate(it) },
                    onDelete = { t -> scope.launch { repo.deleteTemplate(t) } }
                )

                is Screen.Spending -> SpendingScreen(selectedIds, records, categories) { pop() }
                is Screen.Cashflow -> CashflowScreen(selectedIds, records) { pop() }
            }
        }
    }
}

@Composable
fun DrawerItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFE0E0E0), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, color = Color.White, fontSize = 15.sp)
    }
}
