import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:arcore_flutter_plugin/arcore_flutter_plugin.dart';

class AugmentedPage extends StatefulWidget {
  @override
  _AugmentedPageState createState() => _AugmentedPageState();
}

class _AugmentedPageState extends State<AugmentedPage> {
  ArCoreController? arCoreController;
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
    arCoreController?.onTrackingImage = _handleOnTrackingImage;
    //loadSingleImage();
    //OR
    loadImagesDatabase();
  }

  loadSingleImage() async {
    final ByteData bytes =
        await rootBundle.load('assets/earth_augmented_image.jpg');
    arCoreController?.loadSingleAugmentedImage(
        bytes: bytes.buffer.asUint8List());
  }

  loadImagesDatabase() async {
    final ByteData bytes = await rootBundle.load('assets/myimages.imgdb');
    arCoreController?.loadAugmentedImagesDatabase(
        bytes: bytes.buffer.asUint8List());
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
    arCoreController?.addArCoreNodeToAugmentedImage(node, augmentedImage.index);
  }

  @override
  void dispose() {
    arCoreController?.dispose();
    super.dispose();
  }
}
