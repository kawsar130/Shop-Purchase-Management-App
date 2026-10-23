package com.shopkeeper.ui.screens.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopkeeper.ui.theme.BluePrimary
import com.shopkeeper.ui.theme.PureWhite
import com.shopkeeper.ui.theme.Slate200
import com.shopkeeper.ui.theme.Slate500
import com.shopkeeper.ui.theme.Slate700
import com.shopkeeper.ui.theme.Slate900

/**
 * High-contrast, shopkeeper-friendly input field.
 * Guaranteed readable in both Dark and Light system themes.
 */
@Composable
fun ShopTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label?.let {
            {
                Text(
                    text = it,
                    color = Slate700,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        },
        placeholder = placeholder?.let {
            {
                Text(
                    text = it,
                    color = Slate500,
                    fontSize = 15.sp
                )
            }
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = TextStyle(
            color = Slate900,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Slate900,
            unfocusedTextColor = Slate900,
            focusedContainerColor = PureWhite,
            unfocusedContainerColor = PureWhite,
            disabledContainerColor = PureWhite,
            focusedBorderColor = BluePrimary,
            unfocusedBorderColor = Slate200,
            cursorColor = BluePrimary,
            focusedLabelColor = BluePrimary,
            unfocusedLabelColor = Slate500
        )
    )
}
