package com.masajene.qrcodereaderopencv

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.QRCodeDetector


class MainActivity : CameraActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    lateinit var mOpenCvCameraView: CameraBridgeViewBase
    lateinit var mQRCodeDetector: QRCodeDetector

    private var mRgba: Mat? = null
    private var mGray: Mat? = null

    // QRCode quadrangle points
    private var mPoints: MatOfPoint2f? = null
    private var mPointsList: MutableList<Mat> = mutableListOf()

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        /**
         * onManagerConnected
         */
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    logD("OpenCV loaded successfully")
                    setupQRCodeDetector()
                    mOpenCvCameraView.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    } // BaseLoaderCallback

    /**
     * setupQRCodeDetector
     */
    private fun setupQRCodeDetector() {
        mQRCodeDetector = QRCodeDetector()
        try {
            mPoints = MatOfPoint2f(*QUAD)
        } catch (e: UnsupportedOperationException) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        logD("called onCreate")
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        mOpenCvCameraView = findViewById(R.id.cameraView)
        mOpenCvCameraView.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView.setCvCameraViewListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            logD("Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            logD("OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onPause() {
        super.onPause()
        mOpenCvCameraView.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView.disableView()
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {}
            MotionEvent.ACTION_UP -> {}
            MotionEvent.ACTION_MOVE -> {}
            MotionEvent.ACTION_CANCEL -> {}
        }
        return false
    }

    override fun getCameraViewList(): List<CameraBridgeViewBase?> {
        return listOf(mOpenCvCameraView)
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mGray = Mat()
        mRgba = Mat()
    }

    override fun onCameraViewStopped() {
        mGray?.release()
        mRgba?.release()
        mPoints?.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat? {
        val mat = inputFrame.rgba()
        mGray = inputFrame.gray()
        val results = mutableListOf<String>()
        try {
            mQRCodeDetector.detectAndDecodeMulti(mGray, results, mPoints, mPointsList)
            Core.normalize(mat, mat, 0.0, 255.0, Core.NORM_MINMAX)
            mRgba = mat
            drawQuadrangle()
            if (results.isNotEmpty()) {
                logD(results.toString())
                showToastOnUI(results.toString())
            }

        } catch (e: CvException) {
            e.printStackTrace()
        }
        return mRgba
    }
    
    private fun drawQuadrangle() {
        mPoints?.let {

            val pointsArray = it.toArray()
            val length = pointsArray.size

            logD("pointsArray: $length")

            // return if not quad
            if (length == 0 || length % 4 != 0) return

            val points = mutableListOf<MatOfPoint>()

            for (i in 0 until length) {
                var i2 = i + 1
                val diff = if (i2 < 4) {
                    4 - i2
                } else {
                    i2 % 4
                }
                if (diff == 0) {
                    i2 -= 4
                }
                logD("diff: $diff")
                logD("Point: $i $i2")
                val pt1: Point = pointsArray[i]
                val pt2: Point = pointsArray[i2]
                logD("Point: $pt1 $i2")
                points.add(MatOfPoint(pt1, pt2))
            }
            if (points.isNotEmpty()) {
                Imgproc.polylines(mRgba, points, true, LINE_COLOR, LINE_THICKNESS)
            }
        }
    }

    private fun showToastOnUI(msg: String) {
        runOnUiThread { showToast(msg) }
    }

    private fun showToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }

    private fun logD(msg: String) {
        Log.d(TAG, msg)
    }

    companion object {
        // debug
        const val TAG = "MainActivity"
        val QUAD: Array<Point> = arrayOf(
            Point(0.0, 0.0),
            Point(0.0, 1.0),
            Point(1.0, 0.0),
            Point(1.0, 1.0)
        )
        val LINE_COLOR: Scalar = Scalar(0.0, 255.0, 0.0, 255.0)
        const val LINE_THICKNESS = 3
    }
}
