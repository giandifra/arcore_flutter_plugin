package com.difrancescogianmarco.arcore_flutter_plugin.utils

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import androidx.core.app.ActivityCompat
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import com.google.ar.core.exceptions.*
import java.util.*
import androidx.core.content.ContextCompat.getSystemService
import android.os.Build.VERSION_CODES
import com.google.ar.core.*
import com.google.ar.core.CameraConfig


class ArCoreUtils {


    companion object {

        private val TAG = ArCoreUtils::class.java.name
        private val MIN_OPENGL_VERSION = 3.0
        private val CAMERA_PERMISSION_CODE = 0
        private val CAMERA_PERMISSION = Manifest.permission.CAMERA

        /**
         * Creates an ARCore session. This checks for the CAMERA permission, and if granted, checks the
         * state of the ARCore installation. If there is a problem an exception is thrown. Care must be
         * taken to update the installRequested flag as needed to avoid an infinite checking loop. It
         * should be set to true if null is returned from this method, and called again when the
         * application is resumed.
         *
         * @param activity - the activity currently active.
         * @param installRequested - the indicator for ARCore that when checking the state of ARCore, if
         * an installation was already requested. This is true if this method previously returned
         * null. and the camera permission has been granted.
         */
        @Throws(UnavailableException::class)
        fun createArSession(activity: Activity, userRequestedInstall: Boolean, isFrontCamera: Boolean): Session? {
            var session: Session? = null
            // if we have the camera permission, create the session
            if (hasCameraPermission(activity)) {
                session = when (ArCoreApk.getInstance().requestInstall(activity, userRequestedInstall)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        Log.i(TAG, "ArCore INSTALL REQUESTED")
                        null
                    }
                    //                    ArCoreApk.InstallStatus.INSTALLED -> {}
                    else -> {
                        if (isFrontCamera) {
                            Session(activity, EnumSet.of(Session.Feature.FRONT_CAMERA))
                        } else {
                            Session(activity)
                        }
                    }
                }
                session?.let {
                    // Create a camera config filter for the session.
                    val filter = CameraConfigFilter(it)

                    // Return only camera configs that target 30 fps camera capture frame rate.
                    filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))

                    // Return only camera configs that will not use the depth sensor.
                    filter.setDepthSensorUsage(EnumSet.of(CameraConfig.DepthSensorUsage.DO_NOT_USE))

                    // Get list of configs that match filter settings.
                    // In this case, this list is guaranteed to contain at least one element,
                    // because both TargetFps.TARGET_FPS_30 and DepthSensorUsage.DO_NOT_USE
                    // are supported on all ARCore supported devices.
                    val cameraConfigList = it.getSupportedCameraConfigs(filter)

                    // Use element 0 from the list of returned camera configs. This is because
                    // it contains the camera config that best matches the specified filter
                    // settings.
                    it.cameraConfig = cameraConfigList[0]
                }

            }
            return session
        }

        /** Check to see we have the necessary permissions for this app, and ask for them if we don't.  */
        fun requestCameraPermission(activity: Activity, requestCode: Int) {
            ActivityCompat.requestPermissions(
                    activity, arrayOf(Manifest.permission.CAMERA), requestCode)
        }

        /** Check to see we have the necessary permissions for this app.  */
        fun hasCameraPermission(activity: Activity): Boolean {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        }

        /** Check to see if we need to show the rationale for this permission.  */
        fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.CAMERA)
        }

        /** Launch Application Setting to grant permission.  */
        fun launchPermissionSettings(activity: Activity) {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", activity.packageName, null)
            activity.startActivity(intent)
        }

        /**
         * Creates and shows a Toast containing an error message. If there was an exception passed in it
         * will be appended to the toast. The error will also be written to the Log
         */
        fun displayError(
                context: Context, errorMsg: String, @Nullable problem: Throwable?) {
            val tag = context.javaClass.simpleName
            val toastText: String
            if (problem != null && problem.message != null) {
                Log.e(tag, errorMsg, problem)
                toastText = errorMsg + ": " + problem.message
            } else if (problem != null) {
                Log.e(tag, errorMsg, problem)
                toastText = errorMsg
            } else {
                Log.e(tag, errorMsg)
                toastText = errorMsg
            }

            Handler(Looper.getMainLooper())
                    .post {
                        val toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG)
                        toast.setGravity(Gravity.CENTER, 0, 0)
                        toast.show()
                    }
        }

        fun handleSessionException(
                activity: Activity, sessionException: UnavailableException) {

            val message: String
            if (sessionException is UnavailableArcoreNotInstalledException) {
                message = "Please install ARCore"
            } else if (sessionException is UnavailableApkTooOldException) {
                message = "Please update ARCore"
            } else if (sessionException is UnavailableSdkTooOldException) {
                message = "Please update this app"
            } else if (sessionException is UnavailableDeviceNotCompatibleException) {
                message = "This device does not support AR"
            } else {
                message = "Failed to create AR session"
                Log.e(TAG, "Exception: $sessionException")
            }
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }

        /**
         * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
         * on this device.
         *
         *
         * Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
         *
         *
         * Finishes the activity if Sceneform can not run
         */
        fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
            if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
                Log.e(TAG, "Sceneform requires Android N or later")
                Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show()
                activity.finish()
                return false
            }
            val openGlVersionString = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .deviceConfigurationInfo
                    .glEsVersion
            if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
                Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later")
                Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                        .show()
                activity.finish()
                return false
            }
            return true
        }
    }
}