package com.example.loadlensai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<Box> = listOf()

    // 1. 붓(Paint) 설정: 빨간색, 테두리만, 굵게!
    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE // 채우기 말고 테두리만
        strokeWidth = 10f // 아주 굵게 (잘 보이게)
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        style = Paint.Style.FILL
    }

    private val textBackgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        alpha = 160 // 반투명
    }

    // 데이터 클래스 (MainActivity랑 맞춰야 함)
    data class Box(
        val rect: RectF,
        val label: String,
        val score: Float
    )

    // 데이터를 받아서 화면 갱신 요청
    fun setResults(detectionResults: List<Box>) {
        this.results = detectionResults
        // 여기서도 invalidate를 호출해서 확실하게 갱신
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (box in results) {
            // 🔥 [핵심 수정] 라벨 이름에 따라 색깔 바꾸기
            if (box.label.contains("Overload", ignoreCase = true)) {
                // 과적이면 빨간색 🔴
                boxPaint.color = Color.RED
                textBackgroundPaint.color = Color.RED
            } else {
                // 정상이면 초록색 🟢 (일반 차량 포함)
                boxPaint.color = Color.GREEN
                textBackgroundPaint.color = Color.GREEN
            }

            // -------------------------------------------------

            // 1. 박스 그리기
            canvas.drawRect(box.rect, boxPaint)

            // 2. 글씨 배경 그리기
            val text = "${box.label} ${(box.score * 100).toInt()}%"
            val textWidth = textPaint.measureText(text)
            val textHeight = textPaint.textSize

            canvas.drawRect(
                box.rect.left,
                box.rect.top - textHeight - 10f,
                box.rect.left + textWidth + 20f,
                box.rect.top,
                textBackgroundPaint
            )

            // 3. 글씨 쓰기
            canvas.drawText(
                text,
                box.rect.left + 10f,
                box.rect.top - 10f,
                textPaint
            )
        }
    }
}