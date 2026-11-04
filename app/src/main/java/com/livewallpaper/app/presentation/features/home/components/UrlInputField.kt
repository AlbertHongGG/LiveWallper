package com.livewallpaper.app.presentation.features.home.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.presentation.theme.AppColors

/**
 * 網址解析輸入膠囊組件。
 * 單一職責：處理 URL 字串輸入、剪貼簿一鍵貼上與送出，完全與下載/匯入進度解耦。
 */
@Composable
fun UrlInputField(
    urlText: String,
    onUrlChange: (String) -> Unit,
    onSubmitUrl: (String) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEmpty = urlText.isBlank()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color.Black.copy(alpha = 0.08f),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(AppColors.surfaceLight)
            .border(
                width = 1.dp,
                color = Color(0xFFE2E8F0),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = AppColors.secondary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (isEmpty) {
                    Text(
                        text = "貼上 Pinterest 影片網址",
                        color = AppColors.textMuted,
                        fontSize = 14.sp
                    )
                }

                BasicTextField(
                    value = urlText,
                    onValueChange = onUrlChange,
                    singleLine = true,
                    enabled = !isLoading,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.textPrimaryLight
                    ),
                    cursorBrush = SolidColor(AppColors.accentIndigo),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        if (!isEmpty && !isLoading) onSubmitUrl(urlText)
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!isEmpty && !isLoading) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable { onUrlChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "清除",
                        tint = AppColors.textSecondaryLight,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 動作按鈕：空白時一鍵貼上剪貼簿，有文字時開始解析
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLoading) Color(0xFFF1F5F9)
                        else AppColors.accentIndigo
                    )
                    .clickable(enabled = !isLoading) {
                        if (isEmpty) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                            if (clipText.isNotBlank()) {
                                onUrlChange(clipText)
                                onSubmitUrl(clipText)
                            }
                        } else {
                            onSubmitUrl(urlText)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = AppColors.accentIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (isEmpty) Icons.Default.ContentPaste else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
