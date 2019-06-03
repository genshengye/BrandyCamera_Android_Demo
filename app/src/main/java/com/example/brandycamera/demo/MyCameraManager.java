package com.example.brandycamera.demo;


import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class MyCameraManager implements TextureView.SurfaceTextureListener
{
    private static final String TAG = "MyCameraManager";


    public interface IOnPreviewFrameCallback
    {
        public void onPreviewFrame(byte[] data, Camera camera);
    }


    private Activity activity;
    private Context appcontext;
    private TextureView textureView;

    private Camera camera;
    private Camera.CameraInfo cameraInfo;
    private int cameraId = 0;

    private int cameraResolutionWidth = 0, cameraResolutionHeight = 0; //分辨率对应的宽高
    private int cameraScreenWidth = 0, cameraScreenHeight = 0;  //适应屏幕后的宽高

    private static int threadInitNumber = 0;
    private String threadName;

    private Handler uiHandler;
    private Handler workerHandler;
    private HandlerThread handlerThread;

    private IOnPreviewFrameCallback previewFrameCb = null;


    public MyCameraManager(Activity activity, TextureView textureView)
    {
        this(activity, textureView, 0, getNextThreadName());
    }

    public MyCameraManager(Activity activity, TextureView textureView, int cameraId)
    {
        this(activity, textureView, cameraId, getNextThreadName());
    }

    public MyCameraManager(Activity activity, TextureView textureView, int cameraId, String threadName)
    {
        this.activity = activity;
        this.appcontext = activity.getApplicationContext();
        this.textureView = textureView;
        this.cameraId = cameraId;
        this.threadName = threadName;
    }

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height)
    {
        Log.d(TAG, "onSurfaceTextureAvailable: ");

        uiHandler = new Handler(Looper.getMainLooper());
        handlerThread = new HandlerThread(threadName);
        handlerThread.start();
        workerHandler = new Handler(handlerThread.getLooper());

        workerHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                ///打开camera
                if (camera == null)
                {
                    camera = Camera.open(cameraId);
                }

                ///设置camera的orientation
                camera.setDisplayOrientation(getNeededOrientationDegree(camera, cameraId));

                ///设置camera参数
                Camera.Parameters parameters = camera.getParameters();
                List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
                for (Camera.Size size : sizes)
                {
                    if (size.width == cameraResolutionWidth && size.height == cameraResolutionHeight)
                    {
                        parameters.setPreviewSize(cameraResolutionWidth, cameraResolutionHeight);
                        Log.d(TAG, "onSurfaceTextureAvailable: set preview size="
                                + cameraResolutionWidth + "x" + cameraResolutionHeight);
                    }
                    else
                    {
                        Log.d(TAG, "onSurfaceTextureAvailable: supported preview size="
                                + size.width + "x" + size.height);
                    }
                }

                Camera.Size selectedSize = parameters.getPreviewSize();
                int[] screenSize = getRatioSizeOnScreen(selectedSize.width, selectedSize.height, width, height);
                cameraScreenWidth = screenSize[0];
                cameraScreenHeight = screenSize[1];
                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        textureView.setLayoutParams(new RelativeLayout.LayoutParams(cameraScreenWidth, cameraScreenHeight));
                    }
                });
                camera.setParameters(parameters);

                Toast.makeText(appcontext, "preview size: " + selectedSize.width + "x" + selectedSize.height
                                + "\n screen size: " + cameraScreenWidth + "x" + cameraScreenHeight,
                        Toast.LENGTH_SHORT).show();


                ///设置camera回调
                camera.setPreviewCallback(new Camera.PreviewCallback()
                {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera)
                    {
                        if (previewFrameCb != null)
                        {
                            previewFrameCb.onPreviewFrame(data, camera);
                        }
                    }
                });

                ///开始preview
                try
                {
                    camera.setPreviewTexture(surface);
                    camera.startPreview();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        Log.d(TAG, "onSurfaceTextureDestroyed: ");
        if (camera != null)
        {
            camera.stopPreview();
            camera.release();
            camera = null;
        }

        boolean ret = handlerThread.quit();
        handlerThread = null;
        workerHandler = uiHandler = null;

        // return false;
        return ret;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {
        Log.d(TAG, "onSurfaceTextureSizeChanged: w" + width + ",h=" + height);
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
        // Log.d(TAG, "onSurfaceTextureUpdated: ");
    }

    /**
     * 设置preview size
     *
     * @param width
     * @param height
     * @return
     */
    public MyCameraManager setPreferedPreviewSize(int width, int height)
    {
        cameraResolutionWidth = width;
        cameraResolutionHeight = height;
        return this;
    }

    /**
     * 返回preview数据的分辨率 和 屏幕显示的尺寸
     * preview size 和 screen size.
     *
     * @return int[4]
     */
    public int[] getPreviewAndScreenSize()
    {
        return new int[]{cameraResolutionWidth, cameraResolutionHeight, cameraScreenWidth, cameraScreenHeight};
    }

    public MyCameraManager setPreviewFrameCallback(IOnPreviewFrameCallback cb)
    {
        previewFrameCb = cb;
        return this;
    }


    private static String getNextThreadName()
    {
        if (threadInitNumber > 999999)
            threadInitNumber = 0;
        return "CameraManageerThread-" + (threadInitNumber++);
    }

    private int getNeededOrientationDegree(Camera camera, int cameraId)
    {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int degrees = getScreenRotation();
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        }
        else
        {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
        // camera.setDisplayOrientation(result);
    }

    private int getScreenRotation()
    {
        int screen_rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (screen_rotation)
        {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        return degrees;
    }


    private int[] getRatioSizeOnScreen(int contentWidth, int contentHeight, int screenWidth, int screenHeight)
    {
        int w, h;

        double contentRatio = ((double) contentWidth) / contentHeight;
        double screenRatio = ((double) screenWidth) / screenHeight;
        if (contentRatio > screenRatio)
        {
            w = screenWidth;
            h = (int) (w / contentRatio);
        }
        else if (contentRatio < screenRatio)
        {
            h = screenHeight;
            w = (int) (h * contentRatio);
        }
        else
        {
            w = screenWidth;
            h = screenHeight;
        }
        int[] result = new int[]{w, h};
        return result;
    }
}
