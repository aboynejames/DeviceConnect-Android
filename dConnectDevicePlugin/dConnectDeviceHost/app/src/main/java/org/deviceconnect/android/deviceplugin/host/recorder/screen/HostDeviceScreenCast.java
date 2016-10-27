/*
 HostDeviceScreenCast.java
 Copyright (c) 2014 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.host.recorder.screen;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;

import org.deviceconnect.android.deviceplugin.host.recorder.HostDeviceCameraRecorder;
import org.deviceconnect.android.deviceplugin.host.recorder.HostDevicePreviewServer;
import org.deviceconnect.android.deviceplugin.host.recorder.util.MixedReplaceMediaServer;
import org.deviceconnect.android.provider.FileManager;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import static android.R.attr.max;

/**
 * Host Device Screen Cast.
 *
 * @author NTT DOCOMO, INC.
 */
@TargetApi(21)
public class HostDeviceScreenCast extends HostDevicePreviewServer implements HostDeviceCameraRecorder {

    static final String RESULT_DATA = "result_data";

    static final String EXTRA_CALLBACK = "callback";

    private static final String ID = "screen";

    private static final String NAME = "AndroidHost Screen";

    private static final String MIME_TYPE = "video/x-mjpeg";

    private static final double DEFAULT_MAX_FPS = 10.0d;

    /** ファイル名に付けるプレフィックス. */
    private static final String FILENAME_PREFIX = "android_screen_";

    /** ファイルの拡張子. */
    private static final String FILE_EXTENSION = ".jpg";

    /** 日付のフォーマット. */
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMdd_kkmmss", Locale.JAPAN);

    /**
     * マイムタイプ一覧を定義.
     */
    private List<String> mMimeTypes = new ArrayList<String>() {
        {
            add(MIME_TYPE);
        }
    };

    private final Context mContext;

    private final int mDisplayDensityDpi;

    private final Object mLockObj = new Object();

    private final Logger mLogger = Logger.getLogger("host.dplugin");

    private MixedReplaceMediaServer mServer;

    private MediaProjectionManager mManager;

    private MediaProjection mMediaProjection;

    private VirtualDisplay mVirtualDisplay;

    private ImageReader mImageReader;

    private FileManager mFileMgr;

    private boolean mIsCasting;

    private Thread mThread;

    private final List<PictureSize> mSupportedPreviewSizes = new ArrayList<>();
    private final List<PictureSize> mSupportedPictureSizes = new ArrayList<>();

    private PictureSize mPreviewSize;
    private PictureSize mPictureSize;

    private long mFrameInterval;

    private double mMaxFps;

    private RecorderState mState;

    public HostDeviceScreenCast(final Context context, final FileManager fileMgr) {
        super(context, 2000);
        mContext = context;
        mFileMgr = fileMgr;
        mManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        PictureSize size = new PictureSize(metrics.widthPixels, metrics.heightPixels);
        mDisplayDensityDpi = metrics.densityDpi;
        initSupportedPreviewSizes(size);

        mState = RecorderState.INACTTIVE;
        mMaxFps = DEFAULT_MAX_FPS;
        setMaxFrameRate(mMaxFps);
    }

    private void initSupportedPreviewSizes(final PictureSize originalSize) {
        final int num = 4;
        final int w = originalSize.getWidth();
        final int h = originalSize.getHeight();
        mSupportedPreviewSizes.clear();
        for (int i = 1; i <= num; i++) {
            float scale = i / ((float) num);
            PictureSize previewSize = new PictureSize((int) (w * scale), (int) (h * scale));
            mSupportedPreviewSizes.add(previewSize);
            mSupportedPictureSizes.add(previewSize);
        }
        mPreviewSize = mSupportedPreviewSizes.get(0);
        mPictureSize = mSupportedPictureSizes.get(num - 1);
    }

    @Override
    public void initialize() {
        // Nothing to do.
    }

