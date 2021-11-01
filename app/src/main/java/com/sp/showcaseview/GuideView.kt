package com.sp.showcaseview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.sp.showcaseview.config.DismissType
import com.sp.showcaseview.config.ViewType
import com.sp.showcaseview.listener.GuideListener


/**
 * Main view class to showcase the view and add a
 * description text containing information of show case view.
 *
 * @author Saikrishna Pawar
 * @since 10/27/2021
 */
@SuppressLint("ViewConstructor")
class GuideView constructor(context: Context, view: View?) : FrameLayout(context) {

    private val mBackGroundPaint: Paint = Paint()
    private val mArrowPaint: Paint = Paint()
    private val mShowcaseViewPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mPorterDuffModeClear: Xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    private val mTargetView: View?
    private var mShowcaseViewRect: RectF? = null
    private val mBackgroundRect: Rect = Rect()
    private val mDensity: Float
    private var mIsTop = false
    private var mGuideListener: GuideListener? = null
    private var mDismissType: DismissType? = null
    private var mViewType: ViewType? = null
    private val mMessageView: GuideMessageView

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mTargetView != null) {
            mBackGroundPaint.color = BACKGROUND_COLOR
            mBackGroundPaint.style = Paint.Style.FILL
            mBackGroundPaint.isAntiAlias = true
            canvas.drawRect(mBackgroundRect, mBackGroundPaint)

            //Arrow paint circle
            mArrowPaint.color = Color.WHITE

            val drawableId = if (mIsTop) {
                R.drawable.ic_chevron_up
            } else {
                R.drawable.ic_chevron_down
            }
            getVectorBitmap(context, drawableId)?.let { bitmap ->
                val xBitmap = mMessageView.x
                val yBitmap = if (mIsTop) {
                    mMessageView.y.minus(bitmap.height.minus(CHEVRON_TOP_GAP * mDensity))
                } else {
                    mMessageView.y.plus(mMessageView.height.minus(CHEVRON_BOTTOM_GAP * mDensity))
                }
                canvas.drawBitmap(
                    bitmap,
                    xBitmap,
                    yBitmap,
                    mArrowPaint
                )
            }
            mShowcaseViewPaint.xfermode = mPorterDuffModeClear
            mShowcaseViewPaint.isAntiAlias = true
            if (mTargetView is Target) {
                (mTargetView as Target).guidePath()?.let { canvas.drawPath(it, mShowcaseViewPaint) }
            } else {
                canvas.drawRoundRect(
                    mShowcaseViewRect!!,
                    RADIUS_SIZE_TARGET_RECT,
                    RADIUS_SIZE_TARGET_RECT,
                    mShowcaseViewPaint
                )
            }
        }
    }

    /**
     * Returns the drawable as vector bitmap or simply bitmap if it is a bitmap drawable.
     *
     * @param context Context
     * @param drawableId Drawable resource identifier
     *
     * @return Bitmap object
     */
    private fun getVectorBitmap(context: Context, drawableId: Int): Bitmap? {
        var bitmap: Bitmap? = null
        when (val drawable = ContextCompat.getDrawable(context, drawableId)) {
            is BitmapDrawable -> {
                bitmap = drawable.bitmap
            }
            is VectorDrawable -> {
                bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        }
        return bitmap
    }

    /**
     * Dismisses the showcase view
     */
    private fun dismiss() {
        ((context as Activity).window.decorView as ViewGroup).removeView(this)
        if (mGuideListener != null && mTargetView != null) {
            mGuideListener!!.onDismiss(mTargetView)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        if (event.action == MotionEvent.ACTION_DOWN) {
            onDismiss(x, y)
            return true
        }
        return false
    }

    /**
     * On dismiss
     *
     * @param x x co-ordinate.
     * @param y y co-ordinate.
     */
    private fun onDismiss(x: Float, y: Float) {
        when (mDismissType) {
            DismissType.OUTSIDE -> if (!isViewContains(mMessageView, x, y)) {
                dismiss()
            }
            DismissType.ANYWHERE -> dismiss()
            DismissType.TARGET_VIEW -> if (mShowcaseViewRect!!.contains(x, y)) {
                mTargetView?.performClick()
                dismiss()
            }
            DismissType.SELF_VIEW -> if (isViewContains(mMessageView, x, y)) {
                dismiss()
            }
            DismissType.OUTSIDE_TARGET_AND_MESSAGE -> if (!(mShowcaseViewRect!!.contains(
                    x,
                    y
                ) || isViewContains(
                    mMessageView,
                    x,
                    y
                ))
            ) {
                dismiss()
            }
        }
    }

    /**
     * Checks if the view contains the x and y co-ordinates.
     *
     * @param view View that need to be checked.
     * @param rx Point X co-ordinate.
     * @param ry Point Y co-ordinate.
     *
     * @return True if the co-ordinates are present in the view. False, otherwise.
     */
    private fun isViewContains(view: View, rx: Float, ry: Float): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        val w: Int = view.width
        val h: Int = view.height
        return !(rx < x || rx > x + w || ry < y || ry > y + h)
    }

    /**
     * Sets the Point x and y co-ordinates for [GuideMessageView].
     *
     * @param p [Point] Containing the x and y co-ordinates.
     */
    private fun setMessageLocation(p: Point) {
        mMessageView.x = p.x.toFloat()
        mMessageView.y = p.y.toFloat()
        postInvalidate()
    }

    /**
     * Provides the x,y co-ordinate to set the message view on screen
     *
     * @return Point object holding two integer x,y co-ordinates
     */
    private fun resolveMessageViewLocation(): Point {
        var xMessageView = 0
        mShowcaseViewRect?.let { rect ->
            xMessageView = (rect.left.toInt()) * MESSAGE_VIEW_AND_ARROW_LEFT_MARGIN_MULTIPLIER
        }
        if (xMessageView < 0) {
            xMessageView = 0
        }

        //set message view bottom
        val indicatorHeight = INDICATOR_HEIGHT * mDensity
        var yMessageView: Int
        if (mShowcaseViewRect!!.top + indicatorHeight > height / 2f) {
            mIsTop = false
            yMessageView =
                (mShowcaseViewRect!!.top - mMessageView.height - indicatorHeight).toInt()
        } else {
            mIsTop = true
            yMessageView =
                ((mShowcaseViewRect!!.top + mTargetView!!.height + indicatorHeight).toInt())
        }
        if (yMessageView < 0) {
            yMessageView = 0
        }
        return Point(xMessageView, yMessageView)
    }

    /**
     * Displays the showcase view.
     */
    fun show() {
        this.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        this.isClickable = false
        ((context as Activity).window.decorView as ViewGroup).addView(this)
    }

    /**
     * Set content on text view.
     *
     * @param str String content.
     */
    fun setContentText(str: String?) {
        mMessageView.setContentText(str)
    }

    /**
     * Set text size
     *
     * @param size Text size.
     */
    fun setContentTextSize(size: Int) {
        mMessageView.setContentTextSize(size)
    }

    /**
     * Class to build the showcase view
     */
    class Builder(private val context: Context) {
        private var mTargetView: View? = null
        private var mContentText: String? = null
        private var mDismissType: DismissType? = null
        private var mGuideListener: GuideListener? = null
        private var mContentTextSize = 0
        private var mViewType: ViewType? = null

        /**
         * View on which the showcase view has to be displayed.
         *
         * @param view Target view.
         * @return [Builder]
         */
        fun setTargetView(view: View?): Builder {
            mTargetView = view
            return this
        }

        /**
         * Set description on text view.
         *
         * @param contentText Description text.
         * @return [Builder]
         */
        fun setContentText(contentText: String?): Builder {
            this.mContentText = contentText
            return this
        }

        /**
         * Set listener on show case view.
         *
         * @param guideListener Listener for events.
         * @return [Builder]
         */
        fun setGuideListener(guideListener: GuideListener?): Builder {
            this.mGuideListener = guideListener
            return this
        }

        /**
         * Set message view text size.
         *
         * @param size Text size.
         * @return [Builder]
         */
        fun setContentTextSize(size: Int): Builder {
            mContentTextSize = size
            return this
        }

        /**
         * Set dismiss type on showcase view.
         *
         * @param dismissType Type defined in [DismissType]
         * @return [Builder]
         */
        fun setDismissType(dismissType: DismissType?): Builder {
            this.mDismissType = dismissType
            return this
        }

        /**
         * Sets the type of view for text view and arrow alignment.
         *
         * @param viewType [ViewType]
         * @return [Builder]
         */
        fun setViewType(viewType: ViewType): Builder {
            mViewType = viewType
            return this
        }

        /**
         * Builds the showcase view
         *
         * @return [GuideView]
         */
        fun build(): GuideView {
            val guideView = GuideView(context, mTargetView)
            guideView.mDismissType = mDismissType ?: DismissType.TARGET_VIEW
            guideView.mViewType = mViewType
            if (mContentText != null) {
                guideView.setContentText(mContentText)
            }
            if (mContentTextSize != 0) {
                guideView.setContentTextSize(mContentTextSize)
            }
            if (mGuideListener != null) {
                guideView.mGuideListener = mGuideListener
            }
            return guideView
        }
    }

    companion object {
        private const val INDICATOR_HEIGHT = 18 //Space arrow and showcase view
        private const val RADIUS_SIZE_TARGET_RECT = 50f //Corner radius of rectangle
        private const val BACKGROUND_COLOR = -0x67000000
        private const val SHOW_CASE_VIEW_DEFAULT_MARGIN = 16F //Bottom view default margin
        private const val DEFAULT_MESSAGE_VIEW_WIDTH = 250 //Message view width
        private const val CHEVRON_TOP_GAP = 3
        private const val CHEVRON_BOTTOM_GAP = 5
        private const val MESSAGE_VIEW_AND_ARROW_LEFT_MARGIN_MULTIPLIER = 3
    }

    init {
        setWillNotDraw(false)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        mTargetView = view
        mDensity = context.resources.displayMetrics.density
        if (mTargetView != null) {
            mShowcaseViewRect = buildShowCaseRectangle()
        }
        mMessageView = GuideMessageView(getContext())
        mMessageView.setColor(ContextCompat.getColor(context, R.color.message_view_bg))
        mMessageView.setContentTextColor(
            ContextCompat.getColor(
                context,
                R.color.message_view_text_color
            )
        )

        addView(
            mMessageView,
            LayoutParams(
                (DEFAULT_MESSAGE_VIEW_WIDTH * mDensity).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        setMessageLocation(resolveMessageViewLocation())
        val layoutListener: OnGlobalLayoutListener = object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                setMessageLocation(resolveMessageViewLocation())
                if (mTargetView != null) {
                    mShowcaseViewRect = buildShowCaseRectangle()
                }
                mBackgroundRect.set(
                    paddingLeft,
                    paddingTop,
                    width - paddingRight,
                    height - paddingBottom
                )
                viewTreeObserver.addOnGlobalLayoutListener(this)
            }
        }
        viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    /**
     * Builds the showcase rectangle.
     *
     * @return Rectangle size showcase view
     */
    private fun buildShowCaseRectangle(): RectF? {
        val rectF = mTargetView?.let {
            if (it is Target) {
                (it as Target).boundingRect()
            } else {
                val locationTarget = IntArray(2)
                it.getLocationOnScreen(locationTarget)
                val viewMargin = SHOW_CASE_VIEW_DEFAULT_MARGIN
                when (mViewType) {
                    ViewType.BOTTOM_NAVIGATION -> {
                        RectF(
                            locationTarget[0].toFloat().plus(viewMargin),
                            locationTarget[1].toFloat().plus(viewMargin),
                            (locationTarget[0] + it.width).toFloat().minus(viewMargin),
                            (locationTarget[1] + it.height).toFloat().minus(viewMargin)
                        )
                    }
                    else -> {
                        RectF(
                            locationTarget[0].toFloat().plus(viewMargin),
                            locationTarget[1].toFloat(),
                            (locationTarget[0] + it.width).toFloat().minus(viewMargin),
                            (locationTarget[1] + it.height).toFloat()
                        )
                    }
                }
            }
        }
        return rectF
    }
}