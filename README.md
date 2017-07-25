# RadarDiffuseView
网上有很多类似的教程，这次只是从一个初学者的角度，进行一番自定义view的思路分析，以达到举一反三的效果。
![GIF4.gif](http://upload-images.jianshu.io/upload_images/1758858-2fb5c10e71b907f5.gif?imageMogr2/auto-orient/strip)
##一、构思步骤
通过观察扩散圆的扩散动作，我们可以解析出三部分：（1）中心不变圆（2）向外扩散圆（3）雷达旋转扫描
 （1）中心不变圆可以绘制任意图标。   
 （2）扩散圆波纹其实是由同样的一个扩散圆重复绘制出来的，只是在产生时间上的不一致，即先后出现时间不一致而形成的一种效果。
 （3）雷达旋转扫描效果，画笔设置梯度渲染，将canvas画布不断旋转。

   接下来将着重第二个扩散圆波纹的绘制思路，毕竟精力有限。

![Paste_Image.png](http://upload-images.jianshu.io/upload_images/1758858-38c3d67effb95d6c.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)



##二、思路分析
   ####（1）如何绘制出扩散效果

   1、就是不断刷新，每次刷新都改变**半径大小**和**透明度**，值增长到最大值时恢复原值重新开始，对，就是这么简单粗暴，我们看下最基本一个扩散圆的绘制。
             首先参数设定，这个不过多解释，按图按需撅参数。
    
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
    //是否开始扫描
    private Boolean startDiffseBoolean = true;

      @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
       //获取到中心圆坐标
        mCenterX = w/2;
        mCenterY = h/2;
        minRadius = Math.min(mCenterX,mCenterY);
    }


        //在构造函数中扩散圆画笔初始化
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

然后在ondraw（）函数中画中心圆与扩散圆

      
       @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setAlpha(mAlpha);
           //画扩散圆
        mAlpha=mAlpha-1;
        mDiffuseWidth =mDiffuseWidth+2;
        if(mAlpha<= 0 || mDiffuseWidth >= minRadius){
            mAlpha =255;
            mDiffuseWidth = mInnerCircleRadius;
        }
        //画扩散圆
        canvas.drawCircle(mCenterX, mCenterY, mDiffuseWidth, mPaint);

        //画中心圆
        canvas.drawCircle(mCenterX, mCenterY, mInnerCircleRadius, mCenterPaint);
        if (startDiffseBoolean) {
            postInvalidate();
        }
    }
    

    @Override
    public void postInvalidate() {
        if (hasWindowFocus()) {
            super.postInvalidate();
        }
    }

新鲜滚热辣，我们马上看下效果。是的，就是这么简单粗暴，而且效果还不赖。
   ![GIF.gif](http://upload-images.jianshu.io/upload_images/1758858-bf8cd0732f7a582c.gif?imageMogr2/auto-orient/strip)

在这里，就有个地方值得去思考了，透明度的值是从255~0的，扩散圆半径是从中心圆半径变化到最大值的，如果像上述那样，你会看到在透明度还没完全变成0时，就重新扩散了，根本就没有同步变化到相应的值就恢复原值重新开始了，因为这非常难估算，重要的是非常的不优雅，每次更改数值后也都得去算？其实，我们可以用一个共同的纽带，把这些需要变化的值关联起来，使其变化进度一致，是的，那就定义一个float类型的进度值progress吧，范围为0~1，就是这么简单粗暴。
**原理：**从**开始值**到**最终值**，其实就是它们的**差值**从0%变化到100%的过程。（其实这非常类似于**贝塞尔曲线**的实现原理）

我们添加如下函数 并且修改ondraw（）里面的步骤。

    /**
     * @param start    开始值
     * @param end      结束值
     * @param progress 根据进度值计算相应需要的变化值
     * @return
     */
    private float getVualeByProgress(float start, float end, @FloatRange(from = 0.0f, to = 1.0f) float progress) {
        return start + (end - start) * progress;

    }

     private float progress = 0f;
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setAlpha(mAlpha);
           //画扩散圆
        
        progress +=0.05;
        mAlpha = (int) getVualeByProgress(255,0,progress);
        mDiffuseWidth = getVualeByProgress(mInnerCircleRadius,minRadius,progress);
        
       if(progress >=1.0f){
           //恢复原值
           progress = 0f;
       }
        //画扩散圆
        canvas.drawCircle(mCenterX, mCenterY, mDiffuseWidth, mPaint);

        //画中心圆
        canvas.drawCircle(mCenterX, mCenterY, mInnerCircleRadius, mCenterPaint);
        if (startDiffseBoolean) {
            postInvalidate();
        }
    }

  再来运行一下，会感觉透明度的变化会顺畅很多，从255一直变化到0，半径也一直从中心圆半径变化到边缘

