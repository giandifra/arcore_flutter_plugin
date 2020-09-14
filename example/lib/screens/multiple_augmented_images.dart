import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:arcore_flutter_plugin/arcore_flutter_plugin.dart';

class AugmentedImagesPage extends StatefulWidget {
  @override
  _AugmentedImagesPageState createState() => _AugmentedImagesPageState();
}

class _AugmentedImagesPageState extends State<AugmentedImagesPage> {
  ArCoreController arCoreController;
  Map<int, ArCoreAugmentedImage> augmentedImagesMap = Map();

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('AugmentedPage'),
        ),
        body: ArCoreView(
          onArCoreViewCreated: _onArCoreViewCreated,
          type: ArCoreViewType.AUGMENTEDIMAGES,
        ),
      ),
    );
  }

  void _onArCoreViewCreated(ArCoreController controller) async {
    arCoreController = controller;
    arCoreController.onTrackingImage = _handleOnTrackingImage;
    loadMultipleImage();
  }

  loadMultipleImage() async {
    final ByteData bytes1 =
        await rootBundle.load('assets/earth_augmented_image.jpg');
    final ByteData bytes2 = await rootBundle.load('assets/prova_texture.png');
    final ByteData bytes3 = await rootBundle.load('assets/umano_digitale.png');
    List<Uint8List> bytesList = List();
    bytesList.add(bytes1.buffer.asUint8List());
    bytesList.add(bytes2.buffer.asUint8List());
    bytesList.add(bytes3.buffer.asUint8List());

    arCoreController.loadMultipleAugmentedImage(bytes: bytesList);
  }

  _handleOnTrackingImage(ArCoreAugmentedImage augmentedImage) {
    if (!augmentedImagesMap.containsKey(augmentedImage.index)) {
      augmentedImagesMap[augmentedImage.index] = augmentedImage;
      _addSphere(augmentedImage);
    }
  }

  Future _addSphere(ArCoreAugmentedImage augmentedImage) async {
    final ByteData textureBytes = await rootBundle.load('assets/earth.jpg');

    final material = ArCoreMaterial(
      color: Color.fromARGB(120, 66, 134, 244),
      textureBytes: textureBytes.buffer.asUint8List(),
    );
    final sphere = ArCoreSphere(
      materials: [material],
      radius: augmentedImage.extentX / 2,
    );
    final node = ArCoreNode(
      shape: sphere,
    );
    arCoreController.addArCoreNodeToAugmentedImage(node, augmentedImage.index);
  }

  @override
  void dispose() {
    arCoreController.dispose();
    super.dispose();
  }
}
