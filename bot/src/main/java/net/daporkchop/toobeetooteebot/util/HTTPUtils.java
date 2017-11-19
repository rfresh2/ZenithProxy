/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2017 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.daporkchop.toobeetooteebot.util;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

@SuppressWarnings("deprecation")
public class HTTPUtils {
    public static HttpURLConnection createUrlConnection(final URL url) throws IOException {
        Validate.notNull(url);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setUseCaches(false);
        return connection;
    }

    /**
     * Performs a POST request to the specified URL and returns the result.
     * <p/>
     * The POST data will be encoded in UTF-8 as the specified contentType. The response will be parsed as UTF-8.
     * If the server returns an error but still provides a body, the body will be returned as normal.
     * If the server returns an error without any body, a relevant {@link java.io.IOException} will be thrown.
     *
     * @param url         URL to submit the POST request to
     * @param post        POST data in the correct format to be submitted
     * @param contentType Content type of the POST data
     * @return Raw text response from the server
     * @throws IOException The request was not successful
     */
    public static String performPostRequest(final URL url, final String post, final String contentType) throws IOException {
        Validate.notNull(url);
        Validate.notNull(post);
        Validate.notNull(contentType);
        final HttpURLConnection connection = createUrlConnection(url);
        final byte[] postAsBytes = post.getBytes(Charsets.UTF_8);

        connection.setRequestProperty("Content-Type", contentType + "; charset=utf-8");
        connection.setRequestProperty("Content-Length", "" + postAsBytes.length);
        connection.setDoOutput(true);

        OutputStream outputStream = null;
        try {
            outputStream = connection.getOutputStream();
            IOUtils.write(postAsBytes, outputStream);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
            final String result = IOUtils.toString(inputStream, Charsets.UTF_8);
            int statusCode = connection.getResponseCode();
            return result;
        } catch (final IOException e) {
            IOUtils.closeQuietly(inputStream);
            inputStream = connection.getErrorStream();

            if (inputStream != null) {
                final String result = IOUtils.toString(inputStream, Charsets.UTF_8);
                return result;
            } else {
                throw e;
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Performs a POST request to the specified URL and returns the result and the int status code.
     * <p/>
     * The POST data will be encoded in UTF-8 as the specified contentType. The response will be parsed as UTF-8.
     * If the server returns an error but still provides a body, the body will be returned as normal.
     * If the server returns an error without any body, a relevant {@link java.io.IOException} will be thrown.
     *
     * @param url         URL to submit the POST request to
     * @param post        POST data in the correct format to be submitted
     * @param contentType Content type of the POST data
     * @return Raw text response from the server
     * @throws IOException The request was not successful
     */
    public static HTTPRequest performPostRequestWithCode(final URL url, final String post, final String contentType) throws IOException {
        Validate.notNull(url);
        Validate.notNull(post);
        Validate.notNull(contentType);
        final HttpURLConnection connection = createUrlConnection(url);
        final byte[] postAsBytes = post.getBytes(Charsets.UTF_8);

        connection.setRequestProperty("Content-Type", contentType + "; charset=utf-8");
        connection.setRequestProperty("Content-Length", "" + postAsBytes.length);
        connection.setDoOutput(true);

        OutputStream outputStream = null;
        try {
            outputStream = connection.getOutputStream();
            IOUtils.write(postAsBytes, outputStream);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
            final String result = IOUtils.toString(inputStream, Charsets.UTF_8);
            int statusCode = connection.getResponseCode();
            return new HTTPRequest(result, statusCode);
        } catch (final IOException e) {
            IOUtils.closeQuietly(inputStream);
            inputStream = connection.getErrorStream();

            if (inputStream != null) {
                final String result = IOUtils.toString(inputStream, Charsets.UTF_8);
                int statusCode = connection.getResponseCode();
                return new HTTPRequest(result, statusCode);
            } else {
                throw e;
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Performs a POST request to the specified URL and returns the result.
     * <p/>
     * The POST data will be encoded in UTF-8 as the specified contentType. The response will be parsed as UTF-8.
     * If the server returns an error but still provides a body, the body will be returned as normal.
     * If the server returns an error without any body, a relevant {@link java.io.IOException} will be thrown.
     *
     * @param url         URL to submit the POST request to
     * @param post        POST data in the correct format to be submitted
     * @param contentType Content type of the POST data
     * @return Raw text response from the server
     * @throws IOException The request was not successful
     */
    public static String performPostRequestWithAuth(final URL url, final String post, final String contentType, final String auth) throws IOException {
        Validate.notNull(url);
        Validate.notNull(post);
        Validate.notNull(contentType);
        final HttpURLConnection connection = createUrlConnection(url);
        final byte[] postAsBytes = post.getBytes(Charsets.UTF_8);

        connection.setRequestProperty("Authorization", auth);
        connection.setRequestProperty("Content-Type", contentType + "; charset=utf-8");
        connection.setRequestProperty("Content-Length", "" + postAsBytes.length);
        connection.setDoOutput(true);

        OutputStream outputStream = null;
        try {
            outputStream = connection.getOutputStream();
            IOUtils.write(postAsBytes, outputStream);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
            final String result = IOUtils.toString(inputStream, Charsets.UTF_8);
            return result;
        } catch (final IOException e) {
            IOUtils.closeQuietly(inputStream);
            inputStream = connection.getErrorStream();

            if (inputStream != null) {
                final String result = IOUtils.toString(inputStream, Charsets.UTF_8);
                return result;
            } else {
                throw e;
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Performs a GET request to the specified URL and returns the result.
     * <p/>
     * The response will be parsed as UTF-8.
     * If the server returns an error but still provides a body, the body will be returned as normal.
     * If the server returns an error without any body, a relevant {@link java.io.IOException} will be thrown.
     *
     * @param url URL to submit the GET request to
     * @return Raw text response from the server
     * @throws IOException The request was not successful
     */
    public static String performGetRequest(final URL url) throws IOException {
        Validate.notNull(url);
        final HttpURLConnection connection = createUrlConnection(url);

        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
            final String result = IOUtils.toString(inputStream, Charsets.UTF_8);
            return result;
        } catch (final IOException e) {
            IOUtils.closeQuietly(inputStream);
            inputStream = connection.getErrorStream();

            if (inputStream != null) {
                final String result = IOUtils.toString(inputStream, Charsets.UTF_8);
                return result;
            } else {
                throw e;
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Creates a {@link URL} with the specified string, throwing an {@link java.lang.Error} if the URL was malformed.
     * <p/>
     * This is just a wrapper to allow URLs to be created in constants, where you know the URL is valid.
     *
     * @param url URL to construct
     * @return URL constructed
     */
    public static URL constantURL(final String url) {
        try {
            return new URL(url);
        } catch (final MalformedURLException ex) {
            throw new Error("Couldn't create constant for " + url, ex);
        }
    }

    /**
     * Turns the specified Map into an encoded & escaped query
     *
     * @param query Map to convert into a text based query
     * @return Resulting query.
     */
    public static String buildQuery(final Map<String, Object> query) {
        if (query == null) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();

        for (final Map.Entry<String, Object> entry : query.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }

            try {
                builder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            } catch (final UnsupportedEncodingException e) {
            }

            if (entry.getValue() != null) {
                builder.append('=');
                try {
                    builder.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
                } catch (final UnsupportedEncodingException e) {
                }
            }
        }

        return builder.toString();
    }

    /**
     * Concatenates the given {@link java.net.URL} and query.
     *
     * @param url   URL to base off
     * @param query Query to append to URL
     * @return URL constructed
     */
    public static URL concatenateURL(final URL url, final String query) {
        try {
            if (url.getQuery() != null && url.getQuery().length() > 0) {
                return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "&" + query);
            } else {
                return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "?" + query);
            }
        } catch (final MalformedURLException ex) {
            throw new IllegalArgumentException("Could not concatenate given URL with GET arguments!", ex);
        }
    }

    public static class HTTPRequest {
        public String data;
        public int statusCode;

        public HTTPRequest(String a, int b) {
            this.data = a;
            this.statusCode = b;
        }
    }

    /**
     * Downloads an image
     *
     * @param address
     * @return
     */
    public static byte[] downloadImage(String address) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = null;
        URL url = null;
        try {
            url = new URL(address);
            is = url.openStream();
            byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
            int n;

            while ((n = is.read(byteChunk)) > 0) {
                baos.write(byteChunk, 0, n);
            }
        } catch (IOException e) {
            System.err.printf("Failed while reading bytes from %s: %s", url.toExternalForm(), e.getMessage());
            e.printStackTrace();
            // Perform any other exception handling that's appropriate.
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {

            }
        }

        return baos.toByteArray();
    }
}