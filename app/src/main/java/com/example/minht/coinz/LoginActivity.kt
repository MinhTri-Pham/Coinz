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
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val tag = "LoginActivity"
    private lateinit var mAuth : FirebaseAuth
    private lateinit var loginButton : Button
    private lateinit var registerLink : TextView
    private lateinit var emailText : EditText
    private lateinit var passwordText : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()
        loginButton = findViewById<View>(R.id.loginButton) as Button
        // Handle login button and register link click
        loginButton.setOnClickListener{ _ -> loginUser() }
        registerLink = findViewById<View>(R.id.signUpLink) as TextView
        registerLink.setOnClickListener { _ -> switchToRegister() }
    }

    // Invoked when user presses the Log in button
    // If credentials correct, Map view opens, otherwise user is warned to change input
    private fun loginUser() {
        emailText = findViewById<View>(R.id.editTextEmail) as EditText
        passwordText = findViewById<View>(R.id.editTextPassword) as EditText
        val email = emailText.text.toString()
        val password = passwordText.text.toString()
        // Check if email and password text fields are empty
        if (!email.isEmpty() && !password.isEmpty()) {
            // Check validity of credentials, warn user if invalid
            mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener{task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    Log.d(tag, "[loginUser] User logged in successfully")
                    startActivity(Intent(this,MainActivity::class.java))
                    Toast.makeText(this,"Successfully logged in", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(tag, "[loginUser] Authentication failed since credentials were incorrect")
                    Toast.makeText(this,"Email or password wasn't correct", Toast.LENGTH_SHORT).show()
                }
            }
        // If credentials empty warn user about this
        } else {
            Log.d(tag, "[loginUser] Authentication failed since credentials were incorrect")
            Toast.makeText(this, "Please fill the credentials",Toast.LENGTH_SHORT).show()
        }
    }

    // If uses presses the link to create a new account, he's taken to the Register screen
    private fun switchToRegister() {
        startActivity(Intent(this,RegisterActivity::class.java))
    }


}
