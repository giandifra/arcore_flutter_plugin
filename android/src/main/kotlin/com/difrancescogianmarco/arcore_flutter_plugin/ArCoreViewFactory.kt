package com.difrancescogianmarco.arcore_flutter_plugin

import android.app.Activity
import android.content.Context
import android.util.Log
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory


class ArCoreViewFactory(val activity: Activity, val messenger: BinaryMessenger) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context, id: Int, args: Any?): PlatformView {
        val params = args as HashMap<*, *>
        val debug = params["debug"] as Boolean
        val type = params["type"] as String

        if (debug) {
            Log.i("ArCoreViewFactory", id.toString())
            Log.i("ArCoreViewFactory", args.toString())
        }

        if (type == "faces") {
            return ArCoreFaceView(activity, context, messenger, id, debug)
        } else if (type == "augmented") {
            val useSingleImage = params["useSingleImage"] as? Boolean ?: true
            return ArCoreAugmentedImagesView(activity, context, messenger, id, useSingleImage, debug)
        }
        return ArCoreView(activity, context, messenger, id, type == "faces", debug)
    }
}