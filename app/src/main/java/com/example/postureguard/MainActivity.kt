package com.example.postureguard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.postureguard.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import com.google.android.gms.common.GoogleApiAvailability
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load saved language setting before inflating the layout
        loadSavedLanguage()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            redirectToLogin()
            return
        }

        // Setup BottomNavigationView
        setupNavigation()

        // Initialize Google Play Services on the main thread
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this@MainActivity)
            }
        }
    }

    private fun loadSavedLanguage() {
        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val selectedLanguage = sharedPreferences.getString("selectedLanguage", "en") ?: "en"
        updateLocale(selectedLanguage)
    }

    private fun updateLocale(languageCode: String) {
        val locale = when (languageCode) {
            "cn" -> Locale("zh", "CN") // Chinese
            "my" -> Locale("ms", "MY") // Malay
            else -> Locale("en", "US") // Default to English
        }
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_statistic, R.id.navigation_profile)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .commit()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        loadSavedLanguage() // Refresh locale on resume
    }

    override fun onDestroy() {
        super.onDestroy()
        // Manually trigger garbage collection
        System.gc()
    }
}