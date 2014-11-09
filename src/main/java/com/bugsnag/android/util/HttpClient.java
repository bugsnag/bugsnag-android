package com.bugsnag.android;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.net.HttpURLConnection;
import java.net.URL;

class HttpClient {
    public interface Streamable {
        public void toStream(Writer out);
    }

    public static void post(String urlString, Streamable payload) throws IOException {
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
                payload.toStream(new OutputStreamWriter(out));
            } finally {
                if(out != null) {
                    out.close();
                }
            }

            // End the request, get the response code
            int status = conn.getResponseCode();
            if(status / 100 != 2) {
                throw new IOException(String.format("Got non-200 response code (%d) from %s", status, urlString));
            }
        } catch (IOException e) {
            throw new IOException(String.format("Network error when posting to %s", urlString), e);
        } finally {
            if(conn != null) {
                conn.disconnect();
            }
        }
    }
}
