package com.softbankrobotics.pepperpointat

import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.`object`.actuation.Animate
import com.aldebaran.qi.sdk.`object`.actuation.Frame
import com.aldebaran.qi.sdk.builder.AnimateBuilder
import com.aldebaran.qi.sdk.builder.AnimationBuilder
import kotlin.math.*

enum class PointAtAnimation {
    CLOSE_FRONT_LEFT,
    CLOSE_FRONT_RIGHT,
    CLOSE_MEDIUM_LEFT,
    CLOSE_MEDIUM_RIGHT,
    CLOSE_HALF_LEFT,
    CLOSE_HALF_RIGHT,
    FRONT_LEFT,
    FRONT_RIGHT,
    MEDIUM_LEFT,
    MEDIUM_RIGHT,
    HALF_LEFT,
    HALF_RIGHT
}

class PointAtAnimator(context: QiContext) {

    private val qiContext = context

    fun playPointAnimation(pointAtAnimation: PointAtAnimation): Future<Void> {
        return pointAtAnimateBuilder(pointAtAnimation).andThenCompose {
            it.async().run()
        }
    }

    private fun pointAtAnimateBuilder(pointAtAnimation: PointAtAnimation): Future<Animate> {
        val animName = when (pointAtAnimation) {
            PointAtAnimation.CLOSE_FRONT_LEFT -> "PointShortFrontL.qianim"
            PointAtAnimation.CLOSE_FRONT_RIGHT -> "PointShortFrontR.qianim"
            PointAtAnimation.CLOSE_MEDIUM_LEFT -> "PointShortMediumL.qianim"
            PointAtAnimation.CLOSE_MEDIUM_RIGHT -> "PointShortMediumR.qianim"
            PointAtAnimation.CLOSE_HALF_LEFT -> "PointShortHalfL.qianim"
            PointAtAnimation.CLOSE_HALF_RIGHT -> "PointShortHalfR.qianim"
            PointAtAnimation.FRONT_LEFT -> "PointFrontL.qianim"
            PointAtAnimation.FRONT_RIGHT -> "PointFrontR.qianim"
            PointAtAnimation.MEDIUM_LEFT -> "PointMediumL.qianim"
            PointAtAnimation.MEDIUM_RIGHT -> "PointMediumR.qianim"
            PointAtAnimation.HALF_LEFT -> "PointHalfL.qianim"
            PointAtAnimation.HALF_RIGHT -> "PointHalfR.qianim"
        }

        return AnimationBuilder.with(qiContext)
            .withAssets(animName)
            .buildAsync()
            .andThenCompose {
                AnimateBuilder.with(qiContext)
                    .withAnimation(it)
                    .buildAsync()
            }
    }

    fun pointAt(targetFrame: Frame): Future<Void> {
        return qiContext.actuation.async().robotFrame().andThenCompose {
            val transformTime = targetFrame.computeTransform(it)
            val transform = transformTime.transform
            val translation = transform.translation
            val x = translation.x
            val y = translation.y
            val distance = sqrt(x * x + y * y)
            val angle = atan2(y, x) * 180 / PI

            if (x >= 0) {
                handleTargetFrameInFront(distance, angle)
            } else {
                handleTargetFrameBehind(distance, x, y)
            }
        }
    }

    private fun handleTargetFrameInFront(
        distance: Double,
        angle: Double
    ): Future<Void> {
        val pointAtAnimateFuture: Future<Void>

        if (distance <= 3) {
            pointAtAnimateFuture = if (angle <= 0 && angle > -22) {
                playPointAnimation(PointAtAnimation.CLOSE_FRONT_RIGHT)
            } else if (angle >= 0 && angle < 22) {
                playPointAnimation(PointAtAnimation.CLOSE_FRONT_LEFT)
            } else if (angle <= -22 && angle >= -68) {
                playPointAnimation(PointAtAnimation.CLOSE_MEDIUM_RIGHT)
            } else if (angle >= 22 && angle <= 68) {
                playPointAnimation(PointAtAnimation.CLOSE_MEDIUM_LEFT)
            } else if (angle < -22 && angle >= -90) {
                playPointAnimation(PointAtAnimation.CLOSE_HALF_RIGHT)
            } else {
                playPointAnimation(PointAtAnimation.CLOSE_HALF_LEFT)
            }
        } else {
            pointAtAnimateFuture = if (angle <= 0 && angle > -22) {
                playPointAnimation(PointAtAnimation.FRONT_RIGHT)
            } else if (angle >= 0 && angle < 22) {
                playPointAnimation(PointAtAnimation.FRONT_LEFT)
            } else if (angle <= -22 && angle >= -68) {
                playPointAnimation(PointAtAnimation.MEDIUM_RIGHT)
            } else if (angle >= 22 && angle <= 68) {
                playPointAnimation(PointAtAnimation.MEDIUM_LEFT)
            } else if (angle < -22 && angle >= -90) {
                playPointAnimation(PointAtAnimation.HALF_RIGHT)
            } else {
                playPointAnimation(PointAtAnimation.HALF_LEFT)
            }
        }

        return pointAtAnimateFuture
    }

    private fun handleTargetFrameBehind(
        distance: Double,
        x: Double,
        y: Double
    ): Future<Void> {
        // Build coordinates of the Frame at 90° of the target Frame
        val frameToAlignX: Double
        val frameToAlignY: Double
        // Behind left
        if (y >= 0) {
            frameToAlignX = y
            frameToAlignY = -x
        }
        // Behind right
        else {
            frameToAlignX = -y
            frameToAlignY = x
        }

        // Create the rotation Animates
        val theta = atan(frameToAlignY / frameToAlignX)
        val rotationAnimate = buildRotationAnimate(theta)
        val backToInitialPositionAnimate = buildRotationAnimate(-theta)

        // Create the point at Animate
        val pointAtAnimateFuture = if (y >= 0) {
            if (distance > 5) {
                playPointAnimation(PointAtAnimation.HALF_LEFT)
            } else {
                playPointAnimation(PointAtAnimation.CLOSE_HALF_LEFT)
            }
        } else {
            if (distance > 5) {
                playPointAnimation(PointAtAnimation.HALF_RIGHT)
            } else {
                playPointAnimation(PointAtAnimation.CLOSE_HALF_RIGHT)
            }
        }

        return rotationAnimate.async().run().andThenCompose {
            pointAtAnimateFuture.andThenCompose {
                backToInitialPositionAnimate.async().run()
            }
        }
    }

    private fun buildRotationAnimate(theta: Double): Animate {
        val timeMax = 5.0
        val angleMax = Math.PI
        val duration = abs(theta) / angleMax * timeMax
        val animationString =
            String.format(
                "[\"Holonomic\", [\"Line\", [%f, %f]], %f, %f]",
                0.0,
                0.0,
                theta,
                duration
            )

        val animation = AnimationBuilder.with(qiContext)
            .withTexts(animationString)
            .build()

        return AnimateBuilder.with(qiContext)
            .withAnimation(animation)
            .build()
    }
}