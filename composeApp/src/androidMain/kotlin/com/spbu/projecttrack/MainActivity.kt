package com.spbu.projecttrack

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.spbu.projecttrack.core.auth.handleIncomingAuthRedirect
import com.spbu.projecttrack.core.storage.initAppPreferences

class MainActivity : ComponentActivity() {
    private var nativeLaunchSplashView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
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
        showNativeLaunchSplash()
        setContent {
            App(onLaunchReady = ::hideNativeLaunchSplash)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let(::handleIncomingAuthRedirect)
    }

    private fun showNativeLaunchSplash() {
        if (nativeLaunchSplashView != null) return

        val splashView = LayoutInflater.from(this)
            .inflate(R.layout.view_native_launch_splash, null, false)
        addContentView(
            splashView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        )
        nativeLaunchSplashView = splashView
    }

    private fun hideNativeLaunchSplash() {
        nativeLaunchSplashView?.let { splashView ->
            splashView.post {
                if (splashView.parent is ViewGroup) {
                    (splashView.parent as ViewGroup).removeView(splashView)
                }
                if (nativeLaunchSplashView === splashView) {
                    nativeLaunchSplashView = null
                }
            }
        }
    }
}
