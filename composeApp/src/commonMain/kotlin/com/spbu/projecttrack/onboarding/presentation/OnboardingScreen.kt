package com.spbu.projecttrack.onboarding.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.platform.appVersionName
import com.spbu.projecttrack.core.settings.LocalAppStrings
import com.spbu.projecttrack.core.theme.appPalette
import com.spbu.projecttrack.core.theme.dimText
import com.spbu.projecttrack.core.theme.subtleBorder
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.*
import projecttrack.composeapp.generated.resources.Res

private val GitHubButtonWidth = 261.dp
private val GitHubButtonHeight = 55.dp
private val GitHubButtonShape = RoundedCornerShape(25.dp)
private val GitHubButtonContentPadding = 9.dp

@Composable
fun OnboardingScreen(
    onGitHubAuth: () -> Unit,
    onContinueWithoutAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val palette = appPalette()
    // Загрузка шрифтов
    val philosopherBold = FontFamily(Font(Res.font.philosopher_bold, FontWeight.Bold))
    val openSansBold = FontFamily(Font(Res.font.opensans_bold, FontWeight.Bold))

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Добавляем отступ для статус-бара на контент
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {

        // Герб СПбГУ: в тёмной теме — 50% непрозрачности (см. SettingsPalette.spbuBackdropLogoAlpha).
        Image(
            painter = painterResource(Res.drawable.spbu_logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(palette.spbuBackdropLogoAlpha),
            contentScale = ContentScale.Fit
        )

        // Контент поверх фона
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Название приложения - сверху экрана
            Spacer(modifier = Modifier.height(80.dp))

            Text(
                text = strings.appDisplayName,
                fontFamily = philosopherBold,
                fontSize = 40.sp,
                color = palette.title,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            GitHubLoginButton(
                text = strings.loginWithGithub,
                textFontFamily = openSansBold,
                onClick = onGitHubAuth,
            )

            Spacer(modifier = Modifier.height(24.dp))

            ContinueWithoutAuthButton(
                text = strings.continueWithoutAuth,
                textFontFamily = openSansBold,
                onClick = onContinueWithoutAuth,
            )

            Spacer(modifier = Modifier.weight(1f))

            val version = appVersionName()
            if (version.isNotBlank()) {
                Text(
                    text = version,
                    fontFamily = openSansBold,
                    fontSize = 11.sp,
                    color = palette.dimText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        } // Закрываем внутренний Box с systemBarsPadding
    } // Закрываем внешний Box
}

@Composable
private fun GitHubLoginButton(
    text: String,
    textFontFamily: FontFamily,
    onClick: () -> Unit,
) {
    val palette = appPalette()
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonBackgroundColor = Color(0xFFA8ADB4)
    val buttonBorderColor = Color(0xFFD0D5DC)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.972f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 780f),
        label = "onboarding_github_button_scale",
    )
    val shadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.18f else 0.28f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "onboarding_github_button_shadow_alpha",
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFB0B5BC) else buttonBackgroundColor,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 620f),
        label = "onboarding_github_button_background",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFE2E5EA) else buttonBorderColor,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 620f),
        label = "onboarding_github_button_border",
    )

    Box(
        modifier = Modifier
            .requiredWidth(GitHubButtonWidth)
            .height(GitHubButtonHeight)
            .scale(scale)
            .shadow(
                elevation = 14.dp,
                shape = GitHubButtonShape,
                spotColor = Color.Black.copy(alpha = shadowAlpha),
                ambientColor = Color.Black.copy(alpha = shadowAlpha * 0.85f),
            )
            .background(
                color = backgroundColor,
                shape = GitHubButtonShape,
            )
            .border(
                width = 2.dp,
                color = borderColor,
                shape = GitHubButtonShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GitHubButtonContentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.github_logo),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = text,
                fontFamily = textFontFamily,
                fontSize = 20.sp,
                color = palette.primaryText,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ContinueWithoutAuthButton(
    text: String,
    textFontFamily: FontFamily,
    onClick: () -> Unit,
) {
    val palette = appPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 760f),
        label = "onboarding_continue_button_scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "onboarding_continue_button_alpha",
    )
    val color by animateColorAsState(
        targetValue = if (isPressed) palette.secondaryText else palette.dimText,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 680f),
        label = "onboarding_continue_button_color",
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .alpha(alpha)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = textFontFamily,
            fontSize = 14.sp,
            color = color,
            textDecoration = TextDecoration.Underline,
            textAlign = TextAlign.Center,
        )
    }
}

