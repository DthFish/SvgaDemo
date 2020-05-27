package com.dthfish.svgademo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.StateListDrawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.ViewCompat
import com.opensource.svgaplayer.SVGACallback
import com.opensource.svgaplayer.SVGADynamicEntity
import com.opensource.svgaplayer.SVGAParser
import com.opensource.svgaplayer.SVGAVideoEntity
import com.opensource.svgaplayer.utils.SVGARange
import kotlinx.android.synthetic.main.view_gift_game.view.*
import java.util.*
import java.util.concurrent.LinkedBlockingQueue


/**
 * Description
 * Author DthFish
 * Date  2020/5/22.
 */
class GiftGameView : FrameLayout, View.OnClickListener {

    companion object {
        const val ALREADY_FRAME = 151
        const val FRAME_STEP = 15
        const val SCORES_PER_COUNT_99 = 99
        const val SCORES_PER_COUNT_1 = 1
    }

    private val paint = Paint().apply {
        isAntiAlias = true
    }
    private val alphaPaint = Paint().apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE

    }
    private var parser: SVGAParser
    private var currentStep = ALREADY_FRAME
    private var isIn = false
    private var isFgIn = false
    private var isGoalIn = false

    private var clickCount = 0
    private var btn: ImageView

    private val dynamicEntity = SVGADynamicEntity()

    private var timer = Timer()
    private var arrowTask: ArrowTask? = null
    private val arrowList = LinkedBlockingQueue<ArrowFraction>()
    private var arrowBitmap: Bitmap? = null
    private var foregroundArrowAlpha = 1f
    private val destRect = Rect()
    private val srcRect = Rect()
    //目标当分数超过的时候，显示成功动画
    private var goal = 0
    private val redColor = Color.parseColor("#FF617F")
    private val whiteColor = Color.WHITE
    private var drawProgressBar = false
    private var perCountScores = 99

    private val cancelRunnable = Runnable {
        hide()
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    @SuppressLint("ClickableViewAccessibility")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setWillNotDraw(false)
        inflate(context, R.layout.view_gift_game, this)
        parser = SVGAParser(context)
        btn = ImageView(context)
        btn.visibility = View.INVISIBLE
        btn.isClickable = false
        btn.setOnClickListener(this)
        //随便写一个宽高
        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        );
        btn.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (btn.isClickable) {
                        ViewCompat.postInvalidateOnAnimation(this@GiftGameView)
                    }
                }
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_UP -> {
                    ViewCompat.postInvalidateOnAnimation(this@GiftGameView)
                }
            }

            false
        }
        addView(btn, params)

        playingSvga.callback = object : SVGACallback {
            override fun onPause() {
            }

            override fun onFinished() {
            }

            override fun onRepeat() {
            }

            override fun onStep(frame: Int, percentage: Double) {
                // step 2，开场 2秒 + 爱心倒计时 3秒结束
                if (frame == 150) {
                    step2Fire()
                }
            }

        }
        startSvga.callback = object : SVGACallback {
            override fun onPause() {
            }

            override fun onFinished() {
            }

            override fun onRepeat() {
            }

            override fun onStep(frame: Int, percentage: Double) {
                // step 3， 5秒发射时间结束,需要开始隐藏弓箭动画
                if (frame == 300) {
                    step3HideArrow()
                    step4AttainGoal()
                }
            }
        }
        createBitmap()
    }

    private var arrowHeight = 0

    private var arrowWidth = 0
    private var arrowStartY = 0
    private var arrowEndY = 0
    private var arrowX = 0
    private var startArrowFraction = 0f

    private var progressWidth = 0
    private var progressHeight = 0
    private var progressX = 0
    private var progressY = 0
    private var progress2p = 0//进度条间距
    /**
     *
     * 弓宽高（386*264）箭宽高（246*246）
     * 弓中心点位置（375*1119（手机尺寸750*1334情况下））
     * 箭中心点起始位置（375*1091（手机尺寸750*1334情况下））
     * 箭中心点最终位置（375*496（手机尺寸750*1334情况下））
     * 箭运动时间（0.16秒）
     * 箭到达最终位置后会有渐隐效果（0.16秒）
     * 下面计算以高度来计算
     * 进度条 400 * 16
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //根据高算
        val btnWidth = 386f * h / 1334
        val btnHeight = 264f * h / 1334
        val params = LayoutParams(
            btnWidth.toInt(),
            btnHeight.toInt(),
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        )
        params.bottomMargin = (215f * h / 1334 - btnHeight / 2).toInt()

        //最高记录距离右边的 margin, 375 - 12 为右侧距离 750 宽度动画中心的距离
        val goalParams = tvGoal.layoutParams as MarginLayoutParams
        val goalMarginRight = (w / 2 - ((375f - 12) * h / 1334)).toInt()
        if (goalMarginRight > 0) {
            goalParams.rightMargin = goalMarginRight
        }

        post {
            btn.layoutParams = params
            tvGoal.layoutParams = goalParams
        }
        arrowHeight = (246f * h / 1334).toInt()
        arrowWidth = (246f * h / 1334).toInt()
        arrowX = (w - arrowWidth) / 2
        arrowStartY = (h - 243f * h / 1334 - arrowHeight / 2).toInt()
        arrowEndY = (h - 838f * h / 1334 - arrowHeight / 2).toInt()
        destRect.set(0, 0, arrowWidth, arrowHeight)

        progressWidth = (400f * h / 1334).toInt()
        progressHeight = (16f * h / 1334).toInt()
        progressX = (w - progressWidth) / 2
        progressY = (h - (414 + 16f) * h / 1334).toInt()
        progress2p = (2f * h / 1334).toInt()
    }

    fun show(goal: Int) {
        this.goal = goal
        tvGoal.text = "最高记录${goal}年"
        reset()
        parser.apply {
            decodeFromAssets("start.svga", object : SVGAParser.ParseCompletion {
                override fun onComplete(videoItem: SVGAVideoEntity) {
                    startSvga.setVideoItem(videoItem)
                    isFgIn = true
                    startSvga.startAnimation()
                    step1ShowArrow()
                }

                override fun onError() {
                    Log.d("DDDDDD", "onError")
                }

            })

            val playingSvgaFile = if (perCountScores == SCORES_PER_COUNT_99) {
                "playing.svga"
            } else {
                "playing2.svga"
            }

            decodeFromAssets(playingSvgaFile, object : SVGAParser.ParseCompletion {
                override fun onComplete(videoItem: SVGAVideoEntity) {
                    playingSvga.setVideoItem(videoItem, dynamicEntity)
                    isIn = true
                    step1ShowArrow()

                }

                override fun onError() {
                    Log.d("DDDDDD", "onError")
                }

            })
            decodeFromAssets("finish.svga", object : SVGAParser.ParseCompletion {
                override fun onComplete(videoItem: SVGAVideoEntity) {
                    finishSvga.setVideoItem(videoItem)
                    isGoalIn = true
                }

                override fun onError() {
                    Log.d("DDDDDD", "onError")
                }

            })
        }
    }

    fun isShow(): Boolean {
        return visibility == View.VISIBLE
    }

    private fun reset() {
        currentStep = ALREADY_FRAME
        isIn = false
        isFgIn = false
        isGoalIn = false
        playingSvga.loops = 1
        startSvga.loops = 1
        clickCount = 0
        btn.isClickable = false
        btn.visibility = View.INVISIBLE
        updateCount(0)
        arrowTask?.cancel()
        arrowTask = null
        timer.purge()
//        drawForegroundArrow = true
        foregroundArrowAlpha = 1f
        drawProgressBar = false
        finishSvga.visibility = View.INVISIBLE
    }

    private fun step1ShowArrow() {
        // step 1，开场动画，弓箭跟随显示
        if (isFgIn && isIn) {
            visibility = View.VISIBLE
            val range = SVGARange(0, ALREADY_FRAME)
            playingSvga?.startAnimation(range)
            startSvga?.startAnimation()

            btn.visibility = View.VISIBLE
            val translateAnimation = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 2f, Animation.RELATIVE_TO_SELF, 0f
            )
            translateAnimation.setInterpolator {
                startArrowFraction = it
                it
            }
            translateAnimation.duration = 1000
            translateAnimation.startOffset = 1000
            btn.startAnimation(translateAnimation)

            //直接发送一个 13.5 秒的任务，防止过程中出现问题，没有隐藏 GiftGameView
            removeCallbacks(cancelRunnable)
            postDelayed(cancelRunnable, 13500)
        }
    }

    private fun step2Fire() {
        // 弓箭可点击，开始发射
        btn.isClickable = true
        drawProgressBar = true
        arrowTask = ArrowTask()
        timer.scheduleAtFixedRate(arrowTask, 0, 16)
    }

    private fun step3HideArrow() {
        // 弓箭不可点击，开始 1秒内隐藏
        btn.isClickable = false
        btn.isPressed = false
        arrowTask?.markNeedCancel()
        arrowTask = null
        val alpha = AlphaAnimation(1f, 0f)
        alpha.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                btn.visibility = View.INVISIBLE
            }

            override fun onAnimationStart(animation: Animation?) {
            }

        })
        alpha.setInterpolator {
            foregroundArrowAlpha = 1 - it
            it
        }
        alpha.duration = 1000
        btn.startAnimation(alpha)

    }

    private fun step4AttainGoal() {
        // 达到目标播放动画，播放隐藏动画，2秒后隐藏
        if (clickCount > goal && isGoalIn) {
            finishSvga.visibility = View.VISIBLE
            finishSvga.startAnimation()
        }

        val alpha = AlphaAnimation(1f, 0f)
        alpha.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                btn.visibility = View.INVISIBLE
            }

            override fun onAnimationStart(animation: Animation?) {
            }

        })
        alpha.duration = 1000
        alpha.startOffset = 1000
        startAnimation(alpha)
        removeCallbacks(cancelRunnable)
        postDelayed(cancelRunnable, 2000)
    }

    private fun hide() {
        visibility = View.GONE
    }

    private fun createBitmap() {
        val stateListDrawable = StateListDrawable()

        stateListDrawable.addState(
            intArrayOf(android.R.attr.state_pressed),
            BitmapDrawable(
                resources,
                BitmapFactory.decodeResource(resources, R.drawable.icon_press)
            )
        )
        stateListDrawable.addState(
            intArrayOf(0),
            BitmapDrawable(
                resources,
                BitmapFactory.decodeResource(resources, R.drawable.icon_normal)
            )
        )
        btn.setImageDrawable(stateListDrawable)
        arrowBitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_arrow)
        srcRect.set(0, 0, arrowBitmap!!.width, arrowBitmap!!.height)
    }

    override fun onClick(v: View?) {
        clickCount++
        updateCount(clickCount * perCountScores)
        arrowList.add(ArrowFraction())
        if (clickCount % 5 == 0) {
            val range = SVGARange(currentStep, FRAME_STEP)
            currentStep += FRAME_STEP
            if (currentStep > 285) {
                currentStep = 285
                playingSvga.loops = 0
            }
            playingSvga.startAnimation(range)
        }

    }

    private val namePaint = TextPaint()

    private fun updateCount(count: Int) {
        namePaint.color = Color.parseColor("#ffffff")
        namePaint.textSize = 48f
        namePaint.typeface = Typeface.DEFAULT_BOLD
        /*val nameSpanny = Spanny()
        RichTextHelper.processFontSizeRichText(
            context, nameSpanny, RichTextHelper.parseRichText(
                chatInfo.mAnimationSvgaNickname
            ),
            25, 26, Color.parseColor("#ffffff"), svgaImageView
        )*/
        dynamicEntity.setDynamicText(
            StaticLayout(
                count.toString(),
                0,
                count.toString().length,
                namePaint,
                0,
                Layout.Alignment.ALIGN_CENTER,
                1.0f,
                0.0f,
                false
            ), "abcdefg"
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val iterator = arrowList.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            destRect.offsetTo(
                arrowX,
                (arrowStartY + next.toFraction() * (arrowEndY - arrowStartY)).toInt()
            )
            arrowBitmap?.let {
                canvas.drawBitmap(
                    it,
                    srcRect,
                    destRect,
                    paint
                )
            }
        }
    }

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
        if (drawProgressBar) {
            // 画进度条
            val fraction = if (goal == 0 && clickCount == 0) {
                0f
            } else if (goal == 0) {
                1f
            } else {
                (clickCount.toFloat() / goal).coerceAtMost(1f)
            }
            alphaPaint.color = whiteColor
            alphaPaint.strokeWidth = progressHeight.toFloat()
            canvas.drawLine(
                progressX.toFloat(),
                progressY.toFloat(),
                (progressX + progressWidth).toFloat(),
                progressY.toFloat(),
                alphaPaint
            )
            if (fraction > 0) {
                alphaPaint.strokeWidth = (progressHeight - 2 * progress2p).toFloat()
                alphaPaint.color = redColor
                canvas.drawLine(
                    (progressX + progress2p).toFloat(),
                    progressY.toFloat(),
                    (progressX + progress2p).toFloat() + (progressWidth - 2 * progress2p) * fraction,
                    progressY.toFloat(),
                    alphaPaint
                )
            }
        }
    }

    class ArrowFraction {
        var fraction = 0

        fun toFraction(): Float {
            return fraction.toFloat() / 10
        }
    }

    inner class ArrowTask : TimerTask() {
        private var needCancel = false
        override fun run() {
            var handle = false
            val iterator = arrowList.iterator()
            while (iterator.hasNext()) {
                handle = true
                val next = iterator.next()
                next.fraction++
                if (next.fraction > 10) {
                    iterator.remove()
                }
            }
            if (handle) {
                ViewCompat.postInvalidateOnAnimation(this@GiftGameView)
            }
            if (arrowList.isEmpty() && needCancel) {
                cancel()
            }
        }

        fun markNeedCancel() {
            needCancel = true
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> return true
        }
        return super.onTouchEvent(event)
    }

}