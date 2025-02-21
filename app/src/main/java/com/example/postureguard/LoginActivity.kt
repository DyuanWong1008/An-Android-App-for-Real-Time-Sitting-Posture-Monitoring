package com.example.postureguard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.postureguard.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->if (task.isSuccessful) {
                        Toast.makeText(this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Close the login activity
                    } else {
                        Toast.makeText(this, getString(R.string.login_failed, task.exception?.message), Toast.LENGTH_SHORT).show()
                    }
                    }
            } else {
                Toast.makeText(this, getString(R.string.enter_email_password), Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        auth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        val signupLink: TextView = findViewById(R.id.signup_link)
        signupLink.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }
}