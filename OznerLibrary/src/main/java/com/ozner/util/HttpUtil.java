package com.ozner.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by zhiyongxu on 15/10/20.
 */
public class HttpUtil {

    public static String postJSON(String url, String json) throws IOException {
            URL my_url = new URL(url);
            HttpURLConnection connection = null;
            connection = (HttpURLConnection) my_url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/json");
            OutputStream outputStream = connection.getOutputStream();
            outputStream.flush();
            outputStream.close();
            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                byte[] buff = new byte[500];
                while (true) {
                    int rd = inputStream.read(buff);
                    if (rd > 0) {
                        byteArrayOutputStream.write(buff);
                    } else
                        break;
                }
                byteArrayOutputStream.flush();
                if (byteArrayOutputStream.size() > 0) {
                    return new String(byteArrayOutputStream.toByteArray(), "UTF-8");
                } else
                    return "";
            } finally {
                byteArrayOutputStream.close();
            }


    }
}
