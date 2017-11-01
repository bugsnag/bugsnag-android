package com.bugsnag.android;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

class DefaultHttpClient implements ErrorReportApiClient, SessionTrackingApiClient {

    private final ConnectivityManager connectivityManager;

    DefaultHttpClient(ConnectivityManager connectivityManager) {
        this.connectivityManager = connectivityManager;
    }

    @Override
    public void postReport(String urlString,
                           Report report,
                           Map<String, String> headers) throws NetworkException, BadResponseException {
        int status = makeRequest(urlString, report, headers);

        if (status / 100 != 2) {
            throw new BadResponseException(urlString, status);
        }
    }

    @Override
    public void postSessionTrackingPayload(String urlString,
                                           SessionTrackingPayload payload,
                                           Map<String, String> headers) throws NetworkException, BadResponseException {
        int status = makeRequest(urlString, payload, headers);

        if (status != 202) {
            throw new BadResponseException(urlString, status);
        }
    }

    private int makeRequest(String urlString,
                            JsonStream.Streamable streamable,
                            Map<String, String> headers) throws NetworkException {
        checkHasNetworkConnection(urlString);
        HttpURLConnection conn = null;

        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setChunkedStreamingMode(0);
            conn.addRequestProperty("Content-Type", "application/json");

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.addRequestProperty(entry.getKey(), entry.getValue());
            }

            OutputStream out = null;

            try {
                out = conn.getOutputStream();
                JsonStream stream = new JsonStream(new OutputStreamWriter(out));
                streamable.toStream(stream);
                stream.close();
            } finally {
                IOUtils.closeQuietly(out);
            }


            // End the request, get the response code
            return conn.getResponseCode();
        } catch (IOException e) {
            throw new NetworkException(urlString, e);
        } finally {
            IOUtils.close(conn);
        }
    }

    private void checkHasNetworkConnection(String urlString) throws NetworkException {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if (!(activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting())) { // conserve device battery by avoiding radio use
            throw new NetworkException(urlString, new RuntimeException("No network connection available"));
        }
    }

}
