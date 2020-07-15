package com.dron.app.vrtulnicek.views

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.dron.app.R
import kotlinx.android.synthetic.main.icon_info_view.view.*


/**
 * Custom View for showing signal or battery information
 * you can use params to customize this view
 *   - percentage : IN RANGE 0..100, -1 means undefined or unknown icon is grey
 *   - textType : shows percentage as text otherwise using custom signal icon
 *   - yelow_threshold : bellow this value color of icon will be yellow
 *   - red_threshold : bellow this value color of icon will be red
 */
class IconInfoView : LinearLayout
{

    val DEFAULT_YELLOW_THRESHOLD : Int = 50
    val DEFAULT_RED_THRESHOLD : Int = 20

    var textColor = 0

    private var rootView: View = View.inflate(context, R.layout.icon_info_view, this)
    private var textType: Boolean = false
        set(value) {
            if (value) {
                text.visibility = View.VISIBLE
                signal.visibility = View.INVISIBLE
                signalShadow.visibility = View.INVISIBLE
            } else {
                text.visibility = View.INVISIBLE
                signal.visibility = View.VISIBLE
                signalShadow.visibility = View.VISIBLE
            }
        }
    var percentage: Int = -1
        set(value) {
            field = value

            // setting value of text adding %
            text.setText("${value}%")

            if (value<0) text.setText("--")
            if (value==100) text.setText("100%")

            // setup signal icon
            var signalDrawableId = 0;
            when (value) {
                in 0..25 -> {signalDrawableId = R.drawable.ic_signal_icon_low}
                in 26..50 -> {signalDrawableId = R.drawable.ic_signal_icon_med}
                in 51..75 -> {signalDrawableId = R.drawable.ic_signal_icon_high}
                in 76..100 -> {signalDrawableId = R.drawable.ic_signal_icon_full}
                else -> {signalDrawableId = R.drawable.ic_signal_icon_low}
            }

            signal.setImageResource(signalDrawableId)


            // now we decide color
            var colorId = 0;
            if (value in 0..redThreshold) {
                colorId = R.color.signal_red;
            } else if (value in redThreshold..yellowThreshold) {
                colorId = R.color.signal_yellow;
            } else if (value in yellowThreshold..100) {
                colorId = R.color.signal_green;
            } else {
                colorId = R.color.signal_grey;
            }

            signal.imageTintList =  ContextCompat.getColorStateList(context, colorId)
            icon.imageTintList =  ContextCompat.getColorStateList(context, colorId)

            val color = ContextCompat.getColor(context,colorId)
            textColor = color
            text.setTextColor(color)
        }
    var yellowThreshold = DEFAULT_YELLOW_THRESHOLD;
    var redThreshold = DEFAULT_RED_THRESHOLD;
    var iconDrawableId : Int = android.R.drawable.sym_def_app_icon
        set(value) {
            field = value
            icon.setImageResource(value)

        }



    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {

        val a = context.obtainStyledAttributes(attrs, R.styleable.IconInfoView)
        textType = a.getBoolean(R.styleable.IconInfoView_textType, false)
        percentage = a.getInt(R.styleable.IconInfoView_percentage, -1)
        yellowThreshold = a.getInt(R.styleable.IconInfoView_yellowThreshold, DEFAULT_YELLOW_THRESHOLD)
        redThreshold = a.getInt(R.styleable.IconInfoView_redThreshold, DEFAULT_RED_THRESHOLD)
        iconDrawableId = a.getResourceId(R.styleable.IconInfoView_drawableId,android.R.drawable.sym_def_app_icon)


    }


    init { // nothing here

    }

    override fun getRootView(): View {
        return rootView
    }

}