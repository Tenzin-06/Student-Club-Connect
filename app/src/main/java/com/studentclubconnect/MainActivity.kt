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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            val firebaseApp = FirebaseApp.getInstance()

            Log.d(
                "FirebaseTest",
                "Firebase initialized: ${firebaseApp.name}"
            )

            val auth = FirebaseAuth.getInstance()

            Log.d(
                "FirebaseTest",
                "Authentication service initialized"
            )

            val db = FirebaseFirestore.getInstance()

            Log.d(
                "FirebaseTest",
                "Firestore service initialized"
            )

            Toast.makeText(
                this,
                "Firebase connected successfully",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Log.e(
                "FirebaseTest",
                "Firebase initialization failed",
                e
            )

            Toast.makeText(
                this,
                "Unable to connect to the server. Please try again.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}