package com.example.famekodriver

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onBack: () -> Unit,
    viewModel: EarningsViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: MoMo/Manual, 1: Instructions

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activate Daily Service", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
        ) {
            // Amount Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF004E89))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Service Fee Amount", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text(
                        "₵${viewModel.dailyFeeAmount.toInt()}.00",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Active for 24 Hours",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Payment Instructions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.top_up_step_1), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text("Merchant: Fameko Limited", color = Color.Gray, fontSize = 13.sp)
                        
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        Spacer(Modifier.height(12.dp))
                        
                        Text(stringResource(R.string.top_up_step_2), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        
                        if (viewModel.manualReference.isEmpty()) {
                            Button(
                                onClick = { viewModel.generateReference() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004E89)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Generate Reference Code")
                            }
                        } else {
                            OutlinedTextField(
                                value = viewModel.manualReference,
                                onValueChange = { viewModel.manualReference = it },
                                label = { Text("Transaction Reference") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Enter 10-digit ID") },
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.Receipt, null, tint = Color(0xFF004E89)) },
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.generateReference() }) {
                                        Icon(Icons.Default.Refresh, null)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        if (viewModel.manualReference.isBlank()) {
                            Toast.makeText(context, "Please enter the reference ID", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.submitManualDailyFee(
                            onSuccess = {
                                Toast.makeText(context, "Payment submitted for approval!", Toast.LENGTH_LONG).show()
                                onBack()
                            },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745)),
                    enabled = !viewModel.isPaying
                ) {
                    if (viewModel.isPaying) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("I HAVE PAID", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                // Security Notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFFFD7E14))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Once submitted, our team will verify your payment within 5-10 minutes.",
                            fontSize = 12.sp,
                            color = Color(0xFF856404)
                        )
                    }
                }
            }
        }
    }
}
