package com.dron.app.vrtulnicek

import android.view.View
import com.yoavst.kotlin.`KotlinPackage$Tasks$65a6a183`.mainThread
import dji.common.flightcontroller.DJIAircraftRemainingBatteryState
import dji.common.flightcontroller.DJIFlightControllerNoFlyStatus
import kotlinx.android.synthetic.main.activity_vrtulnicek.*
import kotlinx.coroutines.experimental.CommonPool

/**
 * Sigleton provides warnings in center of screen.
 */
object FlightWarning {

    lateinit var activity : VrtulnicekActivity

    fun initialize(_activity:VrtulnicekActivity) {
        activity = _activity
        initCallbacks()
    }

    private fun initCallbacks() {

        CallbacksMgr.addUpdateSystemStateListener { state ->
            val noFlyStatus = state.noFlyStatus
            val remainingBattery = state.remainingBattery

            /* warning image is shown when approching NO FLY ZONE,
               according to SDK documentation when 100 meters outside of zone */
            if (noFlyStatus == DJIFlightControllerNoFlyStatus.ApproachingNoFlyZone) {
                activity.no_fly_zone.visibility = View.VISIBLE
            } else {
                activity.no_fly_zone.visibility = View.INVISIBLE
            }

            // battery warning is shown when in low or verylow state
            when (remainingBattery) {
                DJIAircraftRemainingBatteryState.Low, DJIAircraftRemainingBatteryState.VeryLow -> activity.low_battery.visibility = View.VISIBLE
                else -> activity.low_battery.visibility = View.INVISIBLE
            }

        }
    }

}