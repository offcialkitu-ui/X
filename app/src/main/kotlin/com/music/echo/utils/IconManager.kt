
package iad1tya.echo.music.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconManager {
    private const val BASE_PACKAGE = "iad1tya.echo.music"
    
    val ICONS = listOf(
        "MainActivityAlias", // Default
        "MainActivityLegacy",
        "MainActivityStatic",
        "MainActivityPurple",
        "MainActivityParty"
    )

    fun setIcon(context: Context, iconName: String) {
        val packageManager = context.packageManager
        ICONS.forEach { name ->
            val state = if (name == iconName) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(
                ComponentName(context, "$BASE_PACKAGE.$name"),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
