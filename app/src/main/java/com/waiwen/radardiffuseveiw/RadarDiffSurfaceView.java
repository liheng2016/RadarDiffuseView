package com.waiwen.radardiffuseveiw;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.SweepGradient;
import android.support.annotation.FloatRange;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

import static android.util.Log.d;

/**
 * Created by waiwen .
 * time:2017/7/24 15:56.
 * QQ:958159428
 * E-mail:iwaiwen@163.com .
 */

public class RadarDiffSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {


    private SurfaceHolder surfaceHolder;
    private Canvas canvas;
    private boolean mIsDrawing;

    private static final String TAG = "waiwen";


    //扩散圆宽度取控件的长度最小值
    private float minRadius;
    //中心圆画笔
    private Paint mCenterPaint;
    //中心圆画笔颜色
    private int mCenterPaintColor = getResources().getColor(R.color.colorAccent);
    //透明度
    private int mAlpha = 255;
    //扩散圆画笔
    private Paint mPaint;
    //扩散圆画笔颜色
    private int mPaintColor = getResources().getColor(R.color.colorPrimary);

    //圆心X
    private float mCenterX;
    //圆心Y
    private float mCenterY;
    //中心圆半径
    private float mInnerCircleRadius = 50;
    //扩散圆扩散宽度
    private float mDiffuseWidth = 0;

    //是否打开雷达扫描
    private boolean RadarBool = false;

    //存放进度的数组
    private List<Float> progressList;
    //绘制雷达扫描的画笔
    private Paint mPaintSector;
    //画笔梯度渲染
    private SweepGradient mSweepShader;
    //雷达画笔颜色
    private int mRadarPaintColor = 0x9D00ff00;

    //扩散速度
    private float mProgressIncrement = 8f / 3000f;
    //旋转角度
    private int mOffsetArgs;


    public RadarDiffSurfaceView(Context context) {
        super(context);
        init();
    }

    public RadarDiffSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RadarDiffuseView);
        RadarBool = a.getBoolean(R.styleable.RadarDiffuseView_innerPaint_Color, true);
        mPaintColor = a.getInt(R.styleable.RadarDiffuseView_diffusePaint_Color, R.color.colorAccent);
        mInnerCircleRadius = a.getFloat(R.styleable.RadarDiffuseView_innerCircle_Radius, 100);
        mCenterPaintColor = a.getInt(R.styleable.RadarDiffuseView_innerPaint_Color, R.color.colorPrimary);
        a.recycle();
        init();
    }

    public RadarDiffSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RadarDiffuseView);
        RadarBool = a.getBoolean(R.styleable.RadarDiffuseView_innerPaint_Color, true);
        mPaintColor = a.getInt(R.styleable.RadarDiffuseView_diffusePaint_Color, R.color.colorAccent);
        mInnerCircleRadius = a.getFloat(R.styleable.RadarDiffuseView_innerCircle_Radius, 100);
        mCenterPaintColor = a.getInt(R.styleable.RadarDiffuseView_innerPaint_Color, R.color.colorPrimary);
        a.recycle();
        init();
    }


    private void init() {


        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        this.getKeepScreenOn();

        progressList = new ArrayList<>();
        progressList.add(0f);

        //扩散圆画笔初始化
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //防抖动
        mPaint.setDither(true);

        mPaint.setColor(mPaintColor);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(4);


        //中心画笔初始化
        mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //防抖动
        mCenterPaint.setDither(true);
        mCenterPaint.setStyle(Paint.Style.FILL);
        mCenterPaint.setColor(mCenterPaintColor);

        //雷达扫描画笔
        mPaintSector = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintSector.setDither(true);
        mPaintSector.setColor(mRadarPaintColor);

        //定义一个暗蓝色的梯度渲染
        mSweepShader = new SweepGradient(mCenterX, mCenterY,
                Color.TRANSPARENT, mPaintColor);
        mPaintSector.setShader(mSweepShader);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = w / 2;
        mCenterY = h / 2;
        //获取到扩散的宽度
        minRadius = Math.min(mCenterX, mCenterY);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mIsDrawing = true;
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mIsDrawing = false;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        while (mIsDrawing) {
           draw();
        }
        long end = System.currentTimeMillis();
//        if (end - start < 100) {
//            try {
//                Thread.sleep(100 - (end - start));
//
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        boolean onTouch = false;
        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                startDiffseBoolean(false);
                onTouch = true;
                break;
            case MotionEvent.ACTION_UP:

                startDiffseBoolean(true);
                onTouch = true;
                break;
            default:
                break;
        }
        return onTouch;


    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
     //   draw();
        //画中心圆
        canvas.drawCircle(mCenterX, mCenterY, mInnerCircleRadius, mCenterPaint);
