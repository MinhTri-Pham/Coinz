package com.example.minht.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var sendLinkButton: Button
    private lateinit var mAuth: FirebaseAuth

    companion object {
        const val TAG = "ResetPasswordActivity" // Logging purposes
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)
        mAuth = FirebaseAuth.getInstance()
        emailEditText = findViewById(R.id.resetEmailEditText)
        sendLinkButton = findViewById(R.id.sendLinkButton)
        sendLinkButton.setOnClickListener {_ ->
            val email = emailEditText.text.toString()
            if (!email.isEmpty()) {
                mAuth.sendPasswordResetEmail(email).addOnCompleteListener {task: Task<Void> ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Reset password link sent. Please check " +
                                "your email address!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this,LoginActivity::class.java))
                    }
                    else {
                        // Show detailed reason why password reset failed
                        val exc = task.exception
                        when (exc) {
                            is FirebaseAuthInvalidCredentialsException -> {
                                Log.d(TAG, "[registerUser] Password reset failed since email is malformed")
                                Toast.makeText(this, "Password reset failed, encountered an invalid email address!", Toast.LENGTH_SHORT).show()
                            }
                            is FirebaseAuthInvalidUserException -> {
                                Log.d(TAG, "[registerUser] Password reset failed since no user exists")
                                Toast.makeText(this, "Password reset failed, no user with this email exists!", Toast.LENGTH_SHORT).show()
                            }

                        }
                    }
                }
            }
            else {
                Toast.makeText(this,"Enter your email address first!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
