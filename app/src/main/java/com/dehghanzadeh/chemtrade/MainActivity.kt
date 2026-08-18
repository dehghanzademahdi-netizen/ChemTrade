package com.dehghanzadeh.chemtrade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Chemical(val name: String, val category: String, val price: String, val supplier: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChemTradeApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChemTradeApp() {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val chemicals = remember {
        listOf(
            Chemical("مونو اتانول آمین (MEA)", "حلال و افزودنی", "تماس برای قیمت", "بازرگانی دهقان‌زاده"),
            Chemical("اسید استیک", "اسیدها", "تماس برای قیمت", "تأمین‌کننده تأییدشده"),
            Chemical("متانول", "حلال‌ها", "تماس برای قیمت", "تأمین‌کننده تأییدشده"),
            Chemical("اوره صنعتی", "مواد اولیه کود", "تماس برای قیمت", "تأمین‌کننده تأییدشده")
        )
    }
    val filtered = chemicals.filter { it.name.contains(query, true) || it.category.contains(query, true) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ChemTrade") }, actions = {
                IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "جستجو") }
            })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = {}, label = { Text("بازار") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = {}, label = { Text("درخواست‌ها") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = {}, label = { Text("حساب من") })
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("بازار مواد اولیه", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("جستجوی ماده شیمیایی") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Text("قیمت و موجودی را از تأمین‌کنندگان تأییدشده دریافت کنید.", style = MaterialTheme.typography.bodyMedium)
            }
            items(filtered) { chemical ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(chemical.name, style = MaterialTheme.typography.titleMedium)
                        Text(chemical.category, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        Text(chemical.price)
                        Text(chemical.supplier, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = {}) { Text("مشاهده و درخواست خرید") }
                    }
                }
            }
        }
    }
}
