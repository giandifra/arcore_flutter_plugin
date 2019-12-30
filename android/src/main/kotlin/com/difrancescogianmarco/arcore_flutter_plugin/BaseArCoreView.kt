package com.difrancescogianmarco.arcore_flutter_plugin

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import com.difrancescogianmarco.arcore_flutter_plugin.utils.ArCoreUtils
import com.google.ar.core.ArCoreApk
import com.google.ar.sceneform.ArSceneView
import io.flutter.app.FlutterApplication
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

open class BaseArCoreView(val activity:Activity,context: Context, messenger: BinaryMessenger, id: Int) : PlatformView, MethodChannel.MethodCallHandler {

    lateinit var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks
    private val methodChannel: MethodChannel = MethodChannel(messenger, "arcore_flutter_plugin_$id")
    protected var arSceneView: ArSceneView? = null
//    protected val activity: Activity = (context.applicationContext as FlutterApplication).currentActivity
    protected val RC_PERMISSIONS = 0x123
    protected var installRequested: Boolean = false
    private val TAG: String = BaseArCoreView::class.java.name

    init {
        methodChannel.setMethodCallHandler(this)
        arSceneView = ArSceneView(context)
        ArCoreUtils.requestCameraPermission(activity, RC_PERMISSIONS)
        setupLifeCycle(context)
    }

    private fun setupLifeCycle(context: Context) {
        activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle) {
                Log.i(TAG, "onActivityCreated")
                maybeEnableArButton()
            }

            override fun onActivityStarted(activity: Activity) {
                Log.i(TAG, "onActivityStarted")
            }

            override fun onActivityResumed(activity: Activity) {
                Log.i(TAG, "onActivityResumed")
                onResume()
            }

            override fun onActivityPaused(activity: Activity) {
                Log.i(TAG, "onActivityPaused")
                onPause()
            }

            override fun onActivityStopped(activity: Activity) {
                Log.i(TAG, "onActivityStopped")
                onPause()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                Log.i(TAG, "onActivityDestroyed")
                onDestroy()
            }
        }

       activity.application
                .registerActivityLifecycleCallbacks(this.activityLifecycleCallbacks)
    }

    fun maybeEnableArButton() {
        val availability = ArCoreApk.getInstance().checkAvailability(activity.applicationContext)
        if (availability.isTransient) {
            // Re-query at 5Hz while compatibility is checked in the background.
            Handler().postDelayed({ maybeEnableArButton() }, 200)
        }
        if (availability.isSupported) {
            Log.i(TAG, "AR SUPPORTED")
        } else { // Unsupported or unknown.
            Log.i(TAG, "AR NOT SUPPORTED")
        }
    }

    override fun getView(): View {
        return arSceneView as View
    }

    override fun dispose() {
        if (arSceneView != null) {
            onPause()
            onDestroy()
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {

    }

    open fun onResume() {

        if (arSceneView == null) {
            return
        }

//        if (arSceneView?.session == null) {
//
//            // request camera permission if not already requested
//            if (!ArCoreUtils.hasCameraPermission(activity)) {
//                ArCoreUtils.requestCameraPermission(activity, RC_PERMISSIONS)
//            }
//
//            // If the session wasn't created yet, don't resume rendering.
//            // This can happen if ARCore needs to be updated or permissions are not granted yet.
//            try {
//                val session = ArCoreUtils.createArSession(activity, installRequested, isAugmentedFaces)
//                if (session == null) {
//                    installRequested = ArCoreUtils.hasCameraPermission(activity)
//                    return
//                } else {
//                    val config = Config(session)
//                    if (isAugmentedFaces) {
//                        config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
//                    }
//                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
//                    session.configure(config)
//                    arSceneView?.setupSession(session)
//                }
//            } catch (e: UnavailableException) {
//                ArCoreUtils.handleSessionException(activity, e)
//            }
//        }
//
//        try {
//            arSceneView?.resume()
//        } catch (ex: CameraNotAvailableException) {
//            ArCoreUtils.displayError(activity, "Unable to get camera", ex)
//            activity.finish()
//            return
//        }
    }

    fun onPause() {
        if (arSceneView != null) {
            arSceneView?.pause()
        }
    }

    open fun onDestroy() {
        if (arSceneView != null) {
            arSceneView?.destroy()
            arSceneView = null
        }
    }
}