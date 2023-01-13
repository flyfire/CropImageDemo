package com.solarexsoft.cropimage.animation;

/*
 * Creadted by houruhou on 2023/01/13 15:14
 */
public interface SimpleValueAnimatorListener {
    void onAnimationStarted();

    void onAnimationUpdate(float scale);

    void onAnimationFinished();
}
