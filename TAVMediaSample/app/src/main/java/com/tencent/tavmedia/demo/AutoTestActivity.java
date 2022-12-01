package com.tencent.tavmedia.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class AutoTestActivity extends AppCompatActivity {

    public static void start(Context context) {
        Intent starter = new Intent(context, AutoTestActivity.class);
        context.startActivity(starter);
    }

    private ExportTask exportTask;
    private Button btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_test);
        btnStart = findViewById(R.id.btn_start);
        exportTask = new ExportTask(this);
//        btnStart.post(this::startAutoTest);
    }

    public void startTest(View view) {
        startAutoTest();
    }

    void onTestFinish() {
        setEnabled(true);

    }

    void onExportStart() {

    }

    private void startAutoTest() {
        setEnabled(false);
        exportTask.start();
    }

    private void setEnabled(boolean enabled) {
        runOnUiThread(() -> btnStart.setEnabled(enabled));
    }
}