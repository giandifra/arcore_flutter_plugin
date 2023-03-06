package com.difrancescogianmarco.arcore_flutter_plugin

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.util.Pair
import android.widget.Toast
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCorePose
import com.google.android.filament.Engine
import com.google.android.filament.filamat.MaterialBuilder
import com.google.android.filament.filamat.MaterialPackage
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.TransformableNode
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext


class ArCoreAugmentedImagesView(
    activity: Activity,
    context: Context,
    messenger: BinaryMessenger,
    id: Int,
    debug: Boolean
) : BaseArCoreView(activity, context, messenger, id, debug), CoroutineScope,
    OnSessionConfigurationListener {

    private val TAG: String = ArCoreAugmentedImagesView::class.java.name
    private var onAugmentedImageTrackingUpdate: Scene.OnUpdateListener

    // Augmented image and its associated center pose anchor, keyed by index of the augmented image in
    // the
    // database.
    private val augmentedImageMap = HashMap<Int, Pair<AugmentedImage, AnchorNode>>()
    private var plainVideoModel: Renderable? = null
    private var plainVideoMaterial: Material? = null
    private val futures: MutableList<CompletableFuture<Void>> = ArrayList()

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
                        debugLog("TRACKING: ${augmentedImage.name} ${augmentedImage.trackingMethod}")
                        if (!augmentedImageMap.containsKey(augmentedImage.index)) {
                            debugLog("${augmentedImage.name} is not attached")
                            val anchorNode =
                                AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))
                            augmentedImageMap[augmentedImage.index] =
                                Pair.create(augmentedImage, anchorNode)
                        }
                        /*
                        if (!augmentedImageMap.containsKey(augmentedImage.index)) {

                            // Setting anchor to the center of Augmented Image
                            val anchorNode =
                                AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))
                            anchorNode.worldScale =
                                Vector3(
                                    augmentedImage.extentX,
                                    1.0f,
                                    augmentedImage.extentZ,
                                )
                            addVideoNode()
                            augmentedImageMap[augmentedImage.index] =
                                Pair.create(augmentedImage, anchorNode)
                        }*/
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
                "load_images_on_db" -> {
                    debugLog("load_multiple_image_on_db")
                    val map = call.arguments as HashMap<String, Any>
                    val dbByteMap = map["bytesMap"] as? Map<String, ByteArray>
                    setupExistingImageDatabase(dbByteMap)
                }
                "load_augmented_images_database" -> {
                    debugLog("LOAD DB")
                    val map = call.arguments as HashMap<String, Any>
                    val dbByteArray = map["bytes"] as? ByteArray
                    setupExistingImageDatabase(dbByteArray)
                }
                "attachObjectToAugmentedImage" -> {
                    debugLog("attachObjectToAugmentedImage")
                    val map = call.arguments as HashMap<String, Any>
                    val flutterArCoreNode = FlutterArCoreNode(map["node"] as HashMap<String, Any>)
                    val index = map["index"] as Int
                    if (augmentedImageMap.containsKey(index)) {
                        val augmentedImage = augmentedImageMap[index]!!.first
                        val augmentedAnchorNode = augmentedImageMap[index]!!.second

                        if (flutterArCoreNode.isVideoNode()) {
                            debugLog(flutterArCoreNode.position.toString())
                            debugLog(flutterArCoreNode.rotation.toString())
                            debugLog(flutterArCoreNode.scale.toString())
                            debugLog(augmentedImage.extentX.toString())
                            debugLog(augmentedImage.extentZ.toString())
                            //addVideoNode(augmentedAnchorNode, flutterArCoreNode.video?.bytes)
                            try {
                                val player = MediaPlayer()
                                attachVideo(
                                    augmentedImage,
                                    player,
                                    flutterArCoreNode.video?.bytes!!,
                                    flutterArCoreNode.rotation
                                )
                                /*NodeFactory.createVideoNode(
                                    activity,
                                    arSceneView,
                                    augmentedAnchorNode,
                                    player,
                                    getTransformationSystem(),
                                    flutterArCoreNode.video?.bytes,
                                    augmentedImage.extentX,
                                    augmentedImage.extentZ,
                                    flutterArCoreNode.position,
                                    flutterArCoreNode.rotation,
                                    flutterArCoreNode.scale,
                                )*/
                                mediaPlayers.add(player)
                            } catch (ex: Exception) {
                                debugLog(ex.toString())
                            }
                            result.success(null)
                        } else {
                            NodeFactory.makeNode(
                                activity.applicationContext,
                                flutterArCoreNode,
                                debug
                            ) { node, throwable ->
                                debugLog("inserted ${node?.name}")
                                if (node != null) {
                                    node.parent = augmentedAnchorNode
                                    arSceneView.scene?.addChild(augmentedAnchorNode)
                                    result.success(null)
                                } else if (throwable != null) {
                                    result.error(
                                        "attachObjectToAugmentedImage error",
                                        throwable.localizedMessage,
                                        null
                                    )
                                }
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
        loadMatrixModel()
        loadMatrixMaterial()
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

    private fun setupExistingImageDatabase(bytes: ByteArray?) {
        debugLog("setupExistingImageDatabase()")
        try {
            val session = arSceneView.session ?: return
            val config = Config(session)
            config.focusMode = Config.FocusMode.AUTO
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            bytes?.let {
                if (!useExistingAugmentedImageDatabase(config, bytes)) {
                    throw Exception("Could not setup augmented image database")
                }
            }
            session.configure(config)
            arSceneView.session = session
        } catch (ex: Exception) {
            debugLog(ex.toString())
        }
    }

    private fun setupExistingImageDatabase(bytesMap: Map<String, ByteArray>?) {
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
                    if (augmentedImageBitmap != null) {
                        try {
                            augmentedImageDatabase.addImage(key, augmentedImageBitmap)
                        } catch (ex: Exception) {
                            debugLog(
                                "Image with the title $key cannot be added to the database. " +
                                        "The exception was thrown: " + ex.toString()
                            )
                        }
                    } else {
                        debugLog(
                            "augmentedImageBitmap is null"
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
            debugLog(e.toString())
        }
        return bitmap
    }

    override fun onSessionConfiguration(session: Session, config: Config) {
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        arSceneView.scene?.addOnUpdateListener(onAugmentedImageTrackingUpdate)
    }

    // Function to establish connection and load image
    private fun mLoad(string: String): Bitmap? {
        val url: URL = mStringToURL(string)!!
        val connection: HttpURLConnection?
        try {
            connection = url.openConnection() as HttpURLConnection
            connection.connect()
            val inputStream: InputStream = connection.inputStream
            val bufferedInputStream = BufferedInputStream(inputStream)
            return BitmapFactory.decodeStream(bufferedInputStream)
        } catch (e: IOException) {
            e.printStackTrace()
            debugLog(e.toString())
            Toast.makeText(activity, "Error", Toast.LENGTH_LONG).show()
        }
        return null
    }

    // Function to convert string to URL
    private fun mStringToURL(string: String): URL? {
        try {
            return URL(string)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            debugLog(e.toString())
        }
        return null
    }

    private var matrixDetected = false
    private var rabbitDetected = false

    private fun attachVideo(
        augmentedImage: AugmentedImage,
        mediaPlayer: MediaPlayer,
        byteArray: ByteArray,
        rotation: Quaternion
    ) {
        // Setting anchor to the center of Augmented Image
        val anchorNode = AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))

        // AnchorNode placed to the detected tag and set it to the real size of the tag
        // This will cause deformation if your AR tag has different aspect ratio than your video
        anchorNode.worldScale =
            Vector3(augmentedImage.extentX, 1f, augmentedImage.extentZ)
        arSceneView.scene.addChild(anchorNode)
        val videoNode = TransformableNode(getTransformationSystem())
        // For some reason it is shown upside down so this will rotate it correctly
        //videoNode.localRotation = rotation
        videoNode.localRotation = Quaternion.axisAngle(Vector3(-1f, 0f, 0f), 180f)
        anchorNode.addChild(videoNode)

        // Setting texture
        val externalTexture = ExternalTexture()
        val renderableInstance = videoNode.setRenderable(plainVideoModel)
        renderableInstance.material = plainVideoMaterial

        // Setting MediaPLayer
        renderableInstance.material.setExternalTexture("videoTexture", externalTexture)

        // From bytes
        val filename = "video_" + anchorNode.name
        Log.i(NodeFactory.TAG, "temp file: $filename")
        val temp = File.createTempFile(filename, "mp4", activity.cacheDir)
        temp.deleteOnExit()
        val fos = FileOutputStream(temp)
        fos.write(byteArray)
        fos.close()

        val file = FileInputStream(temp)
        mediaPlayer.setDataSource(file.fd)
        mediaPlayer.isLooping = true
        mediaPlayer.setSurface(externalTexture.surface)
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    private fun loadMatrixModel() {
        futures.add(
            ModelRenderable.builder()
                .setSource(activity, Uri.parse("Video.glb"))
                /*.setSource(activity) {
                    val filename = "video_" + "anchorNode.name"
                    Log.i(NodeFactory.TAG, "temp file: $filename")
                    val temp = File.createTempFile(filename, "mp4", activity.cacheDir)
                    temp.deleteOnExit()
                    val fos = FileOutputStream(temp)
                    fos.write(byteArray)
                    fos.close()
                    val file = FileInputStream(temp)
                    file
                }*/
                .setIsFilamentGltf(true)
                .build()
                .thenAccept { model: ModelRenderable ->
                    //removing shadows for this Renderable
                    debugLog("loadMatrixModel complete")
                    model.isShadowCaster = false
                    model.isShadowReceiver = true
                    plainVideoModel = model
                }
                .exceptionally { throwable: Throwable? ->
                    null
                })
    }

    private fun loadMatrixMaterial() {
        val filamentEngine: Engine = EngineInstance.getEngine().filamentEngine
        MaterialBuilder.init()
        val materialBuilder: MaterialBuilder = MaterialBuilder()
            .platform(MaterialBuilder.Platform.MOBILE)
            .name("External Video Material")
            .require(MaterialBuilder.VertexAttribute.UV0)
            .shading(MaterialBuilder.Shading.UNLIT)
            .doubleSided(true)
            .samplerParameter(
                MaterialBuilder.SamplerType.SAMPLER_EXTERNAL,
                MaterialBuilder.SamplerFormat.FLOAT,
                MaterialBuilder.ParameterPrecision.DEFAULT,
                "videoTexture"
            )
            .optimization(MaterialBuilder.Optimization.NONE)
        val plainVideoMaterialPackage: MaterialPackage = materialBuilder
            .blending(MaterialBuilder.BlendingMode.OPAQUE)
            .material(
                """void material(inout MaterialInputs material) {
    prepareMaterial(material);
    material.baseColor = texture(materialParams_videoTexture, getUV0()).rgba;
}
"""
            )
            .build(filamentEngine)
        if (plainVideoMaterialPackage.isValid) {
            val buffer: ByteBuffer = plainVideoMaterialPackage.buffer
            futures.add(
                Material.builder()
                    .setSource(buffer)
                    .build()
                    .thenAccept { material: Material ->
                        debugLog("loadMatrixMaterial complete")
                        plainVideoMaterial = material
                    }
                    .exceptionally { throwable: Throwable? ->
                        debugLog(throwable.toString())
                        null
                    }
            )
        }
        MaterialBuilder.shutdown()
    }

}