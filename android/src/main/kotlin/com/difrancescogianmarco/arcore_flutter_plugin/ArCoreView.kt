package com.difrancescogianmarco.arcore_flutter_plugin

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreHitTestResult
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCorePose
import com.difrancescogianmarco.arcore_flutter_plugin.models.RotatingNode
import com.difrancescogianmarco.arcore_flutter_plugin.utils.ArCoreUtils
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.ar.sceneform.*
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.AugmentedFaceNode
import io.flutter.app.FlutterApplication
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

class ArCoreView(context: Context, messenger: BinaryMessenger, id: Int, private val isAugmentedFaces: Boolean) : PlatformView, MethodChannel.MethodCallHandler {
    private val methodChannel: MethodChannel = MethodChannel(messenger, "arcore_flutter_plugin_$id")
    private val activity: Activity = (context.applicationContext as FlutterApplication).currentActivity
    lateinit var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks
    private var installRequested: Boolean = false
    private var mUserRequestedInstall = true
    private val TAG: String = ArCoreView::class.java.name
    private var arSceneView: ArSceneView? = null
    private val gestureDetector: GestureDetector
    private val RC_PERMISSIONS = 0x123
    private var sceneUpdateListener: Scene.OnUpdateListener
    private var faceSceneUpdateListener: Scene.OnUpdateListener
    
    //AUGMENTEDFACE
    private var faceRegionsRenderable: ModelRenderable? = null
    private var faceMeshTexture: Texture? = null
    private val faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()

    init {
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

        sceneUpdateListener = Scene.OnUpdateListener { frameTime ->

            val frame = arSceneView?.arFrame ?: return@OnUpdateListener

            if (frame.camera.trackingState != TrackingState.TRACKING) {
                return@OnUpdateListener
            }

            for (plane in frame.getUpdatedTrackables(Plane::class.java)) {
                if (plane.trackingState == TrackingState.TRACKING) {

                    val pose = plane.centerPose
                    val map: HashMap<String, Any> = HashMap<String, Any>()
                    map["type"] = plane.type.ordinal
                    map["centerPose"] = FlutterArCorePose(pose.translation, pose.rotationQuaternion).toHashMap()
                    map["extentX"] = plane.extentX
                    map["extentZ"] = plane.extentZ

                    methodChannel.invokeMethod("onPlaneDetected", map)
                }
            }
        }

        faceSceneUpdateListener = Scene.OnUpdateListener { frameTime ->
            run {
                //                if (faceRegionsRenderable == null || faceMeshTexture == null) {
                if (faceMeshTexture == null) {
                    return@OnUpdateListener
                }

                val faceList = arSceneView?.session?.getAllTrackables(AugmentedFace::class.java)

                faceList?.let {
                    // Make new AugmentedFaceNodes for any new faces.
                    for (face in faceList) {
                        if (!faceNodeMap.containsKey(face)) {
                            val faceNode = AugmentedFaceNode(face)
                            faceNode.setParent(arSceneView?.scene)
                            faceNode.faceRegionsRenderable = faceRegionsRenderable
                            faceNode.faceMeshTexture = faceMeshTexture
                            faceNodeMap[face] = faceNode
                        }
                    }

                    // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
                    val iter = faceNodeMap.iterator()
                    while (iter.hasNext()) {
                        val entry = iter.next()
                        val face = entry.key
                        if (face.trackingState == TrackingState.STOPPED) {
                            val faceNode = entry.value
                            faceNode.setParent(null)
                            iter.remove()
                        }
                    }
                }
            }
        }

        // Lastly request CAMERA permission which is required by ARCore.
        ArCoreUtils.requestCameraPermission(activity, RC_PERMISSIONS)
        setupLifeCycle(context)
    }

