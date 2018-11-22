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
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.QuerySnapshot
import com.google.gson.Gson

class RegisterActivity : AppCompatActivity() {

    private lateinit var mAuth : FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private lateinit var registerButton : Button
    private lateinit var loginLink : TextView
    private lateinit var usernameText : EditText
    private lateinit var emailText : EditText
    private lateinit var passwordText : EditText

    companion object {
        const val TAG = "RegisterActivity" // Logging purposes
        // For holding data in Firestore
        const val COLLECTION_KEY = "Users"
        const val USERNAME_KEY = "Username"
        const val EMAIL_KEY = "Email"
        const val WALLET_KEY = "Wallet"
        const val BANK_KEY = "Bank"
        const val GIFTS_KEY = "Gifts"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        db.firestoreSettings = settings
        registerButton = findViewById<View>(R.id.buttonRegister) as Button
        loginLink = findViewById<View>(R.id.signInLink) as TextView

        // Handle register button and login link click
        registerButton.setOnClickListener { _ -> registerUser() }
        loginLink.setOnClickListener{ _ -> switchToLogin() }
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
            // Check if chosen username is unique
            val usersRef = db.collection(COLLECTION_KEY)
            val checkUsername = usersRef.whereEqualTo(USERNAME_KEY, username)
            checkUsername.get().addOnCompleteListener{checkUsernameTask: Task<QuerySnapshot> ->
                if (checkUsernameTask.isSuccessful) {
                    if (checkUsernameTask.result!!.isEmpty) {
                        Log.d(TAG,"[registerUser] Username us unique, proceeding to further checks")
                        // Check validity of other credentials, warn if invalid
                        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener{ task: Task<AuthResult> ->
                            if (task.isSuccessful) {
                                Log.d(TAG,"[registerUser] Registration successful, update FireStore database")
                                // Create document for user in "Users" collection with fields for username and email
                                val user : HashMap<String, Any> = HashMap()
                                user[USERNAME_KEY] = username
                                user[EMAIL_KEY] = email
                                // Create empty wallet and bank account for user
                                val emptyWallet = ArrayList<Coin>()
                                val emptyBankTransfers = ArrayList<BankTransfer>()
                                val emptyGifts = ArrayList<Gift>()
                                val emptyBankAccount = BankAccount(username,0.0,emptyBankTransfers)
                                val gson = Gson();
                                user[WALLET_KEY] = gson.toJson(emptyWallet)
                                user[BANK_KEY] = gson.toJson(emptyBankAccount)
                                user[GIFTS_KEY] = gson.toJson(emptyGifts)
                                db.collection(COLLECTION_KEY).document(mAuth.uid!!).set(user).addOnSuccessListener{ _: Void? ->
                                    Log.d(TAG,"[registerUser] Created new user")
                                    Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Log.d(TAG,"[registerUser] Registration failed due to bad credentials")
                                val exc = task.exception
                                when (exc) {
                                    is FirebaseAuthUserCollisionException -> {
                                        Log.d(TAG, "[registerUser] Registration failed since email exists")
                                        Toast.makeText(this, "Registration failed, an account with this email already exists!", Toast.LENGTH_SHORT).show()
                                    }
                                    is FirebaseAuthWeakPasswordException -> {
                                        Log.d(TAG, "[registerUser] Registration failed since password was too weak")
                                        Toast.makeText(this, "Registration failed, your password is too weak! Must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                                    }
                                    is FirebaseAuthInvalidCredentialsException -> {
                                        Log.d(TAG, "[registerUser] Registration failed since email is malformed")
                                        Toast.makeText(this, "Registration failed, encountered an invalid email address!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                    else {
                        Toast.makeText(this,"Registration failed, an account with this username already exists!",Toast.LENGTH_SHORT).show()
                        Log.d(TAG,"[registerUser] Username already exists")
                    }
                }
                else {
                    Toast.makeText(this,"Registration failed, check your internet connection!",Toast.LENGTH_SHORT).show()
                    Log.d(TAG,"[registerUser] Problems when checking uniqueness of username")
                }
            }
        }
        else {
            // If credentials empty warn user about this
            Log.d(TAG,"[registerUser] Registration failed because input was empty")
            Toast.makeText(this, "Please fill all credentials", Toast.LENGTH_SHORT).show()
        }

    }

    private fun switchToLogin() {
        startActivity(Intent(this,LoginActivity::class.java))
    }
}