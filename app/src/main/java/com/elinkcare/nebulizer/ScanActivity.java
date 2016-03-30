package com.elinkcare.nebulizer;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.elinkcare.nebulizer.controller.BtDeviceScanner;

import java.util.ArrayList;
import java.util.List;

public class ScanActivity extends Activity {

    private static final int MSG_TYPE_START_SCAN = 0x01;
    private static final int MSG_TYPE_STOP_SCAN = 0x02;
    private static final int MSG_TYPE_REFRESH_DATA = 0x03;

    private List<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    private Button bt_start_scan;
    private Button bt_stop_scan;
    private ListView lv_devices;
    private DeviceListAdapter mAdapter = new DeviceListAdapter();
    private BtDeviceScanner.IScanChangedWatcher mScanChangedWatcher;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TYPE_START_SCAN:
                    refreshDevice(null);
                    break;
                case MSG_TYPE_REFRESH_DATA:
                    refreshDevice((List<BluetoothDevice>) msg.obj);
                    break;
                case MSG_TYPE_STOP_SCAN:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        initView();
        initOnAction();
    }

    private void initView() {
        bt_start_scan = (Button) findViewById(R.id.bt_start_scan);
        bt_stop_scan = (Button) findViewById(R.id.bt_stop_scan);
        lv_devices = (ListView) findViewById(R.id.lv_devices);

        lv_devices.setAdapter(mAdapter);
    }

    private void initOnAction() {
        bt_start_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message msg = new Message();
                msg.what = MSG_TYPE_START_SCAN;
                mHandler.sendMessage(msg);
                BtDeviceScanner.getInstance(getBaseContext()).startScanDevice();
            }
        });

        bt_stop_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BtDeviceScanner.getInstance(getBaseContext()).stopScanDevice();
            }
        });

        lv_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.setClass(getBaseContext(), TestNebulizerControlActivity.class);
                intent.putExtra("BTADR", mDeviceList.get(position).getAddress());
                startActivity(intent);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        mScanChangedWatcher = new BtDeviceScanner.IScanChangedWatcher() {
            @Override
            public void onChanged(List<BluetoothDevice> deviceLists) {
                Message msg = new Message();
                msg.what = MSG_TYPE_REFRESH_DATA;
                msg.obj = deviceLists;
                mHandler.sendMessage(msg);
            }
        };

        BtDeviceScanner.getInstance(this).addScanChangedWatcher(mScanChangedWatcher);
    }

    @Override
    public void onStop() {
        super.onStop();
        BtDeviceScanner.getInstance(this).removeScanChangedWatcher(mScanChangedWatcher);
    }

    private synchronized void refreshDevice(List<BluetoothDevice> devices) {
        mDeviceList.clear();
        if(devices != null)mDeviceList.addAll(devices);
        mAdapter.notifyDataSetChanged();
    }

    private class ViewHolder {
        public TextView tv_name;
        public TextView tv_address;
    }

    public class DeviceListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mDeviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return mDeviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mDeviceList.get(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = getLayoutInflater().inflate(R.layout.listitem_btdevice, null);
                viewHolder.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
                viewHolder.tv_address = (TextView) convertView.findViewById(R.id.tv_address);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            BluetoothDevice device = mDeviceList.get(position);
            viewHolder.tv_name.setText(device.getName());
            viewHolder.tv_address.setText(device.getAddress());
            return convertView;
        }
    }


}
