package com.difrancescogianmarco.arcore_flutter_plugin

//package com.difrancescogianmarco.arcore_flutter_plugin
//
//import android.app.Activity
//import android.app.ActivityManager
//import android.app.Application
//import android.content.ContentValues.TAG
//import android.content.Context
//import android.opengl.GLES20
//import android.opengl.GLSurfaceView
//import android.os.Build
//import android.os.Bundle
//import android.util.Log
//import android.view.View
//import android.widget.Toast
//import com.google.ar.core.ArCoreApk
//import com.google.ar.core.Config
//import io.flutter.app.FlutterApplication
//import io.flutter.plugin.common.BinaryMessenger
//import io.flutter.plugin.common.MethodCall
//import io.flutter.plugin.common.MethodChannel
//import io.flutter.plugin.platform.PlatformView
//import javax.microedition.khronos.egl.EGLConfig
//import javax.microedition.khronos.opengles.GL10
//import com.google.ar.core.Session
//import com.google.ar.core.exceptions.CameraNotAvailableException
//import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
//import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
//import java.io.IOException
//
//class ArCoreView(context: Context, messenger: BinaryMessenger, id: Int) : PlatformView, MethodChannel.MethodCallHandler, GLSurfaceView.Renderer {
//
//
//    val surfaceView: GLSurfaceView
//    val methodChannel: MethodChannel
//    val activity: Activity
//    lateinit var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks
//    private var session: Session? = null
//    private var shouldConfigureSession = false
//    var installRequested: Boolean = false
//
//    init {
//        surfaceView = GLSurfaceView(context)
//        this.activity = (context.applicationContext as FlutterApplication).currentActivity
//        methodChannel = MethodChannel(messenger, "arcore_flutter_plugin_$id")
//        methodChannel.setMethodCallHandler(this)
////        displayRotationHelper = DisplayRotationHelper/*context=*/context
//
//        setupSurface()
//        setupLifeCycle(context)
//
//    }
//
//    private fun setupLifeCycle(context: Context) {
//        activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
//            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle) {}
//
//            override fun onActivityStarted(activity: Activity) {}
//
//            override fun onActivityResumed(activity: Activity) {
////                activity_paused = false
//                _onResume()
//            }
//
//            override fun onActivityPaused(activity: Activity) {
////                activity_paused = true
////                _onPause()
//            }
//
//            override fun onActivityStopped(activity: Activity) {
////                _onPause()
//            }
//
//            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
//
//            override fun onActivityDestroyed(activity: Activity) {}
//        }
//
//        (context.getApplicationContext() as FlutterApplication).currentActivity.application
//                .registerActivityLifecycleCallbacks(this.activityLifecycleCallbacks)
//    }
//
//    private fun setupSurface() {
//        surfaceView.preserveEGLContextOnPause = true
//        surfaceView.setEGLContextClientVersion(2)
//        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.
//        surfaceView.setRenderer(this)
//        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
//    }
//
//    protected fun _onResume() {
//
//        if (session == null) {
//            var exception: Exception? = null
//            var message: String? = null
//            try {
//
//                installRequested = ArCoreUtils.checkIfArCoreIsInstalled(activity, !installRequested)
//
//                // request camera permission if not already requested
//                if (!ArCoreUtils.hasCameraPermission(activity)) {
//                    ArCoreUtils.requestCameraPermission(activity)
//                }
//
//                // create new Session
//                this.session = Session(activity)
//                Log.i(TAG, "Session created ")
//
//            } catch (e: UnavailableUserDeclinedInstallationException) {
//                message = "Please install ARCore"
//                exception = e
//            } catch (e: UnavailableDeviceNotCompatibleException) {
//                message = "This device does not support AR"
//                exception = e
//            } catch (e: Exception) {
//                message = "Failed to create AR session"
//                exception = e
//            }
//
//            if (message != null) {
//
//                Log.e(TAG, "message is $message")
//
//                return
//            }
//            shouldConfigureSession = true
//
//
//        }
//
//        if (shouldConfigureSession) {
//            configureSession()
//            shouldConfigureSession = false
//        }
//        // Note that order matters - see the note in onPause(), the reverse applies here.
//        try {
//            session?.resume()
//        } catch (e: CameraNotAvailableException) {
//            // In some cases (such as another camera app launching) the camera may be given to
//            // a different app instead. Handle this properly by showing a message and recreate the
//            // session at the next iteration.
//            session = null
//            return
//        }
//
//        surfaceView.onResume()
////        displayRotationHelper.onResume()
//
//    }
//
//    private fun configureSession() {
//
//        val config = Config(session)
//        config.focusMode = Config.FocusMode.AUTO
//
////        if (!setupAugmentedImageDatabase(config)) {
////            Log.e(TAG, "Could not setup augmented image database")
////
////        }
//        session?.configure(config)
//    }
//
//
//    override fun onMethodCall(p0: MethodCall?, p1: MethodChannel.Result?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun getView(): View {
//        return surfaceView
//    }
//
//    override fun dispose() {
//
//    }
//
//    override fun onDrawFrame(gl: GL10?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
////        displayRotationHelper.onSurfaceChanged(width, height)
//        GLES20.glViewport(0, 0, width, height)
//    }
//
//    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
//
//        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
//        try {
//            // Create the texture and pass it to ARCore session to be filled during update().
//            backgroundRenderer.createOnGlThread(/*context=*/activity)
//        } catch (e: IOException) {
//            Log.e(TAG, "Failed to read an asset file", e)
//        }
//
//
//    }
//
//}





