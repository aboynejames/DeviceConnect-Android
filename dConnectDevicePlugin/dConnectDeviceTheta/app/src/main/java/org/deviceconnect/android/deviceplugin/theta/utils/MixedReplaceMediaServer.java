/*
 MixedReplaceMediaServer.java
 Copyright (c) 2015 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.theta.utils;

import android.net.Uri;

import org.deviceconnect.android.deviceplugin.theta.BuildConfig;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Mixed Replace Media Server.
 *
 * @author NTT DOCOMO, INC.
 */
public class MixedReplaceMediaServer {

    /**
     * Logger.
     */
    private Logger mLogger = Logger.getLogger("theta.dplugin");

    /**
     * Max value of cache of media.
     */
    private static final int MAX_MEDIA_CACHE = 2;

    /**
     * Max value of client.
     */
    private static final int MAX_CLIENT_SIZE = 8;

    /**
     * Port of the Socket.
     */
    private int mPort = -1;

    /**
     * The boundary of a multipart.
     */
    private String mBoundary = UUID.randomUUID().toString();

    /**
     * Content type.
     * Default is "image/jpg".
     */
    private String mContentType = "image/jpeg";

    /**
     * Stop flag.
     */
    private boolean mIsServerStopped;

    /**
     * Name of web server.
     */
    private String mServerName = "DevicePlugin Server";

    /**
     * Server Socket.
     */
    private ServerSocket mServerSocket;

    /**
     * Manage a thread.
     */
    private final ExecutorService mExecutor = Executors.newFixedThreadPool(MAX_CLIENT_SIZE);

    /**
     * List a Server Runnable.
     */
    private final List<ServerRunnable> mRunnables = Collections.synchronizedList(new ArrayList<ServerRunnable>());

    /**
     * Server Event Listener.
     */
    private ServerEventListener mServerEventListener;

    /**
     * Set a boundary.
     *
     * @param boundary boundary of a multipart
     */
    public void setBoundary(final String boundary) {
        if (boundary == null) {
            throw new IllegalArgumentException("boundary is null.");
        }
        if (boundary.isEmpty()) {
            throw new IllegalArgumentException("boundary is empty.");
        }
        mBoundary = boundary;
    }

    /**
     * Get a boundary.
     *
     * @return boundary
     */
    public String getBoundary() {
        return mBoundary;
    }

    /**
     * Set a content type.
     * <p>
     * Default is "image/jpg".
     * </p>
     *
     * @param contentType content type
     */
    public void setContentType(final String contentType) {
        mContentType = contentType;
    }

    /**
     * Get a content type.
     *
     * @return content type
     */
    public String getContentType() {
        return mContentType;
    }

    /**
     * Set a port of web server.
     *
     * @param port port of a web server
     */
    public void setPort(final int port) {
        if (port < 1000) {
            throw new IllegalArgumentException("Port is smaller than 1000.");
        }
        mPort = port;
    }

    /**
     * Get a port of web server.
     *
     * @return port
     */
    public int getPort() {
        return mPort;
    }

