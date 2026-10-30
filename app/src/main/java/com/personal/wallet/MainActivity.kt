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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

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
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    dao.getAllAccounts().collectLatest { dbAccounts ->
                        if (dbAccounts.isNotEmpty()) accountsState.value = dbAccounts
                    }
                }

                LaunchedEffect(Unit) {
                    dao.getAllCategories().collectLatest { dbCats ->
                        if (dbCats.isNotEmpty()) categoriesState.value = dbCats
                    }
                }

                LaunchedEffect(Unit) {
                    dao.getAllRecords().collectLatest { dbRecords ->
                        recordsState.value = dbRecords
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = Color(0xFF1E1E1E),
                            modifier = Modifier.width(280.dp)
                        ) {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "مدیریت مالی شخصی",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                            Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                            Spacer(Modifier.height(8.dp))
                            DrawerItem("خانه", Icons.Default.Home) {
                                scope.launch { drawerState.close() }
                                currentScreen = "HOME"
                            }
                            DrawerItem("تراکنش‌ها و ریز حساب‌ها", Icons.Default.List) {
                                scope.launch { drawerState.close() }
                                currentScreen = "RECORDS"
                            }
                            DrawerItem("حساب‌های بانکی", Icons.Default.CheckCircle) {
                                scope.launch { drawerState.close() }
                                currentScreen = "HOME"
                            }
                            Spacer(Modifier.weight(1f))
                            Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                            Text(
                                "نسخه ۱.۱ - پویا و هوشمند",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                        when (currentScreen) {
                            "HOME" -> WalletMainScreen(
                                accounts = accountsState.value,
                                recentRecords = recordsState.value.take(5),
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onNavigateToAdd = { currentScreen = "ADD_RECORD" },
                                onNavigateToRecords = { currentScreen = "RECORDS" }
                            )
                            "RECORDS" -> RecordsScreen(
                                records = recordsState.value,
                                accounts = accountsState.value,
                                onBack = { currentScreen = "HOME" }
                            )
                            "ADD_RECORD" -> AddRecordScreen(
                                accounts = accountsState.value,
                                categories = categoriesState.value.map { it.name },
                                onBack = { currentScreen = "HOME" },
                                onSave = { amount, type, fromAccId, targetAccId, category ->
                                    scope.launch {
                                        val fromAccount = accountsState.value.find { it.id == fromAccId }
                                        if (fromAccount != null && amount > 0) {
                                            when (type) {
                                                "EXPENSE" -> {
                                                    dao.updateAccount(fromAccount.copy(balance = fromAccount.balance - amount))
                                                    dao.insertRecord(
                                                        RecordEntity(amount = amount, type = "EXPENSE", accountId = fromAccId, categoryName = category)
                                                    )
                                                }
                                                "INCOME" -> {
                                                    dao.updateAccount(fromAccount.copy(balance = fromAccount.balance + amount))
                                                    dao.insertRecord(
                                                        RecordEntity(amount = amount, type = "INCOME", accountId = fromAccId, categoryName = category)
                                                    )
                                                }
                                                "TRANSFER" -> {
                                                    val targetAccount = accountsState.value.find { it.id == targetAccId }
                                                    if (targetAccount != null && fromAccId != targetAccId) {
                                                        dao.updateAccount(fromAccount.copy(balance = fromAccount.balance - amount))
                                                        dao.updateAccount(targetAccount.copy(balance = targetAccount.balance + amount))
                                                        dao.insertRecord(
                                                            RecordEntity(amount = amount, type = "TRANSFER", accountId = fromAccId, targetAccountId = targetAccId, categoryName = "انتقال بین‌بانکی")
                                                        )
                                                    }
                                                }
                                            }
                                            currentScreen = "HOME"
                                        }
                                    }
                                },
                                onAddNewAccount = { name, balance, colorHex ->
                                    scope.launch {
                                        dao.insertAccount(AccountEntity(name = name, balance = balance, colorHex = colorHex))
                                    }
                                },
                                onAddNewCategory = { name ->
                                    scope.launch {
                                        dao.insertCategory(CategoryEntity(name = name))
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
        Spacer(Modifier.width(16.dp))
        Text(text, color = Color.White, fontSize = 15.sp)
    }
}

fun formatIrr(amount: Long): String = "IRR " + NumberFormat.getNumberInstance(Locale.US).format(amount)

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletMainScreen(
    accounts: List<AccountEntity>,
    recentRecords: List<RecordEntity>,
    onOpenDrawer: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToRecords: () -> Unit
) {
    var hideAmounts by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text("کیف پول من", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White) }
                },
                actions = {
                    IconButton(onClick = { hideAmounts = !hideAmounts }) {
                        Icon(imageVector = if (hideAmounts) Icons.Default.Lock else Icons.Default.CheckCircle, contentDescription = "Toggle Balance", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd, containerColor = Color(0xFF4285F4), contentColor = Color.White, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add Record")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "حساب‌های من", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = "مشاهده همه", color = Color(0xFF4285F4), fontSize = 13.sp, modifier = Modifier.clickable { onNavigateToRecords() })
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(170.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(account.colorHex)), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(account.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.Home, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Text(text = if (hideAmounts) "IRR •••••••" else formatIrr(account.balance), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onNavigateToRecords, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Icon(Icons.Default.List, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ریز تراکنش‌ها", fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "آخرین فعالیت‌ها", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            if (recentRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("هنوز تراکنشی ثبت نشده است.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recentRecords, key = { it.id }) { rec -> RecordItemRow(rec = rec, accounts = accounts) }
                }
            }
        }
    }
}

@Composable
fun RecordItemRow(rec: RecordEntity, accounts: List<AccountEntity>) {
    val accName = accounts.find { it.id == rec.accountId }?.name ?: "نامشخص"
    val targetAccName = accounts.find { it.id == rec.targetAccountId }?.name ?: ""

    val (sign, colorText, title) = when (rec.type) {
        "EXPENSE" -> Triple("-", Color(0xFFE57373), "${rec.categoryName} ($accName)")
        "INCOME" -> Triple("+", Color(0xFF81C784), "${rec.categoryName} ($accName)")
        else -> Triple("⇄", Color(0xFF64B5F6), "انتقال: $accName به $targetAccName")
    }

    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(formatDate(rec.timestamp), color = Color.Gray, fontSize = 11.sp)
            }
            Text(text = "$sign ${formatIrr(rec.amount)}", color = colorText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(records: List<RecordEntity>, accounts: List<AccountEntity>, onBack: () -> Unit) {
    BackHandler { onBack() }
    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text("تاریخچه کامل تراکنش‌ها", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("هیچ رکوردی یافت نشد.", color = Color.Gray) }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(records, key = { it.id }) { rec -> RecordItemRow(rec = rec, accounts = accounts) }
                }
            }
        }
    }
}

@Composable
fun AddRecordScreen(
    accounts: List<AccountEntity>,
    categories: List<String>,
    onBack: () -> Unit,
    onSave: (Long, String, Int, Int?, String) -> Unit,
    onAddNewAccount: (String, Long, Long) -> Unit,
    onAddNewCategory: (String) -> Unit
) {
    BackHandler { onBack() }

    var expression by remember { mutableStateOf("0") }
    var transactionType by remember { mutableStateOf("EXPENSE") }
    var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1) }
    var targetAccId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: 2) }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "عمومی") }

    var showAccountDialog by remember { mutableStateOf(false) }
    var showTargetAccountDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    
    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    val bgColor = when (transactionType) {
        "EXPENSE" -> Color(0xFFB71C1C)
        "INCOME" -> Color(0xFF1B5E20)
        else -> Color(0xFF0D47A1)
    }

    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White) }
            IconButton(onClick = {
                val finalAmount = evaluateMath(expression)
                if (finalAmount > 0) onSave(finalAmount, transactionType, selectedAccId, targetAccId, selectedCategory)
            }) { Icon(Icons.Default.Check, contentDescription = "Confirm", tint = Color.White) }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            TabTypeButton("هزینه", transactionType == "EXPENSE") { transactionType = "EXPENSE" }
            TabTypeButton("درآمد", transactionType == "INCOME") { transactionType = "INCOME" }
            TabTypeButton("انتقال", transactionType == "TRANSFER") { transactionType = "TRANSFER" }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.End) {
            Text(text = expression, color = Color.White, fontSize = 50.sp, fontWeight = FontWeight.Light, maxLines = 1)
            Text(text = "ریال (IRR)", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.25f)).padding(vertical = 12.dp, horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceAround) {
            val currentAccName = accounts.find { it.id == selectedAccId }?.name ?: "انتخاب حساب"
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showAccountDialog = true }) {
                Text(text = if (transactionType == "TRANSFER") "حساب مبدأ" else "حساب", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Text(text = currentAccName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            if (transactionType == "TRANSFER") {
                val targetName = accounts.find { it.id == targetAccId }?.name ?: "انتخاب مقصد"
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showTargetAccountDialog = true }) {
                    Text(text = "حساب مقصد", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text(text = targetName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showCategoryDialog = true }) {
                    Text(text = "دسته‌بندی", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text(text = selectedCategory, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        CalculatorPad(
            onNumberClick = { num -> expression = if (expression == "0") num else expression + num },
            onDeleteClick = { expression = if (expression.length > 1) expression.dropLast(1) else "0" },
            onClearClick = { expression = "0" },
            onEqualClick = { expression = evaluateMath(expression).toString() }
        )
    }

    if (showAccountDialog) {
        SimpleListSelectionDialog(
            title = "انتخاب حساب",
            items = accounts.map { it.name },
            allowAdd = true,
            onAddClick = { showAccountDialog = false; showCreateAccountDialog = true },
            onDismiss = { showAccountDialog = false },
            onSelect = { selectedName ->
                accounts.find { it.name == selectedName }?.let { selectedAccId = it.id }
                showAccountDialog = false
            }
        )
    }

    if (showTargetAccountDialog) {
        SimpleListSelectionDialog(
            title = "انتخاب حساب مقصد",
            items = accounts.filter { it.id != selectedAccId }.map { it.name },
            allowAdd = true,
            onAddClick = { showTargetAccountDialog = false; showCreateAccountDialog = true },
            onDismiss = { showTargetAccountDialog = false },
            onSelect = { selectedName ->
                accounts.find { it.name == selectedName }?.let { targetAccId = it.id }
                showTargetAccountDialog = false
            }
        )
    }

    if (showCategoryDialog) {
        SimpleListSelectionDialog(
            title = "انتخاب دسته‌بندی",
            items = categories,
            allowAdd = true,
            onAddClick = { showCategoryDialog = false; showCreateCategoryDialog = true },
            onDismiss = { showCategoryDialog = false },
            onSelect = { selectedCategory = it; showCategoryDialog = false }
        )
    }
    
    if (showCreateAccountDialog) {
        var newAccName by remember { mutableStateOf("") }
        var newAccBalance by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateAccountDialog = false },
            title = { Text("افزودن حساب جدید", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newAccName, onValueChange = { newAccName = it },
                        label = { Text("نام حساب", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newAccBalance, onValueChange = { newAccBalance = it },
                        label = { Text("موجودی اولیه (ریال)", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newAccName.isNotBlank()) {
                        val bal = newAccBalance.toLongOrNull() ?: 0L
                        val colors = listOf(0xFFD32F2F, 0xFF1976D2, 0xFF388E3C, 0xFFFBC02D, 0xFF8E24AA, 0xFFF57C00, 0xFF0288D1, 0xFF009688)
                        onAddNewAccount(newAccName, bal, colors.random())
                    }
                    showCreateAccountDialog = false
                }) { Text("ذخیره", color = Color(0xFF4285F4)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAccountDialog = false }) { Text("انصراف", color = Color.Gray) }
            },
            containerColor = Color(0xFF252525)
        )
    }
    
    if (showCreateCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateCategoryDialog = false },
            title = { Text("دسته‌بندی جدید", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newCatName, onValueChange = { newCatName = it },
                    label = { Text("نام برچسب", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCatName.isNotBlank()) onAddNewCategory(newCatName)
                    showCreateCategoryDialog = false
                }) { Text("ذخیره", color = Color(0xFF4285F4)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCategoryDialog = false }) { Text("انصراف", color = Color.Gray) }
            },
            containerColor = Color(0xFF252525)
        )
    }
}

@Composable
fun TabTypeButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 16.sp,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
fun SimpleListSelectionDialog(
    title: String,
    items: List<String>,
    allowAdd: Boolean = false,
    onAddClick: () -> Unit = {},
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF252525),
        title = { Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                if (allowAdd) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onAddClick() }.padding(vertical = 14.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF4285F4))
                            Spacer(Modifier.width(8.dp))
                            Text("افزودن جدید...", color = Color(0xFF4285F4), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 0.5.dp)
                    }
                }
                items(items) { item ->
                    Text(
                        text = item,
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(item) }.padding(vertical = 12.dp, horizontal = 8.dp)
                    )
                    Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 0.5.dp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن", color = Color(0xFF4285F4)) }
        }
    )
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
    } catch (e: Exception) { return 0L }
}

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

    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF181818))) {
        for (row in rows) {
            Row(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                for (key in row) {
                    val isAction = key in listOf("C", "÷", "×", "-", "+", "DEL", "=")
                    val bgColor = if (key == "=") Color(0xFF4285F4) else Color.Transparent

                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().background(bgColor).clickable {
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
                            color = if (isAction && key != "=") Color(0xFF64B5F6) else Color.White,
                            fontSize = 20.sp,
                            fontWeight = if (isAction) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
