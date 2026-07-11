package com.weib.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val WeibTypography = Typography(
    headlineLarge = TextStyle(color = WeibTitle, fontSize = 30.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(color = WeibTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(color = WeibTitle, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(color = WeibTitle, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(color = WeibBody, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(color = WeibBody, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
)
