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
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreSceneSetup
import com.difrancescogianmarco.arcore_flutter_plugin.models.RotatingNode
import com.difrancescogianmarco.arcore_flutter_plugin.utils.ArCoreUtils
import com.difrancescogianmarco.arcore_flutter_plugin.utils.DecodableUtils
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.AugmentedFaceNode
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

class ArCoreView(val activity: Activity, context: Context, messenger: BinaryMessenger, id: Int, private val isAugmentedFaces: Boolean, private val debug: Boolean) : PlatformView, MethodChannel.MethodCallHandler {
    private val methodChannel: MethodChannel = MethodChannel(messenger, "arcore_flutter_plugin_$id")

    private val TAG: String = ArCoreView::class.java.name
    lateinit var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks
    private val coordinator by lazy { Coordinator(activity, ::onArTap, ::onNodeSelected, ::onNodeFocused, ::onNodeTapped) }
    private var mUserRequestedInstall = true
    private var arSceneView: ArSceneView? = null
    private val gestureDetector: GestureDetector
    private val RC_PERMISSIONS = 0x123
    private var sceneUpdateListener: Scene.OnUpdateListener
    private var faceSceneUpdateListener: Scene.OnUpdateListener
    private var sceneOnPeekTouchListener: Scene.OnPeekTouchListener

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

        sceneOnPeekTouchListener = Scene.OnPeekTouchListener { hitTestResult, motionEvent ->
            coordinator.onTouch(hitTestResult, motionEvent)
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

    fun debugLog(message: String?) {
        if (debug) {
            Log.i(TAG, message ?: "null message")
        }
    }

/*    private fun onArUpdate() {
        val frame = arSceneView?.arFrame
        val camera = frame?.camera
        val state = camera?.trackingState
        val reason = camera?.trackingFailureReason
    }*/


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
                arScenViewInit(FlutterArCoreSceneSetup(call.arguments as Map<String, *>), result)
            }
            "addArCoreNode" -> {
                debugLog(" addArCoreNode")
                val map = call.arguments as HashMap<String, Any>
                val flutterNode = FlutterArCoreNode(map);
                onAddNode(flutterNode, result)
            }
            "addArCoreNodeWithAnchor" -> {
                debugLog(" addArCoreNodeWithAnchor")
                val map = call.arguments as HashMap<String, Any>
                val flutterNode = FlutterArCoreNode(map)
                addNodeWithAnchor(flutterNode, result)
            }
            "removeARCoreNode" -> {
                debugLog(" removeARCoreNode")
                val map = call.arguments as HashMap<String, Any>
                removeNode(map["nodeName"] as String, result)
            }
            "positionChanged" -> {
                debugLog(" positionChanged")
                coordinator.selectedNode?.apply {
                    localPosition = DecodableUtils.parseVector3(call.argument("position") as? HashMap<String, Double>)
                }
                result.success(null)
            }
            "scaleChanged" -> {
                debugLog(" scaleChanged")

                coordinator.selectedNode?.parent?.apply {
                    localScale = DecodableUtils.parseVector3(call.argument("scale") as? HashMap<String, *>)
                }

                result.success(null)
            }
            "rotationChanged" -> {
                debugLog(" rotationChanged")
                coordinator.selectedNode?.apply {
                    localRotation = DecodableUtils.parseQuaternion(call.argument("rotation") as? HashMap<String, Double>)
                }
                result.success(null)
            }
            "updateMaterials" -> {
                debugLog(" updateMaterials")
                updateMaterials(call, result)

            }
            "loadMesh" -> {
                val map = call.arguments as HashMap<String, Any>
                val textureBytes = map["textureBytes"] as ByteArray
                loadMesh(textureBytes)
            }
            "dispose" -> {
                debugLog("Disposing ARCore now")
                dispose()
            }
            "resume" -> {
                debugLog("Resuming ARCore now")
                onResume()
            }
            "getTrackingState" -> {
                debugLog("1/3: Requested tracking state, returning that back to Flutter now")

                val trState = arSceneView?.arFrame?.camera?.trackingState
                debugLog("2/3: Tracking state is " + trState.toString())
                methodChannel.invokeMethod("getTrackingState", trState.toString())
            }
            else -> {
            }
        }
    }

