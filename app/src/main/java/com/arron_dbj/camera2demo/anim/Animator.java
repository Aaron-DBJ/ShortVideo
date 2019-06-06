package com.arron_dbj.camera2demo.anim;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;

public class Animator {
    /**
     * 闪烁动画
     * @param view
     */
    public static void flickerAnim(View view){
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f, 1f);
        animator.setDuration(1000);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();
    }

    /**
     * 镜头旋转动画
     * @param view
     */
    public static void switchCameraAnim(View view){
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "rotation", 0f, 180f);
        animator.setDuration(1000);
        animator.start();
    }
}
