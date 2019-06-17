package com.arron_dbj.camera2demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.arron_dbj.camera2demo.utils.FileHelper;
import com.arron_dbj.camera2demo.utils.StringUtil;
import com.dueeeke.videocontroller.StandardVideoController;
import com.dueeeke.videoplayer.player.IjkVideoView;
import com.vincent.filepicker.Constant;
import com.vincent.filepicker.activity.AudioPickActivity;
import com.vincent.filepicker.filter.entity.AudioFile;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import VideoHandle.EpEditor;
import VideoHandle.OnEditorListener;
import io.netopen.hotbitmapgg.library.view.RingProgressBar;

import static com.vincent.filepicker.activity.AudioPickActivity.IS_NEED_RECORDER;
import static com.vincent.filepicker.activity.BaseActivity.IS_NEED_FOLDER_LIST;

public class EditVideoActivity extends AppCompatActivity implements View.OnClickListener {
    private IjkVideoView playerView;
    private TextView chooseMusic;
    private static final int CHOOSE_MUSIC_OK = 23;
    private File audioFile;
    private String outputPath;
    private File videoFile;
    private FileHelper fileHelper = FileHelper.getInstance(this);
    private ExecutorService executorService;
    // 对于播放界面的控制设置
    private StandardVideoController controller;
    private RingProgressBar progressBar;
    private int currentProgress = 0;

    private static final int UPDATE_PROGRESS = 0x1;
    private static final int PLAY_COMPOUND_VIDEO= 0x2;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_PROGRESS:
                    if (currentProgress == 10000){
                        progressBar.setVisibility(View.GONE);
                    }else {
                        progressBar.setProgress(currentProgress);
                        Log.d("Camera2Demo", progressBar.getProgress() + "");
                    }
                    break;
                case PLAY_COMPOUND_VIDEO:
                    playerView.release();
                    playVideo(msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_video);
        initView();
        executorService = Executors.newCachedThreadPool();
        videoFile = new File(String.valueOf(getIntent().getData()));
        File dir = fileHelper.getDiskCacheDir(this, "Video");
        if (!dir.exists())
            dir.mkdirs();
        outputPath = new File(dir, StringUtil.getCurrentTime_yyyyMMddHHmmss() + ".mp4").getPath();
        playVideo(videoFile.getAbsolutePath());
    }

    private void initView() {
        chooseMusic = findViewById(R.id.choose_music);
        playerView = findViewById(R.id.video_player);
        chooseMusic.setOnClickListener(this);
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setMax(10000);
    }

    private void playVideo(String url) {
        playerView.setUrl(url);
        controller = new StandardVideoController(this);
        controller.setTitle("测试视频");
        playerView.setVideoController(controller);
        playerView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playerView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerView.release();
    }


    @Override
    public void onBackPressed() {
        if (!playerView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.choose_music:
                getAudioFiles();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.REQUEST_CODE_PICK_AUDIO) {
            assert data != null;
            ArrayList<AudioFile> list = data.getParcelableArrayListExtra(Constant.RESULT_PICK_AUDIO);
//            ArrayList<EssFile> audioFiles = data.getParcelableArrayListExtra(Const.EXTRA_RESULT_SELECTION);
            audioFile = new File(list.get(0).getPath());
            progressBar.setVisibility(View.VISIBLE);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    //参数分别是视频路径，音频路径，输出路径,原始视频音量(1为100%,0.7为70%,以此类推),添加音频音量
                    EpEditor.music(videoFile.getAbsolutePath(), audioFile.getAbsolutePath(), outputPath,
                            1, 0.8f, new OnEditorListener() {
                                @Override
                                public void onSuccess() {
                                    Message message = Message.obtain();
                                    message.what = PLAY_COMPOUND_VIDEO;
                                    message.obj = outputPath;
                                    handler.sendMessage(message);
                                    Looper.prepare();
                                    Toast.makeText(EditVideoActivity.this, "添加背景音乐成功", Toast.LENGTH_SHORT).show();
                                    Looper.loop();
                                }

                                @Override
                                public void onFailure() {

                                }

                                @Override
                                public void onProgress(float progress) {
                                    currentProgress = (int) (progress * 10000);
                                    Message message = Message.obtain();
                                    message.what = UPDATE_PROGRESS;
                                    message.arg1 = currentProgress;
                                    handler.sendMessage(message);
                                }
                            });
                }
            });

        }
    }

    public void getAudioFiles() {
        Intent audioIntent = new Intent(this, AudioPickActivity.class);
        audioIntent.putExtra(IS_NEED_RECORDER, true);
        audioIntent.putExtra(Constant.MAX_NUMBER, 9);
        audioIntent.putExtra(IS_NEED_FOLDER_LIST, true);
        startActivityForResult(audioIntent, Constant.REQUEST_CODE_PICK_AUDIO);
    }
}
