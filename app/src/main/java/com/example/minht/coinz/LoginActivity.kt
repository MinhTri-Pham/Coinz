package com.example.minht.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class LoginActivity : AppCompatActivity() {

    private lateinit var loginButton : Button
    private lateinit var editTextEmail : EditText
    private lateinit var editTextPassword : EditText
    private lateinit var signUpLink : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        loginButton = findViewById(R.id.loginButton)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        signUpLink = findViewById(R.id.signUpLink)
    }


}
