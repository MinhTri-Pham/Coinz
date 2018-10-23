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
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class RegisterActivity : AppCompatActivity() {

    private val tag = "RegisterActivity" // Logging purposes
    private val COLLECTION_KEY = "Users"
    private val USERNAME_KEY = "Username"
    private val EMAIL_KEY = "Email"
    private lateinit var mAuth : FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private lateinit var registerButton : Button
    private lateinit var loginLink : TextView
    private lateinit var usernameText : EditText
    private lateinit var emailText : EditText
    private lateinit var passwordText : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        db?.firestoreSettings = settings
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
        usernameText = findViewById<View>(R.id.editUsername) as EditText
        emailText = findViewById<View>(R.id.editTextEmail) as EditText
        passwordText = findViewById<View>(R.id.editTextPassword) as EditText

        val username = usernameText.text.toString()
        val email = emailText.text.toString()
        val password = passwordText.text.toString()
        // Check if email and password text fields are empty
        if (!username.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
            // Check validity of credentials, warn if invalid
            mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this, OnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(tag,"[registerUser] Registration was successful, update Firestore database")
                    // Create document for user in "Users" collection with fields for username and email
                    val user : HashMap<String, Any> = HashMap();
                    user.put(USERNAME_KEY, username)
                    user.put(EMAIL_KEY,email)
                    db.collection(COLLECTION_KEY).document(mAuth.uid!!).set(user).addOnSuccessListener{
                        _: Void? -> Log.d(tag,"[registerUser] Successfully updated database")
                    }.addOnFailureListener {
                        _: java.lang.Exception -> Log.d(tag,"[registerUser] Failed to update database")
                    }
                    Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(tag,"[registerUser] Registration failed due to bad credentials")
                    Toast.makeText(this, "Couldn't register, try different credentials", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // If credentials empty warn user about this
            Log.d(tag,"[registerUser] Registration failed because input was empty")
            Toast.makeText(this, "Please fill all credentials", Toast.LENGTH_SHORT).show()
        }

    }

    private fun switchToLogin() {
        startActivity(Intent(this,LoginActivity::class.java))
    }

}
