package com.difrancescogianmarco.arcore_flutter_plugin.utils

import java.nio.ByteBuffer
import java.io.File
import java.io.OutputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import android.content.pm.PackageManager;
import android.view.PixelCopy
import android.graphics.Canvas
import android.os.Handler
import android.os.Environment
import android.os.Build
import android.Manifest;
import android.graphics.Bitmap
import android.app.Activity
import android.util.Log
import io.flutter.plugin.common.MethodChannel
import com.google.ar.sceneform.ArSceneView

class ScreenshotsUtils {

    companion object {

        fun getPictureName(): String {

            var sDate: String = SimpleDateFormat("yyyyMMddHHmmss").format(Date());

            return "MyApp-" + sDate + ".png";
        }


        fun saveBitmap(bitmap: Bitmap,activity: Activity): String {

            
            val externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
            
            val sDir = externalDir + File.separator + "MyApp";
                
            val dir = File(sDir);

            val dirPath: String;

            if( dir.exists() || dir.mkdir()) {
                dirPath = sDir + File.separator + getPictureName();
            } else {
                dirPath = externalDir + File.separator + getPictureName();
            }
        

            
            try{

                val file = File(dirPath)
            
                // Get the file output stream
                val stream: OutputStream = FileOutputStream(file)
                
                // Compress bitmap
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                
                // Flush the stream
                stream.flush()

                // Close stream
                stream.close()

            
           }catch (e: Exception){
                e.printStackTrace()
            }


            return dirPath;
           


        }

        fun permissionToWrite(activity: Activity): Boolean {
            
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Log.i("Sreenshot", "Permission to write false due to version codes.");

                return false;
            }

            var perm = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if(perm == PackageManager.PERMISSION_GRANTED) {
                Log.i("Sreenshot", "Permission to write granted!");

                return true;
            }

            Log.i("Sreenshot","Requesting permissions...");
            activity.requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                11
            );
            Log.i("Sreenshot", "No permissions :(");

            return false;
        } 


        fun onGetSnapshot(arSceneView: ArSceneView?, result: MethodChannel.Result,activity: Activity){

            if( !permissionToWrite(activity) ) {
                Log.i("Sreenshot", "Permission to write files missing!");

                result.success(null);

                return;
            }

            if(arSceneView == null){
                Log.i("Sreenshot", "Ar Scene View is NULL!");

                result.success(null);

                return;
            }
     
           
            try {

                //val view = arSceneView!!
                val view: ArSceneView = arFragment.getArSceneView()

                val bitmapImage: Bitmap = Bitmap.createBitmap(
                                view.getWidth(),
                                view.getHeight(),
                                Bitmap.Config.ARGB_8888
                        );
                Log.i("Sreenshot", "PixelCopy requesting now...");
                final HandlerThread handlerThread = new HandlerThread("PixelCopier");
                handlerThread.start();
                Log.i("Sreenshot", "handlerThread $handlerThread");
                PixelCopy.request(view, bitmapImage, { copyResult -> 
                      if (copyResult == PixelCopy.SUCCESS) {
                        Log.i("Sreenshot", "PixelCopy request SUCESS. ${copyResult}");
                        
                        var pathSaved: String = saveBitmap(bitmapImage,activity);

                        Log.i("Sreenshot", "Saved on path: ${pathSaved}");
                        result.success(pathSaved);

                      }else{
                          Log.i("Sreenshot", "PixelCopy request failed. ${copyResult}");
                          result.success(null);
                      }

                    }, 
                    Handler());
                
            } catch (e: Exception){

                e.printStackTrace()
            }
            
            
        }
    }
}