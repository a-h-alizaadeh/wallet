package com.personal.wallet

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    accounts: List<AccountEntity>,
    records: List<RecordEntity>,
    categories: List<CategoryEntity>,
    templates: List<TemplateEntity>,
    selectedIds: Set<Int>,
    period: Period,
    onPeriodChange: (Period) -> Unit,
    onSelectOnly: (Int) -> Unit,
    onToggle: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onOpenDrawer: () -> Unit,
    onAddAccount: () -> Unit,
    onOpenAccounts: () -> Unit,
    onAccountDetail: () -> Unit,
    onRecords: () -> Unit,
    onNewRecord: () -> Unit,
    onTransfer: () -> Unit,
    onTemplate: (TemplateEntity) -> Unit,
    onSpending: () -> Unit,
    onExport: () -> Unit,
    onCashflow: () -> Unit,
    onTemplates: () -> Unit,
    onNotifications: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var hideAmounts by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(WColors.Page)) {
        Column(Modifier.fillMaxSize()) {
            WalletAppBar(
                title = "خانه",
                navIcon = Icons.Filled.Menu,
                onNavClick = onOpenDrawer,
                actions = {
                    IconButton(onClick = { hideAmounts = !hideAmounts }) {
                        Icon(if (hideAmounts) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = Color.White)
                    }
                    IconButton(onClick = onNotifications) {
                        Icon(Icons.Filled.Notifications, null, tint = Color.White)
                    }
                }
            )
            // تب‌ها
            Row(Modifier.fillMaxWidth().background(WColors.AppBar)) {
                listOf("حساب‌ها", "بودجه و اهداف").forEachIndexed { i, label ->
                    Column(
                        Modifier.weight(1f).clickable { tab = i }.padding(top = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            label,
                            color = if (tab == i) Color.White else WColors.Muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(
                            Modifier
                                .width(48.dp)
                                .height(2.dp)
                                .background(if (tab == i) Color.White else Color.Transparent)
                        )
                    }
                }
            }

            if (tab == 0) {
                AccountsTab(
                    accounts, records, categories, templates, selectedIds, period, hideAmounts,
                    onPeriodChange, onSelectOnly, onToggle, onSelectAll, onAddAccount, onOpenAccounts,
                    onAccountDetail, onRecords, onSpending, onExport, onCashflow, onTemplates
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Filled.Savings,
                        title = "بودجه و اهداف",
                        message = "این بخش هنوز در برنامه فعال نشده است."
                    )
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountsTab(
    accounts: List<AccountEntity>,
    records: List<RecordEntity>,
    categories: List<CategoryEntity>,
    templates: List<TemplateEntity>,
    selectedIds: Set<Int>,
    period: Period,
    hideAmounts: Boolean,
    onPeriodChange: (Period) -> Unit,
    onSelectOnly: (Int) -> Unit,
    onToggle: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onAddAccount: () -> Unit,
    onOpenAccounts: () -> Unit,
    onAccountDetail: () -> Unit,
    onRecords: () -> Unit,
    onSpending: () -> Unit,
    onExport: () -> Unit,
    onCashflow: () -> Unit,
    onTemplates: () -> Unit
) {
    fun money(v: Long): String = if (hideAmounts) "IRR ••••••" else formatIrr(v)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
        // ── حساب‌های من ──
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("حساب‌های من", color = Color.White, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF3C3C3C), RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenAccounts),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = WColors.Blue, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(10.dp))

        val cells: List<@Composable (Modifier) -> Unit> = buildList {
            accounts.forEach { acc ->
                add { m ->
                    AccountChip(
                        account = acc,
                        selected = acc.id in selectedIds,
                        amountText = money(acc.balance),
                        modifier = m,
                        onClick = { onSelectOnly(acc.id) },
                        onLongClick = { onToggle(acc.id) }
                    )
                }
            }
            add { m ->
                Row(
                    modifier = m
                        .height(46.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF444444), RoundedCornerShape(6.dp))
                        .clickable(onClick = onAddAccount),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("افزودن حساب", color = WColors.Blue, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.Add, null, tint = WColors.Blue, modifier = Modifier.size(18.dp))
                }
            }
        }
        cells.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { cell -> cell(Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Text(
            "انتخاب همه",
            color = WColors.Blue,
            fontSize = 13.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onSelectAll)
                .padding(16.dp)
        )

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            PillButton("جزئیات حساب", null, onAccountDetail)
            PillButton("تراکنش‌ها", Icons.AutoMirrored.Filled.List, onRecords)
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).background(WColors.Section))

        // ── میانبرها ──
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickChip("هزینه‌ها", Icons.Filled.PieChart, Color(0xFF26C6DA), onSpending)
            QuickChip("خروجی", Icons.Filled.Share, Color(0xFFE0E0E0), onExport)
            QuickChip("جریان نقدی", Icons.Filled.BarChart, Color(0xFF26C6DA), onCashflow)
            QuickChip("قالب‌ها", Icons.Filled.ContentCopy, Color(0xFFE0E0E0), onTemplates)
        }

        // ── جمع موجودی ──
        val total = accounts.filter { it.id in selectedIds }.sumOf { it.balance }
        Column(
            Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(WColors.Card)
                .padding(16.dp)
        ) {
            Text("جمع موجودی حساب‌های انتخاب‌شده", color = WColors.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(money(total), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(12.dp))

        // ── داشبورد ──
        val dash = remember(accounts, records, selectedIds, period) {
            computeDashboard(accounts, records, selectedIds, period)
        }
        Column(
            Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(WColors.Card)
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("داشبورد کیف پول", color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("عملکرد ${period.label} و روند کلی", color = WColors.Muted, fontSize = 13.sp)
                }
                Icon(Icons.Filled.MoreVert, null, tint = WColors.Muted, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().background(Color(0xFF262626)).padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text("تمام مبالغ به ریال نمایش داده می‌شوند.", color = WColors.Muted, fontSize = 12.sp)
            }

            val pagerState = rememberPagerState(pageCount = { 3 })
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                when (page) {
                    0 -> Row(
                        Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        GaugeBlock("موجودی", money(dash.balance), dash.balanceFraction)
                        GaugeBlock("جریان نقدی", money(dash.cashflow), dash.cashFraction)
                    }
                    1 -> Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("درآمد و هزینه", color = Color.White, fontSize = 15.sp)
                        Spacer(Modifier.height(14.dp))
                        val maxV = maxOf(dash.income, dash.expense, 1L).toFloat()
                        BarLine("درآمد", money(dash.income), dash.income / maxV, WColors.Income)
                        Spacer(Modifier.height(12.dp))
                        BarLine("هزینه", money(dash.expense), dash.expense / maxV, WColors.Expense)
                    }
                    else -> Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("پرهزینه‌ترین دسته‌بندی‌ها", color = Color.White, fontSize = 15.sp)
                        Spacer(Modifier.height(12.dp))
                        if (dash.topCategories.isEmpty()) {
                            Text("در این بازه هزینه‌ای ثبت نشده است.", color = WColors.Muted, fontSize = 13.sp)
                        } else {
                            val top = dash.topCategories.first().second.toFloat().coerceAtLeast(1f)
                            dash.topCategories.forEach { (name, sum) ->
                                val c = categories.firstOrNull { it.name == name }
                                BarLine(name, money(sum), sum / top, Color(c?.colorHex ?: 0xFF78909C))
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.Center) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .padding(3.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (pagerState.currentPage == i) WColors.Blue else Color(0xFF555555))
                    )
                }
            }
            PeriodSelector(
                selected = period,
                onSelect = onPeriodChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun PillButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, Color(0xFF555555), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = WColors.Blue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = Color(0xFFEDEDED), fontSize = 13.sp)
    }
}

@Composable
private fun QuickChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF262628))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color(0xFFEDEDED), fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun GaugeBlock(title: String, valueText: String, fraction: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(150.dp)) {
        Gauge(fraction, Modifier.size(124.dp))
        Spacer(Modifier.height(6.dp))
        Text(title, color = WColors.Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(valueText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BarLine(label: String, valueText: String, fraction: Float, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFFE0E0E0), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(valueText, color = Color.White, fontSize = 13.sp, maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF333333))) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
