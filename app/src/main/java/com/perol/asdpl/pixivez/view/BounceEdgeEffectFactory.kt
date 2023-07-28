package com.perol.asdpl.pixivez.view

import android.graphics.Canvas
import android.widget.EdgeEffect
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView

//from https://github.com/juanmeanwhile/BounceRecyclerView/blob/master/app/recyclerview_bounce/src/main/java/com/meanwhile/recyclerview_bounce/BounceEdgeEffectFactory.kt

/**
 * Replace edge effect by a bounce
 */
class BounceEdgeEffectFactory(
    /** The magnitude of translation distance while the list is over-scrolled. */
    val overscrollMagnitude: Float = 0.1f,
    /** The magnitude of translation distance when the list reaches the edge on fling. */
    val flingMagnitude: Float = 0.5f
) : RecyclerView.EdgeEffectFactory() {

    override fun createEdgeEffect(recyclerView: RecyclerView, direction: Int): EdgeEffect {

        return object : EdgeEffect(recyclerView.context) {

            // A reference to the [SpringAnimation] for this RecyclerView used to bring the item back after the over-scroll effect.
            var translationAnim: SpringAnimation? = null

            override fun onPull(deltaDistance: Float) {
                super.onPull(deltaDistance)
                handlePull(deltaDistance)
            }

            override fun onPull(deltaDistance: Float, displacement: Float) {
                super.onPull(deltaDistance, displacement)
                handlePull(deltaDistance)
            }

            private fun handlePull(deltaDistance: Float) {
                // This is called on every touch event while the list is scrolled with a finger.

                // Translate the recyclerView with the distance
                val sign = if (direction == DIRECTION_BOTTOM) -1 else 1
                val translationYDelta =
                    sign * recyclerView.height * deltaDistance * overscrollMagnitude
                recyclerView.translationY += translationYDelta

                translationAnim?.cancel()
            }

            override fun onRelease() {
                super.onRelease()
                // The finger is lifted. Start the animation to bring translation back to the resting state.
                if (recyclerView.translationY != 0f) {
                    translationAnim = createAnim()?.also { it.start() }
                }
            }

            override fun onAbsorb(velocity: Int) {
                super.onAbsorb(velocity)

                // The list has reached the edge on fling.
                val sign = if (direction == DIRECTION_BOTTOM) -1 else 1
                val translationVelocity = sign * velocity * flingMagnitude
                translationAnim?.cancel()
                translationAnim =
                    createAnim().setStartVelocity(translationVelocity)?.also { it.start() }
            }

            override fun draw(canvas: Canvas?): Boolean {
                // don't paint the usual edge effect
                return false
            }

            override fun isFinished(): Boolean {
                // Without this, will skip future calls to onAbsorb()
                return translationAnim?.isRunning?.not() ?: true
            }

            private fun createAnim() = SpringAnimation(recyclerView, SpringAnimation.TRANSLATION_Y)
                .setSpring(
                    SpringForce()
                        .setFinalPosition(0f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
                        .setStiffness(SpringForce.STIFFNESS_LOW)
                )

        }
    }
}