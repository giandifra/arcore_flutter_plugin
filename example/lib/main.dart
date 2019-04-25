import 'package:arcore_flutter_plugin_example/screens/custom_object.dart';
import 'package:arcore_flutter_plugin_example/screens/hello_world.dart';
import 'package:arcore_flutter_plugin_example/screens/runtime_materials.dart';
import 'package:flutter/material.dart';

void main() => runApp(App());

class App extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: HomeScreen(),
    );
  }
}

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
              Navigator.of(context)
                  .push(MaterialPageRoute(builder: (context) => CustomObject()));
            },
            title: Text("Custom Anchored Object with onTap"),
          ),
          ListTile(
            onTap: () {
              Navigator.of(context)
                  .push(MaterialPageRoute(builder: (context) => RuntimeMaterials()));
            },
            title: Text("Change Materials Property in runtime"),
          )
        ],
      ),
    );
  }
}
