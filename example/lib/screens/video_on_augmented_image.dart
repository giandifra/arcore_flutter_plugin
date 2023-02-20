import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:arcore_flutter_plugin/arcore_flutter_plugin.dart';
import 'package:path/path.dart';
import 'package:path_provider/path_provider.dart';
import 'package:vector_math/vector_math_64.dart' as vector;
import 'package:http/http.dart' as http;

class VideoOnAugmentedImage extends StatefulWidget {
  @override
  _VideoOnAugmentedImageState createState() => _VideoOnAugmentedImageState();
}

class _VideoOnAugmentedImageState extends State<VideoOnAugmentedImage> {
  ArCoreController? arCoreController;
  Map<String, ArCoreAugmentedImage> augmentedImagesMap = Map();
  Map<String, Uint8List> bytesMap = Map();

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Video on augmented image'),
        ),
        body: ArCoreView(
          onArCoreViewCreated: _onArCoreViewCreated,
          type: ArCoreViewType.AUGMENTEDIMAGES,
          debug: true,
        ),
      ),
    );
  }

  void _onArCoreViewCreated(ArCoreController controller) async {
    print('AugmentedImages _onArCoreViewCreated');
    arCoreController = controller;
    arCoreController?.onTrackingImage = _handleOnTrackingImage;
    // Future.delayed(Duration(seconds: 5)).then((value) {
    loadMultipleImage();
    // });
  }

  loadMultipleImage() async {
    final bytes1 = await rootBundle.load('assets/earth_augmented_image.jpg');
    bytesMap["earth_augmented_image"] = bytes1.buffer.asUint8List();

    var response = await http.get(Uri.parse(
        'https://github.com/SceneView/sceneform-android/blob/master/samples/augmented-images/src/main/res/drawable-xxhdpi/rabbit.png?raw=true'));
    bytesMap["rabbit"] = response.bodyBytes;
    arCoreController?.loadAugmentedImages(bytesMap: bytesMap);
  }

  _handleOnTrackingImage(ArCoreAugmentedImage augmentedImage) async {
    if (!augmentedImagesMap.containsKey(augmentedImage.name)) {
      augmentedImagesMap[augmentedImage.name] = augmentedImage;
      final bytes2 = await rootBundle.load('assets/sintel.mp4');
      final node = ArCoreVideoNode(
        scale:
            vector.Vector3(augmentedImage.extentX, 1.0, augmentedImage.extentZ),
        video: ArCoreVideo(
          bytes: bytes2.buffer.asUint8List(),
        ),
      );
      arCoreController?.addArCoreNodeToAugmentedImage(
        node,
        augmentedImage.index,
      );
    }
  }

  @override
  void dispose() {
    arCoreController?.dispose();
    super.dispose();
  }
}
