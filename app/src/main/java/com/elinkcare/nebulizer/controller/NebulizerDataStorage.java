package com.elinkcare.nebulizer.controller;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Administrator on 2016/3/29.
 */
public class NebulizerDataStorage {

    private SQLiteDatabase mDatabase;

    private OnDatasetChangedListener mDatasetChangedListener;

    public static interface OnDatasetChangedListener
    {
        public void datasetChanged();
    }

    public NebulizerDataStorage(Context context, String databaseFileName)
    {
        mDatabase = context.openOrCreateDatabase(databaseFileName, Context.MODE_PRIVATE, null);
        String sql = "CREATE TABLE IF NOT EXISTS nebulizer_data ("
                + "start_time INT PRIMARY KEY,"
                + "period INT NOT NULL)";
        mDatabase.execSQL(sql);
    }

    public static class NebulizerRecordData
    {
        public long startTime;
        public long period;
    }

    public static class NebulizerRecordStatistic
    {
        public int times;
        public long totalPeriod;
        public List<NebulizerRecordData> dataList;
    }

    public void saveData(NebulizerRecordData data)
    {
        String sql = "REPLACE INTO nebulizer_data VALUES("
                + "'" + String.format("%d", data.startTime) + "',"
                + "'" + String.format("%d", data.period) + "')";
        mDatabase.execSQL(sql);

        notifyDatasetChanged();
    }

    public void deleteData(NebulizerRecordData data)
    {
        String sql = "DELETE FROM nebulizer_data WHERE "
                + "start_time = '" + data.startTime + "'";
        mDatabase.execSQL(sql);

        notifyDatasetChanged();
    }

    public NebulizerRecordData getData(long startTime)
    {
        String sql = "SELECT * FROM nebulizer_data WHERE "
                + "start_time = '" + startTime + "'";
        Cursor cursor = mDatabase.rawQuery(sql, null);
        while(cursor.moveToNext())
        {
            NebulizerRecordData data = new NebulizerRecordData();
            data.startTime = cursor.getLong(cursor.getColumnIndex("start_time"));
            data.period = cursor.getLong(cursor.getColumnIndex("period"));
            return data;
        }
        return null;
    }

    public List<NebulizerRecordData> getDataInOneDay(long startTime)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startTime);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long beginTime = cal.getTimeInMillis();
        long endTime = beginTime + 24 * 3600 * 1000;

        String sql = "SELECT * FROM nebulizer_data WHERE "
                + "start_time >= '" + beginTime + "' "
                + "AND start_time < '" + endTime + "'";

        Cursor cursor = mDatabase.rawQuery(sql, null);
        List<NebulizerRecordData> dataList = new ArrayList<NebulizerRecordData>();
        while(cursor.moveToNext())
        {
            NebulizerRecordData data = new NebulizerRecordData();
            data.startTime = cursor.getLong(cursor.getColumnIndex("start_time"));
            data.period = cursor.getLong(cursor.getColumnIndex("period"));
            dataList.add(data);
        }

        return dataList.size() == 0?null : dataList;
    }

    public NebulizerRecordStatistic getRecordStatistic(long startTime)
    {
        List<NebulizerRecordData> dataList = getDataInOneDay(startTime);
        if(dataList == null || dataList.size() == 0)return null;
        NebulizerRecordStatistic statistic = new NebulizerRecordStatistic();
        statistic.dataList = dataList;
        statistic.times = 0;
        statistic.totalPeriod = 0;
        for(NebulizerRecordData data : dataList)
        {
            statistic.times ++;
            statistic.totalPeriod += data.period;
        }
        return statistic;
    }

    public void setOnDatasetChangedListener(OnDatasetChangedListener listener)
    {
        mDatasetChangedListener = listener;
    }

    public void close()
    {
        mDatabase.close();
    }

    private void notifyDatasetChanged()
    {
        if(mDatasetChangedListener != null)
        {
            mDatasetChangedListener.datasetChanged();
        }
    }
}
