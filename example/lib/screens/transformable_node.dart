import 'dart:typed_data';

import 'package:arcore_flutter_plugin/arcore_flutter_plugin.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:vector_math/vector_math_64.dart' as vector;

class TransformableNodeScreen extends StatefulWidget {
  @override
  _TransformableNodeState createState() => _TransformableNodeState();
}

class _TransformableNodeState extends State<TransformableNodeScreen> {
  ArCoreController arCoreController;
  String selectedNode;
  final nodesMap = <String, ArCoreNode>{};
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Custom Object on plane detected'),
        ),
        body: Stack(
          fit: StackFit.expand,
          children: <Widget>[
            ArCoreView(
              onArCoreViewCreated: _onArCoreViewCreated,
              enableTapRecognizer: true,
              enableUpdateListener: true,
              debug: true,
            ),
            Align(
              alignment: Alignment.bottomCenter,
              child: Container(
                color: Colors.white,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    Text(selectedNode ?? 'Unselected node'),
                    Row(
                      children: <Widget>[
                        Text('Scale'),
                        IconButton(
                            icon: Icon(Icons.remove),
                            onPressed: selectedNode != null
                                ? () {
                                    if (nodesMap.containsKey(selectedNode)) {
                                      nodesMap[selectedNode].scale.value -=
                                          vector.Vector3(1.0, 1.0, 1.0);
                                      setState(() {});
                                    }
                                  }
                                : null),
                        IconButton(
                            icon: Icon(Icons.add),
                            onPressed: selectedNode != null
                                ? () {
                                    if (nodesMap.containsKey(selectedNode)) {
                                      nodesMap[selectedNode].scale.value +=
                                          vector.Vector3(1.0, 1.0, 1.0);
                                      setState(() {});
                                    }
                                  }
                                : null),
                        if (selectedNode != null &&
                            nodesMap.containsKey(selectedNode))
                          Text(nodesMap[selectedNode].scale.value.text),
                      ],
                    ),
                    Row(
                      children: <Widget>[
                        Text('Position'),
                        IconButton(
                            icon: Icon(Icons.remove),
                            onPressed: selectedNode != null
                                ? () {
                                    if (nodesMap.containsKey(selectedNode)) {
                                      nodesMap[selectedNode].position.value -=
                                          vector.Vector3(0.3, 0.0, 0.0);
                                      setState(() {});
                                    }
                                  }
                                : null),
                        IconButton(
                            icon: Icon(Icons.add),
                            onPressed: selectedNode != null
                                ? () {
                                    if (nodesMap.containsKey(selectedNode)) {
                                      nodesMap[selectedNode].position.value +=
                                          vector.Vector3(0.3, 0.0, 0.0);
                                    }
                                  }
                                : null),
                        if (selectedNode != null &&
                            nodesMap.containsKey(selectedNode))
                          Text(nodesMap[selectedNode].position.value.text),
                      ],
                    ),
                    Row(
                      children: <Widget>[
                        Text('Rotation'),
                        IconButton(
                            icon: Icon(Icons.remove),
                            onPressed: selectedNode != null
                                ? () {
                                    if (nodesMap.containsKey(selectedNode)) {
                                      nodesMap[selectedNode].rotation.value -=
                                          vector.Vector4(0.1, 0.1, 0.1, 0.1);
                                    }
                                  }
                                : null),
                        IconButton(
                            icon: Icon(Icons.add),
                            onPressed: selectedNode != null
                                ? () {
                                    if (nodesMap.containsKey(selectedNode)) {
                                      nodesMap[selectedNode].rotation.value +=
                                          vector.Vector4(0.1, 0.1, 0.1, 0.1);
                                    }
                                  }
                                : null),
                        if (selectedNode != null &&
                            nodesMap.containsKey(selectedNode))
                          Text(nodesMap[selectedNode].rotation.value.text),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _onArCoreViewCreated(ArCoreController controller) {
    arCoreController = controller;
    arCoreController.onPlaneTap = _handleOnPlaneTap;
    arCoreController.onNodeTap = (node) {
      print('TransformableNodeScreen: onNodeTap $node');
      setState(() {
        selectedNode = node;
      });
    };
  }

  Future _addSphere(ArCoreHitTestResult hit) async {
    final ByteData textureBytes = await rootBundle.load('assets/earth.jpg');

    final earthMaterial = ArCoreMaterial(
        color: Color.fromARGB(120, 66, 134, 244),
        textureBytes: textureBytes.buffer.asUint8List());

    final earthShape = ArCoreSphere(
      materials: [earthMaterial],
      radius: 0.1,
    );

    final earth = ArCoreNode(
      shape: earthShape,
      scale: vector.Vector3(1.0, 1.0, 1.0),
      position: hit.pose.translation + vector.Vector3(0.0, 1.0, 0.0),
      rotation: hit.pose.rotation,
    );

    nodesMap[earth.name] = earth;

    arCoreController.addArCoreNodeWithAnchor(earth);
  }

  void _handleOnPlaneTap(List<ArCoreHitTestResult> hits) {
    final hit = hits.first;
    _addSphere(hit);
  }

  void onTapHandler(String name) {
    print("Flutter: onNodeTap");
    showDialog<void>(
      context: context,
      builder: (BuildContext context) =>
          AlertDialog(content: Text('onNodeTap on $name')),
    );
  }

  @override
  void dispose() {
    arCoreController.dispose();
    super.dispose();
  }
}
