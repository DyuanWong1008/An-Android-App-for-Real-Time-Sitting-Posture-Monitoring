package com.example.postureguard

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.postureguard.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                if (!isValidPassword(password)) {
                    binding.etPassword.error = getString(R.string.password_error)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text.toString()
            val age = binding.etAge.text.toString()
            val gender = if (binding.rbMale.isChecked) "Male" else "Female"
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (password != confirmPassword) {
                binding.etConfirmPassword.error = getString(R.string.password_mismatch)
            } else if (!isValidPassword(password)) {
                binding.etPassword.error = getString(R.string.password_error)
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            val user = hashMapOf(
                                "name" to name,
                                "age" to age,
                                "gender" to gender,
                                "email" to email
                            )
                            if (userId != null) {
                                db.collection("users").document(userId).set(user)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, getString(R.string.signup_success), Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, LoginActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, getString(R.string.error, e.message), Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.signup_failed, task.exception?.message), Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        binding.tvLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

    }

    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$"
        return password.matches(passwordPattern.toRegex())
    }
}