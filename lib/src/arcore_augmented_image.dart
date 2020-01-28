import 'package:arcore_flutter_plugin/src/arcore_pose.dart';

class ArCoreAugmentedImage {
  String name;
  int index;
  ArCorePose centerPose;
  TrackingMethod trackingMethod;
  double extentX;
  double extentZ;

  ArCoreAugmentedImage.fromMap(Map<dynamic, dynamic> map)
      : this.name = map['name'],
        this.index = map['index'],
        this.extentX = map['extentX'],
        this.extentZ = map['extentZ'],
        this.centerPose = ArCorePose.fromMap(map['centerPose']),
        this.trackingMethod = TrackingMethod.values[map['trackingMethod']];
}

enum TrackingMethod {
  NOT_TRACKING,
  FULL_TRACKING,
  LAST_KNOWN_POSE,
}
