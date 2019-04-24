package com.difrancescogianmarco.arcore_flutter_plugin

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.difrancescogianmarco.arcore_flutter_plugin.DecodableUtils.Companion.parseVector3
import com.difrancescogianmarco.arcore_flutter_plugin.DecodableUtils.Companion.parseVector4
import com.difrancescogianmarco.arcore_flutter_plugin.GeometryBuilder.Companion.createModelRenderable
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.AnchorNode
import io.flutter.app.FlutterApplication
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*

class ArCoreView(context: Context, messenger: BinaryMessenger, id: Int) : PlatformView, MethodChannel.MethodCallHandler {
    val methodChannel: MethodChannel
    val activity: Activity
    lateinit var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks
    var installRequested: Boolean = false
    val TAG: String = ArCoreView::class.java.name
    private var arSceneView: ArSceneView?
    val gestureDetector: GestureDetector
    private var hasFinishedLoading = true //TODO quando completo il caricamento degli oggetti posso metterlo a true

    private val RC_PERMISSIONS = 0x123

    lateinit var redSphereRenderable: ModelRenderable

    init {
        this.activity = (context.applicationContext as FlutterApplication).currentActivity
        methodChannel = MethodChannel(messenger, "arcore_flutter_plugin_$id")
        methodChannel.setMethodCallHandler(this)
        arSceneView = ArSceneView(context)
        setupLifeCycle(context)
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


        MaterialFactory.makeOpaqueWithColor(activity.applicationContext, com.google.ar.sceneform.rendering.Color(Color.RED))
                .thenAccept { material: Material? ->
                    redSphereRenderable =
                            ShapeFactory.makeSphere(0.1f, Vector3(0.0f, 0.15f, 0.0f), material);
                }
    }

    private fun setupLifeCycle(context: Context) {
        activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle) {}

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

    protected fun onResume() {
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

    override fun getView(): View {
        return arSceneView as View
    }

    private fun onSingleTap(tap: MotionEvent) {
        if (!hasFinishedLoading) {
            // We can't do anything yet.
            return
        }
        Log.i(TAG, " onSingleTap")
        val frame = arSceneView?.arFrame
        if (frame != null) {
            if (addNode(tap, frame)) {
//                hasPlacedSolarSystem = true
            }
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "init" -> {
                arScenViewInit(call, result);
            }
            "addArCoreNode" -> {
                Log.i(TAG, " addArCoreNode")
                onAddNode(call, result)

            }
            else -> {
            }
        }
    }

    private fun arScenViewInit(call: MethodCall, result: MethodChannel.Result) {
        Log.i(TAG, "arScenViewInit")
        val enableTapRecognizer: Boolean? = call.argument("enableTapRecognizer")
        Log.i(TAG, " enableTapRecognizer" + enableTapRecognizer)
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

    //crea nodo quando clicchi
    private fun addNode(tap: MotionEvent?, frame: Frame): Boolean {
        if (tap != null && frame.camera.trackingState == TrackingState.TRACKING) {
            for (hit in frame.hitTest(tap)) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {

                    // Create the Anchor.
                    val anchor = hit.createAnchor()
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(arSceneView?.scene)

                    val node = Node()
                    node.name = "Palla rossa"
                    node.renderable = redSphereRenderable
                    anchorNode.addChild(node)
                }
                return true
            }
        }
        return false
    }


    fun onAddNode(call: MethodCall, result: MethodChannel.Result) {


        //TODO crea geometry
        addNodeToSceneWithGeometry(call, result)
    }

    fun addNodeToSceneWithGeometry(call: MethodCall, result: MethodChannel.Result) {
        val node = getNodeWithGeometry(call.arguments as HashMap<String, Any>)

        if (call.argument<String>("parentNodeName") != null) {
            val parentNode: Node? = arSceneView?.scene?.findByName(call.argument<String>("parentNodeName") as String)
            parentNode?.addChild(node)
        } else {
            arSceneView?.scene?.addChild(node)
        }
        result.success(null);
    }


    fun getNodeWithGeometry(map: HashMap<String, Any>): Node {
        val geometryArguments: HashMap<String, Any> = map["geometry"] as HashMap<String, Any>


        //TODO manca geometry
        val node: Node = Node()
        node.renderable = redSphereRenderable
        node.localPosition = parseVector3(map["position"] as HashMap<String, Any>)

        if (map["scale"] != null) {
            node.localScale = parseVector3(map["scale"] as HashMap<String, Any>)
        }

        if (map["rotation"] != null) {
            node.localRotation = parseVector4(map["rotation"] as HashMap<String, Any>)
        }

        if (map["name"] != null) {
            node.name = map["name"] as String
        }

        if (map["physicsBody"] != null) {
            val physics: HashMap<String, Any> = map["physicsBody"] as HashMap<String, Any>
            //TODO
        }

        //       if (dict[@"physicsBody"] != nil) {
//           NSDictionary *physics = dict[@"physicsBody"];
//           node.physicsBody = [self getPhysicsBodyFromDict:physics];
//       }

        val modelRenderable = createModelRenderable(activity, geometryArguments)

        node.renderable = modelRenderable
        return node
    }


    override fun dispose() {
        if (arSceneView != null) {
            arSceneView?.destroy()
        }
    }


}





