package com.example.famekodriver.customer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.data.repository.OrderRepository
import com.example.famekodriver.customer.ui.theme.BoltGreen
import com.example.famekodriver.customer.ui.theme.BoltLightGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionsScreen(onBack: () -> Unit) {
    val repository = remember { OrderRepository() }
    val focusManager = LocalFocusManager.current
    var promos by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        repository.getPromotions().onSuccess {
            promos = it
            isLoading = false
        }.onFailure {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Promotions", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BoltGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    },
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Enter promo code") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BoltLightGray,
                            unfocusedContainerColor = BoltLightGray,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        trailingIcon = {
                            Text(
                                "Apply",
                                color = BoltGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable { }
                            )
                        }
                    )
                }
                items(promos) { promo ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BoltGreen.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, BoltGreen.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(promo["title"].toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(promo["description"].toString(), fontSize = 12.sp, color = Color.DarkGray)
                                Spacer(Modifier.height(8.dp))
                                Text("Expires: ${promo["expiry"]}", fontSize = 10.sp, color = Color.Gray)
                            }
                            Box(
                                modifier = Modifier
                                    .background(BoltGreen, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(promo["code"].toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
