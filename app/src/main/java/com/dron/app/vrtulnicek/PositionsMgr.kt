package com.dron.app.vrtulnicek

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.yoavst.kotlin.`KotlinPackage$Tasks$65a6a183`.mainThread
import dji.common.flightcontroller.DJILocationCoordinate3D
import kotlinx.coroutines.experimental.CommonPool

/**
 * Singleton for postion callbacks management
 */

object PositionsMgr
{
    private var lm : LocationManager? = null
    private var ll : LocationListener? = null


    var dronePosition: DJILocationCoordinate3D? = null
    var droneHeading: Double? = null
    var userPosition: Location? = null

    val USER_LOC_MIN_UPDATE_TIME = 500L
    val USER_LOC_MIN_UPDATE_DIST = 2F

    private val userLoctionListeners = ArrayList<(Location) -> Unit>()
    private val droneLoctionListeners = ArrayList<(DJILocationCoordinate3D, Double) -> Unit>()

    fun initialize(context: Context)
    {
        lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        ll = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                //Log.d("positionMgr",location.toString())
                mainThread(CommonPool){
                    userPosition = location
                    userLoctionListeners.forEach { listener-> listener(location) }
                }
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String?) {}
            override fun onProviderDisabled(provider: String?) {}
        }
        lm?.requestLocationUpdates(lm?.getBestProvider(Criteria(), false), USER_LOC_MIN_UPDATE_TIME, USER_LOC_MIN_UPDATE_DIST, ll)
    }

    fun onResume() = lm?.requestLocationUpdates(lm?.getBestProvider(Criteria(), false), USER_LOC_MIN_UPDATE_TIME, USER_LOC_MIN_UPDATE_DIST, ll)
    fun onPause() = lm?.removeUpdates(ll)

    fun addOnUserLocationChangeListener(listener : (Location) -> Unit) = userLoctionListeners.add(listener)
    fun addOnDroneLocationChangeListener(listener : (DJILocationCoordinate3D, Double) -> Unit) = droneLoctionListeners.add(listener)

    // Invoke drone location called form CallbackMgr
    fun invokeDroneLocationUpdate(loc: DJILocationCoordinate3D, heading : Double){
        dronePosition = loc
        droneHeading = heading
        droneLoctionListeners.forEach { listener-> listener(loc, heading)}
    }

    fun removeOnUserLocationChangeListener(unit: (Location) -> Unit) {
        userLoctionListeners.remove(unit)
    }

    fun  removeOnDronLocationChangeListener(arg : (DJILocationCoordinate3D, Double) -> Unit)
    {
        droneLoctionListeners.remove(arg)
    }
}