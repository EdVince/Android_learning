package com.example.androidcanvasbasics;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.core.content.ContextCompat;

public class MyView extends View {

    Paint mPaint, otherPaint, outerPaint, mTextPaint;
    RectF mRectF;
    int mPadding;

    float arcLeft, arcTop, arcRight, arcBottom;

    Path mPath;

    public MyView(Context context) {
        super(context);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE); // 空心
        mPaint.setColor(Color.BLUE); // 蓝色
        mPaint.setStrokeWidth(5);

        mTextPaint = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.BLACK); // 黑色字
        mTextPaint.setTextSize(pxFromDp(context,24));

        otherPaint = new Paint();

        outerPaint = new Paint();
        outerPaint.setStyle(Paint.Style.FILL); // 实心
        outerPaint.setColor(Color.YELLOW); // 黄色

        mPadding = 10;

        DisplayMetrics displayMetrics = new DisplayMetrics();

        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        arcLeft = pxFromDp(context,20);
        arcTop = pxFromDp(context,20);
        arcRight = pxFromDp(context,100);
        arcBottom = pxFromDp(context,100);

        Point p1 = new Point((int)pxFromDp(context,80)+(screenWidth/2),(int)pxFromDp(context,40));
        Point p2 = new Point((int)pxFromDp(context,40)+(screenWidth/2),(int)pxFromDp(context,80));
        Point p3 = new Point((int)pxFromDp(context,120)+(screenWidth/2),(int)pxFromDp(context,80));

        mPath = new Path();
        mPath.moveTo(p1.x,p1.y);
        mPath.lineTo(p2.x,p2.y);
        mPath.lineTo(p3.x,p3.y);
        mPath.close();

        mRectF = new RectF(screenWidth/4,screenHeight/3,screenWidth/6,screenHeight/2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 画一个圆角矩形，再掏个洞避免被后面的背景覆盖
        canvas.drawRoundRect(mRectF,10,10,otherPaint);
        canvas.clipRect(mRectF, Region.Op.DIFFERENCE);

        // 画背景颜色
        canvas.drawPaint(outerPaint);

        // 画一条直线
        canvas.drawLine(250,250,400,400,mPaint);

        // 画一个矩形
        canvas.drawRect(mPadding,mPadding,getWidth()-mPadding,getHeight()-mPadding,mPaint);

        // 画一个扇形
        canvas.drawArc(arcLeft,arcTop,arcRight,arcBottom,75,45,true,mPaint);

        // 画一个绿色实心矩形
        otherPaint.setColor(Color.GREEN);
        otherPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(
                getLeft()+(getRight()-getLeft())/3,
                getTop()+(getBottom()-getTop())/3,
                getRight()-(getRight()-getLeft())/3,
                getBottom()-(getBottom()-getTop())/3,otherPaint);


        // 画一个三角形
        canvas.drawPath(mPath,mPaint);

        // 画一个黑色圆
        otherPaint.setColor(Color.BLACK);
        canvas.drawCircle(getWidth()/2,getHeight()/2,arcLeft,otherPaint);

        // 画一行字
        canvas.drawText("Canvas basics",(float)(getWidth()*0.3),(float)(getHeight()*0.8),mTextPaint);

    }

    public static float pxFromDp(final Context context, final float dp) {
        return dp*context.getResources().getDisplayMetrics().density;
    }

}
