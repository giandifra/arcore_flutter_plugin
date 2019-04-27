import 'package:arcore_flutter_plugin/src/arcore_material.dart';
import 'package:flutter/widgets.dart';

abstract class ArCoreShape {
  ArCoreShape({@required List<ArCoreMaterial> materials})
      : materials = ValueNotifier(materials);

  final ValueNotifier<List<ArCoreMaterial>> materials;

  Map<String, dynamic> toMap() => <String, dynamic>{
        'dartType': runtimeType.toString(),
        'materials': materials != null
            ? materials?.value?.map((m) => m.toMap())?.toList()
            : null,
      }..removeWhere((String k, dynamic v) => v == null);
}
