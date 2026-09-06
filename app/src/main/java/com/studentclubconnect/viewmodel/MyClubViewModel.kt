package com.studentclubconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentclubconnect.data.model.Club
import com.studentclubconnect.data.repository.AnnouncementRepository
import com.studentclubconnect.data.repository.ClubRepository
import com.studentclubconnect.data.repository.EventRepository
import com.studentclubconnect.data.repository.MembershipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyClubDashboardState(
    val isLoading: Boolean = false,
    val club: Club? = null,
    val memberCount: Int = 0,
    val eventCount: Int = 0,
    val announcementCount: Int = 0,
    val error: String? = null
)

class MyClubViewModel : ViewModel() {

    private val clubRepository = ClubRepository()
    private val membershipRepository = MembershipRepository()
    private val eventRepository = EventRepository()
    private val announcementRepository = AnnouncementRepository()

    private val _dashboardState = MutableStateFlow(MyClubDashboardState())
    val dashboardState: StateFlow<MyClubDashboardState> = _dashboardState.asStateFlow()

    fun loadDashboardData(clubId: String) {
        viewModelScope.launch {
            _dashboardState.value = _dashboardState.value.copy(isLoading = true, error = null)
            
            val clubResult = clubRepository.getClubById(clubId)
            
            clubResult.fold(
                onSuccess = { club ->
                    if (club == null) {
                        _dashboardState.value = _dashboardState.value.copy(
                            isLoading = false,
                            error = "Club details not found."
                        )
                    } else {
                        // Club found, load stats
                        loadStats(club)
                    }
                },
                onFailure = { error ->
                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        error = "Unable to load your club. Please try again."
                    )
                }
            )
        }
    }

    private suspend fun loadStats(club: Club) {
        val memberResult = membershipRepository.getMembersCountByClub(club.id)
        val eventResult = eventRepository.getEventsByClub(club.id)
        val announcementResult = announcementRepository.getAnnouncementsByClub(club.id)

        _dashboardState.value = _dashboardState.value.copy(
            isLoading = false,
            club = club,
            memberCount = memberResult.getOrDefault(0),
            eventCount = eventResult.getOrDefault(emptyList()).size,
            announcementCount = announcementResult.getOrDefault(emptyList()).size
        )
    }
}
