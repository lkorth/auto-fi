package com.lukekorth.auto_fi.utilities;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtils {

    public static String readStream(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int count; (count = in.read(buffer)) != -1; ) {
            out.write(buffer, 0, count);
        }

        return new String(out.toByteArray(), "UTF-8");
    }

    public static String getAsset(Context context, String asset) {
        InputStream in = null;
        try {
            in = context.getAssets().open(asset);
            byte[] buffer = new byte[in.available()];
            in.read(buffer);
            return new String(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
