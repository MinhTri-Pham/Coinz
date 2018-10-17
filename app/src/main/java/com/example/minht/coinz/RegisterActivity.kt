package com.example.minht.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private val tag = "RegisterActivity"
    private lateinit var mAuth : FirebaseAuth
    private lateinit var registerButton : Button
    private lateinit var loginLink : TextView
    private lateinit var emailText : EditText
    private lateinit var passwordText : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mAuth = FirebaseAuth.getInstance()
        registerButton = findViewById<View>(R.id.buttonRegister) as Button
        loginLink = findViewById<View>(R.id.signInLink) as TextView

        // Handle register button and login link click
        registerButton.setOnClickListener(View.OnClickListener {
            view -> registerUser()
        })

        loginLink.setOnClickListener(View.OnClickListener {
            view -> switchToLogin()
        })
    }

    // Invoked when user presses the Log in button
    // If credentials correct, Log In screen opens, otherwise user is warned to change input
    private fun registerUser() {
        emailText = findViewById<View>(R.id.editTextEmail) as EditText
        passwordText = findViewById<View>(R.id.editTextPassword) as EditText

        val email = emailText.text.toString()
        val password = passwordText.text.toString()
        // Check if email and password text fields are empty
        if (!email.isEmpty() && !password.isEmpty()) {
            // Check validity of credentials, warn if invalid
            mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this, OnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(tag,"[registerUser] Registration was successful")
                    switchToLogin()
                    Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(tag,"[registerUser] Registration failed due to bad credentials")
                    Toast.makeText(this, "Couldn't register, try different credentials", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // If credentials empty warn user about this
            Log.d(tag,"[registerUser] Registration failed because input was empty")
            Toast.makeText(this, "Please fill the credentials", Toast.LENGTH_SHORT).show()
        }

    }

    private fun switchToLogin() {
        startActivity(Intent(this,LoginActivity::class.java))
    }

}
