package com.studentclubconnect

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studentclubconnect.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.btnCreateAccount.setOnClickListener {
            // TODO: Navigate to Signup
            Toast.makeText(this, "Navigate to Signup", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "signInWithEmail:success")
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    // Navigate to MainActivity
                } else {
                    Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Unable to connect to the server. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
    }
}