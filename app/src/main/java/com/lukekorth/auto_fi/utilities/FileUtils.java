package com.lukekorth.auto_fi.utilities;

import android.content.Context;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    public static File getFile(Context context, String filename) {
        return new File(context.getFilesDir(), filename);
    }

    public static boolean isFileAvailable(Context context, String filename) {
        return getFile(context, filename).exists();
    }

    public static File writeAssetFileToDisk(Context context, String assetFile, boolean executable)
            throws IOException {
        return writeAssetFileToDisk(context, assetFile, assetFile, executable);
    }

    @Nullable
    public static File writeAssetFileToDisk(Context context, String assetFile, String destinationFilename,
                                            boolean executable) throws IOException {
        InputStream in = context.getAssets().open(assetFile);
        byte[] buffer = new byte[in.available()];
        in.read(buffer);
        in.close();

        FileOutputStream fos = context.openFileOutput(destinationFilename, Context.MODE_PRIVATE);
        fos.write(buffer);
        fos.close();

        File file = context.getFileStreamPath(destinationFilename);
        if (file.setExecutable(executable)) {
            return file;
        } else {
            return null;
        }
    }

    public static void writeToDisk(Context context, String fileContents, String filename) throws IOException {
        FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
        fos.write(fileContents.getBytes());
        fos.close();
    }
}
