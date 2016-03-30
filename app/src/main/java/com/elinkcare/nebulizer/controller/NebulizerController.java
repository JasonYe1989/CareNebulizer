package com.elinkcare.nebulizer.controller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Jason on 2016/3/22.
 */
public class NebulizerController {

    private static final String TAG = "NebulizerController";
    private BluetoothDevice mDevice = null;
    private BluetoothGatt mBluetoothGatt = null;
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothGattService mGattService = null;
    private BluetoothGattCharacteristic mWriteCharacteristic = null;
    private BluetoothGattCharacteristic mReadCharacteristic = null;
    private NebulizerListener mListener = null;
    private boolean mConnected = false;

    private static final UUID SERVICE_UUID = UUID
            .fromString("0000FFF0-0000-1000-8000-00805F9B34FB");
    private static final UUID CHARACTERISTIC_WRITE_UUID = UUID
            .fromString("0000FFF6-0000-1000-8000-00805F9B34FB");
    private static final UUID CHARACTERISTIC_READ_UUID = UUID
            .fromString("0000FFF7-0000-1000-8000-00805F9B34FB");

    private byte HEAD_LABEL_0 = 0x55;
    private byte HEAD_LABEL_1 = (byte) 0xAA;
    private int READ_BUFFER_MAX_LEN = 256;
    private byte[] mReadDataBuffer = new byte[READ_BUFFER_MAX_LEN];
    private int mBufferReadIndex = 0;
    private int mBufferWriteIndex = 0;
    private int mBufferDataLen = 0;

    private InstrumentInfo mInstInfo = new InstrumentInfo();
    private InstrumentState mInstState = new InstrumentState();
    private CellState mCellState = new CellState();
    private ElectricCurrent mElectricCurrent = new ElectricCurrent();
    private NebulizerData mNebulizerData = new NebulizerData();
    private CupState mCupSate = new CupState();
    private UserInfo mRegisteredUserInfo = new UserInfo();
    private UserInfo mUserInfo = new UserInfo();
    private List<ErrorInfo> mErrorList = new ArrayList<ErrorInfo>();
    private FirmwareUpdate mFirmwareUpdate = new FirmwareUpdate();
    private FlashOperate mFlashOperate = new FlashOperate();

    private Thread mWriteThread;
    private LinkedList<ReData> mReDataList = new LinkedList<ReData>();

    public static interface NebulizerListener {
        public void onConnectionStateChanged(boolean connected);

        public void onDataReceived(byte[] data, UUID uuid);

        public void onNebulizerRefreshed(int cmd, int refreshType);

        public void onError(int errorCode, String error, long time);
    }

    public static class CMD {
        public static final int CMD_GET_INST_INFO = 0x01;
        public static final int CMD_GET_INST_STATE = 0x02;
        public static final int CMD_GET_CELL_STATE = 0x03;
        public static final int CMD_GET_ELECTORIC_CURRENT = 0x04;
        public static final int CMD_GET_NEBULIZER_DATA = 0x05;
        public static final int CMD_GET_CUP_STATE = 0x06;
        public static final int CMD_GET_ERROR_INFO = 0x07;
        public static final int CMD_GET_REGISTERED_USER_INFO = 0x08;
        public static final int CMD_GET_USER_INFO = 0x09;
        public static final int CMD_ACK_ERROR = 0x0A;
        public static final int CMD_SET_NEBULIZING_RATE = 0x10;
        public static final int CMD_SET_NEBULIZER_DOSE = 0x11;
        public static final int CMD_SET_NEBULIZER_TIME = 0x12;
        public static final int CMD_SET_REGISTERED_USER_INFO = 0x13;
        public static final int CMD_SET_USER_INFO = 0x14;
        public static final int CMD_UPDATE_FIRMWARE = 0x80;
        public static final int CMD_SET_INST_PRODUCT_INFO = 0x81;
        public static final int CMD_GET_PRODUCT_INFO = 0x82;
        public static final int CMD_RESET_BLUETOOTH = 0x83;
    }

    /**
     * 仪器信息
     */
    public static class InstrumentInfo {
        public static final int IN_FACTORY_NAME = 0x00;         //厂家名称
        public static final int IN_INST_NAME = 0x01;            //仪器名称
        public static final int IN_INST_TYPE = 0x02;            //仪器型号
        public static final int IN_INST_SN = 0x03;              //仪器序列号
        public static final int IN_PRODUCT_DATE = 0x04;         //仪器生产日期
        public static final int IN_FIRMWARE_VERSION = 0x05;     //Firmware版本
        public static final int IN_PCB_VERSION = 0x06;          //PCB版本
        public static final int IN_PCBA_VERSION = 0x07;         //PCBA版本

        public String mFactoryName;
        public String mInstrumentName;
        public String mInstrumentType;
        public String mInstrumentSN;
        public String mProductDate;
        public String mFirmwareVersion;
        public String mPCBVersion;
        public String mPCBAVersion;
    }

    /**
     * 仪器状态
     */
    public static class InstrumentState {
        public static final int ST_IDLE = 0x00;             //待机
        public static final int ST_WORKING = 0x01;          //雾化进行
        public static final int ST_PAUSE = 0x02;            //雾化暂定
        public static final int ST_WORK_CHARGING = 0x04;   //充电
        public static final int ST_STOP = 0x05;             //停机
        public static final int ST_SHUT_BLUE = 0x06;        //即将关闭蓝牙

        public int mInstrumentState;
    }

    /**
     * 电池状态
     */
    public static class CellState {
        public static final int ST_LESS = 0x00;             //电池电量不足
        public static final int ST_LOW = 0x02;              //电量低
        public static final int ST_NORMAL = 0x03;           //电量正常
        public static final int ST_CHARGING = 0x04;         //充电中
        public static final int ST_CHARGED = 0x05;          //充电完成

        public int mCellState;
    }

    //检测电流
    public static class ElectricCurrent {
        public float mElectricCurrent;                      //监测电流
    }

