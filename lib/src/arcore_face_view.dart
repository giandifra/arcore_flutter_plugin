import 'package:arcore_flutter_plugin/src/arcore_android_view.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import 'arcore_face_controller.dart';
import 'arcore_view.dart';

typedef void ArCoreFaceViewCreatedCallback(ArCoreFaceController controller);

class ArCoreFaceView extends StatefulWidget {
  final ArCoreFaceViewCreatedCallback onArCoreViewCreated;
  final bool enableAugmentedFaces;
  final bool debug;

  const ArCoreFaceView(
      {Key key,
      this.onArCoreViewCreated,
      this.enableAugmentedFaces = false,
      this.debug = false})
      : super(key: key);

  @override
  _ArCoreFaceViewState createState() => _ArCoreFaceViewState();
}

class _ArCoreFaceViewState extends State<ArCoreFaceView>
    with WidgetsBindingObserver {
  @override
  void initState() {
    WidgetsBinding.instance.addObserver(this);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return Container(
        child: ArCoreAndroidView(
          viewType: 'arcore_flutter_plugin',
          onPlatformViewCreated: _onPlatformViewCreated,
          arCoreViewType: ArCoreViewType.AUGMENTEDFACE,
          debug: widget.debug,
        ),
      );
    }
    return Center(
      child:
          Text('$defaultTargetPlatform is not supported by the ar_view plugin'),
    );
  }

  void _onPlatformViewCreated(int id) {
    if (widget.onArCoreViewCreated == null) {
      return;
    }
    widget.onArCoreViewCreated(
      ArCoreFaceController(
        id: id,
        enableAugmentedFaces: widget.enableAugmentedFaces,
      ),
    );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }
}
