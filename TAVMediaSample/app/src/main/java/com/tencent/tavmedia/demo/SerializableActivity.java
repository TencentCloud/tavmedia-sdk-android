package com.tencent.tavmedia.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.tavmedia.TAVComposition;

public class SerializableActivity extends AppCompatActivity {

    private static final String TAG = "SerializableActivity";

    public static void start(Context context) {
        Intent starter = new Intent(context, SerializableActivity.class);
        context.startActivity(starter);
    }

    private FrameLayout flRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serializable);

        flRoot = findViewById(R.id.fl_root);
        flRoot.post(this::initTextureView);
    }


    private void initTextureView() {
        TAVTextureView textureView = new TAVTextureView(this);
        textureView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        String json = Utils.loadJSONFromAssets(this, "tavmedia.json");
        TAVComposition composition = TAVComposition.MakeFromJson(json);
        textureView.setMedia(composition);
        flRoot.addView(textureView);

        // 对Composition进行编辑后，调用toJson存草稿
        Log.d(TAG, "initTextureView: json = " + composition.toJson());
    }

}