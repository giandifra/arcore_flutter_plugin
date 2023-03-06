package com.difrancescogianmarco.arcore_flutter_plugin

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
import com.difrancescogianmarco.arcore_flutter_plugin.utils.ArCoreUtils
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformationSystem
import com.gorisse.thomas.sceneform.light.LightEstimationConfig
import com.gorisse.thomas.sceneform.lightEstimationConfig
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

interface OnSessionConfigurationListener {
    /**
     * The callback will only be invoked once after a Session is initialized and before it is
     * resumed for the first time.
     *
     * @param session The ARCore Session.
     * @param config  The ARCore Session Config.
     * @see .setOnSessionConfigurationListener
     */
    fun onSessionConfiguration(session: Session, config: Config)
}

open class BaseArCoreView(
    val activity: Activity,
    context: Context,
    messenger: BinaryMessenger,
    id: Int,
    protected val debug: Boolean
) : PlatformView, MethodChannel.MethodCallHandler {

    var onSessionConfigurationListener: OnSessionConfigurationListener? = null
    lateinit var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks
    protected val methodChannel: MethodChannel =
        MethodChannel(messenger, "arcore_flutter_plugin_$id")
    protected var arSceneView: ArSceneView = ArSceneView(context)

    val mediaPlayers: MutableList<MediaPlayer> = ArrayList()
    protected val RC_PERMISSIONS = 0x123
    protected var installRequested: Boolean = false
    private val TAG: String = BaseArCoreView::class.java.name
    protected var isSupportedDevice = false
    var isStarted = false
    private var sessionInitializationFailed = false
    var canRequestDangerousPermissions = true

    init {
        debugLog("init");
        requestDangerousPermissions()
        methodChannel.setMethodCallHandler(this)
        if (ArCoreUtils.checkIsSupportedDeviceOrFinish(activity)) {
            isSupportedDevice = true
            ArCoreUtils.requestCameraPermission(activity, RC_PERMISSIONS)
            setupLifeCycle(context)
        }
        debugLog("init complete")
    }

    fun initializeSession() {
        debugLog("initializeSession")
        // Only try once
        if (sessionInitializationFailed) {
            return
        }
        // if we have the camera permission, create the session
        if (ContextCompat.checkSelfPermission(activity, "android.permission.CAMERA")
            == PackageManager.PERMISSION_GRANTED
        ) {
            var sessionException: UnavailableException? = null
            try {
                if (requestInstall()) {
                    return
                }
                val session: Session = onCreateSession()
                val config: Config = onCreateSessionConfig(session)

                // listener per modificare parametri da una classe che estende BaseArCoreView
                onSessionConfigurationListener?.onSessionConfiguration(session, config)

                if (session.cameraConfig.facingDirection == CameraConfig.FacingDirection.FRONT
                    && config.lightEstimationMode == Config.LightEstimationMode.ENVIRONMENTAL_HDR
                ) {
                    config.lightEstimationMode = Config.LightEstimationMode.DISABLED
                }
                session.configure(config)
                arSceneView.session = session
                return
            } catch (e: UnavailableException) {
                sessionException = e
            } catch (e: java.lang.Exception) {
                sessionException = UnavailableException()
                sessionException.initCause(e)
            }
            sessionInitializationFailed = true
            onArUnavailableException(sessionException)
        } else {
            requestDangerousPermissions()
        }
    }

    private fun onCreateSession(): Session {
        debugLog("onCreateSession")
        return Session(activity)
    }

    fun onCreateSessionConfig(session: Session?): Config {
        debugLog("onCreateSessionConfig")
        val config = Config(session)
        val lightEstimationConfig: LightEstimationConfig? =
            if (arSceneView != null) arSceneView.lightEstimationConfig else null
        if (lightEstimationConfig != null) {
            config.lightEstimationMode = lightEstimationConfig.mode
        }
        config.depthMode = Config.DepthMode.DISABLED
        // only for augmented images
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        config.focusMode = Config.FocusMode.AUTO
        // Force the non-blocking mode for the session.
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        return config
    }

    @Throws(UnavailableException::class)
    private fun requestInstall(): Boolean {
        when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                installRequested = true
                return true
            }
            ArCoreApk.InstallStatus.INSTALLED -> {}
        }
        return false
    }

    private fun onArUnavailableException(sessionException: UnavailableException?) {
        //if (onArUnavailableListener != null) {
        //   onArUnavailableListener.onArUnavailableException(sessionException)
        //} else {
        val message: String
        if (sessionException is UnavailableArcoreNotInstalledException) {
            message = "getString(R.string.sceneform_unavailable_arcore_not_installed)"
        } else if (sessionException is UnavailableApkTooOldException) {
            message = "getString(R.string.sceneform_unavailable_apk_too_old)"
        } else if (sessionException is UnavailableSdkTooOldException) {
            message = "getString(R.string.sceneform_unavailable_sdk_too_old)"
        } else if (sessionException is UnavailableDeviceNotCompatibleException) {
            message = "getString(R.string.sceneform_unavailable_device_not_compatible)"
        } else {
            message = "getString(R.string.sceneform_failed_to_create_ar_session)"
        }
        Log.e(TAG, "Error: $message", sessionException)
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        //}
    }

    protected fun requestDangerousPermissions() {
        if (!canRequestDangerousPermissions) {
            // If this is in progress, don't do it again.
            return
        }
        canRequestDangerousPermissions = false
        val permissions: MutableList<String> = ArrayList()
        val additionalPermissions: MutableList<String> = ArrayList()
        val permissionLength = additionalPermissions?.size ?: 0
        for (i in 0 until permissionLength) {
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    additionalPermissions[i]
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(additionalPermissions[i])
            }
        }

        //TODO : Use ARCore CameraPermissionHelper.requestCameraPermission(this); instead

        // Always check for camera permission
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (permissions.isNotEmpty()) {
            debugLog("permissions is not empty")
            /*val requestMultiplePermissions: ActivityResultLauncher<Array<String>> =
                registerForActivityResult<Array<String>, Map<String, Boolean>>(
                    RequestMultiplePermissions(),
                    ActivityResultCallback<Map<String, Boolean?>> { results: Map<String, Boolean?> ->
                        results.forEach { (key: String, value: Boolean?) ->
                            if (key == Manifest.permission.CAMERA) {
                                if (!value!!) {
                                    val builder: AlertDialog.Builder = AlertDialog.Builder(
                                        activity,
                                        android.R.style.Theme_Material_Dialog_Alert
                                    )
                                    builder
                                        .setTitle(R.string.sceneform_camera_permission_required)
                                        .setMessage(R.string.sceneform_add_camera_permission_via_settings)
                                        .setPositiveButton(
                                            android.R.string.ok
                                        ) { dialog: DialogInterface?, which: Int ->
                                            // If Ok was hit, bring up the Settings app.
                                            val intent = Intent()
                                            intent.action =
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                            intent.data = Uri.fromParts(
                                                "package",
                                                activity.packageName,
                                                null
                                            )
                                            activity.startActivity(intent)
                                            // When the user closes the Settings app, allow the app to resume.
                                            // Allow the app to ask for permissions again now.
                                            canRequestDangerousPermissions = true
                                        }
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .setOnDismissListener { arg0: DialogInterface? ->
                                            // canRequestDangerousPermissions will be true if "OK" was selected from the dialog,
                                            // false otherwise.  If "OK" was selected do nothing on dismiss, the app will
                                            // continue and may ask for permission again if needed.
                                            // If anything else happened, finish the activity when this dialog is
                                            // dismissed.
                                            if (!canRequestDangerousPermissions) {
                                                activity.finish()
                                            }
                                        }
                                        .show()
                                }
                            } else {
                                // If any other user defined permission is not
                                // granted, finish the Activity.
                                if (!value!!) activity.finish()
                            }
                        }
                    })*/

            // Request the permissions
            //requestMultiplePermissions.launch(permissions.toTypedArray())
        }
    }

    fun getTransformationSystem(): TransformationSystem {
        val selectionVisualizer = FootprintSelectionVisualizer()

        val transformationSystem =
            TransformationSystem(activity.resources.displayMetrics, selectionVisualizer)

        ModelRenderable.builder()
            .setSource(activity, R.raw.sceneform_footprint)
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { renderable: ModelRenderable? ->
                // If the selection visualizer already has a footprint renderable, then it was set to
                // something custom. Don't override the custom visual.
                if (selectionVisualizer.footprintRenderable == null) {
                    selectionVisualizer.footprintRenderable = renderable
                }
            }
            .exceptionally {
                Log.e(TAG, it.toString())
                val toast = Toast.makeText(
                    activity, "Unable to load footprint renderable", Toast.LENGTH_LONG
                )
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                null
            }
        return transformationSystem
    }

    protected fun debugLog(message: String) {
        if (debug) {
            Log.d(TAG, message)
        }
    }

    override fun getView(): View {
        return arSceneView
    }

    override fun dispose() {
        debugLog("dispose")
        activity.application.unregisterActivityLifecycleCallbacks(this.activityLifecycleCallbacks)
        onDestroy()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {}

    fun attachNodeToParent(node: Node?, parentNodeName: String?) {
        if (parentNodeName != null) {
            debugLog(parentNodeName)
            val parentNode: Node? = arSceneView?.scene?.findByName(parentNodeName)
            parentNode?.addChild(node)
        } else {
            debugLog("addNodeToSceneWithGeometry: NOT PARENT_NODE_NAME")
            arSceneView?.scene?.addChild(node)
        }
    }

    fun onAddNode(flutterArCoreNode: FlutterArCoreNode, result: MethodChannel.Result?) {
        debugLog(flutterArCoreNode.toString())
        NodeFactory.makeNode(
            activity.applicationContext,
            flutterArCoreNode,
            debug
        ) { node, throwable ->
            debugLog("inserted ${node?.name}")

/*            if (flutterArCoreNode.parentNodeName != null) {
                debugLog(flutterArCoreNode.parentNodeName);
                val parentNode: Node? = arSceneView?.scene?.findByName(flutterArCoreNode.parentNodeName)
                parentNode?.addChild(node)
            } else {
                debugLog("addNodeToSceneWithGeometry: NOT PARENT_NODE_NAME")
                arSceneView?.scene?.addChild(node)
            }*/
            if (node != null) {
                attachNodeToParent(node, flutterArCoreNode.parentNodeName)
                for (n in flutterArCoreNode.children) {
                    n.parentNodeName = flutterArCoreNode.name
                    onAddNode(n, null)
                }
                result?.success(null)
            } else if (throwable != null) {
                result?.error("onAddNode", throwable.localizedMessage, null)
            }
        }
    }
/*
    fun addVideoNode(anchorNode: AnchorNode, byteArray: ByteArray?) {
        try {
            val player = MediaPlayer()
            NodeFactory.createVideoNode(
                activity,
                arSceneView,
                anchorNode,
                player,
                getTransformationSystem(),
                byteArray,
            )
            mediaPlayers.add(player)
        } catch (ex: Exception) {
            debugLog(ex.toString())
        }
    }
*/
    fun removeNode(name: String, result: MethodChannel.Result?) {
        val node = arSceneView?.scene?.findByName(name)
        if (node != null) {
            arSceneView?.scene?.removeChild(node)
            debugLog("removed ${node.name}")
        }
        result?.success(null)
    }

    fun removeNode(node: Node) {
        arSceneView.scene?.removeChild(node)
        debugLog("removed ${node.name}")
    }

    // LifeCycle
    private fun setupLifeCycle(context: Context) {
        activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                debugLog("onActivityCreated")
            }

            override fun onActivityStarted(activity: Activity) {
                debugLog("onActivityStarted")
            }

            override fun onActivityResumed(activity: Activity) {
                debugLog("onActivityResumed")
                onResume()
            }

            override fun onActivityPaused(activity: Activity) {
                debugLog("onActivityPaused")
                onPause()
            }

            override fun onActivityStopped(activity: Activity) {
                debugLog("onActivityStopped")
                //onPause()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                debugLog("onActivityDestroyed")
                onDestroy()
            }
        }

        activity.application
            .registerActivityLifecycleCallbacks(this.activityLifecycleCallbacks)
    }

    open fun onResume() {
        debugLog("onResume")
        if (arSceneView.session == null) {
            initializeSession()
        }
        if (isStarted) {
            return
        }
        if (activity != null) {
            isStarted = true
            try {
                arSceneView.resume()
            } catch (ex: java.lang.Exception) {
                sessionInitializationFailed = true
            }
            //if (!sessionInitializationFailed) {
            //    debugLog("sessionInitializationFailed")
            //}
        }
    }

    open fun onPause() {
        debugLog("onPause()")
        for (mediaPlayer in mediaPlayers) {
            mediaPlayer.pause()
        }
        pause()
    }

    private fun pause() {
        debugLog("onPause()")
        if (!isStarted) {
            return
        }
        isStarted = false
        arSceneView.pause()
    }

    open fun onDestroy() {
        debugLog("onDestroy()")
        for (mediaPlayer in mediaPlayers) {
            mediaPlayer.stop()
            mediaPlayer.reset()
        }
        destroy()
    }

    private fun destroy() {
        pause()
        arSceneView.destroy()
    }

}