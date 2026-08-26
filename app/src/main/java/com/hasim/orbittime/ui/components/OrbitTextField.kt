package com.hasim.orbittime.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.hasim.orbittime.ui.theme.OrbitColors
import com.hasim.orbittime.ui.theme.OrbitShapes
import com.hasim.orbittime.ui.theme.OrbitSpacing
import com.hasim.orbittime.ui.theme.OrbitTypography

/**
 * The reference form field: a small-caps label above a flat cream rounded box,
 * with an inline error line beneath it and a text-based show/hide toggle for
 * passwords instead of an icon (keeps the icon-free, minimal look).
 */
@Composable
fun OrbitTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorText: String? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = OrbitTypography.label,
            color = OrbitColors.slate500,
        )
        Spacer(modifier = Modifier.height(OrbitSpacing.xs))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errorText != null,
            textStyle = OrbitTypography.bodyLarge,
            shape = OrbitShapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            trailingIcon = if (isPassword) {
                {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = if (passwordVisible) "Hide" else "Show",
                            style = OrbitTypography.bodySmall,
                            color = OrbitColors.violet600,
                        )
                    }
                }
            } else {
                null
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = OrbitColors.cream50,
                unfocusedContainerColor = OrbitColors.cream50,
                disabledContainerColor = OrbitColors.cream50,
                errorContainerColor = OrbitColors.cream50,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                cursorColor = OrbitColors.violet600,
                focusedTextColor = OrbitColors.ink900,
                unfocusedTextColor = OrbitColors.ink900,
            ),
        )
        if (errorText != null) {
            Spacer(modifier = Modifier.height(OrbitSpacing.xxs))
            Text(
                text = errorText,
                style = OrbitTypography.bodySmall,
                color = OrbitColors.danger,
            )
        }
    }
}
