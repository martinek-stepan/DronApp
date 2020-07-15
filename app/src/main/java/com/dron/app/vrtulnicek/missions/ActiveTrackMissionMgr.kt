package com.dron.app.vrtulnicek.missions
/*
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.dron.app.R
import com.dron.app.example.common.Utils
import com.dron.app.example.common.Utils.setResultToToast
import com.dron.app.vrtulnicek.MapSwapper
import com.dron.app.vrtulnicek.missions.MissionsMgr
import com.dron.app.vrtulnicek.VrtulnicekActivity
import com.dron.app.vrtulnicek.utils.Point
import com.dron.app.vrtulnicek.utils.Rect
import com.dron.app.vrtulnicek.views.FpvView
import com.yoavst.kotlin.`KotlinPackage$Tasks$65a6a183`.mainThread
import dji.common.util.DJICommonCallbacks
import dji.sdk.missionmanager.DJIActiveTrackMission
import dji.sdk.missionmanager.DJIActiveTrackMission.DJIActiveTrackMissionExecutionState
import dji.sdk.missionmanager.DJIActiveTrackMission.DJIActiveTrackMissionProgressStatus
import dji.sdk.missionmanager.DJIMission
import dji.sdk.missionmanager.DJIMissionManager
import kotlinx.android.synthetic.main.activity_vrtulnicek.*
import kotlinx.android.synthetic.main.activity_vrtulnicek.view.*
import kotlinx.coroutines.experimental.CommonPool


/**
 * Created by smartinek on 20.4.2017.
 */

object ActiveTrackMissionMgr: MissionMgr()
{

    lateinit var fpvView: FpvView

    // ActiveTrack
    lateinit var activeTrackIV: ImageView
    lateinit var activeTrackConfirmButt : Button
    lateinit var enableRetreatSw: Switch
    lateinit var stopButt: ImageButton
    lateinit var trackingLayout: RelativeLayout

    var startPoint = Point(0F, 0F)
    private var isDrawingRect: Boolean = false
    private var onMission: Boolean = false

    override fun initialize(activity : VrtulnicekActivity)
    {
        fpvView = activity.rootLayout.imageView
        activeTrackIV = activity.rootLayout.tracking_bg_layout.tracking_send_rect_iv
        activeTrackConfirmButt = activity.rootLayout.tracking_bg_layout.tracking_confirm_btn
        enableRetreatSw = activity.rootLayout.tracking_bg_layout.tracking_pull_back_sw
        stopButt = activity.rootLayout.tracking_bg_layout.tracking_stop_btn
        trackingLayout = activity.rootLayout.tracking_bg_layout
        stopButt.setOnClickListener { v -> missionMgr?.stopMissionExecution{ error -> setResultToToast(v.context, if (error == null) "Success!" else error.description); onMission = false }}
        activeTrackConfirmButt.setOnClickListener {v -> DJIActiveTrackMission.acceptConfirmation {error -> setResultToToast(v.context, if (error == null) "Success!" else error.description) }}
        // Init on drag listener for creating rect for ActiveTrack mission
        fpvView.setOnTouchListener(ActiveTrackMissionMgr::onFPVTouchListener);
    }

    private fun onFPVTouchListener(v: View, event: MotionEvent): Boolean
    {
        if (missionMgr == null || onMission)
            return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDrawingRect = false
                startPoint.x = event.x
                startPoint.y = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val movePoint = Point(event.x, event.y)
                if (startPoint.distance(movePoint) < 20F && !isDrawingRect) {
                    return true
                }
                isDrawingRect = true
                val rect = Rect(startPoint, movePoint)
                changeViewDimensions(activeTrackIV, rect)
                activeTrackIV.setVisibility(View.VISIBLE)
            }

