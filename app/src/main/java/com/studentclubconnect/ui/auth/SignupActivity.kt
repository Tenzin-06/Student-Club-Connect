package com.studentclubconnect.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.studentclubconnect.LoginActivity
import com.studentclubconnect.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

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
    }

    private fun setupListeners() {
        binding.btnSignup.setOnClickListener {
            if (validateFields()) {
                Toast.makeText(this, "Signup details are valid", Toast.LENGTH_SHORT).show()
                // Proceed with signup logic in later milestone
            }
        }

        binding.btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
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
}