    @Override
    public void clean() {
        stopWebServer();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    public RecorderState getState() {
        return mState;
    }

    @Override
    public PictureSize getPictureSize() {
        return mPictureSize;
    }

    @Override
    public void setPictureSize(final PictureSize size) {
        mPictureSize = size;
    }

    @Override
    public PictureSize getPreviewSize() {
        return mPreviewSize;
    }

    @Override
    public synchronized void setPreviewSize(final PictureSize size) {
        mPreviewSize = size;
        if (mIsCasting) {
            restartScreenCast();
        }
    }

    @Override
    public double getMaxFrameRate() {
        return mMaxFps;
    }

    @Override
    public void setMaxFrameRate(double frameRate) {
        mMaxFps = frameRate;
        mFrameInterval =  1000L / max;
    }

    @Override
    public List<PictureSize> getSupportedPreviewSizes() {
        return mSupportedPreviewSizes;
    }

    @Override
    public List<PictureSize> getSupportedPictureSizes() {
        return mSupportedPictureSizes;
    }

    @Override
    public List<String> getSupportedMimeTypes() {
        return mMimeTypes;
    }

    @Override
    public boolean isSupportedPictureSize(int width, int height) {
        return false;
    }

    @Override
    public boolean isSupportedPreviewSize(int width, int height) {
        if (mSupportedPreviewSizes != null) {
            for (PictureSize size : mSupportedPreviewSizes) {
                if (width == size.getWidth() && height == size.getHeight()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void startWebServer(final OnWebServerStartCallback callback) {
        mLogger.info("Starting web server...");
        synchronized (mLockObj) {
            if (mServer == null) {
                mServer = new MixedReplaceMediaServer();
                mServer.setServerName("HostDevicePlugin ScreenCast Server");
                mServer.setContentType(MIME_TYPE);
                final String ip = mServer.start();

                Intent intent = new Intent();
                intent.setClass(mContext, PermissionReceiverActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(EXTRA_CALLBACK, new ResultReceiver(new Handler(Looper.getMainLooper())) {
                    @Override
                    protected void onReceiveResult(final int resultCode, final Bundle resultData) {
                        if (resultCode == Activity.RESULT_OK) {
                            Intent data = resultData.getParcelable(RESULT_DATA);
                            setupMediaProjection(resultCode, data);
                            startScreenCast();
                            callback.onStart(ip);
                        } else {
                            callback.onFail();
                        }
                    }
                });
                mContext.startActivity(intent);
            } else {
                callback.onStart(mServer.getUrl());
            }
        }
        mLogger.info("Started web server.");
    }

    @Override
    public void stopWebServer() {
        mLogger.info("Stopping web server...");
        synchronized (mLockObj) {
            stopScreenCast();
            if (mServer != null) {
                mServer.stop();
                mServer = null;
            }
        }
        mLogger.info("Stopped web server.");
    }

    @Override
    public void takePhoto(final OnCameraEventListener listener) {
        mState = RecorderState.RECORDING;

        Intent intent = new Intent();
        intent.setClass(mContext, PermissionReceiverActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CALLBACK, new ResultReceiver(new Handler(Looper.getMainLooper())) {
            @Override
            protected void onReceiveResult(final int resultCode, final Bundle resultData) {
                if (resultCode == Activity.RESULT_OK) {
                    Intent data = resultData.getParcelable(RESULT_DATA);
                    if (!mIsCasting) {
                        setupMediaProjection(resultCode, data);
                        setupVirtualDisplay(mPictureSize, new VirtualDisplay.Callback() {
                            @Override
                            public void onPaused() {
                                mLogger.info("VirtualDisplay.Callback.onPaused");
                            }

                            @Override
                            public void onResumed() {
                                mLogger.info("VirtualDisplay.Callback.onResumed");
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        takePhotoInternal(listener);
                                        releaseVirtualDisplay();
                                    }
                                }).start();
                            }

                            @Override
                            public void onStopped() {
                                mLogger.info("VirtualDisplay.Callback.onStopped");
                            }
                        });
                    } else {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                takePhotoInternal(listener);
                            }
                        }).start();
                    }
                } else {
                    mState = RecorderState.INACTTIVE;
                    listener.onFailedTakePhoto();
                }
            }
        });
        mContext.startActivity(intent);
    }

