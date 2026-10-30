package com.personal.wallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val dao = database.walletDao()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val accountsState = remember { mutableStateOf<List<AccountEntity>>(emptyList()) }
                val categoriesState = remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
                val recordsState = remember { mutableStateOf<List<RecordEntity>>(emptyList()) }
                var currentScreen by remember { mutableStateOf("HOME") }
                var editingRecord by remember { mutableStateOf<RecordEntity?>(null) }
                var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    dao.getAllAccounts().collectLatest { accountsState.value = it }
                }
                LaunchedEffect(Unit) {
                    dao.getAllCategories().collectLatest { categoriesState.value = it }
                }
                LaunchedEffect(Unit) {
                    dao.getAllRecords().collectLatest { recordsState.value = it }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = Color(0xFF171B2A),
                            modifier = Modifier.width(280.dp)
                        ) {
                            Spacer(Modifier.height(28.dp))
                            Text(
                                text = "Wallet Pro",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Divider(color = Color(0xFF2A3146), thickness = 1.dp)
                            Spacer(Modifier.height(12.dp))
                            DrawerItem("Home", Icons.Default.Home) {
                                scope.launch { drawerState.close() }
                                currentScreen = "HOME"
                            }
                            DrawerItem("Transactions", Icons.Default.List) {
                                scope.launch { drawerState.close() }
                                currentScreen = "LIST"
                            }
                            DrawerItem("Categories", Icons.Default.Category) {
                                scope.launch { drawerState.close() }
                                currentScreen = "CATEGORIES"
                            }
                            Spacer(Modifier.weight(1f))
                            Divider(color = Color(0xFF2A3146), thickness = 1.dp)
                            Text(
                                text = "Smart personal wallet",
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                ) {
                    Surface(color = Color(0xFF0D1220), modifier = Modifier.fillMaxSize()) {
                        when (currentScreen) {
                            "HOME" -> WalletHomeScreen(
                                totalBalance = accountsState.value.sumOf { it.balance },
                                income = recordsState.value.filter { it.type == "INCOME" }.sumOf { it.amount },
                                expense = recordsState.value.filter { it.type == "EXPENSE" }.sumOf { it.amount },
                                accounts = accountsState.value,
                                records = recordsState.value.sortedByDescending { it.timestamp },
                                categories = categoriesState.value,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onAddTransaction = {
                                    editingRecord = null
                                    currentScreen = "EDITOR"
                                },
                                onAddAccount = { name, initialBalance, colorHex ->
                                    scope.launch {
                                        dao.insertAccount(AccountEntity(name = name, balance = initialBalance, colorHex = colorHex))
                                    }
                                },
                                onOpenCategories = { currentScreen = "CATEGORIES" },
                                onEditTransaction = { record ->
                                    editingRecord = record
                                    currentScreen = "EDITOR"
                                },
                                onDeleteTransaction = { record ->
                                    scope.launch {
                                        revertTransactionEffect(dao, accountsState.value, record)
                                        dao.deleteRecord(record)
                                    }
                                }
                            )
                            "LIST" -> TransactionsScreen(
                                records = recordsState.value.sortedByDescending { it.timestamp },
                                accounts = accountsState.value,
                                categories = categoriesState.value,
                                onBack = { currentScreen = "HOME" },
                                onEdit = { record ->
                                    editingRecord = record
                                    currentScreen = "EDITOR"
                                },
                                onDelete = { record ->
                                    scope.launch {
                                        revertTransactionEffect(dao, accountsState.value, record)
                                        dao.deleteRecord(record)
                                    }
                                }
                            )
                            "CATEGORIES" -> CategoriesScreen(
                                categories = categoriesState.value,
                                onBack = { currentScreen = "HOME" },
                                onAddCategory = { name, iconName, colorHex, isExpense ->
                                    scope.launch {
                                        dao.insertCategory(
                                            CategoryEntity(
                                                name = name,
                                                iconName = iconName,
                                                colorHex = colorHex,
                                                isExpense = isExpense
                                            )
                                        )
                                    }
                                },
                                onUpdateCategory = { category ->
                                    scope.launch { dao.updateCategory(category) }
                                },
                                onDeleteCategory = { category ->
                                    scope.launch { dao.deleteCategory(category) }
                                }
                            )
                            "EDITOR" -> TransactionEditorScreen(
                                initialRecord = editingRecord,
                                accounts = accountsState.value,
                                categories = categoriesState.value,
                                onBack = { currentScreen = "HOME" },
                                onSave = { record ->
                                    scope.launch {
                                        val previous = editingRecord
                                        if (previous != null) {
                                            revertTransactionEffect(dao, accountsState.value, previous)
                                            dao.updateRecord(record)
                                        } else {
                                            dao.insertRecord(record)
                                        }
                                        applyTransactionEffect(dao, accountsState.value, record)
                                        editingRecord = null
                                        currentScreen = "HOME"
                                    }
                                },
                                onDelete = { record ->
                                    scope.launch {
                                        revertTransactionEffect(dao, accountsState.value, record)
                                        dao.deleteRecord(record)
                                        editingRecord = null
                                        currentScreen = "HOME"
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun applyTransactionEffect(dao: WalletDao, accounts: List<AccountEntity>, record: RecordEntity) {
    val fromAccount = accounts.find { it.id == record.accountId } ?: return

    when (record.type) {
        "EXPENSE" -> {
            dao.updateAccount(fromAccount.copy(balance = fromAccount.balance - record.amount))
        }
        "INCOME" -> {
            dao.updateAccount(fromAccount.copy(balance = fromAccount.balance + record.amount))
        }
        "TRANSFER" -> {
            val target = accounts.find { it.id == record.targetAccountId } ?: return
            if (record.accountId != record.targetAccountId) {
                dao.updateAccount(fromAccount.copy(balance = fromAccount.balance - record.amount))
                dao.updateAccount(target.copy(balance = target.balance + record.amount))
            }
        }
    }
}

private suspend fun revertTransactionEffect(dao: WalletDao, accounts: List<AccountEntity>, record: RecordEntity) {
    val fromAccount = accounts.find { it.id == record.accountId } ?: return

    when (record.type) {
        "EXPENSE" -> {
            dao.updateAccount(fromAccount.copy(balance = fromAccount.balance + record.amount))
        }
        "INCOME" -> {
            dao.updateAccount(fromAccount.copy(balance = fromAccount.balance - record.amount))
        }
        "TRANSFER" -> {
            val target = accounts.find { it.id == record.targetAccountId } ?: return
            if (record.accountId != record.targetAccountId) {
                dao.updateAccount(fromAccount.copy(balance = fromAccount.balance + record.amount))
                dao.updateAccount(target.copy(balance = target.balance - record.amount))
            }
        }
    }
}

@Composable
fun DrawerItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

fun formatMoney(amount: Long): String = "IRR ${NumberFormat.getNumberInstance(Locale.US).format(amount)}"

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun WalletHomeScreen(
    totalBalance: Long,
    income: Long,
    expense: Long,
    accounts: List<AccountEntity>,
    records: List<RecordEntity>,
    categories: List<CategoryEntity>,
    onOpenDrawer: () -> Unit,
    onAddTransaction: () -> Unit,
    onAddAccount: (String, Long, Long) -> Unit,
    onOpenCategories: () -> Unit,
    onEditTransaction: (RecordEntity) -> Unit,
    onDeleteTransaction: (RecordEntity) -> Unit
) {
    var hideBalance by remember { mutableStateOf(false) }
    var showCreateAccountDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF0D1220),
        topBar = {
            TopAppBar(
                title = { Text("Wallet", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { hideBalance = !hideBalance }) {
                        Icon(
                            imageVector = if (hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141C2C))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = Color(0xFF5B8CFF),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B243B)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total balance", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                        Icon(Icons.Default.AccountBalanceWallet, tint = Color(0xFF5B8CFF), contentDescription = null)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (hideBalance) "IRR ••••••" else formatMoney(totalBalance),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryChip("Income", income, Color(0xFF2ECF8A))
                        SummaryChip("Expense", expense, Color(0xFFEF5350))
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Accounts", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showCreateAccountDialog = true }) {
                    Text("Add", color = Color(0xFF5B8CFF))
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(accounts) { account ->
                    AccountCard(account = account, hideBalance = hideBalance)
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Transactions", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenCategories) {
                    Text("Categories", color = Color(0xFF5B8CFF))
                }
            }
            Spacer(Modifier.height(8.dp))

            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No transactions yet", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(records.take(8)) { record ->
                        TransactionRow(
                            record = record,
                            accounts = accounts,
                            onEdit = onEditTransaction,
                            onDelete = onDeleteTransaction
                        )
                    }
                }
            }
        }
    }

    if (showCreateAccountDialog) {
        var name by remember { mutableStateOf("") }
        var balance by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateAccountDialog = false },
            containerColor = Color(0xFF1B243B),
            title = { Text("New account", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = balance,
                        onValueChange = { balance = it },
                        label = { Text("Initial amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = balance.toLongOrNull() ?: 0L
                    if (name.isNotBlank()) {
                        onAddAccount(name.trim(), parsed, getRandomAccent())
                    }
                    showCreateAccountDialog = false
                }) { Text("Save", color = Color(0xFF5B8CFF)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAccountDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }
}

@Composable
fun SummaryChip(label: String, value: Long, tint: Color) {
    Column(
        modifier = Modifier
            .background(Color(0xFF111827), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, color = Color(0xFF9CA3AF), fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(formatMoney(value), color = tint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AccountCard(account: AccountEntity, hideBalance: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(account.colorHex)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(account.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White.copy(alpha = 0.9f))
            }
            Spacer(Modifier.height(30.dp))
            Text(
                text = if (hideBalance) "IRR ••••••" else formatMoney(account.balance),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TransactionRow(
    record: RecordEntity,
    accounts: List<AccountEntity>,
    onEdit: (RecordEntity) -> Unit,
    onDelete: (RecordEntity) -> Unit
) {
    val accountName = accounts.find { it.id == record.accountId }?.name ?: "Unknown"
    val categoryName = record.categoryName.ifBlank { "General" }
    val sign = if (record.type == "INCOME") "+" else if (record.type == "EXPENSE") "-" else "↔"
    val cardColor = when (record.type) {
        "INCOME" -> Color(0xFF1E3A32)
        "EXPENSE" -> Color(0xFF3A1E2E)
        else -> Color(0xFF1E2D40)
    }
    val tint = when (record.type) {
        "INCOME" -> Color(0xFF2ECF8A)
        "EXPENSE" -> Color(0xFFEF5350)
        else -> Color(0xFF5B8CFF)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF111827), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (record.type) {
                            "INCOME" -> Icons.Default.TrendingUp
                            "EXPENSE" -> Icons.Default.ShoppingCart
                            else -> Icons.Default.SwapHoriz
                        },
                        contentDescription = null,
                        tint = tint
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(categoryName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(accountName, color = Color(0xFFBFC7D8), fontSize = 11.sp)
                    Text(formatDate(record.timestamp), color = Color(0xFF8C93A6), fontSize = 10.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$sign ${formatMoney(record.amount)}",
                    color = tint,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onEdit(record) }) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                }
                IconButton(onClick = { onDelete(record) }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF5350))
                }
            }
        }
    }
}

@Composable
fun TransactionsScreen(
    records: List<RecordEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onEdit: (RecordEntity) -> Unit,
    onDelete: (RecordEntity) -> Unit
) {
    BackHandler { onBack() }
    Scaffold(
        containerColor = Color(0xFF0D1220),
        topBar = {
            TopAppBar(
                title = { Text("Transactions", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141C2C))
            )
        }
    ) { padding ->
        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No transactions yet", color = Color(0xFF9CA3AF))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records) { record ->
                    TransactionRow(record = record, accounts = accounts, onEdit = onEdit, onDelete = onDelete)
                }
            }
        }
    }
}

@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onAddCategory: (String, String, Long, Boolean) -> Unit,
    onUpdateCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit
) {
    BackHandler { onBack() }
    var showEditor by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        containerColor = Color(0xFF0D1220),
        topBar = {
            TopAppBar(
                title = { Text("Categories", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { editingCategory = null; showEditor = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141C2C))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                CategoryCard(
                    category = category,
                    onEdit = {
                        editingCategory = category
                        showEditor = true
                    },
                    onDelete = { onDeleteCategory(category) }
                )
            }
        }
    }

    if (showEditor) {
        CategoryEditorDialog(
            category = editingCategory,
            onDismiss = { showEditor = false; editingCategory = null },
            onSave = { saved ->
                if (editingCategory == null) {
                    onAddCategory(saved.name, saved.iconName, saved.colorHex, saved.isExpense)
                } else {
                    onUpdateCategory(saved)
                }
                showEditor = false
                editingCategory = null
            }
        )
    }
}

@Composable
fun CategoryCard(
    category: CategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171E2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(category.colorHex), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(iconFor(category.iconName), contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(category.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(if (category.isExpense) "Expense" else "Income", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                }
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, tint = Color.White, contentDescription = null) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, tint = Color(0xFFEF5350), contentDescription = null) }
            }
        }
    }
}

