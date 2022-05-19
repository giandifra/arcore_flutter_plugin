import 'dart:math';

import 'package:arcore_flutter_plugin/arcore_flutter_plugin.dart';
import 'package:flutter/material.dart';
import 'package:vector_math/vector_math_64.dart' as vector;

class Matrix3DRenderingPage extends StatefulWidget {
  @override
  _Matrix3DRenderingPageState createState() => _Matrix3DRenderingPageState();
}

class _Matrix3DRenderingPageState extends State<Matrix3DRenderingPage> {
  ArCoreController? arCoreController;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Hello World'),
        ),
        body: ArCoreView(
          onArCoreViewCreated: _onArCoreViewCreated,
          enableTapRecognizer: true,
        ),
      ),
    );
  }

  void _onArCoreViewCreated(ArCoreController controller) {
    arCoreController = controller;
    arCoreController?.onNodeTap = (name) => onTapHandler(name);
    arCoreController?.onPlaneTap = _handleOnPlaneTap;
  }

  @override
  void dispose() {
    arCoreController?.dispose();
    super.dispose();
  }

  void _handleOnPlaneTap(List<ArCoreHitTestResult> hits) {
    final hit = hits.first;
    _addMatrix3D(hit);
  }

  void onTapHandler(String name) {
    print("Flutter: onNodeTap");
    showDialog<void>(
      context: context,
      builder: (BuildContext context) =>
          AlertDialog(content: Text('onNodeTap on $name')),
    );
  }

  void _addMatrix3D(ArCoreHitTestResult hit) {
    final List<ArCoreNode> list = [];
    for (int i = 0; i < 8; i++) {
      for (int z = 0; z < 8; z++) {
        list.add(createNode(createCube(), i, z));
      }
    }

    final node = ArCoreNode(
      shape: null,
      position: hit.pose.translation + vector.Vector3(0.0, 0.5, 0.0),
      rotation: hit.pose.rotation,
      children: list,
    );

    arCoreController?.addArCoreNodeWithAnchor(node);
  }

  createNode(ArCoreCube shape, int i, int z) {
    final cubeNode = ArCoreNode(
      shape: shape,
      position: vector.Vector3(0.1 * z, 0.0, -0.1 * i),
    );

    return cubeNode;
  }

  createCube() {
    final material = ArCoreMaterial(
      color: Color.fromARGB(255, Random().nextInt(255), Random().nextInt(255),
          Random().nextInt(255)),
      metallic: 1.0,
    );
    final cube = ArCoreCube(
      materials: [material],
      size: vector.Vector3(0.1, 0.1, 0.1),
    );

    return cube;
  }
}
