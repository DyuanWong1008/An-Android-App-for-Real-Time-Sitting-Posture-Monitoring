package com.example.postureguard

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.postureguard.databinding.ActivityChangePasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnChangePassword.setOnClickListener {
            val oldPassword = binding.etOldPassword.text.toString()
            val newPassword = binding.etNewPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (newPassword != confirmPassword) {
                binding.etConfirmPassword.error = "Passwords do not match"
            } else {
                val user = auth.currentUser
                if (user != null && user.email != null) {
                    auth.signInWithEmailAndPassword(user.email!!, oldPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                user.updatePassword(newPassword)
                                    .addOnCompleteListener { updateTask ->
                                        if (updateTask.isSuccessful) {
                                            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                                            finish()
                                        } else {
                                            Toast.makeText(this, "Error: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(this, "Old password is incorrect", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }
    }
}