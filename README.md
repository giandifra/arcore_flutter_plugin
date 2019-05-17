# arcore_flutter_plugin

<a href="https://github.com/Solido/awesome-flutter">
   <img alt="Awesome Flutter" src="https://img.shields.io/badge/Awesome-Flutter-blue.svg?longCache=true&style=flat-square" />
</a>

Thanks to [Oleksandr Leuschenko](https://github.com/olexale) for inspiration and his precious code: [arkit_flutter_plugin](https://github.com/olexale/arkit_flutter_plugin)

## Usage

I wrote 2 articles for setup you project and start with ARCore Flutter Plugin:

[ARCore Flutter Plugin: configurations](https://medium.com/@difrancescogianmarco/arcore-flutter-plugin-configurations-3ee53f2dc749).

[ARCore Flutter Plugin: add object on the plane](https://medium.com/@difrancescogianmarco/arcore-flutter-plugin-add-object-on-the-plane-8b3d7cbde3d3).


### Configure your app

To use this plugin, add arcore_flutter_plugin as a [dependency in your pubspec.yaml file](https://pub.dartlang.org/packages/arcore_flutter_plugin#-installing-tab-).

1. Follow [official guide](https://developers.google.com/ar/develop/java/enable-arcore) to enable ArCore

2. Add the Sceneform library to your app's build.gradle file:

```
android {
    // Sceneform libraries use language constructs from Java 8.
    // Add these compile options if targeting minSdkVersion < 26.
    compileOptions {
        sourceCompatibility 1.8
        targetCompatibility 1.8
    }
}

dependencies {
    …

    // Provides ArFragment, and other UX resources.
    implementation 'com.google.ar.sceneform.ux:sceneform-ux:1.8.0'

    // Alternatively, use ArSceneView without the UX dependency.
    implementation 'com.google.ar.sceneform:core:1.8.0'
}
```

3. [Import the Sceneform plugin into your project](https://developers.google.com/ar/develop/java/sceneform/#import-sceneform-plugin) (OPTIONAL)

## Example

The simplest code example:

```dart
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
        ),
        body: ArCoreView(
          onArCoreViewCreated: _onArCoreViewCreated,
        ),
      ),
    );
  }

  void _onArCoreViewCreated(ArCoreController controller) {
    arCoreController = controller;

    _addSphere(arCoreController);
    _addCylindre(arCoreController);
    _addCube(arCoreController);
  }

  void _addSphere(ArCoreController controller) {
    final material = ArCoreMaterial(
        color: Color.fromARGB(120, 66, 134, 244), texture: "earth.jpg");
    final sphere = ArCoreSphere(
      materials: [material],
      radius: 0.1,
    );
    final node = ArCoreNode(
      shape: sphere,
      position: vector.Vector3(0, 0, -1.5),
    );
    controller.add(node);
  }

  void _addCylindre(ArCoreController controller) {
    final material = ArCoreMaterial(
      color: Colors.red,
      reflectance: 1.0,
    );
    final cylindre = ArCoreCylinder(
      materials: [material],
      radius: 0.5,
      height: 0.3,
    );
    final node = ArCoreNode(
      shape: cylindre,
      position: vector.Vector3(0.0, -0.5, -2.0),
    );
    controller.add(node);
  }

  void _addCube(ArCoreController controller) {
    final material = ArCoreMaterial(
      color: Color.fromARGB(120, 66, 134, 244),
      metallic: 1.0,
    );
    final cube = ArCoreCube(
      materials: [material],
      size: vector.Vector3(0.5, 0.5, 0.5),
    );
    final node = ArCoreNode(
      shape: cube,
      position: vector.Vector3(-0.5, 0.5, -3.5),
    );
    controller.add(node);
  }

  @override
  void dispose() {
    arCoreController.dispose();
    super.dispose();
  }
}
```

See the `example` directory for a complete sample app.

## 3D Objects Credits 
[Anonymous](https://poly.google.com/user/f8cGQY15_-g)