    /**
     * Set a name of server.
     *
     * @param name name of server
     */
    public void setServerName(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null.");
        }
        mServerName = name;
    }

    /**
     * Get a name of server.
     *
     * @return name of server
     */
    public String getServerName() {
        return mServerName;
    }

    /**
     * Get a url of server.
     *
     * @return url
     */
    public String getUrl() {
        if (mServerSocket == null) {
            return null;
        }
        return "http://localhost:" + mServerSocket.getLocalPort();
    }

    /**
     * Get a server running status.
     *
     * @return server status
     */
    public synchronized boolean isRunning() {
        return !mIsServerStopped;
    }

    /**
     * Inserts the media data into queue.
     *
     * @param media media data
     */
    public void offerMedia(final String segment, final byte[] media) {
        if (media == null) {
            return;
        }
        if (!mIsServerStopped) {
            synchronized (mRunnables) {
                for (ServerRunnable run : mRunnables) {
                    run.offerMedia(segment, media);
                }
            }
        }
    }

    public void stopMedia(final String segment) {
        synchronized (mRunnables) {
            for (ServerRunnable run : mRunnables) {
                run.stopMedia(segment);
            }
        }
    }

    /**
     * Start a mixed replace media server.
     * <p>
     * If a port is not set, looking for a port that is not used between 9000 to 10000, set to server.
     * </p>
     *
     * @return the local IP address of this server or {@code null} if this server cannot start.
     */
    public synchronized String start() {
        try {
            mServerSocket = openServerSocket();
            mLogger.fine("Open a server socket.");
        } catch (IOException e) {
            // Failed to open server socket
            mIsServerStopped = true;
            return null;
        }

        mIsServerStopped = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!mIsServerStopped) {
                        ServerRunnable run = new ServerRunnable(mServerSocket.accept());
                        synchronized (MixedReplaceMediaServer.this) {
                            mExecutor.execute(run);
                        }
                    }
                } catch (IOException e) {
                    mLogger.warning("Error server socket[" + mServerName + "]");
                } finally {
                    stop();
                }
            }
        }, "MotionJPEG Server Thread").start();
        return getUrl();
    }

    /**
     * Open a server socket that looking for a port that can be used.
     *
     * @return ServerSocket
     * @throws java.io.IOException if an error occurs while open socket.
     */
    private ServerSocket openServerSocket() throws IOException {
        if (mPort != -1) {
            return new ServerSocket(mPort);
        } else {
            for (int i = 9000; i <= 10000; i++) {
                try {
                    return new ServerSocket(i);
                } catch (IOException e) {
                    continue;
                }
            }
            throw new IOException("Cannot open server socket.");
        }
    }

    /**
     * Stop a mixed replace media server.
     */
    public synchronized void stop() {
        if (mIsServerStopped) {
            return;
        }
        mIsServerStopped = true;
        mExecutor.shutdown();
        synchronized (mRunnables) {
            for (ServerRunnable run : mRunnables) {
                run.stopAllMedia();
            }
        }
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
                mServerSocket = null;
            } catch (IOException e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
        if (mServerEventListener != null) {
            mServerEventListener.onCloseServer();
        }
        mLogger.fine("MixedReplaceMediaServer is stop.");
    }

    /**
     * Class of Server.
     */
    private class ServerRunnable implements Runnable {
        /**
         * Defined buffer size.
         */
        private static final int BUF_SIZE = 8192;

        /**
         * Socket.
         */
        private final Socket mSocket;

        /**
         * Stream for writing.
         */
        private OutputStream mStream;

        /**
         * Request.
         */
        private Request mRequest;

        /**
         * Queues that holds the media.
         */
        private final Map<String, BlockingQueue<byte[]>> mMediaQueues =
            new HashMap<String, BlockingQueue<byte[]>>();

        /**
         * Whether media delivery is stopped or not.
         */
        private boolean mIsMediaStopped;

        /**
         * Constructor.
         *
         * @param socket socket
         */
        public ServerRunnable(final Socket socket) {
            mSocket = socket;
        }

        @Override
        public void run() {
            mLogger.fine("accept client.");
            mRunnables.add(this);
            try {
                mStream = mSocket.getOutputStream();

                byte[] buf = new byte[BUF_SIZE];
                InputStream in = mSocket.getInputStream();
                int len = in.read(buf, 0, BUF_SIZE);
                if (len == -1) {
                    // error
                    return;
                }
                HttpHeader header = decodeHeader(buf, len);
                mRequest = new Request(header);

                if (mRunnables.size() > MAX_CLIENT_SIZE) {
                    mStream.write(generateServiceUnavailable().getBytes());
                    mStream.flush();
                    return;
                }

                String segment = Uri.parse(mRequest.getUri()).getLastPathSegment();
                boolean isGet = header.hasParam("snapshot");

                byte[] jpeg = null;
                if (mServerEventListener != null) {
                    jpeg = mServerEventListener.onConnect(mRequest);
                }

                if (isGet) {
                    if (jpeg == null) {
                        BlockingQueue<byte[]> mediaQueue = mMediaQueues.get(segment);
                        if (mediaQueue != null) {
                            jpeg = mediaQueue.take();
                        } else {
                            jpeg = new byte[0];
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("HTTP/1.0 200 OK\r\n");
                    sb.append("Server: " + mServerName + "\r\n");
                    sb.append("Connection: close\r\n");
                    sb.append("Content-Type: image/jpeg\r\n");
                    sb.append("Content-Length: " + jpeg.length + "\r\n");
                    sb.append("\r\n");
                    mStream.write(sb.toString().getBytes());
                    mStream.flush();
                    mStream.write(jpeg);
                    mStream.flush();
                    return;
                }

                mStream.write(generateHttpHeader().getBytes());
                mStream.flush();

                while (!mIsServerStopped && !mIsMediaStopped) {
                    BlockingQueue<byte[]> mediaQueue = mMediaQueues.get(segment);
                    if (mediaQueue != null) {
                        byte[] media = mediaQueue.take();
                        if (media.length > 0) {
                            sendMedia(media);
                        }
                    }
                }
            } catch (InterruptedException e) {
                if (mStream != null) {
                    try {
                        mStream.write(generateInternalServerError().getBytes());
                        mStream.flush();
                    } catch (IOException e1) {
                        mLogger.warning("Error server socket[" + mServerName + "]");
                    }
                }
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                if (mStream != null) {
                    try {
                        mStream.write(generateBadRequest().getBytes());
                        mStream.flush();
                    } catch (IOException e1) {
                        mLogger.warning("Error server socket[" + mServerName + "]");
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                mLogger.fine("socket close.");
                if (mServerEventListener != null && mRequest != null) {
                    mServerEventListener.onDisconnect(mRequest);
                }
                if (mStream != null) {
                    try {
                        mStream.close();
                    } catch (IOException e) {
                        if (BuildConfig.DEBUG) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    mSocket.close();
                } catch (IOException e) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace();
                    }
                }
                mRunnables.remove(this);
            }
        }

        /**
         * Inserts the media data into queue.
         *
         * @param segment the segment of URI of media
         * @param media   the media to add
         * @return true if the media data was added to this queue, else false
         */
        private boolean offerMedia(final String segment, final byte[] media) {
            BlockingQueue<byte[]> mediaQueue;
            synchronized (mMediaQueues) {
                mediaQueue = mMediaQueues.get(segment);
                if (mediaQueue == null) {
                    mediaQueue = new ArrayBlockingQueue<byte[]>(MAX_MEDIA_CACHE);
                    mMediaQueues.put(segment, mediaQueue);
                }
            }
            if (mediaQueue.size() == MAX_MEDIA_CACHE) {
                mediaQueue.remove();
            }
            return mediaQueue.offer(media);
        }

        /**
         * Send a media data.
         *
         * @param media media data
         * @throws java.io.IOException if an error occurs while sending media data.
         */
        private void sendMedia(final byte[] media) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("--" + mBoundary + "\r\n");
            sb.append("Content-type: " + mContentType + "\r\n");
            sb.append("Content-Length: " + media.length + "\r\n");
            sb.append("\r\n");
            mStream.write(sb.toString().getBytes());
            mStream.write(media);
            mStream.write("\r\n\r\n".getBytes());
            mStream.flush();
        }

        private void stopMedia(final String segment) {
            mIsMediaStopped = true;
            synchronized (mMediaQueues) {
                BlockingQueue<byte[]> mediaQueue = mMediaQueues.remove(segment);
                if (mediaQueue != null) {
                    mediaQueue.offer(new byte[0]);
                }
            }
        }

        private void stopAllMedia() {
            synchronized (mMediaQueues) {
                for (Map.Entry<String, BlockingQueue<byte[]>> entry : mMediaQueues.entrySet()) {
                    entry.getValue().clear();
                }
                mMediaQueues.clear();
            }
        }

        /**
         * Decode a Http header.
         *
         * @param buf buffer of http header
         * @param len buffer size
         * @return HTTP header
         * @throws java.io.IOException if this http header is invalid.
         */
        private HttpHeader decodeHeader(final byte[] buf, final int len) throws IOException {
            HashMap<String, String> pre = new HashMap<String, String>();
            HashMap<String, String> headers = new HashMap<String, String>();
            HashMap<String, String> params = new HashMap<String, String>();
            BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, len)));

            // Read the request line
            String inLine = in.readLine();
            if (inLine == null) {
                throw new IOException("no headers.");
            }

            StringTokenizer st = new StringTokenizer(inLine);
            if (!st.hasMoreTokens()) {
                throw new IOException("Header is invalid format.");
            }

            String method = st.nextToken();
            if (!method.toLowerCase(Locale.getDefault()).equals("get")) {
                throw new IOException("Method is invalid.");
            }
            pre.put("method", method);

            if (!st.hasMoreTokens()) {
                throw new IOException("Header is invalid format.");
            }

            String uri = st.nextToken();

            // Decode parameters from the URI
            int qmi = uri.indexOf('?');
            if (qmi >= 0) {
                decodeParams(uri.substring(qmi + 1), params);
                uri = decodePercent(uri.substring(0, qmi));
            } else {
                decodeParams(null, params);
                uri = decodePercent(uri);
            }
            pre.put("uri", uri);

            // If there's another token, it's protocol version,
            // followed by HTTP headers. Ignore version but parse headers.
            // NOTE: this now forces header names lowercase since they are
            // case insensitive and vary by client.
            if (st.hasMoreTokens()) {
                String line = in.readLine();
                while (line != null && line.trim().length() > 0) {
                    int p = line.indexOf(':');
                    if (p >= 0) {
                        headers.put(line.substring(0, p).trim().toLowerCase(Locale.US),
                            line.substring(p + 1).trim());
                    }
                    line = in.readLine();
                }
            }

            String segment = Uri.parse(uri).getLastPathSegment();
            if (segment == null) {
                throw new IOException("Header is invalid format.");
            }
            return new HttpHeader(uri, params);
        }

        /**
         * Decode of uri param.
         *
         * @param params uri
         * @param p
         */
        private void decodeParams(final String params, final Map<String, String> p) {
            if (params == null) {
                return;
            }

            StringTokenizer st = new StringTokenizer(params, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                if (sep >= 0) {
                    p.put(decodePercent(e.substring(0, sep)).trim(), decodePercent(e.substring(sep + 1)));
                } else {
                    p.put(decodePercent(e).trim(), "");
                }
            }
        }

        /**
         * Decode of uri.
         *
         * @param str uri
         * @return The decoded URI
         */
        private String decodePercent(final String str) {
            try {
                return URLDecoder.decode(str, "UTF8");
            } catch (UnsupportedEncodingException ignored) {
                return null;
            }
        }
    }

    /**
     * Generate a http header.
     *
     * @return http header
     */
    private String generateHttpHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.0 200 OK\r\n");
        sb.append("Server: " + mServerName + "\r\n");
        sb.append("Connection: close\r\n");
        sb.append("Max-Age: 0\r\n");
        sb.append("Expires: 0\r\n");
        sb.append("Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n");
        sb.append("Pragma: no-cache\r\n");
        sb.append("Content-Type: multipart/x-mixed-replace; ");
        sb.append("boundary=" + mBoundary + "\r\n");
        sb.append("\r\n");
        sb.append("--" + mBoundary + "\r\n");
        return sb.toString();
    }

    /**
     * Generate a Bad Request.
     *
     * @return Bad Request
     */
    private String generateBadRequest() {
        return generateErrorHeader("400");
    }

    /**
     * Generate a Not Found.
     *
     * @return Bad Request
     */
    private String generateNotFound() {
        return generateErrorHeader("404");
    }

    /**
     * Generate a Internal Serve rError.
     *
     * @return Internal Server Error
     */
    private String generateInternalServerError() {
        return generateErrorHeader("500");
    }

    /**
     * Generate a Service Unavailable.
     *
     * @return Service Unavailable
     */
    private String generateServiceUnavailable() {
        return generateErrorHeader("503");
    }

    /**
     * Generate a error http header.
     *
     * @param status Status
     * @return http header
     */
    private String generateErrorHeader(final String status) {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.0 " + status + " OK\r\n");
        sb.append("Server: " + mServerName + "\r\n");
        sb.append("Connection: close\r\n");
        sb.append("\r\n");
        return sb.toString();
    }

    public void setServerEventListener(final ServerEventListener listener) {
        mServerEventListener = listener;
    }

    public interface ServerEventListener {

        byte[] onConnect(Request request);

        void onDisconnect(Request request);

        void onCloseServer();

    }

    public class Request {

        private HttpHeader mHeader;

        private Request(final HttpHeader header) {
            mHeader = header;
        }

        public String getUri() {
            return getUrl() + mHeader.getUri();
        }

        public boolean isGet() {
            return mHeader.hasParam("snapshot");
        }

    }

    private static class HttpHeader {

        final String mUri;
        final Map<String, String> mParams;

        HttpHeader(final String uri, final Map<String, String> params) {
            mUri = uri;
            mParams = params;
        }

        String getUri() {
            return mUri;
        }

        String getParam(final String key) {
            return mParams.get(key);
        }

        boolean hasParam(final String key) {
            return getParam(key) != null;
        }
    }
}
