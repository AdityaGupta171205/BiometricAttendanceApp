package com.example.biometricattendanceapp.feature_attendance.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onViewEmployeeCalendar: (String) -> Unit = {}
) {
    val pendingEmployees by viewModel.pendingEmployees.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()
    val employeesList by viewModel.employeesList.collectAsState()
    val pendingNotes by viewModel.pendingNotes.collectAsState()
    val globalHolidays by viewModel.globalHolidays.collectAsState()

    val officeClosingTime by viewModel.officeClosingTime.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var selectedEmployeeIdFilter by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HR Administration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // TAB NAVIGATION BAR
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Approvals (${pendingEmployees.size})", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Ledgers", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Notes (${pendingNotes.size})", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Holidays", fontWeight = FontWeight.Bold) })
            }

            when (selectedTab) {
                0 -> ApprovalsTab(pendingEmployees, viewModel)
                1 -> LedgersTab(recentLogs, employeesList, selectedEmployeeIdFilter, { selectedEmployeeIdFilter = it }, onViewEmployeeCalendar)
                2 -> NotesTab(pendingNotes, viewModel)
                3 -> HolidaysTab(globalHolidays, viewModel, officeClosingTime)
            }
        }
    }
}

// TAB 1: REGISTRATION APPROVALS
@Composable
fun ApprovalsTab(pendingEmployees: List<Employee>, viewModel: AdminViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (pendingEmployees.isEmpty()) {
            item { Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text("No pending registrations.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } else {
            items(pendingEmployees) { employee ->
                EmployeeApprovalCard(employee, onApprove = { viewModel.approveEmployee(employee.id) }, onReject = { viewModel.rejectEmployee(employee.id) })
            }
        }
    }
}

// TAB 2: LEDGERS & CALENDARS
@Composable
fun LedgersTab(
    recentLogs: List<AttendanceLog>,
    employeesList: List<Pair<String, String>>,
    selectedFilter: String?,
    onFilterChange: (String?) -> Unit,
    onViewCalendar: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            item { FilterChip(selected = selectedFilter == null, onClick = { onFilterChange(null) }, label = { Text("All Workers") }) }
            items(employeesList) { (userId, userName) ->
                FilterChip(selected = selectedFilter == userId, onClick = { onFilterChange(userId) }, label = { Text(userName) })
            }
        }

        if (selectedFilter != null) {
            Button(onClick = { onViewCalendar(selectedFilter) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("View Full Calendar")
            }
        }

        val filteredLogs = remember(recentLogs, selectedFilter) { if (selectedFilter == null) recentLogs else recentLogs.filter { it.userId == selectedFilter } }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredLogs) { log -> AttendanceLogCard(log = log) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// TAB 3: ABSENCE NOTES
@Composable
fun NotesTab(pendingNotes: List<AdminAbsenceNote>, viewModel: AdminViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(pendingNotes) { note ->
            AbsenceNoteReviewCard(note, onApprove = { viewModel.updateNoteStatus(note.id, "approved") }, onReject = { viewModel.updateNoteStatus(note.id, "rejected") })
        }
    }
}

// TAB 4: HOLIDAYS MANAGEMENT
@Composable
fun HolidaysTab(
    globalHolidays: List<GlobalHoliday>,
    viewModel: AdminViewModel,
    closingTimeState: String
) {
    var inputDate by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }
    var forceDate by remember { mutableStateOf("") }
    var closingTimeInput by remember { mutableStateOf(closingTimeState) }

    var holidayToDelete by remember { mutableStateOf<GlobalHoliday?>(null) }
    var removalReason by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(closingTimeState) { closingTimeInput = closingTimeState }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // Office Settings (Closing Time)
        item {
            Text("Office Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = closingTimeInput,
                onValueChange = { closingTimeInput = it },
                label = { Text("Closing Time (HH:mm, 24hr format)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.updateClosingTime(closingTimeInput) {
                        android.widget.Toast.makeText(context, "Closing time updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Closing Time")
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Add Holiday Form
        item {
            Text("Manage Holidays", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = inputDate, onValueChange = { inputDate = it }, label = { Text("YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = inputName, onValueChange = { inputName = it }, label = { Text("Holiday Name") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { if (inputDate.isNotBlank()) { viewModel.addGlobalHoliday(inputDate, inputName); inputDate = ""; inputName = "" } }, modifier = Modifier.fillMaxWidth()) { Text("Save Holiday") }
        }

        // Force Working Day Form
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Force Working Day", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            OutlinedTextField(value = forceDate, onValueChange = { forceDate = it }, label = { Text("Target Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { if (forceDate.isNotBlank()) { viewModel.removeHolidayWithNote(forceDate, "Forced as Working Day"); forceDate = "" } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Mark as Working Day") }
        }

        // List of Holidays
        items(globalHolidays) { holiday ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { holidayToDelete = holiday }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(holiday.name, fontWeight = FontWeight.Bold)
                        Text(holiday.date, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    // Deletion Dialog
    if (holidayToDelete != null) {
        AlertDialog(
            onDismissRequest = { holidayToDelete = null },
            title = { Text("Remove Holiday") },
            text = {
                Column {
                    Text("Reason for removal:")
                    OutlinedTextField(value = removalReason, onValueChange = { removalReason = it })
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.removeHolidayWithNote(holidayToDelete!!.date, removalReason); holidayToDelete = null }) { Text("Confirm") }
            }
        )
    }
}

@Composable
fun AbsenceNoteReviewCard(note: AdminAbsenceNote, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(note.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(note.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("\"${note.note}\"", style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onReject, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Deny") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onApprove) { Text("Approve") }
            }
        }
    }
}

@Composable
fun EmployeeApprovalCard(employee: Employee, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(employee.name, fontWeight = FontWeight.Bold)
                Text(employee.email, style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onReject) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
                IconButton(onClick = onApprove) { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
fun AttendanceLogCard(log: AttendanceLog) {
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val inTime = timeFormatter.format(Date(log.punchInTime))
    val outTime = if (log.punchOutTime != null) timeFormatter.format(Date(log.punchOutTime)) else "Active"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = log.userName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location Pin",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = log.officeName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Date: ${log.date}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "IN: $inTime | OUT: $outTime", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}