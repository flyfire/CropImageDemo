package com.solarexsoft.cropimage.animation;

/*
 * Creadted by houruhou on 2023/01/13 15:11
 */
public interface SimpleValueAnimator {
    void startAnimation(long duration);

    void cancelAnimation();

    boolean isAnimationStarted();

    void addAnimatorListener(SimpleValueAnimatorListener animatorListener);
}
