package com.dron.app.example.common;

import android.app.Service;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.dron.app.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dji.common.product.Model;
import dji.sdk.airlink.DJILBAirLink;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.camera.DJICamera;
import dji.sdk.codec.DJICodecManager;

/**
 * This class is designed for showing the fpv video feed from the camera or Lightbridge 2.
 */
public class BaseFpvView extends RelativeLayout implements TextureView.SurfaceTextureListener {

    private TextureView mVideoSurface = null;
    private DJICamera.CameraReceivedVideoDataCallback mReceivedVideoDataCallback = null;
    private DJILBAirLink.DJIOnReceivedVideoCallback mOnReceivedVideoCallback = null;
    private DJICodecManager mCodecManager = null;
    private DJIBaseProduct mProduct = null;
    private Camera mCamera;
    private List<AvailabilityCallback> availabilityCallbacks = new ArrayList<>();

    public BaseFpvView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initUI();
    }

    private void initUI() {
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Service.LAYOUT_INFLATER_SERVICE);

        View content = layoutInflater.inflate(R.layout.zz_view_fpv_display, null, false);
        addView(content, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        Log.v("TAG","Start to test");

        mVideoSurface = (TextureView) findViewById(R.id.texture_video_previewer_surface);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);

            // This callback is for
            mOnReceivedVideoCallback = new DJILBAirLink.DJIOnReceivedVideoCallback() {
                @Override
                public void onResult(byte[] videoBuffer, int size) {
                    if (mCodecManager != null) {
                        mCodecManager.sendDataToDecoder(videoBuffer, size);
                    }
                }
            };

            mReceivedVideoDataCallback = new DJICamera.CameraReceivedVideoDataCallback() {
                @Override
                public void onResult(byte[] videoBuffer, int size) {
                    if (null != mCodecManager) {
                        mCodecManager.sendDataToDecoder(videoBuffer, size);
                    }
                }
            };
        }

        initSDKCallback();
    }

    private void initSDKCallback() {
        try {
            mProduct = DJISampleApplication.getProductInstance();

            if (mProduct.getModel() != Model.UnknownAircraft) {
                mProduct.getCamera().setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallback);

            } else {
                mProduct.getAirLink().getLBAirLink().setDJIOnReceivedVideoCallback(mOnReceivedVideoCallback);
            }
        } catch (Exception exception) {}
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        if (mProduct == null)
        {
            mCamera = Camera.open();
            try
            {
                mCamera.setPreviewTexture(surface);
                mCamera.startPreview();
            }
            catch (IOException ioe)
            {
                // Something bad happened
            }
        }
        else
        {
            if (mCodecManager == null)
            {
                mCodecManager = new DJICodecManager(getContext(), surface, width, height);
            }
        }
        adjustAspectRatio();

        for(AvailabilityCallback callback: availabilityCallbacks)
            callback.onAvailible();
    }

    public void setOnCallbackListener(AvailabilityCallback callback)
    {
        availabilityCallbacks.add(callback);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        adjustAspectRatio();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mProduct == null)
        {
            mCamera.stopPreview();
            mCamera.release();
            return true;
        }
        else
        {
            if (mCodecManager != null)
            {
                mCodecManager.cleanSurface();
                mCodecManager = null;
            }
            return false;
        }
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        adjustAspectRatio();
    }


    /**
     * Sets the TextureView transform to preserve the aspect ratio of the video.
     */
    public void adjustAspectRatio()
    {
        int h,w;
        if (mCodecManager != null)
        {
            h = mCodecManager.getVideoHeight();
            w = mCodecManager.getVideoWidth();
        }
        else
        {
            Camera.Parameters params = mCamera.getParameters();
            h = params.getPreviewSize().height;
            w = params.getPreviewSize().width;
        }
        int viewWidth = mVideoSurface.getWidth();
        int viewHeight = mVideoSurface.getHeight();
        double aspectRatio = (double) h / w;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        Matrix txform = new Matrix();
        mVideoSurface.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff, yoff);
        mVideoSurface.setTransform(txform);

    }
}

