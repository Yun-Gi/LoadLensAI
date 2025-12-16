package com.example.loadlensai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.util.LinkedList

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val results = LinkedList<Box>()
    private val boxPaint = Paint()
    private val textPaint = Paint()

    init {
        // 박스 스타일 (빨간색 테두리)
        boxPaint.color = Color.RED
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 8f

        // 글자 스타일 (흰색, 굵게)
        textPaint.color = Color.WHITE
        textPaint.textSize = 50f
        textPaint.style = Paint.Style.FILL
        textPaint.isFakeBoldText = true
    }

    fun setResults(boxes: List<Box>) {
        results.clear()
        results.addAll(boxes)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (box in results) {
            canvas.drawRect(box.rect, boxPaint)
            val text = "${box.label} ${String.format("%.1f%%", box.score * 100)}"
            canvas.drawText(text, box.rect.left, box.rect.top - 10f, textPaint)
        }
    }

    data class Box(
        val rect: RectF,
        val label: String,
        val score: Float
    )
}