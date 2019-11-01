import 'dart:typed_data';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

import '../arcore_flutter_plugin.dart';

class ArCoreFaceController {
  ArCoreFaceController({
    int id,
    this.enableAugmentedFaces,
  }) {
    _channel = MethodChannel('arcore_flutter_plugin_$id');
    _channel.setMethodCallHandler(_handleMethodCalls);
    _channel.invokeMethod<void>('init', {
      'enableAugmentedFaces': enableAugmentedFaces,
    });
  }

  final bool enableAugmentedFaces;
  MethodChannel _channel;
  StringResultHandler onError;

  Future<dynamic> _handleMethodCalls(MethodCall call) async {
    print('_platformCallHandler call ${call.method} ${call.arguments}');
    switch (call.method) {
      case 'onError':
        if (onError != null) {
          onError(call.arguments);
        }
        break;
      default:
        print('Unknowm method ${call.method} ');
    }
    return Future.value();
  }

  Future<void> loadMesh(
      {@required Uint8List textureBytes, String skin3DModelFilename}) {
    assert(textureBytes != null);
    return _channel.invokeMethod('loadMesh', {
      'textureBytes': textureBytes,
      'skin3DModelFilename': skin3DModelFilename
    });
  }

  void dispose() {
    _channel?.invokeMethod<void>('dispose');
  }
}
