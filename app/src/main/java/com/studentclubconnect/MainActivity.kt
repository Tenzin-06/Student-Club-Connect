package com.studentclubconnect

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.studentclubconnect.databinding.ActivityMainBinding
import com.studentclubconnect.ui.clubs.ClubsFragment
import com.studentclubconnect.ui.events.EventsFragment
import com.studentclubconnect.ui.home.HomeFragment
import com.studentclubconnect.ui.profile.ProfileFragment
import com.studentclubconnect.viewmodel.AuthViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Final safety check: if not logged in, go to Login
        if (authViewModel.getCurrentUser() == null) {
            navigateToLogin()
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupNavigation()
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_clubs -> replaceFragment(ClubsFragment())
                R.id.nav_events -> replaceFragment(EventsFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment())
            }
            true
        }
        
        // Set default fragment
        if (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) == null) {
            replaceFragment(HomeFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}