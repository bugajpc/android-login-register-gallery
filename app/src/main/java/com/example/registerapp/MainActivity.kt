package com.example.registerapp

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var imageView: ImageView
    private var imageURL: String = ""
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == Activity.RESULT_OK)
        {
            val data: Intent? = it.data
            val imageUri = data?.data
            imageView.setImageURI(imageUri)

            val storageReference = FirebaseStorage.getInstance().reference
            val imageRef = storageReference.child("images/" + UUID.randomUUID().toString())
            val uploadTask = imageRef.putFile(imageUri!!)

            uploadTask.addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    Log.d("MAIN", downloadUrl)
                    imageURL = downloadUrl
                }.addOnFailureListener {
                    Log.d("MAIN", it.toString())
                }
            }.addOnFailureListener {
                Log.d("MAIN", it.toString())
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val intentHome = Intent(this, HomeActivity::class.java)
        auth = Firebase.auth

        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(intentHome)
            finish()
        }
        val emailEdit: EditText = findViewById(R.id.register_email_editText)
        val passwordEdit: EditText = findViewById(R.id.register_password_editText)
        val registerButton: Button = findViewById(R.id.register_button)
        val goToLogin: TextView = findViewById(R.id.goToLogin_textView)
        imageView = findViewById(R.id.imageView)
        val db = Firebase.firestore

        Picasso.get().load("https://i.imgur.com/DvpvklR.png").into(imageView)

        imageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            getContent.launch(intent)
        }

        goToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        registerButton.setOnClickListener {
            if (imageURL.isEmpty() || emailEdit.text.isEmpty() || passwordEdit.text.isEmpty()) {
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(emailEdit.text.toString(), passwordEdit.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        Toast.makeText(
                            baseContext,
                            "Hello " + user?.email.toString(),
                            Toast.LENGTH_SHORT,
                        ).show()

                        val newUser = hashMapOf(
                            "email" to emailEdit.text.toString(),
                            "avatar" to imageURL
                        )

                        db.collection("users").document(user!!.uid)
                            .set(newUser)
                            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                        startActivity(intentHome)
                        finish()
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }
    }
}