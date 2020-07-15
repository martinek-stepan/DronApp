package com.dron.app.vrtulnicek

import android.location.Location
import android.support.v4.content.ContextCompat
import com.dron.app.R
import com.dron.app.vrtulnicek.utils.sqrt
import com.yoavst.kotlin.`KotlinPackage$Tasks$65a6a183`.mainThread

import dji.common.flightcontroller.DJILocationCoordinate3D
import kotlinx.android.synthetic.main.activity_vrtulnicek.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import java.util.*

/**
 * Singleton provides onscreen information about drone coordinates, altitude,
 * speed and distance from user. When no new data is updated for THRESHOLD ms
 * it shows NO FLIGHT DATA
 */
object FlightDataMgr {

    enum class FlightData(val value: Array<String>) {
        LATITUDE(arrayOf("Latitude", "%.5f", "°")),
        LONGITUDE(arrayOf("Longtiude", "%.5f", "°")),
        ALTITUDE(arrayOf("Altitude", "%.3f", "m")),
        SPEED(arrayOf("Speed", "%.3f", "m/s")),
        DISTANCE(arrayOf("Distance", "%.3f", "m"))
    }

    private val THRESHOLD = 3000L // in ms
    private val INTERVAL = 500L

    private var speedUpdateTime: Long = 0L
    private var locUpdateTime: Long = 0L


    val data = TreeMap<FlightData,Float?>()

    var userLocation: Location? = null

    lateinit var activity: VrtulnicekActivity

    fun initialize(_activity: VrtulnicekActivity) {
        activity = _activity

        // user loc updates
        PositionsMgr.addOnUserLocationChangeListener { userLocation = it }
        // drone location updates
        PositionsMgr.addOnDroneLocationChangeListener { djiLocationCoordinate3D, _ ->
            updateAircraftLocation(djiLocationCoordinate3D)
        }
        // speed is read from DJI callbacks
        CallbacksMgr.addUpdateSystemStateListener { state ->
            updateSpeed(state.velocityX, state.velocityY, state.velocityZ)
        }
        backGroundUpdateView()
    }

    // periodic updates of view
    private fun backGroundUpdateView() {
        async(CommonPool) {
            while (true) {
                val currentTime = System.currentTimeMillis()
                if (speedUpdateTime < currentTime - THRESHOLD) {
                    data[FlightData.SPEED] = null
                }

                if (locUpdateTime < currentTime - THRESHOLD) {
                    data[FlightData.LATITUDE] = null
                    data[FlightData.LONGITUDE] = null
                    data[FlightData.ALTITUDE] = null
                    data[FlightData.DISTANCE] = null
                } else {
                    try {
                        data[FlightData.DISTANCE] =
                                distanceBetween(data[FlightData.LATITUDE]!!, userLocation!!.latitude.toFloat(),
                                        data[FlightData.LONGITUDE]!!, userLocation!!.longitude.toFloat(),
                                        data[FlightData.ALTITUDE]!!, 0f)
                    } catch (e: NullPointerException) {
                        data[FlightData.DISTANCE] = null
                    }
                }
                updateView()
                delay(INTERVAL)
            }
        }
    }

    private fun updateView() {

        var no_data = true
        data.map { if (it.value != null) no_data = false }

        if (no_data) {
            mainThread(CommonPool) {
                activity.flight_data_left.setTextColor(ContextCompat.getColor(activity, R.color.red))
                activity.flight_data_left.text = "NO FLIGHT DATA"
                activity.flight_data_right.text = ""
            }

        } else {

            val sbl = StringBuilder()
            val sbr = StringBuilder()

            data.forEach { (k, v) ->
                if (v != null) {
                    sbl.append(k.value[0])
                    sbl.append("\t")
                    sbr.append(k.value[1].format(v))
                    sbr.append(k.value[2])

                    if (k != data.entries.last().key) {
                        sbl.append('\n')
                        sbr.append('\n')
                    }
                }
            }
            mainThread(CommonPool) {
                activity.flight_data_left.setTextColor(ContextCompat.getColor(activity, R.color.abc_primary_text_material_dark))
                activity.flight_data_left.text = sbl.toString()
                activity.flight_data_right.text = sbr.toString()
            }
        }
    }




    private fun updateAircraftLocation(aircraftLocation: DJILocationCoordinate3D?) {
        if (aircraftLocation != null && !aircraftLocation.latitude.isNaN()
                && !aircraftLocation.longitude.isNaN()) {
            data[FlightData.LATITUDE] = aircraftLocation.latitude.toFloat()
            data[FlightData.LONGITUDE] = aircraftLocation.longitude.toFloat()
            data[FlightData.ALTITUDE] = aircraftLocation.altitude
            locUpdateTime = System.currentTimeMillis()
        } else {
            data[FlightData.LATITUDE] = null
            data[FlightData.LONGITUDE] = null
            data[FlightData.ALTITUDE] = null
        }
    }


    private fun updateSpeed(x: Float, y: Float, z: Float) {
        val speed = Float.sqrt(x * x + y * y + z * z)
        data[FlightData.SPEED] = if (speed > 0.01) speed else null
        speedUpdateTime = System.currentTimeMillis()
    }


    fun distanceBetween(lat1: Float, lat2: Float, lng1: Float, lng2: Float, alt1: Float, alt2: Float): Float {
        val dLat = lat1 - lat2
        val dLng = lng1 - lng2
        val dy = dLat * Math.PI * SelfieMgr.rEarth / 180F
        val dx = dLng * Math.PI * SelfieMgr.rEarth / 180F * Math.cos(Math.PI / 180F * lat1)
        val dz = (alt1 - alt2).toDouble()
        val dist = Math.sqrt(dx * dx + dy * dy + dz * dz)
        return dist.toFloat()
    }
}
