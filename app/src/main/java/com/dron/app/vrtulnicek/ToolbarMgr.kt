package com.dron.app.vrtulnicek

import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.dron.app.R
import com.dron.app.vrtulnicek.missions.MissionsMgr
import com.dron.app.vrtulnicek.views.placeaAndHookViewAsContextPopup
import com.yoavst.kotlin.`KotlinPackage$Tasks$65a6a183`.mainThread
import dji.common.airlink.DJISignalInformation
import dji.common.battery.DJIBatteryCell
import dji.common.error.DJIError
import dji.common.flightcontroller.DJIGPSSignalStatus
import dji.common.util.DJICommonCallbacks
import kotlinx.android.synthetic.main.activity_vrtulnicek.*
import kotlinx.coroutines.experimental.CommonPool

/**
 * This singleton is managing toolbar row on screen
 * with indication icons, smart functions, mode indication and settings
 */
object ToolbarMgr {
    var batteryListener: Int = 0
    lateinit var activity: VrtulnicekActivity

    fun initialize(_activity: VrtulnicekActivity) {
        activity = _activity

        // DJI Callbacks updates icons on top row of screen
        initToolbarUpdateCallbacks()

        // some indicators have popup with detailed information
        initBatteryPopup()
        initControllerSignalPopup()
        initControllerBatteryPopup()
        initVideoSignalPopup()
        initDroneSignalPopup()

        // letter indicator of remote controller mode
        initModeIndicator()

        // settings popup
        activity.settings_popup = placeaAndHookViewAsContextPopup(activity, R.layout.settings_popup,
                activity.rootLayout, activity.settingsButton, R.id.settings_popup_id, activity.toolBarLL, {}, {})
        // smart functions popup
        activity.smart_functions_popup = placeaAndHookViewAsContextPopup(activity, R.layout.smart_functions_popup,
                activity.rootLayout, activity.smartButton, R.id.smart_functions_popup_id, activity.toolBarLL, {}, {})
    }

    private fun initToolbarUpdateCallbacks() {

        // battery of drone
        CallbacksMgr.addBatteryListener{ state -> activity.icon_drone_battery.percentage = state.batteryEnergyRemainingPercent }

        // battery of controller
        CallbacksMgr.addControllerBatteryListener { _, djircBatteryInfo ->
            activity.icon_controller_battery.percentage = djircBatteryInfo.remainingEnergyInPercent
        }

        // data signal strength on controller side
        CallbacksMgr.addControllerSignalListener {antennas ->
            if (antennas.count() == 0) activity.icon_controller_signal.percentage = -1
            activity.icon_controller_signal.percentage = antennas.map(DJISignalInformation::getPercent).average().toInt()
        }

        // data signal strength on drone side
        CallbacksMgr.addLightbridgeModuleSignalListener {antennas ->
            if (antennas.count() == 0) activity.icon_signal_drone.percentage = -1
            activity.icon_signal_drone.percentage = antennas.map(DJISignalInformation::getPercent).average().toInt()
        }

        // video signal strength
        CallbacksMgr.addVideoSignalStrengthListener {signalStrength ->
            activity.icon_signal_video.percentage = signalStrength
        }

        // gps strength and satelite count
        CallbacksMgr.addUpdateSystemStateListener {state ->
            val satCount = state.satelliteCount.toInt()
            val gps = state.gpsSignalStatus

            val signal = when (gps) {
                DJIGPSSignalStatus.Level0 -> 0
                DJIGPSSignalStatus.Level1 -> 20
                DJIGPSSignalStatus.Level2 -> 40
                DJIGPSSignalStatus.Level3 -> 60
                DJIGPSSignalStatus.Level4 -> 80
                DJIGPSSignalStatus.Level5 -> 100
                DJIGPSSignalStatus.None,null -> -1
            }
            activity.icon_signal_gps.percentage = signal
            activity.signal_gps_count.text = satCount.toString()
            activity.signal_gps_count.setTextColor(activity.icon_signal_gps.textColor)
        }
    }


