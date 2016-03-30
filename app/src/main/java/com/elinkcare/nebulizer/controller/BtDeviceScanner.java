package com.elinkcare.nebulizer.controller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2016/3/22.
 */
public class BtDeviceScanner {

    private static BtDeviceScanner manager = new BtDeviceScanner();

    private BluetoothAdapter mBtAdapter;
    private Set<BluetoothDevice> mDeviceSet = new HashSet<BluetoothDevice>();
    private Timer mTimer;
    private long mScanPeriod = 2000;
    private boolean mIsConnected = false;
    private Set<IScanChangedWatcher> mScanChangedWatcherSet = new HashSet<IScanChangedWatcher>();
    private IScanResultFilter mScanResultFilter;

    private BtDeviceScanner()
    {
        //TODO: nothing
    }

    public static synchronized BtDeviceScanner getInstance(Context context)
    {
        if(context == null) throw new NullPointerException("BluetoothManager.getInstance(context), context couldn't be null");
        if(manager.mBtAdapter == null)
        {
            manager.mBtAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        }
        return manager;
    }

    public static interface IScanChangedWatcher
    {
        public void onChanged(List<BluetoothDevice> devices);
    }

    public static interface IScanResultFilter
    {
        public boolean filter(BluetoothDevice device);
    }

    public static class WHQFilter implements IScanResultFilter
    {

        @Override
        public boolean filter(BluetoothDevice device) {
            return device.getName().startsWith("eLinkCareWHQ");
        }
    }

    public synchronized void enableBluetooth()
    {
        mBtAdapter.enable();
    }

    public synchronized  void disableBluetooth()
    {
        mBtAdapter.disable();
    }

    public synchronized void startScanDevice()
    {
        mDeviceSet.clear();
        mBtAdapter.startLeScan(mLeScanCallback);

        if(mTimer != null)
        {
            mTimer.purge();
        }

        mTimer = new Timer();
        mTimer.schedule(new TimerTask()
        {
            @Override
            public void run() {
                mBtAdapter.stopLeScan(mLeScanCallback);
                mTimer = null;
                refreshDevice(null);

            }
        }, mScanPeriod);
    }

    public synchronized void stopScanDevice()
    {
        mTimer.purge();
        mBtAdapter.stopLeScan(mLeScanCallback);
        mTimer = null;
    }

    public BluetoothDevice getDevice(String address)
    {
        return mBtAdapter.getRemoteDevice(address);
    }

    public void addScanChangedWatcher(IScanChangedWatcher watcher)
    {
        mScanChangedWatcherSet.add(watcher);
    }

    public void removeScanChangedWatcher(IScanChangedWatcher watcher)
    {
        mScanChangedWatcherSet.remove(watcher);
    }

    public void setmScanResultFilter(IScanResultFilter filter)
    {
        mScanResultFilter = filter;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
    {

        @Override
        public synchronized void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(mScanResultFilter != null)
            {
                if(!mScanResultFilter.filter(device))return;
            }

            mDeviceSet.add(device);
            ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
            for(BluetoothDevice item : mDeviceSet)
            {
                deviceList.add(item);
            }

            refreshDevice(deviceList);

        }
    };

    private synchronized void refreshDevice(ArrayList<BluetoothDevice> deviceList)
    {
        Iterator<IScanChangedWatcher> iterator = mScanChangedWatcherSet.iterator();
        while(iterator.hasNext())
        {
            iterator.next().onChanged(deviceList);
        }
    }
}
