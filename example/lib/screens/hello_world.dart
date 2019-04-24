import 'package:arcore_flutter_plugin/arcore_flutter_plugin.dart';
import 'package:flutter/material.dart';
import 'package:vector_math/vector_math_64.dart' as vector;

class HelloWorld extends StatefulWidget {
  @override
  _HelloWorldState createState() => _HelloWorldState();
}

class _HelloWorldState extends State<HelloWorld> {
  ArCoreController arCoreController;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Hello World'),
          actions: <Widget>[
            IconButton(
              icon: Icon(Icons.add),
              onPressed: () {},
            )
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
    arCoreController.onTap = (name) => onTapHandler(name);

    _addSphere(arCoreController);
    _addCylindre(arCoreController);
    _addCube(arCoreController);
  }

  void _addSphere(ArCoreController controller) {
    final material = ArCoreMaterial(color: Colors.yellow);
    final sphere = ArCoreSphere(
      materials: [material],
      radius: 0.1,
    );
    final node = ArCoreNode(
      geometry: sphere,
      position: vector.Vector3(0, 0, -1.5),
    );
    controller.add(node);
  }

  void _addCylindre(ArCoreController controller) {
    final material = ArCoreMaterial(color: Colors.red);
    final sphere = ArCoreCylinder(
      materials: [material],
      radius: 0.5,
      height: 0.3,
    );
    final node = ArCoreNode(
      geometry: sphere,
      position: vector.Vector3(0.0, -0.5, -2.0),
    );
    controller.add(node);
  }

  void _addCube(ArCoreController controller) {
    final material = ArCoreMaterial(
      color: Colors.green,
      materialFactory: MaterialFactory.TRANSPARENT_WITH_COLOR,
    );
    final sphere = ArCoreCube(
      materials: [material],
      size: vector.Vector3(0.5, 0.5, 0.5),
    );
    final node = ArCoreNode(
      geometry: sphere,
      position: vector.Vector3(-0.5, 0.5, -3.5),
    );
    controller.add(node);
  }

  void onTapHandler(String name) {
    print("Flutter: onTap");
  }

  @override
  void dispose() {
    arCoreController.dispose();
    super.dispose();
  }
}
