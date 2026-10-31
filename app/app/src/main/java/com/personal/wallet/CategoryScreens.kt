package com.personal.wallet

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ───────────────────────────── مدیریت دسته‌بندی‌ها ─────────────────────────────

@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onEdit: (CategoryEntity) -> Unit,
    onAdd: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(WColors.Page)) {
        Column(Modifier.fillMaxSize()) {
            WalletAppBar(title = "مدیریت دسته‌بندی‌ها", navIcon = Icons.AutoMirrored.Filled.ArrowBack, onNavClick = onBack)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                val tops = categories.filter { it.parentId == null }
                tops.forEach { parent ->
                    item(key = "c${parent.id}") {
                        CategoryRow(
                            parent,
                            trailing = { Icon(Icons.Filled.Edit, null, tint = WColors.Muted, modifier = Modifier.size(20.dp)) }
                        ) { onEdit(parent) }
                    }
                    categories.filter { it.parentId == parent.id }.forEach { child ->
                        item(key = "c${child.id}") {
                            Row(Modifier.padding(start = 32.dp)) {
                                Box(Modifier.weight(1f)) {
                                    CategoryRow(
                                        child,
                                        trailing = { Icon(Icons.Filled.Edit, null, tint = WColors.Muted, modifier = Modifier.size(20.dp)) }
                                    ) { onEdit(child) }
                                }
                            }
                        }
                    }
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

// ───────────────────────────── ساخت / ویرایش دسته‌بندی (نام، رنگ، آیکون، والد) ─────────────────────────────

@Composable
fun CategoryEditScreen(
    existing: CategoryEntity?,
    categories: List<CategoryEntity>,
    presetParentId: Int?,
    onBack: () -> Unit,
    onSave: (CategoryEntity, String?) -> Unit,
    onDelete: (CategoryEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var color by remember { mutableStateOf(existing?.colorHex ?: CategoryPalette[0]) }
    var iconKey by remember { mutableStateOf(existing?.iconKey ?: "category") }
    var parentId by remember { mutableStateOf(existing?.parentId ?: presetParentId) }
    var confirmDelete by remember { mutableStateOf(false) }

    // والد فقط می‌تواند یک دسته‌بندی سطح‌بالا باشد (به‌جز خودش)
    val parentOptions: List<Pair<String, String>> =
        listOf("0" to "بدون والد (دسته‌بندی اصلی)") +
            categories.filter { it.parentId == null && it.id != existing?.id }.map { it.id.toString() to it.name }
    val hasChildren = existing != null && categories.any { it.parentId == existing.id }

    Column(Modifier.fillMaxSize().background(WColors.Page)) {
        WalletAppBar(
            title = if (existing == null) "دسته‌بندی جدید" else "ویرایش دسته‌بندی",
            navIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavClick = onBack,
            actions = {
                if (existing != null) {
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, null, tint = Color.White) }
                }
                IconButton(onClick = {
                    val n = name.trim()
                    val duplicate = categories.any { it.name.equals(n, ignoreCase = true) && it.id != (existing?.id ?: -1) }
                    when {
                        n.isEmpty() -> Toast.makeText(context, "نام دسته‌بندی را وارد کنید", Toast.LENGTH_SHORT).show()
                        duplicate -> Toast.makeText(context, "این نام قبلاً استفاده شده است", Toast.LENGTH_SHORT).show()
                        else -> onSave(
                            CategoryEntity(
                                id = existing?.id ?: 0,
                                name = n,
                                colorHex = color,
                                iconKey = iconKey,
                                parentId = if (hasChildren) null else parentId
                            ),
                            existing?.name
                        )
                    }
                }) { Icon(Icons.Filled.Check, null, tint = Color.White) }
            }
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                CircleIcon(iconFor(iconKey), Color(color), size = 76.dp, iconSize = 40.dp)
            }
            UnderlineField("نام", name, { name = it }, placeholder = "مثلاً خوراک")
            if (!hasChildren) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    DropdownField(
                        label = "دسته‌بندی والد",
                        valueText = categories.firstOrNull { it.id == parentId }?.name ?: "بدون والد (دسته‌بندی اصلی)",
                        options = parentOptions,
                        onSelect = { parentId = it.toIntOrNull()?.takeIf { id -> id != 0 } }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("رنگ", color = WColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            Column(Modifier.padding(horizontal = 16.dp)) { ColorPicker(CategoryPalette, color) { color = it } }
            Spacer(Modifier.height(12.dp))
            Text("آیکون", color = WColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { IconPicker(iconKey, Color(color)) { iconKey = it } }
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "حذف دسته‌بندی",
            text = "دسته‌بندی «${existing.name}» حذف می‌شود. تراکنش‌های قبلی با همین نام باقی می‌مانند (با آیکون پیش‌فرض)." +
                if (hasChildren) " زیردسته‌های آن هم حذف می‌شوند." else "",
            confirmText = "حذف",
            onConfirm = { confirmDelete = false; onDelete(existing) },
            onDismiss = { confirmDelete = false }
        )
    }
}