    private fun initModeIndicator() {
        CallbacksMgr.addUpdateSystemStateListener { state ->

            val modeString = state.flightModeString

            // mode string is reduced to first letter then shown in toolbar
            val modeChar = modeString[0]
            activity.mode_indicator.text = modeChar.toString()


            // missions are enabled when in right mode
            when (modeString) {
                "F_GPS","Navi" -> activity.missionsContol.visibility = View.VISIBLE
                else -> {
                    if (activity.missionsContol.visibility == View.VISIBLE) {
                        MissionsMgr.cancelCurrentMission()
                        activity.missionsContol.visibility = View.GONE
                    }

                }
            }

            // when landing or taking off cancel button if shown
            when (modeString) {
                "Landing","TakeOff" -> {
                    activity.cancelTakeOff.visibility = View.VISIBLE
                    activity.cancelTakeOff.isClickable = true
                    activity.cancelTakeOff.bringToFront()
                }
                else -> {
                    activity.cancelTakeOff.visibility = View.INVISIBLE
                    activity.cancelTakeOff.isClickable = false
                }
            }
        }
    }

    private fun initDroneSignalPopup() {
        var droneSignalListener: Int = 0
        activity.drone_signal_popup = placeaAndHookViewAsContextPopup(activity, R.layout.controller_signal_popup, //stejnej layout
                activity.rootLayout, activity.icon_signal_drone, R.id.drone_signal_popup_id, activity.toolBarLL, {
            droneSignalListener = CallbacksMgr.addLightbridgeModuleSignalListener { antennas ->
                val tl = activity.drone_signal_popup?.findViewById(R.id.antennas_table_layout) as TableLayout
                if (antennas.count() == 0) {
                    (activity.drone_signal_popup?.findViewById(R.id.no_antennas) as TextView).visibility = View.VISIBLE
                    (activity.drone_signal_popup?.findViewById(R.id.no_antennas) as TextView).layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                    (activity.drone_signal_popup?.findViewById(R.id.no_antennas) as TextView).requestLayout()
                    return@addLightbridgeModuleSignalListener
                }
                tl.removeAllViews()
                for (i in antennas.indices) {
                    val row = LayoutInflater.from(activity).inflate(R.layout.controller_signal_popup_row, null) as TableRow
                    (row.findViewById(R.id.antenna_name) as TextView).text = "Antenna $i: "
                    (row.findViewById(R.id.antenna_percent) as TextView).text = antennas[i].percent.toString() + "%"
                    (row.findViewById(R.id.antenna_power) as TextView).text = antennas[i].power.toString() + "dBm"
                    tl.addView(row)
                }
                tl.requestLayout()
            }
        }, { CallbacksMgr.removeLightbridgeModuleSignalListener(droneSignalListener) }
        )
    }

    private fun initVideoSignalPopup() {
        var videoSignalListener: Int = 0
        activity.video_signal_popup = placeaAndHookViewAsContextPopup(activity, R.layout.video_signal_popup,
                activity.rootLayout, activity.icon_signal_video, R.id.video_signal_popup_id, activity.toolBarLL,
                {
                    videoSignalListener = CallbacksMgr.addVideoSignalStrengthListener { signalStrength ->
                        (activity.video_signal_popup?.findViewById(R.id.signal_strength) as TextView).text = "$signalStrength"
                    }
                },
                {
                    CallbacksMgr.removeVideoSignalStrengthListener(videoSignalListener)
                }
        )


    }

    private fun initControllerBatteryPopup() {
        var controllerBatteryListener: Int = 0
        activity.controller_battery_popup = placeaAndHookViewAsContextPopup(activity, R.layout.controller_battery_popup,
                activity.rootLayout, activity.icon_controller_battery, R.id.controller_battery_popup_id, activity.toolBarLL,
                {
                    controllerBatteryListener = CallbacksMgr.addControllerBatteryListener { _, djircBatteryInfo ->
                        val mah = djircBatteryInfo.remainingEnergyInMAh
                        val percent = djircBatteryInfo.remainingEnergyInPercent
                        val full = if (percent!=0) mah / percent * 100 else null
                        (activity.controller_battery_popup?.findViewById(R.id.remaining_mah) as TextView).text = "${mah}mAh"
                        (activity.controller_battery_popup?.findViewById(R.id.remaining_percent) as TextView).text = "$percent%"
                        (activity.controller_battery_popup?.findViewById(R.id.full_mah) as TextView).text = if (full==null) "--" else "" + full +"mAh"
                    }
                },
                {
                    CallbacksMgr.removeControllerBatteryListener(controllerBatteryListener)
                }
        )

    }