    //雾化数据
    public static class NebulizerData {
        public int mCount;                                   //仪器雾化总次数
        public int mRecordCount;                            //仪器当前保存的雾化数据记录数，范围0-51,0表示没有记录
        public long mStartTimeInMillis;                    //雾化开始的绝对时间，精确到秒
        public long mCurrentTimeInMillis;                  //仪器时钟的当前时间，当雾化结束时，表示雾化结束的时间，精确到秒
        public long mNebulizedTimeInMillis;               //雾化已完成时间，不包含中间暂停的时间
        public int mNebulizedDose;                         //雾化已完成剂量，单位0.1ml或ug
        public int mPredesignedDose;                        //雾化设置剂量，单位0.1ml或ug
        public int mNebulizingRate;                         //当前雾化速率，三挡速率分别为0,1,2
    }

    //雾化杯状态
    public static class CupState {
        public static final int ST_OUT = 0x00;              //雾化杯拔出
        public static final int ST_IN1 = 0x01;             //雾化杯类型1插入
        public static final int ST_IN2 = 0x02;              //雾化杯类型2插入
        public static final int ST_IN3 = 0x03;              //雾化杯类型3插入
        public static final int ST_IN4 = 0x04;              //雾化杯类型4插入
        public static final int ST_IN5 = 0x05;              //雾化杯类型5插入

        public int mCupState;
    }

    //错误信息
    public static class ErrorInfo {
        public static final int ERR_CUP_CONNECTION_EXCEPTION = 0xE0;     //雾化杯连接异常
        public static final int ERR_READ_INST_INFO_CHECK = 0xE1;          //仪器信息读取Flash校验错误
        public static final int ERR_READ_FACTORY_INFO_CHECK = 0xE2;      //出厂参数读取Flash校验错误
        public static final int ERR_READ_USER_INFO_CHECK = 0xE3;          //用户信息读取Flash校验错误
        public static final int ERR_READ_ERROR_INFO_CHECK = 0xE4;         //错误信息读取Flash校验错误
        public static final int ERR_READ_NEBU_INFO_CHECK = 0xE5;         //雾化数据读取Flash校验错误
        public static final int ERR_BT_COM_CHECK = 0xE6;                   //蓝牙通讯数据校验错误
        public static final int ERR_BT_INIT = 0xE7;                         //蓝牙模块初始化错误

        public int mCount;                                   //仪器单签的错误信息计数，包括Flash存满已经擦除的信息
        public int mRecordCount;                            //当前共有几条错误信息，0表示没有信息
        public long mTimeInMillis;                           //表示错误信息的记录时间，精确到秒
        public int mErrorCode;                              //错误代码
    }

    public static class UserInfo {
        public String mId;                                      //账号，可以是用户名（英文、数字等字符），邮箱等，仪器默认为User1
        public String mSex;                                     //性别，male或female，仪器默认为male
        public String mAge;                                      //年龄
    }

    public static class FirmwareUpdate {
        public static final int UPDATE_READY = 0x00;        //设置进入IAP标志
        public static final int UPDATE_RESET = 0x01;        //系统复位重启
        public static final int UPDATE_START = 0x02;        //传输启动
        public static final int UPDATE_TRANSMIT = 0x03;     //传输bin文件
        public static final int UPDATE_END = 0x04;          //传输结束
        public static final int UPDATE_QUERY = 0x05;       //查询状态

        public class Callback {
            public static final int UPDATE_OK = 0x01;           //正常
            public static final int UPDATE_ERROR = 0x02;        //错误
            public static final int UPDATE_BUSY = 0x03;         //忙
        }

        public int mUpdateStep = -1;
        public InputStream mInputStream;
    }

    public static class FlashOperate {
        public static final int FLASH_PSW = 0x00;           //提交密码
        public static final int FLASH_INFO = 0x01;          //烧写仪器信息
        public static final int FLASH_DEFAULT = 0x02;       //出厂默认参数
        public static final int FLASH_USER = 0x03;          //擦除用户信息
        public static final int FLASH_ERROR = 0x04;         //擦除错误信息
        public static final int FLASH_PARA = 0x05;          //擦除雾化次数等记录参数
        public static final int FLASH_QUERY = 0x06;         //查询状态

        public class Callback {
            public static final int FLASH_OK = 0x01;         //完成
            public static final int FLASH_ERROR = 0x02;     //错误
            public static final int FLASH_BUSY = 0x03;      //忙
        }

        public String mBtName = "eLinkCareWHQ";         //蓝牙名称开始字符串
        public int mNebulizerRate = 2;                  //雾化速率对应参数，低俗：0.2ml/min，中速：0.5ml/min，高速：0.8ml/min，实际保存乘以10的整数，2，5，8
        public int mStopTime = 60;                      //停机定时时间：60秒
        public int mBtStopTime = 1200;                  //蓝牙关闭定时时间：1200秒
        public int mCellVoltageOrangeLimit = 34;      //电池电量橙色警告电压：3.4V，实际保存乘以10的整数，34
        public int mCellVoltageRedLimit = 32;          //电池电量红色警告电压：3.2V，实际保存乘以10的整数，32
        public int mNebulizeFrequency1 = 120;           //雾化器类型1谐振频率：120kHz
        public int mNebulizeFrequency2 = 120;           //雾化器类型2谐振频率：120kHz
        public int mNebulizeFrequency3 = 120;           //雾化器类型3谐振频率：120kHz
        public int mNebulizeFrequency4 = 120;           //雾化器类型4谐振频率：120kHz
        public int mNebulizeFrequency5 = 120;           //雾化器类型5谐振频率：120kHz
    }

    private class ReData {
        public byte cmd;
        public int len;
        public byte[] data = new byte[128];
        public int crc;

