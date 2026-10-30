package com.personal.wallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val dao = database.walletDao()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val accountsState = remember { mutableStateOf(InitialPersonalData.defaultAccounts) }
                var hideAmounts by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    dao.getAllAccounts().collectLatest { dbAccounts ->
                        if (dbAccounts.isNotEmpty()) {
                            accountsState.value = dbAccounts
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    WalletMainScreen(
                        accounts = accountsState.value,
                        hideAmounts = hideAmounts,
                        onToggleHide = { hideAmounts = !hideAmounts }
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
fun WalletMainScreen(
    accounts: List<AccountEntity>,
    hideAmounts: Boolean,
    onToggleHide: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text("Home", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleHide) {
                        // در اینجا آیکون قفل جایگزین چشم شد
                        Icon(
                            imageVector = if (hideAmounts) Icons.Default.Lock else Icons.Default.CheckCircle,
                            contentDescription = "Toggle Amounts",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Accounts in Wallet",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF4285F4)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(150.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
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
                                // در اینجا آیکون خانه جایگزین کیف پول شد
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = account.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
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

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Account Detail", color = Color.White, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Records", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}
