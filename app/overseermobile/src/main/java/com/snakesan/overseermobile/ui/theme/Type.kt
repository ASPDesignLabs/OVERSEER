package com.snakesan.overseermobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.snakesan.overseermobile.R

/** The watch face's display font, copied into this module's own resources. */
val CyberFont = FontFamily(Font(R.font.cyberfont))

// Headings and wedge labels use the cyber font to match the watch. Body and
// help text stay on the system default font at a comfortable size, since
// stylized display fonts are harder to read at length.
val Typography = Typography(
    headlineSmall = TextStyle(
        fontFamily = CyberFont,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        letterSpacing = 1.sp
    ),
    titleMedium = TextStyle(
        fontFamily = CyberFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    )
)
