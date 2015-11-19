package com.ozner.ui.library;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by xzyxd on 2015/9/17.
 */
public class TDSChartView extends UIXChartView {

    private final static int LineColor50 = 0xff5591fb;
    private final static int LineColor200 = 0xffa28aea;
    private final static int LineColor400 = 0xffec4756;
    private final static int SharpColor50 = Color.WHITE;
    private final static int SharpColor200 = 0xffeadbf0;
    private final static int SharpColor400 = 0xfff29599;
    private final static int SharpAlpha = 180;
    float ani_y_rate = 0;
    float ani_x_rate = 0;
    int ani_sharp_alpha = 0;

    public TDSChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setAdapter(testAdapter);
        this.setMinimumHeight(Screen.dip2px(getContext(), 30));
    }

    @Override
    protected void init() {
        super.init();
        valueTag.put(50, getResources().getString(R.string.good_water));
        valueTag.put(200, getResources().getString(R.string.normal_water));
        valueTag.put(400, getResources().getString(R.string.bad_water));
    }


    ChartAdapter testAdapter=new ChartAdapter() {
        int[] data;
        @Override
        protected void init() {
            data=new int[20];
            Random random=new Random();
            for (int i=0;i<data.length;i++)
            {
                data[i]=random.nextInt(400);
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
            return 400;
        }

        @Override
        public ViewMode getViewMode() {
            return ViewMode.Day;
        }
    };

    @Override
    public void onAnimationStart(Animator animation) {
        if (getStep() == 0) {
            ani_y_rate = 0;
            ani_sharp_alpha = 0;
            ani_x_rate = x_rate;
            this.invalidate();
        }
        super.onAnimationStart(animation);
    }

    @Override
    public Animator[] getAnimation(int step) {
        ArrayList<ValueAnimator> animator = new ArrayList<>();
        if (adapter == null) return null;
        if (step == 0) {
            ValueAnimator animatory = ValueAnimator.ofFloat(0, y_rate);
            animatory.setDuration(800);
            animatory.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    ani_y_rate = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            animator.add(animatory);

            /*ValueAnimator animatorx = ValueAnimator.ofFloat(0, x_rate);
            animatorx.setDuration(1000);
            animatorx.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    ani_x_rate = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            animator.add(animatorx);*/

            ValueAnimator animator_a = ValueAnimator.ofInt(0, SharpAlpha);
            animator_a.setDuration(800);
            animator_a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    ani_sharp_alpha = (int) animation.getAnimatedValue();
                    invalidate();
                }
            });
            animator.add(animator_a);
        }
        return animator.toArray(new Animator[0]);
    }

    @Override
    public int getAnimationCount() {
        return 1;
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            this.startAnimation();
//        }
//        return super.onTouchEvent(event);
//    }

    private Shader getLineShare() {
        return new LinearGradient(0, valueRect.bottom, 0, valueRect.top,
                new int[]{LineColor50, LineColor200, LineColor400},
                new float[]{0f,
                        //中间颜色起点
                        Math.abs(getPostionByValue((int) valueTag.keySet().toArray()[1]) - valueRect.bottom) / valueRect.height()
                        , Math.abs(getPostionByValue((int) valueTag.keySet().toArray()[2]) - valueRect.bottom) / valueRect.height()}
                , Shader.TileMode.CLAMP);
    }

    private float valueToPostion(float value) {
        return valueRect.bottom - value * (isAnnmatorRuning() ? ani_y_rate : y_rate);
    }

    private float indexToPostion(int index) {
        return valueRect.left + index * (isAnnmatorRuning() ? ani_x_rate : x_rate);
    }

    private void drawLine(Canvas canvas) {
        if (adapter == null) return;
        int count = adapter.count();
        int maxValue = adapter.getMax();
        Path linePath = new Path();

        float x = 0;
        float y = 0;


        for (int i = 0; i < count; i++) {
            int value = adapter.getValue(i);
            if (value > maxValue) {
                value = maxValue;
            }
            x = indexToPostion(i);
            y = valueToPostion(value);
            if (i == 0) {
                linePath.moveTo(x, y);
                continue;
            }
            linePath.lineTo(x, y);

        }
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setShader(getLineShare());
        paint.setStrokeWidth(dpToPx(1));

        canvas.drawPath(linePath, paint);
    }

    private void drawPoint(Canvas canvas) {
        if (adapter == null) return;
        int count = adapter.count();
        int maxValue = adapter.getMax();
        Path pointPath = new Path();
        Path pointPath2 = new Path();
        float potSize = dpToPx(3);
        float potSize2 = dpToPx(2);
        float x = 0;
        float y = 0;
        for (int i = 0; i < count; i++) {
            int value = adapter.getValue(i);
            if (value > maxValue) {
                value = maxValue;
            }
            x = indexToPostion(i);
            y = valueToPostion(value);
            pointPath.addCircle(x, y, potSize, Path.Direction.CCW);
            pointPath2.addCircle(x, y, potSize2, Path.Direction.CCW);
        }
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(getLineShare());
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawPath(pointPath, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        paint.setColor(Color.WHITE);
        canvas.drawPath(pointPath2, paint);

    }


    private void drawSharp(Canvas canvas) {
        if (adapter==null) return;

        int count = adapter.count();
        if (count <= 0) return;
        int maxValue = adapter.getMax();
        Path path = new Path();
        float x = 0;
        float y = 0;
        float firstY = 0;
        for (int i = 0; i < count; i++) {
            int value = adapter.getValue(i);
            if (value > maxValue) {
                value = maxValue;
            }
            x = indexToPostion(i);
            y = valueToPostion(value);
            if (i == 0) {
                firstY = y;
                path.moveTo(x, y);
            } else
                path.lineTo(x, y);
        }
        path.lineTo(x, valueRect.bottom);
        path.lineTo(valueRect.left, valueRect.bottom);
        path.lineTo(valueRect.left, firstY);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, valueRect.bottom, 0, valueRect.top,
                new int[]{SharpColor50, SharpColor200, SharpColor400},
                new float[]{0f,
                        //中间颜色起点
                        Math.abs(getPostionByValue((int) valueTag.keySet().toArray()[1]) - valueRect.bottom) / valueRect.height()
                        , Math.abs(getPostionByValue((int) valueTag.keySet().toArray()[2]) - valueRect.bottom) / valueRect.height()}
                , Shader.TileMode.CLAMP));

        paint.setAlpha(isAnnmatorRuning() ? ani_sharp_alpha : SharpAlpha);
        canvas.drawPath(path, paint);

        /*paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));

        RectF rect_l=new RectF(valueRect.left,getPostionByValue((int)valueTag.keySet().toArray()[0]),
                valueRect.right,valueRect.bottom);
        paint.setColor(Color.WHITE);
        canvas.drawRect(rect_l, paint);

        RectF rect_m=new RectF(valueRect.left,getPostionByValue((int)valueTag.keySet().toArray()[1]),
                valueRect.right,getPostionByValue((int)valueTag.keySet().toArray()[0]));
        paint.setShader(new LinearGradient(0,rect_m.bottom , 0,rect_m.top,
                Color.WHITE,   0xffeadbf0, Shader.TileMode.REPEAT));
        canvas.drawRect(rect_m, paint);

        RectF rect_h=new RectF(valueRect.left,getPostionByValue((int)valueTag.keySet().toArray()[2]),
                valueRect.right,getPostionByValue((int)valueTag.keySet().toArray()[1]));
        paint.setShader(new LinearGradient(0,rect_h.bottom , 0,rect_h.top,
                0xffeadbf0,   0xfff29599, Shader.TileMode.REPEAT));
        canvas.drawRect(rect_h, paint);*/

    }

    @Override
    protected void drawValue(Canvas canvas) {
        drawSharp(canvas);
        drawLine(canvas);
        drawPoint(canvas);

    }
}
