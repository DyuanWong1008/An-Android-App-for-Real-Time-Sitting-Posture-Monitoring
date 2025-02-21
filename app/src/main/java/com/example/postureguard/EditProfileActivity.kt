package com.example.postureguard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.postureguard.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        binding.etName.setText(document.getString("name") ?: "")
                        binding.etAge.setText(document.getString("age") ?: "")
                        val gender = document.getString("gender") ?: ""
                        if (gender == "Male") {
                            binding.rbMale.isChecked = true
                        } else if (gender == "Female") {
                            binding.rbFemale.isChecked = true
                        }
                        binding.etEmail.setText(document.getString("email") ?: "")
                    } else {
                        Toast.makeText(this, "No such document", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString()
            val age = binding.etAge.text.toString()
            val gender = if (binding.rbMale.isChecked) "Male" else "Female"
            val email = binding.etEmail.text.toString()

            val userUpdates = mapOf<String, Any>(
                "name" to name,
                "age" to age,
                "gender" to gender,
                "email" to email
            )

            if (userId != null) {
                db.collection("users").document(userId).update(userUpdates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent()
                        intent.putExtra("isUpdated", true)
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }


        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}