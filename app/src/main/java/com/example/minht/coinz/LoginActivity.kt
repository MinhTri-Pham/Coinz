package com.example.minht.coinz

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
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

    private lateinit var mAuth : FirebaseAuth
    private lateinit var emailText : EditText
    private lateinit var passwordText : EditText
    private lateinit var loginButton : Button
    private lateinit var registerLink : TextView
    private lateinit var forgotLink : TextView

    companion object {
        const val TAG = "LoginActivity" // For debugging purposes
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()
        loginButton = findViewById(R.id.loginButton)
        loginButton.setOnClickListener{ _ ->
            // Warn user if no network connection
            if (isNetworkAvailable()) {
                Log.d(TAG,"[onCreate] User connected, can proceed with log in")
                loginUser()
            }
            else {
                Log.d(TAG, "[onCreate] User not connected, can't proceed with log in")
                Toast.makeText(this,"Check your internet connection!", Toast.LENGTH_SHORT).show()
            }
        }
        registerLink = findViewById(R.id.signUpLink)
        registerLink.setOnClickListener { _ ->
            startActivity(Intent(this,RegisterActivity::class.java))
        }
        forgotLink = findViewById(R.id.forgotPasswordLink)
        forgotLink.setOnClickListener{_ ->
            startActivity(Intent(this,ResetPasswordActivity::class.java))
        }
    }

    // If credentials correct, Map view opens, otherwise user is warned to change input
    private fun loginUser() {
        emailText = findViewById<View>(R.id.editTextEmail) as EditText
        passwordText = findViewById<View>(R.id.editTextPassword) as EditText
        // Extract credentials
        val email = emailText.text.toString()
        val password = passwordText.text.toString()
        if (!email.isEmpty() && !password.isEmpty()) {
            // Check validity of credentials, warn user if invalid
            mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener{task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    Log.d(TAG, "[loginUser] User logged in successfully")
                    startActivity(Intent(this,MainActivity::class.java))
                    Toast.makeText(this,"Successfully logged in", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "[loginUser] Authentication failed since credentials were incorrect")
                    Toast.makeText(this,"Email or password wasn't correct", Toast.LENGTH_SHORT).show()
                }
            }
        // If credentials empty warn user about this
        } else {
            Log.d(TAG, "[loginUser] Authentication failed since credentials were incorrect")
            Toast.makeText(this, "Please fill all credentials",Toast.LENGTH_SHORT).show()
        }
    }

    // Check if internet connection is available
    private fun isNetworkAvailable() : Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}