            MotionEvent.ACTION_UP -> {

                onMission = true
                val mission: DJIActiveTrackMission
                if (isDrawingRect)
                    mission = DJIActiveTrackMission(getActiveTrackRect(activeTrackIV))
                else
                    mission = DJIActiveTrackMission(PointF(startPoint.x/ fpvView.realWidth, startPoint.y/ fpvView.realHeight))

                mission.isRetreatEnabled = enableRetreatSw.isChecked()
                missionMgr?.prepareMission(mission, null, DJICommonCallbacks.DJICompletionCallback { error ->
                    if (!onMission)
                        return@DJICompletionCallback
                    Utils.setResultToToast(v.context, "Prepare: " + if (error == null) "Success" else error.description)
                    if (error != null)
                    {
                        onMission = false
                        return@DJICompletionCallback
                    }


                    missionMgr?.startMissionExecution(DJICommonCallbacks.DJICompletionCallback { error ->
                        if (error == null)
                            mainThread(CommonPool) { switchVisibility(true) }
                        else
                            onMission = false
                        Utils.setResultToToast(v.context,"Start: " + if (error == null) "Success" else error.description)
                    })
                })

                activeTrackIV.setVisibility(View.INVISIBLE)
            }
        }
        return true
    }

    private fun switchVisibility(started: Boolean) {

        if (started) {
            stopButt.setVisibility(View.VISIBLE)
            enableRetreatSw.setVisibility(View.INVISIBLE)
        }
        else
        {
            stopButt.setVisibility(View.INVISIBLE)
            enableRetreatSw.setVisibility(View.VISIBLE)
        }
    }

    private fun onMissionProgressStatusUpdate(status: DJIMission.DJIMissionProgressStatus)
    {
        if (status !is DJIActiveTrackMissionProgressStatus)
            return

        val sb = StringBuffer()
        Utils.addLineToSB(sb, "center x", status.trackingRect.centerX())
        Utils.addLineToSB(sb, "center y", status.trackingRect.centerY())
        Utils.addLineToSB(sb, "width", status.trackingRect.width())
        Utils.addLineToSB(sb, "height", status.trackingRect.height())
        Utils.addLineToSB(sb, "Executing State", status.executionState.name)
        Utils.addLineToSB(sb, "is human", status.isHuman)
        Utils.addLineToSB(sb, "Error", status.error?.description ?: "No Errors")
        MissionsMgr.updateStatus(sb.toString())
        updateActiveTrackRect(activeTrackConfirmButt, status)
    }


    private fun getActiveTrackRect(iv: View): RectF {
        val parent = iv.parent as View
        return RectF(
                (iv.left.toFloat() + iv.x) / parent.width.toFloat(),
                (iv.top.toFloat() + iv.y) / parent.height.toFloat(),
                (iv.right.toFloat() + iv.x) / parent.width.toFloat(),
                (iv.bottom.toFloat() + iv.y) / parent.height.toFloat()
        )
    }


    private fun updateActiveTrackRect(textView : TextView, progressStatus : DJIActiveTrackMissionProgressStatus?)
    {
        if (progressStatus == null)
            return

        val trackingRect = progressStatus.trackingRect

        val rect = Rect(Point(((trackingRect.centerX() - trackingRect.width() / 2) * fpvView.realWidth),((trackingRect.centerY() - trackingRect.height() / 2) * fpvView.realHeight)),
                        Point(((trackingRect.centerX() + trackingRect.width() / 2) * fpvView.realWidth),((trackingRect.centerY() + trackingRect.height() / 2) * fpvView.realHeight)))


        mainThread(CommonPool){
            if (progressStatus.executionState == DJIActiveTrackMissionExecutionState.TrackingWithLowConfidence ||
                progressStatus.executionState == DJIActiveTrackMissionExecutionState.CannotContinue)
            {
                textView.setBackgroundColor(0x55ff0000)
                textView.isClickable = false
                textView.text = ""
            }
            else if (progressStatus.executionState == DJIActiveTrackMissionExecutionState.WaitingForConfirmation)
            {
                textView.setBackgroundColor(0x5500ff00)
                textView.isClickable = true
                textView.text = "OK"
            }
            else
            {
                textView.setBackgroundResource(R.drawable.visual_track_now)
                textView.isClickable = false
                textView.text = ""
            }

            if (progressStatus.executionState == DJIActiveTrackMissionExecutionState.TargetLost)
            {
                textView.visibility = View.INVISIBLE
            }
            else
            {
                textView.visibility = View.VISIBLE
            }

            changeViewDimensions(textView,rect)

        }
    }

    fun changeViewDimensions(view: View, rect: Rect)
    {
        view.x = rect.left()
        view.y = rect.top()
        view.getLayoutParams().width = rect.width().toInt()
        view.getLayoutParams().height = rect.height().toInt()
        view.requestLayout()
    }

    override fun activate(mgr : DJIMissionManager?)
    {
        super.activate(mgr)
        MapSwapper.enableSwapping(false)
        missionMgr?.setMissionExecutionFinishedCallback { error ->
        Utils.setResultToToast(fpvView.context,"Execution finished: " + if (error == null) "Success!" else error.description)
            activeTrackConfirmButt.setVisibility(View.INVISIBLE)
            stopButt.setVisibility(View.INVISIBLE)
            stopButt.setClickable(false)
            enableRetreatSw.setVisibility(View.VISIBLE)
        }
        missionMgr?.setMissionProgressStatusCallback(ActiveTrackMissionMgr::onMissionProgressStatusUpdate)
        trackingLayout.visibility = View.VISIBLE
    }

    override fun deactivate()
    {
        super.deactivate()
        missionMgr?.setMissionExecutionFinishedCallback(null)
        missionMgr?.setMissionProgressStatusCallback(null)
        fpvView.setOnTouchListener(null)
        trackingLayout.visibility = View.INVISIBLE
        MapSwapper.enableSwapping()
    }
}
*/