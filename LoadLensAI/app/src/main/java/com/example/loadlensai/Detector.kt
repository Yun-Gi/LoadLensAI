package com.example.loadlensai

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.BufferedReader
import java.io.InputStreamReader

class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String,
    private val threshold: Float = 0.5f // 50% 이상만 감지
) {
    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()

    // YOLO 입력 크기 (640x640)
    private val tensorWidth = 640
    private val tensorHeight = 640

    fun setup() {
        val model = FileUtil.loadMappedFile(context, modelPath)
        val options = Interpreter.Options()
        interpreter = Interpreter(model, options)

        // 라벨 읽기
        val reader = BufferedReader(InputStreamReader(context.assets.open(labelPath)))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            labels.add(line!!)
        }
    }

    fun detect(bitmap: Bitmap): List<OverlayView.Box> {
        if (interpreter == null) return emptyList()

        // 1. 이미지 전처리 (640x640 리사이징 + 정규화)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(tensorHeight, tensorWidth, ResizeOp.ResizeMethod.BILINEAR))
            .add(CastOp(DataType.FLOAT32))
            .add(NormalizeOp(0f, 255f)) // 0~255 값을 0~1로 변환
            .build()

        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. 추론 실행
        // Output Shape: [1, 10, 8400] (4개 좌표 + 6개 클래스 = 10줄)
        val output = Array(1) { Array(10) { FloatArray(8400) } }
        interpreter?.run(tensorImage.buffer, output)

        // 3. 결과 해석 (NMS 포함)
        val boxes = mutableListOf<OverlayView.Box>()
        val outputArray = output[0] // [10][8400]

        // 8400개 박스를 전부 확인
        for (i in 0 until 8400) {
            // 확률(Score)이 가장 높은 클래스 찾기
            var maxScore = 0f
            var maxClassIndex = -1

            // 4번 인덱스부터 9번 인덱스까지가 클래스 확률 (0~3은 좌표)
            for (c in 0 until 6) {
                val score = outputArray[4 + c][i]
                if (score > maxScore) {
                    maxScore = score
                    maxClassIndex = c
                }
            }

            if (maxScore > threshold) {
                // 좌표 계산 (중심점 -> 좌상단 좌표)
                val x = outputArray[0][i]
                val y = outputArray[1][i]
                val w = outputArray[2][i]
                val h = outputArray[3][i]

                val left = (x - w / 2) * bitmap.width / tensorWidth
                val top = (y - h / 2) * bitmap.height / tensorHeight
                val right = (x + w / 2) * bitmap.width / tensorWidth
                val bottom = (y + h / 2) * bitmap.height / tensorHeight

                val rect = android.graphics.RectF(left, top, right, bottom)
                boxes.add(OverlayView.Box(rect, labels[maxClassIndex], maxScore))
            }
        }

        return nms(boxes) // 겹치는 박스 제거
    }

    // NMS (겹치는 박스 중 제일 좋은 것만 남기기)
    private fun nms(boxes: List<OverlayView.Box>): List<OverlayView.Box> {
        val sorted = boxes.sortedByDescending { it.score }.toMutableList()
        val selected = mutableListOf<OverlayView.Box>()

        while (sorted.isNotEmpty()) {
            val first = sorted.removeAt(0)
            selected.add(first)
            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (iou(first.rect, next.rect) > 0.5f) { // 겹치는 비율이 50% 넘으면 제거
                    iterator.remove()
                }
            }
        }
        return selected
    }

    // IoU (겹치는 비율 계산)
    private fun iou(a: android.graphics.RectF, b: android.graphics.RectF): Float {
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        val interLeft = kotlin.math.max(a.left, b.left)
        val interTop = kotlin.math.max(a.top, b.top)
        val interRight = kotlin.math.min(a.right, b.right)
        val interBottom = kotlin.math.min(a.bottom, b.bottom)
        val interArea = kotlin.math.max(0f, interRight - interLeft) * kotlin.math.max(0f, interBottom - interTop)
        return interArea / (areaA + areaB - interArea)
    }
}