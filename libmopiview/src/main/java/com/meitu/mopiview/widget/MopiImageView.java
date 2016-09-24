package com.meitu.mopiview.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.meitu.mopiview.R;
import com.meitu.mopiview.gestures.OnGestureListener;
import com.meitu.mopiview.gestures.PaintGestureDetector;
import com.meitu.mopiview.gestures.PaintGestureDetectorListener;
import com.meitu.mopiview.gestures.VersionedGestureDetector;
import com.meitu.mopiview.log.LogManager;
import com.meitu.mopiview.utils.Compat;
import com.meitu.mopiview.utils.FileUtils;
import com.meitu.mopiview.utils.PaintConfig;
import com.meitu.mopiview.utils.PathDrawer;
import com.meitu.mopiview.utils.SerializablePath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;

/**
 * 功能描述：
 * 1，双指捏合能进行缩放
 * 2，双指拖动能移动放大后的图片
 * 3，图片在经过缩放和平移之后要求能够自适应视图恢复到合适的位置。
 * 4，用户可以通过控件选择画笔或橡皮进行涂抹和擦除操作，可以设置画笔和橡皮的宽度、颜色和透明度。
 * 4，单指使用画笔的时候会有放大镜显示
 * 5，画笔的路径保存在本地，所有的路径就是Mask层
 * 6，画笔可以进行撤销
 * <p/>
 * Created by lcj on 16/5/24.
 */
