package com.meitu.mopiview.gestures;

import android.graphics.Color;
import android.graphics.RectF;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;

import com.meitu.mopiview.utils.PaintConfig;
import com.meitu.mopiview.utils.SerializablePath;

/**
 * 画笔手势相关处理
 */
public class PaintGestureDetector {

    private SerializablePath mCurrentDrawingPath = new SerializablePath();
    private PaintGestureDetectorListener mDelegate;
    private PaintConfig mConfig;
    private boolean mDownAndUpGesture = false;
    private float mScaleFactor = 1.0f;
    private RectF mViewRect = new RectF();
    private RectF mCanvasRect = new RectF();
    private boolean mIsEraser = false;

    public PaintGestureDetector(PaintGestureDetectorListener delegate) {
        this.mDelegate = delegate;
    }

    public void onTouchEvent(MotionEvent event) {
        float touchX = (MotionEventCompat.getX(event, 0) + mViewRect.left) / mScaleFactor;
        float touchY = (MotionEventCompat.getY(event, 0) + mViewRect.top) / mScaleFactor;

        //Log.d("Drawer", "T[" + touchX + "," + touchY + "] V[" + mViewRect.toShortString() + "] S[" + mScaleFactor + "]");
        switch (MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_DOWN:
                actionDown(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                actionMove(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                actionUp();
                break;
            case MotionEventCompat.ACTION_POINTER_DOWN:
                actionPointerDown();
                break;
        }
    }

    private void actionDown(float touchX, float touchY) {
        if (insideCanvas(touchX, touchY)) {
            mDownAndUpGesture = true;
            if(mCurrentDrawingPath == null){
                mCurrentDrawingPath = new SerializablePath();

            }
            if (mConfig != null) {

                if(mIsEraser){
                    mCurrentDrawingPath.setmIsEraser(true);
                    mCurrentDrawingPath.setmEraserColor(mConfig.getmEraserStrokeColor());
                    mCurrentDrawingPath.setmEraserWidth(mConfig.getmEraserStrokeWidth());
                    mCurrentDrawingPath.setmEraserAlpha(mConfig.getmEraserAlpha());

                }else {
                    mCurrentDrawingPath.setmIsEraser(false);
                    mCurrentDrawingPath.setColor(mConfig.getmStrokeColor());
                    mCurrentDrawingPath.setWidth(mConfig.getmStrokeWidth());
                    mCurrentDrawingPath.setmAlpha(mConfig.getmAlpha());

                }
            }
            mCurrentDrawingPath.saveMoveTo(touchX, touchY);
            mDelegate.onCurrentGestureChanged(mCurrentDrawingPath);
        }
    }

    private void actionMove(float touchX, float touchY) {
        if (insideCanvas(touchX, touchY)) {
            mDownAndUpGesture = false;
            if (mCurrentDrawingPath != null) {
                mCurrentDrawingPath.saveLineTo(touchX, touchY);
            }
        } else {
            actionUp();
        }
    }

    private void actionUp() {
        if (mCurrentDrawingPath != null) {
            if (mDownAndUpGesture) {
                mCurrentDrawingPath.savePoint();
                mDownAndUpGesture = false;
            }
            mDelegate.onGestureCreated(mCurrentDrawingPath);
            mCurrentDrawingPath = null;
            mDelegate.onCurrentGestureChanged(null);
        }
    }

    private void actionPointerDown() {
        mCurrentDrawingPath = null;
        mDelegate.onCurrentGestureChanged(null);
    }

    public boolean ismIsEraser() {
        return mIsEraser;
    }

    public void setmIsEraser(boolean mIsEraser) {
        this.mIsEraser = mIsEraser;
    }

    private boolean insideCanvas(float touchX, float touchY) {
       // return mCanvasRect.contains(touchX, touchY);
        return true;
    }

    public void setmConfig(PaintConfig mConfig) {
        this.mConfig = mConfig;
    }

    public void onScaleChange(float scaleFactor) {
        this.mScaleFactor = scaleFactor;
    }

    public void onViewPortChange(RectF viewRect) {
        this.mViewRect = viewRect;
    }

    public void onCanvasChanged(RectF canvasRect) {
        this.mCanvasRect.right = canvasRect.right / mScaleFactor;
        this.mCanvasRect.bottom = canvasRect.bottom / mScaleFactor;
    }
}
