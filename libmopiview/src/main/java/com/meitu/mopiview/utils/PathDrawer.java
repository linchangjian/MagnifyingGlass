package com.meitu.mopiview.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

public class PathDrawer {

  private Paint mGesturePaint;
  private boolean mIsShowHistory = true;
  public PathDrawer() {
    initGesturePaint();
  }

  public void onDraw(Canvas canvas, SerializablePath currentDrawingPath, List<SerializablePath> paths) {
    if (!mIsShowHistory) {
      drawGestures(canvas, paths);
    }

    if (currentDrawingPath != null) {
      drawGesture(canvas, currentDrawingPath);
    }
  }

  public void isShowHistoryPaths(boolean isShow){
    this.mIsShowHistory = isShow;
  }

  public Paint getmGesturePaint() {
    return mGesturePaint;
  }

  public void drawGestures(Canvas canvas, List<SerializablePath> paths) {
    for (SerializablePath path : paths) {
      drawGesture(canvas, path);
    }
  }

  public Bitmap obtainBitmap(Bitmap createdBitmap, List<SerializablePath> paths) {
    Canvas composeCanvas = new Canvas(createdBitmap);
    drawGestures(composeCanvas, paths);
    return createdBitmap;
  }
  public Bitmap obtainBitmap(Bitmap createdBitmap, SerializablePath path) {
    Canvas composeCanvas = new Canvas(createdBitmap);
    drawGesture(composeCanvas, path);
    return createdBitmap;
  }

  private void drawGesture(Canvas canvas, SerializablePath path) {

    if(path.ismIsEraser()){
      mGesturePaint.setStrokeWidth(path.getmEraserWidth());
      mGesturePaint.setColor(path.getmEraserColor());
      mGesturePaint.setAlpha(path.getmEraserAlpha());
    }else{
      mGesturePaint.setStrokeWidth(path.getWidth());
      mGesturePaint.setColor(path.getColor());
      mGesturePaint.setAlpha(path.getmAlpha());
    }

    canvas.drawPath(path, mGesturePaint);

  }

  private void initGesturePaint() {
    mGesturePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    mGesturePaint.setStyle(Paint.Style.STROKE);
    mGesturePaint.setStrokeJoin(Paint.Join.ROUND);
    mGesturePaint.setStrokeCap(Paint.Cap.ROUND);
    mGesturePaint.setAntiAlias(true);
  }
}