@Composable
fun CategoryEditorDialog(
    category: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit
) {
    var name by remember(category?.id) { mutableStateOf(category?.name ?: "") }
    var iconName by remember(category?.id) { mutableStateOf(category?.iconName ?: "attach_money") }
    var color by remember(category?.id) { mutableStateOf(category?.colorHex ?: 0xFF5B8CFF) }
    var isExpense by remember(category?.id) { mutableStateOf(category?.isExpense ?: true) }

    val iconOptions = listOf("attach_money", "shopping_cart", "restaurant", "home", "school", "card_giftcard", "flight", "directions_car", "medical_services", "phone_iphone", "wifi", "trending_up")
    val colorOptions = listOf(0xFF5B8CFF, 0xFF2ECF8A, 0xFFEF5350, 0xFFFFB300, 0xFF9C27B0, 0xFF00BCD4, 0xFFFF7043, 0xFF8BC34A)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF171E2E),
        title = { Text(if (category == null) "New Category" else "Edit Category", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Text("Icon", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(iconOptions) { option ->
                        val selected = option == iconName
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(if (selected) Color(color) else Color(0xFF0F172A), CircleShape)
                                .border(1.dp, if (selected) Color.White else Color.Transparent, CircleShape)
                                .clickable { iconName = option },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(iconFor(option), contentDescription = null, tint = Color.White)
                        }
                    }
                }

                Text("Color", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorOptions) { option ->
                        val selected = option == color
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(option), CircleShape)
                                .border(2.dp, if (selected) Color.White else Color.Transparent, CircleShape)
                                .clickable { color = option },
                            contentAlignment = Alignment.Center
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isExpense,
                        onCheckedChange = { isExpense = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF5B8CFF))
                    )
                    Text("Expense category", color = Color.White)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        CategoryEntity(
                            id = category?.id ?: 0,
                            name = name.trim(),
                            iconName = iconName,
                            colorHex = color,
                            isExpense = isExpense
                        )
                    )
                }
            }) { Text("Save", color = Color(0xFF5B8CFF)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

@Composable
fun TransactionEditorScreen(
    initialRecord: RecordEntity?,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onSave: (RecordEntity) -> Unit,
    onDelete: (RecordEntity) -> Unit
) {
    BackHandler { onBack() }

    val defaultType = initialRecord?.type ?: "EXPENSE"
    val defaultCategory = categories.find { it.id == initialRecord?.categoryId } ?: categories.firstOrNull() ?: CategoryEntity(name = "General")
    val selectedAccountId = remember(initialRecord?.id) { mutableStateOf(initialRecord?.accountId ?: accounts.firstOrNull()?.id ?: 1) }
    val targetAccountId = remember(initialRecord?.id) { mutableStateOf(initialRecord?.targetAccountId ?: accounts.getOrNull(1)?.id ?: (accounts.firstOrNull()?.id ?: 1)) }
    var amountText by remember(initialRecord?.id) { mutableStateOf((initialRecord?.amount ?: 0L).toString()) }
    var note by remember(initialRecord?.id) { mutableStateOf(initialRecord?.note ?: "") }
    var selectedType by remember(initialRecord?.id) { mutableStateOf(defaultType) }
    var selectedCategory by remember(initialRecord?.id) { mutableStateOf(defaultCategory) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }

    val bgColor = when (selectedType) {
        "INCOME" -> Color(0xFF173A2D)
        "TRANSFER" -> Color(0xFF132C4A)
        else -> Color(0xFF3A1E2E)
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = { Text(if (initialRecord == null) "New Transaction" else "Edit Transaction", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = null, tint = Color.White) }
                },
                actions = {
                    if (initialRecord != null) {
                        IconButton(onClick = { onDelete(initialRecord) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF5350))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111827))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TypeSelector("Expense", selectedType == "EXPENSE") { selectedType = "EXPENSE" }
                TypeSelector("Income", selectedType == "INCOME") { selectedType = "INCOME" }
                TypeSelector("Transfer", selectedType == "TRANSFER") { selectedType = "TRANSFER" }
            }

            Spacer(Modifier.height(26.dp))
            Text(
                text = if (amountText.isBlank()) "0" else amountText,
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1
            )
            Text("IRR", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                if (selectedType == "TRANSFER") {
                    SelectorLabel("From", accounts.find { it.id == selectedAccountId.value }?.name ?: "Select") { showAccountDialog = true }
                    SelectorLabel("To", accounts.find { it.id == targetAccountId.value }?.name ?: "Select") { showTargetDialog = true }
                } else {
                    SelectorLabel("Account", accounts.find { it.id == selectedAccountId.value }?.name ?: "Select") { showAccountDialog = true }
                    SelectorLabel("Category", selectedCategory.name) { showCategoryDialog = true }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Note") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val value = amountText.toLongOrNull() ?: 0L
                    if (value > 0) {
                        val record = RecordEntity(
                            id = initialRecord?.id ?: 0,
                            amount = value,
                            type = selectedType,
                            accountId = selectedAccountId.value,
                            targetAccountId = if (selectedType == "TRANSFER") targetAccountId.value else null,
                            categoryId = selectedCategory.id,
                            categoryName = selectedCategory.name,
                            note = note,
                            timestamp = initialRecord?.timestamp ?: System.currentTimeMillis()
                        )
                        onSave(record)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B8CFF))
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            CalculatorPad(
                onNumberClick = { key ->
                    amountText = if (amountText == "0") key else amountText + key
                },
                onDeleteClick = {
                    amountText = if (amountText.length > 1) amountText.dropLast(1) else "0"
                },
                onClearClick = { amountText = "0" },
                onEqualClick = { amountText = (amountText.toLongOrNull() ?: 0L).toString() }
            )
        }
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            containerColor = Color(0xFF171E2E),
            title = { Text("Select account", color = Color.White) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(accounts) { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAccountId.value = account.id
                                    showAccountDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(Color(account.colorHex), CircleShape)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(account.name, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAccountDialog = false }) { Text("Close") } }
        )
    }

    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            containerColor = Color(0xFF171E2E),
            title = { Text("Select target account", color = Color.White) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(accounts.filter { it.id != selectedAccountId.value }) { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    targetAccountId.value = account.id
                                    showTargetDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(Color(account.colorHex), CircleShape)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(account.name, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTargetDialog = false }) { Text("Close") } }
        )
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            containerColor = Color(0xFF171E2E),
            title = { Text("Select category", color = Color.White) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(categories) { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCategory = category
                                    showCategoryDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color(category.colorHex), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(iconFor(category.iconName), contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(category.name, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
fun TypeSelector(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color(0xFF5B8CFF) else Color(0xFF1F2A3A)
    Text(
        text = label,
        color = if (selected) Color.White else Color(0xFF9CA3AF),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    )
}

@Composable
fun SelectorLabel(label: String, value: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Text(label, color = Color(0xFF9CA3AF), fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

fun iconFor(name: String): ImageVector = when (name) {
    "home" -> Icons.Default.Home
    "restaurant" -> Icons.Default.Restaurant
    "shopping_cart" -> Icons.Default.ShoppingCart
    "flight" -> Icons.Default.Flight
    "directions_car" -> Icons.Default.DirectionsCar
    "medical_services" -> Icons.Default.MedicalServices
    "school" -> Icons.Default.School
    "card_giftcard" -> Icons.Default.CardGiftcard
    "attach_money" -> Icons.Default.AttachMoney
    "trending_up" -> Icons.Default.TrendingUp
    "wifi" -> Icons.Default.Wifi
    "phone_iphone" -> Icons.Default.PhoneIphone
    else -> Icons.Default.Category
}

fun getRandomAccent(): Long = listOf(
    0xFF5B8CFF,
    0xFF2ECF8A,
    0xFFEF5350,
    0xFFF9A825,
    0xFF9C27B0,
    0xFF00BCD4,
    0xFF8BC34A,
    0xFFFF7043
).random()

@Composable
fun CalculatorPad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearClick: () -> Unit,
    onEqualClick: () -> Unit
) {
    val rows = listOf(
        listOf("C", "÷", "×", "DEL"),
        listOf("7", "8", "9", "-"),
        listOf("4", "5", "6", "+"),
        listOf("1", "2", "3", "="),
        listOf("00", "0", "000", "=")
    )

    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF111827))) {
        for (row in rows) {
            Row(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                for (key in row) {
                    val isAction = key in listOf("C", "÷", "×", "-", "+", "DEL", "=")
                    val bgColor = if (key == "=") Color(0xFF5B8CFF) else Color.Transparent
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(bgColor)
                            .clickable {
                                when (key) {
                                    "DEL" -> onDeleteClick()
                                    "C" -> onClearClick()
                                    "=" -> onEqualClick()
                                    else -> onNumberClick(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            color = if (isAction && key != "=") Color(0xFF8CCBFF) else Color.White,
                            fontSize = 20.sp,
                            fontWeight = if (isAction) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

fun evaluateMath(expr: String): Long {
    try {
        val sanitized = expr.replace("×", "*").replace("÷", "/")
        val parts = sanitized.split(Regex("(?<=[-+*/])|(?=[-+*/])"))
        if (parts.isEmpty()) return 0L
        var result = parts[0].toLongOrNull() ?: 0L
        var currentOp = ""
        for (i in 1 until parts.size) {
            val part = parts[i]
            if (part in listOf("+", "-", "*", "/")) {
                currentOp = part
            } else {
                val num = part.toLongOrNull() ?: 0L
                when (currentOp) {
                    "+" -> result += num
                    "-" -> result -= num
                    "*" -> result *= num
                    "/" -> if (num != 0L) result /= num
                }
            }
        }
        return result
    } catch (_: Exception) {
        return 0L
    }
}
