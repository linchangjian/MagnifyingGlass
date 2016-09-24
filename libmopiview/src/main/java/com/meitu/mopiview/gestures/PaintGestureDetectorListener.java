package com.meitu.mopiview.gestures;


import com.meitu.mopiview.utils.SerializablePath;

public interface PaintGestureDetectorListener {
    /**
     * 历史绘制的路径
     * @param serializablePath
     */
    void onGestureCreated(SerializablePath serializablePath);

    /**
     * 当前绘制的路径
     * @param currentDrawingPath
     */
    void onCurrentGestureChanged(SerializablePath currentDrawingPath);
}
