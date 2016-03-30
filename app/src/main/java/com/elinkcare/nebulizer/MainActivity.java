package com.elinkcare.nebulizer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    private Button bt_test;
    private Button bt_test1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initOnAction();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Intent intent = new Intent();
        intent.setClass(getBaseContext(), NebulizerControlActivity.class);
        startActivity(intent);
        finish();
    }

    private void initView()
    {
        bt_test = (Button) findViewById(R.id.bt_test);
        bt_test1 = (Button) findViewById(R.id.bt_test1);
    }

    private void initOnAction()
    {
        bt_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(getBaseContext(), ScanActivity.class);
                startActivity(intent);
            }
        });

        bt_test1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(getBaseContext(), NebulizerControlActivity.class);
                startActivity(intent);
            }
        });
    }
}
