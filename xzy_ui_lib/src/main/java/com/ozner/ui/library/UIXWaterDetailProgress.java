package com.ozner.ui.library;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by zhiyongxu on 15/9/15.
 */
public class UIXWaterDetailProgress extends UIXBaseView {
    private final static int lineStartColor1 = 0xfff0a3a6;
    private final static int lineStartColor2 = 0xffab94eb;
    private final static int lineStartColor3 = 0xff94b8f2;
    private final static int lineEndColor1 = 0xffec4b54;
    private final static int lineEndColor2 = 0xff6b3ee1;
    private final static int lineEndColor3 = 0xff3677ef;
    private final static int _ani_duration = 300;
    float _lineWidth = 0;
    float _space;
    private int _bad_progress = 40;
    private int _normal_progress = 35;
    private int _good_progress = 25;
    private int _ani_bad_progress = 0;
    private int _ani_normal_progress = 0;
    private int _ani_good_progress = 0;
    private int _ani_bad_alpha = 0;
    private int _ani_normal_alpha = 0;
    private int _ani_good_alpha = 0;
    ValueAnimator.AnimatorUpdateListener animatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int value = (Integer) animation.getAnimatedValue();
            switch (getStep()) {
                case 0:
                    _ani_bad_alpha = value;
                    break;
                case 1:
                    _ani_bad_progress = value < _bad_progress ? value : _bad_progress;
                    break;
                case 2:
                    _ani_normal_alpha = value;
                    break;
                case 3:
                    _ani_normal_progress = value < _normal_progress ? value : _normal_progress;
                    break;
                case 4:
                    _ani_good_alpha = value;
                    break;
                case 5:
                    _ani_good_progress = value < _good_progress ? value : _good_progress;
                    break;
            }

