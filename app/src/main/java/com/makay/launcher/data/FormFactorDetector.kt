package com.makay.launcher.data

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import com.makay.launcher.domain.AppEdition
import com.makay.launcher.domain.FormFactor
import kotlin.math.min

object FormFactorDetector {
    fun detect(context: Context): FormFactor {
        val config = context.resources.configuration
        val uiType = config.uiMode and Configuration.UI_MODE_TYPE_MASK
        val leanback = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            context.packageManager.hasSystemFeature("android.software.leanback_only")
        if (uiType == Configuration.UI_MODE_TYPE_TELEVISION || leanback) {
            return FormFactor.TV
        }
        val metrics = context.resources.displayMetrics
        val currentSmallest = min(
            metrics.widthPixels / metrics.density,
            metrics.heightPixels / metrics.density,
        ).toInt()
        return if (config.smallestScreenWidthDp >= 600 || currentSmallest >= 600) {
            FormFactor.TABLET
        } else {
            FormFactor.PHONE
        }
    }

    fun suggestedEdition(context: Context): AppEdition =
        if (detect(context) == FormFactor.TV) AppEdition.TV else AppEdition.MOBILE
}