![GIF1.gif](http://upload-images.jianshu.io/upload_images/1758858-164249b9612dddfc.gif?imageMogr2/auto-orient/strip)

###2、如何绘制扩散波纹效果？
 #####这是本次分析的重点！！！！！
我们按照正常的思路去思考，就是在它第一个半径扩散相应的宽度后再添加多一个扩散圆扩散出来，然后依次类推，第二个扩散圆扩散到相应的宽度后又再添加多一个扩散圆扩散出来，这样无限循环，然后就是。。。毫无头绪，刚开始的我是懵逼的。
####（1）先完成静态圆形扩散图
但是你从画面的角度去思考，其实这个地球上的电视电影，屏幕上的动画都是由一帧一帧画面构成的，**只是切换的速度快了，只是每帧画面有了细微的变化，也就成了动画。**
所以我先考虑，怎么画出**一张静态不动的扩散波纹图**！这么说，你是不是就觉得马上来头绪了？只要开一个数组List，存储不同的进度值，计算相应的半径和透明度，for循环绘制圆形，一张波纹图就浮现在你眼前了。

      //画静态扩散圆
        List<Float> progressL = new ArrayList<>();
        for(int i = 0;i<5;i++){
            progressL.add(i*0.2f);
        }
        for(int i = 0;i<progressL.size();i++){
            mAlpha = (int) getVualeByProgress(255,0,progressL.get(i));
            mDiffuseWidth = getVualeByProgress(mInnerCircleRadius,minRadius,progressL.get(i));
            mPaint.setAlpha(mAlpha);
            canvas.drawCircle(mCenterX, mCenterY, mDiffuseWidth, mPaint);
        }


![](http://upload-images.jianshu.io/upload_images/1758858-7020b6f7ffd6f5e8.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
###（2）让静态图动起来
我们一开始是利用一个扩散圆不断变化透明度和半径来达到了动态扩散的效果，那现在呢，当然是让List里面的每个圆（一个进度值progress可以绘制一个圆）也进行不断的变化，我们再次修改代码如下，让每个进度值在绘制完之后逐渐增加

          for(int i = 0;i<progressL.size();i++){
            mAlpha = (int) getVualeByProgress(255,0,progressL.get(i));
            mDiffuseWidth = getVualeByProgress(mInnerCircleRadius,minRadius,progressL.get(i));
            mPaint.setAlpha(mAlpha);
            canvas.drawCircle(mCenterX, mCenterY, mDiffuseWidth, mPaint);


            if(progressL.get(i)<1.0f) {
                progressL.set(i,progressL.get(i)+0.01f);
               }else {
                progressL.set(i,i*0.2f);
            }
        }



![GIF2.gif](http://upload-images.jianshu.io/upload_images/1758858-d5ee8f01636305e8.gif?imageMogr2/auto-orient/strip)

虽然效果惨不忍睹，但是至少能动了呀，我们离成功又近了一步啊，接下来要分析画面如此惨不忍睹的原因，其实仔细看代码梳理一下，就会发现，list里 的每个圆都是在到最大值后直接粗暴地恢复到原始值，然而我为了扩散的效果在初始化的时候，每个初始值都不是0（**progressL.add(i*0.2f);**），所以也就造成这种不伦不类的效果。

那接下来又要怎么解决呢，这时候可以再翻上去看一下上面构思步骤的（2）
>扩散圆波纹其实是由同样的一个扩散圆重复绘制出来的，只是在产生时间上的不一致，即先后出现时间不一致而形成的一种效果。

也就是说，只要解决先后顺序就可以了，而不是同时绘制，那我一开始的话，list就只给一个值就好了，等到第一个值达到相应值了，再添加（list.add（））多一个值，刷新时for循环绘制圆，list的size（）达到一定数量就把第一个圆remove掉（第一个圆这时已经透明度为0），这样一进一出，就可以无限扩散了。修改代码如下：

       List<Float> progressL;
      //画静态扩散圆
        progressL = new ArrayList<>();
         //初始化时只添加一个值
            progressL.add(0f);

    }
 
    private float progress = 0f;
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for(int i = 0;i<progressL.size();i++){
            mAlpha = (int) getVualeByProgress(255,0,progressL.get(i));
            mDiffuseWidth = getVualeByProgress(mInnerCircleRadius,minRadius,progressL.get(i));
            mPaint.setAlpha(mAlpha);
            canvas.drawCircle(mCenterX, mCenterY, mDiffuseWidth, mPaint);

            if(progressL.get(i)<1.0f) {
                progressL.set(i,progressL.get(i)+0.008f);
               }
        }

        //当宽度达到相应宽度时，list添加多一个progress，这样不断从中心圆扩散出新的圆
        if (mDiffuseWidth > minRadius / 3) {
            progressL.add(0f);
        }
        //当线圈的数量超过指定数量时，移除list第一个线圈，这时list.get(0)已经扩散到最边缘，已经没有存在的必要了啊亲，这样防止list无限增长
        if (progressL.size() >= 6) {
            progressL.remove(0);
        }


![GIF3.gif](http://upload-images.jianshu.io/upload_images/1758858-07e5c863737683ef.gif?imageMogr2/auto-orient/strip)
###（3）绘制雷达扫描
   这个有点鸡肋，不过或许有些人会用上呢。

       //定义一个暗蓝色的梯度渲染
        mSweepShader = new SweepGradient(mCenterX, mCenterY,
                Color.TRANSPARENT,mPaintColor);
        mPaintSector.setShader(mSweepShader);


        //绘制雷达扫描效果
        canvas.save();
        canvas.rotate(mOffsetArgs,mCenterX,mCenterY);
        canvas.drawCircle(mCenterX, mCenterY, minRadius, mPaintSector);
        if(mOffsetArgs<360){
            mOffsetArgs+=1;

        }else if(mOffsetArgs == 360){
            mOffsetArgs = 0;
        }
        canvas.restore();


![GIF4.gif](http://upload-images.jianshu.io/upload_images/1758858-2fb5c10e71b907f5.gif?imageMogr2/auto-orient/strip)

##三、最后完善
为view添加自定义属性。
第一步：在values目录下创建自定义属性的XML，比如attrs.xml，也可以选择类似于attrs_radardiffuse_view.xml等这种以attrs_开头的文件名。

     <?xml version="1.0" encoding="utf-8"?>
      <resources>
    <declare-styleable name="RadarDiffuseView">
        <!--扩散圆画笔颜色-->
        <attr name="diffusePaint_Color" format="color"    />
        <!--中心圆画笔颜色-->
        <attr name="innerPaint_Color" format="color"/>
        <!--中心圆半径-->
        <attr name="innerCircle_Radius" format="dimension"/>
        <!--是否开启雷达扫描-->
        <attr name="radarBool" format="boolean"/>
    </declare-styleable>
    </resources>
第二步：在构造函数中取出自定义属性。

     public RadarDiffuseView(Context context, @Nullable AttributeSet attrs)
          { super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.RadarDiffuseView);
        RadarBool = a.getBoolean(R.styleable.RadarDiffuseView_innerPaint_Color,true);
        mPaintColor =a.getInt(R.styleable.RadarDiffuseView_diffusePaint_Color,R.color.colorAccent);
        mInnerCircleRadius = a.getFloat(R.styleable.RadarDiffuseView_innerCircle_Radius,100);
        mCenterPaintColor = a.getInt(R.styleable.RadarDiffuseView_innerPaint_Color,R.color.colorPrimary);
        a.recycle();
        init();
    }
第三步：在布局中使用。
         
      <?xml version="1.0" encoding="utf-8"?>
    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.waiwen.radardiffuseveiw.MainActivity">

          <com.waiwen.radardiffuseveiw.RadarDiffuseView
       android:id="@+id/radar_view"
       android:layout_width="match_parent"
       app:innerCircle_Radius="50dp"
       app:diffusePaint_Color="#3300ff00"
       android:layout_height="match_parent" />

     </LinearLayout>
##四、总结。

看似复杂的动画其实都是由一个一个元素构成的，所以要分解部件，逐一击破。
很少很少写博客，码字匆忙，如有错误或者需要改进的地方可以指出哟，发现码字比码代码其实还要辛苦，如果你觉得有意思可以点个**喜欢**（听说点的都很帅）鼓励下哟。
代码上面已经贴出大部分，如果要看下源码可以留言，我再传到github，我就先不上传了。
毕竟**思路**才是最重要的。

![886.gif](http://upload-images.jianshu.io/upload_images/1758858-8a78dfa2d1738436.gif?imageMogr2/auto-orient/strip)


###参考：
《Android开发艺术探索》
[周游的博客](http://blog.csdn.net/airsaid/article/details/52683193)
[慕课网--带您完成神秘的涟漪按钮效果-入门篇](http://www.imooc.com/learn/741)
