## 0.2.0-alpha

* Upgraded dart constraint >= 3.0.0 < 4.0.0
* Upgraded gradle to 7.3.1
* Upgraded kotlin to 1.7.20
* Upgraded compileSdk to 34
* Upgraded AR Core to 1.17.1
* Upgraded Sceneform to 1.17.1

## 0.1.0

* Upgraded dart constraint to 2.14
* Migrate example to null-safety
* Migrate to new android plugin api
* Bug fix (@shabbirAlam)
* Added "Screenshot feature" (@wiizarrrd)
* Fix null check operator
* Migrate to null-safety (@GTripathee)

## 0.0.11

* Add ability to toggle Plane Renderer Visibility during runtime (@matwright)
* Allow change Augmented Face assets on runtime (@francezu)
* Load multiple images into a database on a background thread (@AlinaStepanova)

## 0.0.10

* Fix debugLog error

## 0.0.9

* Initialize an AugmentedImageDatabase only with valid images (@AlinaStepanova)
* ArCoreReferenceNode objectUrl now using .glb or .gltf2 depending on file extension of URL (@BrutalCoding)
* Added new parameter to enable/disable the white dots on surface (@BrutalCoding)
* Added debug parameter to control logging for dart and kotlin (@xvld)

## 0.0.8

* Show 2D image with ArCoreImage

## 0.0.7

* You can load multiple image into augmented images database (@AlinaStepanova)
* Add check to see if AR are installed (@AlinaStepanova)
* Fix Exception when use with another plugin (@BrutalCoding)

## 0.0.6

* Added Augmented Image feature

## 0.0.5+1

* Migrate to new Android Plugin Api
* Update AR Core and Sceneform dependencies to 1.14+
* Add method to check AR Core compatibility
* Add Augmented Images feature (from single image or from imgdb file)

## 0.0.4

* Add AugmentedFace feature

## 0.0.2+2

* Add ArCoreReferenceNode for use local object (.sfb) or remote objcet (.gltf)

## 0.0.2+1

* Now you can define children property for node (see example custom_object.dart) 

## 0.0.2

* Add onPlaneDetect handler
* Add onPlaneTap handler
* Split addNode in addArCoreNode and addArCodeNodeWithAnchor (for detected plane)

## 0.0.1

* Add ArCoreNode
* Add ArCoreRotatingNode
* Add ArCoreShape (sphere, cube, cylindre)
* Add shape materials properties (color, texture, metallic, roughness, reflectance)