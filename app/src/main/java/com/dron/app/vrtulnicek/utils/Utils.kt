package com.dron.app.vrtulnicek.utils

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.text.Html
import android.text.Spanned
import android.view.View
import android.widget.Toast
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * utility file for various short functions, for shorter code
 */


internal fun getMarkerIconFromDrawable(drawable : Drawable) : BitmapDescriptor
{
    val canvas = Canvas()
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    canvas.setBitmap(bitmap)
    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

// for shorter toast function, thanks to kotlin ability to add functions to any class, you can cast it on any Activity in project
fun Activity.toast(message: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

// call bringToFront() on List of View
fun List<View?>.toFront() = this.forEach {item -> item?.let { item.bringToFront() } }


fun View.swapLayoutParams(b : View)
{
    val c = layoutParams
    layoutParams = b.layoutParams
    b.layoutParams = c
}

// compute if given coordinates are inside given view
fun isClickInsideView(xClick: Float, yClick:Float, view: View) :Boolean {
    val position : IntArray = IntArray(2)
    view.getLocationOnScreen(position)
    val x = position[0]
    val y = position[1]
    val w = view.width
    val h = view.height
    return !(xClick< x || xClick> x + w || yClick< y || yClick> y + h)
}

// Helper for creating html formated text for textView
class HTMLText(var text: String)
{
    fun addLine(line : String) : HTMLText
    {
        text += "<bt>" + line
        return this
    }

    fun getSpanned() : Spanned
    {
        return Html.fromHtml(text)
    }

}
// for shorter toast function, thanks to kotlin ability to add functions to any class, you can cast it on any View in project
fun View.toast(s : String, lenght: Int = Toast.LENGTH_SHORT)
{
    Toast.makeText(context, s, lenght).show()
}

// for inline formating of Doubles to given count of digits
fun Double.format(digits: Int): String = java.lang.String.format("%.${digits}f", this)

// shorter Float sqrt
fun Float.Companion.sqrt(value: Float) = Math.sqrt(value.toDouble()).toFloat()
