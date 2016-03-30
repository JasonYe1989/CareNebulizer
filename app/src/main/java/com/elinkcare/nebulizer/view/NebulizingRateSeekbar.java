package com.elinkcare.nebulizer.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.SeekBar;

import com.elinkcare.nebulizer.R;

/**
 * Created by Administrator on 2016/3/25.
 */
public class NebulizingRateSeekbar extends SeekBar{

    private static final String TAG = "NebulizingRateSeekbar";

    private static final int MSG_TYPE_AUTO_ALIGN = 0x01;

    private Bitmap bmp_lowrate;
    private Bitmap bmp_midrate;
    private Bitmap bmp_highrate;

    private boolean isInStracking = false;
    private boolean isInAutoAlign = false;
    private OnSeekBarChangeListener mSeekbarChangedListener;
    private OnRateChangedListener mOnRateChangedListener;

    private int mRate = 1; //0, 1, 2

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MSG_TYPE_AUTO_ALIGN:
                    autoAlign();
                    break;
            }
        }
    };

    private void init()
    {
        this.setMax(100);
        this.setProgress(50);
        this.setProgressDrawable(null);
        this.setBackground(getResources().getDrawable(R.drawable.shape_seekbar_background));
        this.setClickable(false);

        super.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (mSeekbarChangedListener != null) {
                    mSeekbarChangedListener.onProgressChanged(seekBar, progress, fromUser);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isInStracking = true;
                if (mSeekbarChangedListener != null) {
                    mSeekbarChangedListener.onStartTrackingTouch(seekBar);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                onSeekBarChanged(seekBar.getProgress());
                isInStracking = false;
                autoAlign();
                if (mSeekbarChangedListener != null) {
                    mSeekbarChangedListener.onStopTrackingTouch(seekBar);
                }
            }
        });
    }

    public static interface OnRateChangedListener
    {
        public void rateChanged(int rate);
    }

    public NebulizingRateSeekbar(Context context) {
        super(context);
        init();
    }

    public NebulizingRateSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NebulizingRateSeekbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        if(bmp_lowrate != null)
        {
            bmp_lowrate.recycle();
            bmp_highrate.recycle();
            bmp_midrate.recycle();
        }
        if(bmp_lowrate == null)
        {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(getResources(), R.drawable.bmp_lowrate, opts);
            float scale = 0.95f * (float) h / (float) opts.outHeight;

            opts.inJustDecodeBounds = false;
            //opts.inSampleSize = (int) (opts.outHeight / (h * 0.8));
            Log.e(TAG, "outWidth = " + opts.outHeight);

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);


            Bitmap temp_bmp = BitmapFactory.decodeResource(getResources(), R.drawable.bmp_lowrate, opts);
            bmp_lowrate = Bitmap.createBitmap(temp_bmp, 0, 0, temp_bmp.getWidth(), temp_bmp.getHeight(), matrix, true);
            temp_bmp.recycle();

            temp_bmp = BitmapFactory.decodeResource(getResources(), R.drawable.bmp_highrate, opts);
            bmp_highrate = Bitmap.createBitmap(temp_bmp, 0, 0, temp_bmp.getWidth(), temp_bmp.getHeight(), matrix, true);
            temp_bmp.recycle();

            temp_bmp = BitmapFactory.decodeResource(getResources(), R.drawable.bmp_midrate, opts);
            bmp_midrate = Bitmap.createBitmap(temp_bmp, 0, 0, temp_bmp.getWidth(), temp_bmp.getHeight(), matrix, true);

            Log.e(TAG, "height = " + h + ", thumb height = " + bmp_lowrate.getHeight() + ",simeple size = " + opts.inSampleSize);
            refreshThumb();

        }
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener)
    {
        this.mSeekbarChangedListener = listener;
        this.onDetachedFromWindow();
    }

    public int getRate()
    {
        return mRate;
    }

    public void setOnRateChangedListener(OnRateChangedListener listener)
    {
        mOnRateChangedListener = listener;
    }

    public void setRate(int rate)
    {
        if(rate < 0 || rate >= 3)return;
        if(mRate == rate)return;
        mRate = rate;
        refreshThumb();

        autoAlign();

        if(mOnRateChangedListener != null)mOnRateChangedListener.rateChanged(mRate);
    }

    private void refreshThumb()
    {
        switch (mRate)
        {
            case 0: //low rate
                this.setThumb(new BitmapDrawable(bmp_lowrate));
                break;
            case 1:
                this.setThumb(new BitmapDrawable(bmp_midrate));
                break;
            case 2:
                this.setThumb(new BitmapDrawable(bmp_highrate));
                break;
        }
    }

    private void onSeekBarChanged(int progress)
    {
        if(isInAutoAlign)return;
        switch (mRate)
        {
            case 0:
                onSeekBarChangedAtRate0(progress);
                break;
            case 1:
                onSeekBarChangedAtRate1(progress);
                break;
            case 2:
                onSeekBarChangedAtRate2(progress);
                break;
        }

    }

    private void onSeekBarChangedAtRate0(int progress)
    {
        if(progress < 1)
        {
            setRate(0);
        }
        else if(progress > 75)
        {
            setRate(2);
        }
        else
        {
            setRate(1);
        }
    }

    private void onSeekBarChangedAtRate1(int progress)
    {
        if(progress < 49)
        {
            setRate(0);
        }
        else if(progress > 51)
        {
            setRate(2);
        }
        else
        {
            setRate(1);
        }
    }

    private void onSeekBarChangedAtRate2(int progress)
    {
        Log.e(TAG, "progress = " + progress);
        if(progress < 25)
        {
            setRate(0);
        }
        else if(progress > 98)
        {
            setRate(2);
        }
        else {
            setRate(1);
        }
    }

    private void autoAlign()
    {
        if(isInStracking)return;
        int targetProgress = 0;
        switch (mRate)
        {
            case 0:
                targetProgress = 5;
                break;
            case 1:
                targetProgress = 50;
                break;
            case 2:
                targetProgress = 95;
                break;
        }

        if(getProgress() == targetProgress)
        {
            isInAutoAlign = false;
            return;
        }
        if(Math.abs(getProgress() - targetProgress) < 5)
        {
            this.setProgress(targetProgress);
            isInAutoAlign = false;
            return;
        }

        if(this.getProgress() < targetProgress)
        {
            isInAutoAlign = true;
            this.setProgress(this.getProgress() + 5);
        }
        else if(this.getProgress() > targetProgress) {
            isInAutoAlign = true;
            this.setProgress(this.getProgress() - 5);
        }

        Message msg = new Message();
        msg.what = MSG_TYPE_AUTO_ALIGN;
        mHandler.sendMessageDelayed(msg, 10);
    }


}
