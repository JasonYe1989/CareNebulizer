package com.elinkcare.nebulizer.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.elinkcare.nebulizer.R;
import com.elinkcare.nebulizer.controller.NebulizerDataStorage;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by Administrator on 2016/3/24.
 */
public class NebulizerStatisticView extends View {
    private static final String TAG = "NebulizerStatisticView";
    private static final int MILLIS_IN_DAY = 24 * 3600 * 1000;

    private static final int MSG_TYPE_AUTO_ALIGN = 0x01;
    private static final int MSG_TYPE_FLY_ITEMS = 0x02;
    private static final int MSG_TYPE_REFRESH_VIEW = 0x03;

    private Random mRandom = new Random(1);
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("MM/dd");

    private Bitmap mTrangleBmp;

    private int mBackGroundColor = 0xFF23ADE5;     //navy_blue
    private int mDateBarColor = 0xFFEEEEEE;
    private int mDateTextColor = 0xFF323232;
    private int mFutureDateTextColor = 0xFFAAAAAA;
    private int mCurrentDateTextColor = mBackGroundColor;
    private int mLegendTextColor = 0xFFFFFFFF;
    private int mDurationPillarColor = 0xFF8DDCFB;
    private int mTimesPillarColor = 0xFFFFFFFF;

    private float mLegendTextSize = this.getResources().getDimension(R.dimen.wordtips);
    private float mDateTextSize = this.getResources().getDimension(R.dimen.wordfuzhu);
    private float mCurrentDateTextSize = this.getResources().getDimension(R.dimen.wordtips);
    private float mDateBarHeight = 60;
    private float mDateTextOffset = 10;
    private float mLegendWidth = 80;
    private float mLegendHeight = 30;
    private float mLegendOffset = 10;

    private int mVisibleItemCount = 5;
    private int mItemViewWidth;
    private int mWidth;
    private int mHeight;

    private List<ItemView> mCacheItemViewList = new LinkedList<ItemView>();
    private LinkedList<ItemView> mVisibleItemViewList = new LinkedList<ItemView>();
    private ItemViewTreatment mItemViewTreatment = new ItemViewTreatment();

    private boolean isTouched = false;
    private int startOffsetX = 0;
    private int startOffsetY = 0;
    private int offsetX = 0;
    private int offsetY = 0;
    private float touchStartX;
    private float touchStartY;
    private long startTouchTime;
    private int mFlyVelocity;

    private NebulizerDataStorage mDataStorage;

    private OnSelectedChangedListener mSelectedChangedListener;

