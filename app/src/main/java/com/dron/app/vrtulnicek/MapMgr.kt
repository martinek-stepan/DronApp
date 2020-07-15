package com.dron.app.vrtulnicek

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import com.dron.app.R
import com.dron.app.vrtulnicek.MapSwapper.inMinimapMode
import com.dron.app.vrtulnicek.utils.getMarkerIconFromDrawable
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*


/**
 * Singleton for googlemap management. MapMgr shows markers of drone and user.
 * If enable polylines of user and drone trajectory are periodicaly drawed on map.
 * Click events can be listened form outside of singleton by registering listener.
 * Thats used in TapFlyMission
 */
object MapMgr
{
    var map: GoogleMap? = null
    var polyLineDron: Polyline? =null
    var polyLineUser: Polyline? = null
    var polyLineDroneOld: Polyline? = null
    var polyLineUserOld: Polyline? = null
    var polyOptionsDron:PolylineOptions = PolylineOptions()
    var polyOptionsUser:PolylineOptions = PolylineOptions()
    var droneMarker: Marker? = null
    private lateinit var mapView: MapView
    var lastUserLocUpdate = System.currentTimeMillis()/1000
    var lastDroneLocUpdate = System.currentTimeMillis()/1000

    fun initialize(mapView : MapView, context: Context, paddingTop: Int, savedInstanceState: Bundle?){
        this.mapView = mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { m ->
            map = m
            map?.uiSettings?.isZoomControlsEnabled = false
            MapSwapper.enableMapControls(m, false)
            MapsInitializer.initialize(mapView.context)

            val droneMarkerOptions = MarkerOptions()
                    .icon(getMarkerIconFromDrawable(context.getDrawable(R.drawable.ic_drone_map_marker)))
                    .draggable(false)
                    .position(LatLng(0.0, 0.0))
                    .anchor(0.5f, 0.5f)
            droneMarker = m.addMarker(droneMarkerOptions)

            m.setPadding(0, paddingTop, 0, 0)

            map?.setOnMapLongClickListener { loc -> longListeners.forEach {l -> l(loc)}}
            map?.setOnMapClickListener { loc -> listeners.forEach {l -> l(loc)} }
            // Add map to map swapper
            MapSwapper.setMap(m)
        }

        polyOptionsDron.color(Color.RED)
        polyOptionsDron.width(5F)
        polyOptionsDron.visible(true)
        polyLineDron = map?.addPolyline(polyOptionsDron)

        polyOptionsUser.color(Color.BLUE)
        polyOptionsUser.width(5F)
        polyOptionsUser.visible(true)
        polyLineUser = map?.addPolyline(polyOptionsUser)


        PositionsMgr.addOnDroneLocationChangeListener { loc, heading ->
            if (lastDroneLocUpdate<System.currentTimeMillis()/1000) {
                val latLng = LatLng(loc.latitude, loc.longitude)
                polyOptionsDron.add(latLng)
                if (SettingsMgr.dronePath.isChecked) {
                    var temp = map?.addPolyline(polyOptionsDron)
                    polyLineDroneOld?.remove()
                    polyLineDroneOld = polyLineDron
                    polyLineDron = temp
                }

                droneMarker?.position = latLng
                droneMarker?.rotation = heading.toFloat()
                if (inMinimapMode)
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17F))
            }
        }
        PositionsMgr.addOnUserLocationChangeListener { loc ->

            if (lastUserLocUpdate<System.currentTimeMillis()/1000) {
                val latLng = LatLng(loc.latitude,loc.longitude)
                polyOptionsUser.add(latLng)
                if (SettingsMgr.userPath.isChecked) {
                    var temp = map?.addPolyline(polyOptionsUser)
                    polyLineUserOld?.remove()
                    polyLineUserOld = polyLineUser
                    polyLineUser = temp
                }
            }
        }
    }

    fun onResume() =    mapView.onResume()
    fun onDestroy() =   mapView.onDestroy()
    fun onPause() =     mapView.onPause()
    fun onLowMemory() = mapView.onLowMemory()

    // Map click listeners
    val listeners = ArrayList<(LatLng) -> Unit>()
    val longListeners = ArrayList<(LatLng) -> Unit>()

    fun addOnMapClickListener(listener : (LatLng) -> Unit) = listeners.add(listener)
    fun removeOnMapClickListener(listener : (LatLng) -> Unit) = listeners.remove(listener)
    fun addOnMapLongClickListener(listener : (LatLng) -> Unit) = longListeners.add(listener)
    fun removeOnMapLongClickListener(listener : (LatLng) -> Unit) = longListeners.remove(listener)

    // Function to show/hide user path on map
    fun showUserPath(show: Boolean){
        polyLineUser?.remove()
        polyLineUserOld?.remove()
        polyLineUser = null
        polyLineUserOld = null

        if (show)
            polyLineUser = map?.addPolyline(polyOptionsUser)
    }

    // Function to show/hide drone path on map
    fun showDronePath(show: Boolean){
        polyLineDron?.remove()
        polyLineDroneOld?.remove()
        polyLineDron = null
        polyLineDroneOld = null

        if (show)
            polyLineDron = map?.addPolyline(polyOptionsDron)
    }
}