package com.dron.app.vrtulnicek.views

import android.app.Service
import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.TextureView
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.dron.app.R
import com.dron.app.example.common.AvailabilityCallback
import com.dron.app.example.common.DJISampleApplication
import dji.common.product.Model
import dji.sdk.airlink.DJILBAirLink
import dji.sdk.base.DJIBaseProduct
import dji.sdk.camera.DJICamera
import dji.sdk.codec.DJICodecManager
import kotlinx.android.synthetic.main.zz_view_fpv_display.view.*
import java.io.IOException
import java.util.*

/**
 * This class is designed for showing the fpv video feed from the camera or Lightbridge 2.
 * Class is based on BaseFpvView from tutorial, adding extra features like
 * * Camera input when drone is not connected in time app is started
 * * Croping or Transforming input
 * * Notifying listeners when video feed is available (needed since after becoming available TextureView jump to front)
 */
class FpvView : RelativeLayout, TextureView.SurfaceTextureListener
{
    private var mReceivedVideoDataCallback : DJICamera.CameraReceivedVideoDataCallback? = null
    private var mOnReceivedVideoCallback : DJILBAirLink.DJIOnReceivedVideoCallback? = null
    private var mCodecManager : DJICodecManager? = null
    private var mProduct : DJIBaseProduct? = null
    private var mCamera : Camera? = null
    private val availabilityCallbacks = ArrayList<AvailabilityCallback>()
    var cropped = false
    var realWidth = width
    var realHeight = height

    init
    {
        initUI()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    {

        val a = context.obtainStyledAttributes(attrs, R.styleable.FpsView)
        cropped = a.getBoolean(R.styleable.FpsView_cropped, false)
    }

    private fun initUI()
    {
        val layoutInflater = context.getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val content = layoutInflater.inflate(R.layout.zz_view_fpv_display, null, false)
        addView(content, RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT))

        texture_video_previewer_surface.surfaceTextureListener = this

        // This callback is for
        mOnReceivedVideoCallback = DJILBAirLink.DJIOnReceivedVideoCallback {videoBuffer, size ->
            mCodecManager?.sendDataToDecoder(videoBuffer, size)
        }

        mReceivedVideoDataCallback = DJICamera.CameraReceivedVideoDataCallback {videoBuffer, size ->
            mCodecManager?.sendDataToDecoder(videoBuffer, size)
        }

        initSDKCallback()
    }

    private fun initSDKCallback()
    {
        try
        {
            mProduct = DJISampleApplication.getProductInstance()

            if (mProduct?.model != Model.UnknownAircraft)
            {
                mProduct?.camera?.setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallback)

            }
            else
            {
                mProduct?.airLink?.lbAirLink?.setDJIOnReceivedVideoCallback(mOnReceivedVideoCallback)
            }
        }
        catch (exception : Exception)
        {
        }

    }

    override fun onSurfaceTextureAvailable(surface : SurfaceTexture, width : Int, height : Int)
    {

        if (mProduct == null)
        {
            mCamera = Camera.open()
            try
            {
                mCamera?.setPreviewTexture(surface)
                mCamera?.startPreview()
            }
            catch (ioe : IOException)
            {
                // Something bad happened
            }

        }
        else if (mCodecManager == null)
        {
            mCodecManager = DJICodecManager(context, surface, width, height)
        }

        adjustAspectRatio()

        for (callback in availabilityCallbacks)
            callback.onAvailible()
    }

    fun setOnCallbackListener(callback : AvailabilityCallback)
    {
        availabilityCallbacks.add(callback)
    }

    override fun onSurfaceTextureSizeChanged(surface : SurfaceTexture, width : Int, height : Int)
    {
        adjustAspectRatio()
    }

    override fun onSurfaceTextureDestroyed(surface : SurfaceTexture) : Boolean
    {
        if (mProduct == null)
        {
            mCamera?.stopPreview()
            mCamera?.release()
            return true
        }
        else
        {
            mCodecManager?.cleanSurface()
            mCodecManager = null
            return false
        }
    }

    override fun onSurfaceTextureUpdated(surface : SurfaceTexture)
    {
        adjustAspectRatio()
    }


    /**
     * Sets the TextureView transform to preserve the aspect ratio of the video.
     */
    fun adjustAspectRatio()
    {
        val h : Int
        val w : Int

        h = mCodecManager?.videoHeight ?: mCamera?.parameters?.previewSize?.height ?: 0
        w = mCodecManager?.videoWidth ?: mCamera?.parameters?.previewSize?.width ?: 0

        if (h == 0 || w == 0)
            return

        val viewWidth = texture_video_previewer_surface.width
        val viewHeight = texture_video_previewer_surface.height
        val aspectRatio = h.toDouble() / w

        if (viewHeight > (viewWidth * aspectRatio).toInt() || cropped)
        {
            // limited by narrow width; restrict height
            realWidth = viewWidth
            realHeight = (viewWidth * aspectRatio).toInt()
        }
        else
        {
            // limited by short height; restrict width
            realWidth = (viewHeight / aspectRatio).toInt()
            realHeight = viewHeight
        }

        val xoff = (viewWidth - realWidth) / 2
        val yoff = (viewHeight - realHeight) / 2
        val txform = Matrix()
        texture_video_previewer_surface.getTransform(txform)
        txform.setScale(realWidth.toFloat() / viewWidth, realHeight.toFloat() / viewHeight)
        txform.postTranslate(xoff.toFloat(), yoff.toFloat())
        texture_video_previewer_surface.setTransform(txform)
    }
}
