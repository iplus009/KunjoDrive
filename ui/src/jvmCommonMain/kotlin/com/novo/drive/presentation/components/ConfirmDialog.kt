package com.novo.drive.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import com.novo.drive.resources.Res
import com.novo.drive.resources.common_cancel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        modifier = Modifier
            .padding(16.dp)
            .liquidGlassBackground(
                shape = RoundedCornerShape(22.dp),
                tintAlpha = if (isDark) 0.85f else 0.95f,
                borderAlpha = 0.40f
            ),
        title = {
            Text(
                title,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
        },
        text = {
            Text(
                message,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shapes = ButtonDefaults.shapes(),
                colors = if (destructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes()
            ) { Text(stringResource(Res.string.common_cancel)) }
        }
    )
}