public class MopiImageView extends ImageView implements IPhotoView,
        OnGestureListener,
        ViewTreeObserver.OnGlobalLayoutListener,
        View.OnTouchListener,
        PaintGestureDetectorListener {

    private static final String LOG_TAG = "MopiImageView";
    private String mPaintLocalPath;

    private boolean mZoomEnabled;
    private float mMinScale = DEFAULT_MIN_SCALE;
    private float mMidScale = DEFAULT_MID_SCALE;
    private float mMaxScale = DEFAULT_MAX_SCALE;

    // These are set so we don't keep allocating them on the heap
    private final Matrix mBaseMatrix = new Matrix();
    private final Matrix mDrawMatrix = new Matrix();
    private final Matrix mSuppMatrix = new Matrix();
    private final RectF mDisplayRect = new RectF();
    private final float[] mMatrixValues = new float[9];

    private ScaleType mScaleType = ScaleType.FIT_CENTER;

    int ZOOM_DURATION = DEFAULT_ZOOM_DURATION;

    private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();
    private com.meitu.mopiview.gestures.GestureDetector mScaleDragDetector;

    private int mIvTop, mIvRight, mIvBottom, mIvLeft;

    // let debug flag be dynamic, but still Proguard can be used to remove from
    // release builds
    private static final boolean DEBUG = Log.isLoggable(LOG_TAG, Log.DEBUG);

    //-----------------------
    private Paint mOutterPaint;
    private Path mPath;
    private Canvas mCanvas;
    private Bitmap mBitmap;
    private int mLastX;
    private int mLastY;

    private boolean mIsDraw;

    // 放大镜内容-------------------
    private PopupWindow mPopup;
    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;
    private Magnifier mMagnifier; //放大镜视图
    private Bitmap mResBitmap;
    // private Point mDstPoint;
    private volatile Bitmap mMagnifierBitmap;//生成的位图


    //mCacheView 在屏幕上的位置，用来确定放大镜popwindow的位置
    private int mThisOnScreenX;
    private int mThisOnScreenY;

    //画布画笔部分------------------
    PaintGestureDetector mPaintGestureDetector;
    private ArrayList<SerializablePath> mPaths = new ArrayList<>();
    private SerializablePath mCurrentDrawingPath;
    private PathDrawer mPathDrawer;

    //-----------------------------
    // public Activity mActivity;

    private Context mContext;
    private int mCanvasX;
    private int mCanvasY;
    private BitmapDrawable resDrawable;

    public MopiImageView(Context context) {
        this(context, null);
    }

    public MopiImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MopiImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;

        init();
        initPaint();
    }

    private void init() {
        setOnTouchListener(this);
        ViewTreeObserver observer = getViewTreeObserver();
        if (null != observer)
            observer.addOnGlobalLayoutListener(this);
        // Create Gesture Detectors...
        mScaleDragDetector = VersionedGestureDetector.newInstance(getContext(), this);
        mPaintGestureDetector = new PaintGestureDetector(this);
        mPathDrawer = new PathDrawer();
        mPaintLocalPath = FileUtils.getExternalCacheDir(mContext) + "PathsObject.txt";

        setZoomable(true);

        initMagnifier();

    }

    private void initOnScreenPosition() {
        int[] location = new int[2];
        getLocationOnScreen(location);
        mThisOnScreenX = location[0];
        mThisOnScreenY = location[1];
    }

    public void setZoomable(boolean zoomable) {
        mZoomEnabled = zoomable;
        update();
    }

    public void update() {

        if (mZoomEnabled) {
            // Make sure we using MATRIX Scale Type
            setImageViewScaleTypeMatrix();

            // Update the base matrix using the current drawable
            updateBaseMatrix(getDrawable());
        } else {
            // Reset the Matrix...
            resetMatrix();
        }
    }

    /**
     * Set's the ImageView's ScaleType to Matrix.
     */
    private void setImageViewScaleTypeMatrix() {
        /**
         * PhotoView sets it's own ScaleType to Matrix, then diverts all calls
         * setScaleType to this.setScaleType automatically.
         */
        if (!ScaleType.MATRIX.equals(getScaleType())) {
            setScaleType(ScaleType.MATRIX);
        }
    }

    /**
     * @return true if the ImageView exists, and it's Drawable exists
     */
    private static boolean hasDrawable(ImageView imageView) {
        return null != imageView && null != imageView.getDrawable();
    }

    public float getScale() {
        return (float) Math.sqrt((float) Math.pow(getValue(mSuppMatrix, Matrix.MSCALE_X), 2) + (float) Math.pow(getValue(mSuppMatrix, Matrix.MSKEW_Y), 2));
    }

    /**
     * Helper method that 'unpacks' a Matrix and returns the required value
     *
     * @param matrix     - Matrix to unpack
     * @param whichValue - Which value from Matrix.M* to return
     * @return float - returned value
     */
    private float getValue(Matrix matrix, int whichValue) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[whichValue];
    }

    public RectF getDisplayRect() {
        checkMatrixBounds();
        return getDisplayRect(getDrawMatrix());
    }

    @Deprecated
    /**
     * Method should be private
     * Use {@link #getDisplayMatrix(Matrix)}
     */
    public Matrix getDrawMatrix() {
        mDrawMatrix.set(mBaseMatrix);
        mDrawMatrix.postConcat(mSuppMatrix);
        return mDrawMatrix;
    }

    /**
     * Helper method that maps the supplied Matrix to the current Drawable
     *
     * @param matrix - Matrix to map Drawable against
     * @return RectF - Displayed Rectangle
     */
    private RectF getDisplayRect(Matrix matrix) {
        // ImageView imageView = getImageView();

        //if (null != imageView) {
        Drawable d = this.getDrawable();
        if (null != d) {
            mDisplayRect.set(0, 0, d.getIntrinsicWidth(),
                    d.getIntrinsicHeight());
            matrix.mapRect(mDisplayRect);
            return mDisplayRect;
        }
        //  }
        return null;
    }

    private boolean checkMatrixBounds() {
//        final ImageView imageView = getImageView();
//        if (null == imageView) {
//            return false;
//        }

        final RectF rect = getDisplayRect(getDrawMatrix());
        if (null == rect) {
            return false;
        }

        final float height = rect.height(), width = rect.width();
        float deltaX = 0, deltaY = 0;

        final int viewHeight = getImageViewHeight(this);
        if (height <= viewHeight) {
            switch (mScaleType) {
                case FIT_START:
                    deltaY = -rect.top;
                    break;
                case FIT_END:
                    deltaY = viewHeight - height - rect.top;
                    break;
                default:
                    deltaY = (viewHeight - height) / 2 - rect.top;
                    break;
            }
        } else if (rect.top > 0) {
            deltaY = -rect.top;
        } else if (rect.bottom < viewHeight) {
            deltaY = viewHeight - rect.bottom;
        }

        final int viewWidth = getImageViewWidth(this);
        if (width <= viewWidth) {
            switch (mScaleType) {
                case FIT_START:
                    deltaX = -rect.left;
                    break;
                case FIT_END:
                    deltaX = viewWidth - width - rect.left;
                    break;
                default:
                    deltaX = (viewWidth - width) / 2 - rect.left;
                    break;
            }
            // mScrollEdge = EDGE_BOTH;
        } else if (rect.left > 0) {
            //  mScrollEdge = EDGE_LEFT;
            deltaX = -rect.left;
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right;
            //  mScrollEdge = EDGE_RIGHT;
        } else {
            //  mScrollEdge = EDGE_NONE;
        }

        // Finally actually translate the matrix
        mSuppMatrix.postTranslate(deltaX, deltaY);
        return true;
    }

    private int getImageViewHeight(ImageView imageView) {
        if (null == imageView)
            return 0;
        return imageView.getHeight() - imageView.getPaddingTop() - imageView.getPaddingBottom();
    }

    private int getImageViewWidth(ImageView imageView) {
        if (null == imageView)
            return 0;
        return imageView.getWidth() - imageView.getPaddingLeft() - imageView.getPaddingRight();
    }

    @Override
    public void onGlobalLayout() {

        if (mZoomEnabled) {
            final int top = getTop();
            final int right = getRight();
            final int bottom = getBottom();
            final int left = getLeft();

            /**
             * We need to check whether the ImageView's bounds have changed.
             * This would be easier if we targeted API 11+ as we could just use
             * View.OnLayoutChangeListener. Instead we have to replicate the
             * work, keeping track of the ImageView's bounds and then checking
             * if the values change.
             */
            if (top != mIvTop || bottom != mIvBottom || left != mIvLeft
                    || right != mIvRight) {
                // Update our base matrix, as the bounds have changed
                updateBaseMatrix(getDrawable());

                // Update values as something has changed
                mIvTop = top;
                mIvRight = right;
                mIvBottom = bottom;
                mIvLeft = left;
            }
        } else {
            updateBaseMatrix(getDrawable());
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean handled = false;
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (mZoomEnabled && hasDrawable(this)) {
            ViewParent parent = getParent();
            switch (event.getAction()) {
                case ACTION_DOWN:
                    mIsDraw = true;
                    // First, disable the Parent from intercepting the touch
                    // event
                    if (null != parent) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    } else {
                        LogManager.getLogger().i(LOG_TAG, "onTouch getParent() returned null");
                    }

                    // If we're flinging, and the user presses down, cancel
                    // fling
                    mLastX = x;
                    mLastY = y;
                    mPath.moveTo(mLastX, mLastY);
                    //setDrawingCacheEnabled(true);

                    if (mMagnifierBitmap == null) {
                        if (resDrawable == null) {
                            resDrawable = (BitmapDrawable) getDrawable();

                        }
                        this.mMagnifierBitmap = resDrawable.getBitmap();

                        mMagnifierBitmap = Bitmap.createBitmap(mMagnifierBitmap, 0, 0, mMagnifierBitmap.getWidth(),
                                mMagnifierBitmap.getHeight(), mBaseMatrix, true);
                        //mCurrentDrawMatrix.set(mDrawMatrix);

                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mIsDraw) {
                        int dx = Math.abs(x - mLastX);
                        int dy = Math.abs(y - mLastY);

                        //判断移动距离
                        if (dx > 3 || dy > 3) {
                            mPath.lineTo(x, y);
                        }

                        mLastX = x;
                        mLastY = y;
                        //放大镜
//                        mResBitmap = getBitmap((int) event.getRawX() - WIDTH / 2, (int) event.getRawY() - HEIGHT / 2, WIDTH, HEIGHT);
//                        calculate((int) event.getRawX(), (int) event.getRawY(), MotionEvent.ACTION_DOWN);

                    }

                    break;
                case MotionEvent.ACTION_POINTER_2_DOWN:
                    mIsDraw = false;
                    break;
                case ACTION_CANCEL:
                case ACTION_UP:
                    mIsDraw = false;
                    //setDrawingCacheEnabled(false);

                    // If the user has zoomed less than min scale, zoom back
                    // to min scale
                    if (getScale() < mMinScale) {
                        RectF rect = getDisplayRect();
                        if (null != rect) {
                            this.post(new AnimatedZoomRunnable(getScale(), mMinScale,
                                    rect.centerX(), rect.centerY()));
                            handled = true;
                        }
                    }
//                    //dis掉放大镜
//                    removeCallbacks(mShowZoom);
//                    //drawLayout();
//                    mPopup.dismiss();
                    break;
            }
            // Try the Scale/Drag detector
            if (null != mScaleDragDetector) {
                handled = mScaleDragDetector.onTouchEvent(event);
            }
            if (null != mPaintGestureDetector) {
                mPaintGestureDetector.onTouchEvent(event);
            }

        }

        invalidate();
        return handled;
    }

    public void isChooseEraser(boolean isChecked) {
        mPaintGestureDetector.setmIsEraser(isChecked);
        LogManager.getLogger().d(LOG_TAG, isChecked + "");

    }

    private class AnimatedZoomRunnable implements Runnable {

        private final float mFocalX, mFocalY;
        private final long mStartTime;
        private final float mZoomStart, mZoomEnd;

        public AnimatedZoomRunnable(final float currentZoom, final float targetZoom,
                                    final float focalX, final float focalY) {
            mFocalX = focalX;
            mFocalY = focalY;
            mStartTime = System.currentTimeMillis();
            mZoomStart = currentZoom;
            mZoomEnd = targetZoom;
        }

        @Override
        public void run() {
            ImageView imageView = MopiImageView.this;
            if (imageView == null) {
                return;
            }

            float t = interpolate();
            float scale = mZoomStart + t * (mZoomEnd - mZoomStart);
            float deltaScale = scale / getScale();

            onScale(deltaScale, mFocalX, mFocalY);

            // We haven't hit our target scale yet, so post ourselves again
            if (t < 1f) {
                Compat.postOnAnimation(imageView, this);
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / ZOOM_DURATION;
            t = Math.min(1f, t);
            t = mInterpolator.getInterpolation(t);
            return t;
        }
    }

    @Override
    public void onDrag(float dx, float dy) {
        if (!mScaleDragDetector.isDragging()) {
            return; // Do not drag if we are already scaling
        }
        if (DEBUG) {
            LogManager.getLogger().d(LOG_TAG, String.format("onDrag: dx: %.2f. dy: %.2f", dx, dy));
        }

        mSuppMatrix.postTranslate(dx, dy);
        checkAndDisplayMatrix();

    }

    @Override
    public void onScale(float scaleFactor, float focusX, float focusY) {
        if (DEBUG) {
            LogManager.getLogger().d(
                    LOG_TAG,
                    String.format("onScale: scale: %.2f. fX: %.2f. fY: %.2f", scaleFactor, focusX, focusY));
        }

        if ((getScale() < mMaxScale || scaleFactor < 1f) && (getScale() > mMinScale || scaleFactor > 1f)) {
            mSuppMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
            checkAndDisplayMatrix();
        }

        if (mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }

    /**
     * Helper method that simply checks the Matrix, and then displays the result
     */
    private void checkAndDisplayMatrix() {
        if (checkMatrixBounds()) {
            setImageViewMatrix(getDrawMatrix());
        }
    }

    private void setImageViewMatrix(Matrix matrix) {

        checkImageViewScaleType();
        setImageMatrix(matrix);
    }

    private void checkImageViewScaleType() {

        /**
         * PhotoView's getScaleType() will just divert to this.getScaleType() so
         * only call if we're not attached to a PhotoView.
         */
        if (!ScaleType.MATRIX.equals(getScaleType())) {
            throw new IllegalStateException(
                    "The ImageView's ScaleType has been changed since attaching a PhotoViewAttacher. You should call setScaleType on the PhotoViewAttacher instead of on the ImageView");
        }
    }

    /**
     * Calculate Matrix for FIT_CENTER
     *
     * @param d - Drawable being displayed
     */
    private void updateBaseMatrix(Drawable d) {
        ImageView imageView = this;
        if (null == imageView || null == d) {
            return;
        }

        final float viewWidth = getImageViewWidth(imageView);
        final float viewHeight = getImageViewHeight(imageView);
        final int drawableWidth = d.getIntrinsicWidth();
        final int drawableHeight = d.getIntrinsicHeight();

        mBaseMatrix.reset();

        final float widthScale = viewWidth / drawableWidth;
        final float heightScale = viewHeight / drawableHeight;

        if (mScaleType == ScaleType.CENTER) {
            mBaseMatrix.postTranslate((viewWidth - drawableWidth) / 2F,
                    (viewHeight - drawableHeight) / 2F);

        } else if (mScaleType == ScaleType.CENTER_CROP) {
            float scale = Math.max(widthScale, heightScale);
            mBaseMatrix.postScale(scale, scale);
            mBaseMatrix.postTranslate((viewWidth - drawableWidth * scale) / 2F,
                    (viewHeight - drawableHeight * scale) / 2F);

        } else if (mScaleType == ScaleType.CENTER_INSIDE) {
            float scale = Math.min(1.0f, Math.min(widthScale, heightScale));
            mBaseMatrix.postScale(scale, scale);
            mBaseMatrix.postTranslate((viewWidth - drawableWidth * scale) / 2F,
                    (viewHeight - drawableHeight * scale) / 2F);

        } else {
            RectF mTempSrc = new RectF(0, 0, drawableWidth, drawableHeight);
            RectF mTempDst = new RectF(0, 0, viewWidth, viewHeight);


//            if ((int) mBaseRotation % 180 != 0) {
//                mTempSrc = new RectF(0, 0, drawableHeight, drawableWidth);
//            }

            switch (mScaleType) {
                case FIT_CENTER:
                    mBaseMatrix.setRectToRect(mTempSrc, mTempDst, Matrix.ScaleToFit.CENTER);
                    break;

                case FIT_START:
                    mBaseMatrix.setRectToRect(mTempSrc, mTempDst, Matrix.ScaleToFit.START);
                    break;

                case FIT_END:
                    mBaseMatrix.setRectToRect(mTempSrc, mTempDst, Matrix.ScaleToFit.END);
                    break;

                case FIT_XY:
                    mBaseMatrix.setRectToRect(mTempSrc, mTempDst, Matrix.ScaleToFit.FILL);
                    break;

                default:
                    break;
            }
        }
        resetMatrix();
    }

    /**
     * Resets the Matrix back to FIT_CENTER, and then displays it.s
     */
    private void resetMatrix() {
        mSuppMatrix.reset();
        setImageViewMatrix(getDrawMatrix());
        checkMatrixBounds();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        }
        if (mCanvas == null) {
            mCanvas = new Canvas(mBitmap);

        }

    }

    public void getMagnifierMove() {

        int drawableViewLeft = (int) getDisplayRect(getDrawMatrix()).left;

        int drawableViewTop = (int) getDisplayRect(getDrawMatrix()).top;
        //放大镜画布左边移动极限
        if (mLastX < 150 + drawableViewLeft) {
            mLastX = 150 + drawableViewLeft;
        }
        //放大镜画布顶边移动极限
        if (mLastY < 150) {
            mLastY = 150;
        }

        //放大镜画布右边移动极限
        if (mLastX > getWidth() - 150 - drawableViewLeft) {
            mLastX = getWidth() - 150 - drawableViewLeft;
        }
        //放大镜画布底边移动极限
        if (mLastY > getHeight() - 150) {
            mLastY = getHeight() - 150;
        }
        mLastX = mLastX - drawableViewLeft;
        mLastX = (-mLastX + 150);
        mLastY = (-mLastY + 150 + drawableViewTop);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mIsDraw) {
            if (mMagnifierBitmap == null) return;
            canvas.save();
            LogManager.getLogger().d(LOG_TAG, (int) getDisplayRect(getDrawMatrix()).width() + " getDisplayRect  ");
            canvas.clipRect(0, 0, 300, 300);
//          canvas.setMatrix(mDrawMatrix);
            Rect src = new Rect(0, 0, mMagnifierBitmap.getWidth(), mMagnifierBitmap.getHeight());

            Rect dst = new Rect(0, 0, (int) (mMagnifierBitmap.getWidth() * getScale()), (int) (mMagnifierBitmap.getHeight() * getScale()));
            LogManager.getLogger().d(LOG_TAG, getScale() + "");
            getMagnifierMove();

            canvas.translate(mLastX, mLastY);
            canvas.drawBitmap(mMagnifierBitmap, src, dst, null);
            canvas.restore();
            //mPathDrawer.onDraw(canvas, mCurrentDrawingPath, mPaths);

        }

//        canvas.clipRect(getDisplayRect(getDrawMatrix()));
//        mPathDrawer.onDraw(canvas, mCurrentDrawingPath, mPaths);
//        if (mPopup.isShowing()) {
//            canvas.drawCircle(mLastX, mLastY, mOutterPaint.getStrokeWidth(), mOutterPaint);
//
//        }
        //canvas.drawRect(getDisplayRect(getDrawMatrix()),mOutterPaint);
    }

    private void initPaint() {
        mOutterPaint = new Paint();
        mPath = new Path();
        mOutterPaint.setColor(Color.WHITE);
        mOutterPaint.setAntiAlias(true);
        mOutterPaint.setDither(true);
        mOutterPaint.setStrokeJoin(Paint.Join.ROUND);
        mOutterPaint.setStrokeCap(Paint.Cap.ROUND);
        mOutterPaint.setStyle(Paint.Style.FILL);
        mOutterPaint.setStrokeWidth(20);
        mOutterPaint.setAlpha(100);


    }

    private void drawPath() {
        mOutterPaint.setStyle(Paint.Style.STROKE);
        mOutterPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        mCanvas.drawPath(mPath, mOutterPaint);
    }

    private void initMagnifier() {
        BitmapDrawable resDrawable = (BitmapDrawable) mContext.getResources().getDrawable(R.drawable.ic_launcher);
        mResBitmap = resDrawable.getBitmap();

        mMagnifier = new Magnifier(mContext);

        //pop在宽高的基础上多加出边框的宽高
        mPopup = new PopupWindow(mMagnifier, WIDTH + 2, HEIGHT + 10);
        mPopup.setAnimationStyle(android.R.style.Theme_Black);

        // mDstPoint = new Point(0, 0);
    }

    class Magnifier extends View {
        private Paint mPaint;
        private Path path1;

        public Magnifier(Context context) {
            super(context);
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(0xff008000);
            mPaint.setColor(Color.LTGRAY);
            mPaint.setStrokeWidth(2);

            path1 = new Path();
            path1.moveTo(0, 0);
            path1.lineTo(WIDTH, 0);
            path1.lineTo(WIDTH, HEIGHT);
            path1.lineTo(0, HEIGHT);
            path1.close();//封闭
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.save();
            // draw mPopup
            mPaint.setAlpha(255);

            canvas.drawBitmap(mResBitmap, 0, 0, mPaint);
            canvas.translate(-mCanvasX, -mCanvasY);
            mPathDrawer.onDraw(canvas, mCurrentDrawingPath, mPaths);
            canvas.drawCircle(mLastX, mLastY, mOutterPaint.getStrokeWidth(), mOutterPaint);
            // canvas.drawBitmap(mMagnifierBitmap, 0, 0, mPaint);
            canvas.restore();
            //draw mPopup frame
            mPaint.reset();//重置
            mPaint.setStyle(Paint.Style.STROKE);//设置空心

            canvas.drawPath(path1, mPaint);
        }
    }

    /**
     * @param
     * @param x      截图起始的横坐标
     * @param y      截图起始的纵坐标
     * @param width
     * @param height
     * @return
     */
    private Bitmap getBitmap(int x, int y, int width, int height) {
//        setDrawingCacheEnabled(true);
        if (resDrawable == null) {
            this.resDrawable = (BitmapDrawable) getDrawable();
            mMagnifierBitmap = resDrawable.getBitmap();

        }
        y = y - mThisOnScreenY;
        //边界处理,否则会崩滴
        if (x < 0)
            x = 0;
        if (y < 0)
            y = 0;

        if (x + width > mMagnifierBitmap.getWidth()) {
//            x = x + WIDTH / 2;
//            width = mMagnifierBitmap.getWidth() - x;
            //保持不改变,截取图片宽高的原则
            x = mMagnifierBitmap.getWidth() - width;
        }
        if (y + height > mMagnifierBitmap.getHeight()) {
//            y = y + HEIGHT / 2;
//            height = mMagnifierBitmap.getHeight() - y;
            y = mMagnifierBitmap.getHeight() - height;
        }
        mCanvasX = x;
        mCanvasY = y;
        // mMagnifierBitmap = Bitmap.createBitmap(mMagnifierBitmap, x, y, width, height);
//        setDrawingCacheEnabled(false);
        return mMagnifierBitmap;
    }

    Runnable mShowZoom = new Runnable() {
        public void run() {
            mPopup.showAtLocation(MopiImageView.this,
                    Gravity.NO_GRAVITY,
                    getLeft() + mThisOnScreenX,
                    getTop() + mThisOnScreenY);
        }
    };

    private boolean calculate(int touchX, int touchY, int action) {
        //mDstPoint.set(x - WIDTH / 2, y - 3 * HEIGHT);
        if (touchY < 0) {
            // hide mPopup if out of bounds
            mPopup.dismiss();
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            removeCallbacks(mShowZoom);
            postDelayed(mShowZoom, 0);
        } else if (!mPopup.isShowing()) {
            mShowZoom.run();
        }
        initOnScreenPosition();
        int popupTouchRightX = mThisOnScreenX + getWidth() - mPopup.getWidth();
        int popupTouchRightY = mThisOnScreenY + getTop() + mPopup.getHeight();

        int popupTouchLeftUpX = mThisOnScreenX + mPopup.getWidth();
        int popupTouchLeftUpY = mThisOnScreenY + mPopup.getHeight();

        //手指移动到右上角的时候，放大镜移动到左边
        if (touchX > popupTouchRightX && touchY < popupTouchRightY) {
            mPopup.update(getLeft() + mThisOnScreenX, getTop() + mThisOnScreenY, -1, -1);

        }

        //手指移动到左上角的时候，放大镜移动到右边
        if (touchX < popupTouchLeftUpX && touchY < popupTouchLeftUpY) {
            mPopup.update(getWidth() + mThisOnScreenX + mPopup.getWidth(), getTop() + mThisOnScreenY, -1, -1);

        }

//        LogManager.getLogger().d(LOG_TAG, touchX + " touch xy " + touchY);
//        LogManager.getLogger().d(LOG_TAG, touchY + " mThisOnScreenX " + (mThisOnScreenY + getTop() + mPopup.getHeight()));
        mMagnifier.invalidate();
        return true;
    }

    @Override
    public void onGestureCreated(SerializablePath serializablePath) {
        mPaths.add(serializablePath);
    }

    @Override
    public void onCurrentGestureChanged(SerializablePath currentDrawingPath) {
        this.mCurrentDrawingPath = currentDrawingPath;

    }

    public void setConfig(PaintConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Paint configuration cannot be null");
        }
        mPaintGestureDetector.setmConfig(config);
    }

    public void undo() {
        if (mPaths.size() > 0) {
            mPaths.remove(mPaths.size() - 1);
            invalidate();
        }
    }

    public void isShowHistoryPaths(boolean isShow) {
        mPathDrawer.isShowHistoryPaths(isShow);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        SerializePaths();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        DeserializePerson();
    }

    /**
     * 反序列化，获取本地存的画笔路径
     */
    private void DeserializePerson() {

        new AsyncTask<Void, Void, ArrayList<SerializablePath>>() {
            @Override
            protected ArrayList<SerializablePath> doInBackground(Void... params) {

                ObjectInputStream ois;
                ArrayList<SerializablePath> sPaths = null;

                try {
                    File mPaintLocalPathFile = new File(mPaintLocalPath);
                    if (mPaintLocalPathFile.exists()) {
                        ois = new ObjectInputStream(new FileInputStream(mPaintLocalPathFile));
                        sPaths = (ArrayList<SerializablePath>) ois.readObject();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                return sPaths;
            }

            @Override
            protected void onPostExecute(ArrayList<SerializablePath> paintPath) {
                if (paintPath != null) {
                    mPaths = paintPath;
                    for (SerializablePath p : mPaths) {
                        p.loadPathPointsAsQuadTo();
                    }
                    invalidate();
                }
            }
        }.execute();
    }

    /**
     * 序列化画笔路径到本地
     */
    private void SerializePaths() {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ObjectOutputStream oos = null;
                try {
                    oos = new ObjectOutputStream(new FileOutputStream(new File(mPaintLocalPath)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (oos != null) {
                        oos.writeObject(mPaths); //写入到本地
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (oos != null) {
                            oos.close();//关流

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        }.execute();

    }
}
