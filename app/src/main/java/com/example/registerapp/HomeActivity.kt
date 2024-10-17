package com.example.registerapp

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.squareup.picasso.Picasso

class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = Firebase.auth

        val emailText: TextView = findViewById(R.id.email_textView)
        val signOutButton: Button = findViewById(R.id.signOut_button)
        val avatarImage: ImageView = findViewById(R.id.avatar_imageView)
        val db = Firebase.firestore

        val docRef = db.collection("users").document(auth.currentUser!!.uid.toString())
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Picasso.get().load(document.data!!["avatar"].toString()).into(avatarImage)
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }

        emailText.text = auth.currentUser?.email.toString() ?: "nothing"

        signOutButton.setOnClickListener {
            Firebase.auth.signOut()
            val intentRegister = Intent(this, MainActivity::class.java)
            startActivity(intentRegister)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            val intentRegister = Intent(this, MainActivity::class.java)
            startActivity(intentRegister)
            finish()
        }
    }
}