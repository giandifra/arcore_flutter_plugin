import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

typedef PlatformViewCreatedCallback = void Function(int id);

class ArCoreAndroidView extends AndroidView {
  final String viewType;
  final PlatformViewCreatedCallback onPlatformViewCreated;

  ArCoreAndroidView({
    Key key,
    @required this.viewType,
    this.onPlatformViewCreated,
  }) : super(viewType: viewType, onPlatformViewCreated: onPlatformViewCreated);
}
