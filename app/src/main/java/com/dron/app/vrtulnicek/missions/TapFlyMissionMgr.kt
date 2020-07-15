package com.dron.app.vrtulnicek.missions
/*
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.dron.app.R
import com.dron.app.example.common.Utils
import com.dron.app.example.common.Utils.setResultToToast
import com.dron.app.vrtulnicek.MapSwapper
import com.dron.app.vrtulnicek.missions.MissionsMgr
import com.dron.app.vrtulnicek.VrtulnicekActivity
import com.google.android.gms.maps.MapView
import com.yoavst.kotlin.`KotlinPackage$Tasks$65a6a183`.mainThread
import dji.common.error.DJIError
import dji.common.util.DJICommonCallbacks
import dji.sdk.missionmanager.DJIMission
import dji.sdk.missionmanager.DJIMissionManager
import dji.sdk.missionmanager.DJITapFlyMission
import kotlinx.android.synthetic.main.activity_vrtulnicek.*
import kotlinx.android.synthetic.main.activity_vrtulnicek.view.*
import kotlinx.coroutines.experimental.CommonPool

/**
 * Created by Kexik on 21.04.2017.
 */
object TapFlyMissionMgr: MissionMgr()
{
    lateinit var assisSw: Switch
    lateinit var stopButt: ImageButton
    lateinit var startButt: Button
    lateinit var tapFlyLayout: RelativeLayout
    lateinit var pointIV: ImageView
    lateinit var speedSeekBar: SeekBar
    lateinit var speedTv: TextView
    lateinit var mapView: MapView

    var mission = DJITapFlyMission()

    override fun initialize(activity : VrtulnicekActivity)
    {
        assisSw = activity.rootLayout.pointing_assistant_sw
        stopButt = activity.rootLayout.pointing_stop_btn
        startButt = activity.rootLayout.pointing_start_btn
        pointIV = activity.rootLayout.pointing_rst_point_iv
        speedSeekBar = activity.rootLayout.pointing_speed_sb
        speedTv = activity.rootLayout.pointing_speed_tv
        tapFlyLayout = activity.rootLayout.pointing_bg_layout
        mapView = activity.mapView

        stopButt.setOnClickListener(TapFlyMissionMgr::onButtonClick)
        startButt.setOnClickListener(TapFlyMissionMgr::onButtonClick)
        tapFlyLayout.setOnTouchListener(TapFlyMissionMgr::onLayoutTouch)
        speedSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar : SeekBar?, progress : Int, fromUser : Boolean)
            {
                speedTv.setText((progress + 1).toString());
            }

            override fun onStartTrackingTouch(seekBar : SeekBar? ){}

