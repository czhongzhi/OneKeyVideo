package com.runbo.onekeyvideo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button).setOnClickListener(this);

        startService(new Intent(this,RecordService.class));

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button:
                stopService(new Intent(this,RecordService.class));
                finish();
                break;
        }
    }
}
