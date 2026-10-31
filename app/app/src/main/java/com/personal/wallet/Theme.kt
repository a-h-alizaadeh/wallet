package com.personal.wallet

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** رنگ‌ها از روی ویدیوی اپ مرجع (Wallet by BudgetBakers) نمونه‌برداری شده‌اند. */
object WColors {
    val AppBar = Color(0xFF232323)
    val Page = Color(0xFF1B1B1B)
    val Card = Color(0xFF212121)
    val Section = Color(0xFF0F0F0F)
    val Line = Color(0xFF303030)
    val Blue = Color(0xFF5E97F6)
    val Muted = Color(0xFF9E9E9E)
    val Income = Color(0xFF1DB571)
    val Expense = Color(0xFFE53935)
    val ChipGray = Color(0xFF6D6D6D)
    val Keypad = Color(0xFF1B1B1B)
    val KeypadSide = Color(0xFF0F0F0F)
    val KeyText = Color(0xFFB0B0B0)
}

@Composable
fun WalletTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = WColors.Blue,
            onPrimary = Color.White,
            background = WColors.Page,
            onBackground = Color.White,
            surface = WColors.Card,
            onSurface = Color.White,
            surfaceVariant = WColors.Card,
            onSurfaceVariant = Color(0xFFBDBDBD)
        ),
        content = content
    )
}

// ───────────────────────────── قالب‌بندی ─────────────────────────────

fun formatNum(amount: Long): String = NumberFormat.getNumberInstance(Locale.US).format(amount)
fun formatIrr(amount: Long): String = "IRR " + formatNum(amount)
fun formatDate(ts: Long): String = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(ts))
fun formatTime(ts: Long): String = SimpleDateFormat("h:mm a", Locale.US).format(Date(ts))
fun formatShortDate(ts: Long): String = SimpleDateFormat("MMM d", Locale.US).format(Date(ts))

// ───────────────────────────── لیست‌های انتخابی (مقدار ذخیره‌شده → برچسب نمایشی) ─────────────────────────────

val PaymentTypes = listOf(
    "Cash" to "نقد",
    "Debit card" to "کارت بانکی",
    "Credit card" to "کارت اعتباری",
    "Bank transfer" to "انتقال بانکی",
    "Voucher" to "کوپن / ژتون",
    "Mobile payment" to "پرداخت موبایلی",
    "Web payment" to "پرداخت اینترنتی"
)

val RecordStatuses = listOf(
    "Cleared" to "انجام‌شده",
    "Uncleared" to "در انتظار",
    "Reconciled" to "تطبیق‌شده"
)

val Warranties = listOf(
    "None" to "ندارد",
    "6 months" to "۶ ماه",
    "1 year" to "۱ سال",
    "2 years" to "۲ سال",
    "3 years" to "۳ سال",
    "5 years" to "۵ سال",
    "10 years" to "۱۰ سال"
)

val AccountTypes = listOf(
    "Cash" to "نقد",
    "Bank account" to "حساب بانکی",
    "Credit card" to "کارت اعتباری",
    "Savings" to "پس‌انداز",
    "Investment" to "سرمایه‌گذاری"
)

fun labelOf(options: List<Pair<String, String>>, key: String): String =
    options.firstOrNull { it.first == key }?.second ?: key

// ───────────────────────────── آیکون‌ها ─────────────────────────────

/** کلید آیکون ذخیره‌شده در دیتابیس → آیکون. برای افزودن آیکون جدید فقط یک خط به این لیست اضافه کنید. */
val IconRegistry: Map<String, ImageVector> by lazy {
    linkedMapOf(
        "restaurant" to Icons.Filled.Restaurant,
        "fastfood" to Icons.Filled.Fastfood,
        "cafe" to Icons.Filled.LocalCafe,
        "grocery" to Icons.Filled.LocalGroceryStore,
        "shopping_bag" to Icons.Filled.ShoppingBag,
        "cart" to Icons.Filled.ShoppingCart,
        "home" to Icons.Filled.Home,
        "apartment" to Icons.Filled.Apartment,
        "electric" to Icons.Filled.Lightbulb,
        "wifi" to Icons.Filled.Wifi,
        "phone" to Icons.Filled.Smartphone,
        "tv" to Icons.Filled.Tv,
        "bus" to Icons.Filled.DirectionsBus,
        "car" to Icons.Filled.DirectionsCar,
        "taxi" to Icons.Filled.LocalTaxi,
        "train" to Icons.Filled.Train,
        "subway" to Icons.Filled.Subway,
        "gas" to Icons.Filled.LocalGasStation,
        "flight" to Icons.Filled.Flight,
        "hotel" to Icons.Filled.Hotel,
        "beach" to Icons.Filled.BeachAccess,
        "person" to Icons.Filled.Person,
        "favorite" to Icons.Filled.Favorite,
        "child" to Icons.Filled.ChildCare,
        "pets" to Icons.Filled.Pets,
        "school" to Icons.Filled.School,
        "book" to Icons.Filled.MenuBook,
        "book2" to Icons.Filled.Book,
        "language" to Icons.Filled.Language,
        "edit" to Icons.Filled.Edit,
        "brush" to Icons.Filled.Brush,
        "computer" to Icons.Filled.Computer,
        "games" to Icons.Filled.SportsEsports,
        "movie" to Icons.Filled.Movie,
        "music" to Icons.Filled.MusicNote,
        "headphones" to Icons.Filled.Headphones,
        "camera" to Icons.Filled.CameraAlt,
        "fitness" to Icons.Filled.FitnessCenter,
        "hospital" to Icons.Filled.LocalHospital,
        "pharmacy" to Icons.Filled.LocalPharmacy,
        "medical" to Icons.Filled.MedicalServices,
        "spa" to Icons.Filled.Spa,
        "laundry" to Icons.Filled.LocalLaundryService,
        "clothes" to Icons.Filled.Checkroom,
        "gift" to Icons.Filled.CardGiftcard,
        "redeem" to Icons.Filled.Redeem,
        "cake" to Icons.Filled.Cake,
        "build" to Icons.Filled.Build,
        "handyman" to Icons.Filled.Handyman,
        "work" to Icons.Filled.Work,
        "payments" to Icons.Filled.Payments,
        "savings" to Icons.Filled.Savings,
        "bank" to Icons.Filled.AccountBalance,
        "card" to Icons.Filled.CreditCard,
        "coins" to Icons.Filled.MonetizationOn,
        "money" to Icons.Filled.AttachMoney,
        "atm" to Icons.Filled.LocalAtm,
        "wallet" to Icons.Filled.AccountBalanceWallet,
        "trending" to Icons.Filled.TrendingUp,
        "receipt" to Icons.Filled.Receipt,
        "sell" to Icons.Filled.Sell,
        "public" to Icons.Filled.Public,
        "star" to Icons.Filled.Star,
        "category" to Icons.Filled.Category
    )
}

fun iconFor(key: String): ImageVector = IconRegistry[key] ?: Icons.Filled.Category

fun accountIcon(type: String): ImageVector = when (type) {
    "Bank account" -> Icons.Filled.AccountBalance
    "Credit card" -> Icons.Filled.CreditCard
    "Savings" -> Icons.Filled.Savings
    "Investment" -> Icons.Filled.TrendingUp
    else -> Icons.Filled.MonetizationOn
}
