package com.example.famekodriver

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleRegistrationScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DriverRepository() }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var vehicleName by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") }
    var plateNumber by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("BOTH") }
    
    val vehicleTypes = listOf(
        "car" to "Taxi/Car",
        "okada" to "Motor Okada",
        "aboboyaa" to "Abobo Yaa",
        "praggia" to "Praggia",
        "truck" to "Truck",
        "bicycle" to "Bicycle"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register Your Vehicle", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Enter your vehicle details to start accepting rides and deliveries.", color = Color.Gray, fontSize = 14.sp)

            OutlinedTextField(
                value = vehicleName,
                onValueChange = { vehicleName = it },
                label = { Text("Vehicle Make/Name (e.g. Toyota Vitz)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = vehicleModel,
                onValueChange = { vehicleModel = it },
                label = { Text("Model Year") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = plateNumber,
                onValueChange = { plateNumber = it },
                label = { Text("Plate Number") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Text("Select Vehicle Type", fontWeight = FontWeight.Bold)
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                vehicleTypes.forEach { (id, label) ->
                    FilterChip(
                        selected = selectedType == id,
                        onClick = { selectedType = id },
                        label = { Text(label) }
                    )
                }
            }

            Text("Service Type", fontWeight = FontWeight.Bold)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedService == "RIDE_HAILING", onClick = { selectedService = "RIDE_HAILING" })
                    Text("Ride Hailing", modifier = Modifier.clickable { selectedService = "RIDE_HAILING" })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedService == "PACKAGE_DELIVERY", onClick = { selectedService = "PACKAGE_DELIVERY" })
                    Text("Package Delivery", modifier = Modifier.clickable { selectedService = "PACKAGE_DELIVERY" })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedService == "BOTH", onClick = { selectedService = "BOTH" })
                    Text("Both Services", modifier = Modifier.clickable { selectedService = "BOTH" })
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (vehicleName.isBlank() || plateNumber.isBlank() || selectedType.isBlank()) {
                        Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    scope.launch {
                        val driverId = sessionManager.getDriverId() ?: return@launch
                        // We need an API to update driver vehicle details
                        repository.updateDriverVehicle(
                            driverId = driverId,
                            type = selectedType,
                            number = plateNumber,
                            model = vehicleModel,
                            service = selectedService
                        ).onSuccess {
                            Toast.makeText(context, "Vehicle registered successfully!", Toast.LENGTH_SHORT).show()
                            onComplete()
                        }.onFailure {
                            Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004E89))
            ) {
                Text("Register Vehicle", modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
