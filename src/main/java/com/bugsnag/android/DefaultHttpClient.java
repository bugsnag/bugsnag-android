package com.bugsnag.android;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

class DefaultHttpClient implements ErrorReportApiClient {

    @Override
    public void postReport(String urlString, Report report) throws NetworkException, BadResponseException {
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
                report.toStream(stream);
                stream.close();
            } finally {
                IOUtils.closeQuietly(out);
            }

            // End the request, get the response code
            int status = conn.getResponseCode();
            if (status / 100 != 2) {
                throw new BadResponseException(urlString, status);
            }
        } catch (IOException e) {
            throw new NetworkException(urlString, e);
        } finally {
            IOUtils.close(conn);
        }
    }

}
