package com.example.famekodriver.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.famekodriver.admin.ui.theme.FamekoPrimary
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.utils.ImageLinks
import kotlinx.coroutines.launch

class AdminLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager(this)
        
        // Redirect if already logged in (Admin role)
        if (sessionManager.isLoggedIn() && sessionManager.getDriverRole() == "ADMIN") {
            startActivity(Intent(this, AdminMapActivity::class.java))
            finish()
        }

        setContent {
            AdminLoginScreen { username, password ->
                lifecycleScope.launch {
                    val repository = DriverRepository.getInstance()
                    repository.adminLogin(username, password).onSuccess { admin ->
                        sessionManager.saveDriverSession(
                            admin.id.toString(),
                            admin.username,
                            "ADMIN"
                        )
                        Toast.makeText(this@AdminLoginActivity, "Welcome, ${admin.username}", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@AdminLoginActivity, AdminMapActivity::class.java))
                        finish()
                    }.onFailure {
                        Toast.makeText(this@AdminLoginActivity, "Login Failed: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

@Composable
fun AdminLoginScreen(onLogin: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = ImageLinks.IC_FAMEKO_LOGO,
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "FAMEKO ADMIN",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = FamekoPrimary,
                letterSpacing = 2.sp
            )
            Text(
                "Management Console",
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(48.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Person, null) }
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    val description = if (passwordVisible) "Hide password" else "Show password"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                }
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { onLogin(username, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamekoPrimary)
            ) {
                Text("Login to Console", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
