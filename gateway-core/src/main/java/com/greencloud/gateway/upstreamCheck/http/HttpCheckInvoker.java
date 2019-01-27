package com.greencloud.gateway.upstreamCheck.http;

import com.google.common.base.Strings;
import com.greencloud.gateway.upstreamCheck.config.Status;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author leejianhao
 */
public class HttpCheckInvoker {

    public static Status get(String url, int connectTimeout, int readTimeout, String expectResponse, Integer statusCode) {
        HttpURLConnection conn = null;
        BufferedReader rd = null;
        StringBuilder sb = new StringBuilder();
        String line = null;
        String response = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setReadTimeout(readTimeout);
            conn.setConnectTimeout(connectTimeout);
            conn.setUseCaches(false);
            conn.connect();

            if (statusCode != null) {
                if (statusCode == conn.getResponseCode()) {
                    return Status.UP;
                }
            }

            rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            response = sb.toString();

            if (!Strings.isNullOrEmpty(expectResponse)) {
                if (expectResponse.contains(response)) {
                    return Status.UP;
                }
            }

            return Status.DOWN;
        } catch (MalformedURLException e) {
            return Status.DOWN;
        } catch (IOException e) {
            return Status.DOWN;
        } finally {
            try {
                if (rd != null) {
                    rd.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
