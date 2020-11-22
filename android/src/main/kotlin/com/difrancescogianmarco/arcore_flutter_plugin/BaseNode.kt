package com.difrancescogianmarco.arcore_flutter_plugin

import android.view.MotionEvent
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNodeConfiguration
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.TransformableNode

class BaseNode(coordinator: Coordinator, config: FlutterArCoreNodeConfiguration) : TransformableNode(coordinator) {

    init {

        name = config.name

        localScale = config.currentScale
        scaleController.apply {
            isEnabled = config.scaleEnabled
            minScale = config.minScale
            maxScale = config.maxScale
        }

        localPosition = config.currentPosition
        translationController.apply {
            isEnabled = config.translationEnabled

        }

        localRotation = config.currentRotation
        rotationController.apply { 
            isEnabled = config.rotationEnabled
        }
    }

    override fun getTransformationSystem(): Coordinator = super.getTransformationSystem() as Coordinator


    fun attach(anchor: Anchor, scene: Scene, focus: Boolean = false) {
        setParent(AnchorNode(anchor).apply { setParent(scene) })
        if (focus) {
            transformationSystem.focusNode(this)
        }
    }

    override fun setRenderable(renderable: Renderable?) {
        super.setRenderable(renderable?.apply {})
    }

    override fun onTap(hitTestResult: HitTestResult?, motionEvent: MotionEvent?) {
        super.onTap(hitTestResult, motionEvent)
        if (isTransforming) return
        transformationSystem.focusNode(this)
    }
}