package com.example.famekodriver.customer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.customer.ui.theme.*

@Composable
fun PackageDetailsSheet(
    category: String,
    weightSize: String,
    isFragile: Boolean,
    recipientName: String,
    recipientPhone: String,
    notes: String,
    onCategoryChange: (String) -> Unit,
    onWeightSizeChange: (String) -> Unit,
    onFragileChange: (Boolean) -> Unit,
    onRecipientNameChange: (String) -> Unit,
    onRecipientPhoneChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    isPlacing: Boolean
) {
    val categories = listOf("Document", "Food", "Clothing", "Electronics", "Gifts", "Other")
    val sizes = listOf("Small", "Medium", "Large")
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Delivery Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("What are you sending?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { onCategoryChange(cat) },
                    label = { Text(cat) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FamekoBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Package Size", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sizes.forEach { size ->
                        val isSelected = weightSize == size
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable { onWeightSizeChange(size) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) FamekoBlue else BoltLightGray,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(size, color = if (isSelected) Color.White else BoltDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Fragile?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Switch(
                    checked = isFragile,
                    onCheckedChange = onFragileChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BoltOrange)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Recipient Information", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = recipientName,
            onValueChange = onRecipientNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Recipient Name") },
            placeholder = { Text("e.g. John Doe") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.Gray) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = recipientPhone,
            onValueChange = onRecipientPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Recipient Phone") },
            placeholder = { Text("e.g. 0244000000") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color.Gray) }
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Delivery instructions (optional)") },
            shape = RoundedCornerShape(12.dp),
            minLines = 2,
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue),
            enabled = !isPlacing && recipientName.isNotEmpty() && recipientPhone.isNotEmpty()
        ) {
            if (isPlacing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Confirm Delivery", fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
    }
}
