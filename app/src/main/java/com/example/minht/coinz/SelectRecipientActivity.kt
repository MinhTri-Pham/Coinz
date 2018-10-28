package com.example.minht.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class SelectRecipientActivity : AppCompatActivity() {

    private val tag = "SelectRecipientActivity"
    private val COLLECTION_KEY = "Users"
    private val USERNAME_KEY = "Username"
    private lateinit var db : FirebaseFirestore
    private lateinit var usernameInputEditText : EditText
    private lateinit var confirmUsernameButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_recipient)

        db = FirebaseFirestore.getInstance()
        usernameInputEditText = findViewById(R.id.recipient_username_editText)
        confirmUsernameButton = findViewById(R.id.confirm_username_button)
        // Handle button click
        confirmUsernameButton.setOnClickListener { _ -> findUsername() }
    }

    private fun findUsername() {
        val usernameText = usernameInputEditText.text.toString()
        if (!usernameText.isEmpty()) {
            // Query if input username exists in the database
            val usersRef = db.collection(COLLECTION_KEY)
            val findUsernameQuery = usersRef.whereEqualTo(USERNAME_KEY,usernameText)
            findUsernameQuery.get().addOnCompleteListener{ task: Task<QuerySnapshot> ->
                if (task.isSuccessful) {
                    if (!task.result!!.isEmpty) {
                        Toast.makeText(this, "User found, proceed to choosing coins", Toast.LENGTH_SHORT).show()
                        makeTransfer()
                    } else {
                        Toast.makeText(this, "Such user doesn't exist, try a different username", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Query failed, check permissions", Toast.LENGTH_SHORT).show()
                }
            }

        } else {
            Log.d(tag,"[findUsername] Can't query since input was empty")
            Toast.makeText(this, "Please choose a player to send coins to", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeTransfer() {

    }
}
