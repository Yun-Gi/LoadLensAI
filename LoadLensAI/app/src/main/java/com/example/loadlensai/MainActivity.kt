package com.example.loadlensai

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var overlayView: OverlayView
    private lateinit var detector: Detector
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlayView = findViewById(R.id.overlayView)

        // 1. AI 모델 초기화
        detector = Detector(this, "yolov8.tflite", "labels.txt")
        try {
            detector.setup()
        } catch (e: Exception) {
            Toast.makeText(this, "모델 로딩 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // 2. 카메라 권한 체크 후 실행
        if (checkPermission()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            val viewFinder = findViewById<PreviewView>(R.id.viewFinder)
            preview.setSurfaceProvider(viewFinder.surfaceProvider)

            // 이미지 분석기 (AI에게 화면을 보내주는 역할)
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(viewFinder.display.rotation) // 👈 이 줄 추가! (중요)
                .build()

            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                val bitmap = imageProxy.toBitmap()
                if (bitmap != null) {
                    // 회전 문제 해결 (세로 모드)
                    val matrix = Matrix()
                    matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                    // 추론 실행
                    val results = detector.detect(rotatedBitmap)
                    // 디버깅용
//                    android.util.Log.d("AI_CHECK", "--------------------------------")
//                    android.util.Log.d("AI_CHECK", "감지된 개수: ${results.size}")
//                    results.forEach {
//                        android.util.Log.d("AI_CHECK", "물체: ${it.label}, 점수: ${it.score}, 좌표: ${it.rect}")
//                    }
                    //----
                    // 화면 업데이트 (메인 스레드에서)
                    runOnUiThread {
                        // ⚠️ 수정된 로직: 0~1 사이의 좌표를 화면 크기로 뻥튀기(Scale)
                        val scaledResults = results.map { box ->
                            val scaledRect = android.graphics.RectF(
                                box.rect.left * overlayView.width,   // 가로 위치 = 0.35 * 화면너비
                                box.rect.top * overlayView.height,   // 세로 위치 = 0.27 * 화면높이
                                box.rect.right * overlayView.width,
                                box.rect.bottom * overlayView.height
                            )
                            OverlayView.Box(scaledRect, box.label, box.score)
                        }

                        // 갱신 명령
                        overlayView.setResults(scaledResults)
                        overlayView.invalidate()
                    }
                }
                imageProxy.close()
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                Toast.makeText(this, "카메라 시작 실패", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ImageProxy -> Bitmap 변환 함수
    private fun ImageProxy.toBitmap(): Bitmap? {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    // 권한 관련 코드
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) startCamera()
        else Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}
