package com.studentclubconnect

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.studentclubconnect.databinding.ActivityLoginBinding
import com.studentclubconnect.ui.auth.SignupActivity
import com.studentclubconnect.viewmodel.AuthState
import com.studentclubconnect.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in
        if (viewModel.getCurrentUser() != null) {
            navigateToMain()
            return
        }

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
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
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.btnCreateAccount.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
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

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty()) {
            binding.etEmail.error = "Please enter your email"
            return
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Please enter your password"
            return
        }

        viewModel.signIn(email, password)
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        // Add ProgressBar visibility if you have one
    }

    private fun handleError(message: String) {
        android.util.Log.d("LoginActivity", "Error received: $message")
        val userFriendlyMessage = when {
            message.contains("user-not-found", true) || 
            message.contains("wrong-password", true) || 
            message.contains("credential", true) ||
            message.contains("invalid-email", true) -> {
                "Incorrect email or password."
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
        startActivity(intent)
        finish()
    }
}