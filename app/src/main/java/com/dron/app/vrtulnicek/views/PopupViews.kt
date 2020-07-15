package com.dron.app.vrtulnicek.views

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.dron.app.R

/*
* function place Popup layout into main constraint layout and set listeners for button to show view*/
fun placeaAndHookViewAsContextPopup(ctx: Context, linearLayoutId:Int, rootLayout: ConstraintLayout, toolbarElement : View, uniqueId: Int, toolBar : LinearLayout, onshow: () -> Unit,onhide: () -> Unit) : LinearLayout {

    val popup = LinearLayout(ctx)

    LayoutInflater.from(ctx).inflate(linearLayoutId,popup)

    popup.layoutParams = ViewGroup.LayoutParams(50,50)
    popup.id = uniqueId

    popup.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    rootLayout.addView(popup)

    val location = IntArray(2)
    toolbarElement.getLocationOnScreen(location)


    val window_size = Point()
    (ctx as Activity).windowManager.defaultDisplay.getRealSize(window_size)
    // compute horizontal bias of popup on screen, if toolbarElement is in left size of screen popup will be biase to left side too
    val bias :Float = location[0].toFloat() /  (window_size.x - toolbarElement.width)


    // setup constraint for newly made popup
    val set =  ConstraintSet()
    set.clone(rootLayout)
    set.connect(popup.id, ConstraintSet.TOP,toolBar.id, ConstraintSet.BOTTOM)
    set.centerHorizontally(popup.id,rootLayout.id, ConstraintSet.LEFT,0,rootLayout.id, ConstraintSet.RIGHT,0,bias)
    set.applyTo(rootLayout)


    // backing up toolbarElement id to tag
    popup.setTag(R.id.toolbar_parent,toolbarElement.id.toString())
    popup.visibility = View.INVISIBLE

    // onclick action for toolbarElement
    toolbarElement.setOnClickListener {
        popup.bringToFront()
        popup.visibility = View.VISIBLE
        popup.isFocusableInTouchMode = true
        // using focus as indication for activity which popup is opened
        popup.requestFocus()
        onshow()
    }

    popup.setOnFocusChangeListener { v, hasFocus ->
        // checking whether there is no other
        if (!hasFocus) {
            onhide()
        }
    }

    return popup
}