package com.studentclubconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentclubconnect.data.model.Club
import com.studentclubconnect.data.repository.AnnouncementRepository
import com.studentclubconnect.data.repository.ClubRepository
import com.studentclubconnect.data.repository.EventRepository
import com.studentclubconnect.data.repository.MembershipRepository
import com.studentclubconnect.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val userRepository = UserRepository()
    private val eventRepository = EventRepository()
    private val announcementRepository = AnnouncementRepository()

    private val _dashboardState = MutableStateFlow(MyClubDashboardState())
    val dashboardState: StateFlow<MyClubDashboardState> = _dashboardState.asStateFlow()

    fun loadDashboardData(clubId: String) {
        viewModelScope.launch {
            _dashboardState.value = _dashboardState.value.copy(isLoading = true, error = null)
            
            // Observe real-time members and existing users to ensure count is accurate
            combine(
                membershipRepository.getMembershipsByClubFlow(clubId),
                userRepository.getAllUsersFlow()
            ) { memberships, allUsers ->
                memberships to allUsers
            }.collect { (memberships, allUsers) ->
                val clubResult = clubRepository.getClubById(clubId)
                clubResult.onSuccess { club ->
                    if (club != null) {
                        val validUserIds = allUsers.map { it.uid }.toSet()
                        val activeMemberCount = memberships.count { validUserIds.contains(it.userId) }
                        
                        val eventResult = eventRepository.getEventsByClub(clubId)
                        val announcementResult = announcementRepository.getAnnouncementsByClub(clubId)

                        _dashboardState.value = MyClubDashboardState(
                            isLoading = false,
                            club = club,
                            memberCount = activeMemberCount,
                            eventCount = eventResult.getOrDefault(emptyList()).size,
                            announcementCount = announcementResult.getOrDefault(emptyList()).size
                        )
                    }
                }
            }
        }
    }
}
