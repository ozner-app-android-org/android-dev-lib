package com.ozner.ui.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/**
 * Created by xzy on 2015/5/14.
 */
public class RoundDrawable extends Drawable {
    Bitmap mOutputBitmap = null;
    Paint mPaint = new Paint();
    Context mContext;
    String mText = "";
    int mImageSize = 0;
    int mTextWidth = 0;
    float mLeftPadding = 0;
    float mTextPadding = 0;

    public void setText(String text) {
        mText = text;
        //paint.setFontFeatureSettings(F);
        //paint.setFontFeatureSettings(F);
        mTextWidth = (int) mPaint.measureText(text);
    }

    public RoundDrawable(Context context) {
        mContext = context;
        mImageSize = Screen.dip2px(mContext, 42);
        mLeftPadding = Screen.dip2px(mContext, 8);
        mTextPadding = mContext.getResources().getDimension(R.dimen.abc_action_bar_icon_vertical_padding_material);
        mPaint.setTextSize(mContext.getResources().getDimension(R.dimen.abc_text_size_title_material_toolbar));
        Typeface font = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        mPaint.setTypeface(font);
        mPaint.setAntiAlias(true);
        mPaint.setTextAlign(Paint.Align.LEFT);
        mPaint.setColor(Color.WHITE);
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        mOutputBitmap = Bitmap.createBitmap(mImageSize, mImageSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mOutputBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        paint.setColor(Color.GRAY);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(canvas.getWidth() / 2,
                canvas.getHeight() / 2,
                canvas.getWidth() / 2,
                paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        Matrix matrix = new Matrix();
        matrix.postScale(canvas.getWidth() * 1.01f / bitmap.getWidth(), canvas.getHeight() * 1.01f / bitmap.getHeight(), bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        matrix.postTranslate((canvas.getWidth() - bitmap.getWidth()) / 2, (canvas.getHeight() - bitmap.getHeight()) / 2);
        //matrix.postScale((float)canvas.getWidth()/mBitmap.getWidth(),(float)canvas.getHeight()/mBitmap.getHeight());
        canvas.drawBitmap(bitmap, matrix, paint);
    }

    @Override
    public int getIntrinsicHeight() {
        return mImageSize;
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) (mLeftPadding + mImageSize + mTextPadding + mTextWidth);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
    }

    @Override
    public void draw(Canvas canvas) {
        if (mOutputBitmap != null) {
            float x = mLeftPadding;
            float y = 0;
            canvas.drawBitmap(mOutputBitmap, new Rect(0, 0, mOutputBitmap.getWidth(), mOutputBitmap.getHeight()),
                    new RectF(x, y, x + mOutputBitmap.getWidth(), y + mOutputBitmap.getHeight()),
                    mPaint);
            Rect targetRect = getBounds();
            Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
            int baseline = targetRect.top + (targetRect.bottom - targetRect.top - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
            // 下面这行是实现水平居中，drawText对应改为传入targetRect.centerX()
            canvas.drawText(mText, x + mImageSize + mTextPadding, baseline, mPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return 0;
    }
}
