package com.tencent.tavmedia.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.tavmedia.TAVComposition;
import com.tencent.tavmedia.TAVMovie;
import com.tencent.tavmedia.TAVMovieAsset;
import com.tencent.tavmedia.TAVPAGEditableInfo;
import com.tencent.tavmedia.TAVPAGEffect;
import com.tencent.tavmedia.TAVPAGImageLayerInfo;
import com.tencent.tavmedia.TAVPAGImageReplacement;

public class PAGTemplateActivity extends AppCompatActivity {

    private static final String TAG = "PAGTemplateActivity";

    private FrameLayout flRoot;

    public static void start(Context context) {
        Intent starter = new Intent(context, PAGTemplateActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pag_template);
        flRoot = findViewById(R.id.fl_root);
        flRoot.post(this::initTextureView);
    }


    private void initTextureView() {

        TAVTextureView textureView = new TAVTextureView(this);
        textureView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        // 1.加载PAG文件
        TAVPAGEffect effect = Utils.makePAGEffect("决战冬奥.pag", displayMetrics.widthPixels,
                displayMetrics.heightPixels);
        effect.setDuration(effect.fileDuration() - 1);

        // 2.获取所有PAG可替换图片的信息
        TAVPAGEditableInfo[] editableInfos = effect.getEditableInfo(TAVPAGEditableInfo.TAVPAGEditableTypeImage);
        int editLayerCount = editableInfos.length;
        if (MainActivity.SELECT_DATA.size() < editLayerCount) {
            Toast.makeText(this, "素材数量不够，请选取" + editLayerCount + "段素材", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 3.构建一个容器，能够容纳PAG模板轨道和视频/图片轨道
        TAVComposition composition = TAVComposition
                .Make(displayMetrics.widthPixels, displayMetrics.heightPixels, 0, effect.duration());
        composition.setDuration(effect.duration());

        // 4.加载视频、图片资源
        int inputIndex = 0;
        for (int i = 0; i < editLayerCount; i++) {
            TAVPAGEditableInfo editableInfo = editableInfos[i];
            for (TAVPAGImageLayerInfo imageLayerInfo : editableInfo.layerInfo) {
                // 5.基于每个图层的VideoRanges信息，创建对应的视频轨道/图片轨道
                MediaItem imageInfoBean = MainActivity.SELECT_DATA.get(i);
                if (imageInfoBean.isVideo()) {
                    TAVMovieAsset asset = TAVMovieAsset.MakeFromPath(imageInfoBean.getPath());
                    TAVMovie movie = TAVMovie.MakeFrom(asset, imageLayerInfo.displayVideoRanges);
                    movie.setStartTime(imageLayerInfo.layerStartTime);
                    effect.addInput(movie);
                    // 把Movie替换到占位图
                    TAVPAGImageReplacement replacement = TAVPAGImageReplacement.MakeFromInputIndex(inputIndex++);
                    effect.replaceImage(editableInfo.editableIndex, replacement);
                }
                if (imageInfoBean.isImage()) {
                    // 把Image替换到占位图
                    TAVPAGImageReplacement replacement = TAVPAGImageReplacement.MakeFromPath(imageInfoBean.getPath());
                    effect.replaceImage(editableInfo.editableIndex, replacement);
                }
            }
        }

        composition.addClip(effect);
        textureView.setMedia(composition);
        Log.i(TAG, "initTextureView: composition = " + composition.toJson());
        flRoot.addView(textureView);
    }
}