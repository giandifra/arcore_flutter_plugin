import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:vector_math/vector_math_64.dart' as vector;
import 'package:arcore_flutter_plugin/arcore_flutter_plugin.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  ArCoreController arCoreController;

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
          actions: <Widget>[
            IconButton(icon:Icon(Icons.add),
              onPressed: () {
                _addSphere(arCoreController);
              },)
          ],
        ),

        body: ArCoreView(
          onArCoreViewCreated: _onArCoreViewCreated,
//          enableTapRecognizer: true,
        ),
      ),
    );
  }

  void _onArCoreViewCreated(ArCoreController controller) {
    arCoreController = controller;
    arCoreController.onTap = (name)=>onTapHandler(name);


  }

  void _addSphere(ArCoreController controller) {
    final material = ArCoreMaterial(
      color: Colors.yellow
    );
    final sphere = ArCoreSphere(
      materials: [material],
      radius: 0.1,
    );
    final node = ArCoreNode(
      geometry: sphere,
      position: vector.Vector3(0, 0, -0.5),
    );
    controller.add(node);
  }

  void onTapHandler(String name) {
   print("Flutter: onTap");
  }
}