        public byte[] toBytes() {
            int totalLen = 2 + 1 + len + 2 + 1;
            byte[] res = new byte[totalLen];
            calCrc(this);
            res[0] = HEAD_LABEL_0;
            res[1] = HEAD_LABEL_1;
            res[2] = (byte) (len + 3);
            for (int i = 0; i < len; i++) {
                res[i + 3] = data[i];
            }
            res[len + 3] = (byte) crc;
            res[len + 4] = (byte) (crc >> 8);
            res[len + 5] = 0x0D;
            return res;
        }
    }

    public NebulizerController(BluetoothDevice device, Context context) {
        mDevice = device;
        if (mDevice == null || context == null) {
            throw new NullPointerException("device and context couldn't be null");
        }
        mBtAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
    }

    public synchronized void connect(Context context) {
        mBluetoothGatt = mDevice.connectGatt(context, false, mBtGattCallback);
    }

    public synchronized void disconnect() {
        mBluetoothGatt.disconnect();
    }

    public synchronized void writeData(byte[] data) {
        if (!mConnected) return;
        if (mWriteCharacteristic == null) {
            mGattService = mBluetoothGatt.getService(SERVICE_UUID);
            if (mGattService == null) return;
            mWriteCharacteristic = mGattService.getCharacteristic(CHARACTERISTIC_WRITE_UUID);
        }
        if (mWriteCharacteristic == null) return;
        mWriteCharacteristic.setValue(data);
        mBluetoothGatt.writeCharacteristic(mWriteCharacteristic);
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getDeviceAddress() {
        return mDevice.getAddress();
    }

    public String getDeviceName() {
        return mDevice.getName();
    }

    public void setNebulizerListener(NebulizerListener listener) {
        this.mListener = listener;
    }

    public synchronized void writeCommand(int cmd, byte[] data) {
        ReData reData = new ReData();
        reData.cmd = (byte) cmd;
        if (data != null) {
            reData.len = data.length + 1;
            for (int i = 0; i < data.length; i++) {
                reData.data[i + 1] = data[i];
            }
        } else {
            reData.len = 1;
        }
        reData.data[0] = reData.cmd;

        writeReDataList(reData);
    }

    private void writeReDataList(ReData data) {
        synchronized (mReDataList) {
            mReDataList.addLast(data);
        }
        initWriteThread();
    }

    private ReData readReDataList() {
        synchronized (mReDataList) {
            if (mReDataList.size() > 0)
                return mReDataList.pollFirst();
            else
                return null;
        }
    }

    private synchronized void initWriteThread() {
        if (mWriteThread != null) return;
        mWriteThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mReDataList.size() > 0) {
                    ReData data = readReDataList();
                    if (data == null) break;

                    byte[] bleData = data.toBytes();
                    int startIndex = 0;
                    while (bleData.length - startIndex > 20) {
                        writeData(Arrays.copyOfRange(bleData, startIndex, startIndex + 20));
                        startIndex += 20;
                    }

                    if (startIndex < bleData.length) {
                        writeData(Arrays.copyOfRange(bleData, startIndex, bleData.length));
                    }
                }
                mWriteThread = null;
            }
        });
        mWriteThread.start();
    }

    public InstrumentInfo getmInstInfo() {
        return this.mInstInfo;
    }

    public InstrumentState getmInstState() {
        return this.mInstState;
    }

    public CellState getmCellState() {
        return this.mCellState;
    }

    public ElectricCurrent getmElectricCurrent() {
        return this.mElectricCurrent;
    }

    public NebulizerData getNebulizerData() {
        return mNebulizerData;
    }

    public CupState getmCupSate() {
        return this.mCupSate;
    }

    public List<ErrorInfo> getErrorList(List<ErrorInfo> list) {
        if (list == null) list = new ArrayList<ErrorInfo>();
        list.clear();
        list.addAll(mErrorList);
        return list;
    }

    public UserInfo getmRegisteredUserInfo() {
        return mRegisteredUserInfo;
    }

    public UserInfo getmUserInfo() {
        return mUserInfo;
    }

    public boolean ismConnected()
    {
        return mConnected;
    }

    public void GET_IN_FACTORY_NAME() {
        writeCommand(CMD.CMD_GET_INST_INFO, new byte[]{InstrumentInfo.IN_FACTORY_NAME});
    }

    public void GET_INST_NAME() {
        writeCommand(CMD.CMD_GET_INST_INFO, new byte[]{InstrumentInfo.IN_INST_NAME});
    }

    public void GET_INST_TYPE() {
        writeCommand(CMD.CMD_GET_INST_INFO, new byte[]{InstrumentInfo.IN_INST_TYPE});
    }

    public void GET_INST_SN() {
        writeCommand(CMD.CMD_GET_INST_INFO, new byte[]{InstrumentInfo.IN_INST_SN});
    }

    public void GET_PRODUCT_DATE() {
        writeCommand(CMD.CMD_GET_INST_INFO, new byte[]{InstrumentInfo.IN_PRODUCT_DATE});
    }

    public void GET_FIRMWARE_VERSION() {
        writeCommand(CMD.CMD_GET_INST_INFO, new byte[]{InstrumentInfo.IN_FIRMWARE_VERSION});
    }

    public void GET_PCB_VERSION() {
        writeCommand(CMD.CMD_GET_INST_INFO, new byte[]{InstrumentInfo.IN_PCB_VERSION});
    }

    public void GET_PCBA_VERSION() {
        writeCommand(CMD.CMD_GET_INST_INFO, new byte[]{InstrumentInfo.IN_PCBA_VERSION});
    }

    public void GET_INST_STATE() {
        writeCommand(CMD.CMD_GET_INST_STATE, null);
    }

    public void GET_CELL_STATE() {
        writeCommand(CMD.CMD_GET_CELL_STATE, null);
    }

    public void GET_ELECTRIC_CURRENT() {
        writeCommand(CMD.CMD_GET_ELECTORIC_CURRENT, null);
    }

    public void GET_NEBULIZER_DATA(int i) {
        writeCommand(CMD.CMD_GET_NEBULIZER_DATA, new byte[]{(byte)(i & 0xFF)});
    }

    public void GET_CURRENT_NEBULIZER_DATA() {
        GET_NEBULIZER_DATA(0xEE);
    }

    public void GET_CUP_STATE() {
        writeCommand(CMD.CMD_GET_CUP_STATE, null);
    }

    public void GET_ERROR_INFO(int i) {
        writeCommand(CMD.CMD_GET_ERROR_INFO, new byte[]{(byte) i});
    }

    public void GET_CURRENT_ERROR_INFO() {
        GET_ERROR_INFO(0xee);
    }

    public void GET_REGISTERED_USER_INFO() {
        writeCommand(CMD.CMD_GET_REGISTERED_USER_INFO, null);
    }

    public void GET_USER_INFO() {
        writeCommand(CMD.CMD_GET_USER_INFO, null);
    }

    public void SET_NEBULIZING_RATE(int rate) {
        writeCommand(CMD.CMD_SET_NEBULIZING_RATE, new byte[]{(byte) rate});
    }

    public void SET_NEBULIZER_DOSE(int dose) {
        writeCommand(CMD.CMD_SET_NEBULIZER_DOSE, new byte[]{(byte) dose});
    }

    public void SET_NEBULIZER_TIME(long timeInMillis) {
        writeCommand(CMD.CMD_SET_NEBULIZER_TIME, getBytesTimeFromLong(timeInMillis));
    }

    public void SYN_NEBULIZER_TIME() {
        SET_NEBULIZER_TIME(Calendar.getInstance().getTimeInMillis());
    }

    public void SET_REGISTERED_USER_INFO(UserInfo userInfo) {
        writeCommand(CMD.CMD_SET_REGISTERED_USER_INFO, getBytesFromUserInfo(userInfo));
    }

    public void SET_USER_INFO(UserInfo userInfo) {
        writeCommand(CMD.CMD_SET_USER_INFO, getBytesFromUserInfo(userInfo));
    }

    public void UPDATE_FIRMWARE(File binaryFile) throws FileNotFoundException {
        mFirmwareUpdate.mInputStream = new FileInputStream(binaryFile);
        startUpdateFirmware();
    }

    public void SET_INST_INFO()
    {
        byte[][] bytes = new byte[8][];
        bytes[0] = mInstInfo.mFactoryName.getBytes();
        bytes[1] = mInstInfo.mInstrumentName.getBytes();
        bytes[2] = mInstInfo.mInstrumentType.getBytes();
        bytes[3] = mInstInfo.mInstrumentSN.getBytes();
        bytes[4] = mInstInfo.mProductDate.getBytes();
        bytes[5] = mInstInfo.mFirmwareVersion.getBytes();
        bytes[6] = mInstInfo.mPCBVersion.getBytes();
        bytes[7] = mInstInfo.mPCBAVersion.getBytes();
        int totalLen = 0;
        for(byte[] byteArray : bytes)
        {
            totalLen += byteArray.length + 1;
        }
        byte[] data = new byte[totalLen + 1];
        int startIndex = 1;
        for(int i = 0; i < bytes.length; i++)
        {
            System.arraycopy(bytes[i], 0, data, startIndex, bytes[i].length);
            startIndex += bytes[i].length + 1;
        }

        data[0] = FlashOperate.FLASH_INFO;
        writeCommand(CMD.CMD_SET_INST_PRODUCT_INFO, data);
    }

    public void SET_INST_PSW(byte[] psw)
    {
        byte[] data = new byte[psw.length + 1];
        data[0] = FlashOperate.FLASH_PSW;
        System.arraycopy(psw, 0, data, 1, psw.length);
        writeCommand(CMD.CMD_SET_INST_PRODUCT_INFO, data);
    }

    public void SET_PRODUCT_INFO()
    {
        byte[] bytesBtName = mFlashOperate.mBtName.getBytes();

        int btNameLen = (bytesBtName.length + 1) / 2 * 2;
        int totalLen = btNameLen + 20 + 1;
        byte[] data = new byte[totalLen];
        System.arraycopy(bytesBtName, 0, data, 1, bytesBtName.length);
        data[btNameLen + 1] =(byte) mFlashOperate.mNebulizerRate;
        data[btNameLen + 2] = (byte) (mFlashOperate.mNebulizerRate >> 8);
        data[btNameLen + 3] = (byte) mFlashOperate.mStopTime;
        data[btNameLen + 4] = (byte) (mFlashOperate.mStopTime >> 8);
        data[btNameLen + 5] = (byte) mFlashOperate.mBtStopTime;
        data[btNameLen + 6] = (byte) (mFlashOperate.mBtStopTime >> 8);
        data[btNameLen + 7] = (byte) mFlashOperate.mCellVoltageOrangeLimit;
        data[btNameLen + 8] = (byte) (mFlashOperate.mCellVoltageOrangeLimit >> 8);
        data[btNameLen + 9] = (byte) mFlashOperate.mCellVoltageRedLimit;
        data[btNameLen + 10] = (byte) (mFlashOperate.mCellVoltageRedLimit >> 8);
        data[btNameLen + 11] = (byte) mFlashOperate.mNebulizeFrequency1;
        data[btNameLen + 12] = (byte) (mFlashOperate.mNebulizeFrequency1 >> 8);
        data[btNameLen + 13] = (byte) mFlashOperate.mNebulizeFrequency2;
        data[btNameLen + 14] = (byte) (mFlashOperate.mNebulizeFrequency2 >> 8);
        data[btNameLen + 15] = (byte) mFlashOperate.mNebulizeFrequency3;
        data[btNameLen + 16] = (byte) (mFlashOperate.mNebulizeFrequency3 >> 8);
        data[btNameLen + 17] = (byte) mFlashOperate.mNebulizeFrequency4;
        data[btNameLen + 18] = (byte) (mFlashOperate.mNebulizeFrequency4 >> 8);
        data[btNameLen + 19] = (byte) mFlashOperate.mNebulizeFrequency5;
        data[btNameLen + 20] = (byte) (mFlashOperate.mNebulizeFrequency5 >> 8);

        data[0] = FlashOperate.FLASH_DEFAULT;
        writeCommand(CMD.CMD_SET_INST_PRODUCT_INFO, data);
    }

    public void ERASE_USER_INFO()
    {
        writeCommand(CMD.CMD_SET_INST_PRODUCT_INFO, new byte[]{FlashOperate.FLASH_USER});
    }

    public void ERASE_ERROR_INFO()
    {
        writeCommand(CMD.CMD_SET_INST_PRODUCT_INFO, new byte[]{FlashOperate.FLASH_ERROR});
    }

    public void ERASE_NEBULIZER_PARA()
    {
        writeCommand(CMD.CMD_SET_INST_PRODUCT_INFO, new byte[]{FlashOperate.FLASH_PARA});
    }

    public void FLASH_QUERY()
    {
        writeCommand(CMD.CMD_SET_INST_PRODUCT_INFO, new byte[]{FlashOperate.FLASH_QUERY});
    }

    public void GET_PRODUCT_INFO()
    {
        writeCommand(CMD.CMD_GET_PRODUCT_INFO, null);
    }

    public void RESET_BLUETOOTH() {
        writeCommand(CMD.CMD_RESET_BLUETOOTH, null);
    }

    private void startUpdateFirmware() {
        if (mFirmwareUpdate.mUpdateStep >= 0) return;
        if (mFirmwareUpdate.mInputStream == null) return;
        writeFirmware(FirmwareUpdate.UPDATE_READY, null);
    }

    private void updateFirmware() {
        if(mFirmwareUpdate.mUpdateStep < 0)return;
        int byteCount = 32;
        byte[] fb = new byte[byteCount];
        int crc = 0x00;

        switch (mFirmwareUpdate.mUpdateStep) {
            case 0:
                writeFirmware(FirmwareUpdate.UPDATE_RESET, null);
                mFirmwareUpdate.mUpdateStep = 1;
                break;
            case 1: //下位机重启
                writeFirmware(FirmwareUpdate.UPDATE_QUERY, null);
                mFirmwareUpdate.mUpdateStep = 2;
                break;
            case 2: //启动传输，发送数据长度
                try {
                    int firmwareLen = mFirmwareUpdate.mInputStream.available();
                    byte[] data = new byte[]{(byte) firmwareLen, (byte) (firmwareLen >> 8), (byte) (firmwareLen >> 16), (byte) (firmwareLen >> 24)};
                    writeFirmware(FirmwareUpdate.UPDATE_START, data);
                    mFirmwareUpdate.mUpdateStep = 3;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            case 3: //传输固件
            {

                try {
                    int readCount = mFirmwareUpdate.mInputStream.read(fb);
                    if (readCount == 0) {
                        mFirmwareUpdate.mUpdateStep = 4;
                        mFirmwareUpdate.mInputStream.close();
                        mFirmwareUpdate.mInputStream = null;
                        break;
                    }
                    for (int i = 0; i < readCount; i++) {
                        crc += fb[i];
                    }
                    if (readCount < fb.length) {
                        writeFirmware(FirmwareUpdate.UPDATE_TRANSMIT, Arrays.copyOf(fb, readCount));
                    } else {
                        writeFirmware(FirmwareUpdate.UPDATE_TRANSMIT, fb);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            break;
            case 4: {
                byte[] data = new byte[]{(byte) crc, (byte) (crc >> 8)};
                writeFirmware(FirmwareUpdate.UPDATE_END, data);
                mFirmwareUpdate.mUpdateStep = 5;
            }
            break;
            case 5:
                mFirmwareUpdate.mUpdateStep = 0;
                break;
        }
    }

    private void writeFirmware(int updateState, byte[] data) {
        byte[] firmWareData;
        if (data != null) {
            firmWareData = new byte[data.length + 1];
            System.arraycopy(data, 0, firmWareData, 1, data.length);
        } else {
            firmWareData = new byte[1];
        }

        firmWareData[0] = (byte) updateState;

        writeCommand(CMD.CMD_UPDATE_FIRMWARE, firmWareData);
    }

    private BluetoothGattCallback mBtGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

                refreshConnectionState(true);
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                refreshConnectionState(false);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mBluetoothGatt != null) {
                    mGattService = mBluetoothGatt.getService(SERVICE_UUID);
                    if (mGattService == null) return;
                    mReadCharacteristic = mGattService.getCharacteristic(CHARACTERISTIC_READ_UUID);
                    if (mReadCharacteristic != null)
                        mBluetoothGatt.setCharacteristicNotification(mReadCharacteristic, true);
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            refreshDataReceived(characteristic.getValue(), characteristic.getUuid());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            refreshDataReceived(characteristic.getValue(), characteristic.getUuid());
        }
    };

    private void refreshConnectionState(boolean connected) {
        mConnected = connected;
        if (mListener == null) return;
        mListener.onConnectionStateChanged(connected);
    }

    private void refreshDataReceived(byte[] data, UUID uuid) {
        writeDataToBuffer(data);
        if (mListener == null) return;
        mListener.onDataReceived(data, uuid);
    }

    private void refreshNebulizer(int cmd, int refreshType) {
        if (mListener == null) return;
        mListener.onNebulizerRefreshed(cmd, refreshType);
    }

    private void refreshError(int errorCode, long time) {
        if (mListener == null) return;
        mListener.onError(errorCode, getErrorString(errorCode), time);
    }

    private synchronized void writeDataToBuffer(byte[] data) {
        if (data == null || data.length == 0) return;
        if (READ_BUFFER_MAX_LEN - mBufferDataLen < data.length) {
            mBufferDataLen = 0;
            mBufferReadIndex = 0;
            mBufferWriteIndex = 0;
        }

        for (int i = 0; i < data.length; i++) {
            mReadDataBuffer[mBufferWriteIndex++] = data[i];
            if (mBufferWriteIndex >= READ_BUFFER_MAX_LEN) mBufferWriteIndex = 0;
        }

        mBufferDataLen += data.length;

        ReData reData = verifyCrc(readDataFromBuffer());
        if (reData != null) handleReData(reData);
    }

    private synchronized ReData readDataFromBuffer() {
        int index = mBufferReadIndex;
        while (index != mBufferWriteIndex) {
            int nextIndex = index + 1;
            if (nextIndex >= READ_BUFFER_MAX_LEN) nextIndex = 0;
            if (mReadDataBuffer[index] == HEAD_LABEL_0 && mReadDataBuffer[nextIndex] == HEAD_LABEL_1) {
                int lenIndex = index + 2;
                if (lenIndex >= READ_BUFFER_MAX_LEN) lenIndex -= READ_BUFFER_MAX_LEN;
                int len = (int) (mReadDataBuffer[lenIndex] & 0xFF);
                if (len + 3 <= mBufferDataLen) {
                    ReData data = new ReData();
                    data.len = len - 3;
                    for (int i = 0; i < data.len + 6; i++) {
                        if (i < 3) {
                            mBufferReadIndex++;    //read head
                        } else if (i == data.len + 3) {
                            data.crc = (int) (mReadDataBuffer[mBufferReadIndex++] & 0xFF); //read crc
                        } else if (i == data.len + 4) {
                            data.crc = data.crc | (((int) (mReadDataBuffer[mBufferReadIndex++] & 0xFF)) << 8); //read crc
                        } else {
                            data.data[i - 3] = mReadDataBuffer[mBufferReadIndex++]; //read data
                        }

                        mBufferDataLen--;
                        if (mBufferReadIndex >= READ_BUFFER_MAX_LEN) {
                            mBufferReadIndex = 0;
                        }
                    }
                    data.cmd = data.data[0];
                    Log.e("NEBULIZER CONTROLLER", "GET DATA CMD = " + data.data[0] + ", buffer read index = " + mBufferReadIndex);
                    return data;
                } else {
                    return null;
                }

            } else {
                mBufferReadIndex++;
                mBufferDataLen--;
                if (mBufferReadIndex >= READ_BUFFER_MAX_LEN) mBufferReadIndex = 0;
                index = mBufferReadIndex;
            }
        }
        return null;
    }

    private void handleReData(ReData data) {
        switch ((int) (data.data[0] & 0xFF)) {
            case CMD.CMD_GET_INST_INFO:
                handleInstrumentInfo(data);
                break;
            case CMD.CMD_GET_INST_STATE:
                handleInstrumentState(data);
                break;
            case CMD.CMD_GET_CELL_STATE:
                handleCellState(data);
                break;
            case CMD.CMD_GET_ELECTORIC_CURRENT:
                handleElectricCurrent(data);
                break;
            case CMD.CMD_GET_NEBULIZER_DATA:
                handleNebulizerData(data);
                break;
            case CMD.CMD_GET_CUP_STATE:
                handleCupState(data);
                break;
            case CMD.CMD_GET_ERROR_INFO:
                handleError(data);
                break;
            case CMD.CMD_GET_REGISTERED_USER_INFO:
                handleRegisteredUserInfo(data);
                break;
            case CMD.CMD_GET_USER_INFO:
                handleUserInfo(data);
                break;
            case CMD.CMD_ACK_ERROR:
                handleAckError(data);
                break;
            case CMD.CMD_SET_NEBULIZING_RATE:
                handleSetNebulizerRate(data);
                break;
            case CMD.CMD_SET_NEBULIZER_DOSE:
                handleSetNebulizerDose(data);
                break;
            case CMD.CMD_SET_NEBULIZER_TIME:
                handleSetNebulizerTime(data);
                break;
            case CMD.CMD_SET_REGISTERED_USER_INFO:
                handleSetRegsiteredUser(data);
                break;
            case CMD.CMD_SET_USER_INFO:
                handleSetUserInfo(data);
                break;
            case CMD.CMD_UPDATE_FIRMWARE:
                handleFirmwareUpdate(data);
                break;
            case CMD.CMD_SET_INST_PRODUCT_INFO:
                handleFlashOperate(data);
                break;
            case CMD.CMD_GET_PRODUCT_INFO:
                handleGetProductInfo(data);
                break;
            case CMD.CMD_RESET_BLUETOOTH:
                handleResetBluetooth(data);
                break;
            default:
                Log.e(TAG, "unknown Nebulizer Command " + String.format("0x%02x", data.data[0]));
        }
    }

    private void handleInstrumentInfo(ReData data) {
        int infoType = data.data[1];
        String info = new String(Arrays.copyOfRange(data.data, 2, data.data.length));
        switch (infoType) {
            case InstrumentInfo.IN_FACTORY_NAME:
                mInstInfo.mFactoryName = info;
                break;
            case InstrumentInfo.IN_INST_NAME:
                mInstInfo.mInstrumentName = info;
                break;
            case InstrumentInfo.IN_INST_TYPE:
                mInstInfo.mInstrumentType = info;
                break;
            case InstrumentInfo.IN_INST_SN:
                mInstInfo.mInstrumentSN = info;
                break;
            case InstrumentInfo.IN_PRODUCT_DATE:
                mInstInfo.mProductDate = info;
                break;
            case InstrumentInfo.IN_FIRMWARE_VERSION:
                mInstInfo.mFirmwareVersion = info;
                break;
            case InstrumentInfo.IN_PCB_VERSION:
                mInstInfo.mPCBVersion = info;
                break;
            case InstrumentInfo.IN_PCBA_VERSION:
                mInstInfo.mPCBAVersion = info;
                break;
        }

        this.refreshNebulizer(CMD.CMD_GET_INST_INFO, infoType);
    }

    private void handleInstrumentState(ReData data) {
        mInstState.mInstrumentState = (int) (data.data[1] & 0xFF);

        this.refreshNebulizer(CMD.CMD_GET_INST_STATE, -1);
    }

    private void handleCellState(ReData data) {
        mCellState.mCellState = (int) (data.data[1] & 0xFF);

        this.refreshNebulizer(CMD.CMD_GET_CELL_STATE, -1);
    }

    private void handleElectricCurrent(ReData data) {
        int eCurrent = (int) (data.data[1] & 0xFF);
        eCurrent = (eCurrent << 8) | data.data[0];
        mElectricCurrent.mElectricCurrent = eCurrent / 4095.f * 2500.f / 11.f;

        this.refreshNebulizer(CMD.CMD_GET_ELECTORIC_CURRENT, -1);
    }

    private void handleNebulizerData(ReData data) {
        NebulizerData nebulizerData = mNebulizerData;
        nebulizerData.mCount = (int) (data.data[2] & 0xFF);
        nebulizerData.mCount = (nebulizerData.mCount << 8) | (data.data[1] & 0xFF);
        nebulizerData.mRecordCount = (int) (data.data[3] & 0xFF);
        nebulizerData.mStartTimeInMillis = getLongTimeFromBytes(Arrays.copyOfRange(data.data, 4, 10));
        nebulizerData.mCurrentTimeInMillis = getLongTimeFromBytes(Arrays.copyOfRange(data.data, 10, 16));
        long nebulizedTimeInSecond = (long) (data.data[17] & 0xFF);
        nebulizedTimeInSecond = (nebulizedTimeInSecond << 8) | (data.data[16] & 0xFF);
        nebulizerData.mNebulizedTimeInMillis = nebulizedTimeInSecond * 1000;
        nebulizerData.mNebulizedDose = (int) (data.data[18] & 0xFF);
        nebulizerData.mPredesignedDose = (int) (data.data[19] & 0xFF);
        nebulizerData.mNebulizingRate = (int) (data.data[20] & 0xFF);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd, hh:mm:ss");
        Log.e(TAG, "start time " + format.format(nebulizerData.mStartTimeInMillis));

        refreshNebulizer(CMD.CMD_GET_NEBULIZER_DATA, -1);
    }

    private void handleCupState(ReData data) {
        mCupSate.mCupState = (int) (data.data[1] & 0xFF);
        refreshNebulizer(CMD.CMD_GET_CUP_STATE, -1);
    }

    private void handleError(ReData data) {
        ErrorInfo error = new ErrorInfo();
        error.mCount = (int) (data.data[2] & 0xFF);
        error.mCount = (error.mCount << 8) | (data.data[1] & 0xFF);

        error.mRecordCount = (int) (data.data[3] & 0xFF);
        error.mTimeInMillis = getLongTimeFromBytes(Arrays.copyOfRange(data.data, 4, 10));
        error.mErrorCode = (int) (data.data[10] & 0xFF);
        mErrorList.add(error);
        refreshError(error.mErrorCode, error.mTimeInMillis);
    }

    private void handleRegisteredUserInfo(ReData data) {
        getUserInfo(mRegisteredUserInfo, data);
        refreshNebulizer(CMD.CMD_GET_REGISTERED_USER_INFO, -1);
    }

    private void handleUserInfo(ReData data) {
        getUserInfo(mUserInfo, data);
        refreshNebulizer(CMD.CMD_GET_USER_INFO, -1);
    }

    private void handleAckError(ReData data) {
        int errorCode = (int) (data.data[1] & 0xFF);
        refreshError(errorCode, Calendar.getInstance().getTimeInMillis());
    }

    private void handleSetNebulizerRate(ReData data)
    {
        refreshNebulizer(CMD.CMD_SET_NEBULIZING_RATE, -1);
    }

    private void handleSetNebulizerDose(ReData data)
    {
        refreshNebulizer(CMD.CMD_SET_NEBULIZER_DOSE, -1);
    }

    private void handleSetNebulizerTime(ReData data)
    {
        Log.e(TAG, "SET TIME SUCCESS");
        refreshNebulizer(CMD.CMD_SET_NEBULIZER_TIME, -1);
    }

    private void handleSetRegsiteredUser(ReData data)
    {
        refreshNebulizer(CMD.CMD_SET_REGISTERED_USER_INFO, -1);
    }

    private void handleSetUserInfo(ReData data)
    {
        refreshNebulizer(CMD.CMD_SET_USER_INFO, -1);
    }

    private void handleFirmwareUpdate(ReData data) {
        switch (data.data[1]) {
            case FirmwareUpdate.Callback.UPDATE_OK:
                updateFirmware();
                break;
            case FirmwareUpdate.Callback.UPDATE_ERROR:
            case FirmwareUpdate.Callback.UPDATE_BUSY:
                mFirmwareUpdate.mUpdateStep = 0;
                try {
                    mFirmwareUpdate.mInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void handleFlashOperate(ReData data)
    {
        switch (data.data[1])
        {
            case FlashOperate.Callback.FLASH_OK:
                break;
            case FlashOperate.Callback.FLASH_ERROR:
            case FlashOperate.Callback.FLASH_BUSY:
                break;
        }
    }

    private void handleGetProductInfo(ReData data)
    {
        int startIndex = 1;
        for(int i = 0; i < data.data.length; i++)
        {
            if(data.data[i] == 0x00)
            {
                mFlashOperate.mBtName = new String(Arrays.copyOf(data.data, i));
                startIndex = i + 1;
                break;
            }
        }
        mFlashOperate.mNebulizerRate = data.data[startIndex] & 0xFF | (data.data[startIndex + 1] & 0xFF) << 8;
        mFlashOperate.mStopTime = data.data[startIndex + 2] & 0xFF | (data.data[startIndex + 3] & 0xFF) << 8;
        mFlashOperate.mBtStopTime = data.data[startIndex + 4] & 0xFF | (data.data[startIndex + 5] & 0xFF) << 8;
        mFlashOperate.mCellVoltageOrangeLimit = data.data[startIndex + 6] & 0xFF | (data.data[startIndex + 7] & 0xFF) << 8;
        mFlashOperate.mCellVoltageRedLimit = data.data[startIndex + 8] & 0xFF | (data.data[startIndex + 9] & 0xFF) << 8;
        mFlashOperate.mNebulizeFrequency1 = data.data[startIndex + 10] & 0xFF | (data.data[startIndex + 11] & 0xFF) << 8;
        mFlashOperate.mNebulizeFrequency2 = data.data[startIndex + 12] & 0xFF | (data.data[startIndex + 13] & 0xFF) << 8;
        mFlashOperate.mNebulizeFrequency3 = data.data[startIndex + 14] & 0xFF | (data.data[startIndex + 15] & 0xFF) << 8;
        mFlashOperate.mNebulizeFrequency4 = data.data[startIndex + 16] & 0xFF | (data.data[startIndex + 17] & 0xFF) << 8;
        mFlashOperate.mNebulizeFrequency5 = data.data[startIndex + 18] & 0xFF | (data.data[startIndex + 19] & 0xFF) << 8;

        refreshNebulizer(CMD.CMD_GET_PRODUCT_INFO, -1);
    }

    private void handleResetBluetooth(ReData data)
    {
        refreshNebulizer(CMD.CMD_RESET_BLUETOOTH, -1);
    }

    /**
     * @param data
     * @return time in millisecond
     */
    private long getLongTimeFromBytes(byte[] data) {
        int year = (int) (data[0] & 0xFF);
        int month = (int) (data[1] & 0xFF);
        int day = (int) (data[2] & 0xFF);
        int hour = (int) (data[3] & 0xFF);
        int min = (int) (data[4] & 0xFF);
        int sec = (int) (data[5] & 0xFF);

        Log.e(TAG, "time: " + year + "-" + month + "-" + day + "-" + hour + "-" + min + "-" + sec);
        Calendar cal = Calendar.getInstance();
        if (year < 50)
            cal.set(Calendar.YEAR, 2000 + year);
        else
            cal.set(Calendar.YEAR, 1900 + year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, sec);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * @param timeInMillis
     * @return yy-mm-dd-hh-mm-ss
     */
    private byte[] getBytesTimeFromLong(long timeInMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeInMillis);

        byte[] data = new byte[6];
        int year = cal.get(Calendar.YEAR);
        data[0] = (byte) (year % 100);
        data[1] = (byte) (cal.get(Calendar.MONTH) + 1);
        data[2] = (byte) (cal.get(Calendar.DAY_OF_MONTH));
        data[3] = (byte) (cal.get(Calendar.HOUR_OF_DAY));
        data[4] = (byte) (cal.get(Calendar.MINUTE));
        data[5] = (byte) (cal.get(Calendar.SECOND));

        Log.e(TAG, "get times bytes " + data[0] + "-" + data[1] + "-" + data[2] + "-" + data[3] + "-" + data[4] + "-" + data[5]);
        return data;
    }

    private String getErrorString(int errorCode) {
        switch (errorCode) {
            case ErrorInfo.ERR_CUP_CONNECTION_EXCEPTION:
                return "nebulizer cup connection exception";
            case ErrorInfo.ERR_READ_INST_INFO_CHECK:
                return "read instrument information error";
            case ErrorInfo.ERR_READ_FACTORY_INFO_CHECK:
                return "read factory information error";
            case ErrorInfo.ERR_READ_USER_INFO_CHECK:
                return "read user information error";
            case ErrorInfo.ERR_READ_ERROR_INFO_CHECK:
                return "read error information error";
            case ErrorInfo.ERR_READ_NEBU_INFO_CHECK:
                return "read nebulizer data error";
            case ErrorInfo.ERR_BT_COM_CHECK:
                return "bluetooth communication error";
            case ErrorInfo.ERR_BT_INIT:
                return "bluetooth module initialization error";
            default:
                return "unknown error: " + errorCode;
        }
    }

    private void getUserInfo(UserInfo userInfo, ReData data) {
        int startIndex;
        startIndex = 1;
        userInfo.mId = null;
        userInfo.mSex = null;
        userInfo.mAge = null;
        for (int i = 1; i < data.len; i++) {
            if (data.data[i] == 0x00) {
                String string = new String(Arrays.copyOfRange(data.data, startIndex, i));
                if (userInfo.mId == null) {
                    userInfo.mId = string;
                } else if (userInfo.mSex == null) {
                    userInfo.mSex = string;
                } else {
                    userInfo.mAge = string;
                }
                startIndex = i + 1;
            }
        }
    }

    private byte[] getBytesFromUserInfo(UserInfo userInfo) {
        if (userInfo.mId == null || userInfo.mSex == null || userInfo.mAge == null) return null;
        byte[] bytesUserId = userInfo.mId.getBytes();
        byte[] bytesSex = userInfo.mSex.getBytes();
        byte[] bytesAge = userInfo.mAge.getBytes();
        byte[] data = new byte[bytesUserId.length + 1 + bytesSex.length + 1 + bytesAge.length + 1];
        System.arraycopy(bytesUserId, 0, data, 0, bytesUserId.length);
        System.arraycopy(bytesSex, 0, data, bytesUserId.length + 1, bytesSex.length);
        System.arraycopy(bytesAge, 0, data, bytesUserId.length + 1 + bytesSex.length + 1, bytesAge.length);
        return data;
    }

    private int calCrc(ReData data) {
        int crc = 0x55 + 0xAA + data.len + 3;
        for (int i = 0; i < data.len; i++) {
            crc += (int) (data.data[i] & 0xFF);
        }
        data.crc = crc;
        return crc;
    }

    private ReData verifyCrc(ReData data) {
        if (data == null) return null;
        int crc = data.crc;
        int cal_crc = calCrc(data);
        data.crc = crc;
        if (crc == cal_crc) {
            Log.e("verify ReData", "receive data: " + data.data[0]);
            return data;
        } else {
            Log.e("verify ReData", "crc failed crc = " + crc + "cal_crc = " + cal_crc);
        }
        return null;
    }
}
