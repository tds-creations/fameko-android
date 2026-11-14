package com.example.famekodriver.customer.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.famekodriver.customer.ui.theme.BoltDark
import com.example.famekodriver.customer.ui.theme.FamekoBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun UpgradedScheduleRideDialog(
    onDismissRequest: () -> Unit,
    onConfirmSchedule: (scheduledDateTime: String) -> Unit
) {
    var selectedStep by remember { mutableIntStateOf(0) } // 0: Date, 1: Time
    
    val calendar = remember { Calendar.getInstance() }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis
    )
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE)
    )

    // Formatted date helper
    val formattedDate = remember(datePickerState.selectedDateMillis) {
        val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(millis))
    }

    // Formatted time helper
    val formattedTime = remember(timePickerState.hour, timePickerState.minute) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
            set(Calendar.MINUTE, timePickerState.minute)
        }
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = FamekoBlue.copy(alpha = 0.1f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selectedStep == 0) Icons.Default.Event else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = FamekoBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Schedule a Ride",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = BoltDark
                            )
                            Text(
                                text = if (selectedStep == 0) "Step 1 of 2 • Select Date" else "Step 2 of 2 • Select Time",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Step Segmented Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selectedStep == 0) FamekoBlue else Color.Transparent)
                            .clickable { selectedStep = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                null,
                                tint = if (selectedStep == 0) Color.White else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "1. Date",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedStep == 0) Color.White else Color.Gray
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selectedStep == 1) FamekoBlue else Color.Transparent)
                            .clickable { selectedStep = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                null,
                                tint = if (selectedStep == 1) Color.White else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "2. Time",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedStep == 1) Color.White else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Step Content View
                AnimatedContent(
                    targetState = selectedStep,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ScheduleStep"
                ) { step ->
                    if (step == 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Quick Date Shortcuts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickDateChip(
                                    label = "Today",
                                    isSelected = isSameDay(datePickerState.selectedDateMillis, 0),
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        datePickerState.selectedDateMillis = cal.timeInMillis
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickDateChip(
                                    label = "Tomorrow",
                                    isSelected = isSameDay(datePickerState.selectedDateMillis, 1),
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        cal.add(Calendar.DAY_OF_YEAR, 1)
                                        datePickerState.selectedDateMillis = cal.timeInMillis
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickDateChip(
                                    label = "In 2 Days",
                                    isSelected = isSameDay(datePickerState.selectedDateMillis, 2),
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        cal.add(Calendar.DAY_OF_YEAR, 2)
                                        datePickerState.selectedDateMillis = cal.timeInMillis
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom Styled DatePicker
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                DatePicker(
                                    state = datePickerState,
                                    showModeToggle = false,
                                    colors = DatePickerDefaults.colors(
                                        containerColor = Color.White,
                                        headlineContentColor = BoltDark,
                                        titleContentColor = Color.Gray,
                                        weekdayContentColor = Color(0xFF64748B),
                                        subheadContentColor = BoltDark,
                                        yearContentColor = BoltDark,
                                        currentYearContentColor = FamekoBlue,
                                        selectedYearContentColor = Color.White,
                                        selectedYearContainerColor = FamekoBlue,
                                        dayContentColor = BoltDark,
                                        selectedDayContainerColor = FamekoBlue,
                                        selectedDayContentColor = Color.White,
                                        todayDateBorderColor = FamekoBlue,
                                        todayContentColor = FamekoBlue
                                    )
                                )
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Quick Time Preset Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickTimeChip(
                                    label = "+15 mins",
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        cal.add(Calendar.MINUTE, 15)
                                        timePickerState.hour = cal.get(Calendar.HOUR_OF_DAY)
                                        timePickerState.minute = cal.get(Calendar.MINUTE)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickTimeChip(
                                    label = "+30 mins",
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        cal.add(Calendar.MINUTE, 30)
                                        timePickerState.hour = cal.get(Calendar.HOUR_OF_DAY)
                                        timePickerState.minute = cal.get(Calendar.MINUTE)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickTimeChip(
                                    label = "+1 hour",
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        cal.add(Calendar.HOUR_OF_DAY, 1)
                                        timePickerState.hour = cal.get(Calendar.HOUR_OF_DAY)
                                        timePickerState.minute = cal.get(Calendar.MINUTE)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom Styled TimePicker
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TimePicker(
                                        state = timePickerState,
                                        colors = TimePickerDefaults.colors(
                                            clockDialColor = Color.White,
                                            clockDialSelectedContentColor = Color.White,
                                            clockDialUnselectedContentColor = BoltDark,
                                            selectorColor = FamekoBlue,
                                            containerColor = Color.White,
                                            periodSelectorBorderColor = FamekoBlue.copy(alpha = 0.3f),
                                            periodSelectorSelectedContainerColor = FamekoBlue,
                                            periodSelectorSelectedContentColor = Color.White,
                                            periodSelectorUnselectedContainerColor = Color.White,
                                            periodSelectorUnselectedContentColor = BoltDark,
                                            timeSelectorSelectedContainerColor = FamekoBlue.copy(alpha = 0.15f),
                                            timeSelectorSelectedContentColor = FamekoBlue,
                                            timeSelectorUnselectedContainerColor = Color.White,
                                            timeSelectorUnselectedContentColor = BoltDark
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Schedule Summary Banner Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Scheduled Pickup",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                            Text(
                                text = "$formattedDate at $formattedTime",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF064E3B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dialog Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (selectedStep == 1) {
                                selectedStep = 0
                            } else {
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = if (selectedStep == 1) "Back" else "Cancel",
                            fontWeight = FontWeight.Bold,
                            color = BoltDark
                        )
                    }

                    Button(
                        onClick = {
                            if (selectedStep == 0) {
                                selectedStep = 1
                            } else {
                                // Formatted scheduled date time string
                                val dateStr = datePickerState.selectedDateMillis?.let {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                                } ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                
                                val timeStr = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                                val scheduledTimeStr = "$dateStr $timeStr"
                                onConfirmSchedule(scheduledTimeStr)
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (selectedStep == 0) "Next: Select Time" else "Confirm Schedule",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = if (selectedStep == 0) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickDateChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) FamekoBlue else Color(0xFFF1F5F9),
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else BoltDark
            )
        }
    }
}

@Composable
private fun QuickTimeChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFEFF6FF),
        border = BorderStroke(1.dp, FamekoBlue.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = FamekoBlue
            )
        }
    }
}

private fun isSameDay(dateMillis: Long?, daysOffset: Int): Boolean {
    if (dateMillis == null) return false
    val cal1 = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val cal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, daysOffset) }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
