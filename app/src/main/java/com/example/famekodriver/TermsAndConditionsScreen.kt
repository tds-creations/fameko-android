package com.example.famekodriver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(
    onBack: () -> Unit,
    onAccept: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Watermark Logo Background
        Image(
            painter = painterResource(id = R.drawable.ic_fameko_driver_logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .size(280.dp)
                .alpha(0.04f),
            contentScale = ContentScale.Fit
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text("Terms and Conditions", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                            Text("Driver partner contract & fee schedules", fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    navigationIcon = {
                        if (onAccept == null) {
                            IconButton(onClick = onBack) {
                                Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = TermsConstants.FULL_TERMS_TEXT,
                            fontSize = 14.sp,
                            color = Color(0xFF334155),
                            lineHeight = 22.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
                
                if (onAccept != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Text("I Accept the Terms and Conditions", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