    fun loadMesh(textureBytes: ByteArray?) {
        // Load the face regions renderable.
        // This is a skinned model that renders 3D objects mapped to the regions of the augmented face.
        /*ModelRenderable.builder()
                .setSource(activity, Uri.parse("fox_face.sfb"))
                .build()
                .thenAccept { modelRenderable ->
                    faceRegionsRenderable = modelRenderable;
                    modelRenderable.isShadowCaster = false;
                    modelRenderable.isShadowReceiver = false;
                }*/

        // Load the face mesh texture.
        //                .setSource(activity, Uri.parse("fox_face_mesh_texture.png"))
        Texture.builder()
                .setSource(BitmapFactory.decodeByteArray(textureBytes, 0, textureBytes!!.size))
                .build()
                .thenAccept { texture -> faceMeshTexture = texture }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "init" -> {
                arScenViewInit(call, result, activity)
            }
            "addArCoreNode" -> {
                val map = call.arguments as HashMap<String, Any>
                val flutterNode = FlutterArCoreNode(map);
                onAddNode(flutterNode, result)
            }
            "addArCoreNodeWithAnchor" -> {
                val map = call.arguments as HashMap<String, Any>
                val flutterNode = FlutterArCoreNode(map);
                addNodeWithAnchor(flutterNode, result)
            }
            "removeARCoreNode" -> {
                val map = call.arguments as HashMap<String, Any>
                removeNode(map["nodeName"] as String, result)
            }
            "positionChanged" -> {

            }
            "rotationChanged" -> {
                updateRotation(call, result)

            }
            "updateMaterials" -> {
                updateMaterials(call, result)

            }
            "loadMesh" -> {
                val map = call.arguments as HashMap<String, Any>
                val textureBytes = map["textureBytes"] as ByteArray
                loadMesh(textureBytes)
            }
            "dispose" -> {
                dispose()
            }
            else -> {
            }
        }
    }

