package com.example.biometricattendanceapp.feature_attendance.presentation.calendar

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    targetUserId: String? = null,
    viewModel: CalendarViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.refreshCalendarData()
    }
    val isAdmin = targetUserId != null
    val calendarDays by viewModel.calendarDays.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(targetUserId) { viewModel.initialize(targetUserId) }

    // --- UPDATED: We now store the whole Day object to determine the absence reason ---
    var writingNoteDay by remember { mutableStateOf<CalendarDay?>(null) }
    var noteInputText by remember { mutableStateOf("") }

    var viewingDayDetails by remember { mutableStateOf<CalendarDay?>(null) }
    var viewingHoliday by remember { mutableStateOf<CalendarDay?>(null) }
    var viewingAdminNote by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAdmin) "Employee Calendar" else "My Attendance") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) { Icon(Icons.Default.ChevronLeft, "Previous") }
                Text(text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.nextMonth() }, enabled = currentMonth.isBefore(java.time.YearMonth.now())) { Icon(Icons.Default.ChevronRight, "Next") }
            }

            // LEGEND
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween // Pushes items nicely to the edges
            ) {
                LegendItem(Color(0xFF2E7D32), "Present")
                LegendItem(Color(0xFF1976D2), "Active")
                LegendItem(Color(0xFFE65100), "Incomplete")
                LegendItem(Color(0xFFD32F2F), "Absent")
                LegendItem(Color(0xFFFBC02D), "Holiday")
            }

            // GRID
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(calendarDays) { day ->
                    CalendarCell(day = day) { clickedDay ->
                        val isPastDate = clickedDay.date?.isBefore(LocalDate.now()) == true
                        val completelyAbsent = clickedDay.log == null && clickedDay.status == DayStatus.ABSENT
                        val forgotToPunchOut = clickedDay.log != null && clickedDay.log.punchOutTime == null

                        when {
                            clickedDay.note != null -> viewingDayDetails = clickedDay
                            clickedDay.adminRemovalNote != null -> viewingAdminNote = clickedDay.adminRemovalNote
                            clickedDay.status == DayStatus.HOLIDAY -> viewingHoliday = clickedDay
                            !isAdmin && isPastDate && (completelyAbsent || forgotToPunchOut) -> writingNoteDay = clickedDay
                            clickedDay.log != null -> viewingDayDetails = clickedDay
                        }
                    }
                }
            }
        }
    }

    if (writingNoteDay != null) {
        val forgotToPunchOut = writingNoteDay!!.log != null && writingNoteDay!!.log?.punchOutTime == null
        val warningMessage = if (forgotToPunchOut) {
            "Incomplete Shift: You forgot to punch out. Please provide a reason."
        } else {
            "Absent: Please provide a reason for your absence."
        }

        AlertDialog(
            onDismissRequest = { writingNoteDay = null },
            title = { Text("Submit Absence Note") },
            text = {
                Column {
                    Text(
                        text = warningMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = noteInputText,
                        onValueChange = { noteInputText = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    writingNoteDay!!.date?.let { date ->
                        viewModel.submitAbsenceNote(date, noteInputText) {
                            Toast.makeText(context, "Note sent to HR", Toast.LENGTH_SHORT).show()
                            writingNoteDay = null
                            noteInputText = ""
                        }
                    }
                }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { writingNoteDay = null }) { Text("Cancel") } }
        )
    }
    if (viewingDayDetails != null) {
        val note = viewingDayDetails!!.note
        val log = viewingDayDetails!!.log
        val isApproved = note?.status == "approved"

        AlertDialog(
            onDismissRequest = { viewingDayDetails = null },
            title = { Text("Day Details - ${viewingDayDetails!!.date}") },
            text = {
                Column {
                    if (note != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Absence Note", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("\"${note.note}\"")
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Status: ${note.status.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isApproved) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (log != null) {
                        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val inTime = timeFormatter.format(Date(log.punchInTime))
                        val outTime = if (log.punchOutTime != null) timeFormatter.format(Date(log.punchOutTime!!)) else "Active Shift"

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "Location", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(log.officeName, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("IN: $inTime", fontWeight = FontWeight.Bold)
                                    Text("OUT: $outTime", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { viewingDayDetails = null }) { Text("Close") } },
            dismissButton = {
                if (!isAdmin && note != null && !isApproved) {
                    TextButton(
                        onClick = {
                            viewModel.deleteAbsenceNote(note.id) {
                                Toast.makeText(context, "Note Deleted", Toast.LENGTH_SHORT).show()
                                viewingDayDetails = null
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete Note") }
                }
            }
        )
    }

    // ADMIN REMOVAL NOTE DIALOG
    if (viewingAdminNote != null) {
        AlertDialog(
            onDismissRequest = { viewingAdminNote = null },
            title = { Text("Admin's Note") },
            text = { Text(viewingAdminNote!!, style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { Button(onClick = { viewingAdminNote = null }) { Text("Close") } }
        )
    }

    // HOLIDAY DIALOG
    if (viewingHoliday != null) {
        AlertDialog(
            onDismissRequest = { viewingHoliday = null },
            title = { Text("Holiday") },
            text = { Text(viewingHoliday!!.holidayName ?: "Weekend", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { Button(onClick = { viewingHoliday = null }) { Text("Close") } }
        )
    }
}

@Composable
fun CalendarCell(day: CalendarDay, onClick: (CalendarDay) -> Unit) {
    if (day.status == DayStatus.EMPTY || day.date == null) {
        Box(modifier = Modifier.aspectRatio(1f))
        return
    }

    val bgColor = when (day.status) {
        DayStatus.PRESENT -> Color(0xFF2E7D32)    // Green (Shift Complete)
        DayStatus.ACTIVE -> Color(0xFF1976D2)     // Blue (Currently Working)
        DayStatus.INCOMPLETE -> Color(0xFFE65100) // Orange (Forgot to punch out yesterday)
        DayStatus.ABSENT -> Color(0xFFD32F2F)     // Red (Didn't show up)
        DayStatus.HOLIDAY -> Color(0xFFFBC02D)    // Yellow
        else -> Color.Transparent
    }
    val txtColor = if (day.status == DayStatus.HOLIDAY) Color.Black else if (day.status != DayStatus.FUTURE) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(enabled = day.status != DayStatus.FUTURE) { onClick(day) },
        contentAlignment = Alignment.Center
    ) {
        Text(text = day.date.dayOfMonth.toString(), color = txtColor, fontWeight = if (day.status != DayStatus.FUTURE) FontWeight.Bold else FontWeight.Normal)

        // INDICATORS
        if (day.note != null) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).size(4.dp).clip(CircleShape).background(Color.White))
        } else if (day.adminRemovalNote != null) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).size(4.dp).clip(CircleShape).background(Color.Blue))
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}