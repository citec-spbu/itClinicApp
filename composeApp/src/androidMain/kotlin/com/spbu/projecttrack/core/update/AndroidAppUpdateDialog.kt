package com.spbu.projecttrack.core.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spbu.projecttrack.core.settings.AppLanguage
import com.spbu.projecttrack.core.settings.AppThemeMode
import com.spbu.projecttrack.core.settings.AppUiSettingsController
import com.spbu.projecttrack.core.settings.ITClinicTheme
import com.spbu.projecttrack.core.settings.LocalAppStrings
import com.spbu.projecttrack.core.settings.LocalSettingsPalette
import com.spbu.projecttrack.core.theme.AppFonts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val UpdateDialogShape = RoundedCornerShape(10.dp)
private val UpdateDialogButtonShape = RoundedCornerShape(10.dp)
private val UpdateDialogInfoShape = RoundedCornerShape(10.dp)
private val UpdateDialogBadgeShape = RoundedCornerShape(999.dp)

@Composable
fun AndroidAppUpdateDialog(
    update: AndroidAppUpdate,
    isInstalling: Boolean,
    onDismiss: (() -> Unit)?,
    onUpdateClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val palette = LocalSettingsPalette.current
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    fun dismissAnimated() {
        val dismissAction = onDismiss ?: return
        if (isInstalling || isDismissing) return
        isDismissing = true
        isVisible = false
        scope.launch {
            delay(180)
            dismissAction()
        }
    }

    Dialog(
        onDismissRequest = ::dismissAnimated,
        properties = DialogProperties(
            dismissOnBackPress = !update.isRequired,
            dismissOnClickOutside = !update.isRequired,
        ),
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
            ),
            exit = fadeOut() + scaleOut(
                targetScale = 0.94f,
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 620f),
            ),
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(),
                shape = UpdateDialogShape,
                color = palette.surface,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.dialogBorder),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(52.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            palette.accentBorder,
                                            palette.accent,
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            UpdateDialogBadge(
                                text = if (update.isRequired) "REQUIRED" else update.channel.name.uppercase(),
                            )
                            Text(
                                text = strings.updateAvailableTitle,
                                color = palette.secondaryText,
                                fontFamily = AppFonts.OpenSansBold,
                                fontSize = 24.sp,
                                lineHeight = 28.sp,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (update.isRequired) {
                            strings.updateRequiredMessage
                        } else {
                            strings.updateAvailableMessage
                        },
                        color = palette.primaryText,
                        fontFamily = AppFonts.OpenSansRegular,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        UpdateVersionCard(
                            title = strings.currentVersionLabel,
                            value = update.currentVersionName,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        UpdateVersionCard(
                            title = strings.availableVersionLabel,
                            value = update.availableVersionName,
                            modifier = Modifier.fillMaxWidth(),
                            accent = true,
                        )
                    }

                    if (update.changelog.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 188.dp)
                                .clip(UpdateDialogInfoShape)
                                .background(palette.background)
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text(
                                text = strings.updateWhatsNewLabel,
                                color = palette.secondaryText,
                                fontFamily = AppFonts.OpenSansBold,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            update.changelog.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "•",
                                        color = palette.accent,
                                        fontFamily = AppFonts.OpenSansBold,
                                        fontSize = 16.sp,
                                        lineHeight = 20.sp,
                                    )
                                    Text(
                                        text = item,
                                        modifier = Modifier.weight(1f),
                                        color = palette.primaryText,
                                        fontFamily = AppFonts.OpenSansRegular,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (onDismiss != null) {
                            UpdateDialogButton(
                                text = strings.remindMeLaterLabel,
                                background = palette.disabledButton,
                                onClick = ::dismissAnimated,
                                enabled = !isInstalling,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        UpdateDialogButton(
                            text = if (isInstalling) {
                                strings.updateDownloadingLabel
                            } else {
                                strings.installUpdateLabel
                            },
                            background = palette.accent,
                            borderColor = palette.accentBorder,
                            onClick = onUpdateClick,
                            enabled = !isInstalling,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateDialogBadge(text: String) {
    val palette = LocalSettingsPalette.current

    Box(
        modifier = Modifier
            .clip(UpdateDialogBadgeShape)
            .background(palette.accent.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = palette.accentBorder.copy(alpha = 0.55f),
                shape = UpdateDialogBadgeShape,
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = palette.accent,
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 11.sp,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
private fun UpdateVersionCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val palette = LocalSettingsPalette.current
    val backgroundColor = if (accent) palette.accent.copy(alpha = 0.08f) else palette.background
    val borderColor = if (accent) palette.accentBorder.copy(alpha = 0.65f) else palette.dialogBorder

    Column(
        modifier = modifier
            .clip(UpdateDialogInfoShape)
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = title,
            color = palette.secondaryText,
            fontFamily = AppFonts.OpenSansSemiBold,
            fontSize = 12.sp,
            lineHeight = 15.sp,
        )
        Text(
            text = value,
            color = palette.primaryText,
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UpdateDialogButton(
    text: String,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color? = null,
) {
    val palette = LocalSettingsPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 780f),
        label = "update_dialog_button_scale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(UpdateDialogButtonShape)
            .background(if (enabled) background else background.copy(alpha = 0.72f))
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, UpdateDialogButtonShape)
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = palette.buttonText,
            fontFamily = AppFonts.OpenSansBold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun AndroidAppUpdateDialogPreview() {
    ITClinicTheme(
        language = AppLanguage.Russian,
        themeMode = AppThemeMode.Light,
        settingsController = AppUiSettingsController(
            language = AppLanguage.Russian,
            themeMode = AppThemeMode.Light,
            onLanguageChange = {},
            onThemeModeChange = {},
        ),
    ) {
        AndroidAppUpdateDialog(
            update = AndroidAppUpdate(
                channel = AndroidUpdateChannel.Beta,
                versionCode = 245,
                currentVersionName = "main.244-a1b2c3d",
                availableVersionName = "main.245-d4e5f6a",
                apkUrl = "https://example.com/itclinicapp-release.apk",
                sha256 = "preview",
                releasePageUrl = "https://example.com/release",
                changelog = listOf(
                    "Исправлена авторизация через GitHub",
                    "Добавлена встроенная установка APK после загрузки",
                    "Обновлен канал доставки обновлений через GitHub",
                ),
                isRequired = false,
            ),
            isInstalling = false,
            onDismiss = {},
            onUpdateClick = {},
        )
    }
}

@Preview
@Composable
private fun AndroidAppRequiredUpdateDialogPreview() {
    ITClinicTheme(
        language = AppLanguage.Russian,
        themeMode = AppThemeMode.Light,
        settingsController = AppUiSettingsController(
            language = AppLanguage.Russian,
            themeMode = AppThemeMode.Light,
            onLanguageChange = {},
            onThemeModeChange = {},
        ),
    ) {
        AndroidAppUpdateDialog(
            update = AndroidAppUpdate(
                channel = AndroidUpdateChannel.Stable,
                versionCode = 310,
                currentVersionName = "v1.4.2",
                availableVersionName = "v1.5.0",
                apkUrl = "https://example.com/itclinicapp-release.apk",
                sha256 = "preview",
                releasePageUrl = "https://example.com/release",
                changelog = listOf(
                    "Обновлен API-контракт приложения",
                    "Старые версии больше не поддерживаются",
                ),
                isRequired = true,
            ),
            isInstalling = false,
            onDismiss = null,
            onUpdateClick = {},
        )
    }
}
