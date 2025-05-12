/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.handspeak

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var resultsHand: HandLandmarkerResult? = null
    private var resultsPose: PoseLandmarkerResult? = null
    private var handLinePaint = Paint()
    private var poseLinePaint = Paint()
    private var handPointPaint = Paint()
    private var posePointPaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    init {
        initPaints()
    }

    fun clear() {
        resultsHand = null
        resultsPose = null
        handLinePaint.reset()
        poseLinePaint.reset()
        handPointPaint.reset()
        posePointPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        // Hand landmark paints
        handLinePaint.color = Color.BLUE
        handLinePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        handLinePaint.style = Paint.Style.STROKE

        handPointPaint.color = Color.CYAN
        handPointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        handPointPaint.style = Paint.Style.FILL

        // Pose landmark paints
        poseLinePaint.color = Color.RED
        poseLinePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        poseLinePaint.style = Paint.Style.STROKE

        posePointPaint.color = Color.YELLOW
        posePointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        posePointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        
        // Draw hand landmarks
        resultsHand?.let { handLandmarkerResult ->
            for (landmark in handLandmarkerResult.landmarks()) {
                // Draw points
                for (normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        handPointPaint
                    )
                }

                // Draw connections
                HandLandmarker.HAND_CONNECTIONS.forEach {
                    canvas.drawLine(
                        landmark.get(it!!.start()).x() * imageWidth * scaleFactor,
                        landmark.get(it.start()).y() * imageHeight * scaleFactor,
                        landmark.get(it.end()).x() * imageWidth * scaleFactor,
                        landmark.get(it.end()).y() * imageHeight * scaleFactor,
                        handLinePaint
                    )
                }
            }
        }

        // Draw pose landmarks
        resultsPose?.let { poseLandmarkerResult ->
            for (landmark in poseLandmarkerResult.landmarks()) {
                // Draw points
                for (normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        posePointPaint
                    )
                }

                // Draw connections
                PoseLandmarker.POSE_LANDMARKS.forEach {
                    canvas.drawLine(
                        landmark.get(it!!.start()).x() * imageWidth * scaleFactor,
                        landmark.get(it.start()).y() * imageHeight * scaleFactor,
                        landmark.get(it.end()).x() * imageWidth * scaleFactor,
                        landmark.get(it.end()).y() * imageHeight * scaleFactor,
                        poseLinePaint
                    )
                }
            }
        }
    }

    fun setResults(
        handLandmarkerResults: HandLandmarkerResult?,
        poseLandmarkerResults: PoseLandmarkerResult?,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        resultsHand = handLandmarkerResults
        resultsPose = poseLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
    }
}
