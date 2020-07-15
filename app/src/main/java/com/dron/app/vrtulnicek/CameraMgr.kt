package com.dron.app.vrtulnicek

import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import com.dron.app.R
import com.dron.app.example.utils.DJIModuleVerificationUtil
import com.dron.app.vrtulnicek.utils.toast
import com.yoavst.kotlin.`KotlinPackage$Tasks$65a6a183`.mainThread
import dji.common.camera.DJICameraSettingsDef
import dji.sdk.camera.DJICamera
import dji.sdk.products.DJIAircraft
import kotlinx.android.synthetic.main.activity_vrtulnicek.*
import kotlinx.coroutines.experimental.CommonPool

/**
 * Singleton provides video and photo capturing capabilities. If check
 * DJIModuleVerificationUtil.isCameraModuleAvailable() fail camera controls are hidden from user interface.
 * This check also fails when device doesnt have sd card inserted.
 */
object CameraMgr {

    lateinit var activity : VrtulnicekActivity
    var camera: DJICamera? = null

    var inPhotoMode: Boolean = true
        set (value) {
            field = value
            if (value) {
                activity.cameraButton.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.camera_button_photo)
                activity.cameraButton.setImageResource(R.drawable.ic_camera_white_48dp)
            } else {
                activity.cameraButton.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.camera_button_video)
                shootingVideoState = shootingVideoState
            }
        }

    var shootingVideoState: Boolean = false
        set (value) {
            field = value
            if (!inPhotoMode) {
                if (value) {
                    activity.cameraButton.setImageResource(R.drawable.ic_stop_white_48dp)
                } else
                    activity.cameraButton.setImageResource(R.drawable.ic_fiber_manual_record_white_48dp)
            }
        }

    fun initialize(_activity: VrtulnicekActivity) {
        activity = _activity
        camera = App.getProductInstance()?.camera
        camera?.setCameraMode(DJICameraSettingsDef.CameraMode.ShootPhoto) {}

        initCameraControls()
    }

    private fun initCameraControls() {
        if (!DJIModuleVerificationUtil.isCameraModuleAvailable()) {
            activity.cameraControls.visibility = View.INVISIBLE
            activity.cameraControls.isEnabled = false
            return
        }


        activity.cameraSwitch.setOnCheckedChangeListener { button, b ->
            changeCameraMode(b)
        }
        activity.cameraButton.setOnClickListener {
            if (inPhotoMode) makePhoto()
            else makeVideo(!shootingVideoState)
        }


    }
    fun makeVideo(startVideo: Boolean) {
        if (startVideo == shootingVideoState) return
        if (startVideo) {
            camera?.startRecordVideo {
                if (it == null) {
                    mainThread(CommonPool) { shootingVideoState = startVideo }
                }
                else {
                    Log.d("camera_buttons", "video_err " + it.description)
                    mainThread(CommonPool) { activity.toast(it.description) }
                }
            }
        } else {
            camera?.stopRecordVideo {
                if (it==null) {
                    mainThread(CommonPool) { shootingVideoState = startVideo }
                }
            else {
                    Log.d("camera_buttons","video_err "+it.description)
                    mainThread(CommonPool) { activity.toast(it.description) }
                }
            }
        }

    }

    fun makePhoto() {
        (App.getProductInstance() as DJIAircraft?)?.camera?.
                startShootPhoto(DJICameraSettingsDef.CameraShootPhotoMode.Single,
                        {
                            if (it == null) {
                                mainThread(CommonPool) { activity.toast("Shooted photo!") }
                            }
                            else {
                                Log.d("camera_buttons", "photo_err " + it.description)
                                mainThread(CommonPool) { activity.toast(it.description) }
                            }
                        }
                )
    }

    fun changeCameraMode(setToPhotoMode: Boolean) {
        inPhotoMode = setToPhotoMode
        if (shootingVideoState && inPhotoMode) {
            makeVideo(false)
        }
        (App.getProductInstance() as DJIAircraft?)?.camera?.setCameraMode(
                if (inPhotoMode) DJICameraSettingsDef.CameraMode.ShootPhoto else
                    DJICameraSettingsDef.CameraMode.RecordVideo
                , {}
        )

        Log.d("camera_buttons", "In photo modde: " + inPhotoMode)

    }
}