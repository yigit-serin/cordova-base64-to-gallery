package it.nexxa.base64ToGallery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Objects;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.content.Context;

/**
 * Base64ToGallery.java
 * <p>
 * Android implementation of the Base64ToGallery for iOS.
 * Inspirated by Joseph's "Save HTML5 Canvas Image to Gallery" plugin
 * http://jbkflex.wordpress.com/2013/06/19/save-html5-canvas-image-to-gallery-phonegap-android-plugin/
 *
 * @author Vegard Løkken <vegard@headspin.no>
 */
public class Base64ToGallery extends CordovaPlugin {

    private Boolean isBuildVersionEqualOrOverAndroid10;

    public Base64ToGallery() {
        this.isBuildVersionEqualOrOverAndroid10 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    // Consts
    public static final String EMPTY_STR = "";

    @Override
    public boolean execute(String action, JSONArray args,
                           CallbackContext callbackContext) throws JSONException {

        String base64 = args.optString(0);
        String filePrefix = args.optString(1);
        boolean mediaScannerEnabled = args.optBoolean(2);

        // isEmpty() requires API level 9
        if (base64.equals(EMPTY_STR)) {
            callbackContext.error("Missing base64 string");
        }

        // Create the bitmap from the base64 string
        byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
        Bitmap bmp = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        if (bmp == null) {
            callbackContext.error("The image could not be decoded");

        } else {

            // Save the image
            File imageFile = savePhoto(bmp, filePrefix);

            if (imageFile == null) {
                callbackContext.error("Error while saving image");
            }

            // Update image gallery
            if (mediaScannerEnabled) {
                scanPhoto(imageFile);
            }

            callbackContext.success(imageFile.toString());
        }

        return true;
    }

    private File savePhoto(Bitmap bmp, String prefix) {
        File retVal = null;

        try {
            String deviceVersion = Build.VERSION.RELEASE;
            Calendar c = Calendar.getInstance();
            String date = EMPTY_STR
                    + c.get(Calendar.YEAR)
                    + c.get(Calendar.MONTH)
                    + c.get(Calendar.DAY_OF_MONTH)
                    + c.get(Calendar.HOUR_OF_DAY)
                    + c.get(Calendar.MINUTE)
                    + c.get(Calendar.SECOND);

            if (isBuildVersionEqualOrOverAndroid10) {

                ContentResolver resolver = this.cordova.getActivity().getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, prefix + date + ".png");
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                OutputStream imageOut = resolver.openOutputStream(Objects.requireNonNull(imageUri));
                bmp.compress(Bitmap.CompressFormat.PNG, 100, imageOut);
                imageOut.flush();
                imageOut.close();
                retVal = new File(imageUri.toString());

            } else {

                File folder;
                folder = Environment.getExternalStorageDirectory();
                File imageFile = new File(folder, prefix + date + ".png");
                FileOutputStream out = new FileOutputStream(imageFile);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();

                retVal = imageFile;
            }

        } catch (Exception e) {
            Log.e("Base64ToGallery", "An exception occured while saving image: " + e.toString());
        }

        return retVal;
    }

    /**
     * Invoke the system's media scanner to add your photo to the Media Provider's database,
     * making it available in the Android Gallery application and to other apps.
     */
    private void scanPhoto(File imageFile) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(imageFile);

        mediaScanIntent.setData(contentUri);

        cordova.getActivity().sendBroadcast(mediaScanIntent);
    }
}
