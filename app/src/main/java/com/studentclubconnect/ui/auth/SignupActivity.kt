package com.studentclubconnect.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.studentclubconnect.LoginActivity
import com.studentclubconnect.MainActivity
import com.studentclubconnect.databinding.ActivitySignupBinding
import com.studentclubconnect.viewmodel.AuthState
import com.studentclubconnect.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnSignup.setOnClickListener {
            if (validateFields()) {
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()
                viewModel.signUp(email, password)
            }
        }

        binding.btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    when (state) {
                        is AuthState.Loading -> {
                            setLoading(true)
                        }
                        is AuthState.Success -> {
                            setLoading(false)
                            navigateToMain()
                        }
                        is AuthState.Error -> {
                            setLoading(false)
                            handleError(state.message)
                        }
                        is AuthState.Idle -> {
                            setLoading(false)
                        }
                    }
                }
            }
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        val fullName = binding.etFullName.text.toString().trim()
        val studentId = binding.etStudentId.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Reset errors
        binding.tilFullName.error = null
        binding.tilStudentId.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Please enter your full name"
            isValid = false
        }

        if (studentId.isEmpty()) {
            binding.tilStudentId.error = "Please enter your student ID"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Please enter your email"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Please enter a password"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        return isValid
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSignup.isEnabled = !isLoading
    }

    private fun handleError(message: String) {
        android.util.Log.d("SignupActivity", "Error received: $message")
        val userFriendlyMessage = when {
            message.contains("email-already-in-use", true) || 
            message.contains("email-already-exists", true) ||
            message.contains("already in use", true) -> {
                "An account with this email already exists. Please log in instead."
            }
            message.contains("weak-password", true) || message.contains("password should be", true) -> {
                "Password must be at least 6 characters"
            }
            message.contains("network", true) || message.contains("timeout", true) || message.contains("connectivity", true) -> {
                "Unable to connect. Please check your internet connection and try again."
            }
            else -> "Something went wrong. Please try again."
        }
        Toast.makeText(this, userFriendlyMessage, Toast.LENGTH_LONG).show()
        viewModel.resetState()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}