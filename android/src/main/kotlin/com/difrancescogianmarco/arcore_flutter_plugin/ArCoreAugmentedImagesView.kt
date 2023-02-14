package com.difrancescogianmarco.arcore_flutter_plugin

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.util.Log
import android.util.Pair
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCorePose
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.ArFragment
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.coroutines.CoroutineContext


class ArCoreAugmentedImagesView(
    activity: Activity,
    context: Context,
    messenger: BinaryMessenger,
    id: Int,
    val useSingleImage: Boolean,
    debug: Boolean
) : BaseArCoreView(activity, context, messenger, id, debug), CoroutineScope, OnSessionConfigurationListener {

    private val TAG: String = ArCoreAugmentedImagesView::class.java.name
    private var onAugmentedImageTrackingUpdate: Scene.OnUpdateListener

    // Augmented image and its associated center pose anchor, keyed by index of the augmented image in
    // the
    // database.
    private val augmentedImageMap = HashMap<Int, Pair<AugmentedImage, AnchorNode>>()

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    init {
        Log.i(TAG, "init")
        onAugmentedImageTrackingUpdate = Scene.OnUpdateListener { frameTime ->

            if (arSceneView.session == null) return@OnUpdateListener
            val frame = arSceneView.arFrame ?: return@OnUpdateListener

            // If there is no frame or ARCore is not tracking yet, just return.
            if (frame.camera.trackingState != TrackingState.TRACKING) {
                debugLog("no camera tracking state")
                return@OnUpdateListener
            }

            val updatedAugmentedImages = arSceneView.updatedAugmentedImages

            for (augmentedImage in updatedAugmentedImages) {
                when (augmentedImage.trackingState) {
                    TrackingState.PAUSED -> {
                        val text = String.format("Detected Image %d", augmentedImage.index)
                        debugLog(text)
                    }

                    TrackingState.TRACKING -> {
                        debugLog("${augmentedImage.name} ${augmentedImage.trackingMethod}")
                        if (!augmentedImageMap.containsKey(augmentedImage.index)) {

                            // Setting anchor to the center of Augmented Image
                            // Setting anchor to the center of Augmented Image
                            val anchorNode =
                                AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))
                            anchorNode.worldScale =
                                Vector3(
                                    augmentedImage.extentX,
                                    1.0f,
                                    augmentedImage.extentZ,
                                )
                            val player = MediaPlayer()
                            NodeFactory.createVideoNode(
                                activity,
                                arSceneView,
                                anchorNode,
                                player,
                                getTransformationSystem(),
                                augmentedImage
                            )
                            mediaPlayers.add(player)
                            //val anchorNode = AnchorNode()
                            //anchorNode.anchor = centerPoseAnchor
                            augmentedImageMap[augmentedImage.index] =
                                Pair.create(augmentedImage, anchorNode)
                        }
                        sendAugmentedImageToFlutter(augmentedImage)
                    }

                    TrackingState.STOPPED -> {
                        debugLog("STOPPED: ${augmentedImage.name}")
                        val anchorNode = augmentedImageMap[augmentedImage.index]?.second
                        anchorNode?.let {
                            augmentedImageMap.remove(augmentedImage.index)
                            arSceneView.scene?.removeChild(anchorNode)
                            val text = String.format("Removed Image %d", augmentedImage.index)
                            debugLog(text)
                        }
                    }

                    else -> {
                        debugLog(augmentedImage.trackingState.toString())
                    }
                }
            }
        }
        onSessionConfigurationListener = this
        Log.i(TAG, "init complete")
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (isSupportedDevice) {
            debugLog(call.method + "called on supported device")
            when (call.method) {
                "init" -> {
                    arSceneViewInit(call, result)
                }
                "load_single_image_on_db" -> {
                    debugLog("load_single_image_on_db")
                    val map = call.arguments as HashMap<String, Any>
                    val singleImageBytes = map["bytes"] as? ByteArray
                    setupSession(singleImageBytes, true)
                }
                "load_multiple_images_on_db" -> {
                    debugLog("load_multiple_image_on_db")
                    val map = call.arguments as HashMap<String, Any>
                    val dbByteMap = map["bytesMap"] as? Map<String, ByteArray>
                    setupSession(dbByteMap)
                }
                "load_augmented_images_database" -> {
                    debugLog("LOAD DB")
                    val map = call.arguments as HashMap<String, Any>
                    val dbByteArray = map["bytes"] as? ByteArray
                    setupSession(dbByteArray, false)
                }
                "attachObjectToAugmentedImage" -> {
                    debugLog("attachObjectToAugmentedImage")
                    val map = call.arguments as HashMap<String, Any>
                    val flutterArCoreNode = FlutterArCoreNode(map["node"] as HashMap<String, Any>)
                    val index = map["index"] as Int
                    if (augmentedImageMap.containsKey(index)) {
//                        val augmentedImage = augmentedImageMap[index]!!.first
                        val anchorNode = augmentedImageMap[index]!!.second
//                        setImage(augmentedImage, anchorNode)
//                        onAddNode(flutterArCoreNode, result)
                        NodeFactory.makeNode(
                            activity.applicationContext,
                            flutterArCoreNode,
                            debug
                        ) { node, throwable ->
                            debugLog("inserted ${node?.name}")
                            if (node != null) {
                                node.parent = anchorNode
                                arSceneView?.scene?.addChild(anchorNode)
                                result.success(null)
                            } else if (throwable != null) {
                                result.error(
                                    "attachObjectToAugmentedImage error",
                                    throwable.localizedMessage,
                                    null
                                )
                            }
                        }
                    } else {
                        result.error(
                            "attachObjectToAugmentedImage error",
                            "Augmented image there isn't ona hashmap",
                            null
                        )
                    }
                }
                "removeARCoreNodeWithIndex" -> {
                    debugLog("removeObject")
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
                    debugLog(" updateMaterials")
                    job.cancel()
                    dispose()
                }
                else -> {
                    result.notImplemented()
                }
            }
        } else {
            debugLog("Impossible call " + call.method + " method on unsupported device")
            job.cancel()
            result.error("Unsupported Device", "", null)
        }
    }

    private fun arSceneViewInit(call: MethodCall, result: MethodChannel.Result) {
        debugLog("arSceneViewInit")
        initializeSession()
        result.success(null)
        debugLog("arSceneViewInit complete")
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

    private fun setupSession(bytes: ByteArray?, useSingleImage: Boolean) {
        debugLog("setupSession()")
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
            arSceneView?.session = session
        } catch (ex: Exception) {
            debugLog(ex.localizedMessage)
        }
    }

    private fun setupSession(bytesMap: Map<String, ByteArray>?) {
        debugLog("setupSession()")
        try {
            val session = arSceneView.session ?: return
            val config = onCreateSessionConfig(session)
            bytesMap?.let {
                addMultipleImagesToAugmentedImageDatabase(config, bytesMap, session)
            }
        } catch (ex: Exception) {
            debugLog(ex.toString())
        }
    }

    private fun addMultipleImagesToAugmentedImageDatabase(
        config: Config,
        bytesMap: Map<String, ByteArray>,
        session: Session
    ) {
        debugLog("addImageToAugmentedImageDatabase")
        val augmentedImageDatabase = AugmentedImageDatabase(session)

        launch {
            val operation = async(Dispatchers.Default) {
                for ((key, value) in bytesMap) {
                    val augmentedImageBitmap = loadAugmentedImageBitmap(value)
                    try {
                        augmentedImageDatabase.addImage(key, augmentedImageBitmap)
                    } catch (ex: Exception) {
                        debugLog(
                            "Image with the title $key cannot be added to the database. " +
                                    "The exception was thrown: " + ex.toString()
                        )
                    }
                }
                if (augmentedImageDatabase.numImages == 0) {
                    throw Exception("Could not setup augmented image database")
                }

                config.augmentedImageDatabase = augmentedImageDatabase
                session.configure(config)

                //arSceneView.destroySession()
                //arSceneView.session = session
            }
            operation.await()
        }
    }

    private fun addImageToAugmentedImageDatabase(config: Config, bytes: ByteArray): Boolean {
        debugLog("addImageToAugmentedImageDatabase")
        try {
            val augmentedImageBitmap = loadAugmentedImageBitmap(bytes) ?: return false
            val augmentedImageDatabase = AugmentedImageDatabase(arSceneView?.session)
            augmentedImageDatabase.addImage("image_name", augmentedImageBitmap)
            config.augmentedImageDatabase = augmentedImageDatabase
            return true
        } catch (ex: Exception) {
            debugLog(ex.localizedMessage)
            return false
        }
    }

    private fun useExistingAugmentedImageDatabase(config: Config, bytes: ByteArray): Boolean {
        debugLog("useExistingAugmentedImageDatabase")
        return try {
            val inputStream = ByteArrayInputStream(bytes)
            val augmentedImageDatabase =
                AugmentedImageDatabase.deserialize(arSceneView.session, inputStream)
            config.augmentedImageDatabase = augmentedImageDatabase
            true
        } catch (e: IOException) {
            Log.e(TAG, "IO exception loading augmented image database.", e)
            false
        }
    }

    private fun loadAugmentedImageBitmap(bitmapdata: ByteArray): Bitmap? {
        debugLog("loadAugmentedImageBitmap")
        try {
            return BitmapFactory.decodeByteArray(bitmapdata, 0, bitmapdata.size)
        } catch (e: Exception) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e)
            return null
        }
    }

    private fun getBitmapFromAsset(context: Context, filePath: String?): Bitmap? {
        val assetManager = context.assets
        val istr: InputStream
        var bitmap: Bitmap? = null
        try {
            istr = assetManager.open(filePath!!)
            bitmap = BitmapFactory.decodeStream(istr)
        } catch (e: IOException) {
            // handle exception
        }
        return bitmap
    }

    override fun onSessionConfiguration(session: Session, config: Config) {
        // Disable plane detection
        // Disable plane detection
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED

        // Images to be detected by our AR need to be added in AugmentedImageDatabase
        // This is how database is created at runtime
        // You can also prebuild database in you computer and load it directly (see: https://developers.google.com/ar/develop/java/augmented-images/guide#database)


        // Images to be detected by our AR need to be added in AugmentedImageDatabase
        // This is how database is created at runtime
        // You can also prebuild database in you computer and load it directly (see: https://developers.google.com/ar/develop/java/augmented-images/guide#database)
        /*database = AugmentedImageDatabase(session)

        val matrixImage = BitmapFactory.decodeResource(getResources(), R.drawable.matrix)
        val rabbitImage = BitmapFactory.decodeResource(getResources(), R.drawable.rabbit)
        // Every image has to have its own unique String identifier
        // Every image has to have its own unique String identifier
        database!!.addImage("matrix", matrixImage)
        database!!.addImage("rabbit", rabbitImage)

        config!!.setAugmentedImageDatabase(database)*/

        arSceneView.scene?.addOnUpdateListener(onAugmentedImageTrackingUpdate)
    }
}