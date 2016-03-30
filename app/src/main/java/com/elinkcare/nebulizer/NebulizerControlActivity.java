package com.elinkcare.nebulizer;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.elinkcare.nebulizer.controller.BtDeviceScanner;
import com.elinkcare.nebulizer.controller.NebulizerController;
import com.elinkcare.nebulizer.controller.NebulizerDataStorage;
import com.elinkcare.nebulizer.view.NebulizerStatisticView;
import com.elinkcare.nebulizer.view.NebulizingRateSeekbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NebulizerControlActivity extends Activity {

    private static final String TAG = "NebulizerControlActivity";

    private static final int MSG_TYPE_GET_NEBULIZER_DATA = 0x01;
    private static final int MSG_TYPE_SYN_SYSTEM_TIME = 0x02;
    private static final int MSG_TYPE_TRY_CONNECT_DIVICE = 0x03;

    private ImageView iv_back;
    private ListView lv_nebulizer_data;
    private NebulizerStatisticView v_statisic;
    private NebulizingRateSeekbar sb_rate;

    private SimpleDateFormat mDateFormat = new SimpleDateFormat("hh:mma");
    private boolean lv_touch_flag = true;

    private List<NebulizerDataStorage.NebulizerRecordData> mDataList = new ArrayList<NebulizerDataStorage.NebulizerRecordData>();
    private NebulizerParamAdapter mAdapter = new NebulizerParamAdapter();

    private View mNeblizerStatisticView;
    private View mNebulizerRateView;
    private View mListTitleView;

    private List<NebulizerController.NebulizerData> mNebulizerDataList = new ArrayList<NebulizerController.NebulizerData>();
    private NebulizerController mNebuController;
    private NebulizerDataStorage mNebulizerDataStorage;

    private int mNebulizerDataIndex = -1;

    private boolean mRateModified = false;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            Message nMsg = new Message();
            switch (msg.what) {
                case MSG_TYPE_GET_NEBULIZER_DATA:
                    if(mNebuController == null)break;
                    mNebuController.GET_NEBULIZER_DATA(0xEE);
                    break;
                case MSG_TYPE_SYN_SYSTEM_TIME:
                    if(mNebuController == null)break;
                    mNebuController.SYN_NEBULIZER_TIME();
                    nMsg.what = MSG_TYPE_GET_NEBULIZER_DATA;
                    mHandler.sendMessageDelayed(nMsg, 100);
                    break;
                case MSG_TYPE_TRY_CONNECT_DIVICE:
                    if(mNebuController == null)startScanDevice();
                    nMsg.what = MSG_TYPE_TRY_CONNECT_DIVICE;
                    mHandler.sendMessageDelayed(nMsg, 20000);
                    break;

            }
        }
    };

    private NebulizerController.NebulizerListener mNebulizerListener
            = new NebulizerController.NebulizerListener() {
        @Override
        public void onConnectionStateChanged(boolean connected) {
            if (connected) {
                showMessage("device connected");
                Message msg = new Message();
                msg.what = MSG_TYPE_SYN_SYSTEM_TIME;
                mHandler.sendMessageDelayed(msg, 2000);
            } else {
                showMessage("device disconnected");
            }
        }

        @Override
        public void onDataReceived(byte[] data, UUID uuid) {

        }

        @Override
        public void onNebulizerRefreshed(int cmd, int refreshType) {
            handleNebulizerRefreshed(cmd, refreshType);
        }

        @Override
        public void onError(int errorCode, String error, long time) {

        }
    };

    private BtDeviceScanner.IScanChangedWatcher mScanChangedWatcher
            = new BtDeviceScanner.IScanChangedWatcher() {
        @Override
        public void onChanged(List<BluetoothDevice> devices) {
            if (devices == null || devices.size() == 0) {
                if (mNebuController == null) {
                    showMessage("no device scanned");
                }
                return;
            }
            if (mNebuController != null && mNebuController.ismConnected()) return;
            mNebuController = new NebulizerController(devices.get(0), getBaseContext());
            mNebuController.setNebulizerListener(mNebulizerListener);
            mNebuController.connect(getBaseContext());
        }
    };

    private NebulizingRateSeekbar.OnRateChangedListener mOnRateChangedListener
            = new NebulizingRateSeekbar.OnRateChangedListener() {
        @Override
        public void rateChanged(int rate) {
            if (mNebuController != null) {
                //mNebuController.SET_NEBULIZING_RATE(rate);
                mNebuController.GET_INST_STATE();
                mRateModified = true;
            }
        }
    };

    private NebulizerStatisticView.OnSelectedChangedListener mOnStatisticSelectedChangedListener
            = new NebulizerStatisticView.OnSelectedChangedListener() {

        @Override
        public void onSelectedChanged(long selectedTime, boolean flying) {
            if(!flying)refreshData(selectedTime);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nebulizer_control);

        initView();
        initOnAction();

        mNebulizerDataStorage = new NebulizerDataStorage(getBaseContext(), "nebulizer_data.db");
        BtDeviceScanner.getInstance(getBaseContext()).addScanChangedWatcher(mScanChangedWatcher);
        startScanDevice();
        Message msg = new Message();
        msg.what = MSG_TYPE_TRY_CONNECT_DIVICE;
        mHandler.sendMessageDelayed(msg, 10000);
    }

    @Override
    public void onStart() {
        super.onStart();
        //testData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BtDeviceScanner.getInstance(getBaseContext()).removeScanChangedWatcher(mScanChangedWatcher);
        if (mNebuController != null) {
            mNebuController.disconnect();
        }
    }

    private void initView() {

        iv_back = (ImageView) findViewById(R.id.iv_back);

        lv_nebulizer_data = (ListView) findViewById(R.id.lv_nebulizer_data);

        lv_nebulizer_data.setAdapter(mAdapter);
    }

    private void initOnAction()
    {
        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               finish();
            }
        });
    }

    /*
    private synchronized void testData() {
        mDataList.clear();
        NebulizerController.NebulizerData data = new NebulizerController.NebulizerData();
        data.mStartTimeInMillis = Calendar.getInstance().getTimeInMillis();
        data.mNebulizedTimeInMillis = 10 * 60000;
        mDataList.add(data);

        data = new NebulizerController.NebulizerData();
        data.mStartTimeInMillis = Calendar.getInstance().getTimeInMillis() + 3600 * 1000;
        data.mNebulizedTimeInMillis = 13 * 60000;
        mDataList.add(data);

        data = new NebulizerController.NebulizerData();
        data.mStartTimeInMillis = Calendar.getInstance().getTimeInMillis() + 2 * 3600 * 100 + 30 * 60000;
        data.mNebulizedTimeInMillis = 20 * 60000;
        mDataList.add(data);

        mAdapter.notifyDataSetChanged();
    }*/

    private void refreshData(final long time)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<NebulizerDataStorage.NebulizerRecordData> dataList = mNebulizerDataStorage.getDataInOneDay(time);
                mDataList.clear();
                if(dataList != null) mDataList.addAll(dataList);
                mAdapter.notifyDataSetChanged();
            }
        });

    }

    private class ViewHolder {
        public TextView tv_start_time;
        public TextView tv_duration;
    }

    private class RootView extends View {

        public RootView(Context context) {
            super(context);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                lv_touch_flag = false;
            }
            return false;
        }
    }

    private class NebulizerParamAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mDataList.size() + 3;
        }

        @Override
        public Object getItem(int position) {
            if (position < 3) return null;
            return mDataList.get(position - 3);
        }

        @Override
        public long getItemId(int position) {
            if (position < 3) return 0;
            return mDataList.get(position - 3).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position >= 3) {
                ViewHolder viewHolder;
                if (convertView != null && convertView.getTag() == null) {
                    convertView = null;
                }
                if (convertView == null) {
                    convertView = LayoutInflater.from(getBaseContext()).inflate(R.layout.listitem_nebulizer_data, null);
                    viewHolder = new ViewHolder();
                    viewHolder.tv_start_time = (TextView) convertView.findViewById(R.id.tv_start_time);
                    viewHolder.tv_duration = (TextView) convertView.findViewById(R.id.tv_duration);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }

                NebulizerDataStorage.NebulizerRecordData data = mDataList.get(position - 3);
                viewHolder.tv_start_time.setText(mDateFormat.format(data.startTime).toUpperCase());
                viewHolder.tv_duration.setText(String.format("%d" + getResources().getString(R.string.minute)
                        , (data.period + 59999) / 60000));
            } else if (position == 0) {
                if (mNeblizerStatisticView == null) {
                    convertView = LayoutInflater.from(getBaseContext()).inflate(R.layout.listitem_nebulizer_statistic, null);
                    mNeblizerStatisticView = convertView;
                    v_statisic = (NebulizerStatisticView) convertView.findViewById(R.id.v_statistic);
                    v_statisic.bindNebulizerDataStorage(mNebulizerDataStorage);
                    v_statisic.setOnSelectedChangedListener(mOnStatisticSelectedChangedListener);
                } else {
                    convertView = mNeblizerStatisticView;
                }
            } else if (position == 1) {
                if (mNebulizerRateView == null) {
                    convertView = LayoutInflater.from(getBaseContext()).inflate(R.layout.listitem_nebulizer_rate, null);
                    mNebulizerRateView = convertView;
                    sb_rate = (NebulizingRateSeekbar) convertView.findViewById(R.id.sb_rate);
                    sb_rate.setOnRateChangedListener(mOnRateChangedListener);
                } else {
                    convertView = mNebulizerRateView;
                }
            } else if (position == 2) {
                if(mListTitleView == null) {
                    convertView = LayoutInflater.from(getBaseContext()).inflate(R.layout.listtitle_nebulizer_para, null);
                    mListTitleView = convertView;
                }
                else
                {
                    convertView = mListTitleView;
                }
            }
            return convertView;
        }
    }

    private void startScanDevice() {
        BtDeviceScanner scanner = BtDeviceScanner.getInstance(getBaseContext());
        scanner.enableBluetooth();
        scanner.setmScanResultFilter(new BtDeviceScanner.WHQFilter());
        scanner.startScanDevice();
    }

    private void handleNebulizerRefreshed(int cmd, int refreshType) {
        switch (cmd) {
            case NebulizerController.CMD.CMD_GET_NEBULIZER_DATA:
                handleGetNebulizerData(cmd, refreshType);
                break;
            case NebulizerController.CMD.CMD_GET_INST_STATE:
                handleGetInstrumentState(cmd, refreshType);
                break;
            case NebulizerController.CMD.CMD_SET_NEBULIZING_RATE:
                showMessage("set nebulizing rate success");
                break;
        }
    }

    private void handleGetNebulizerData(int cmd, int refreshType) {
        final NebulizerController.NebulizerData data = mNebuController.getNebulizerData();


        if (mNebulizerDataIndex < 0) {
            mNebulizerDataIndex = data.mRecordCount - 2;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "Nebulizer Rate = " + data.mNebulizingRate);
                    sb_rate.setRate(data.mNebulizingRate);
                }
            });
        }
        NebulizerDataStorage.NebulizerRecordData recordData = new NebulizerDataStorage.NebulizerRecordData();
        recordData.startTime = data.mStartTimeInMillis;
        recordData.period = data.mNebulizedTimeInMillis;
        mNebulizerDataStorage.saveData(recordData);
        if (mNebulizerDataIndex > 1) {
            mNebulizerDataIndex--;
            mNebuController.GET_NEBULIZER_DATA(mNebulizerDataIndex);
        }

        Log.e(TAG, "index = " + mNebulizerDataIndex);
        Log.e(TAG, "mNebulized times = " + data.mCount);
        Log.e(TAG, "mNebulizerData = " + data.mStartTimeInMillis);
        Log.e(TAG, "mNebulizedTime = " + data.mNebulizedTimeInMillis);
    }

    private void handleGetInstrumentState(int cmd, int freshType)
    {
        Log.e(TAG, "receive state = " + mNebuController.getmInstState().mInstrumentState);
        if(mNebuController.getmInstState().mInstrumentState != NebulizerController.InstrumentState.ST_WORKING)
        {
            if(mRateModified)
            {
                mNebuController.SET_NEBULIZING_RATE(sb_rate.getRate());
                mRateModified = false;
            }
        }
        else
        {
            if(mRateModified)
            {
                showMessage("set nebulizing rate failed: the instrument is working");
                mNebuController.GET_NEBULIZER_DATA(0xEE);
                mRateModified = false;
            }
        }

    }

    private void showMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

    }
}
