package com.sp.showcaseview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding

/**
 * Text view which provides description of show cased view
 *
 * @author Saikrishna Pawar
 * @since 10/27/2021
 */
internal class MessageView(context: Context) :
    LinearLayout(context) {

    private val mPaint: Paint
    private val mRect: RectF
    private val mContentTextView: TextView
    private var mLocation = IntArray(2)

    companion object {
        private const val RADIUS_SIZE = 5
        private const val DEFAULT_CONTENT_TEXT_SIZE = 14
        private const val DEFAULT_CONTENT_TEXT_VIEW_PADDING = 16
    }

    init {
        val density = context.resources.displayMetrics.density
        setWillNotDraw(false)
        orientation = VERTICAL

        mRect = RectF()
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.strokeCap = Paint.Cap.ROUND

        mContentTextView = TextView(context)
        mContentTextView.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            DEFAULT_CONTENT_TEXT_SIZE.toFloat()
        )
        mContentTextView.setPadding(DEFAULT_CONTENT_TEXT_VIEW_PADDING * density.toInt())

        addView(
            mContentTextView,
            LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        getLocationOnScreen(mLocation)
        mRect.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            (height - paddingBottom).toFloat()
        )
        val density = resources.displayMetrics.density.toInt()
        val radiusSize = (RADIUS_SIZE * density).toFloat()
        canvas.drawRoundRect(mRect, radiusSize, radiusSize, mPaint)
    }

    /**
     * Set text on text view.
     *
     * @param content Text.
     */
    fun setContentText(content: String?) {
        mContentTextView.text = content
    }

    /**
     * Text view text size.
     *
     * @param size Text size.
     */
    fun setContentTextSize(size: Int) {
        mContentTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size.toFloat())
    }

    /**
     * Text view text color.
     *
     * @param color Text color.
     */
    fun setContentTextColor(color: Int) {
        mContentTextView.setTextColor(color)
    }

    /**
     * Text view background color.
     *
     * @param color Background color.
     */
    fun setColor(color: Int) {
        mPaint.color = color
        invalidate()
    }
}