            override fun onStopTrackingTouch(seekBar : SeekBar)
            {
                if (missionMgr?.currentExecutingMission != null)
                    DJITapFlyMission.setAutoFlightSpeed(getSpeed(), {error -> Utils.setResultToToast(speedSeekBar.context, if (error == null) "Success" else error.description) })
            }
        });
    }

    private fun onMissionExecutionFinished(error : DJIError?)
    {
        Utils.setResultToToast(tapFlyLayout.context, "Execution finished: " + if (error == null) "Success!" else error.description)
        setVisible(pointIV, false)
        setVisible(stopButt, false)
        setVisible(assisSw, true)
    }


    private fun onMissionProgressStatusUpdate(progressStatus : DJIMission.DJIMissionProgressStatus)
    {
        if (progressStatus !is DJITapFlyMission.DJITapFlyMissionProgressStatus)
            return

        val pointingStatus = progressStatus
        val sb = StringBuffer()
        Utils.addLineToSB(sb, "Flight state", pointingStatus.executionState.name)
        Utils.addLineToSB(sb, "pointing direction X", pointingStatus.direction.x)
        Utils.addLineToSB(sb, "pointing direction Y", pointingStatus.direction.y)
        Utils.addLineToSB(sb, "pointing direction Z", pointingStatus.direction.z)
        Utils.addLineToSB(sb, "point x", pointingStatus.imageLocation.x)
        Utils.addLineToSB(sb, "point y", pointingStatus.imageLocation.y)
        Utils.addLineToSB(sb, "Bypass state", pointingStatus.bypassDirection.name)
        Utils.addLineToSB(sb, "Error", if (pointingStatus.error == null) "No Errors" else pointingStatus.error.description)
        MissionsMgr.updateStatus(sb.toString())
        showPointByTapFlyPoint(pointingStatus.imageLocation, pointIV)

    }

    private fun showPointByTapFlyPoint(point : PointF?, iv : ImageView)
    {
        if (point == null)
        {
            return
        }
        val parent = iv.parent as View
        mainThread(CommonPool){
            iv.x = point.x * parent.width - iv.width / 2
            iv.y = point.y * parent.height - iv.height / 2
            iv.visibility = View.VISIBLE
            iv.requestLayout()
        }
    }
    private fun setVisible(v : View, visible : Boolean)
    {
        mainThread(CommonPool) {v.visibility = if (visible) View.VISIBLE else View.INVISIBLE}
    }


    fun onLayoutTouch(v : View, event : MotionEvent) : Boolean
    {
        if (v.id != R.id.pointing_bg_layout|| missionMgr != null)
            return false

        when (event.action)
        {
            MotionEvent.ACTION_UP ->
            {
                startButt.setVisibility(View.VISIBLE)
                startButt.setX(event.x - startButt.getWidth() / 2)
                startButt.setY(event.y - startButt.getHeight() / 2)
                startButt.requestLayout()
                mission.imageLocationToCalculateDirection = getTapFlyPoint(startButt)
                missionMgr?.prepareMission(mission, null, DJICommonCallbacks.DJICompletionCallback {error ->
                    if (error == null)
                    {
                        setVisible(startButt, true)
                    }
                    else
                    {
                        setVisible(startButt, false)
                    }
                    Utils.setResultToToast(v.context, if (error == null) "Success" else error.description)
                })
            }
        }
        return true
    }

    private fun getTapFlyPoint(iv : View) : PointF?
    {
        val parent = iv.parent as View
        var centerX = iv.left.toFloat() + iv.x + iv.width.toFloat() / 2
        var centerY = iv.top.toFloat() + iv.y + iv.height.toFloat() / 2
        centerX = if (centerX < 0) 0F else centerX
        centerX = if (centerX > parent.width) parent.width.toFloat() else centerX
        centerY = if (centerY < 0) 0F else centerY
        centerY = if (centerY > parent.height) parent.height.toFloat() else centerY

        return PointF(centerX / parent.width, centerY / parent.height)
    }


    fun onButtonClick(v : View)
    {
        if (missionMgr != null)
        {
            when (v.id)
            {
                R.id.pointing_start_btn ->
                {
                    mission.autoFlightSpeed = getSpeed()
                    mission.isHorizontalObstacleAvoidanceEnabled = assisSw.isChecked()

                    missionMgr?.startMissionExecution(DJICommonCallbacks.DJICompletionCallback {error ->
                        if (error == null)
                        {
                            setVisible(startButt, false)
                            setVisible(stopButt, true)
                            setVisible(assisSw, false)
                        }
                        else
                        {
                            setVisible(startButt, true)
                            setVisible(stopButt, false)
                            setVisible(assisSw, true)
                        }
                        setResultToToast(v.context, "Start: " + if (error == null) "Success" else error.description)
                    })
                }
                R.id.pointing_stop_btn -> missionMgr?.stopMissionExecution(DJICommonCallbacks.DJICompletionCallback {error -> Utils.setResultToToast(v.context, "Stop: " + if (error == null) "Success" else error.description)})

                else ->
                {
                }
            }
        }
    }

    private fun  getSpeed() : Float
    {
        return (speedSeekBar.progress+1).toFloat()
    }

    override fun activate(mgr : DJIMissionManager?)
    {
        super.activate(mgr)
        missionMgr?.setMissionExecutionFinishedCallback(TapFlyMissionMgr::onMissionExecutionFinished)
        missionMgr?.setMissionProgressStatusCallback(TapFlyMissionMgr::onMissionProgressStatusUpdate)
        tapFlyLayout.visibility = View.VISIBLE
        MapSwapper.enableSwapping(false)
        mapView.visibility = View.INVISIBLE
    }

    override fun deactivate()
    {
        super.deactivate()
        missionMgr?.setMissionExecutionFinishedCallback(null)
        missionMgr?.setMissionProgressStatusCallback(null)
        tapFlyLayout.visibility = View.INVISIBLE
        MapSwapper.enableSwapping(true)
        mapView.visibility = View.VISIBLE
    }

}
*/