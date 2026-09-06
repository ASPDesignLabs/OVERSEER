package com.snakesan.overseermobile.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

/**
 * Lists apps the user can pick as a shortcut target. Querying
 * ACTION_MAIN/CATEGORY_LAUNCHER activities is exempt from Android 11+
 * package-visibility filtering, so no extra manifest permission is needed.
 */
object InstalledAppsProvider {
    suspend fun listLaunchableApps(context: Context): List<LaunchableApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolved = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        resolved
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(pm)?.toString() ?: packageName
                val icon = try {
                    resolveInfo.loadIcon(pm)
                } catch (e: Exception) {
                    null
                } ?: return@mapNotNull null
                LaunchableApp(packageName, label, icon)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