            invalidate();
        }
    };

    public UIXWaterDetailProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        _lineWidth = dpToPx(12);
        _space = dpToPx(6);

    }

    public int bad_progress() {
        return _bad_progress;
    }

    public int normal_progress() {
        return _normal_progress;
    }

    public int good_progress() {
        return _good_progress;
    }

    public void set_bad_progress(int _bad_progress) {
        this._bad_progress = _bad_progress;
    }

    public void set_normal_progress(int _normal_progress) {
        this._normal_progress = _normal_progress;
    }

    public void set_good_progress(int _good_progress) {
        this._good_progress = _good_progress;
    }

    public void update(int bad, int normal, int good) {
        _bad_progress = bad;
        _normal_progress = normal;
        _good_progress = good;
        invalidate();
    }

    @Override
    public int getAnimationCount() {
        return 6;
    }

    @Override
    public Animator[] getAnimation(int step) {
        ValueAnimator animator;
        switch (step) {
            case 0:
                animator = ValueAnimator.ofInt(50, 255);
                animator.setDuration(100);
                break;
            case 1:
                animator = ValueAnimator.ofInt(0, _bad_progress);
                animator.setDuration(_ani_duration);
                break;
            case 2:
                animator = ValueAnimator.ofInt(50, 255);
                animator.setDuration(100);
                break;
            case 3:
                animator = ValueAnimator.ofInt(0, _normal_progress);
                animator.setDuration(_ani_duration);
                break;
            case 4:
                animator = ValueAnimator.ofInt(50, 255);
                animator.setDuration(100);
                break;
            case 5:
                animator = ValueAnimator.ofInt(0, _good_progress);
                animator.setDuration(_ani_duration);
                break;
            default:
                return null;
        }
        animator.addUpdateListener(animatorUpdateListener);
        return new Animator[]{animator};
    }

    @Override
    public void onAnimationStart(Animator animation) {
        if (getStep() == 0) {
            _ani_bad_progress = 0;
            _ani_normal_progress = 0;
            _ani_good_progress = 0;
            _ani_bad_alpha = 0;
            _ani_good_alpha = 0;
            _ani_normal_alpha = 0;
        }
        super.onAnimationStart(animation);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startAnimation();
        }
        return super.onTouchEvent(event);
    }

    private void drawBackgroundLine(RectF rect, Canvas canvas) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(0xffe7ecf8);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(_lineWidth);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(new RectF(rect.left + _lineWidth / 2, rect.top + _lineWidth / 2,
                        rect.right - _lineWidth / 2, rect.bottom - _lineWidth / 2),
                -90, 270, false, paint);
    }

    private void drawValueLine(RectF rect, Canvas canvas, int startColor, int endColor, float Value) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(startColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(_lineWidth);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                startColor, endColor, Shader.TileMode.REPEAT));


        canvas.drawArc(new RectF(rect.left + _lineWidth / 2, rect.top + _lineWidth / 2,
                        rect.right - _lineWidth / 2, rect.bottom - _lineWidth / 2),
                -90, 270 * (Value / 100.0f), false, paint);
    }


    protected void onDrawBackground(Canvas canvas) {
        float space = dpToPx(6);
        float offset = 0;

        drawBackgroundLine(new RectF(offset, offset, this.getWidth() - offset, this.getHeight() - offset), canvas);

        offset += space + _lineWidth;
        drawBackgroundLine(new RectF(offset, offset, this.getWidth() - offset, this.getHeight() - offset), canvas);

        offset += space + _lineWidth;
        drawBackgroundLine(new RectF(offset, offset, this.getWidth() - offset, this.getHeight() - offset), canvas);
    }


    protected void onDrawValue(Canvas canvas) {

        float offset = 0;

        drawValueLine(new RectF(offset, offset, this.getWidth() - offset, this.getHeight() - offset),
                canvas, lineStartColor1, lineEndColor1, isAnnmatorRuning() ? _ani_bad_progress : _bad_progress);


        offset += _space + _lineWidth;
        drawValueLine(new RectF(offset, offset, this.getWidth() - offset, this.getHeight() - offset),
                canvas, lineStartColor2, lineEndColor2, isAnnmatorRuning() ? _ani_normal_progress : _normal_progress);

        offset += _space + _lineWidth;
        drawValueLine(new RectF(offset, offset, this.getWidth() - offset, this.getHeight() - offset)
                , canvas, lineStartColor3, lineEndColor3, isAnnmatorRuning() ? _ani_good_progress : _good_progress);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

    }

    protected void onDrawText(Canvas canvas) {
        float offset = _lineWidth;
        float space = dpToPx(14);


        Paint badPaint = new Paint();
        badPaint.setAntiAlias(true);
        badPaint.setColor(lineEndColor1);
        badPaint.setTextSize(spToPx(13));
        badPaint.setTextAlign(Paint.Align.RIGHT);
        if (isAnnmatorRuning()) {
            badPaint.setAlpha(_ani_bad_alpha);
        }

        Paint normalPaint = new Paint();
        normalPaint.setAntiAlias(true);
        normalPaint.setColor(lineEndColor2);
        normalPaint.setTextSize(spToPx(13));
        normalPaint.setTextAlign(Paint.Align.RIGHT);
        if (isAnnmatorRuning()) {
            normalPaint.setAlpha(_ani_normal_alpha);
        }

        Paint goodPaint = new Paint();
        goodPaint.setAntiAlias(true);
        goodPaint.setColor(lineEndColor3);
        goodPaint.setTextSize(spToPx(13));
        goodPaint.setTextAlign(Paint.Align.RIGHT);
        if (isAnnmatorRuning()) {
            goodPaint.setAlpha(_ani_good_alpha);
        }


        String text = String.format(getResources().getString(R.string.bad_water) + " %d%%", _bad_progress);
        canvas.drawText(text, this.getWidth() / 2 - space, offset, badPaint);
        offset += _space + _lineWidth;

        text = String.format(getResources().getString(R.string.normal_water) + " %d%%", _normal_progress);
        canvas.drawText(text, this.getWidth() / 2 - space, offset, normalPaint);


        offset += _space + _lineWidth;
        text = String.format(getResources().getString(R.string.good_water) + " %d%%", _good_progress);
        canvas.drawText(text, this.getWidth() / 2 - space, offset, goodPaint);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        onDrawBackground(canvas);
        onDrawValue(canvas);
        onDrawText(canvas);
    }
}
