package com.personal.wallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.wallet.InitialPersonalData.defaultAccounts
import java.text.NumberFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var hideAmounts by remember { mutableStateOf(false) }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    MainWalletScreen(
                        hideAmounts = hideAmounts,
                        onToggleHideAmounts = { hideAmounts = !hideAmounts }
                    )
                }
            }
        }
    }
}

fun formatIrr(amount: Long, hide: Boolean): String {
    if (hide) return "IRR •••••••"
    return "IRR " + NumberFormat.getNumberInstance(Locale.US).format(amount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWalletScreen(hideAmounts: Boolean, onToggleHideAmounts: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home", color = Color.White) },
                actions = {
                    IconButton(onClick = onToggleHideAmounts) {
                        Icon(
                            imageVector = if (hideAmounts) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Hide Amounts",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* ثبت تراکنش جدید */ },
                containerColor = Color(0xFF4285F4),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // تب‌های بالا
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                TextButton(onClick = { selectedTab = 0 }) {
                    Text("Accounts", color = if (selectedTab == 0) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { selectedTab = 1 }) {
                    Text("Budgets & Goals", color = if (selectedTab == 1) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("My Accounts in Wallet", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // نمایش کارت حساب‌های ملت، ملی، تجارت
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(150.dp)
            ) {
                items(defaultAccounts) { account ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(account.colorHex)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text(account.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = formatIrr(account.balance, hideAmounts),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
