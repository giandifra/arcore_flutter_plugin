import 'dart:math';

import 'package:arcore_flutter_plugin/arcore_flutter_plugin.dart';
import 'package:flutter/material.dart';
import 'package:vector_math/vector_math_64.dart' as vector;

class RuntimeMaterials extends StatefulWidget {
  @override
  _RuntimeMaterialsState createState() => _RuntimeMaterialsState();
}

class _RuntimeMaterialsState extends State<RuntimeMaterials> {
  ArCoreController? arCoreController;
  ArCoreNode? sphereNode;

  double metallic = 0.0;
  double roughness = 0.4;
  double reflectance = 0.5;
  Color color = Colors.yellow;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Materials Runtime Change'),
          actions: <Widget>[
            IconButton(
              icon: Icon(Icons.update),
              onPressed: () {},
            )
          ],
        ),
        body: Column(
          children: <Widget>[
            SphereControl(
              initialColor: color,
              initialMetallicValue: metallic,
              initialRoughnessValue: roughness,
              initialReflectanceValue: reflectance,
              onColorChange: onColorChange,
              onMetallicChange: onMetallicChange,
              onRoughnessChange: onRoughnessChange,
              onReflectanceChange: onReflectanceChange,
            ),
            Expanded(
              child: ArCoreView(
                onArCoreViewCreated: _onArCoreViewCreated,
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _onArCoreViewCreated(ArCoreController controller) {
    arCoreController = controller;

    _addSphere();
  }

  void _addSphere() {
    final material = ArCoreMaterial(
      color: Colors.yellow,
    );
    final sphere = ArCoreSphere(
      materials: [material],
      radius: 0.1,
    );
    sphereNode = ArCoreNode(
      shape: sphere,
      position: vector.Vector3(0, 0, -1.5),
    );
    arCoreController?.addArCoreNode(sphereNode!);
  }

  onColorChange(Color newColor) {
    if (newColor != this.color) {
      this.color = newColor;
      updateMaterials();
    }
  }

  onMetallicChange(double newMetallic) {
    if (newMetallic != this.metallic) {
      metallic = newMetallic;
      updateMaterials();
    }
  }

  onRoughnessChange(double newRoughness) {
    if (newRoughness != this.roughness) {
      this.roughness = newRoughness;
      updateMaterials();
    }
  }

  onReflectanceChange(double newReflectance) {
    if (newReflectance != this.reflectance) {
      this.reflectance = newReflectance;
      updateMaterials();
    }
  }

  updateMaterials() {
    debugPrint("updateMaterials");
    if (sphereNode == null) {
      return;
    }
    debugPrint("updateMaterials sphere node not null");
    final material = ArCoreMaterial(
      color: color,
      metallic: metallic,
      roughness: roughness,
      reflectance: reflectance,
    );
    sphereNode?.shape?.materials.value = [material];
  }

  @override
  void dispose() {
    arCoreController?.dispose();
    super.dispose();
  }
}

class SphereControl extends StatefulWidget {
  final double? initialRoughnessValue;
  final double? initialReflectanceValue;
  final double? initialMetallicValue;
  final Color? initialColor;
  final ValueChanged<Color>? onColorChange;
  final ValueChanged<double>? onMetallicChange;
  final ValueChanged<double>? onRoughnessChange;
  final ValueChanged<double>? onReflectanceChange;

  const SphereControl(
      {Key? key,
      this.initialRoughnessValue,
      this.initialReflectanceValue,
      this.initialMetallicValue,
      this.initialColor,
      this.onColorChange,
      this.onMetallicChange,
      this.onRoughnessChange,
      this.onReflectanceChange})
      : super(key: key);

  @override
  _SphereControlState createState() => _SphereControlState();
}

class _SphereControlState extends State<SphereControl> {
  late double metallicValue;
  late double roughnessValue;
  late double reflectanceValue;
  Color? color;

  @override
  void initState() {
    roughnessValue = widget.initialRoughnessValue ?? 0.0;
    reflectanceValue = widget.initialReflectanceValue ?? 0.0;
    metallicValue = widget.initialRoughnessValue ?? 0.0;
    color = widget.initialColor;
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          Row(
            children: <Widget>[
              ElevatedButton(
                child: Text("Random Color"),
                onPressed: () {
                  final newColor = Colors.accents[Random().nextInt(14)];
                  widget.onColorChange?.call(newColor);
                  setState(() {
                    color = newColor;
                  });
                },
              ),
              Padding(
                padding: const EdgeInsets.only(left: 20.0),
                child: CircleAvatar(
                  radius: 20.0,
                  backgroundColor: color,
                ),
              ),
            ],
          ),
          Row(
            children: <Widget>[
              Text("Metallic"),
              Checkbox(
                value: metallicValue == 1.0,
                onChanged: (value) {
                  metallicValue = (value ?? false) ? 1.0 : 0.0;
                  widget.onMetallicChange?.call(metallicValue);
                  setState(() {});
                },
              )
            ],
          ),
          Row(
            children: <Widget>[
              Text("Roughness"),
              Expanded(
                child: Slider(
                  value: roughnessValue,
                  divisions: 10,
                  onChangeEnd: (value) {
                    roughnessValue = value;
                    widget.onRoughnessChange?.call(roughnessValue);
                  },
                  onChanged: (double value) {
                    setState(() {
                      roughnessValue = value;
                    });
                  },
                ),
              ),
            ],
          ),
          Row(
            children: <Widget>[
              Text("Reflectance"),
              Expanded(
                child: Slider(
                  value: reflectanceValue,
                  divisions: 10,
                  onChangeEnd: (value) {
                    reflectanceValue = value;
                    widget.onReflectanceChange?.call(reflectanceValue);
                  },
                  onChanged: (double value) {
                    setState(() {
                      reflectanceValue = value;
                    });
                  },
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