/*    fun maybeEnableArButton() {
        Log.i(TAG,"maybeEnableArButton" )
        try{
            val availability = ArCoreApk.getInstance().checkAvailability(activity.applicationContext)
            if (availability.isTransient) {
                // Re-query at 5Hz while compatibility is checked in the background.
                Handler().postDelayed({ maybeEnableArButton() }, 200)
            }
            if (availability.isSupported) {
                debugLog("AR SUPPORTED")
            } else { // Unsupported or unknown.
                debugLog("AR NOT SUPPORTED")
            }
        }catch (ex:Exception){
            Log.i(TAG,"maybeEnableArButton ${ex.localizedMessage}" )
        }

    }*/

    private fun setupLifeCycle(context: Context) {
        activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
                debugLog("onActivityCreated")
//                maybeEnableArButton()
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
                debugLog("onActivityStopped (Just so you know)")
//                onPause()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                debugLog("onActivityDestroyed (Just so you know)")
//                onDestroy()
//                dispose()
            }
        }

        activity.application.registerActivityLifecycleCallbacks(this.activityLifecycleCallbacks)
    }

    private fun onSingleTap(tap: MotionEvent?) {
        debugLog(" onSingleTap")
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

    private fun arScenViewInit(flutterArCoreSceneSetup: FlutterArCoreSceneSetup, result: MethodChannel.Result) {
        debugLog("arScenViewInit")

        if (flutterArCoreSceneSetup.enableTapRecognizer == true) {
            arSceneView?.scene?.addOnPeekTouchListener(sceneOnPeekTouchListener)
        }

        if (flutterArCoreSceneSetup.enableUpdateListener == true) {
            // Set an update listener on the Scene that will hide the loading message once a Plane is
            // detected.
            arSceneView?.scene?.addOnUpdateListener(sceneUpdateListener)
        }

        if (flutterArCoreSceneSetup.enablePlaneRenderer == false) {
            debugLog(" The plane renderer (enablePlaneRenderer) is set to false")
            arSceneView!!.planeRenderer.isVisible = false
        }

        result.success(null)
    }

    fun addNodeWithAnchor(flutterArCoreNode: FlutterArCoreNode, result: MethodChannel.Result) {
        debugLog("addNodeWithAnchor " + flutterArCoreNode.configuration.name)
        if (arSceneView == null) {
            return
        }

        RenderableCustomFactory.makeRenderable(activity.applicationContext, flutterArCoreNode) { renderable, t ->
            if (t != null) {
                result.error("Make Renderable Error", t.localizedMessage, null)
                return@makeRenderable
            }

            val myAnchor = arSceneView?.session?.createAnchor(Pose(flutterArCoreNode.getPosition(), flutterArCoreNode.getRotation()))
            myAnchor?.let { anchor ->
                NodeFactory.makeTransformableNode(activity.applicationContext, flutterArCoreNode, debug, coordinator) { node, throwable ->
                    node?.let { baseNode ->
                        baseNode.attach(anchor, arSceneView!!.scene, true)
                        for (n in flutterArCoreNode.children) {
                            n.parentNodeName = flutterArCoreNode.configuration.name
                            onAddNode(n, null)
                        }
                    }
                }
            }
            result.success(null)
        }
    }

    fun onAddNode(flutterArCoreNode: FlutterArCoreNode, result: MethodChannel.Result?) {

        debugLog(flutterArCoreNode.toString())
        NodeFactory.makeNode(activity.applicationContext, flutterArCoreNode, debug) { node, throwable ->

            debugLog("onAddNode inserted ${node?.name}")
            node?.let {
                attachNodeToParent(node, flutterArCoreNode.parentNodeName)
                for (n in flutterArCoreNode.children) {
                    n.parentNodeName = flutterArCoreNode.configuration.name
                    onAddNode(n, null)
                }
            }

        }
        result?.success(null)
    }

    fun attachNodeToParent(node: Node?, parentNodeName: String?) {
        if (parentNodeName != null) {
            debugLog(parentNodeName);
            val parentNode: Node? = arSceneView?.scene?.findByName(parentNodeName)
            parentNode?.addChild(node)
        } else {
            debugLog("addNodeToSceneWithGeometry: NOT PARENT_NODE_NAME")
            arSceneView?.scene?.addChild(node)
        }
    }

    fun removeNode(name: String, result: MethodChannel.Result) {
        val node = arSceneView?.scene?.findByName(name)
        if (node != null) {
            arSceneView?.scene?.removeChild(node);
            debugLog("removed ${node.name}")
        }

        result.success(null)
    }

    fun updateRotation(call: MethodCall, result: MethodChannel.Result) {
        val name = call.argument<String>("name")
        val node = arSceneView?.scene?.findByName(name) as RotatingNode
        debugLog("rotating node:  $node")
        val degreesPerSecond = call.argument<Double?>("degreesPerSecond")
        debugLog("rotating value:  $degreesPerSecond")
        if (degreesPerSecond != null) {
            debugLog("rotating value:  ${node.degreesPerSecond}")
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
        debugLog("onResume()")

        if (arSceneView == null) {
            return
        }

        // request camera permission if not already requested
        if (!ArCoreUtils.hasCameraPermission(activity)) {
            ArCoreUtils.requestCameraPermission(activity, RC_PERMISSIONS)
        }

        if (arSceneView?.session == null) {
            debugLog("session is null")
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
            //arSceneView!!.planeRenderer.isVisible = false
            debugLog("Searching for surfaces")
        }
    }

    fun onPause() {
        if (arSceneView != null) {
            arSceneView?.pause()
        }
    }

    fun onDestroy() {
        if (arSceneView != null) {
            debugLog("Goodbye ARCore! Destroying the Activity now 7.")

            try {
                arSceneView?.scene?.removeOnUpdateListener(sceneUpdateListener)
                arSceneView?.scene?.removeOnUpdateListener(faceSceneUpdateListener)
                arSceneView?.scene?.removeOnPeekTouchListener(sceneOnPeekTouchListener)
                debugLog("Goodbye arSceneView.")

                arSceneView?.destroy()
                arSceneView = null

            } catch (e: Exception) {
                e.printStackTrace();
            }
        }
    }

    private fun onArTap(motionEvent: MotionEvent) {
        debugLog("onArTap")
        val frame = arSceneView?.arFrame ?: return
        if (frame.camera.trackingState != TrackingState.TRACKING) {
            coordinator.selectNode(null)
            return
        }

        frame.hitTest(motionEvent).firstOrNull {
            val trackable = it.trackable
            when {
                trackable is Plane && trackable.isPoseInPolygon(it.hitPose) -> true
                trackable is Point -> true
                else -> false
            }
        }?.let { hit ->
            val list = ArrayList<HashMap<String, Any>>()
            val distance: Float = hit.distance
            val translation = hit.hitPose.translation
            val rotation = hit.hitPose.rotationQuaternion
            val flutterArCoreHitTestResult = FlutterArCoreHitTestResult(distance, translation, rotation)
            val arguments = flutterArCoreHitTestResult.toHashMap()
            list.add(arguments)
            methodChannel.invokeMethod("onPlaneTap", list)
        } ?: coordinator.selectNode(null)
    }

    //Called every frame
    private fun onNodeUpdate(node: BaseNode?) {
        //debugLog("onNodeUpdate: " + node?.name)

    }

    private fun onNodeSelected(old: BaseNode? = coordinator.selectedNode, new: BaseNode?) {
        debugLog("onNodeSelected old: " + old?.name)
        debugLog("onNodeSelected new: " + new?.name)
        old?.onNodeUpdate = null
        new?.onNodeUpdate = ::onNodeUpdate
    }

    private fun onNodeFocused(node: BaseNode?) {
        debugLog("onNodeFocused: " + node?.name)
    }

    private fun onNodeTapped(node: Node?) {
        debugLog("onNodeTapped: " + node?.name)
        methodChannel.invokeMethod("onNodeTap", node?.name)
    }
}