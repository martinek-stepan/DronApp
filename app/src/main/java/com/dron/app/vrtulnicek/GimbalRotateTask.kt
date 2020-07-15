package com.dron.app.vrtulnicek

import android.util.Log
import com.dron.app.example.common.DJISampleApplication
import com.dron.app.example.utils.DJIModuleVerificationUtil
import dji.common.gimbal.DJIGimbalSpeedRotation
import java.util.*

/**
 * For timing gimbal rotation
 * Class is modified version from example gimbal/MoveGimbalWithSpeedView.java
 */
class GimbalRotateTimerTask(var mPitch: DJIGimbalSpeedRotation?, var mRoll: DJIGimbalSpeedRotation?, var mYaw: DJIGimbalSpeedRotation?) : TimerTask() {
    override fun run() {
        if (DJIModuleVerificationUtil.isGimbalModuleAvailable()) {
            DJISampleApplication.getProductInstance()?.gimbal?.rotateGimbalBySpeed(mPitch, mRoll, mYaw
            ) { e -> if (e!=null) Log.d("GimbalRotateTimerTask",e.description)}
        }
    }
}