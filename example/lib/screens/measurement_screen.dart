import 'package:arcore_flutter_plugin/arcore_flutter_plugin.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'dart:math';
import 'package:vector_math/vector_math_64.dart';

class MeasurementScreen extends StatefulWidget {
  @override
  _MeasurementScreenState createState() => _MeasurementScreenState();
}

class _MeasurementScreenState extends State<MeasurementScreen> {
  final double scale = 0.005;
  Vector3 scaleVector;
  final double lineRadius = 0.5;
  bool busy = false;
  ArCoreController arCoreController;
  List<ArCoreNode> nodes = [];
  ArCorePlane plane;
  double measurement = 0.0;

  @override
  void initState() {
    scaleVector = Vector3(scale, scale, scale);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Measurements'),
      ),
      body: Stack(
        children: <Widget>[
          ArCoreView(
            onArCoreViewCreated: _onArCoreViewCreated,
            enableUpdateListener: true,
            enableTapRecognizer: true,
            forceTapOnScreenCenter: true,
          ),
          Container(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
                Text('Tap anywhere to place a node'),
                Text('measurement: ${measurement.toString()}'),
              ],
            ),
          ),
          const Center(
            child: Icon(
              Icons.add,
              color: Color.fromRGBO(255, 255, 255, 1),
              size: 50.0,
            ),
          ),
        ],
      ),
    );
  }

  void _onArCoreViewCreated(ArCoreController controller) {
    arCoreController = controller;
    arCoreController.onPlaneTap = _onPlaneTap;
    arCoreController.onPlaneDetected = (plane) {
      this.plane = plane;
    };
  }

  void _onPlaneTap(List<ArCoreHitTestResult> results) {
    final ArCoreHitTestResult hit = results.first;
    if (nodes.length <= 2) {
      _addSphere(hit);
    }
  }

  Future _addSphere(ArCoreHitTestResult hit) async {
    final material = ArCoreMaterial(
      color: Color.fromRGBO(255, 255, 255, 1),
      roughness: 1.0,
      reflectance: 0.0,
    );

    final shape = ArCoreSphere(
      materials: [material],
      radius: 1,
    );

    final node = ArCoreNode(
        scale: scaleVector,
        shape: shape,
        position: hit.pose.translation,
        rotation: hit.pose.rotation);

    nodes.add(node);
    arCoreController.addArCoreNode(node);

    if (nodes.length == 2) {
      _drawLine();
    }
  }

  void _drawLine() {
    Vector3 firstPosition = nodes.first.position.value;
    Vector3 lastPosition = nodes.last.position.value;

    final material = ArCoreMaterial(
      color: Color.fromRGBO(255, 255, 255, 1),
      roughness: 1.0,
      reflectance: 0.0,
    );

    final measurement =
        _calculateDistanceBetweenPoints(firstPosition, lastPosition);

    final shape = ArCoreCylinder(
      materials: [material],
      radius: lineRadius,
      height: measurement / scale,
    );

    final middlePoint = _getMiddleVector(firstPosition, lastPosition);
    final rotationVector = _getRotationVector(firstPosition, lastPosition);

    final lineNode = ArCoreNode(
      scale: scaleVector,
      shape: shape,
      position: middlePoint,
      rotation: rotationVector,
    );

    arCoreController.addArCoreNode(lineNode);

    setState(() {
      this.measurement = measurement;
    });
  }

  Vector3 _getMiddleVector(Vector3 A, Vector3 B) {
    return Vector3((A.x + B.x) / 2.0, (A.y + B.y) / 2.0, (A.z + B.z) / 2.0);
  }

  Vector4 _getRotationVector(Vector3 firstPosition, Vector3 lastPosition) {
    final Vector3 directionA = Vector3(0, 1, 0).normalized();
    final Vector3 directionB =
        subtract(firstPosition, lastPosition).normalized();

    final double theta = acos(directionA.dot(directionB));
    final Vector3 rotationAxis = directionA.cross(directionB).normalized();
    Quaternion quaternion = Quaternion.axisAngle(rotationAxis, theta);
    return Vector4(quaternion.x, quaternion.y, quaternion.z, quaternion.w);
  }

  Vector3 subtract(Vector3 A, Vector3 B) {
    return Vector3(A.x - B.x, A.y - B.y, A.z - B.z);
  }

  double _calculateDistanceBetweenPoints(Vector3 A, Vector3 B) {
    // distance is in mm
    return A.distanceTo(B);
  }

  @override
  void dispose() {
    arCoreController.dispose();
    super.dispose();
  }
}
