package com.tencent.tavmedia.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.tavmedia.TAVComposition;
import com.tencent.tavmedia.TAVFont;
import com.tencent.tavmedia.TAVPAGEffect;
import com.tencent.tavmedia.TAVPAGTextAttribute;

public class MultiClipActivity extends AppCompatActivity {

    /**
     * 此页面seekBar的max设置过大会干扰到TAVTextureView的刷新率，导致卡顿现象，应该是Android UI系统的问题
     */
    private static final int MAX_PROGRESS = 1000;
    private FrameLayout flRoot;
    private SeekBar seekBar;
    private TAVTextureView textureView;

    public static void start(Context context) {
        Intent starter = new Intent(context, MultiClipActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_clip);
        flRoot = findViewById(R.id.fl_root);
        flRoot.post(this::initTextureView);
        seekBar = findViewById(R.id.seekBar);
        seekBar.setMax(MAX_PROGRESS);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || textureView == null) {
                    return;
                }
                textureView.seekTo(progress * 1.0 / MAX_PROGRESS);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    private void initTextureView() {

        textureView = new TAVTextureView(this);
        textureView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        TAVComposition composition = Utils.makeComposition(MainActivity.SELECT_DATA, displayMetrics.widthPixels,
                displayMetrics.heightPixels
        );
//        TAVComposition composition = TAVComposition.Make(displayMetrics.widthPixels, displayMetrics.heightPixels, 0,10000000);
//        // 替换文本
        TAVPAGEffect title = Utils.makePAGEffect("font.pag", displayMetrics.widthPixels,
                displayMetrics.heightPixels);
        // register font：没有需求的话不需要注册字体库，默认使用Android系统字体
        boolean sucess = TAVFont.RegisterFont(Utils.OUT_SAVE_DIR + "Source Han Sans CN-Regular.ttc", 0, "Source Han Sans CN", "Regular");
        if (!sucess) {
            throw new RuntimeException("register font failed");
        }
        // unregister font：取消掉注册的字体后，会回到Android字体
//        TAVFont.UnregisterFont("PingFang SC", "Semibold");
        title.setDuration(composition.duration());

        composition.addClip(title);
        textureView.setMedia(composition);
        textureView.setPlayerListener(process -> seekBar.setProgress((int) (process * MAX_PROGRESS)));
        flRoot.addView(textureView);

        flRoot.setOnClickListener(v -> {
            if (flag) {
                Toast.makeText(this, "设置字体", Toast.LENGTH_SHORT).show();
                TAVPAGTextAttribute attribute = new TAVPAGTextAttribute();
                attribute.fontFamily = "Source Han Sans CN";
                attribute.fontStyle = "Regular";
                attribute.fontSize = 60;
                attribute.text = "文本替换试验";
                title.replaceText(0, attribute);
            } else {
                TAVPAGTextAttribute attribute = new TAVPAGTextAttribute();
                attribute.fontSize = 60;
                attribute.text = "文本替换试验";
                Toast.makeText(this, "取消字体", Toast.LENGTH_SHORT).show();
                title.replaceText(0, attribute);
            }
            flag = !flag;
        });
    }

    private boolean flag = false;

}