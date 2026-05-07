package com.spbu.projecttrack

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.spbu.projecttrack.core.auth.handleIncomingAuthRedirect
import com.spbu.projecttrack.core.storage.initAppPreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppContextHolder.initialize(this)
        intent?.dataString?.let(::handleIncomingAuthRedirect)
        
        // Настройка статус-бара: белый фон с темными иконками
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true // Темные иконки на светлом фоне
        }
        
        // Устанавливаем цвет статус-бара и навигационного бара
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE
        
        // Инициализируем AppPreferences для Android
        initAppPreferences(this)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let(::handleIncomingAuthRedirect)
    }
}
