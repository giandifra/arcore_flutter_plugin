#import "ArcoreFlutterPlugin.h"
#import <arcore_flutter_plugin/arcore_flutter_plugin-Swift.h>

@implementation ArcoreFlutterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftArcoreFlutterPlugin registerWithRegistrar:registrar];
}
@end
