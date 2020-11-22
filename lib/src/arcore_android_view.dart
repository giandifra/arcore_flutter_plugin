import 'package:arcore_flutter_plugin/src/arcore_view.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

typedef PlatformViewCreatedCallback = void Function(int id);

class ArCoreAndroidView extends AndroidView {
  final String viewType;
  final PlatformViewCreatedCallback onPlatformViewCreated;
  final ArCoreViewType arCoreViewType;
  final bool debug;

  ArCoreAndroidView(
      {Key key,
      @required this.viewType,
      this.onPlatformViewCreated,
      this.arCoreViewType = ArCoreViewType.STANDARDVIEW,
      this.debug = false})
      : super(
          viewType: viewType,
          onPlatformViewCreated: onPlatformViewCreated,
          creationParams: <String, dynamic>{
            "type": arCoreViewType == ArCoreViewType.AUGMENTEDFACE
                ? "faces"
                : arCoreViewType == ArCoreViewType.AUGMENTEDIMAGES
                    ? "augmented"
                    : "standard",
            "debug": debug
          },
          creationParamsCodec: const StandardMessageCodec(),
        );
}
