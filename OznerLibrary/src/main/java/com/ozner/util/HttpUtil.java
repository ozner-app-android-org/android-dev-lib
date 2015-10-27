package com.ozner.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
        connection.setReadTimeout(1000 * 30);
        connection.setConnectTimeout(1000 * 10);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setUseCaches(false);
        //connection.setRequestProperty("Charset", "UTF-8");
        //connection.setRequestProperty("Content-Type", "application/json");
        try {
            connection.connect();
            OutputStream outputStream = connection.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            try {
                writer.write(json);
            } finally {
                outputStream.flush();
                outputStream.close();
            }

            InputStream inputStream = connection.getInputStream();
            try {
                InputStreamReader reader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(reader);
                StringBuffer strBuffer = new StringBuffer();
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    strBuffer.append(line);
                }
                return strBuffer.toString();
            } finally {
                inputStream.close();
            }

        } finally {
            connection.disconnect();
        }

    }
}
