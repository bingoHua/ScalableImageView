package com.example.scalableimageview

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat
import kotlin.math.max
import kotlin.math.min

private const val EXTRA_SCALE_FACTOR = 1.5f

class ScalableImageView(context: Context, attrs: AttributeSet?) : View(context, attrs),
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener, Runnable {
    private val bitmap = getBeauty(resources, 300.dp.toInt())
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var originOffsetX = 0f
    private var originOffsetY = 0f
    private var offsetX = 0f
    private var offsetY = 0f
    private var smallScale = 0f
    private var bigScale = 0f
    private var big = false
    private val gestureDetector = GestureDetectorCompat(context, this)
    private val scaleGestureDetector = ScaleGestureDetector(context, this)
    private var scroller = OverScroller(context)
    private var scaleFraction = 0f
        set(value) {
            field = value
            invalidate()
        }

    private val scaleAnimator: ObjectAnimator by lazy {
        ObjectAnimator.ofFloat(this, "scaleFraction", smallScale, bigScale)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        originOffsetX = (width - bitmap.width) / 2.toFloat()
        originOffsetY = (height - bitmap.height) / 2.toFloat()

        if (bitmap.width.toFloat() / width > bitmap.height.toFloat() / height) {
            smallScale = width / bitmap.width.toFloat()
            bigScale = height / bitmap.height.toFloat() * EXTRA_SCALE_FACTOR
        } else {
            smallScale = height / bitmap.height.toFloat()
            bigScale = width / bitmap.width.toFloat() * EXTRA_SCALE_FACTOR
        }
        scaleFraction = smallScale
        scaleAnimator.setFloatValues(smallScale, bigScale)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) {
            gestureDetector.onTouchEvent(event)
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = (scaleFraction - smallScale) / (bigScale - smallScale)
        canvas.translate(offsetX * scale, offsetY * scale)
        canvas.scale(scaleFraction, scaleFraction, width / 2f, height / 2f)
        canvas.drawBitmap(bitmap, originOffsetX, originOffsetY, paint)
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    override fun onShowPress(e: MotionEvent?) {
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        if (big) {
            offsetX -= distanceX
            offsetX = min(offsetX, (bitmap.width * bigScale - width) / 2.toFloat())
            offsetX = max(offsetX, -(bitmap.width * bigScale - width) / 2.toFloat())
            offsetY -= distanceY
            offsetY = min(offsetY, (bitmap.height * bigScale - height) / 2.toFloat())
            offsetY = max(offsetY, -(bitmap.height * bigScale - height) / 2.toFloat())
            invalidate()
        }
        return true
    }

    override fun onLongPress(e: MotionEvent?) {
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (big) {
            scroller.fling(offsetX.toInt(), offsetY.toInt(),
                    velocityX.toInt(), velocityY.toInt(),
                    (-(bitmap.width * bigScale - width) / 2).toInt(),
                    ((bitmap.width * bigScale - width) / 2).toInt(),
                    (-(bitmap.height * bigScale - height) / 2).toInt(),
                    ((bitmap.height * bigScale - height) / 2).toInt())
            postOnAnimation(this)
        }
        return true
    }

    private fun refresh() {
        if (scroller.computeScrollOffset()) {
            offsetX = scroller.currX.toFloat()
            offsetY = scroller.currY.toFloat()
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        return false
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        big = !big
        if (big) {
            offsetX = (e.x - width / 2f) * (1 - bigScale / smallScale)
            offsetY = (e.y - height / 2f) * (1 - bigScale / smallScale)
            scaleAnimator.start()
        } else {
            scaleAnimator.reverse()
        }
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
        return false
    }

    override fun run() {
        refresh()
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        val temp = scaleFraction * detector.scaleFactor
        return if (temp > bigScale || temp < smallScale) {
            big = false
            false
        } else {
            big = true
            scaleFraction *= detector.scaleFactor
            true
        }
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        val temp = scaleFraction * detector.scaleFactor
        if (temp > bigScale && temp < smallScale) {
            offsetX = (detector.focusX - width / 2) * (1 - bigScale / smallScale)
            offsetY = (detector.focusY - height / 2) * (1 - bigScale / smallScale)
        }
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector?) {

    }

}