package com.example.handspeak.fragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Camera
import androidx.camera.core.AspectRatio
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.handspeak.HandLandmarkerHelper
import com.example.handspeak.PoseLandmarkerHelper
import com.example.handspeak.MainViewModel
import com.example.handspeak.R
import com.example.handspeak.databinding.FragmentCameraBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment(), HandLandmarkerHelper.LandmarkerListener, PoseLandmarkerHelper.LandmarkerListener  {

    companion object {
        private const val TAG = "CameraFragment"
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ExecutorService

    override fun onResume() {
        super.onResume()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(
                requireActivity(), R.id.fragment_container
            ).navigate(R.id.action_camera_to_permissions)
        }

        backgroundExecutor.execute {
            if (handLandmarkerHelper.isClose()) {
                handLandmarkerHelper.setupHandLandmarker()
            }
            if (poseLandmarkerHelper.isClose()) {
                poseLandmarkerHelper.setupPoseLandmarker()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if(this::handLandmarkerHelper.isInitialized && this::poseLandmarkerHelper.isInitialized) {
            viewModel.setMaxHands(handLandmarkerHelper.maxNumHands)
            viewModel.setMinHandDetectionConfidence(handLandmarkerHelper.minHandDetectionConfidence)
            viewModel.setMinHandTrackingConfidence(handLandmarkerHelper.minHandTrackingConfidence)
            viewModel.setMinHandPresenceConfidence(handLandmarkerHelper.minHandPresenceConfidence)
            viewModel.setHandDelegate(handLandmarkerHelper.currentDelegate)
            viewModel.setPoseDelegate(poseLandmarkerHelper.currentDelegate)

            backgroundExecutor.execute {
                handLandmarkerHelper.clearHandLandmarker()
                poseLandmarkerHelper.clearPoseLandmarker()
            }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding =
            FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Setup RecyclerView
        fragmentCameraBinding.detectedWordsList.layoutManager = LinearLayoutManager(context)
        // TODO: Add adapter for detected words

        // Setup buttons
        fragmentCameraBinding.clearButton.setOnClickListener {
            // TODO: Clear detected words
        }

        fragmentCameraBinding.formSentenceButton.setOnClickListener {
            // TODO: Form sentence from detected words
        }

        fragmentCameraBinding.viewFinder.post {
            setUpCamera()
        }

        backgroundExecutor.execute {
            handLandmarkerHelper = HandLandmarkerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minHandDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
                minHandTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
                minHandPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
                maxNumHands = viewModel.currentMaxHands,
                currentDelegate = viewModel.currentHandDelegate,
                handLandmarkerHelperListener = this
            )

            poseLandmarkerHelper = PoseLandmarkerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                currentModel = viewModel.currentPoseModel,
                currentDelegate = viewModel.currentPoseDelegate,
                poseLandmarkerHelperListener = this
            )
        }
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        detectFrame(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectFrame(imageProxy: ImageProxy) {
        try {
            // Convert ImageProxy to Bitmap once
            val bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }

            val matrix = Matrix().apply {
                // Rotate the frame received from the camera to be in the same direction as it'll be shown
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                // flip image if user use front camera
                if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                    postScale(-1f, 1f, imageProxy.width.toFloat() / 2, imageProxy.height.toFloat() / 2)
                }
            }
            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            // Run hand detection with the bitmap
            handLandmarkerHelper.detectLiveStream(
                bitmap = rotatedBitmap,
                isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
            )
            
            // Run pose detection with the same bitmap
            poseLandmarkerHelper.detectLiveStream(
                bitmap = rotatedBitmap,
                isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
            )
        }
        finally {
            // Close the original image after we're done with it
            imageProxy.close()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }

    // Update UI after hand have been detected. Extracts original
    // image height/width to scale and place the landmarks properly through
    // OverlayView
    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {
                val results = resultBundle.results

                results.forEachIndexed { index, result ->
                    // 1. Lấy danh sách handedness
                    val handednessList = result.handedness()

                    // 2. Lấy thông tin tay trái / tay phải
                    handednessList.forEachIndexed { i, classifications ->
                        val originalHandLabel = classifications.firstOrNull()?.categoryName() ?: "Unknown"
                        val score = classifications.firstOrNull()?.score() ?: 0f
                        
                        // Đảo ngược kết quả nếu đang dùng camera trước
                        val handLabel = if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                            if (originalHandLabel == "Left") "Right" else "Left"
                        } else {
                            originalHandLabel
                        }
                        
                        Log.d(TAG, "Hand $i is $handLabel with confidence $score")
                    }

                    // 3. Log tọa độ landmarks
                    val handLandmarks = result.landmarks()
                    handLandmarks.forEachIndexed { i, hand ->
                        Log.d(TAG, "Hand $i landmarks:")
                        hand.forEachIndexed { j, landmark ->
                            Log.d(
                                TAG,
                                "Landmark $j: x=${landmark.x()}, y=${landmark.y()}, z=${landmark.z()}, visibility=${landmark.visibility()}"
                            )
                        }
                    }
                }

                fragmentCameraBinding.overlay.setResults(
                    resultBundle.results.first(),
                    null,
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )
            }
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {
                // Get the first pose landmarks result
                val poseLandmarks = resultBundle.results.firstOrNull()
                
                // Print coordinates for each landmark
                poseLandmarks?.let { landmarks ->
                    Log.d(TAG, "Found ${landmarks.landmarks().size} poses")
                    landmarks.landmarks().forEachIndexed { poseIndex, pose ->
                        Log.d(TAG, "Pose $poseIndex landmarks:")
                        pose.forEachIndexed { index, landmark ->
                            Log.d(TAG, "Pose Landmark $index: x=${landmark.x()}, y=${landmark.y()}, z=${landmark.z()}, visibility=${landmark.visibility()}")
                        }
                    }
                }

                fragmentCameraBinding.overlay.setResults(
                    null,
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )
            }
        }
    }

    // Implement both interfaces' onError methods
    override fun onHandError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == HandLandmarkerHelper.GPU_ERROR) {
                // TODO: Handle GPU error
            }
        }
    }

    override fun onPoseError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == PoseLandmarkerHelper.GPU_ERROR) {
                // TODO: Handle GPU error
            }
        }
    }
}
