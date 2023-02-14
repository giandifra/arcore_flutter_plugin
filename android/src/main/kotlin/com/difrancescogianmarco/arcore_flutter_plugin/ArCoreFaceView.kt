package com.difrancescogianmarco.arcore_flutter_plugin

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.difrancescogianmarco.arcore_flutter_plugin.utils.ArCoreUtils
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.AugmentedFaceNode
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.util.function.Function
import com.google.ar.sceneform.ux.ArFragment

class ArCoreFaceView(
    activity: Activity,
    context: Context,
    messenger: BinaryMessenger,
    id: Int,
    debug: Boolean
) : BaseArCoreView(activity, context, messenger, id, debug) {

    private val TAG: String = ArCoreFaceView::class.java.name
    private var faceRegionsRenderable: ModelRenderable? = null
    private var faceMeshTexture: Texture? = null
    private val facesNodes: HashMap<AugmentedFace, AugmentedFaceNode> = HashMap()
    private var faceSceneUpdateListener: Scene.OnUpdateListener =
        Scene.OnUpdateListener {
            run {
                if (faceRegionsRenderable == null || faceMeshTexture == null) {
                    return@OnUpdateListener
                }

                val augmentedFaces = arSceneView?.updatedAugmentedFaces;
                augmentedFaces?.let {

                    for (augmentedFace in it) {
                        onAugmentedFaceTrackingUpdate(augmentedFace)
                    }
                }

                //val faceList = arSceneView?.session?.getAllTrackables(AugmentedFace::class.java)
                /*
                faceList?.let {
                    // Make new AugmentedFaceNodes for any new faces.
                    for (face in faceList) {
                        if (!facesNodes.containsKey(face)) {
                            val faceNode = AugmentedFaceNode(face)
                            faceNode.parent = arSceneView?.scene
                            faceNode.faceRegionsRenderable = faceRegionsRenderable
                            faceNode.faceMeshTexture = faceMeshTexture
                            facesNodes[face] = faceNode

                            // change assets on runtime
                        } else if (facesNodes[face]?.faceRegionsRenderable != faceRegionsRenderable || facesNodes[face]?.faceMeshTexture != faceMeshTexture) {
                            facesNodes[face]?.faceRegionsRenderable = faceRegionsRenderable
                            facesNodes[face]?.faceMeshTexture = faceMeshTexture
                        }
                    }

                    // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
                    val iter = facesNodes.iterator()
                    while (iter.hasNext()) {
                        val entry = iter.next()
                        val face = entry.key
                        if (face.trackingState == TrackingState.STOPPED) {
                            val faceNode = entry.value
                            faceNode.parent = null
                            iter.remove()
                        }
                    }
                }*/
            }
        }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (isSupportedDevice) {
            debugLog(call.method + "called on supported device")
            when (call.method) {
                "init" -> {
                    arSceneViewInit(call, result)
                }
                "loadMesh" -> {
                    val map = call.arguments as HashMap<*, *>
                    val textureBytes = map["textureBytes"] as ByteArray
                    val skin3DModelFilename = map["skin3DModelFilename"] as? String
                    loadMesh(textureBytes, skin3DModelFilename)
                }
                "dispose" -> {
                    debugLog(" updateMaterials")
                    dispose()
                }
                else -> {
                    result.notImplemented()
                }
            }
        } else {
            debugLog("Impossible call " + call.method + " method on unsupported device")
            result.error("Unsupported Device", "", null)
        }
    }

    private fun loadMesh(textureBytes: ByteArray?, skin3DModelFilename: String?) {
        Log.d(TAG, "skin model path: $skin3DModelFilename");
        if (skin3DModelFilename != null) {
            // Load the face regions renderable.
            // This is a skinned model that renders 3D objects mapped to the regions of the augmented face.
            ModelRenderable.builder()
                .setSource(activity, Uri.parse(skin3DModelFilename))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept { modelRenderable ->
                    faceRegionsRenderable = modelRenderable
                    modelRenderable.isShadowCaster = false
                    modelRenderable.isShadowReceiver = false
                }.exceptionally { throwable: Throwable? ->
                    Log.e(TAG, throwable.toString())
                    Toast.makeText(activity, "Unable to load renderable", Toast.LENGTH_LONG).show()
                    null
                }
        }

        // Load the face mesh texture.
        Texture.builder()
            //.setSource(activity, Uri.parse("fox_face_mesh_texture.png"))
            .setSource(BitmapFactory.decodeByteArray(textureBytes, 0, textureBytes!!.size))
            .setUsage(Texture.Usage.COLOR_MAP)
            .build()
            .thenAccept { texture -> faceMeshTexture = texture }
            .exceptionally { throwable ->
                Log.e(TAG, throwable.toString())
                Toast.makeText(activity, "Unable to load texture", Toast.LENGTH_LONG).show()
                null
            }
    }

    private fun arSceneViewInit(call: MethodCall, result: MethodChannel.Result) {
        val enableAugmentedFaces: Boolean? = call.argument("enableAugmentedFaces")
        if (enableAugmentedFaces != null && enableAugmentedFaces) {
            // This is important to make sure that the camera stream renders first so that
            // the face mesh occlusion works correctly.
            arSceneView?.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST)
            arSceneView?.scene?.addOnUpdateListener(faceSceneUpdateListener)
        }

        result.success(null)
    }

    private fun onAugmentedFaceTrackingUpdate(augmentedFace: AugmentedFace) {
        if (faceRegionsRenderable == null || faceMeshTexture == null) {
            return
        }
        val existingFaceNode: AugmentedFaceNode? = facesNodes[augmentedFace]
        when (augmentedFace.trackingState) {
            TrackingState.TRACKING -> if (existingFaceNode == null) {
                val faceNode = AugmentedFaceNode(augmentedFace)
                val modelInstance = faceNode.setFaceRegionsRenderable(faceRegionsRenderable)
                modelInstance.isShadowCaster = false
                modelInstance.isShadowReceiver = true
                faceNode.faceMeshTexture = faceMeshTexture
                arSceneView.scene.addChild(faceNode)
                facesNodes[augmentedFace] = faceNode
            }
            TrackingState.STOPPED -> {
                if (existingFaceNode != null) {
                    arSceneView.scene.removeChild(existingFaceNode)
                }
                facesNodes.remove(augmentedFace)
            }
            else -> {
                Log.d(TAG, augmentedFace.trackingState.toString())
            }
        }
    }

    override fun onResume() {
        if (arSceneView == null) {
            return
        }

        if (arSceneView?.session == null) {

            // request camera permission if not already requested
            if (!ArCoreUtils.hasCameraPermission(activity)) {
                ArCoreUtils.requestCameraPermission(activity, RC_PERMISSIONS)
            }

            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                val session = ArCoreUtils.createArSession(activity, installRequested, true)
                if (session == null) {
                    installRequested = false
                    return
                } else {
                    val config = Config(session)
                    config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    session.configure(config)
                    config.focusMode = Config.FocusMode.AUTO
                    //ArCoreUtils.updateLightEstimationModeFromView(session, config, arSceneView)
                    arSceneView?.session = session
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

    }

    override fun onDestroy() {
        arSceneView?.scene?.removeOnUpdateListener(faceSceneUpdateListener)
        super.onDestroy()
    }
}