package com.dron.app.vrtulnicek

import android.view.View
import android.widget.Button
import android.widget.ImageButton
import com.dron.app.R
import com.dron.app.example.common.DJISampleApplication
import com.dron.app.vrtulnicek.utils.toast
import com.yoavst.kotlin.`KotlinPackage$Tasks$65a6a183`.mainThread
import kotlinx.android.synthetic.main.activity_vrtulnicek.*
import kotlinx.coroutines.experimental.CommonPool

/**
 * Singleton is managing various special functions in smart menu
 * these functions are
 *  - Follow Selfie
 *  - Reset Gimbal
 *  - AutoLand
 *  - AutoTakeOff
 */
object SmartFunctionsMgr {

    lateinit var activity: VrtulnicekActivity
    lateinit var resetGimbalButton: Button
    lateinit var autoTakeoffButt: ImageButton
    lateinit var autoLandButt: ImageButton

    fun initialize(_activity: VrtulnicekActivity) {
        activity = _activity

        resetGimbalButton = activity.smart_functions_popup?.findViewById(R.id.reset_gimbal) as Button
        resetGimbalButton.setOnClickListener { resetGimbal() }

        autoTakeoffButt = activity.smart_functions_popup?.findViewById(R.id.autoTakeoff) as ImageButton
        autoTakeoffButt.setOnClickListener(SmartFunctionsMgr::autoTakeOff)

        autoLandButt = activity.smart_functions_popup?.findViewById(R.id.autoLand) as ImageButton
        autoLandButt.setOnClickListener(SmartFunctionsMgr::autoLand)


        // Landing and taking off buttons are swapped when in air
        PositionsMgr.addOnDroneLocationChangeListener { loc, _ ->
            if (loc.altitude > 0F)
            {
                autoLandButt.visibility = View.VISIBLE
                autoTakeoffButt.visibility = View.GONE
            }
            else
            {
                autoLandButt.visibility = View.GONE
                autoTakeoffButt.visibility = View.VISIBLE
            }
        }
    }

    private fun resetGimbal() {
        App.getProductInstance()?.gimbal?.resetGimbal { err ->
            mainThread(CommonPool) {
                if (err != null) activity.toast(err.description.toString())
            }
        }
    }

    private fun autoTakeOff(v: View){
        DJISampleApplication.getAircraftInstance()?.flightController?.takeOff {
            djiError -> mainThread(CommonPool){v.toast("Auto-takeoff: ${djiError?.description ?: "Success"}")}
        }
        activity.cancelTakeOff.text = "Cancel Auto-takeoff"
        activity.cancelTakeOff.bringToFront()
        activity.cancelTakeOff.setOnClickListener {
            DJISampleApplication.getAircraftInstance()?.flightController?.cancelTakeOff {
                djiError -> mainThread(CommonPool){v.toast("Auto-takeoff cancel: ${djiError?.description ?: "Canceled"}")}
            }
        }
    }

    private fun autoLand(v: View){
        DJISampleApplication.getAircraftInstance()?.flightController?.autoLanding {
            djiError -> mainThread(CommonPool){v.toast("Auto-land: ${djiError?.description ?: "Success"}")}
        }
        activity.cancelTakeOff.text = "Cancel Auto-land"
        activity.cancelTakeOff.bringToFront()
        activity.cancelTakeOff.setOnClickListener {
            DJISampleApplication.getAircraftInstance()?.flightController?.cancelAutoLanding {
                djiError -> mainThread(CommonPool){v.toast("Auto-land cancel: ${djiError?.description ?: "Canceled"}")}
            }
        }
    }

}