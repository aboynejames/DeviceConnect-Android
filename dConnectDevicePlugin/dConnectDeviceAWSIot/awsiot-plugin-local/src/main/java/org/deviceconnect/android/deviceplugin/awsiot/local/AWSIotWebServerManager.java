package org.deviceconnect.android.deviceplugin.awsiot.local;

import android.content.Context;
import android.util.Log;

import org.deviceconnect.android.deviceplugin.awsiot.p2p.WebServer;
import org.deviceconnect.android.deviceplugin.awsiot.util.AWSIotUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AWSIotWebServerManager {
    private static final boolean DEBUG = true;
    private static final String TAG = "AWS-Local";

    private final List<WebServer> mWebServerList = Collections.synchronizedList(new ArrayList<WebServer>());

    private AWSIotLocalManager mIot;
    private Context mContext;

    public AWSIotWebServerManager(final Context context, final AWSIotLocalManager ctl) {
        mContext = context;
        mIot = ctl;
    }

    public void destroy() {
        synchronized (mWebServerList) {
            for (WebServer server : mWebServerList) {
                server.stop();
            }
        }
        mWebServerList.clear();
    }

    public String createWebServer(final String address, final String path) {
        if (DEBUG) {
            Log.i(TAG, "createWebServer: address=" + address + " path=" + path);
        }

        // TODO WebServerをどのタイミングで止めるか検討
        WebServer webServer = new WebServer(mContext, address) {
            @Override
            public void onNotifySignaling(final String signaling) {
                mIot.publish(AWSIotUtil.createLocalP2P(signaling));
            }
            @Override
            protected void onConnected() {
                if (DEBUG) {
                    Log.i(TAG, "WebServer#onConnected");
                }
            }
            @Override
            protected void onDisconnected() {
                if (DEBUG) {
                    Log.i(TAG, "WebServer#onDisconnected");
                }
                mWebServerList.remove(this);
            }
        };
        webServer.setPath(path);
        String url = webServer.start();
        if (url != null) {
            mWebServerList.add(webServer);
        }

        if (DEBUG) {
            Log.i(TAG, "url=" + url);
        }

        return url;
    }

    public void onReceivedSignaling(final String message) {
        synchronized (mWebServerList) {
            for (WebServer server : mWebServerList) {
                if (server.hasConnectionId(message)) {
                    server.onReceivedSignaling(message);
                }
            }
        }
    }
}
