package com.spbu.projecttrack.core.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.spbu.projecttrack.core.settings.LocalAppStrings

@Composable
fun AndroidAppUpdateDialog(
    update: AndroidAppUpdate,
    onDismiss: () -> Unit,
    onUpdateClick: () -> Unit,
) {
    val strings = LocalAppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.updateAvailableTitle,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                Text(
                    text = strings.updateAvailableMessage,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                Text(
                    text = "${strings.currentVersionLabel}: ${update.currentVersionName}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(6.dp))
                Text(
                    text = "${strings.availableVersionLabel}: ${update.availableVersionName}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdateClick) {
                Text(strings.installUpdateLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.remindMeLaterLabel)
            }
        },
    )
}
