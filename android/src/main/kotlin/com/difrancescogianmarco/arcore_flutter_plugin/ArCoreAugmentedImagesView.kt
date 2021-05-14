package com.difrancescogianmarco.arcore_flutter_plugin

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.Pair
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCorePose
import com.difrancescogianmarco.arcore_flutter_plugin.utils.ArCoreUtils
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Scene
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.*
import com.google.ar.core.exceptions.*
import com.google.ar.core.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class ArCoreAugmentedImagesView(activity: Activity, context: Context, messenger: BinaryMessenger, id: Int, val useSingleImage: Boolean, debug: Boolean) : BaseArCoreView(activity, context, messenger, id, debug), CoroutineScope {

    private val TAG: String = ArCoreAugmentedImagesView::class.java.name
    private var sceneUpdateListener: Scene.OnUpdateListener
    // Augmented image and its associated center pose anchor, keyed by index of the augmented image in
    // the
    // database.
    private val augmentedImageMap = HashMap<Int, Pair<AugmentedImage, AnchorNode>>()

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    init {

        sceneUpdateListener = Scene.OnUpdateListener { frameTime ->

            val frame = arSceneView?.arFrame ?: return@OnUpdateListener

            // If there is no frame or ARCore is not tracking yet, just return.
            if (frame.camera.trackingState != TrackingState.TRACKING) {
                return@OnUpdateListener
            }

            val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)

            for (augmentedImage in updatedAugmentedImages) {
                when (augmentedImage.trackingState) {
                    TrackingState.PAUSED -> {
                        val text = String.format("Detected Image %d", augmentedImage.index)
                        debugLog( text)
                    }

                    TrackingState.TRACKING -> {
                        debugLog( "${augmentedImage.name} ${augmentedImage.trackingMethod}")
                        if (!augmentedImageMap.containsKey(augmentedImage.index)) {
                            debugLog( "${augmentedImage.name} ASSENTE")
                            val centerPoseAnchor = augmentedImage.createAnchor(augmentedImage.centerPose)
                            val anchorNode = AnchorNode()
                            anchorNode.anchor = centerPoseAnchor
                            augmentedImageMap[augmentedImage.index] = Pair.create(augmentedImage, anchorNode)
                        }

                        sendAugmentedImageToFlutter(augmentedImage)
                    }

                    TrackingState.STOPPED -> {
                        debugLog( "STOPPED: ${augmentedImage.name}")
                        val anchorNode = augmentedImageMap[augmentedImage.index]!!.second
                        augmentedImageMap.remove(augmentedImage.index)
                        arSceneView?.scene?.removeChild(anchorNode)
                        val text = String.format("Removed Image %d", augmentedImage.index)
                        debugLog( text)
                    }

                    else -> {
                    }
                }
            }
        }
    }

    private fun sendAugmentedImageToFlutter(augmentedImage: AugmentedImage) {
        val map: HashMap<String, Any> = HashMap<String, Any>()
        map["name"] = augmentedImage.name
        map["index"] = augmentedImage.index
        map["extentX"] = augmentedImage.extentX
        map["extentZ"] = augmentedImage.extentZ
        map["centerPose"] = FlutterArCorePose.fromPose(augmentedImage.centerPose).toHashMap()
        map["trackingMethod"] = augmentedImage.trackingMethod.ordinal
        activity.runOnUiThread {
            methodChannel.invokeMethod("onTrackingImage", map)
        }
    }

