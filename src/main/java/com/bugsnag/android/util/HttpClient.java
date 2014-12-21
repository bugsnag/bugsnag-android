package com.bugsnag.android;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.net.HttpURLConnection;
import java.net.URL;

class HttpClient {
    static class BadResponseException extends Exception {
        public BadResponseException(String url, int responseCode) {
            super(String.format("Got non-200 response code (%d) from %s", responseCode, url));
        }
    }

    static class NetworkException extends IOException {
        public NetworkException(String url, Exception ex) {
            super(String.format("Network error when posting to %s", url));
            initCause(ex);
        }
    }

    static void post(String urlString, JsonStream.Streamable payload) throws NetworkException, BadResponseException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setChunkedStreamingMode(0);
            conn.addRequestProperty("Content-Type", "application/json");

            OutputStream out = null;
            try {
                out = conn.getOutputStream();

                JsonStream stream = new JsonStream(new OutputStreamWriter(out));
                payload.toStream(stream);
                stream.close();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // End the request, get the response code
            int status = conn.getResponseCode();
            if(status / 100 != 2) {
                throw new BadResponseException(urlString, status);
            }
        } catch (IOException e) {
            throw new NetworkException(urlString, e);
        } finally {
            if(conn != null) {
                conn.disconnect();
            }
        }
    }
}
