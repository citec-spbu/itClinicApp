package com.spbu.projecttrack.onboarding.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.settings.LocalAppStrings
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.*
import projecttrack.composeapp.generated.resources.Res

// Кастомные цвета из дизайна
private val AppTitleColor = Color(0xFF9F2D20)
private val ButtonBackgroundColor = Color(0xFFA8ADB4)
private val ButtonBorderColor = Color(0xFFD0D5DC)
private val ButtonTextColor = Color(0xFF000000)
private val ContinueTextColor = Color(0xFFBDBDBD)
private val WhiteBackground = Color(0xFFFFFFFF)

@Composable
fun OnboardingScreen(
    onGitHubAuth: () -> Unit,
    onContinueWithoutAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    // Загрузка шрифтов
    val philosopherBold = FontFamily(Font(Res.font.philosopher_bold, FontWeight.Bold))
    val openSansBold = FontFamily(Font(Res.font.opensans_bold, FontWeight.Bold))
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WhiteBackground), // Белый фон на весь экран включая статус-бар
        contentAlignment = Alignment.Center
    ) {
        // Добавляем отступ для статус-бара на контент
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
        
        // Герб СПбГУ с прозрачностью 50%
        Image(
            painter = painterResource(Res.drawable.spbu_logo),
            contentDescription = "SPbU Logo",
            modifier = Modifier
                .fillMaxSize()
                .alpha(1.0f),
            contentScale = ContentScale.Fit
        )
        
        // Контент поверх фона
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Название приложения - сверху экрана
            Spacer(modifier = Modifier.height(80.dp))
            
            Text(
                text = getLocalizedAppName(),
                fontFamily = philosopherBold,
                fontSize = 40.sp,
                color = AppTitleColor,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Кнопка авторизации через GitHub - по центру экрана
            Button(
                onClick = {
                    onGitHubAuth()
                },
                modifier = Modifier
                    .width(261.dp)
                    .height(55.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(25.dp),
                        spotColor = Color.Black.copy(alpha = 1f),
                        ambientColor = Color.Black.copy(alpha = 1f)
                    )
                    .border(
                        width = 2.dp,
                        color = ButtonBorderColor,
                        shape = RoundedCornerShape(25.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonBackgroundColor
                ),
                shape = RoundedCornerShape(25.dp),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Лого GitHub - 35×35
                    Image(
                        painter = painterResource(Res.drawable.github_logo),
                        contentDescription = "GitHub Logo",
                        modifier = Modifier.size(35.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Текст кнопки
                    Text(
                        text = strings.loginWithGithub,
                        fontFamily = openSansBold,
                        fontSize = 20.sp,
                        color = ButtonTextColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Кликабельный текст "Продолжить без авторизации"
            Text(
                text = strings.continueWithoutAuth,
                fontFamily = openSansBold,
                fontSize = 14.sp,
                color = ContinueTextColor,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable(onClick = onContinueWithoutAuth)
                    .padding(8.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
        }
        } // Закрываем внутренний Box с systemBarsPadding
    } // Закрываем внешний Box
}

// Функции локализации
@Composable
expect fun getLocalizedAppName(): String

@Composable
expect fun getLocalizedAuthText(): String

@Composable
expect fun getLocalizedContinueText(): String
