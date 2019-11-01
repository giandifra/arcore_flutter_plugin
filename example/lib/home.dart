import 'package:arcore_flutter_plugin_example/screens/augmented_faces.dart';
import 'package:flutter/material.dart';
import 'screens/hello_world.dart';
import 'screens/custom_object.dart';
import 'screens/runtime_materials.dart';
import 'screens/texture_and_rotation.dart';
import 'screens/assets_object.dart';
import 'screens/auto_detect_plane.dart';
import 'screens/remote_object.dart';

class HomeScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('ArCore Demo'),
      ),
      body: ListView(
        children: <Widget>[
          ListTile(
            onTap: () {
              Navigator.of(context)
                  .push(MaterialPageRoute(builder: (context) => HelloWorld()));
            },
            title: Text("Hello World"),
          ),
          ListTile(
            onTap: () {
              Navigator.of(context).push(
                  MaterialPageRoute(builder: (context) => CustomObject()));
            },
            title: Text("Custom Anchored Object with onTap"),
          ),
          ListTile(
            onTap: () {
              Navigator.of(context).push(
                  MaterialPageRoute(builder: (context) => RuntimeMaterials()));
            },
            title: Text("Change Materials Property in runtime"),
          ),
          ListTile(
            onTap: () {
              Navigator.of(context).push(MaterialPageRoute(
                  builder: (context) => ObjectWithTextureAndRotation()));
            },
            title: Text("Custom object with texture and rotation listener "),
          ),
          ListTile(
            onTap: () {
              Navigator.of(context).push(
                  MaterialPageRoute(builder: (context) => AutoDetectPlane()));
            },
            title: Text("Plane detect handler"),
          ),
          ListTile(
            onTap: () {
              Navigator.of(context).push(
                  MaterialPageRoute(builder: (context) => AssetsObject()));
            },
            title: Text("Custom sfb object"),
          ),
          ListTile(
            onTap: () {
              Navigator.of(context).push(
                  MaterialPageRoute(builder: (context) => RemoteObject()));
            },
            title: Text("Remote object"),
          ),
          ListTile(
            onTap: () {
              Navigator.of(context).push(MaterialPageRoute(
                  builder: (context) => AugmentedFacesScreen()));
            },
            title: Text("Augmented Faces"),
          ),
        ],
      ),
    );
  }
}
