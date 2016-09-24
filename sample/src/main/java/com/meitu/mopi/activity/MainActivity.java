package com.meitu.mopi.activity;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.meitu.mopi.R;
import com.meitu.mopiview.utils.PaintConfig;
import com.meitu.mopiview.widget.MopiImageView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private Button mBtnPaintWidthAdd;
    private Button mBtnPaintWidthMinus;
    private Button mBtnUndo;
    private Button mBtnPaintColor;
    private Button mBtnEraser;
    private MopiImageView mMopiImageView;
    private Switch mSHistory;
    private PaintConfig mConfig = new PaintConfig();
    private Switch mSEraser;
    private Button mBtnEraserWidthAdd;
    private Button mBtnEraserWidthMinus;
    private Button mBtnEraserColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMopiImageView = (MopiImageView) findViewById(R.id.miv_mopi);
        mBtnPaintWidthAdd = (Button) findViewById(R.id.btn_paint_width_add);
        mBtnPaintWidthMinus = (Button) findViewById(R.id.btn_paint_width_minus);
        mBtnPaintColor = (Button) findViewById(R.id.btn_paint_color);
        mBtnUndo = (Button) findViewById(R.id.btn_undo);
        mSHistory = (Switch) findViewById(R.id.s_history);
        mSEraser = (Switch) findViewById(R.id.s_eraser);
        mBtnEraserWidthAdd = (Button) findViewById(R.id.btn_eraser_width_add);
        mBtnEraserWidthMinus = (Button) findViewById(R.id.btn_eraser_width_minus);
        mBtnEraserColor = (Button) findViewById(R.id.btn_eraser_color);

        mConfig.setmStrokeColor(getResources().getColor(android.R.color.black));
        mConfig.setmStrokeWidth(20.0f);
        mConfig.setmAlpha(50);

        mConfig.setmEraserStrokeColor(getResources().getColor(android.R.color.holo_red_dark));
        mConfig.setmEraserStrokeWidth(30.0f);
        mConfig.setmEraserAlpha(100);
        mMopiImageView.isChooseEraser(true);

        mMopiImageView.setConfig(mConfig);
        mBtnPaintWidthAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConfig.setmStrokeWidth(mConfig.getmStrokeWidth()+2);
            }
        });

        mBtnPaintWidthMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConfig.setmStrokeWidth(mConfig.getmStrokeWidth()-2);
            }
        });

        mBtnPaintColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Random random = new Random();
                mConfig.setmStrokeColor(Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            }
        });

        mBtnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMopiImageView.undo();
            }
        });

        mSHistory.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mMopiImageView.isShowHistoryPaths(isChecked);
            }
        });

        mSEraser.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mMopiImageView.isChooseEraser(isChecked);
            }
        });

        mBtnEraserWidthAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConfig.setmEraserStrokeWidth(mConfig.getmEraserStrokeWidth()+2);
            }
        });

        mBtnEraserWidthMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConfig.setmEraserStrokeWidth(mConfig.getmEraserStrokeWidth()-2);
            }
        });

        mBtnEraserColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Random random = new Random();
                mConfig.setmEraserStrokeColor(Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            }
        });

    }
}
