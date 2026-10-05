package com.example.famekodriver.customer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.famekodriver.customer.ui.theme.BoltDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(
    onBack: () -> Unit,
    onAccept: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Watermark Logo Background
        Image(
            painter = painterResource(id = R.drawable.ic_fameko_logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .size(280.dp)
                .alpha(0.08f),
            contentScale = ContentScale.Fit
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Terms and Conditions", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (onAccept == null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                    .padding(20.dp)
            ) {
                Text(
                    text = TermsConstants.FULL_TERMS_TEXT,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily.SansSerif
                )
                
                if (onAccept != null) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.famekodriver.customer.ui.theme.FamekoBlue)
                    ) {
                        Text("I Accept the Terms and Conditions", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