    private void init() {
        mItemViewTreatment.setAdapter(new ItemViewAdapter());
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TYPE_AUTO_ALIGN:
                    if (mFlyVelocity != 0) break;
                    if (!isTouched) autoAlign();
                    invalidate();
                    break;
                case MSG_TYPE_FLY_ITEMS:
                    if (!isTouched) flyItemView();
                    invalidate();
                    break;
                case MSG_TYPE_REFRESH_VIEW:
                    invalidate();
                    break;
            }
        }
    };

    public NebulizerStatisticView(Context context) {
        super(context);
        init();
    }

    public NebulizerStatisticView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        final Resources.Theme theme = context.getTheme();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NebulizerStatisticView);
        Log.e(TAG, "mCurrentDateTextSize = " + mCurrentDateTextColor);
        if (null != a) {
            int n = a.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);
                switch (attr) {
                    //import dimens
                    case R.styleable.NebulizerStatisticView_legendTextSize:
                        mLegendTextSize = a.getDimension(attr, mLegendTextSize);
                        break;
                    case R.styleable.NebulizerStatisticView_dateTextSize:
                        mDateTextSize = a.getDimension(attr, mDateTextSize);
                        Log.e(TAG, "mDateTextSize = " + mDateTextSize);
                        break;
                    case R.styleable.NebulizerStatisticView_currentDateTextSize:
                        mCurrentDateTextSize = a.getDimension(attr, mCurrentDateTextSize);
                        Log.e(TAG, "mCurrentDateTextSize = " + mCurrentDateTextColor);
                        break;
                    case R.styleable.NebulizerStatisticView_dateBarHeight:
                        mDateBarHeight = a.getDimension(attr, mDateBarHeight);
                        break;
                    case R.styleable.NebulizerStatisticView_dateTextOffset:
                        mDateTextOffset = a.getDimension(attr, mDateTextOffset);
                        break;
                    case R.styleable.NebulizerStatisticView_legendOffset:
                        mLegendOffset = a.getDimension(attr, mLegendOffset);
                        break;

                    //import colors
                    case R.styleable.NebulizerStatisticView_backgroundColor:
                        mBackGroundColor = a.getColor(attr, mBackGroundColor);
                        break;
                    case R.styleable.NebulizerStatisticView_dateBarColor:
                        mDateBarColor = a.getColor(attr, mDateBarColor);
                        break;
                    case R.styleable.NebulizerStatisticView_dateTextColor:
                        mDateTextColor = a.getColor(attr, mDateTextColor);
                        break;
                    case R.styleable.NebulizerStatisticView_currentDateTextColor:
                        mCurrentDateTextColor = a.getColor(attr, mCurrentDateTextColor);
                        break;
                    case R.styleable.NebulizerStatisticView_legendTextColor:
                        mLegendTextColor = a.getColor(attr, mLegendTextColor);
                        break;
                    case R.styleable.NebulizerStatisticView_durationPillarColor:
                        mDurationPillarColor = a.getColor(attr, mDurationPillarColor);
                        break;
                    case R.styleable.NebulizerStatisticView_timesPillarColor:
                        mTimesPillarColor = a.getColor(attr, mTimesPillarColor);
                        break;

                }
            }
        }

    }

    public NebulizerStatisticView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public static interface OnSelectedChangedListener {
        public void onSelectedChanged(long selectedTime, boolean flying);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mWidth = this.getWidth();
        mHeight = this.getHeight();
        mItemViewWidth = mWidth / 5;

        mItemViewTreatment.notifyDataSetChanged();
        drawBackground(canvas);
        drawItemViews(canvas);
        drawLegend(canvas);
    }

    public boolean requestTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e(TAG, "ACTION DOWN");
                touchStartX = event.getX();
                touchStartY = event.getY();
                startOffsetX = offsetX;
                startOffsetY = offsetY;
                mFlyVelocity = 0;
                isTouched = true;
                startTouchTime = Calendar.getInstance().getTimeInMillis();
                clearMessageQueue();
                break;
            case MotionEvent.ACTION_MOVE:
            {
                int oldOffsetX = offsetX;
                mFlyVelocity = 0;
                isTouched = true;
                offsetX = startOffsetX + (int) (event.getX() - touchStartX);
                if(oldOffsetX == offsetX)break;
                mItemViewTreatment.notifyDataSetChanged();
            }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if(isTouched == false)break;
                Log.e(TAG, "ACTION UP");
                isTouched = false;
                mFlyVelocity = (int) ((event.getX() - touchStartX) * 500 / (Calendar.getInstance().getTimeInMillis() - startTouchTime));
                if (Math.abs(mFlyVelocity) > 500) {
                    flyItemView();
                } else {
                    mFlyVelocity = 0;
                    autoAlign();
                }
        }
        return true;
    }

    public void bindNebulizerDataStorage(NebulizerDataStorage dataStorage) {
        mDataStorage = dataStorage;
        mDataStorage.setOnDatasetChangedListener(new NebulizerDataStorage.OnDatasetChangedListener() {
            @Override
            public void datasetChanged() {
                refreshItemView();
                mItemViewTreatment.notifyDataSetChanged();
            }
        });
    }

    public void setOnSelectedChangedListener(OnSelectedChangedListener listener) {
        mSelectedChangedListener = listener;
    }

    private void clearMessageQueue()
    {
        mHandler.removeMessages(MSG_TYPE_AUTO_ALIGN);
        mHandler.removeMessages(MSG_TYPE_FLY_ITEMS);
    }

    private synchronized void autoAlign() {
        int currentIndex = mVisibleItemCount / 2;
        ItemView currentItemView = mItemViewTreatment.mCenterView;
        int alignLeft = mItemViewWidth * currentIndex;
        int currentItemLeft = currentItemView.getLeft();
        if (currentItemLeft == alignLeft) {
            refreshItemView();
            if (mSelectedChangedListener != null) {
                long dayTime = Calendar.getInstance().getTimeInMillis() + (long) currentItemView.getDay() * MILLIS_IN_DAY;
                mSelectedChangedListener.onSelectedChanged(dayTime, false);
            }
            Log.e(TAG, "END AUTO ALIGN");
            return;
        }
        clearMessageQueue();

        Message msg = new Message();
        msg.what = MSG_TYPE_AUTO_ALIGN;
        int step;
        if (Math.abs(currentItemLeft - alignLeft) >= 20) {
            step = 20;
        } else {
            step = 1;
        }
        if (currentItemLeft < alignLeft) {
            offsetX += step;
        } else {
            offsetX -= step;
        }

        mHandler.sendMessageDelayed(msg, 40);
    }

    private synchronized void flyItemView() {
        int flyStep = mFlyVelocity * 20 / 1000;
        offsetX += flyStep;

        if (flyStep == 0) {
            mFlyVelocity = 0;
        }

        if (mFlyVelocity >= 240) {
            mFlyVelocity -= 20;
        } else if (mFlyVelocity <= -240) {
            mFlyVelocity += 20;
        } else {
            mFlyVelocity = 0;
        }

        clearMessageQueue();
        if (mFlyVelocity != 0) {
            Message msg = new Message();
            msg.what = MSG_TYPE_FLY_ITEMS;
            mHandler.sendMessageDelayed(msg, 20);
        } else {
            Log.e(TAG, "end flying item");
            mItemViewTreatment.refreshCenterView();
            autoAlign();
        }

    }

    private void drawBackground(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(mBackGroundColor);
        canvas.drawRect(0, 0, mWidth, mHeight, paint);
        paint.setColor(mDateBarColor);
        canvas.drawRect(0, mHeight - mDateBarHeight, mWidth, mHeight, paint);

        if (mTrangleBmp == null) {
            float trangleHeight = mDateBarHeight / 4;
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(getResources(), R.drawable.triangle, opt);
            opt.inJustDecodeBounds = false;
            opt.inSampleSize = (int) (opt.outWidth / trangleHeight);
            mTrangleBmp = BitmapFactory.decodeResource(getResources(), R.drawable.triangle, opt);
        }
        canvas.drawBitmap(mTrangleBmp, (mWidth - mTrangleBmp.getWidth()) / 2, mHeight - mDateBarHeight, paint);
    }

    private synchronized void drawItemViews(Canvas canvas) {
        ItemView itemView;
        for (int i = 0; i < mVisibleItemViewList.size(); i++) {
            itemView = mVisibleItemViewList.get(i);
            drawItemView(canvas, itemView);
        }

        int lineY = (int) (mHeight - mDateBarHeight);
        int lineSpace = lineY / 6;
        Paint paint = new Paint();
        paint.setColor(mBackGroundColor);
        paint.setStrokeWidth(2);
        for (int i = 0; i < 6; i++) {
            canvas.drawLine(0, lineY, mWidth, lineY, paint);
            lineY -= lineSpace;
        }
    }

    private void drawItemView(Canvas canvas, ItemView itemView) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);

        // canvas.drawLine(itemView.getLeft(), itemView.getTop(), itemView.getLeft(), itemView.getBottom(), paint);
        //canvas.drawLine(itemView.getRight(), itemView.getTop(), itemView.getRight(), itemView.getBottom(), paint);
        //canvas.drawText(String.format("%d", itemView.getDay()), itemView.getLeft() + 0.5f * itemView.getWidth(), itemView.getBottom() - 20, paint);

        int pillarBottom = (int) (mHeight - mDateBarHeight);
        int pillarWidth = (int) (mItemViewWidth / 3.5f);
        int pillarHeight = (int) ((mHeight - mDateBarHeight) / 6);
        int pillarOffset = pillarWidth / 2;
        mLegendHeight = pillarWidth;
        mLegendWidth = pillarHeight;

        //Duration Pillar
        int pillarLeft = itemView.getLeft() + pillarOffset;
        int pillarRight = pillarLeft + pillarWidth;
        int pillarTop = pillarBottom - itemView.getDuration() * pillarHeight / 10;
        paint.setColor(mDurationPillarColor);
        canvas.drawRect(pillarLeft, pillarTop, pillarRight, pillarBottom, paint);

        //Times Pillar
        pillarLeft = pillarRight + pillarOffset;
        pillarRight = pillarLeft + pillarWidth;
        pillarTop = pillarBottom - itemView.getTimes() * pillarHeight;
        paint.setColor(mTimesPillarColor);
        canvas.drawRect(pillarLeft, pillarTop, pillarRight, pillarBottom, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        int textX = itemView.getLeft() + itemView.getWidth() / 2;
        int textY = (int) (mHeight - mDateTextOffset);
        String dateStr;
        if (itemView.getDay() == 0) {
            dateStr = getResources().getString(R.string.today);
        } else {
            long dayTime = Calendar.getInstance().getTimeInMillis() + (long) itemView.getDay() * MILLIS_IN_DAY;
            dateStr = mDateFormat.format(dayTime);
        }
        if (itemView == mItemViewTreatment.mCenterView)      //draw today
        {
            paint.setColor(mCurrentDateTextColor);
            paint.setTextSize(mCurrentDateTextSize);
            canvas.drawText(dateStr, textX, textY, paint);
        } else {
            paint.setTextSize(mDateTextSize);
            if (itemView.getDay() > 0)
                paint.setColor(mFutureDateTextColor);
            else
                paint.setColor(mDateTextColor);
            canvas.drawText(dateStr, textX, textY, paint);
        }

    }

    private void drawLegend(Canvas canvas) {
        String legend_times = getResources().getString(R.string.legend_nebulizer_times);
        String legend_duration = getResources().getString(R.string.legend_nebulizer_duration);

        Paint paint = new Paint();

        paint.setColor(mBackGroundColor);
        canvas.drawRect(0, 0, mWidth, mLegendHeight + mLegendOffset * 2, paint);

        paint.setTextSize(mLegendTextSize);
        paint.setColor(mLegendTextColor);
        int legend_times_text_width = (int) paint.measureText(legend_times);
        int legend_duration_text_width = (int) paint.measureText(legend_duration);

        int left = (int) (mWidth - mLegendOffset - legend_duration_text_width);
        int top = (int) mLegendOffset;
        canvas.drawText(legend_duration, left, top + mLegendHeight, paint);

        paint.setColor(mDurationPillarColor);
        left -= mLegendOffset + mLegendWidth;
        canvas.drawRect(left, top, left + mLegendWidth, top + mLegendHeight, paint);

        paint.setColor(mLegendTextColor);
        left -= mLegendOffset + legend_times_text_width;
        canvas.drawText(legend_times, left, top + mLegendHeight, paint);

        paint.setColor(mTimesPillarColor);
        left -= mLegendOffset + mLegendWidth;
        canvas.drawRect(left, top, left + mLegendWidth, top + mLegendHeight, paint);
    }

    private class ItemView {
        private int mLeft;
        private int mRight;
        private int mBottom;
        private int mTop;
        private int mWidth;
        private int mHeight;

        private int mDay;
        private int mTimes;
        private int mDuration;

        //getters
        public int getLeft() {
            return mLeft + offsetX;
        }

        public int getRight() {
            return mRight + offsetX;
        }

        public int getTop() {
            return mTop + offsetY;
        }

        public int getBottom() {
            return mBottom + offsetY;
        }

        public int getRawLeft() {
            return mLeft;
        }

        public int getRawRight() {
            return mRight;
        }

        public int getRawTop() {
            return mTop;
        }

        public int getRawBottom() {
            return mBottom;
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }

        public int getDay() {
            return mDay;
        }

        public int getTimes() {
            return mTimes;
        }

        public int getDuration() {
            return mDuration;
        }

        //setters
        public void setRawLeft(int left) {
            mLeft = left;
            mRight = mLeft + mWidth;
        }

        public void setRawRight(int right) {
            mRight = right;
            mLeft = mRight - mWidth;
        }

        public void setRawTop(int top) {
            mTop = top;
            mBottom = mTop + mHeight;
        }

        public void setRawBottom(int bottom) {
            mBottom = bottom;
            mTop = mBottom - mHeight;
        }

        public void setWidth(int width) {
            mWidth = width;
            mRight = mLeft + mWidth;
        }

        public void setHeight(int height) {
            mHeight = height;
            mBottom = mTop + mHeight;
        }

        public void setDay(int day) {
            mDay = day;
        }

        public void setTimes(int times) {
            mTimes = times;
        }

        public void setDuration(int duration) {
            mDuration = duration;
        }

    }

    private class ItemViewTreatment {
        private ItemViewAdapter mAdapter;
        private ItemView mCenterView;
        private Thread mNotifyThread = null;

        public void setAdapter(ItemViewAdapter adapter) {
            if (adapter == null) return;
            mAdapter = adapter;
        }

        public synchronized void notifyDataSetChanged() {
            if(mNotifyThread == null) {
                mNotifyThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (releaseUnvisableItem()) ;
                        while (loadVisibleItem()) ;
                        refreshOffsetX();
                        refreshCenterView();
                        mNotifyThread = null;
                        Message msg = new Message();
                        msg.what = MSG_TYPE_REFRESH_VIEW;
                        mHandler.sendMessage(msg);
                    }
                });
                mNotifyThread.start();
            }
        }

        private synchronized void refreshCenterView() {
            int centerX = mWidth / 2;
            ItemView oldCenterView = mCenterView;
            for (int i = 0; i < mVisibleItemViewList.size(); i++) {
                ItemView itemView = mVisibleItemViewList.get(i);
                if (itemView.getLeft() <= centerX && itemView.getRight() > centerX) {
                    mCenterView = itemView;
                    break;
                }
            }

            if (oldCenterView == null && mCenterView != null) {
                if (mSelectedChangedListener != null) {
                    long dayTime = Calendar.getInstance().getTimeInMillis() + (long) mCenterView.getDay() * MILLIS_IN_DAY;
                    mSelectedChangedListener.onSelectedChanged(dayTime, false);
                }
            }

        }

        private boolean releaseUnvisableItem() {
            if (mVisibleItemViewList.size() == 0) return false;
            if (mVisibleItemViewList.getFirst().getRight() <= 0) {
                releaseChacheItemView(mVisibleItemViewList.getFirst());
                return true;
            } else if (mVisibleItemViewList.getLast().getLeft() >= mWidth) {
                releaseChacheItemView(mVisibleItemViewList.getLast());
                return true;
            }
            return false;
        }

        private boolean loadVisibleItem() {
            int requestLocation = ItemViewAdapter.NO_REQUEST;
            int day = 0;
            if (mVisibleItemViewList.size() == 0) {
                requestLocation = ItemViewAdapter.REQUEST_FIRST;
                day = -2;
            } else if (mVisibleItemViewList.getFirst().getLeft() > 0) {
                requestLocation = ItemViewAdapter.REQUEST_LEFT;
                day = mVisibleItemViewList.getFirst().getDay() - 1;
            } else if (mVisibleItemViewList.getLast().getRight() < mWidth) {
                requestLocation = ItemViewAdapter.REQUEST_RIGHT;
                day = mVisibleItemViewList.getLast().getDay() + 1;
            }

            if (requestLocation != ItemViewAdapter.NO_REQUEST) {

                ItemView itemView = mAdapter.getView(day, this.getCacheItemView(), requestLocation);

                switch (requestLocation) {
                    case ItemViewAdapter.REQUEST_FIRST:
                        mVisibleItemViewList.add(itemView);
                        break;
                    case ItemViewAdapter.REQUEST_RIGHT:
                        mVisibleItemViewList.addLast(itemView);
                        break;
                    case ItemViewAdapter.REQUEST_LEFT:
                        mVisibleItemViewList.addFirst(itemView);
                        break;
                }
            }
            return requestLocation != ItemViewAdapter.NO_REQUEST;
        }

        private synchronized void releaseChacheItemView(ItemView itemView) {
            mVisibleItemViewList.remove(itemView);
            itemView.setDuration(0);
            itemView.setTimes(0);
            if (mCacheItemViewList.size() == 0) mCacheItemViewList.add(itemView);
        }

        private synchronized ItemView getCacheItemView() {
            if (mCacheItemViewList.size() == 0) return null;
            ItemView itemView = mCacheItemViewList.get(0);
            mCacheItemViewList.remove(itemView);
            return itemView;
        }

        private synchronized void refreshOffsetX() {
            if (offsetX < -mWidth) {
                startOffsetX += mWidth;
                offsetX += mWidth;
                for (int i = 0; i < mVisibleItemViewList.size(); i++) {
                    ItemView itemView = mVisibleItemViewList.get(i);
                    itemView.setRawLeft(itemView.getRawLeft() - mWidth);
                }
            } else if (offsetX > 2 * mWidth) {
                startOffsetX -= mWidth;
                offsetX -= mWidth;
                for (int i = 0; i < mVisibleItemViewList.size(); i++) {
                    ItemView itemView = mVisibleItemViewList.get(i);
                    itemView.setRawLeft(itemView.getRawLeft() + mWidth);
                }
            }
        }
    }

    private class ItemViewAdapter {

        public static final int NO_REQUEST = 0x00;
        public static final int REQUEST_LEFT = 0x01;
        public static final int REQUEST_RIGHT = 0x02;
        public static final int REQUEST_FIRST = 0x03;

        public ItemView getView(int day, ItemView convertView, int requestLocation) {
            if (convertView == null) {
                convertView = new ItemView();
            }
            convertView.setDay(day);
            convertView.setRawLeft(0);
            convertView.setRawTop(0);
            convertView.setWidth(mItemViewWidth);
            convertView.setHeight((int) (mHeight - mDateBarHeight));

            switch (requestLocation) {
                case REQUEST_LEFT:
                    convertView.setRawRight(mVisibleItemViewList.getFirst().getRawLeft());
                    break;
                case REQUEST_RIGHT:
                    convertView.setRawLeft(mVisibleItemViewList.getLast().getRawRight());
                    break;
            }

            setItemView(convertView);

            return convertView;
        }

    }


    private void setItemView(ItemView itemView) {
        if (mFlyVelocity == 0 && mDataStorage != null && itemView.getDay() <= 0) {
            long dayTime = Calendar.getInstance().getTimeInMillis() + (long) itemView.getDay() * MILLIS_IN_DAY;
            NebulizerDataStorage.NebulizerRecordStatistic statistic = mDataStorage.getRecordStatistic(dayTime);
            if (statistic != null) {
                itemView.setTimes(statistic.times);
                itemView.setDuration((int) (statistic.totalPeriod / 60000));
            } else {
                itemView.setTimes(0);
                itemView.setDuration(0);
            }
        } else {
            itemView.setTimes(0);
            itemView.setDuration(0);
        }
    }

    private void refreshItemView() {

        for (int i = 0; i < mVisibleItemViewList.size(); i++) {
            setItemView(mVisibleItemViewList.get(i));
        }
    }

}
