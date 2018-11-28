package com.example.minht.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var sendLinkButton: Button
    private lateinit var mAuth: FirebaseAuth

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
                                "your email address", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this,LoginActivity::class.java))
                    }
                    else {
                        val message = task.exception!!.message
                        Toast.makeText(this,"Error occurred: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else {
                Toast.makeText(this,"Enter you enter address first!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
