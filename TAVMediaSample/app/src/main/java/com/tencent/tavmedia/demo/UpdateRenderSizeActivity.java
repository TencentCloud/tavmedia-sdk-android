package com.tencent.tavmedia.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.tavmedia.TAVComposition;

public class UpdateRenderSizeActivity extends AppCompatActivity {

    private FrameLayout flRoot;

    public static void start(Context context) {
        Intent starter = new Intent(context, UpdateRenderSizeActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_render_size);
        flRoot = findViewById(R.id.fl_root);
        flRoot.post(() -> initTextureView(720f, 1280f));
    }

    private void initTextureView(float width, float height) {
        flRoot.removeAllViews();
        TAVTextureView textureView = new TAVTextureView(this);
        textureView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        TAVComposition composition = Utils.makeComposition(MainActivity.SELECT_DATA, width, height);
        Utils.fitToTarget(composition, displayMetrics.widthPixels, displayMetrics.heightPixels);

        textureView.setMedia(composition);
        flRoot.addView(textureView);
    }


    public void switchRenderSize(View view) {
        initTextureView(1000f, 1000f);
    }
}