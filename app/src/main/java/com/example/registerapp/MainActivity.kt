package com.example.registerapp

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private lateinit var imageView: ImageView
    private var imageURL: String = ""
    private lateinit var imageUri2: Uri
    private var imagePicked = false
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == Activity.RESULT_OK)
        {
            val data: Intent? = it.data
            val imageUri = data?.data
            imageUri2 = imageUri!!
            imageView.setImageURI(imageUri)
            imagePicked = true
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

        auth = Firebase.auth

        val emailEdit: EditText = findViewById(R.id.register_email_editText)
        val passwordEdit: EditText = findViewById(R.id.register_password_editText)
        val registerButton: Button = findViewById(R.id.register_button)
        val goToLogin: TextView = findViewById(R.id.goToLogin_textView)
        imageView = findViewById(R.id.imageView)

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
            if (emailEdit.text.isEmpty() || passwordEdit.text.isEmpty()) {
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
                        createUserInDB(this, user)
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

    private fun createUserInDB(context: Context, user: FirebaseUser?) {
        if (!imagePicked) {
            imageURL = "https://i.imgur.com/DvpvklR.png"
            saveUserToFirestore(context, user)
        } else {
            val storageReference = FirebaseStorage.getInstance().reference
            val imageRef = storageReference.child("images/" + UUID.randomUUID().toString())

            imageRef.putFile(imageUri2)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        imageURL = uri.toString()
                        saveUserToFirestore(context, user)
                    }.addOnFailureListener {
                        Log.d("MAIN", it.toString())
                    }
                }.addOnFailureListener {
                    Log.d("MAIN", it.toString())
                }
        }
    }

    private fun saveUserToFirestore(context: Context, user: FirebaseUser?) {
        val newUser = hashMapOf(
            "email" to user?.email.toString(),
            "avatar" to imageURL
        )
        db.collection("users").document(user!!.uid)
            .set(newUser)
            .addOnSuccessListener {
                val intentRegister = Intent(context, HomeActivity::class.java)
                startActivity(intentRegister)
                finish()
            }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            val intentHome = Intent(this, HomeActivity::class.java)
            startActivity(intentHome)
            finish()
        }
    }
}