    private fun initControllerSignalPopup() {
        var controllerSignalListener: Int = 0
        activity.controller_signal_popup = placeaAndHookViewAsContextPopup(activity, R.layout.controller_signal_popup,
                activity.rootLayout, activity.icon_controller_signal, R.id.controller_signal_popup_id, activity.toolBarLL, {
            controllerSignalListener = CallbacksMgr.addControllerSignalListener { antennas ->
                val tl = activity.controller_signal_popup?.findViewById(R.id.antennas_table_layout) as TableLayout
                if (antennas.count() == 0) {
                    (activity.controller_signal_popup?.findViewById(R.id.no_antennas) as TextView).visibility = View.VISIBLE
                    (activity.controller_signal_popup?.findViewById(R.id.no_antennas) as TextView).layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                    (activity.controller_signal_popup?.findViewById(R.id.no_antennas) as TextView).requestLayout()
                    return@addControllerSignalListener
                }
                tl.removeAllViews()
                for (i in antennas.indices) {
                    val row = LayoutInflater.from(activity).inflate(R.layout.controller_signal_popup_row, null) as TableRow
                    (row.findViewById(R.id.antenna_name) as TextView).text = "Antenna $i: "
                    (row.findViewById(R.id.antenna_percent) as TextView).text = antennas[i].percent.toString() + "%"
                    (row.findViewById(R.id.antenna_power) as TextView).text = antennas[i].power.toString() + "dBm"
                    tl.addView(row)
                }
                tl.requestLayout()
            }
        }, { CallbacksMgr.removeControllerSignalListener(controllerSignalListener) }
        )
    }

    private fun initBatteryPopup() {
        activity.battery_popup = placeaAndHookViewAsContextPopup(activity, R.layout.battery_popup, activity.rootLayout, activity.icon_drone_battery, R.id.battery_popup_id, activity.toolBarLL, {
            val tl = activity.battery_popup?.findViewById(R.id.cells_table_layout) as TableLayout
            batteryListener = CallbacksMgr.addBatteryListener { state ->
                (activity.battery_popup?.findViewById(R.id.temperature) as TextView).text = "" + state.batteryTemperature + " °C"
                (activity.battery_popup?.findViewById(R.id.times_charged) as TextView).text = state.numberOfDischarge.toString()
                (activity.battery_popup?.findViewById(R.id.voltage) as TextView).text = "" + state.currentVoltage + "mV"
                (activity.battery_popup?.findViewById(R.id.current) as TextView).text = "" + state.currentCurrent + "mA"
                (activity.battery_popup?.findViewById(R.id.energy) as TextView).text = "" + state.currentEnergy + "mAh"
                (activity.battery_popup?.findViewById(R.id.full_energy) as TextView).text = "" + state.fullChargeEnergy + "mAh"

                App.getProductInstance()?.battery?.getCellVoltages(
                        object : DJICommonCallbacks.DJICompletionCallbackWith<Array<DJIBatteryCell>> {
                            override fun onFailure(p0: DJIError?) {}

                            override fun onSuccess(cells: Array<DJIBatteryCell>?) {
                                mainThread(CommonPool) {
                                    tl.removeAllViews()

                                    for (i in cells!!.indices) {
                                        val row = LayoutInflater.from(activity).inflate(R.layout.battery_popup_row, null) as TableRow
                                        (row.findViewById(R.id.cell_name) as TextView).text = "Cell " + i + ": "
                                        (row.findViewById(R.id.cell_current) as TextView).text = "" + cells[i].voltage + "mV"
                                        tl.addView(row)
                                    }
                                    tl.requestLayout()
                                }
                            }

                        }
                )


            }
            if (App.getProductInstance()?.battery?.isSmartBattery ?: false) {
                (activity.battery_popup?.findViewById(R.id.smart_battery) as TextView).text = "YES"
                (activity.battery_popup?.findViewById(R.id.smart_battery) as TextView).setTextColor(ContextCompat.getColor(activity, R.color.green))
            } else {
                (activity.battery_popup?.findViewById(R.id.smart_battery) as TextView).text = "NO"
                (activity.battery_popup?.findViewById(R.id.smart_battery) as TextView).setTextColor(ContextCompat.getColor(activity, R.color.red))
            }


        }, { CallbacksMgr.removeBatteryListener(batteryListener) }
        )
    }


}