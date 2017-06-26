package com.lukekorth.auto_fi.utilities;

import android.content.Context;
import android.support.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    public static boolean isAvailable(Context context, String filename) {
        return get(context, filename).exists();
    }

    public static File get(Context context, String filename) {
        return new File(context.getFilesDir(), filename);
    }

    public static String read(Context context, String filename) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(get(context, filename)))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
                line = bufferedReader.readLine();
            }
            return stringBuilder.toString();
        }
    }

    public static void write(Context context, String fileContents, String filename) throws IOException {
        FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
        fos.write(fileContents.getBytes());
        fos.close();
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
}