/*    fun maybeEnableArButton() {
        try{
            val availability = ArCoreApk.getInstance().checkAvailability(activity.applicationContext)
            if (availability.isTransient) {
                // Re-query at 5Hz while compatibility is checked in the background.
                Handler().postDelayed({ maybeEnableArButton() }, 200)
            }
            if (availability.isSupported) {
            } else { // Unsupported or unknown.
            }
        }catch (ex:Exception){
        }

    }*/

    private fun setupLifeCycle(context: Context) {
        activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle) {
//                maybeEnableArButton()
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
                onResume()
            }

            override fun onActivityPaused(activity: Activity) {
                onPause()
            }

            override fun onActivityStopped(activity: Activity) {
                onPause()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                onDestroy()
            }
        }

        (context.applicationContext as FlutterApplication).currentActivity.application
                .registerActivityLifecycleCallbacks(this.activityLifecycleCallbacks)
    }

    private fun onSingleTap(tap: MotionEvent?) {
        val frame = arSceneView?.arFrame
        if (frame != null) {
            if (tap != null && frame.camera.trackingState == TrackingState.TRACKING) {
                val hitList = frame.hitTest(tap)
                val list = ArrayList<HashMap<String, Any>>()
                for (hit in hitList) {
                    val trackable = hit.trackable
                    if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                        hit.hitPose
                        val distance: Float = hit.distance
                        val translation = hit.hitPose.translation
                        val rotation = hit.hitPose.rotationQuaternion
                        val flutterArCoreHitTestResult = FlutterArCoreHitTestResult(distance, translation, rotation)
                        val arguments = flutterArCoreHitTestResult.toHashMap()
                        list.add(arguments)
                    }
                }
                methodChannel.invokeMethod("onPlaneTap", list)
            }
        }
    }

    private fun arScenViewInit(call: MethodCall, result: MethodChannel.Result, context: Context) {
        val enableTapRecognizer: Boolean? = call.argument("enableTapRecognizer")
        if (enableTapRecognizer != null && enableTapRecognizer) {
            arSceneView
                    ?.scene
                    ?.setOnTouchListener { hitTestResult: HitTestResult, event: MotionEvent? ->

                        if (hitTestResult.node != null) {
                                                                methodChannel.invokeMethod("onNodeTap", hitTestResult.node?.name)
                            return@setOnTouchListener true
                        }
                        return@setOnTouchListener gestureDetector.onTouchEvent(event)
                    }
        }
        val enableUpdateListener: Boolean? = call.argument("enableUpdateListener")
        if (enableUpdateListener != null && enableUpdateListener) {
            // Set an update listener on the Scene that will hide the loading message once a Plane is
            // detected.
            arSceneView?.scene?.addOnUpdateListener(sceneUpdateListener)
        }
        result.success(null)
    }

    fun addNodeWithAnchor(flutterArCoreNode: FlutterArCoreNode, result: MethodChannel.Result) {

        if (arSceneView == null) {
            return
        }

        RenderableCustomFactory.makeRenderable(activity.applicationContext, flutterArCoreNode) { renderable, t ->
            if (renderable != null) {
                val myAnchor = arSceneView?.session?.createAnchor(Pose(flutterArCoreNode.getPosition(), flutterArCoreNode.getRotation()))
                if (myAnchor != null) {
                    val anchorNode = AnchorNode(myAnchor)
                    anchorNode.name = flutterArCoreNode.name
                    anchorNode.renderable = renderable

                        attachNodeToParent(anchorNode, flutterArCoreNode.parentNodeName)

                    for (node in flutterArCoreNode.children) {
                        node.parentNodeName = flutterArCoreNode.name
                        onAddNode(node, null)
                    }
                }
            }
        }
        result.success(null)
    }

    fun onAddNode(flutterArCoreNode: FlutterArCoreNode, result: MethodChannel.Result?) {

        NodeFactory.makeNode(activity.applicationContext, flutterArCoreNode) { node, throwable ->


/*            if (flutterArCoreNode.parentNodeName != null) {
                 val parentNode: Node? = arSceneView?.scene?.findByName(flutterArCoreNode.parentNodeName)
                parentNode?.addChild(node)
            } else {
                arSceneView?.scene?.addChild(node)
            }*/
            if (node != null) {
                attachNodeToParent(node, flutterArCoreNode.parentNodeName)
                for (n in flutterArCoreNode.children) {
                    n.parentNodeName = flutterArCoreNode.name
                    onAddNode(n, null)
                }
            }

        }
        result?.success(null)
    }

    fun attachNodeToParent(node: Node?, parentNodeName: String?) {
        if (parentNodeName != null) {
            val parentNode: Node? = arSceneView?.scene?.findByName(parentNodeName)
            parentNode?.addChild(node)
        } else {
            arSceneView?.scene?.addChild(node)
        }
    }

    fun removeNode(name: String, result: MethodChannel.Result) {
        val node = arSceneView?.scene?.findByName(name)
        if (node != null) {
            arSceneView?.scene?.removeChild(node);
        }

        result.success(null)
    }

    fun updateRotation(call: MethodCall, result: MethodChannel.Result) {
        val name = call.argument<String>("name")
        val node = arSceneView?.scene?.findByName(name) as RotatingNode
        val degreesPerSecond = call.argument<Double?>("degreesPerSecond")
        if (degreesPerSecond != null) {
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
            onPause()
            onDestroy()
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
            try {
                val session = ArCoreUtils.createArSession(activity, mUserRequestedInstall, isAugmentedFaces)
                if (session == null) {
                    // Ensures next invocation of requestInstall() will either return
                    // INSTALLED or throw an exception.
                    mUserRequestedInstall = false
                    return
                } else {
                    val config = Config(session)
                    if (isAugmentedFaces) {
                        config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
                    }
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    config.focusMode = Config.FocusMode.AUTO;
                    session.configure(config)
                    arSceneView?.setupSession(session)
                }
            } catch (ex: UnavailableUserDeclinedInstallationException) {
                // Display an appropriate message to the user zand return gracefully.
                Toast.makeText(activity, "TODO: handle exception " + ex.localizedMessage, Toast.LENGTH_LONG)
                        .show();
                return
            } catch (e: UnavailableException) {
                ArCoreUtils.handleSessionException(activity, e)
                return
            }
        }

        try {
            arSceneView?.resume()
        } catch (ex: CameraNotAvailableException) {
            ArCoreUtils.displayError(activity, "Unable to get camera", ex)
            activity.finish()
            return
        }

        if (arSceneView?.session != null) {
        }
    }

    fun onPause() {
        if (arSceneView != null) {
            arSceneView?.pause()
        }
    }

    fun onDestroy() {
        if (arSceneView != null) {
            arSceneView?.scene?.removeOnUpdateListener(sceneUpdateListener)
            arSceneView?.scene?.removeOnUpdateListener(faceSceneUpdateListener)
            arSceneView?.destroy()
            arSceneView = null
        }
    }

    /* private fun tryPlaceNode(tap: MotionEvent?, frame: Frame) {
        if (tap != null && frame.camera.trackingState == TrackingState.TRACKING) {
            for (hit in frame.hitTest(tap)) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    // Create the Anchor.
                    val anchor = hit.createAnchor()
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(arSceneView?.scene)

                    ModelRenderable.builder()
                            .setSource(activity.applicationContext, Uri.parse("TocoToucan.sfb"))
                            .build()
                            .thenAccept { renderable ->
                                val node = Node()
                                node.renderable = renderable
                                anchorNode.addChild(node)
                            }.exceptionally { throwable ->
                                return@exceptionally null
                            }
                }
            }
        }

    }*/

    /*    fun updatePosition(call: MethodCall, result: MethodChannel.Result) {
        val name = call.argument<String>("name")
        val node = arSceneView?.scene?.findByName(name)
        node?.localPosition = parseVector3(call.arguments as HashMap<String, Any>)
        result.success(null)
    }*/
}