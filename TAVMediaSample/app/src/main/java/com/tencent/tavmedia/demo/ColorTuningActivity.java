package com.tencent.tavmedia.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.tavmedia.TAVColorTuning;
import com.tencent.tavmedia.TAVColorTuningEffect;
import com.tencent.tavmedia.TAVComposition;
import com.tencent.tavmedia.TAVProperty;

public class ColorTuningActivity extends AppCompatActivity {

    private FrameLayout flRoot;
    private TAVColorTuningEffect effect;

    public static void start(Context context) {
        Intent starter = new Intent(context, ColorTuningActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_tuning);
        flRoot = findViewById(R.id.fl_root);
        flRoot.post(this::initTextureView);
    }


    private void initTextureView() {

        TAVTextureView textureView = new TAVTextureView(this);
        textureView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        TAVComposition composition = Utils.makeComposition(MainActivity.SELECT_DATA, displayMetrics.widthPixels,
                displayMetrics.heightPixels
        );

        TAVColorTuning colorTuning = getTavColorTuning(10);
        effect = TAVColorTuningEffect.Make(colorTuning);
        effect.setDuration(composition.duration());
        effect.addInput(composition);

        TAVComposition root = TAVComposition.Make(composition.width(), composition.height(), 0, composition.duration());
        root.setDuration(composition.duration());
        root.addClip(effect);

        textureView.setMedia(root);
        flRoot.addView(textureView);
    }


    private TAVColorTuning getTavColorTuning(float value) {
        TAVColorTuning colorTuning = new TAVColorTuning();
        colorTuning.kelvin = new TAVProperty(0);
        colorTuning.hue = new TAVProperty(0);
        colorTuning.saturation = new TAVProperty(0);
        colorTuning.brightness = new TAVProperty(value);
        colorTuning.contrast = new TAVProperty(value);
        colorTuning.exposure = new TAVProperty(0);
        colorTuning.gamma = new TAVProperty(0);
        return colorTuning;
    }

    public void updateColorTuning() {
        effect.updateColorTuning(getTavColorTuning(20));
    }
}