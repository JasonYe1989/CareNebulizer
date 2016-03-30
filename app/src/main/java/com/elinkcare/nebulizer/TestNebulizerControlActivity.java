package com.elinkcare.nebulizer;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.elinkcare.nebulizer.controller.BtDeviceScanner;
import com.elinkcare.nebulizer.controller.NebulizerController;

import java.text.SimpleDateFormat;
import java.util.UUID;

public class TestNebulizerControlActivity extends Activity {

    private static final int MSG_TYPE_DEVICE_CONNECTED = 0x01;
    private static final int MSG_TYPE_DEVICE_DISCONNECTED = 0x02;
    private static final int MSG_TYPE_DATA_RECEIVED = 0x03;
    private static final int MSG_TYPE_SEEK_CHANGED = 0x04;

    private TextView tv_receive_content;
    private Button bt_clear;
    private Button bt_send;
    private EditText et_send_content;
    private Button bt_send_command;
    private SeekBar sb_test;

    private boolean mSeekbarTouched = false;
    private int mSeekbarState = 0;
    private int mSeekbarTarget = 0;

    private NebulizerController mController;
    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
                case MSG_TYPE_DEVICE_CONNECTED:
                    appendReceivedContent("device connected: " + mController.getDeviceAddress());
                    appendReceivedContent("device name: " + mController.getDeviceName());
                    break;
                case MSG_TYPE_DEVICE_DISCONNECTED:
                    appendReceivedContent("device disconnect");
                    break;
                case MSG_TYPE_DATA_RECEIVED:
                    if(msg.obj == null)break;
                    if(msg.obj instanceof  byte[]) {
                        byte[] data = (byte[]) msg.obj;
                        appendReceivedContent("[]" + new String(data));
                    }
                    else if(msg.obj instanceof  String)
                    {
                        appendReceivedContent("[]" + (String) msg.obj);
                    }
                    break;
                case MSG_TYPE_SEEK_CHANGED:
                    sb_test.setProgress(msg.arg1);
                    break;
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_nebulizer_control);

        initController();
        initView();
        initOnAction();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        releaseController();
    }

    private void initView()
    {
        tv_receive_content = (TextView) findViewById(R.id.tv_receive_content);
        bt_clear = (Button) findViewById(R.id.bt_clear);
        bt_send = (Button) findViewById(R.id.bt_send);
        et_send_content = (EditText) findViewById(R.id.et_send_content);
        bt_send_command = (Button) findViewById(R.id.bt_send_command);
        sb_test = (SeekBar) findViewById(R.id.sb_test);
    }

    static int commandState = 0;
    private void initOnAction()
    {
        bt_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                tv_receive_content.setText("");
            }
        });

         bt_send.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {

                 mController.writeData(et_send_content.getText().toString().getBytes());
             }
         });

        bt_send_command.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NebulizerController.UserInfo userInfo = new NebulizerController.UserInfo();
                userInfo.mId = "User123";
                userInfo.mSex = "male";
                userInfo.mAge="12";
                switch (commandState)
                {
                    case 0:
                        et_send_content.setText("GET_IN_FACTORY_NAME");
                        mController.GET_IN_FACTORY_NAME();
                        break;
                    case 1:
                        et_send_content.setText("GET_INST_NAME");
                        mController.GET_INST_NAME();
                        break;
                    case 2:
                        et_send_content.setText("GET_INST_TYPE");
                        mController.GET_INST_TYPE();
                        break;
                    case 3:
                        et_send_content.setText("GET_INST_SN");
                        mController.GET_INST_SN();
                        break;
                    case 4:
                        et_send_content.setText("GET_PRODUCT_DATE");
                        mController.GET_PRODUCT_DATE();
                        break;
                    case 6:
                        et_send_content.setText("GET_FIREWARE_VERSION");
                        mController.GET_FIRMWARE_VERSION();
                        break;
                    case 7:
                        et_send_content.setText("GET_PCB_VERSION");
                        mController.GET_PCB_VERSION();
                        break;
                    case 8:
                        et_send_content.setText("GET_PCBA_VERSION");
                        mController.GET_PCBA_VERSION();
                        break;
                    case 9:
                        et_send_content.setText("GET_INST_STATE");
                        mController.GET_INST_STATE();
                        break;
                    case 10:
                        et_send_content.setText("GET_CELL_STATE");
                        mController.GET_CELL_STATE();
                        break;
                    case 11:
                        et_send_content.setText("GET_ELECTRIC_CURRENT");
                        mController.GET_ELECTRIC_CURRENT();
                        break;
                    case 12:
                        et_send_content.setText("GET_NEBULIZER_DATA I = 3");
                        mController.GET_NEBULIZER_DATA(3);
                        break;
                    case 13:
                        et_send_content.setText("GET_CUP_STATE");
                        mController.GET_CUP_STATE();
                        break;
                    case 14:
                        et_send_content.setText("GET_ERROR_INFO I = 2");
                        mController.GET_ERROR_INFO(2);
                        break;
                    case 15:
                        et_send_content.setText("GET_REGISTERED_USER_INFO");
                        mController.GET_REGISTERED_USER_INFO();
                        break;
                    case 16:
                        et_send_content.setText("GET_USER_INFO");
                        mController.GET_USER_INFO();
                        break;
                    case 17:
                        et_send_content.setText("SET_NEBULIZING_RATE: 2");
                        mController.SET_NEBULIZING_RATE(2);
                        break;
                    case 18:
                        et_send_content.setText("SET_NEBULIZER_DOSE： 60");
                        mController.SET_NEBULIZER_DOSE(60);
                        break;
                    case 19:
                        et_send_content.setText("SYN_SYSTEM_TIME");
                        mController.SYN_NEBULIZER_TIME();
                        break;
                    case 20:
                        userInfo.mId = "REGISTER USER";
                        et_send_content.setText("SET_REGISTERED_USER_INFO");
                        mController.SET_REGISTERED_USER_INFO(userInfo);
                        break;
                    case 21:
                        userInfo.mId = "USER";
                        et_send_content.setText("SET_USER_INFO");
                        mController.SET_USER_INFO(userInfo);
                        break;
                    case 22:
                        et_send_content.setText("SET_INST_PSW");
                        mController.SET_INST_PSW(new byte[]{1, 2, 3, 4, 5, 6});
                        break;
                }

                commandState ++;
                if(commandState > 21)commandState = 0;

            }
        });

        sb_test.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mSeekbarTouched)return;
                if(progress == 0 || progress == 50 || progress == 100)return;
                Message msg = new Message();
                msg.what = MSG_TYPE_SEEK_CHANGED;
                if(progress <= 25)
                {
                    msg.arg1 = progress - 2;
                }
                else if(progress > 25 && progress < 50)
                {
                    msg.arg1 = progress + 2;
                }
                else if(progress > 50 && progress <= 75)
                {
                    msg.arg1 = progress - 2;
                }
                else if(progress >75)
                {
                    msg.arg1 = progress + 2;
                }
                if(msg.arg1 == 49 || msg.arg1 == 51)msg.arg1 = 50;
                mHandler.sendMessageDelayed(msg, 5);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mSeekbarTouched = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSeekbarTouched = false;
                if(seekBar.getProgress() == 0) {
                    sb_test.setProgress(seekBar.getProgress() + 1);
                }
                else
                {
                    sb_test.setProgress(seekBar.getProgress() - 1);
                }
            }
        });
    }

    private void initController()
    {
        if(mController != null)return;
        String btAddress = getIntent().getStringExtra("BTADR");
        if(btAddress == null)return;
        BluetoothDevice device = BtDeviceScanner.getInstance(getBaseContext()).getDevice(btAddress);
        if(device == null)return;
        mController = new NebulizerController(device, getBaseContext());
        mController.connect(getBaseContext());

        mController.setNebulizerListener(mNebulizerListener);
    }

    private void releaseController()
    {
        if(mController == null)return;
        mController.disconnect();
        mController = null;
    }

    private NebulizerController.NebulizerListener mNebulizerListener = new NebulizerController.NebulizerListener()
    {

        @Override
        public void onConnectionStateChanged(boolean connected) {
            Message msg = new Message();
            if(connected)
            {
                msg.what = MSG_TYPE_DEVICE_CONNECTED;
            }
            else
            {
                msg.what = MSG_TYPE_DEVICE_DISCONNECTED;
            }
            mHandler.sendMessage(msg);
        }

        @Override
        public void onDataReceived(byte[] data, UUID uuid) {
            Message msg = new Message();
            msg.what = MSG_TYPE_DATA_RECEIVED;
            msg.obj = data;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onNebulizerRefreshed(int cmd, int refreshType) {
            handleTest(cmd, refreshType);
        }

        @Override
        public void onError(int errorCode, String error, long time) {
            handleTestError(errorCode, error, time);
        }
    };

    private void appendReceivedContent(String received)
    {
        tv_receive_content.setText(tv_receive_content.getText() + received + "\n");
    }

    private void handleTestError(int errorCode, String error, long time)
    {
        SimpleDateFormat format = new SimpleDateFormat("hh:mm");
        String errorStr = error + ": " + String.format("0x%x", errorCode) + "@" + format.format(time);
        Message msg = new Message();
        msg.what = MSG_TYPE_DATA_RECEIVED;
        msg.obj = "ERROR: " + errorStr;
        mHandler.sendMessage(msg);
    }

    private void handleTest(int cmd, int refreshType)
    {
        switch (cmd)
        {
            case NebulizerController.CMD.CMD_GET_INST_INFO:
                handleTestGetInstInfo(cmd, refreshType);
                break;
            case NebulizerController.CMD.CMD_GET_INST_STATE:
                handleTestGetInstState(cmd, refreshType);
                break;
            case NebulizerController.CMD.CMD_GET_CELL_STATE:
                handleTestGetCellState(cmd, refreshType);
                break;
            case NebulizerController.CMD.CMD_GET_ELECTORIC_CURRENT:
                handleTestGetElectricCurrent(cmd, refreshType);
                break;
            case NebulizerController.CMD.CMD_GET_NEBULIZER_DATA:
                handleTestGetNebuData(cmd, refreshType);
                break;
            case NebulizerController.CMD.CMD_GET_CUP_STATE:
                handleTestGetCupState(cmd, refreshType);
                break;
            case NebulizerController.CMD.CMD_GET_REGISTERED_USER_INFO:
                handleTestGetRegisteredUser(cmd, refreshType);
                break;
            case NebulizerController.CMD.CMD_GET_USER_INFO:
                handleTestGetUser(cmd, refreshType);
                break;
        }
    }

    private void handleTestGetInstInfo(int cmd, int refreshType)
    {
        Message msg = new Message();
        switch (refreshType)
        {
            case NebulizerController.InstrumentInfo.IN_FACTORY_NAME:
                msg.what = MSG_TYPE_DATA_RECEIVED;
                msg.obj = "FACTORY NAME: " + mController.getmInstInfo().mFactoryName;
                mHandler.sendMessage(msg);
                break;
            case NebulizerController.InstrumentInfo.IN_INST_NAME:
                msg.what = MSG_TYPE_DATA_RECEIVED;
                msg.obj = "INSTRUMENT NAME: " + mController.getmInstInfo().mInstrumentName;
                mHandler.sendMessage(msg);
                break;
        }
    }

    private void handleTestGetInstState(int cmd, int refreshType)
    {
        Message msg = new Message();
        msg.what = MSG_TYPE_DATA_RECEIVED;
        msg.obj = "INSTRUMENT STATE: " + mController.getmInstState().mInstrumentState;
        mHandler.sendMessage(msg);
    }

    private void handleTestGetCellState(int cmd, int refreshType)
    {
        Message msg = new Message();
        msg.what = MSG_TYPE_DATA_RECEIVED;
        msg.obj = "CELL STATE: " + mController.getmCellState().mCellState;
        mHandler.sendMessage(msg);
    }

    private void handleTestGetElectricCurrent(int cmd, int refreshType)
    {
        Message msg = new Message();
        msg.what = MSG_TYPE_DATA_RECEIVED;
        msg.obj = "E-CURRENT = " + String.format("%1.2f", mController.getmElectricCurrent().mElectricCurrent);
        mHandler.sendMessage(msg);
    }

    private void handleTestGetNebuData(int cmd, int refreshType)
    {
        StringBuilder builder = new StringBuilder();
        builder.append( "---------------\nNebulizer DATA:\n");

        NebulizerController.NebulizerData data = mController.getNebulizerData();
       // for(int i = 0; i < list.size(); i++)
        //{
            builder.append("count = ").append(data.mCount).append("\r\n");
            builder.append("record count = ").append(data.mRecordCount).append("\r\n");
            builder.append("start time : ").append(SimpleDateFormat.getInstance().format(data.mStartTimeInMillis)).append("\r\n");
            builder.append("current time : ").append(SimpleDateFormat.getInstance().format(data.mCurrentTimeInMillis)).append("\r\n");
            builder.append("nebulized time : ").append(data.mNebulizedTimeInMillis).append("\r\n");
            builder.append("nebulized dose : ").append(data.mNebulizedDose).append("\r\n");
            builder.append("predesigned dose : ").append(data.mPredesignedDose).append("\r\n");
            builder.append("nebulizing rate : ").append(data.mNebulizingRate).append("\r\n");
            builder.append("\n");
       // }

        Message msg = new Message();
        msg.what = MSG_TYPE_DATA_RECEIVED;
        msg.obj = builder.toString();
        mHandler.sendMessage(msg);
    }

    private void handleTestGetCupState(int cmd, int refreshType)
    {
        Message msg = new Message();
        msg.what = MSG_TYPE_DATA_RECEIVED;
        msg.obj = "Cup State : = " + mController.getmCupSate().mCupState;
        mHandler.sendMessage(msg);
    }

    private void handleTestGetRegisteredUser(int cmd, int refreshType)
    {
        StringBuilder builder = new StringBuilder();
        NebulizerController.UserInfo userInfo = mController.getmRegisteredUserInfo();
        builder.append("USER ID: ").append(userInfo.mId).append("\n");
        builder.append("SEX: ").append(userInfo.mSex).append("\n");
        builder.append("AGE: ").append(userInfo.mAge).append("\n");
        Message msg = new Message();
        msg.what = MSG_TYPE_DATA_RECEIVED;
        msg.obj = "REGISTERED USER : " + builder.toString();
        mHandler.sendMessage(msg);
    }

    private void handleTestGetUser(int cmd, int refreshType)
    {
        StringBuilder builder = new StringBuilder();
        NebulizerController.UserInfo userInfo = mController.getmUserInfo();
        builder.append("USER ID: ").append(userInfo.mId).append("\n");
        builder.append("SEX: ").append(userInfo.mSex).append("\n");
        builder.append("AGE: ").append(userInfo.mAge).append("\n");
        Message msg = new Message();
        msg.what = MSG_TYPE_DATA_RECEIVED;
        msg.obj = "USER : " + builder.toString();
        mHandler.sendMessage(msg);
    }

}