/*    fun setImage(image: AugmentedImage, anchorNode: AnchorNode) {
        if (!mazeRenderable.isDone) {
            Log.d(TAG, "loading maze renderable still in progress. Wait to render again")
            CompletableFuture.allOf(mazeRenderable)
                    .thenAccept { aVoid: Void -> setImage(image, anchorNode) }
                    .exceptionally { throwable ->
                        Log.e(TAG, "Exception loading", throwable)
                        null
                    }
            return
        }

        // Set the anchor based on the center of the image.
        // anchorNode.anchor = image.createAnchor(image.centerPose)

        val mazeNode = Node()
        mazeNode.setParent(anchorNode)
        mazeNode.renderable = mazeRenderable.getNow(null)

        *//* // Make sure longest edge fits inside the image
         val maze_edge_size = 492.65f
         val max_image_edge = Math.max(image.extentX, image.extentZ)
         val maze_scale = max_image_edge / maze_edge_size
 
         // Scale Y extra 10 times to lower the wall of maze
         mazeNode.localScale = Vector3(maze_scale, maze_scale * 0.1f, maze_scale)*//*
    }*/

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (isSupportedDevice) {
            debugLog( call.method + "called on supported device")
            when (call.method) {
                "init" -> {
                    debugLog( "INIT AUGMENTED IMAGES")
                    arScenViewInit(call, result)
                }
                "load_single_image_on_db" -> {
                    debugLog( "load_single_image_on_db")
                    val map = call.arguments as HashMap<String, Any>
                    val singleImageBytes = map["bytes"] as? ByteArray
                    setupSession(singleImageBytes, true)
                }
                "load_multiple_images_on_db" -> {
                    debugLog( "load_multiple_image_on_db")
                    val map = call.arguments as HashMap<String, Any>
                    val dbByteMap = map["bytesMap"] as? Map<String, ByteArray>
                    setupSession(dbByteMap)
                }
                "load_augmented_images_database" -> {
                    debugLog( "LOAD DB")
                    val map = call.arguments as HashMap<String, Any>
                    val dbByteArray = map["bytes"] as? ByteArray
                    setupSession(dbByteArray, false)
                }
                "attachObjectToAugmentedImage" -> {
                    debugLog( "attachObjectToAugmentedImage")
                    val map = call.arguments as HashMap<String, Any>
                    val flutterArCoreNode = FlutterArCoreNode(map["node"] as HashMap<String, Any>)
                    val index = map["index"] as Int
                    if (augmentedImageMap.containsKey(index)) {
//                        val augmentedImage = augmentedImageMap[index]!!.first
                        val anchorNode = augmentedImageMap[index]!!.second
//                        setImage(augmentedImage, anchorNode)
//                        onAddNode(flutterArCoreNode, result)
                        NodeFactory.makeNode(activity.applicationContext, flutterArCoreNode, debug) { node, throwable ->
                            debugLog( "inserted ${node?.name}")
                            if (node != null) {
                                node.setParent(anchorNode)
                                arSceneView?.scene?.addChild(anchorNode)
                                result.success(null)
                            } else if (throwable != null) {
                                result.error("attachObjectToAugmentedImage error", throwable.localizedMessage, null)
                            }
                        }
                    } else {
                        result.error("attachObjectToAugmentedImage error", "Augmented image there isn't ona hashmap", null)
                    }
                }
                "removeARCoreNodeWithIndex" -> {
                    debugLog( "removeObject")
                    try {
                        val map = call.arguments as HashMap<String, Any>
                        val index = map["index"] as Int
                        removeNode(augmentedImageMap[index]!!.second)
                        augmentedImageMap.remove(index)
                        result.success(null)
                    } catch (ex: Exception) {
                        result.error("removeARCoreNodeWithIndex", ex.localizedMessage, null)
                    }
                }
                "dispose" -> {
                    debugLog( " updateMaterials")
                    job.cancel()
                    dispose()
                }
                else -> {
                    result.notImplemented()
                }
            }
        } else {
            debugLog( "Impossible call " + call.method + " method on unsupported device")
            job.cancel()
            result.error("Unsupported Device", "", null)
        }
    }

    private fun arScenViewInit(call: MethodCall, result: MethodChannel.Result) {
        arSceneView?.scene?.addOnUpdateListener(sceneUpdateListener)
        onResume()
        result.success(null)
    }

    override fun onResume() {
        debugLog( "onResume")
        if (arSceneView == null) {
            debugLog( "arSceneView NULL")
            return
        }
        debugLog( "arSceneView NOT null")

        if (arSceneView?.session == null) {
            debugLog( "session NULL")
            if (!ArCoreUtils.hasCameraPermission(activity)) {
                ArCoreUtils.requestCameraPermission(activity, RC_PERMISSIONS)
                return
            }

            debugLog( "Camera has permission")
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                val session = ArCoreUtils.createArSession(activity, installRequested, false)
                if (session == null) {
                    installRequested = false
                    return
                } else {
                    val config = Config(session)
                    config.focusMode = Config.FocusMode.AUTO
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    session.configure(config)
                    arSceneView?.setupSession(session)
                }
            } catch (e: UnavailableException) {
                ArCoreUtils.handleSessionException(activity, e)
            }
        }

        try {
            arSceneView?.resume()
            debugLog( "arSceneView.resume()")
        } catch (ex: CameraNotAvailableException) {
            ArCoreUtils.displayError(activity, "Unable to get camera", ex)
            debugLog( "CameraNotAvailableException")
            activity.finish()
            return
        }
    }

    fun setupSession(bytes: ByteArray?, useSingleImage: Boolean) {
        debugLog( "setupSession()")
        try {
            val session = arSceneView?.session ?: return
            val config = Config(session)
            config.focusMode = Config.FocusMode.AUTO
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            bytes?.let {
                if (useSingleImage) {
                    if (!addImageToAugmentedImageDatabase(config, bytes)) {
                        throw Exception("Could not setup augmented image database")
                    }
                } else {
                    if (!useExistingAugmentedImageDatabase(config, bytes)) {
                        throw Exception("Could not setup augmented image database")
                    }
                }
            }
            session.configure(config)
            arSceneView?.setupSession(session)
        } catch (ex: Exception) {
            debugLog( ex.localizedMessage)
        }
    }

    fun setupSession(bytesMap: Map<String, ByteArray>?) {
        debugLog( "setupSession()")
        try {
            val session = arSceneView?.session ?: return
            val config = Config(session)
            config.focusMode = Config.FocusMode.AUTO
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            bytesMap?.let {
                addMultipleImagesToAugmentedImageDatabase(config, bytesMap, session)
            }
        } catch (ex: Exception) {
            debugLog( ex.localizedMessage)
        }
    }

    private fun addMultipleImagesToAugmentedImageDatabase(config: Config, bytesMap: Map<String, ByteArray>, session: Session) {
        debugLog( "addImageToAugmentedImageDatabase")
        val augmentedImageDatabase = AugmentedImageDatabase(arSceneView?.session)

        launch {
            val operation = async(Dispatchers.Default) {
                for ((key, value) in bytesMap) {
                    val augmentedImageBitmap = loadAugmentedImageBitmap(value)
                    try {
                        augmentedImageDatabase.addImage(key, augmentedImageBitmap)
                    } catch (ex: Exception) {
                        debugLog("Image with the title $key cannot be added to the database. " +
                        "The exception was thrown: " + ex?.toString())
                    }
                }
                if (augmentedImageDatabase?.getNumImages() == 0) {
                    throw Exception("Could not setup augmented image database")
                }
                config.augmentedImageDatabase = augmentedImageDatabase
                session.configure(config)
                arSceneView?.setupSession(session)
            }
            operation.await()
        }
    }

    private fun addImageToAugmentedImageDatabase(config: Config, bytes: ByteArray): Boolean {
        debugLog( "addImageToAugmentedImageDatabase")
        try {
            val augmentedImageBitmap = loadAugmentedImageBitmap(bytes) ?: return false
            val augmentedImageDatabase = AugmentedImageDatabase(arSceneView?.session)
            augmentedImageDatabase.addImage("image_name", augmentedImageBitmap)
            config.augmentedImageDatabase = augmentedImageDatabase
            return true
        } catch (ex:Exception) {
            debugLog(ex.localizedMessage)
            return false
        }
    }

    private fun useExistingAugmentedImageDatabase(config: Config, bytes: ByteArray): Boolean {
        debugLog( "useExistingAugmentedImageDatabase")
        return try {
            val inputStream = ByteArrayInputStream(bytes)
            val augmentedImageDatabase = AugmentedImageDatabase.deserialize(arSceneView?.session, inputStream)
            config.augmentedImageDatabase = augmentedImageDatabase
            true
        } catch (e: IOException) {
            Log.e(TAG, "IO exception loading augmented image database.", e)
            false
        }
    }

    private fun loadAugmentedImageBitmap(bitmapdata: ByteArray): Bitmap? {
        debugLog( "loadAugmentedImageBitmap")
       try {
           return  BitmapFactory.decodeByteArray(bitmapdata, 0, bitmapdata.size)
        } catch (e: Exception) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e)
            return  null
        }
    }
}