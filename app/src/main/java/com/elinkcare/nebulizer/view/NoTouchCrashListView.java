package com.elinkcare.nebulizer.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

import com.elinkcare.nebulizer.R;

/**
 * Created by Administrator on 2016/3/29.
 */
public class NoTouchCrashListView extends ListView {

    private NebulizerStatisticView mStatisticView = null;

    public NoTouchCrashListView(Context context) {
        super(context);
    }

    public NoTouchCrashListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoTouchCrashListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        int x = (int)event.getX();
        int y = (int)event.getY();
        int position = pointToPosition(x, y);
        Log.e("ListView", "ACTION = " + event.getAction());
        if(position == 0
                || event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
            View touchItemView = this.getChildAt(position);
            if (touchItemView != null) {
                if (mStatisticView == null) {
                    mStatisticView = (NebulizerStatisticView) touchItemView.findViewById(R.id.v_statistic);
                }
            }
            if(mStatisticView != null) mStatisticView.requestTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }
}
