package com.sp.showcaseview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AlphaAnimation
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
    private var indicatorHeight = 0f
    private var mGuideListener: GuideListener? = null
    private var mDismissType: DismissType? = null
    private var mViewType: ViewType? = null
    private val mMessageView: GuideMessageView

    private fun init() {
        indicatorHeight = INDICATOR_HEIGHT * mDensity
    }

    private fun getNavigationBarSize(): Int {
        val resources: Resources = context.resources
        val resourceId: Int = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    private fun isLandscape(): Boolean {
        val displayMode = resources.configuration.orientation
        return displayMode != Configuration.ORIENTATION_PORTRAIT
    }

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
                    mMessageView.y - bitmap.height
                } else {
                    mMessageView.y + mMessageView.height
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
            return true
        }
        return false
    }

    private fun isViewContains(view: View, rx: Float, ry: Float): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        val w: Int = view.width
        val h: Int = view.height
        return !(rx < x || rx > x + w || ry < y || ry > y + h)
    }

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
            xMessageView = rect.left.toInt()
        }

        if (isLandscape()) {
            xMessageView -= getNavigationBarSize()
        }
        if (xMessageView < 0) {
            xMessageView = 0
        }

        //set message view bottom
        var yMessageView = 0
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

    fun show() {
        this.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        this.isClickable = false
        ((context as Activity).window.decorView as ViewGroup).addView(this)
        val startAnimation = AlphaAnimation(0.0f, 1.0f)
        startAnimation.duration = APPEARING_ANIMATION_DURATION.toLong()
        startAnimation.fillAfter = true
        startAnimation(startAnimation)
    }

    fun setContentText(str: String?) {
        mMessageView.setContentText(str)
    }

    fun setContentTextSize(size: Int) {
        mMessageView.setContentTextSize(size)
    }

    class Builder(private val context: Context) {
        private var targetView: View? = null
        private var contentText: String? = null
        private var dismissType: DismissType? = null
        private var guideListener: GuideListener? = null
        private var contentTextSize = 0
        private var mViewType: ViewType? = null

        fun setTargetView(view: View?): Builder {
            targetView = view
            return this
        }

        /**
         * Set description for the target view.
         *
         * @param contentText Description text.
         * @return [Builder]
         */
        fun setContentText(contentText: String?): Builder {
            this.contentText = contentText
            return this
        }

        /**
         * adding a listener on show case view
         *
         * @param guideListener a listener for events
         */
        fun setGuideListener(guideListener: GuideListener?): Builder {
            this.guideListener = guideListener
            return this
        }

        /**
         * the defined text size overrides any defined size in the default or provided style
         *
         * @param size title text by sp unit
         * @return builder
         */
        fun setContentTextSize(size: Int): Builder {
            contentTextSize = size
            return this
        }

        /**
         * this method defining the type of dismissing function
         *
         * @param dismissType should be one type of DismissType enum. for example: outside -> Dismissing with click on outside of MessageView
         */
        fun setDismissType(dismissType: DismissType?): Builder {
            this.dismissType = dismissType
            return this
        }

        /**
         * Sets the type of view
         *
         * @param viewType [ViewType]
         */
        fun setViewType(viewType: ViewType): Builder {
            mViewType = viewType
            return this
        }

        fun build(): GuideView {
            val guideView = GuideView(context, targetView)
            guideView.mDismissType = dismissType ?: DismissType.TARGET_VIEW
            guideView.mViewType = mViewType
            if (contentText != null) {
                guideView.setContentText(contentText)
            }
            if (contentTextSize != 0) {
                guideView.setContentTextSize(contentTextSize)
            }
            if (guideListener != null) {
                guideView.mGuideListener = guideListener
            }
            return guideView
        }
    }

    companion object {
        private const val INDICATOR_HEIGHT = 18 //Space between message view and arrow
        private const val APPEARING_ANIMATION_DURATION = 400
        private const val RADIUS_SIZE_TARGET_RECT = 50f //Corner radius of rectangle
        private const val BACKGROUND_COLOR = -0x67000000
        private const val SHOW_CASE_VIEW_DEFAULT_MARGIN = 16F //Bottom view default margin
        private const val DEFAULT_MESSAGE_VIEW_WIDTH = 250 //Message view width
    }

    init {
        setWillNotDraw(false)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        mTargetView = view
        mDensity = context.resources.displayMetrics.density
        init()
        if (mTargetView != null) {
            mShowcaseViewRect = buildShowCaseRectangle()
        }
        mMessageView = GuideMessageView(getContext())
        mMessageView.setColor(Color.WHITE)

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