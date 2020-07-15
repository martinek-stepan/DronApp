package com.dron.app.vrtulnicek

import android.location.Location
import android.util.Log
import android.widget.ToggleButton
import dji.common.flightcontroller.DJILocationCoordinate3D
import dji.common.gimbal.DJIGimbalAngleRotation
import dji.common.gimbal.DJIGimbalRotateAngleMode
import dji.common.gimbal.DJIGimbalRotateDirection
import dji.sdk.gimbal.DJIGimbal
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Singleton is managing selfie function and rotating
 * gimbal towards user when function is active, this is done by tracking user and drone
 * position and altitude
 */

object SelfieMgr
{
    private var gimball: DJIGimbal? = null
    private lateinit var selfieToggleButton: ToggleButton

    private val updateSelfieReady = AtomicBoolean(true)

    private var userPosition: Location? = null
    private var dronePosition: DJILocationCoordinate3D? = null
    private var droneHeading: Double? = null
    private var droneTakeOffAltitude: Double = 0.0
    private var useShittyAltitudeBasedOnPhoneShittyGPS:Boolean = false


    //radius of Earth
    val rEarth = 6_367_442.5F

    fun initialize(selfieToggle: ToggleButton) {
        selfieToggleButton = selfieToggle
        gimball = App.getProductInstance()?.gimbal


        PositionsMgr.addOnDroneLocationChangeListener { pos, head ->
            dronePosition = pos
            droneHeading = head
            updateSelfieFunction()
        }

        PositionsMgr.addOnUserLocationChangeListener { loc ->
            userPosition = loc
            updateSelfieFunction()
        }
    }

    private fun alignDroneTakeOffAltitude(userPosition: Location){
        droneTakeOffAltitude = userPosition.altitude
    }

    private fun updateSelfieFunction() {
        if ( userPosition == null || dronePosition == null)
            return

        if (userPosition!!.hasAltitude()&& droneTakeOffAltitude==0.0){
            droneTakeOffAltitude = userPosition!!.altitude
        }
        if (!updateSelfieReady.get() || !selfieToggleButton.isChecked )
            return
        //Log.d("updateSelfieFunction","after check")




        val pole = doMath(userPosition!!, dronePosition!!, droneHeading!!, droneTakeOffAltitude)



        val pitch: DJIGimbalAngleRotation = DJIGimbalAngleRotation(true,pole[0] , DJIGimbalRotateDirection.Clockwise)
        val roll: DJIGimbalAngleRotation = DJIGimbalAngleRotation(false, pole[1], DJIGimbalRotateDirection.Clockwise)
        //val yaw: DJIGimbalAngleRotation = DJIGimbalAngleRotation(true, Math.max(Math.min(170F, (smb - droneHeading!!).toFloat()), -170F), DJIGimbalRotateDirection.Clockwise)
        val yaw: DJIGimbalAngleRotation = DJIGimbalAngleRotation(true, pole[2], DJIGimbalRotateDirection.Clockwise)

        updateSelfieReady.set(false)
        gimball?.completionTimeForControlAngleAction = 1.0
        gimball?.rotateGimbalByAngle(DJIGimbalRotateAngleMode.AbsoluteAngle, pitch, roll, yaw, {
            async(CommonPool){
                delay(500L)
                updateSelfieReady.set(true)
                //Log.d("updateSelfieFunction","reloaded delay")
            }
        })
    }
    private fun doMath(userPosition:Location, dronePosition:DJILocationCoordinate3D, droneHeading:Double, droneTakeOffAltitude:Double): FloatArray {

        val dAlt:Double
        if ((droneTakeOffAltitude!=0.0)&& useShittyAltitudeBasedOnPhoneShittyGPS){
            dAlt = (dronePosition.altitude+droneTakeOffAltitude)-userPosition.altitude
        }else{
            dAlt = dronePosition.altitude.toDouble()
        }
        val dLat = userPosition.latitude - dronePosition.latitude
        val dLng = Math.cos(Math.PI / 180F * dronePosition.latitude) * (userPosition.longitude - dronePosition.longitude)
        val sm = Math.toDegrees(Math.atan2(dLng,dLat))

        val dY = dLat * Math.PI * rEarth/180F
        val dX = dLng * Math.PI * rEarth/180F
        val distance = Math.sqrt(dX*dX + dY*dY + dAlt*dAlt)
        val pitch = -Math.toDegrees(Math.asin(dAlt / distance)).toFloat()
        val pom = (sm - droneHeading).toFloat()
        val yaw:Float
        if(pom>180F){
            yaw = -180F+(pom-180F)
        }else{
            yaw = pom
        }

        /*Log.d("doMath2","pitch: "+pitch+" dAlt: "+dAlt+" distance: "+distance+" userAlt: "+userPosition.altitude+" droneTakeOffAlt: "+droneTakeOffAltitude+
        " droneAltitude: "+dronePosition.altitude)*/
        return floatArrayOf(pitch, 0F, yaw)
    }
}