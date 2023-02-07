
I'm working to a new [sceneview_flutter](https://pub.dev/packages/sceneview_flutter) plugin to implement all features available in [sceneview](https://github.com/SceneView/sceneview-android).
SceneView is a [Sceneform Maintained](https://github.com/SceneView/sceneform-android) replacement in Kotlin.

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
        color: Color.fromARGB(120, 66, 134, 244));
    final sphere = ArCoreSphere(
      materials: [material],
      radius: 0.1,
    );
    final node = ArCoreNode(
      shape: sphere,
      position: vector.Vector3(0, 0, -1.5),
    );
    controller.addArCoreNode(node);
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
    controller.addArCoreNode(node);
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
    controller.addArCoreNode(node);
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

---

## Documentation 

### Classes provided by the plugin

**There are a total of 13 classes provided by this plugin until May 2020.**

- ArCoreView
- ArCoreController
- ArCoreFaceView
- ArCoreFaceContrller
- ArCoreSphere
- ArCoreCylinder
- ArCoreCube
- ArCoreNode
- ArCoeMaterial
- ArCoreHitTestResult
- ArCoreRotatingNode
- ArCorePlane
- ArCoreReferenceNode

---

### ArCoreView

This class returns the view type. There are two types of views in it.

**AUGMENTEDFACE**
**STANDARDVIEW**

There are 4 properties in it:
- onArCoreViewCreated
- enableTapRecoginzer
- enableUpdateListener
- type

---

### onArCoreViewCreated

This property takes a **ArCoreController**.

---

**enableTapRecoginzer**

Initially, set to false. It is used as an argument by the MethodChannel.

---

**enableUpdateListener** 

Initially, set to false. It is used as an argument by the MethodChannel.

---

**type**

It is a view type, it is either **AUGMENTEDFACE, STANDARDVIEW***. It is set to **STANDARDVIEW** by default.

---
### ArCoreController

This controller used to add a ArNode using addArCoreNode function, add a ArCoreNode with ancher using a addArCoreNodeWithAncher function and also remove node using removeNode function.

---

### ArCoreFaceView
It is a stateful widget that returns a **ArCoreAndroidView**. It has two properties **enableAugmentedFaces, onArCoreViewCreated**.

Initially, **enableAugmentedFaces** is set to false.
**onArCoreViewCreated** takes a function with **ArCoreController** argument.

---

### ArCoreFaceController
It used dispose and **loadMesh** method to control the **FaceView**.

---

### ArCoreSphere
It is **ArCoreShape**, takes a **radius & ArCoreMaterial**.

---

### ArCoreCylender
It is **ArCoreShape**, takes a **radius, height, & ArCoreMaterial**.

---

### ArCoreCube
It is **ArCoreShape**, takes a size i.e. **Vector3 & ArCoreMaterial**.

---

### ArCoreNode
This widget is used to provide the **position, shape, scale, rotation, name**.

---

### ArCoreMaterial
It is used to describe the outlook of the virtual object created by the user.

It has **color,textureBytes, metallic, roughness, reflection**.

---

### ArCoreRotatingNode
It is an **ArCoreNode** with a **degreesPerSecond** *property* which is a double value.

---

### ArCorePlane
It takes the **x, y** coordinate of the plane, **ArCorePose & ArCorePlaneType**.

There are three types of plane:
- **HORIZONTAL_UPWARD_FACING**
- **HORIZONTAL_DOWNWARD_FACING**
- **VERTICAL**

---

### ArCoreReferenceNode
It is ArCoreNode, it has all the properties that the ArCoreNode has also it has objectUrl and object3DFileName.

---

### objectUrl
URL of glft object for remote rendering.

---

### object3DFileName
Filename of sfb object in assets folder.
