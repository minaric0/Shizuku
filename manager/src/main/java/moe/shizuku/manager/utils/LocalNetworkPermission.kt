package moe.shizuku.manager.utils

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build

/**
 * Android 17 gates NSD/ADB pairing behind a runtime Nearby devices permission.
 * The prompt must originate from an Activity; never request it from a boot receiver.
 */
object LocalNetworkPermission {
    private const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
    private const val REQUEST_CODE = 3701

    fun isGranted(context: android.content.Context): Boolean =
        Build.VERSION.SDK_INT < 37 ||
            context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun ensure(activity: Activity): Boolean {
        if (isGranted(activity)) return true
        activity.requestPermissions(arrayOf(PERMISSION), REQUEST_CODE)
        return false
    }
}
