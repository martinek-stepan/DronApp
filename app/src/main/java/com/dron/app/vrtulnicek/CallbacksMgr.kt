package com.dron.app.vrtulnicek

import com.yoavst.kotlin.`KotlinPackage$Tasks$65a6a183`.mainThread
import dji.common.airlink.DJISignalInformation
import dji.common.battery.DJIBatteryState
import dji.common.flightcontroller.DJIFlightControllerCurrentState
import dji.common.flightcontroller.DJIGPSSignalStatus
import dji.common.remotecontroller.DJIRCBatteryInfo
import dji.sdk.products.DJIAircraft
import dji.sdk.remotecontroller.DJIRemoteController
import kotlinx.coroutines.experimental.CommonPool

/**
 * Sigleton provides service for registering DJI callbacks across app. Listeners can be registered
 * and unregistered. One callback can provide data to various components of app. For example
 * DJIAircraft.flightController.setUpdateSystemStateCallback gives various state information about drone including gps state,
 * battery and zone warnings, location and speed of aircraft and more.
 */

object CallbacksMgr
{
    var uniqueId = 0


    //battery
    private val batteryListeners = HashMap<Int,(DJIBatteryState) -> Unit>()
    fun addBatteryListener(listener: (DJIBatteryState) -> Unit): Int{
        batteryListeners.put(uniqueId,listener)
        return uniqueId++
    }
    fun removeBatteryListener(id: Int) = batteryListeners.remove(id)

    //controller battery
    private val controllerBatteryListeners = HashMap<Int,((DJIRemoteController, DJIRCBatteryInfo) -> Unit)>()
    fun addControllerBatteryListener(listener: (DJIRemoteController, DJIRCBatteryInfo) -> Unit): Int{
        controllerBatteryListeners.put(uniqueId,listener)
        return uniqueId++
    }
    fun removeControllerBatteryListener(id: Int) = controllerBatteryListeners.remove(id)

    //controller signal
    private val controllerSignalListeners = HashMap<Int,(ArrayList<DJISignalInformation>) -> Unit>()
    fun addControllerSignalListener(listener: (ArrayList<DJISignalInformation>) -> Unit): Int{
        controllerSignalListeners.put(uniqueId,listener)
        return uniqueId++
    }
    fun removeControllerSignalListener(id: Int) = controllerSignalListeners.remove(id)

    //lightbridge
    private val lightbridgeModuleSignalListeners = HashMap<Int,(ArrayList<DJISignalInformation>) -> Unit>()
    fun addLightbridgeModuleSignalListener(listener: (ArrayList<DJISignalInformation>) -> Unit): Int{
        lightbridgeModuleSignalListeners.put(uniqueId,listener)
        return uniqueId++
    }
    fun removeLightbridgeModuleSignalListener(id: Int) = lightbridgeModuleSignalListeners.remove(id)

    //video
    private val videoSignalStrengthListeners = HashMap<Int,(Int) -> Unit>()
    fun addVideoSignalStrengthListener(listener: (Int) -> Unit): Int{
        videoSignalStrengthListeners.put(uniqueId,listener)
        return uniqueId++
    }
    fun removeVideoSignalStrengthListener(id: Int) = videoSignalStrengthListeners.remove(id)

    //system update
    private val updateSystemStateListeners = HashMap<Int,(DJIFlightControllerCurrentState) -> Unit>()
    fun addUpdateSystemStateListener(listener: (DJIFlightControllerCurrentState) -> Unit): Int{
        updateSystemStateListeners.put(uniqueId,listener)
        return uniqueId++
    }
    fun removeUpdateSystemStateListener(id: Int) = updateSystemStateListeners.remove(id)



    // Set listeners to DJI callbacks
    fun initilize()
    {
        val battery = (App.getProductInstance() as DJIAircraft?)?.battery
        val airLink = App.getProductInstance()?.airLink
        val lbAirLink = airLink?.lbAirLink
        val remoteController = (App.getProductInstance() as DJIAircraft?)?.remoteController

        //battery
        battery?.setBatteryStateUpdateCallback { state ->
            mainThread(CommonPool) {
                batteryListeners.forEach { (_, listener) -> listener(state) }
            }
        }

        //controller battery
        remoteController?.setBatteryStateUpdateCallback { djiRemoteController, djircBatteryInfo ->
            mainThread(CommonPool) {
                controllerBatteryListeners.forEach { (_, listener) -> listener(djiRemoteController,djircBatteryInfo) }
            }
        }

        //controller signal
        lbAirLink?.setLBAirLinkUpdatedRemoteControllerSignalInformationCallback { antennas ->
            mainThread(CommonPool) {
                controllerSignalListeners.forEach { (_, listener) -> listener(antennas) }
            }
        }

        //lightbridge
        lbAirLink?.setDJILBAirLinkUpdatedLightbridgeModuleSignalInformationCallback { antennas ->
            mainThread(CommonPool) {
                lightbridgeModuleSignalListeners.forEach { (_, listener) -> listener(antennas) }
            }
        }

        //video
        lbAirLink?.setVideoSignalStrengthChangeCallback { signalStrength ->
            mainThread(CommonPool) {
                videoSignalStrengthListeners.forEach { (_, listener) -> listener(signalStrength) }
            }
        }


        // update drone system state
        val flightController = (App.getProductInstance() as DJIAircraft?)?.flightController
        flightController?.setUpdateSystemStateCallback { state ->
            mainThread(CommonPool) {
                //system state update
                updateSystemStateListeners.forEach {(_, listener) -> listener(state)  }

                // dron gps
                if (! state.isHomePointSet || state.gpsSignalStatus == DJIGPSSignalStatus.None)
                    return@mainThread

                PositionsMgr.invokeDroneLocationUpdate(state.aircraftLocation, flightController.compass.heading)
            }
        }
    }
}