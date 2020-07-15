package com.dron.app.vrtulnicek

import android.view.View
import android.widget.LinearLayout
import com.dron.app.example.common.AvailabilityCallback
import com.dron.app.vrtulnicek.utils.swapLayoutParams
import com.dron.app.vrtulnicek.utils.toFront
import com.dron.app.vrtulnicek.views.FpvView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Singleton managing swap action between map and videofeed.
 * Positions and size of these two View on screen are swaped,
 * and views contents are updated after that.
 */
object MapSwapper
{
    private lateinit var fpvView: FpvView
    private lateinit var mapView: MapView
    private lateinit var cameraControls: LinearLayout

    private var canSwap: AtomicBoolean = AtomicBoolean(false)
    private val frontStuff = mutableListOf<View>()

    var inMinimapMode: Boolean = true
    private var map: GoogleMap? = null

    // Set map called from MapMgr when map is ready
    fun setMap(m: GoogleMap)
    {
        map = m
        // disable interactivity with marker
        map?.setOnMarkerClickListener { marker ->
            if(marker == MapMgr.droneMarker)
            {
                swapMapAndVideo(!inMinimapMode)
                return@setOnMarkerClickListener true
            }
            return@setOnMarkerClickListener false
        }
    }

    fun initialize(_fpvView: FpvView, _mapView: MapView, _cameraControls: LinearLayout, _frontStuff : MutableList<View>)
    {
        fpvView = _fpvView
        mapView = _mapView
        cameraControls = _cameraControls
        frontStuff.addAll(_frontStuff)

        setVideoFeedAvailabilityCallback()
        setOnClickListeners()

        PositionsMgr.addOnDroneLocationChangeListener{ loc, _ ->
            if (MapSwapper.inMinimapMode)
                MapSwapper.map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 17F))
        }
        canSwap.set(true)
    }

    // Register availability callback to fpvView
    private fun setVideoFeedAvailabilityCallback()
    {
        fpvView.setOnCallbackListener(AvailabilityCallback {
            frontStuff.toFront()
            if (inMinimapMode)
                mapView.bringToFront()
        })
    }

    private fun setOnClickListeners()
    {
        MapMgr.addOnMapClickListener {swapMapAndVideo(false)}

        fpvView.setOnClickListener {
            swapMapAndVideo()
        }
    }


    // If ready, switches position of minimap and fpvView, disable switching for 1sec and move rest of UI back to front
    fun swapMapAndVideo(setToMinimapMode: Boolean = true, forceSwap:Boolean = false)
    {
        if (inMinimapMode == setToMinimapMode || (!canSwap.get() && !forceSwap))
            return

        async(CommonPool)
        {
            delay(1000L)
            canSwap.set(true)
        }

        canSwap.set(false)
        mapView.swapLayoutParams(fpvView)

        inMinimapMode = setToMinimapMode
        cameraControls.visibility = if (inMinimapMode && cameraControls.isEnabled) View.VISIBLE else View.INVISIBLE

        if (setToMinimapMode)
            mapView.bringToFront()
        else
            fpvView.bringToFront()

        if (map != null)
            enableMapControls(map!!, !setToMinimapMode)
        frontStuff.toFront()
        fpvView.adjustAspectRatio()
    }

    fun enableMapControls(map: GoogleMap, enable: Boolean = true) {
        map.uiSettings.setAllGesturesEnabled(enable)
        map.uiSettings.isCompassEnabled = enable
        map.isMyLocationEnabled = enable
    }

    fun <T : View> addFrontStuff(view : T)
    {
        frontStuff.add(view)
    }
    fun <T : View> addFrontStuff(list : List<T>)
    {
        frontStuff.addAll(list)
    }
}