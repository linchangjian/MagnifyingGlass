package com.meitu.mopiview.utils;

import android.graphics.Path;

import java.io.Serializable;
import java.util.ArrayList;

public class SerializablePath extends Path implements Serializable {

    private ArrayList<float[]> mPathPoints;
    private int mColor;
    private float mWidth;
    private int mAlpha;
    private boolean mIsEraser = false;

    private int mEraserColor;
    private float mEraserWidth;
    private int mEraserAlpha;

    public int getmEraserColor() {
        return mEraserColor;
    }

    public void setmEraserColor(int mEraserColor) {
        this.mEraserColor = mEraserColor;
    }

    public float getmEraserWidth() {
        return mEraserWidth;
    }

    public void setmEraserWidth(float mEraserWidth) {
        this.mEraserWidth = mEraserWidth;
    }

    public int getmEraserAlpha() {
        return mEraserAlpha;
    }

    public void setmEraserAlpha(int mEraserAlpha) {
        this.mEraserAlpha = mEraserAlpha;
    }

    public SerializablePath() {
        super();
        mPathPoints = new ArrayList<>();
    }

    public SerializablePath(SerializablePath p) {
        super(p);
        mPathPoints = p.mPathPoints;
    }

    public void addPathPoints(float[] points) {
        this.mPathPoints.add(points);
    }

    public void saveMoveTo(float x, float y) {
        super.moveTo(x, y);

        addPathPoints(new float[]{x, y});
    }

    public void saveLineTo(float x, float y) {
        super.lineTo(x, y);
        addPathPoints(new float[]{x, y});
    }

    public void saveReset() {
        super.reset();
        mPathPoints.clear();
    }

    public void savePoint() {
        if (mPathPoints.size() > 0) {
            float[] points = mPathPoints.get(0);
            saveLineTo(points[0] + 1, points[1] + 1);
        }
    }

    public void loadPathPointsAsQuadTo() {
        float[] initPoints = mPathPoints.get(0);
        this.moveTo(initPoints[0], initPoints[1]);
        for (int j = 1; j < mPathPoints.size(); j++) {
            float[] pointSet = mPathPoints.get(j);
            this.lineTo(pointSet[0], pointSet[1]);
        }
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        this.mColor = color;
    }

    public float getWidth() {
        return mWidth;
    }

    public void setWidth(float width) {
        this.mWidth = width;
    }

    public int getmAlpha() {
        return mAlpha;
    }

    public void setmAlpha(int mAlpha) {
        this.mAlpha = mAlpha;
    }

    public boolean ismIsEraser() {
        return mIsEraser;
    }

    public void setmIsEraser(boolean mIsEraser) {
        this.mIsEraser = mIsEraser;
    }
}
