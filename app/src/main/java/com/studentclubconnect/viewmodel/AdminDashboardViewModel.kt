package com.studentclubconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentclubconnect.data.model.Event
import com.studentclubconnect.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class AdminDashboardStats(
    val isLoading: Boolean = false,
    val studentCount: Int = 0,
    val clubCount: Int = 0,
    val presidentCount: Int = 0,
    val eventCount: Int = 0,
    val membershipCount: Int = 0,
    val announcementCount: Int = 0,
    val clubMemberships: Map<String, Int> = emptyMap(),
    val eventsByMonth: Map<String, Int> = emptyMap(),
    val error: String? = null
)

class AdminDashboardViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val clubRepository = ClubRepository()
    private val eventRepository = EventRepository()
    private val membershipRepository = MembershipRepository()
    private val announcementRepository = AnnouncementRepository()

    private val _statsState = MutableStateFlow(AdminDashboardStats())
    val statsState: StateFlow<AdminDashboardStats> = _statsState.asStateFlow()

    fun loadStats() {
        viewModelScope.launch {
            _statsState.value = _statsState.value.copy(isLoading = true, error = null)
            
            try {
                combine(
                    userRepository.getAllUsersFlow(),
                    membershipRepository.getAllMembershipsFlow(),
                    // For the others we can still use one-time get for now to minimize refactor, 
                    // but the above two are the most important for the user's deletion issue.
                ) { allUsers, memberships ->
                    allUsers to memberships
                }.collect { (allUsers, memberships) ->
                    val clubsResult = clubRepository.getAllClubs()
                    val eventsResult = eventRepository.getAllEvents()
                    val announcementsResult = announcementRepository.getAllAnnouncements()

                    val clubs = clubsResult.getOrDefault(emptyList())
                    val events = eventsResult.getOrDefault(emptyList())

                    // Filter memberships to only include those belonging to existing users
                    val validUserIds = allUsers.map { it.uid }.toSet()
                    val validMemberships = memberships.filter { validUserIds.contains(it.userId) }

                    // Analytics 1: Club Memberships
                    val clubMap = clubs.associate { it.id to it.name }
                    val membershipsByClub = validMemberships
                        .groupBy { it.clubId }
                        .mapKeys { clubMap[it.key] ?: "Unknown Club" }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(10)
                        .toMap()

                    // Analytics 2: Events by Month
                    val eventsByMonth = calculateEventsByMonth(events)

                    _statsState.value = AdminDashboardStats(
                        isLoading = false,
                        studentCount = allUsers.count { it.role.lowercase() == "student" },
                        clubCount = clubs.size,
                        presidentCount = allUsers.count { it.role.lowercase() == "president" },
                        eventCount = events.size,
                        membershipCount = validMemberships.size,
                        announcementCount = announcementsResult.getOrDefault(emptyList()).size,
                        clubMemberships = membershipsByClub,
                        eventsByMonth = eventsByMonth
                    )
                }

            } catch (e: Exception) {
                _statsState.value = _statsState.value.copy(
                    isLoading = false,
                    error = "Failed to load dashboard data"
                )
            }
        }
    }

    private fun calculateEventsByMonth(events: List<Event>): Map<String, Int> {
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        
        // Initialize last 6 months with 0
        val last6Months = mutableMapOf<String, Int>()
        for (i in 5 downTo 0) {
            val tempCal = Calendar.getInstance()
            tempCal.add(Calendar.MONTH, -i)
            last6Months[monthFormat.format(tempCal.time)] = 0
        }

        // Standard SCC date format is "yyyy-MM-dd" or similar
        val eventDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        events.forEach { event ->
            try {
                val date = eventDateFormat.parse(event.date)
                if (date != null) {
                    val month = monthFormat.format(date)
                    if (last6Months.containsKey(month)) {
                        last6Months[month] = last6Months[month]!! + 1
                    }
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }

        return last6Months
    }
}
