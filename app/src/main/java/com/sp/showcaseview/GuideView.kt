package com.sp.showcaseview

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import com.sp.showcaseview.config.DismissType
import com.sp.showcaseview.config.PointerType
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

    private val selfPaint: Paint = Paint()
    private val paintCircle: Paint = Paint()
    private val paintCircleInner: Paint = Paint()
    private val targetPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val porterDuffModeClear: Xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    private val target: View?
    private var targetRect: RectF? = null
    private val selfRect: Rect = Rect()
    private val density: Float
    private var stopY = 0f
    private var isTop = false
    private var yMessageView = 0
    private var startYLineAndCircle = 0f
    private var circleIndicatorSize = 0f
    private var circleIndicatorSizeFinal = 0f
    private var circleInnerIndicatorSize = 0f
    private var marginGuide = 0f
    private var indicatorHeight = 0f
    private var isPerformedAnimationSize = false
    private var mGuideListener: GuideListener? = null
    private var dismissType: DismissType? = null
    private var pointerType: PointerType? = null
    private var mViewType: ViewType? = null
    private val mMessageView: GuideMessageView

    private fun startAnimationSize() {
        if (!isPerformedAnimationSize) {
            val circleSizeAnimator = ValueAnimator.ofFloat(
                0f,
                circleIndicatorSizeFinal
            )
            circleSizeAnimator.addUpdateListener {
                circleIndicatorSize = it.animatedValue as Float
                circleInnerIndicatorSize = it.animatedValue as Float - density
                postInvalidate()
            }
            val linePositionAnimator = ValueAnimator.ofFloat(
                stopY,
                startYLineAndCircle
            )
            linePositionAnimator.addUpdateListener {
                startYLineAndCircle = it.animatedValue as Float
                postInvalidate()
            }
            linePositionAnimator.duration = SIZE_ANIMATION_DURATION.toLong()
            linePositionAnimator.start()
            linePositionAnimator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {
                    //Do nothing
                }

                override fun onAnimationEnd(animator: Animator) {
                    circleSizeAnimator.duration = SIZE_ANIMATION_DURATION.toLong()
                    circleSizeAnimator.start()
                    isPerformedAnimationSize = true
                }

                override fun onAnimationCancel(animator: Animator) {
                    //Do nothing
                }

                override fun onAnimationRepeat(animator: Animator) {
                    //Do nothing
                }
            })
        }
    }

    private fun init() {
        marginGuide = MARGIN_INDICATOR * density
        indicatorHeight = INDICATOR_HEIGHT * density
        circleIndicatorSizeFinal = CIRCLE_INDICATOR_SIZE * density
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
        if (target != null) {
            selfPaint.color = BACKGROUND_COLOR
            selfPaint.style = Paint.Style.FILL
            selfPaint.isAntiAlias = true
            canvas.drawRect(selfRect, selfPaint)

            //Arrow paint circle
            paintCircle.style = Paint.Style.FILL
            paintCircle.color = CIRCLE_INDICATOR_COLOR
            paintCircle.isAntiAlias = true

            paintCircleInner.style = Paint.Style.FILL
            paintCircleInner.color = CIRCLE_INNER_INDICATOR_COLOR
            paintCircleInner.isAntiAlias = true
            val x = MESSAGE_VIEW_MARGIN_START * density
            when (pointerType) {
                PointerType.ARROW -> {
                    val path = Path()
                    if (isTop) {
                        path.moveTo(x, startYLineAndCircle - circleIndicatorSize * 2)
                        path.lineTo(x + circleIndicatorSize, startYLineAndCircle)
                        path.lineTo(x - circleIndicatorSize, startYLineAndCircle)
                        path.close()
                    } else {
                        path.moveTo(x, startYLineAndCircle + circleIndicatorSize * 2)
                        path.lineTo(x + circleIndicatorSize, startYLineAndCircle)
                        path.lineTo(x - circleIndicatorSize, startYLineAndCircle)
                        path.close()
                    }
                    canvas.drawPath(path, paintCircle)
                }
                PointerType.NONE -> {
                    //Do nothing
                }
            }
            targetPaint.xfermode = porterDuffModeClear
            targetPaint.isAntiAlias = true
            if (target is Target) {
                (target as Target).guidePath()?.let { canvas.drawPath(it, targetPaint) }
            } else {
                canvas.drawRoundRect(
                    targetRect!!,
                    RADIUS_SIZE_TARGET_RECT,
                    RADIUS_SIZE_TARGET_RECT,
                    targetPaint
                )
            }
        }
    }

    /**
     * Dismisses the showcase view
     */
    private fun dismiss() {
        ((context as Activity).window.decorView as ViewGroup).removeView(this)
        if (mGuideListener != null && target != null) {
            mGuideListener!!.onDismiss(target)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        if (event.action == MotionEvent.ACTION_DOWN) {
            when (dismissType) {
                DismissType.OUTSIDE -> if (!isViewContains(mMessageView, x, y)) {
                    dismiss()
                }
                DismissType.ANYWHERE -> dismiss()
                DismissType.TARGET_VIEW -> if (targetRect!!.contains(x, y)) {
                    target?.performClick()
                    dismiss()
                }
                DismissType.SELF_VIEW -> if (isViewContains(mMessageView, x, y)) {
                    dismiss()
                }
                DismissType.OUTSIDE_TARGET_AND_MESSAGE -> if (!(targetRect!!.contains(
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
        var xMessageView: Int = (MESSAGE_VIEW_MARGIN_START * 2.2F).toInt()

        if (isLandscape()) {
            xMessageView -= getNavigationBarSize()
        }
        if (xMessageView < 0) {
            xMessageView = 0
        }

        //set message view bottom
        if (targetRect!!.top + indicatorHeight > height / 2f) {
            isTop = false
            yMessageView = (targetRect!!.top - mMessageView.height - indicatorHeight).toInt()
        } else {
            isTop = true
            yMessageView = ((targetRect!!.top + target!!.height + indicatorHeight).toInt())
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
        private var pointerType: PointerType? = null
        private var guideListener: GuideListener? = null
        private var contentTextSize = 0
        private var circleIndicatorSize = 0f
        private var circleInnerIndicatorSize = 0f
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
         * this method defining the type of pointer
         *
         * @param pointerType should be one type of PointerType enum. for example: arrow -> To show arrow pointing to target view
         */
        fun setPointerType(pointerType: PointerType?): Builder {
            this.pointerType = pointerType
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
            guideView.dismissType = dismissType ?: DismissType.TARGET_VIEW
            guideView.pointerType = pointerType ?: PointerType.ARROW
            guideView.mViewType = mViewType
            val density: Float = context.resources.displayMetrics.density
            if (contentText != null) {
                guideView.setContentText(contentText)
            }
            if (contentTextSize != 0) {
                guideView.setContentTextSize(contentTextSize)
            }
            if (guideListener != null) {
                guideView.mGuideListener = guideListener
            }
            if (circleIndicatorSize != 0f) {
                guideView.circleIndicatorSize = circleIndicatorSize * density
            }
            if (circleInnerIndicatorSize != 0f) {
                guideView.circleInnerIndicatorSize = circleInnerIndicatorSize * density
            }
            return guideView
        }
    }

    companion object {
        private const val INDICATOR_HEIGHT = 18 //Space between message view and arrow
        private const val SIZE_ANIMATION_DURATION = 700
        private const val APPEARING_ANIMATION_DURATION = 400
        private const val CIRCLE_INDICATOR_SIZE = 8 //Arrow mark size
        private const val RADIUS_SIZE_TARGET_RECT = 50f //Corner radius of rectangle
        private const val MARGIN_INDICATOR = 20 //Space between showcase view and arrow
        private const val BACKGROUND_COLOR = -0x67000000
        private const val CIRCLE_INNER_INDICATOR_COLOR = -0x333334
        private const val CIRCLE_INDICATOR_COLOR: Int = Color.WHITE
        private const val DEFAULT_MARGIN = 0F
        private const val SHOW_CASE_VIEW_DEFAULT_MARGIN = 16F //Bottom view default margin
        private const val MESSAGE_VIEW_MARGIN_START = 32F //Start margin of message view
        private const val DEFAULT_MESSAGE_VIEW_WIDTH = 250 //Message view width
    }

    init {
        setWillNotDraw(false)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        target = view
        density = context.resources.displayMetrics.density
        init()
        if (target != null) {
            targetRect = buildShowCaseRectangle()
        }
        mMessageView = GuideMessageView(getContext())
        mMessageView.setColor(Color.WHITE)

        addView(
            mMessageView,
            LayoutParams(
                (DEFAULT_MESSAGE_VIEW_WIDTH * density).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        setMessageLocation(resolveMessageViewLocation())
        val layoutListener: OnGlobalLayoutListener = object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                setMessageLocation(resolveMessageViewLocation())
                if (target != null) {
                    targetRect = buildShowCaseRectangle()
                }
                selfRect.set(
                    paddingLeft,
                    paddingTop,
                    width - paddingRight,
                    height - paddingBottom
                )
                marginGuide = (if (isTop) marginGuide else -marginGuide).toFloat()
                startYLineAndCircle =
                    ((if (isTop) targetRect?.bottom else targetRect?.top)?.plus(marginGuide)
                        ?: 0).toFloat()
                stopY = yMessageView + indicatorHeight
                startAnimationSize()
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
        val rectF = target?.let {
            if (it is Target) {
                (it as Target).boundingRect()
            } else {
                val locationTarget = IntArray(2)
                it.getLocationOnScreen(locationTarget)
//                val viewMargin = bottomViewMargin(locationTarget)
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

//    /**
//     * Margin if the view is placed at bottom of the screen.
//     *
//     * @param locationTarget Position of the view on screen.
//     *        It has x,y co-ordinates present in it
//     *@return Margin that needs to be adjusted for the view.
//     */
//    private fun bottomViewMargin(locationTarget: IntArray): Float {
//        val margin =
//            if (locationTarget[0] == 0 && target?.height?.plus(locationTarget[1]) == height) {
//                //View is placed at bottom of the screen
//                SHOW_CASE_VIEW_DEFAULT_MARGIN
//            } else {
//                DEFAULT_MARGIN
//            }
//        return margin
//    }
}