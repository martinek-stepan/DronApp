package com.dron.app.vrtulnicek

import android.graphics.Color
import android.location.Location
import android.support.v4.content.ContextCompat
import com.dron.app.R
import com.dron.app.example.common.DJISampleApplication
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.yoavst.kotlin.`KotlinPackage$Tasks$65a6a183`.mainThread
import dji.common.error.DJIError
import dji.common.flightcontroller.DJILocationCoordinate3D
import dji.common.util.DJICommonCallbacks
import kotlinx.android.synthetic.main.activity_vrtulnicek.*
import kotlinx.coroutines.experimental.CommonPool

/**
 * Singleton provides functionality
 */
object GraphMgr {

    val DEFAULT_MAX_HEIGHT = 200.0

    lateinit var graph: GraphView
    lateinit var data : LineGraphSeries<DataPoint>
    lateinit var activity: VrtulnicekActivity

    fun initialize(_activity: VrtulnicekActivity) {
        activity = _activity
        graph = activity.graphAlt
        graph.viewport.isYAxisBoundsManual = true
        graph.viewport.setMinY(0.0)
        graph.viewport.setMaxY(DEFAULT_MAX_HEIGHT)
        graph.viewport.isXAxisBoundsManual = true
        graph.viewport.setMinX(0.0)
        graph.viewport.setMaxX(60.0)
        graph.viewport.isScalable = false
        data = LineGraphSeries()
        val color = ContextCompat.getColor(activity,R.color.button_tint)
        data.backgroundColor = Color.argb(50,Color.red(color),Color.green(color),Color.blue(color))
        data.color = color
        data.isDrawBackground = true
        data.isDrawAsPath = true
        data.setAnimated(true)
        graph.gridLabelRenderer.isVerticalLabelsVisible = true
        graph.gridLabelRenderer.isHumanRounding = true
        graph.gridLabelRenderer.isHorizontalLabelsVisible = false
        graph.addSeries(data)

        // read altitude limitation from drone
        DJISampleApplication.getAircraftInstance()?.flightController?.flightLimitation?.getMaxFlightHeight(object : DJICommonCallbacks.DJICompletionCallbackWith<Float>{
            override fun onFailure(err: DJIError?) {
                //nothing
            }
            override fun onSuccess(height: Float?) {
                mainThread(CommonPool) {
                    graph.viewport.setMaxY(height?.toDouble() ?: DEFAULT_MAX_HEIGHT)
                    graph.viewport.isYAxisBoundsManual = true
                }
            }
        })

        PositionsMgr.addOnDroneLocationChangeListener { loc, _ -> update(loc)}
    }
    fun update(dronePosition : DJILocationCoordinate3D){
        addValue(dronePosition.altitude.toDouble())
    }
    fun update(userPosition: Location){
        addValue(userPosition.altitude)
    }
    fun addValue(value:Double){
        if((System.currentTimeMillis()/1000) != data.highestValueX.toLong()) {
            val dataPoint: DataPoint = DataPoint((System.currentTimeMillis() / 1000).toDouble(), value)
            data.appendData(dataPoint, true, 60)
        }
    }
}