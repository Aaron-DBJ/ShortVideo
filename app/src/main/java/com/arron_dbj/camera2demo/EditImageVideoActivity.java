package com.arron_dbj.camera2demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

public class EditImageVideoActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageView mainImage, unSaveImage, saveImage;
    private Bitmap bitmap;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_handle);
        initView();
        Bundle bundle = getIntent().getExtras();
        byte[] bytes = bundle.getByteArray("ImageBytes");
        loadImage(bytes);
    }

    private void initView(){
        mainImage = findViewById(R.id.unhandled_image);
        unSaveImage = findViewById(R.id.back_unsave);
        saveImage = findViewById(R.id.back_save);
        unSaveImage.setOnClickListener(this);
        saveImage.setOnClickListener(this);
    }

    private void loadImage(byte[] bytes){
        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        mainImage.setImageBitmap(bitmap);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.back_save:

                break;
            case R.id.back_unsave:
                finish();
                break;
        }
    }
}
