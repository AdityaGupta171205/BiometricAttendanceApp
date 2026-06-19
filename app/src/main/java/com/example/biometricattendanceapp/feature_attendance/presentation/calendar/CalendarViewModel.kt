package com.example.biometricattendanceapp.feature_attendance.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometricattendanceapp.feature_attendance.data.AttendanceRepository
import com.example.biometricattendanceapp.feature_attendance.presentation.admin.AttendanceLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// --- NEW: Added INCOMPLETE to track forgotten punch-outs ---
enum class DayStatus { PRESENT, ABSENT, INCOMPLETE, ACTIVE, HOLIDAY, FUTURE, EMPTY }

data class AbsenceNote(
    val id: String,
    val date: String,
    val note: String,
    val status: String
)

data class CalendarDay(
    val date: LocalDate?,
    val status: DayStatus,
    val log: AttendanceLog? = null,
    val note: AbsenceNote? = null,
    val holidayName: String? = null,
    val adminRemovalNote: String? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: AttendanceRepository
) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _calendarDays = MutableStateFlow<List<CalendarDay>>(emptyList())
    val calendarDays = _calendarDays.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth = _currentMonth.asStateFlow()

    private var currentLogListener: ListenerRegistration? = null
    private var currentNotesListener: ListenerRegistration? = null
    private var holidaysListener: ListenerRegistration? = null
    private var removalNotesListener: ListenerRegistration? = null

    private var isPersonalCalendar = true
    private var targetUid: String = ""
    private var globalHolidays = mapOf<LocalDate, String>()
    private var removalNotes = mapOf<LocalDate, String>()
    private var currentMonthLogs = listOf<AttendanceLog>()
    private var currentMonthNotes = listOf<AbsenceNote>()

    fun initialize(passedUserId: String?) {
        if (passedUserId != null) {
            targetUid = passedUserId
            isPersonalCalendar = false
        } else {
            targetUid = auth.currentUser?.uid ?: ""
            isPersonalCalendar = true
        }
        fetchGlobalHolidays()
        fetchHolidayRemovalLogs()
    }

    private fun fetchGlobalHolidays() {
        holidaysListener?.remove()
        holidaysListener = db.collection("holidays").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            globalHolidays = snapshot?.documents?.mapNotNull { doc ->
                val dateStr = doc.getString("date") ?: return@mapNotNull null
                val name = doc.getString("name") ?: "Holiday"
                LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")) to name
            }?.toMap() ?: emptyMap()
            fetchMonthData(_currentMonth.value)
        }
    }

    private fun fetchHolidayRemovalLogs() {
        removalNotesListener?.remove()
        removalNotesListener = db.collection("holiday_removal_logs").addSnapshotListener { snapshot, _ ->
            removalNotes = snapshot?.documents?.mapNotNull { doc ->
                val dateStr = doc.getString("date") ?: return@mapNotNull null
                val reason = doc.getString("reason") ?: "No reason provided"
                LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")) to reason
            }?.toMap() ?: emptyMap()
            fetchMonthData(_currentMonth.value)
        }
    }

    fun previousMonth() { fetchMonthData(_currentMonth.value.minusMonths(1)) }
    fun nextMonth() { fetchMonthData(_currentMonth.value.plusMonths(1)) }

    private fun fetchMonthData(yearMonth: YearMonth) {
        _currentMonth.value = yearMonth
        _calendarDays.value = emptyList()

        currentLogListener?.remove()
        currentNotesListener?.remove()

        val startDate = yearMonth.atDay(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val endDate = yearMonth.atEndOfMonth().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        currentLogListener = db.collection("attendance_logs")
            .whereEqualTo("userId", targetUid)
            .addSnapshotListener { snapshot, _ ->
                val allLogs = snapshot?.documents?.mapNotNull { doc ->
                    AttendanceLog(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = "",
                        date = doc.getString("date") ?: "",
                        punchInTime = doc.getLong("punchInTime") ?: 0L,
                        punchOutTime = doc.getLong("punchOutTime"),
                        officeName = doc.getString("officeName") ?: "Unknown"
                    )
                } ?: emptyList()
                currentMonthLogs = allLogs.filter { it.date in startDate..endDate }
                generateCalendarGrid(yearMonth)
            }

        currentNotesListener = db.collection("attendance_notes")
            .whereEqualTo("userId", targetUid)
            .addSnapshotListener { snapshot, _ ->
                val allNotes = snapshot?.documents?.mapNotNull { doc ->
                    AbsenceNote(
                        id = doc.id,
                        date = doc.getString("date") ?: "",
                        note = doc.getString("note") ?: "",
                        status = doc.getString("status") ?: ""
                    )
                } ?: emptyList()
                currentMonthNotes = allNotes.filter { it.date in startDate..endDate }
                generateCalendarGrid(yearMonth)
            }
    }

    private fun generateCalendarGrid(yearMonth: YearMonth) {
        val daysList = mutableListOf<CalendarDay>()
        val firstDayOfMonth = yearMonth.atDay(1)

        for (i in 0 until (firstDayOfMonth.dayOfWeek.value % 7)) {
            daysList.add(CalendarDay(null, DayStatus.EMPTY))
        }

        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)
            val dateString = date.format(dateFormatter)

            val logForDay = currentMonthLogs.find { it.date == dateString && it.punchOutTime != null && it.punchOutTime > 0L }
                ?: currentMonthLogs.find { it.date == dateString }
            val noteForDay = currentMonthNotes.find { it.date == dateString }

            val isRemoved = removalNotes.containsKey(date)
            val isHoliday = (globalHolidays.containsKey(date) || date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) && !isRemoved

            val holidayName = globalHolidays[date] ?: if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) "Weekend" else null
            val removalNote = removalNotes[date]
            val status = when {
                isHoliday -> DayStatus.HOLIDAY
                date.isAfter(today) -> DayStatus.FUTURE

                noteForDay?.status == "approved" -> DayStatus.PRESENT

                logForDay != null && logForDay.punchOutTime != null && logForDay.punchOutTime > 0L -> DayStatus.PRESENT

                logForDay != null && (logForDay.punchOutTime == null || logForDay.punchOutTime == 0L) -> {
                    if (date.isBefore(today)) {
                        DayStatus.INCOMPLETE
                    } else {
                        DayStatus.ACTIVE
                    }
                }

                else -> DayStatus.ABSENT
            }

            daysList.add(CalendarDay(date, status, logForDay, noteForDay, holidayName, removalNote))
        }
        _calendarDays.value = daysList
    }

    fun submitAbsenceNote(date: LocalDate, note: String, onSuccess: () -> Unit) {
        val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val noteData = hashMapOf(
            "userId" to targetUid, "date" to dateString, "note" to note,
            "timestamp" to System.currentTimeMillis(), "status" to "pending_review"
        )
        viewModelScope.launch {
            db.collection("attendance_notes").add(noteData).addOnSuccessListener { onSuccess() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentLogListener?.remove()
        currentNotesListener?.remove()
        holidaysListener?.remove()
        removalNotesListener?.remove()
    }

    fun refreshCalendarData() {
        if (isPersonalCalendar) {
            val freshestUid = auth.currentUser?.uid
            if (freshestUid != null && targetUid != freshestUid) {
                targetUid = freshestUid
            }
        }
        fetchMonthData(_currentMonth.value)
    }

    fun deleteAbsenceNote(noteId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            db.collection("attendance_notes").document(noteId).delete()
                .addOnSuccessListener { onSuccess() }
        }
    }
}