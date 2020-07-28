package com.luck.pictureselector;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.xhdz.customcamera.activity.XhCustomSurfaceViewCameraActivity;


public class SimpleActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn_activity, btn_fragment, btn_custom_camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other);
        btn_activity = findViewById(R.id.btn_activity);
        btn_fragment = findViewById(R.id.btn_fragment);
        btn_custom_camera = findViewById(R.id.btn_custom_camera);
        btn_activity.setOnClickListener(this);
        btn_fragment.setOnClickListener(this);
        btn_custom_camera.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.btn_activity:
                intent = new Intent(SimpleActivity.this, MainActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_fragment:
                intent = new Intent(SimpleActivity.this, PhotoFragmentActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_custom_camera:
                // 使用camera2
//                intent = new Intent(SimpleActivity.this, XhCustomTextureViewCameraActivity.class);

                // 使用camera1
                intent = new Intent(SimpleActivity.this, XhCustomSurfaceViewCameraActivity.class);

                startActivity(intent);
                break;
            default:
                break;
        }
    }
}
