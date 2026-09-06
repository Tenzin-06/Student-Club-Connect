package com.studentclubconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentclubconnect.data.model.Announcement
import com.studentclubconnect.data.model.Club
import com.studentclubconnect.data.model.Event
import com.studentclubconnect.data.model.User
import com.studentclubconnect.data.repository.AnnouncementRepository
import com.studentclubconnect.data.repository.ClubRepository
import com.studentclubconnect.data.repository.EventRepository
import com.studentclubconnect.data.repository.MembershipRepository
import com.studentclubconnect.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeState {
    object Loading : HomeState()
    data class Success(
        val user: User?,
        val upcomingEvents: List<Event>,
        val joinedClubs: List<Club>,
        val popularClubs: List<Club> = emptyList(),
        val announcements: List<Announcement> = emptyList(),
        val clubNames: Map<String, String> = emptyMap()
    ) : HomeState()
    data class Error(val message: String) : HomeState()
}

class HomeViewModel : ViewModel() {

    private val clubRepository = ClubRepository()
    private val eventRepository = EventRepository()
    private val membershipRepository = MembershipRepository()
    private val userRepository = UserRepository()
    private val announcementRepository = AnnouncementRepository()

    private val _homeState = MutableStateFlow<HomeState>(HomeState.Loading)
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    fun loadHomeData(userId: String) {
        viewModelScope.launch {
            _homeState.value = HomeState.Loading
            try {
                // 1. Fetch User Profile
                val userResult = userRepository.getUserProfile(userId)
                val user = userResult.getOrNull()

                // 2. Fetch Upcoming Events
                val eventsResult = eventRepository.getAllEvents()
                val allEvents = eventsResult.getOrDefault(emptyList())
                val upcomingEvents = allEvents.take(5)

                // 3. Fetch Joined Clubs
                val membershipsResult = membershipRepository.getMembershipsByUser(userId)
                val joinedClubIds = membershipsResult.getOrDefault(emptyList()).map { it.clubId }
                
                val joinedClubs = mutableListOf<Club>()
                for (clubId in joinedClubIds) {
                    clubRepository.getClubById(clubId).onSuccess { club ->
                        club?.let { joinedClubs.add(it) }
                    }
                }

                // 4. Fetch Popular Clubs (using all clubs for now)
                val popularClubsResult = clubRepository.getAllClubs()
                val popularClubs = popularClubsResult.getOrDefault(emptyList()).take(3)

                // 5. Fetch Recent Announcements
                val announcementsResult = announcementRepository.getAllAnnouncements()
                val announcements = announcementsResult.getOrDefault(emptyList())
                    .sortedByDescending { it.createdAt }
                    .take(5)

                // 6. Resolve Club Names for Announcements and Events if needed
                // For efficiency, fetch all clubs and create a map
                val allClubsResult = clubRepository.getAllClubs()
                val clubNames = allClubsResult.getOrDefault(emptyList()).associate { it.id to it.name }

                _homeState.value = HomeState.Success(
                    user = user,
                    upcomingEvents = upcomingEvents,
                    joinedClubs = joinedClubs,
                    popularClubs = popularClubs,
                    announcements = announcements,
                    clubNames = clubNames
                )
            } catch (e: Exception) {
                _homeState.value = HomeState.Error("Unable to load information. Please try again.")
            }
        }
    }
}
