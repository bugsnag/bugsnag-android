package com.bugsnag.android;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A Request represents an HTTP request that forms part of an {@link Event}.
 */
@SuppressWarnings("ConstantValue")
public final class Request extends AbstractHttpEntity implements JsonStream.Streamable {
    private final Map<String, String> params = new LinkedHashMap<>();

    @Nullable
    private String httpMethod;

    @Nullable
    private String httpVersion;

    @Nullable
    private String url;

    /**
     * Constructs a new Request with the specified HTTP version, method, and URL.
     * If the URL contains query parameters, they will be extracted and stored separately.
     *
     * @param httpVersion the HTTP version (e.g. "HTTP/1.1"), or null
     * @param httpMethod  the HTTP method (e.g. "GET", "POST"), or null
     * @param url         the request URL, optionally including query parameters, or null
     */
    public Request(
            @Nullable String httpVersion,
            @Nullable String httpMethod,
            @Nullable String url) {

        this.httpMethod = httpMethod;
        this.httpVersion = httpVersion;
        setUrl(url);
    }

    /**
     * Constructs a new Request with the specified HTTP method and URL.
     * The HTTP version will be set to null.
     * If the URL contains query parameters, they will be extracted and stored separately.
     *
     * @param httpMethod the HTTP method (e.g. "GET", "POST"), or null
     * @param url        the request URL, optionally including query parameters, or null
     */
    public Request(
            @Nullable String httpMethod,
            @Nullable String url) {

        this(null, httpMethod, url);
    }

    /**
     * Return the HTTP method for this request (e.g. "GET").
     *
     * @return the HTTP method
     */
    @Nullable
    public String getHttpMethod() {
        return httpMethod;
    }

    /**
     * Set the HTTP method for this request (e.g. "GET", "POST").
     *
     * @param httpMethod the HTTP method name
     */
    public void setHttpMethod(@Nullable String httpMethod) {
        this.httpMethod = httpMethod;
    }

    /**
     * Return the HTTP version for this request (e.g. "HTTP/1.1").
     *
     * @return the HTTP version
     */
    @Nullable
    public String getHttpVersion() {
        return httpVersion;
    }

    /**
     * Set the HTTP version for this request (e.g. "HTTP/1.1").
     *
     * @param httpVersion the HTTP version
     */
    public void setHttpVersion(@Nullable String httpVersion) {
        this.httpVersion = httpVersion;
    }

    /**
     * Return the URL for this request, excluding query parameters.
     *
     * @return the request URL
     */
    @Nullable
    public String getUrl() {
        return url;
    }

    /**
     * Set the URL for this request. If the URL contains a query string, the query parameters
     * will be extracted and stored separately, and the URL will be set without the query string.
     *
     * @param url the request URL, optionally including query parameters
     */
    public void setUrl(@Nullable String url) {
        if (url != null) {
            int querySeparatorIndex = url.indexOf('?');

            if (querySeparatorIndex > 0) {
                setUrlWithQueryString(url);
            } else {
                this.url = url;
            }
        } else {
            this.url = "";
            this.params.clear();
        }
    }

    private void setUrlWithQueryString(@NonNull String url) {
        try {
            Uri uri = Uri.parse(url);

            params.clear();
            for (String queryName : uri.getQueryParameterNames()) {
                params.put(queryName, uri.getQueryParameter(queryName));
            }

            this.url = uri.buildUpon()
                    .clearQuery()
                    .build()
                    .toString();
        } catch (RuntimeException ignored) {
            this.url = url;
        }
    }

    /**
     * Add a query parameter to this reported request.
     *
     * @param name  the name of the query parameter
     * @param value the value of the query parameter
     */
    public void addQueryParameter(@NonNull String name, @Nullable String value) {
        if (name != null) {
            params.put(name, value);
        }
    }

    /**
     * Remove the specified query parameter by name (case-sensitive).
     *
     * @param name the query parameter to remove
     */
    public void removeQueryParameter(@NonNull String name) {
        params.remove(name);
    }

    /**
     * Return the query parameter names that are set for this request.
     *
     * @return the query parameter names
     */
    @NonNull
    public Set<String> getQueryParameterNames() {
        return Collections.unmodifiableSet(params.keySet());
    }

    /**
     * Return the query parameter value by name or null if the parameter is not present.
     *
     * @param name the query parameter name (case-sensitive)
     * @return the value of the query parameter or null
     */
    @Nullable
    public String getQueryParameter(@NonNull String name) {
        return params.get(name);
    }

    @Override
    public void toStream(@NotNull JsonStream writer) throws IOException {
        writer.beginObject();
        writer.name("httpMethod").value(httpMethod);
        writer.name("httpVersion").value(httpVersion);
        writer.name("url").value(url);

        writer.name("body").value(getBody());

        long bodyLength = getBodyLength();
        if (bodyLength >= 0) {
            writer.name("bodyLength").value(bodyLength);
        }

        writer.name("headers").value(headers, true);
        writer.name("params").value(params, true);

        writer.endObject();
    }
}
