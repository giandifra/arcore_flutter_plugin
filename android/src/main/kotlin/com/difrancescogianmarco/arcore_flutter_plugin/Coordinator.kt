package com.difrancescogianmarco.arcore_flutter_plugin

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.ux.BaseTransformableNode
import com.google.ar.sceneform.ux.SelectionVisualizer
import com.google.ar.sceneform.ux.TransformationSystem

class Coordinator(
    context: Context,
    private val onArTap: (MotionEvent) -> Unit,
    private val onNodeSelected: (old: BaseNode?, new: BaseNode?) -> Unit,
    private val onNodeFocused: (nodes: BaseNode?) -> Unit,
    private val onNodeTapped: (nodes: Node?) -> Unit
) : TransformationSystem(
    context.resources.displayMetrics, SelectionNodeVisualizer(context)
) {

    override fun getSelectedNode(): BaseNode? = super.getSelectedNode() as? BaseNode

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
            onArTap(motionEvent)
            return true
        }
    })

    override fun getSelectionVisualizer(): SelectionNodeVisualizer {
        return super.getSelectionVisualizer() as SelectionNodeVisualizer
    }

    override fun setSelectionVisualizer(selectionVisualizer: SelectionVisualizer?) {
        // Prevent changing the selection visualizer
    }

    override fun onTouch(hitTestResult: HitTestResult?, motionEvent: MotionEvent?) {
        super.onTouch(hitTestResult, motionEvent)
        hitTestResult?.let{
            if (it.node == null) {
                gestureDetector.onTouchEvent(motionEvent)
            }else{
                onNodeTapped(it.node)
            }
        }
        
    }

    override fun selectNode(node: BaseTransformableNode?): Boolean {
        val old = selectedNode
        when (node) {
            selectedNode -> return true /*ignored*/
            is BaseNode -> {
                return super.selectNode(node).also { selected ->
                    if (!selected) return@also
                    onNodeSelected(old, node)
                    /*transfer current focus*/
                    if (old != null && old == focusedNode) focusNode(node)
                }
            }
            null -> {
                return super.selectNode(null).also {
                    focusNode(null)
                    onNodeSelected(old, null)
                }
            }
        }
        return false
    }

    var focusedNode: BaseNode? = null
        private set

    fun focusNode(node: BaseNode?) {
        if (node == focusedNode) return /*ignored*/
        focusedNode = node
        if (node != null && node != selectedNode) selectNode(node)
        onNodeFocused(node)
    }

}
