package com.lukekorth.auto_fi.utilities;

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
}
