package com.pwr.yourrhythm.theme

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

class CurveProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val strokeDp = 12f
    private val stroke: Float
    private val extraTextTopDp = 10f   // dodatkowa przestrzeń nad tekstem (dp)
    private val textMarginDp = 6f      // odległość tekstu nad łukiem (dp)

    private val paintBg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintProgress = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)

    // dynamiczne wartości
    private var progress: Float = 0f // 0..1
    private var bpmValue: Int = 0

    // obliczane w px
    private var extraTopPx = 0f

    init {
        val metrics = context.resources.displayMetrics
        stroke = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, strokeDp, metrics)

        paintBg.apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#33FFFFFF")
        }

        paintProgress.apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            // shader ustawimy w onSizeChanged
        }

        paintText.apply {
            color = Color.parseColor("#222222")
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, metrics) // 20sp domyślnie
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
    }

    // Settery
    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    fun setBpm(bpm: Int) {
        bpmValue = bpm
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        paintProgress.shader = LinearGradient(
            0f, 0f, w.toFloat(), 0f,
            intArrayOf(Color.parseColor("#22E07A"), Color.parseColor("#8AFBC0")),
            null,
            Shader.TileMode.CLAMP
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)

        val prefArcHeightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, resources.displayMetrics)

        val desiredHeight = when (hMode) {
            MeasureSpec.EXACTLY -> hSize.toFloat()
            MeasureSpec.AT_MOST -> min(hSize.toFloat(), prefArcHeightPx)
            else -> prefArcHeightPx
        }

        val textSize = paintText.textSize
        val extraTopEstimate = textSize + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, extraTextTopDp, resources.displayMetrics)

        val finalHeight = (desiredHeight + extraTopEstimate).toInt()

        setMeasuredDimension(wSize, finalHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val metrics = resources.displayMetrics
        val textSize = paintText.textSize
        val textMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, textMarginDp, metrics)
        val extraTextTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, extraTextTopDp, metrics)

        val text = bpmValue.toString()
        val textBounds = Rect()
        paintText.getTextBounds(text, 0, text.length, textBounds)
        val textHeight = textBounds.height().toFloat()

        extraTopPx = textHeight + extraTextTop + 4f

        canvas.save()
        canvas.translate(0f, extraTopPx)

        val widthF = width.toFloat()
        val heightF = height.toFloat() - extraTopPx

        val padding = stroke / 2f
        val capOffset = stroke / 2f

        val rectLeft = padding + capOffset
        val rectRight = widthF - padding - capOffset

        val maxArcHeight = heightF.coerceAtMost(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80f, resources.displayMetrics))
        val arcHeight = maxArcHeight

        val rectTop = heightF - arcHeight
        val rectBottom = heightF + arcHeight / 2f

        val rect = RectF(rectLeft, rectTop, rectRight, rectBottom)

        val startAngle = 180f + 30f
        val sweepAngle = 180f - 60f

        canvas.drawArc(rect, startAngle, sweepAngle, false, paintBg)
        canvas.drawArc(rect, startAngle, sweepAngle * progress, false, paintProgress)

        val path = Path()
        val sweepForPath = (sweepAngle * progress).coerceAtLeast(0.0001f)
        path.addArc(rect, startAngle, sweepForPath)

        val pm = PathMeasure(path, false)
        val pos = FloatArray(2)
        val tan = FloatArray(2)
        if (pm.length > 0f) {
            pm.getPosTan(pm.length, pos, tan)

            val rawTextX = pos[0]
            val rawTextY = pos[1] - (stroke * 2.2f)

            val angle = Math.toDegrees(atan2(tan[1].toDouble(), tan[0].toDouble())).toFloat()

            val minAllowedY = 0f + paintText.textSize / 2f + 2f
            val adjustedTextY = if (rawTextY < minAllowedY) minAllowedY else rawTextY

            canvas.save()
            canvas.rotate(angle, rawTextX, adjustedTextY)
            canvas.drawText(bpmValue.toString(), rawTextX, adjustedTextY, paintText)
            canvas.restore()
        }

        canvas.restore()
    }
}
