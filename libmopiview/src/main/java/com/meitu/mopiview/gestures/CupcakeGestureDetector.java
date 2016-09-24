/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.meitu.mopiview.gestures;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

@SuppressWarnings("deprecation")
public class CupcakeGestureDetector implements GestureDetector {

    protected OnGestureListener mListener;
    private static final String LOG_TAG = "CupcakeGestureDetector";
    float mLastTouchX;
    float mLastTouchY;
    final float mTouchSlop;
    private boolean mIsDragging = true;
    private TOUCH_MODE mTouchMode = TOUCH_MODE.NONE;

    enum TOUCH_MODE {
        NONE, MOVE
    }

    @Override
    public void setOnGestureListener(OnGestureListener listener) {
        this.mListener = listener;
    }

    public CupcakeGestureDetector(Context context) {
        final ViewConfiguration configuration = ViewConfiguration
                .get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
    }


    float getActiveX(MotionEvent ev) {
        return ev.getX();
    }

    float getActiveY(MotionEvent ev) {
        return ev.getY();
    }

    @Override
    public boolean isScaling() {
        return false;
    }

    @Override
    public boolean isDragging() {
        return mIsDragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {


        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mLastTouchX = getActiveX(ev);
                mLastTouchY = getActiveY(ev);
                mIsDragging = false;

                break;
            }
            case MotionEvent.ACTION_POINTER_2_DOWN:
                mTouchMode = TOUCH_MODE.MOVE;
                break;

            case MotionEvent.ACTION_MOVE: {
                if (mTouchMode == TOUCH_MODE.MOVE) {
                    final float x = getActiveX(ev);
                    final float y = getActiveY(ev);


                    final float dx = x - mLastTouchX, dy = y - mLastTouchY;

                    if (!mIsDragging) {
                        // Use Pythagoras to see if drag length is larger than
                        // touch slop
                        mIsDragging = Math.sqrt((dx * dx) + (dy * dy)) >= mTouchSlop;
                    }

                    if (mIsDragging) {
                        mListener.onDrag(dx, dy);
                        mLastTouchX = x;
                        mLastTouchY = y;
                    }
                }

                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mTouchMode = TOUCH_MODE.NONE;
                break;
            }

            case MotionEvent.ACTION_UP: {
                mTouchMode = TOUCH_MODE.NONE;
                break;
            }
        }

        return true;
    }
}
