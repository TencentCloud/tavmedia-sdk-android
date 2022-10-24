package com.tencent.tavmedia.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.tavmedia.TAVAudio;
import com.tencent.tavmedia.TAVAudioAsset;
import com.tencent.tavmedia.TAVComposition;
import com.tencent.tavmedia.TAVMovie;
import com.tencent.tavmedia.TAVMovieAsset;
import com.tencent.tavmedia.TAVPAGEffect;
import com.tencent.tavmedia.TAVTimeStretchMode;

public class TemplateActivity extends AppCompatActivity {

    private static final String TAG = "TemplateActivity";

    public static void start(Context context) {
        Intent starter = new Intent(context, TemplateActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template);
        initTextureView(720, 1280);
    }

    public void switchRenderSize(View view) {
        initTextureView(1000, 1000);
    }

    private void initTextureView(float width, float height) {

        TAVTextureView textureView = new TAVTextureView(this);
        textureView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        TAVComposition composition = MakeComposition(width, height);
        Utils.fitToTarget(composition, displayMetrics.widthPixels, displayMetrics.heightPixels);
        textureView.setMedia(composition);

        FrameLayout root = findViewById(R.id.fl_root);
        root.removeAllViews();
        root.addView(textureView);
    }

    /**
     * 请关注，关键逻辑，构建TAVMedia结构
     */
    private TAVComposition MakeComposition(float width, float height) {
        // 准备资源，用四个视频素材构建四个movie对象
        TAVMovieAsset asset1 = TAVMovieAsset.MakeFromPath(Utils.OUT_SAVE_DIR + "1.mp4");
        int movieDuration = 3_000_000;
        TAVMovie movie1 = makeMovie(asset1, movieDuration, width, height);
        movie1.setDuration(movieDuration);

        TAVMovieAsset asset2 = TAVMovieAsset.MakeFromPath(Utils.OUT_SAVE_DIR + "2.mp4");
        TAVMovie movie2 = makeMovie(asset2, movieDuration, width, height);
        movie2.setDuration(movieDuration);

        TAVMovieAsset asset3 = TAVMovieAsset.MakeFromPath(Utils.OUT_SAVE_DIR + "3.mp4");
        TAVMovie movie3 = makeMovie(asset3, movieDuration, width, height);
        movie3.setDuration(movieDuration);

        TAVMovieAsset asset4 = TAVMovieAsset.MakeFromPath(Utils.OUT_SAVE_DIR + "4.mp4");
        TAVMovie movie4 = makeMovie(asset4, movieDuration, width, height);
        movie4.setDuration(movieDuration);

        // 用pag文件构建effect对象，指定时间长度，这里是三个做转场的effect
        TAVPAGEffect transition1 = Utils.makePAGEffect("ZC1.pag", width, height);
        transition1.setDuration(1_000_000);

        TAVPAGEffect transition2 = Utils.makePAGEffect("ZC2.pag", width, height);
        transition2.setDuration(1_250_000);

        TAVPAGEffect transition3 = Utils.makePAGEffect("ZC3.pag", width, height);
        transition3.setDuration(2_000_000);

        // 先对上述对象，在时间轴上做好布局
        movie1.setStartTime(0);
        movie2.setStartTime(movieDuration - transition1.duration());
        transition1.setStartTime(movie2.startTime());

        movie3.setStartTime(movieDuration * 2 - transition1.duration() - transition2.duration());
        transition2.setStartTime(movie3.startTime());

        movie4.setStartTime(
                movieDuration * 3 - transition1.duration() - transition2.duration() - transition3.duration());
        transition3.setStartTime(movie4.startTime());

        // 将movie加到effect的输入中
        transition1.addInput(movie1);
        transition1.addInput(movie2);
        transition1.replaceImage(0, 0);
        transition1.replaceImage(1, 1);
        transition2.addInput(transition1);
        transition2.addInput(movie3);
        transition2.replaceImage(0, 0);
        transition2.replaceImage(1, 1);
        transition3.addInput(transition2);
        transition3.addInput(movie4);
        transition3.replaceImage(0, 0);
        transition3.replaceImage(1, 1);

        // 到这里主轨道装配完成，我们把主轨道包到一个Composition中，再将这个Composition视为一个整体，对其添加氛围、片头片尾等

        // 计算总时长
        long totalDuration = movie4.startTime() + movie4.duration();
        TAVComposition composition = TAVComposition.Make((int) width, (int) height, 0, totalDuration);
        composition.addClip(transition1);
        composition.addClip(transition2);
        composition.addClip(transition3);
        composition.setDuration(totalDuration);

        TAVPAGEffect pt = Utils.makePAGEffect("PT.pag", width, height);
        pt.setDuration(3_000_000);
        if (pt.numImages() > 0) {
            pt.addInput(composition);
            pt.replaceImage(0, 0);
        }

        TAVPAGEffect fw = Utils.makePAGEffect("FW.pag", width, height);
        fw.setDuration(composition.duration());
        fw.setTimeStretchMode(TAVTimeStretchMode.TAVTimeStretchModeRepeat);
        if (fw.numImages() > 0) {
            fw.addInput(composition);
            fw.replaceImage(0, 0);
        }

        TAVPAGEffect pw = Utils.makePAGEffect("PW.pag", width, height);
        pw.setStartTime(composition.duration() - 2_000_000);
        pw.setDuration(2_000_000);
        if (pw.numImages() > 0) {
            pw.addInput(composition);
            pw.replaceImage(0, 0);
        }

        TAVComposition root = TAVComposition.Make(composition.width(), composition.height(), 0, composition.duration());
        root.setDuration(composition.duration());
        root.addClip(pt);
        root.addClip(fw);
        root.addClip(pw);

        // 加个bgm
        TAVAudioAsset audioAsset = TAVAudioAsset.MakeFromPath(Utils.OUT_SAVE_DIR + "kanong.mp3");
        TAVAudio audio = TAVAudio.MakeFrom(audioAsset, 1_000_000, composition.duration());
        audio.setDuration(composition.duration());
        root.addClip(audio);

        return root;
    }

    private TAVMovie makeMovie(TAVMovieAsset asset1, int movieDuration, float width, float height) {
        TAVMovie movie = TAVMovie.MakeFrom(asset1, 0, movieDuration);
        Utils.fitToTarget(movie, width, height);
        return movie;
    }

    public void toast(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_SHORT).show());
    }
}