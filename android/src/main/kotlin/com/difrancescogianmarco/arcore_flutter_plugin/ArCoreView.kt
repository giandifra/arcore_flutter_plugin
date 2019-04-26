package com.difrancescogianmarco.arcore_flutter_plugin

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
import com.difrancescogianmarco.arcore_flutter_plugin.models.RotatingNode
import com.difrancescogianmarco.arcore_flutter_plugin.utils.ArCoreUtils
import com.difrancescogianmarco.arcore_flutter_plugin.utils.DecodableUtils.Companion.parseVector3
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ModelRenderable
import io.flutter.app.FlutterApplication
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

class ArCoreView(context: Context, messenger: BinaryMessenger, id: Int) : PlatformView, MethodChannel.MethodCallHandler {
    val methodChannel: MethodChannel
    val activity: Activity
    lateinit var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks
    var installRequested: Boolean = false
    val TAG: String = ArCoreView::class.java.name
    private var arSceneView: ArSceneView?
    val gestureDetector: GestureDetector
    private val RC_PERMISSIONS = 0x123

    init {
        this.activity = (context.applicationContext as FlutterApplication).currentActivity
        methodChannel = MethodChannel(messenger, "arcore_flutter_plugin_$id")
        methodChannel.setMethodCallHandler(this)
        arSceneView = ArSceneView(context)
        // Set up a tap gesture detector.
        gestureDetector = GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        onSingleTap(e)
                        return true
                    }

