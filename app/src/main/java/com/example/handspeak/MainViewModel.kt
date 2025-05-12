package com.example.handspeak

import androidx.lifecycle.ViewModel

/**
 *  This ViewModel is used to store hand and pose landmarker helper settings
 */
class MainViewModel : ViewModel() {

    // Pose model selection
    private var _poseModel = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL

    // Delegates for both landmarkers
    private var _handDelegate: Int = HandLandmarkerHelper.DELEGATE_CPU
    private var _poseDelegate: Int = PoseLandmarkerHelper.DELEGATE_CPU

    // Hand landmarker settings
    private var _minHandDetectionConfidence: Float = HandLandmarkerHelper.DEFAULT_HAND_DETECTION_CONFIDENCE
    private var _minHandTrackingConfidence: Float = HandLandmarkerHelper.DEFAULT_HAND_TRACKING_CONFIDENCE
    private var _minHandPresenceConfidence: Float = HandLandmarkerHelper.DEFAULT_HAND_PRESENCE_CONFIDENCE
    private var _maxHands: Int = HandLandmarkerHelper.DEFAULT_NUM_HANDS

    // Pose landmarker settings
    private var _minPoseDetectionConfidence: Float = PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE
    private var _minPoseTrackingConfidence: Float = PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE
    private var _minPosePresenceConfidence: Float = PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE

    // Hand landmarker getters
    val currentHandDelegate: Int get() = _handDelegate
    val currentMinHandDetectionConfidence: Float get() = _minHandDetectionConfidence
    val currentMinHandTrackingConfidence: Float get() = _minHandTrackingConfidence
    val currentMinHandPresenceConfidence: Float get() = _minHandPresenceConfidence
    val currentMaxHands: Int get() = _maxHands

    // Pose landmarker getters
    val currentPoseDelegate: Int get() = _poseDelegate
    val currentPoseModel: Int get() = _poseModel
    val currentMinPoseDetectionConfidence: Float get() = _minPoseDetectionConfidence
    val currentMinPoseTrackingConfidence: Float get() = _minPoseTrackingConfidence
    val currentMinPosePresenceConfidence: Float get() = _minPosePresenceConfidence

    // Hand landmarker setters
    fun setHandDelegate(delegate: Int) {
        _handDelegate = delegate
    }

    fun setMinHandDetectionConfidence(confidence: Float) {
        _minHandDetectionConfidence = confidence
    }

    fun setMinHandTrackingConfidence(confidence: Float) {
        _minHandTrackingConfidence = confidence
    }

    fun setMinHandPresenceConfidence(confidence: Float) {
        _minHandPresenceConfidence = confidence
    }

    fun setMaxHands(maxResults: Int) {
        _maxHands = maxResults
    }

    // Pose landmarker setters
    fun setPoseDelegate(delegate: Int) {
        _poseDelegate = delegate
    }

    fun setPoseModel(model: Int) {
        _poseModel = model
    }

    fun setMinPoseDetectionConfidence(confidence: Float) {
        _minPoseDetectionConfidence = confidence
    }

    fun setMinPoseTrackingConfidence(confidence: Float) {
        _minPoseTrackingConfidence = confidence
    }

    fun setMinPosePresenceConfidence(confidence: Float) {
        _minPosePresenceConfidence = confidence
    }
}
