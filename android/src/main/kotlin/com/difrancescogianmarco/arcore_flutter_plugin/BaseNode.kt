package com.difrancescogianmarco.arcore_flutter_plugin

import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.ux.TransformableNode

class BaseNode(coordinator: Coordinator) : TransformableNode(coordinator) {

    fun attach(anchor: Anchor, scene: Scene, focus: Boolean = false) {
        setParent(AnchorNode(anchor).apply { setParent(scene) })
        if (focus) {
            (transformationSystem as Coordinator).focusNode(this)
        }
    }
}