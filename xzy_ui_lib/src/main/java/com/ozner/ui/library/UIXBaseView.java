package com.ozner.ui.library;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

/**
 * Created by zhiyongxu on 15/9/16.
 */
public abstract class UIXBaseView extends View implements AnimatorListener {
    Animator[] _animation;
    int _step = 0;

    public UIXBaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean isAnnmatorRuning() {
        return _animation != null;
    }

    public abstract Animator[] getAnimation(int step);

    public abstract int getAnimationCount();


    public void startAnimation() {
        if (isAnnmatorRuning()) return;
        _step = 0;
        initAnimator();
        invalidate();
    }

    private void initAnimator() {
        _animation = getAnimation(_step);
        if (_animation == null) return;
        for (Animator animator : _animation) {
            animator.addListener(this);
            animator.start();
        }
    }

    protected int getStep() {
        return _step;
    }

    public void cancelAnimation() {
        if (_animation != null) {
            for (Animator animator : _animation) {
                animator.cancel();
            }
        }
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (_step >= getAnimationCount() - 1) {
            _animation = null;
            invalidate();
            return;
        }
        _step++;
        initAnimator();
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        _animation = null;
        invalidate();
    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    protected float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    protected float spToPx(float spValue) {
        final float fontScale = getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }
}