    private void takePhotoInternal(final OnCameraEventListener listener) {
        long t = System.currentTimeMillis();
        Bitmap bitmap = getScreenshot();
        while (bitmap == null && (System.currentTimeMillis() - t) < 5000) {
            bitmap = getScreenshot();
        }
        if (bitmap == null) {
            listener.onFailedTakePhoto();
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] media = baos.toByteArray();
        if (media == null) {
            mState = RecorderState.INACTTIVE;
            listener.onFailedTakePhoto();
            return;
        }

        mFileMgr.saveFile(createNewFileName(), media, new FileManager.SaveFileCallback() {
            @Override
            public void onSuccess(@NonNull final String uri) {
                mState = RecorderState.INACTTIVE;
                listener.onTakePhoto(uri, null);
            }

            @Override
            public void onFail(@NonNull final Throwable throwable) {
                mState = RecorderState.INACTTIVE;
                listener.onFailedTakePhoto();
            }
        });
    }

    private String createNewFileName() {
        return FILENAME_PREFIX + mSimpleDateFormat.format(new Date()) + FILE_EXTENSION;
    }

    private void setupMediaProjection(final int resultCode, final Intent data) {
        mMediaProjection = mManager.getMediaProjection(resultCode, data);
    }

    private void setupVirtualDisplay() {
        setupVirtualDisplay(mPreviewSize, new VirtualDisplay.Callback() {
            @Override
            public void onPaused() {
                mLogger.info("VirtualDisplay.Callback.onPaused");
                stopScreenCast();
            }

            @Override
            public void onResumed() {
                mLogger.info("VirtualDisplay.Callback.onResumed");
                if (mIsCasting) {
                    startScreenCast();
                }
            }

            @Override
            public void onStopped() {
                mLogger.info("VirtualDisplay.Callback.onStopped");
            }
        });
    }

    private void setupVirtualDisplay(final PictureSize size, final VirtualDisplay.Callback callback) {
        int w = size.getWidth();
        int h = size.getHeight();

        mImageReader = ImageReader.newInstance(w, h, ImageFormat.RGB_565, 10);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
            "Android Host Screen",
            w,
            h,
            mDisplayDensityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mImageReader.getSurface(), callback, null);
    }

    private void releaseVirtualDisplay() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private void startScreenCast() {
        if (mIsCasting) {
            mLogger.info("MediaProjection is already running.");
            return;
        }
        mIsCasting = true;

        setupVirtualDisplay();
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mLogger.info("Server URL: " + mServer.getUrl());
                sendNotification();
                while (mIsCasting) {
                    try {
                        long start = System.currentTimeMillis();

                        Bitmap bitmap = getScreenshot();
                        if (bitmap == null) {
                            continue;
                        }
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] media = baos.toByteArray();
                        mServer.offerMedia(media);

                        long end = System.currentTimeMillis();
                        long interval = mFrameInterval - (end - start);
                        if (interval > 0) {
                            Thread.sleep(interval);
                        }
                    } catch (Throwable e) {
                        break;
                    }
                }
                hideNotification();
            }
        });
        mThread.start();
    }

    private void stopScreenCast() {
        if (!mIsCasting) {
            return;
        }
        mIsCasting = false;
        if (mThread != null) {
            try {
                mThread.interrupt();
                mThread.join();
            } catch (InterruptedException e) {
                // NOP
            }
            mThread = null;
        }
        releaseVirtualDisplay();
    }

    private void restartScreenCast() {
        stopScreenCast();
        startScreenCast();
    }

    private synchronized Bitmap getScreenshot() {
        Image image = mImageReader.acquireLatestImage();
        if (image == null) {
            return null;
        }
        return decodeToBitmap(image);
    }

    private Bitmap decodeToBitmap(final Image img) {
        Image.Plane[] planes = img.getPlanes();
        if (planes[0].getBuffer() == null) {
            return null;
        }

        int width = img.getWidth();
        int height = img.getHeight();

        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        Bitmap bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride, height,
                Bitmap.Config.RGB_565);
        bitmap.copyPixelsFromBuffer(planes[0].getBuffer());
        img.close();

        return bitmap;
    }
}
