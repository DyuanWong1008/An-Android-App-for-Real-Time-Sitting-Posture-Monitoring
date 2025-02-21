package com.example.postureguard.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.postureguard.ChangePasswordActivity
import com.example.postureguard.EditProfileActivity
import com.example.postureguard.LoginActivity
import com.example.postureguard.MainActivity
import com.example.postureguard.R
import com.example.postureguard.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        loadUserData()
        setupButtons()
        setupLanguageSpinner()

        return root
    }

    private fun setupButtons() {
        binding.btnEditProfile.setText(R.string.profile_edit)
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(activity, EditProfileActivity::class.java))
        }

        binding.btnChangePassword.setText(R.string.profile_change_password)
        binding.btnChangePassword.setOnClickListener {
            startActivity(Intent(activity, ChangePasswordActivity::class.java))
        }

        binding.btnLogout.setText(R.string.profile_logout)
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun setupLanguageSpinner() {
        val languageSpinner = binding.languageSpinner
        val languages = arrayOf("English", "中文", "Bahasa Melayu")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, languages)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        languageSpinner.adapter = adapter

        val sharedPreferences = getSharedPreferences()
        val currentLanguage = sharedPreferences.getString("selectedLanguage", "en") ?: "en"
        val defaultPosition = when (currentLanguage) {
            "cn" -> 1
            "my" -> 2
            else -> 0
        }
        languageSpinner.setSelection(defaultPosition)

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLanguage = when (position) {
                    1 -> "cn"
                    2 -> "my"
                    else -> "en"
                }

                if (selectedLanguage != currentLanguage) {
                    saveLanguageSetting(selectedLanguage)
                    restartMainActivity()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun getSharedPreferences() = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun saveLanguageSetting(language: String) {
        val sharedPreferences = getSharedPreferences()
        with(sharedPreferences.edit()) {
            putString("selectedLanguage", language)
            apply()
        }
    }

    private fun restartMainActivity() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        binding.tvName.text = document.getString("name") ?: getString(R.string.profile_name)
                        binding.tvAge.text = document.getString("age") ?: getString(R.string.profile_age)
                        binding.tvGender.text = document.getString("gender") ?: getString(R.string.profile_gender)
                        binding.tvEmail.text = document.getString("email") ?: getString(R.string.profile_email)
                    } else {
                        Toast.makeText(context, getString(R.string.profile_no_document), Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, getString(R.string.profile_error, exception.message), Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, getString(R.string.profile_user_not_logged_in), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}