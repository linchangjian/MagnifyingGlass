package com.meitu.mopiview.utils;

import java.io.Serializable;

public class PaintConfig implements Serializable {

    private float mStrokeWidth;
    private int mStrokeColor;
    private int mAlpha;

    private float mEraserStrokeWidth;
    private int mEraserStrokeColor;
    private int mEraserAlpha;

    private int mCanvasWidth;
    private int mCanvasHeight;
    private float mMinZoom;
    private float mMaxZoom;
    private boolean mShowCanvasBounds;



    public float getmMaxZoom() {
        return mMaxZoom;
    }

    public void setmMaxZoom(float mMaxZoom) {
        this.mMaxZoom = mMaxZoom;
    }

    public float getmMinZoom() {
        return mMinZoom;
    }

    public void setmMinZoom(float mMinZoom) {
        this.mMinZoom = mMinZoom;
    }

    public int getmCanvasHeight() {
        return mCanvasHeight;
    }

    public void setmCanvasHeight(int mCanvasHeight) {
        this.mCanvasHeight = mCanvasHeight;
    }

    public int getmCanvasWidth() {
        return mCanvasWidth;
    }

    public void setmCanvasWidth(int mCanvasWidth) {
        this.mCanvasWidth = mCanvasWidth;
    }

    public float getmStrokeWidth() {
        return mStrokeWidth;
    }

    public void setmStrokeWidth(float mStrokeWidth) {
        this.mStrokeWidth = mStrokeWidth;
    }

    public int getmStrokeColor() {
        return mStrokeColor;
    }

    public void setmStrokeColor(int mStrokeColor) {
        this.mStrokeColor = mStrokeColor;
    }

    public boolean ismShowCanvasBounds() {
        return mShowCanvasBounds;
    }

    public void setmShowCanvasBounds(boolean mShowCanvasBounds) {
        this.mShowCanvasBounds = mShowCanvasBounds;
    }

    public int getmAlpha() {
        return mAlpha;
    }

    public void setmAlpha(int mAlpha) {
        this.mAlpha = mAlpha;
    }

    public float getmEraserStrokeWidth() {
        return mEraserStrokeWidth;
    }

    public void setmEraserStrokeWidth(float mEraserStrokeWidth) {
        this.mEraserStrokeWidth = mEraserStrokeWidth;
    }

    public int getmEraserStrokeColor() {
        return mEraserStrokeColor;
    }

    public void setmEraserStrokeColor(int mEraserStrokeColor) {
        this.mEraserStrokeColor = mEraserStrokeColor;
    }

    public int getmEraserAlpha() {
        return mEraserAlpha;
    }

    public void setmEraserAlpha(int mEraserAlpha) {
        this.mEraserAlpha = mEraserAlpha;
    }
}
