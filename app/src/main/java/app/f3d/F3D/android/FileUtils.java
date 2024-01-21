package app.f3d.F3D.android;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class FileUtils {

    public static String createTempFileFromUri(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                String originalFileName = getFileNameFromUri(context.getContentResolver(), uri);
                String[] nameAndExtension = extractNameAndExtension(originalFileName);
                File tempFile = createTempFile(context, nameAndExtension[0], nameAndExtension[1]);

                try (BufferedInputStream bis = new BufferedInputStream(inputStream);
                     BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(tempFile.toPath()))) {

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                    return tempFile.getAbsolutePath();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String[] extractNameAndExtension(String fileName) {
        String[] nameAndExtension = new String[2];
        String[] parts = fileName.split("\\.");

        if (parts.length >= 2) {
            nameAndExtension[0] = fileName.substring(0, fileName.lastIndexOf("."));
            nameAndExtension[1] = parts[parts.length - 1];
        } else {
            nameAndExtension[0] = fileName;
            nameAndExtension[1] = "";
        }

        return nameAndExtension;
    }

//    private static File createTempFile(Context context, String name, String extension) throws IOException {
//        File cacheDir = context.getCacheDir();
//        return new File(cacheDir, name + "-temp." + extension);
//    }

    private static File createTempFile(Context context, String name, String extension) throws IOException {
        // Get the external cache directory or another suitable directory
        File externalCacheDir = context.getExternalCacheDir();

        // Check if the external cache directory is available, otherwise, use the internal cache directory
        File cacheDir = (externalCacheDir != null) ? externalCacheDir : context.getCacheDir();

        // Create a subdirectory named "f3d" within the cache directory
        File f3dDir = new File(cacheDir, "f3d");
        if (!f3dDir.exists()) {
            f3dDir.mkdirs(); // Create the directory if it doesn't exist
        }

        // Create the temporary file within the "f3d" directory
        return new File(f3dDir, name + "." + extension);
    }


    private static String getFileNameFromUri(ContentResolver contentResolver, Uri uri) {
        String fileName = null;

        try (Cursor cursor = contentResolver.query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                fileName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
            }
        }

        return fileName;
    }
}
