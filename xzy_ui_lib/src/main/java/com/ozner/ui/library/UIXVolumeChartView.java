package com.ozner.ui.library;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by xzyxd on 2015/9/17.
 */
public class UIXVolumeChartView extends UIXChartView {

    float ani_value = 0;

    public UIXVolumeChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAdapter(testAdapter);
        this.setMinimumHeight(Screen.dip2px(getContext(), 30));
    }

    ChartAdapter testAdapter=new ChartAdapter() {
        int[] data;
        @Override
        protected void init() {
            data=new int[20];
            Random random=new Random();
            for (int i=0;i<data.length;i++)
            {
                data[i]=random.nextInt(2000);
            }
            super.init();
        }

        public int count() {
            return data.length;
        }

        @Override
        public int getValue(int Index) {
            return data[Index];
        }

        @Override
        public int getMax() {
            return 2000;
        }

        @Override
        public ViewMode getViewMode() {
            return ViewMode.Day;
        }
    };

    @Override
    protected void init() {
        super.init();
        valueTag.put(500, "500\n" +
                "ml");
        valueTag.put(1000, "1000");
        valueTag.put(1500, "1500");
        valueTag.put(2000, "2000");
    }

    @Override
    public void onAnimationStart(Animator animation) {
        if (getStep() == 0) {
            ani_value = 0;
            this.invalidate();
        }
        super.onAnimationStart(animation);
    }

    @Override
    public Animator[] getAnimation(int step) {
        ArrayList<ValueAnimator> animator = new ArrayList<>();
        if (adapter == null) return null;
        if (step == 0) {
            float sum = 0;
            for (int i = 0; i < adapter.count(); i++) {
                sum += adapter.getValue(i);
            }
            ValueAnimator animatory = ValueAnimator.ofFloat(0, sum);
            animatory.setDuration(1200);
            animatory.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    ani_value = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            animator.add(animatory);

        }
        return animator.toArray(new Animator[0]);
    }

    @Override
    public int getAnimationCount() {
        return 1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            this.startAnimation();
        }
        return super.onTouchEvent(event);
    }


    private void drawBar(Canvas canvas) {
        if (adapter == null) return;
        float width = dpToPx(5);
        int count = adapter.count();
        int maxValue = adapter.getMax();
        float x = 0;
        float y = 0;
        int startColor = 0xff5c87ef;
        int endColor = 0xff89b3ff;
        Paint barPaint = new Paint();
        barPaint.setAntiAlias(true);
        barPaint.setStrokeCap(Paint.Cap.ROUND);
        barPaint.setStrokeWidth(width);
        barPaint.setColor(Color.BLUE);
        barPaint.setStyle(Paint.Style.FILL);
        float sum = 0;
        for (int i = 0; i < count; i++) {
            float value = adapter.getValue(i);
            if (isAnnmatorRuning()) {
                if (sum >= ani_value)
                    break;
                if (value > ani_value - sum) {
                    value = ani_value - sum;
                }
                if (value > maxValue) {
                    value = maxValue;
                }
            }

            x = getPostionByIndex(i);
            y = getPostionByValue(value);
            barPaint.setShader(new LinearGradient(x, valueRect.bottom, x + width, valueRect.top,
                    startColor, endColor, Shader.TileMode.MIRROR));

            canvas.drawLine(x, y, x, getPostionByValue(0), barPaint);
            sum += value;
        }
        //canvas.drawPath(linePath,barPaint);
    }


    @Override
    protected void drawValue(Canvas canvas) {
        drawBar(canvas);

    }
}