//        if (startDiffseBoolean) {
//            postInvalidate();
//        }

    }

    /**
     * 画图
     */
    private void draw() {
        try {
            canvas = surfaceHolder.lockCanvas();
            //清屏
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);



            //   ---------------------------核心代码----------------------------：
            // 循环取出progreessList数组上的progress，进行相应的计算获取每次的alph和width进行绘制
            for (int i = 0; i < progressList.size(); i++) {

                float progress = progressList.get(i);
                d(TAG, "onDraw: progress          " + progress);
                //获取到每次绘制的值
                float realProgress = progress;//mEnterInterpplator.getInterpolation(progress);
                //     float realProgress = progress;//mAcce.getInterpolation(progress);
                mDiffuseWidth = getVualeByProgress(mInnerCircleRadius, minRadius, progress);
                mAlpha = (int) getVualeByProgress(255, 0, progress);
                //绘制
                mPaint.setAlpha(mAlpha);
                canvas.drawCircle(mCenterX, mCenterY, mDiffuseWidth, mPaint);
                //list中的progress不断进行叠加,这样才会有扩散的效果
                if (progress < 1.0f) {
                    progressList.set(i, progress + mProgressIncrement);
                }

            }
            //当宽度达到相应宽度时，list添加多一个progress，这样不断从中心圆扩散出新的圆
            if (mDiffuseWidth > minRadius / 3) {
                d(TAG, "onDraw:  添加");
                progressList.add(0f);
            }
            //当线圈的数量超过指定数量时，移除list第一个线圈，这时list.get(0)已经扩散到最边缘，已经没有存在的必要了啊亲，这样防止list无限增长
            if (progressList.size() >= 6) {
                d(TAG, "onDraw:    移除");
                progressList.remove(0);
            }

//            if (RadarBool) {
//
//                //绘制雷达扫描效果
//                canvas.save();
//                canvas.rotate(mOffsetArgs, mCenterX, mCenterY);
//                canvas.drawCircle(mCenterX, mCenterY, minRadius, mPaintSector);
//                if (mOffsetArgs < 360) {
//                    mOffsetArgs += 1;
//                } else if (mOffsetArgs == 360) {
//                    mOffsetArgs = 0;
//                }
//                canvas.restore();
//            }




        } catch (Exception e) {
        } finally {
            if (canvas != null) {
                surfaceHolder.unlockCanvasAndPost(canvas);

            }

        }


    }

    /**
     * @param start    开始值
     * @param end      结束值
     * @param progress 根据进度值计算相应需要的变化值
     * @return
     */
    private float getVualeByProgress(float start, float end, @FloatRange(from = 0.0f, to = 1.0f) float progress) {
        return start + (end - start) * progress;

    }

    /**
     * @param radarBool 是否雷达扫描
     */
    public void setRadarBool(boolean radarBool) {
        this.RadarBool = radarBool;
        postInvalidate();
    }

    /**
     * @param startDiffseBoolean    是否开始扩散
     */

    //是否开始扫描
    private Boolean startDiffseBoolean = true;

    public void startDiffseBoolean(Boolean startDiffseBoolean) {
        this.startDiffseBoolean = startDiffseBoolean;
        postInvalidate();
    }

//    @Override
//    public void postInvalidate() {
//        if (hasWindowFocus()) {
//            super.postInvalidate();
//        }
//    }
}
