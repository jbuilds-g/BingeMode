package com.jbuilds.bingemode.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jbuilds.bingemode.R

private const val GoogleSansRoundedAxis = 100f

/**
 * Google Sans Flex rounded cut, matching mpvRx's app-wide body and UI typography.
 */
@OptIn(ExperimentalTextApi::class)
private val GoogleSansRounded = FontFamily(
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Thin, variationSettings = FontVariation.Settings(FontVariation.weight(100), FontVariation.Setting("ROND", GoogleSansRoundedAxis))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.ExtraLight, variationSettings = FontVariation.Settings(FontVariation.weight(200), FontVariation.Setting("ROND", GoogleSansRoundedAxis))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300), FontVariation.Setting("ROND", GoogleSansRoundedAxis))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400), FontVariation.Setting("ROND", GoogleSansRoundedAxis))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500), FontVariation.Setting("ROND", GoogleSansRoundedAxis))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600), FontVariation.Setting("ROND", GoogleSansRoundedAxis))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700), FontVariation.Setting("ROND", GoogleSansRoundedAxis))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800), FontVariation.Setting("ROND", GoogleSansRoundedAxis))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900), FontVariation.Setting("ROND", GoogleSansRoundedAxis)))
)

/**
 * Wider, slanted expressive display cut used by mpvRx for headings.
 */
@OptIn(ExperimentalTextApi::class)
private val GoogleSansExpressiveDisplay = FontFamily(
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500), FontVariation.width(110f), FontVariation.slant(-4f), FontVariation.Setting("GRAD", 60f), FontVariation.Setting("ROND", GoogleSansRoundedAxis))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600), FontVariation.width(111f), FontVariation.slant(-4f), FontVariation.Setting("GRAD", 70f), FontVariation.Setting("ROND", GoogleSansRoundedAxis))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700), FontVariation.width(112f), FontVariation.slant(-4f), FontVariation.Setting("GRAD", 80f), FontVariation.Setting("ROND", GoogleSansRoundedAxis))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800), FontVariation.width(112f), FontVariation.slant(-3f), FontVariation.Setting("GRAD", 90f), FontVariation.Setting("ROND", GoogleSansRoundedAxis))),
    Font(resId = R.font.google_sans_flex, weight = FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900), FontVariation.width(112f), FontVariation.slant(-2f), FontVariation.Setting("GRAD", 100f), FontVariation.Setting("ROND", GoogleSansRoundedAxis)))
)

/**
 * BingeMode's Material 3 type scale. Font sizes, line heights and tracking are
 * preserved; only the font's variable axes and semantic weight mapping change.
 */
val Typography = Typography(
    displayLarge = TextStyle(fontFamily = GoogleSansExpressiveDisplay, fontWeight = FontWeight.Black, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = 0.sp),
    displayMedium = TextStyle(fontFamily = GoogleSansExpressiveDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
    displaySmall = TextStyle(fontFamily = GoogleSansExpressiveDisplay, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = GoogleSansExpressiveDisplay, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = GoogleSansExpressiveDisplay, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = GoogleSansExpressiveDisplay, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    bodyMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
)
