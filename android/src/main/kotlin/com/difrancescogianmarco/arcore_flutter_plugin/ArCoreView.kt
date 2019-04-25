package com.difrancescogianmarco.arcore_flutter_plugin

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.difrancescogianmarco.arcore_flutter_plugin.DecodableUtils.Companion.parseVector3
import com.difrancescogianmarco.arcore_flutter_plugin.DecodableUtils.Companion.parseVector4
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import io.flutter.app.FlutterApplication
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.util.concurrent.CompletableFuture

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
                onAddNode(call, result)

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

    fun tryPlaceObjectWithCustomTexture(tap: MotionEvent?, frame: Frame) {
        if (tap != null && frame.camera.trackingState == TrackingState.TRACKING) {
            for (hit in frame.hitTest(tap)) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    // Create the Anchor.
                    val anchor = hit.createAnchor()
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(arSceneView?.scene)

                    var earthSphereRenderable: ModelRenderable

                    val builder = com.google.ar.sceneform.rendering.Texture.builder();
                    builder.setSource(activity.applicationContext, Uri.parse("earth.png"))
                    builder.build().thenAccept { texture ->
                        MaterialFactory.makeOpaqueWithTexture(activity.applicationContext, texture).thenAccept { material ->
                            earthSphereRenderable =
                                    ShapeFactory.makeSphere(0.1f, Vector3(0.0f, 0.0f, 0.0f), material)
                            Toast.makeText(activity.applicationContext, "All done", Toast.LENGTH_SHORT).show();
                            val node = Node()
                            node.renderable = earthSphereRenderable
                            anchorNode.addChild(node)
                        }
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

    fun onAddNode(call: MethodCall, result: MethodChannel.Result) {
        val map = call.arguments as HashMap<String, Any>
        val geometryArguments: HashMap<String, Any> = map["geometry"] as HashMap<String, Any>

        val node = Node()
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

        val materials = geometryArguments["materials"] as ArrayList<HashMap<String, *>>

        val textureName = materials[0][MaterialCustomFactory.MATERIAL_TEXTURE] as? String
        val color = materials[0][MaterialCustomFactory.MATERIAL_COLOR] as? ArrayList<Int>
        if (textureName != null) {
            Log.i(TAG, "textureName: $textureName")
            val builder = com.google.ar.sceneform.rendering.Texture.builder();
            builder.setSource(activity.applicationContext, Uri.parse(textureName))
            builder.build().thenAccept { texture ->
                MaterialCustomFactory.makeWithTexture(activity.applicationContext, texture)?.thenAccept { material ->

                    node.renderable = getBasicModelRenderable(material, geometryArguments)
                    if (call.argument<String>("parentNodeName") != null) {
                        Log.i(TAG, call.argument<String>("parentNodeName"));
                        val parentNode: Node? = arSceneView?.scene?.findByName(call.argument<String>("parentNodeName") as String)
                        parentNode?.addChild(node)
                    } else {
                        Log.i(TAG, "addNodeToSceneWithGeometry: NOT PARENT_NODE_NAME")
                        arSceneView?.scene?.addChild(node)
                    }
                }?.exceptionally { throwable ->
                    Log.e(TAG, "Unable to load Renderable.", throwable);
                    return@exceptionally null
                }

            }

        } else if (color != null) {
            MaterialCustomFactory.makeWithColor(activity.applicationContext, materials[0])
                    ?.thenAccept { material: Material ->
                        Log.i(TAG, "makeOpaqueWithColor then Accept")

                        node.renderable = getBasicModelRenderable(material, geometryArguments)
                        if (call.argument<String>("parentNodeName") != null) {
                            Log.i(TAG, call.argument<String>("parentNodeName"));
                            val parentNode: Node? = arSceneView?.scene?.findByName(call.argument<String>("parentNodeName") as String)
                            parentNode?.addChild(node)
                        } else {
                            Log.i(TAG, "addNodeToSceneWithGeometry: NOT PARENT_NODE_NAME")
                            arSceneView?.scene?.addChild(node)
                        }
                    }?.exceptionally { throwable ->
                        Log.e(TAG, "Unable to load Renderable.", throwable);
                        return@exceptionally null
                    }

        }


        Log.i(TAG, "addNodeToSceneWithGeometry: COMPLETE")
        result.success(null)
    }


    private fun getBasicModelRenderable(material: Material, map: HashMap<String, Any>): ModelRenderable? {
        val type = map["dartType"] as String
        if (type == "ArCoreSphere") {
            val radius: Float = (map["radius"] as Double).toFloat()
            return ShapeFactory.makeSphere(radius, Vector3(0.0f, 0.15f, 0.0f), material);
        } else if (type == "ArCoreCube") {
            val sizeMap = map["size"] as HashMap<String, Any>
            val size = parseVector3(sizeMap)
            return ShapeFactory.makeCube(size, Vector3(0.0f, 0.15f, 0.0f), material);
        } else if (type == "ArCoreCylinder") {
            val radius: Float = (map["radius"] as Double).toFloat()
            val height: Float = (map["height"] as Double).toFloat()
            return ShapeFactory.makeCylinder(radius, height, Vector3(0.0f, 0.15f, 0.0f), material);
        } else {
            //TODO return exception
            return null

        }
    }

    fun customTexture() {
        var earthSphereRenderable: ModelRenderable
        val node = Node()

        val builder = com.google.ar.sceneform.rendering.Texture.builder();
        builder.setSource(activity.applicationContext, Uri.parse("earth.png"))
        builder.build().thenAccept { texture ->
            MaterialFactory.makeOpaqueWithTexture(activity.applicationContext, texture).thenAccept { material ->
                earthSphereRenderable =
                        ShapeFactory.makeSphere(0.1f, Vector3(0.0f, 0.0f, 0.0f), material)
                Toast.makeText(activity.applicationContext, "All done", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*fun addNodeToSceneWithGeometry(call: MethodCall, result: MethodChannel.Result) {
        Log.i(TAG,"addNodeToSceneWithGeometry")
        val node = getNodeWithGeometry(call.arguments as HashMap<String, Any>)
        Log.i(TAG,"getNodeWithGeometry complete")
        if (call.argument<String>("parentNodeName") != null) {
            Log.i(TAG,call.argument<String>("parentNodeName"));
            val parentNode: Node? = arSceneView?.scene?.findByName(call.argument<String>("parentNodeName") as String)
            parentNode?.addChild(node)
        } else {
            Log.i(TAG, "addNodeToSceneWithGeometry: NOT PARENt_NODE_NAME")
            arSceneView?.scene?.addChild(node)
        }
        Log.i(TAG, "addNodeToSceneWithGeometry: COMPLETE")
        result.success(null)
    }


    fun getNodeWithGeometry(map: HashMap<String, Any>): Node {
        Log.i(TAG, "getNodeWithGeometry")
        val geometryArguments: HashMap<String, Any> = map["geometry"] as HashMap<String, Any>

        //TODO manca geometry
        val node = Node()
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

//        val modelRenderable = createModelRenderable(activity, geometryArguments)
        Log.i(TAG, "createModelRenderable COMPLETE")
//        val materials = geometryArguments["materials"] as ArrayList<HashMap<String, Any>>
//        val rgb = materials[0]["color"] as ArrayList<Int>
//        val color = com.google.ar.sceneform.rendering.Color(Color.argb(255,rgb[0],rgb[1],rgb[2]))
//        redSphereRenderable.material.setFloat3(MaterialFactory.MATERIAL_COLOR, color)
        node.renderable = redSphereRenderable
        return node
    }*/

    fun updatePosition(call: MethodCall, result: MethodChannel.Result) {
        val name = call.argument<String>("name")
        val node = arSceneView?.scene?.findByName(name)
        node?.localPosition = parseVector3(call.arguments as HashMap<String, Any>)
        result.success(null)
    }

    fun updateRotation(call: MethodCall, result: MethodChannel.Result) {
        val name = call.argument<String>("name")
        val node = arSceneView?.scene?.findByName(name)
        node?.localRotation = parseVector4(call.arguments as HashMap<String, Any>)
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





