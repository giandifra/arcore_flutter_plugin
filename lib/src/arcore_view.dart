import 'package:arcore_flutter_plugin/src/arcore_android_view.dart';
import 'package:arcore_flutter_plugin/src/arcore_controller.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

typedef void ArCoreViewCreatedCallback(ArCoreController controller);

enum ArCoreViewType {
  AUGMENTEDFACE,
  STANDARDVIEW,
}

class ArCoreView extends StatefulWidget {
  final ArCoreViewCreatedCallback onArCoreViewCreated;

//  final UnsupportedHandler onArCoreUnsupported;

  final bool enableTapRecognizer;
  final bool enableUpdateListener;
  final ArCoreViewType type;

  const ArCoreView({
    Key key,
    @required this.onArCoreViewCreated,
//    @required this.onArCoreUnsupported,
    this.enableTapRecognizer = false,
    this.enableUpdateListener = false,
    this.type = ArCoreViewType.STANDARDVIEW,
  }) : super(key: key);

  @override
  _ArCoreViewState createState() => _ArCoreViewState();
}

class _ArCoreViewState extends State<ArCoreView> with WidgetsBindingObserver {
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
          arCoreViewType: widget.type,
        ),
      );
    }
    return Center(
      child: Text(
          '$defaultTargetPlatform is not  supported by the ar_view plugin'),
    );
  }

  void _onPlatformViewCreated(int id) {
    if (widget.onArCoreViewCreated == null) {
      return;
    }
    widget.onArCoreViewCreated(ArCoreController(
      id: id,
      enableTapRecognizer: widget.enableTapRecognizer,
      enableUpdateListener: widget.enableUpdateListener,
//      onUnsupported: widget.onArCoreUnsupported,
    ));
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }
}