                    override fun onDown(e: MotionEvent): Boolean {
                        return true
                    }
                })

        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        arSceneView?.scene
                ?.addOnUpdateListener { frameTime ->

                    val frame = arSceneView?.arFrame ?: return@addOnUpdateListener

                    if (frame.camera.trackingState != TrackingState.TRACKING) {
                        return@addOnUpdateListener
                    }

                    for (plane in frame.getUpdatedTrackables(Plane::class.java)) {
                        if (plane.trackingState == TrackingState.TRACKING) {
                            //TODO il piano è stato rilevato
                        }
                    }
                }

        // Lastly request CAMERA permission which is required by ARCore.
        ArCoreUtils.requestCameraPermission(activity, RC_PERMISSIONS)
        setupLifeCycle(context)
    }

    private fun setupLifeCycle(context: Context) {
        activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle) {
                Log.i(TAG, "onActivityCreated")
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

        (context.getApplicationContext() as FlutterApplication).currentActivity.application
                .registerActivityLifecycleCallbacks(this.activityLifecycleCallbacks)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "init" -> {
                arScenViewInit(call, result);
            }
            "addArCoreNode" -> {
                Log.i(TAG, " addArCoreNode")
                val map = call.arguments as HashMap<String, Any>
                val flutterNode = FlutterArCoreNode(map);
                onAddNode(flutterNode, result, map)
            }
            "positionChanged" -> {
                Log.i(TAG, " positionChanged")
                updatePosition(call, result)

            }
            "rotationChanged" -> {
                Log.i(TAG, " rotationChanged")
                updateRotation(call, result)

            }
            "updateMaterials" -> {
                Log.i(TAG, " updateMaterials")
                updateMaterials(call, result)

            }
            else -> {
            }
        }
    }

    fun onResume() {
        if (arSceneView == null) {
            return
        }

        // request camera permission if not already requested
        if (!ArCoreUtils.hasCameraPermission(activity)) {
            ArCoreUtils.requestCameraPermission(activity, RC_PERMISSIONS)
        }


        if (arSceneView?.session == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                val session = ArCoreUtils.createArSession(activity, installRequested)
                if (session == null) {
                    installRequested = ArCoreUtils.hasCameraPermission(activity)
                    return
                } else {
                    arSceneView?.setupSession(session)
                }
            } catch (e: UnavailableException) {
                ArCoreUtils.handleSessionException(activity, e)
            }

        }

        try {
            arSceneView?.resume()
        } catch (ex: CameraNotAvailableException) {
            ArCoreUtils.displayError(activity, "Unable to get camera", ex)
            activity.finish()
            return
        }

        if (arSceneView?.getSession() != null) {
            Log.i(TAG, "Searching for surfaces")
        }
    }

    private fun tryPlaceNode(tap: MotionEvent?, frame: Frame) {
        if (tap != null && frame.camera.trackingState == TrackingState.TRACKING) {
            for (hit in frame.hitTest(tap)) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    // Create the Anchor.
                    val anchor = hit.createAnchor()
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(arSceneView?.scene)

                    ModelRenderable.builder()
                            .setSource(activity.applicationContext, Uri.parse("Andy.sfb"))
                            .build().thenAccept { renderable ->
                                val node = Node()
                                node.renderable = renderable
                                anchorNode.addChild(node)
                            }.exceptionally { throwable ->
                                Log.e(TAG, "Unable to load Renderable.", throwable);
                                return@exceptionally null
                            }
                }
            }
        }

    }

    private fun onSingleTap(tap: MotionEvent) {
//        if (!hasFinishedLoading) {
//            // We can't do anything yet.
//            return
//        }
        Log.i(TAG, " onSingleTap")
        val frame = arSceneView?.arFrame
        if (frame != null) {
            tryPlaceNode(tap, frame)
        }
    }

    private fun arScenViewInit(call: MethodCall, result: MethodChannel.Result) {
        Log.i(TAG, "arScenViewInit")
        onResume() //TODO delete?
        val enableTapRecognizer: Boolean? = call.argument("enableTapRecognizer")
        if (enableTapRecognizer != null && enableTapRecognizer) {
            arSceneView
                    ?.scene
                    ?.setOnTouchListener { hitTestResult: HitTestResult, event: MotionEvent ->

                        if (hitTestResult.node != null) {
                            Log.i(TAG, " onTap " + hitTestResult.node?.name)
                            methodChannel.invokeMethod("onTap", hitTestResult.node?.name)
                            return@setOnTouchListener true
                        }

                        //TODO se l'oggetto non è stato inserito
                        if (true) {
                            return@setOnTouchListener gestureDetector.onTouchEvent(event)
                        }

                        // Otherwise return false so that the touch event can propagate to the scene.
                        false
                    }
        }

        result.success(null)
    }

    fun onAddNode(flutterArCoreNode: FlutterArCoreNode, result: MethodChannel.Result, map: HashMap<String, *>) {

        Log.i(TAG, flutterArCoreNode.toString())
//        val node = flutterArCoreNode.buildNode()
        NodeFactory.makeNode(activity.applicationContext, flutterArCoreNode) { node, throwable ->

            if (flutterArCoreNode.parentNodeName != null) {
                Log.i(TAG, flutterArCoreNode.parentNodeName);
                val parentNode: Node? = arSceneView?.scene?.findByName(flutterArCoreNode.parentNodeName)
                parentNode?.addChild(node)
            } else {
                Log.i(TAG, "addNodeToSceneWithGeometry: NOT PARENT_NODE_NAME")
                arSceneView?.scene?.addChild(node)
            }
        }
        result.success(null)
    }

    fun updatePosition(call: MethodCall, result: MethodChannel.Result) {
        val name = call.argument<String>("name")
        val node = arSceneView?.scene?.findByName(name)
        node?.localPosition = parseVector3(call.arguments as HashMap<String, Any>)
        result.success(null)
    }

    fun updateRotation(call: MethodCall, result: MethodChannel.Result) {
//        Log.i(TAG, "rotating")
//        Log.i(TAG, call.arguments.toString())
//        val name = call.argument<String>("name")
//        val node = arSceneView?.scene?.findByName(name)
//        node?.localRotation = parseVector4(call.arguments as HashMap<String, Double>)
//        Log.i(TAG, node?.localRotation.toString())

        val name = call.argument<String>("name")
        val node = arSceneView?.scene?.findByName(name) as RotatingNode
        Log.i(TAG, "rotating node:  $node")
        val degreesPerSecond = call.argument<Double?>("degreesPerSecond")
        Log.i(TAG, "rotating value:  $degreesPerSecond")
        if (degreesPerSecond != null) {
            Log.i(TAG, "rotating value:  ${node.degreesPerSecond}")
            node.degreesPerSecond = degreesPerSecond.toFloat()
        }
        result.success(null)
    }

    fun updateMaterials(call: MethodCall, result: MethodChannel.Result) {
        val name = call.argument<String>("name")
        val materials = call.argument<ArrayList<HashMap<String, *>>>("materials")!!
        val node = arSceneView?.scene?.findByName(name)
        val oldMaterial = node?.renderable?.material?.makeCopy()
        if (oldMaterial != null) {
            val material = MaterialCustomFactory.updateMaterial(oldMaterial, materials[0])
            node.renderable?.material = material
        }
        result.success(null)
    }

    override fun getView(): View {
        return arSceneView as View
    }

    override fun dispose() {
        if (arSceneView != null) {
            arSceneView?.destroy()
        }
    }

    fun onPause() {
        if (arSceneView != null) {
            arSceneView?.pause()
        }
    }

    fun onDestroy() {
        if (arSceneView != null) {
            arSceneView?.destroy()
        }
    }
}





