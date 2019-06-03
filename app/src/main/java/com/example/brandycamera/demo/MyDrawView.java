package com.example.brandycamera.demo;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Looper;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MyDrawView extends View
{
    public interface IUserDrawAction
    {
        public void drawOn(Canvas canvas);
    }


    private boolean change = true;
    private float rotate_degrees = 0;

    private Map<String, IUserDrawAction> mapStableAct;  //稳定,例如常驻的提示框等
    private Map<String, IUserDrawAction> mapInstantAct; //短暂,例如实时结果等
    private Object mtx = new Object();  // TODO: rw lock

    public MyDrawView(Context context)
    {
        super(context);

        mapStableAct = new HashMap<>();
        mapInstantAct = new HashMap<>();
    }

    public void refreshView()
    {
        this.change = true;
        invalidateView();
    }

    public void cleanView()
    {
        this.change = false;
        invalidateView();
    }

    private void invalidateView()
    {
        if (Looper.myLooper() == Looper.getMainLooper())
        {
            this.invalidate();
        }
        else
        {
            this.postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        if (change)
        {
            change = false;
            // drawOnScreenImpl(canvas);
            drawImpl(canvas);
        }
    }


    /*
     * 增删查
     */

    ////稳定
    public void putStable(String key, IUserDrawAction action)
    {
        putImpl(key, action, 0);
    }

    public String[] hasStable(String keyRegex)
    {
        return hasImpl(keyRegex, 0);
    }

    public int removeStable(String keyRegex)
    {
        return removeImpl(keyRegex, 0);
    }

    public void clearStable()
    {
        clearImpl(0);
    }

    ////短暂
    public void putInstant(String key, IUserDrawAction action)
    {
        putImpl(key, action, 1);
    }

    public String[] hasInstant(String keyRegex)
    {
        return hasImpl(keyRegex, 1);
    }

    public int removeInstant(String keyRegex)
    {
        return removeImpl(keyRegex, 1);
    }

    public void clearInstant()
    {
        clearImpl(1);
    }

    // 全部:稳定+短暂
    public String[] has(String keyRegex)
    {
        return hasImpl(keyRegex, 2);
    }

    public int remove(String keyRegex)
    {
        return removeImpl(keyRegex, 2);
    }

    public void clear()
    {
        clearImpl(2);
    }


    /**
     * @param key
     * @param action
     * @param type   0=stable, 1=instant, 2=both
     * @return
     */
    private void putImpl(String key, IUserDrawAction action, int type)
    {
        synchronized (mtx)
        {
            switch (type)
            {
                case 1:
                    mapInstantAct.put(key, action);
                    break;

                case 0:
                    mapStableAct.put(key, action);
                    break;
            }
        }
    }

    /**
     * 查询是否含有regex的key
     *
     * @param keyRegex regex
     * @param type     {@link #putImpl(String, IUserDrawAction, int)}
     * @return 返回key的String数组, 若无则返回null
     */
    private String[] hasImpl(String keyRegex, int type)
    {
        String[] result = null;
        synchronized (mtx)
        {
            List<String> li = new ArrayList<>();
            switch (type)
            {
                case 2:
                    for (String s : mapStableAct.keySet())
                    {
                        if (s.matches(keyRegex))
                        {
                            li.add(s);
                        }
                    }
                    // no break;

                case 1:
                    for (String s : mapInstantAct.keySet())
                    {
                        if (s.matches(keyRegex))
                        {
                            li.add(s);
                        }
                    }
                    break;

                case 0:
                    for (String s : mapStableAct.keySet())
                    {
                        if (s.matches(keyRegex))
                        {
                            li.add(s);
                        }
                    }
                    break;
            }

            if (!li.isEmpty())
            {
                result = new String[li.size()];
                for (int i = 0; i < li.size(); i++)
                {
                    result[i] = li.get(i);
                }
            }
        }
        return result;
    }

    /**
     * @param keyRegex regex
     * @param type     {@link #putImpl(String, IUserDrawAction, int)}
     * @return 被remove的计数
     */
    private int removeImpl(String keyRegex, int type)
    {
        int cnt = 0;
        synchronized (mtx)
        {
            Set<String> keys = null;
            switch (type)
            {
                case 2:
                    keys = new HashSet<>(mapStableAct.keySet());
                    for (String s : keys)
                    {
                        if (s.matches(keyRegex))
                        {
                            mapStableAct.remove(s);
                            cnt++;
                        }
                    }
                    // no break

                case 1:
                    keys = new HashSet<>(mapInstantAct.keySet());
                    for (String s : keys)
                    {
                        if (s.matches(keyRegex))
                        {
                            mapInstantAct.remove(s);
                            cnt++;
                        }
                    }
                    break;

                case 0:
                    keys = new HashSet<>(mapStableAct.keySet());
                    for (String s : keys)
                    {
                        if (s.matches(keyRegex))
                        {
                            mapStableAct.remove(s);
                            cnt++;
                        }
                    }
                    break;
            }
        }
        return cnt;
    }

    /**
     * @param type {@link #putImpl(String, IUserDrawAction, int)}
     */
    private void clearImpl(int type)
    {
        synchronized (mtx)
        {
            switch (type)
            {
                case 2:
                    mapStableAct.clear();
                    // no break;

                case 1:
                    mapInstantAct.clear();
                    break;

                case 0:
                    mapStableAct.clear();
                    break;
            }
        }
    }


    private void drawImpl(Canvas canvas)
    {
        synchronized (mtx)
        {
            for (IUserDrawAction act : mapStableAct.values())
            {
                act.drawOn(canvas);
            }
            for (IUserDrawAction act : mapInstantAct.values())
            {
                act.drawOn(canvas);
            }
        }
    }


    // /*
    //  * 线的操作
    //  */
    // //直接坐标
    // public void putLine(String key, float startX, float startY, float stopX, float stopY)
    // {
    //     putLine(key, startX, startY, stopX, stopY, defaultPaintLine);
    // }
    //
    // public void putLine(String key, float startX, float startY, float stopX, float stopY, Paint paint)
    // {
    //     LineInfo line = new LineInfo(startX, startY, stopX, stopY, paint);
    //     lineMap.put(key, line);
    // }
    //
    // //归一化[0~1]形式的坐标
    // public void putLine(String key, float startX_norm, float startY_norm, float stopX_norm, float stopY_norm, int width, int height)
    // {
    //     putLine(key, startX_norm, startY_norm, stopX_norm, stopY_norm, width, height, defaultPaintLine);
    // }
    //
    // public void putLine(String key, float startX_norm, float startY_norm, float stopX_norm, float stopY_norm, int width, int height, Paint paint)
    // {
    //     float startX = startX_norm * width;
    //     float startY = startY_norm * height;
    //     float stopX = stopX_norm * width;
    //     float stopY = stopY_norm * height;
    //     putLine(key, startX, startY, stopX, stopY, paint);
    // }
    //
    // public void removeLine(String key)
    // {
    //     lineMap.remove(key);
    // }
    //
    // public void clearLine()
    // {
    //     lineMap.clear();
    // }
    //
    //
    // /*
    //  * 圆形的操作
    //  */
    //
    // //直接坐标
    // public void putCircle(int key, float x, float y)
    // {
    //     putCircle(key, x, y, 2 /*默认*/, defaultPaintCircle);
    // }
    //
    // public void putCircle(int key, float x, float y, float radius)
    // {
    //     putCircle(key, x, y, radius, defaultPaintCircle);
    // }
    //
    // public void putCircle(int key, float x, float y, float radius, Paint paint)
    // {
    //     CircleInfo circle = new CircleInfo(x, y, radius, paint);
    //     circleMap.put(key, circle);
    // }
    //
    // //归一化[0~1]形式的坐标
    // public void putCircle(int key, float x_norm, float y_norm, int width, int height)
    // {
    //     putCircle(key, x_norm, y_norm, width, height, 2 /*默认*/, defaultPaintCircle);
    // }
    //
    // public void putCircle(int key, float x_norm, float y_norm, int width, int height, float radius)
    // {
    //     putCircle(key, x_norm, y_norm, width, height, radius, defaultPaintCircle);
    // }
    //
    // public void putCircle(int key, float x_norm, float y_norm, int width, int height, float radius, Paint paint)
    // {
    //     float x, y;
    //     x = x_norm * width;
    //     y = y_norm * height;
    //     putCircle(key, x, y, radius, paint); //直接坐标
    // }
    //
    // public void removeCircle(int key)
    // {
    //     circleMap.remove(key);
    // }
    //
    // public void clearCircle()
    // {
    //     circleMap.clear();
    // }
    //
    //
    // /*
    //  * 矩形框的操作
    //  */
    //
    // //直接坐标
    // public void putRect(String key, float left, float top, float right, float bottom)
    // {
    //     putRect(key, left, top, right, bottom, defaultPaintRect);
    // }
    //
    // public void putRect(String key, float left, float top, float right, float bottom, Paint paint)
    // {
    //     RectangleInfo rect = new RectangleInfo(left, top, right, bottom, paint);
    //     rectMap.put(key, rect);
    // }
    //
    // //归一化[0~1]形式的坐标
    // public void putRect(String key, float left_norm, float top_norm, float right_norm, float bottom_norm, int width, int height)
    // {
    //     putRect(key, left_norm, top_norm, right_norm, bottom_norm, width, height, defaultPaintRect);
    // }
    //
    // public void putRect(String key, float left_norm, float top_norm, float right_norm, float bottom_norm, int width, int height, Paint paint)
    // {
    //     float l, t, r, b;
    //     l = left_norm * width;
    //     t = top_norm * height;
    //     r = right_norm * width;
    //     b = bottom_norm * height;
    //     putRect(key, l, t, r, b, paint);  //直接坐标
    // }
    //
    // public void removeRect(String key)
    // {
    //     rectMap.remove(key);
    // }
    //
    // public void clearRect()
    // {
    //     rectMap.clear();
    // }
    //
    //
    // /*
    //  * 文本的操作
    //  */
    //
    // public void putText(String key, String text, float x, float y)
    // {
    //     putText(key, text, x, y, defaultPaintMsg);
    // }
    //
    // public void putText(String key, String text, float x, float y, Paint paint)
    // {
    //     MsgInfo msg = new MsgInfo(text, x, y, paint);
    //     msgMap.put(key, msg);
    // }
    //
    // public void removeText(String key)
    // {
    //     msgMap.remove(key);
    // }
    //
    // public void clearText()
    // {
    //     msgMap.clear();
    // }
    //
    //
    // /*
    //  * all:
    //  */
    // public void clearAll()
    // {
    //     clearA();
    //     clearLine();
    //     clearCircle();
    //     clearRect();
    //     clearText();
    // }
    //
    // public float getRotate()
    // {
    //     return rotate_degrees;
    // }
    //
    // public void setRotate(float degrees)
    // {
    //     rotate_degrees = degrees;
    // }
    //
    //
    // /*
    //  * 绘图
    //  */
    //
    // private void drawOnScreenImpl(Canvas canvas)
    // {
    //     canvas.drawColor(Color.TRANSPARENT);
    //     // canvas.rotate(rotate_degrees);
    //
    //     //通用
    //     for (IUserDrawAction action : actionMap.values())
    //     {
    //         if (action != null)
    //         {
    //             action.drawOn(canvas);
    //         }
    //     }
    //     //线
    //     for (LineInfo line : lineMap.values())
    //     {
    //         canvas.drawLine(line.startX, line.startY, line.stopX, line.stopY, line.paint);
    //     }
    //     //圆形
    //     for (CircleInfo circle : circleMap.values())
    //     {
    //         canvas.drawCircle(circle.x, circle.y, circle.radius, circle.paint);
    //     }
    //     //矩形
    //     for (RectangleInfo rect : rectMap.values())
    //     {
    //         canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, rect.paint);
    //     }
    //     //文本
    //     for (MsgInfo msg : msgMap.values())
    //     {
    //         canvas.drawText(msg.text, msg.x, msg.y, msg.paint);
    //     }
    // }


    /*
     * 方便的类
     */

    public static class BrushBase
    {
        public Paint paint;

        public BrushBase()
        {
            this.paint = new Paint();
            this.paint.setAntiAlias(true);
            this.paint.setColor(Color.BLACK);
        }


        public BrushBase setAntiAlias(boolean aa)
        {
            this.paint.setAntiAlias(aa);
            return this;
        }

        public BrushBase setColor(int color)
        {
            this.paint.setColor(color);
            return this;
        }

        public BrushBase setStyle(Paint.Style style)
        {
            this.paint.setStyle(style);
            return this;
        }

        public BrushBase setStrokeWidth(float width)
        {
            this.paint.setStrokeWidth(width);
            return this;
        }
    }

    public static class BrushLine extends BrushBase
    {
        public BrushLine()
        {
            super();
        }
    }


    public static class BrushCircle extends BrushBase
    {
        public BrushCircle()
        {
            super();
            this.paint.setStyle(Paint.Style.STROKE);
        }

        public BrushCircle makeFill(boolean fill)
        {
            if (fill)
            {
                this.paint.setStyle(Paint.Style.FILL);
            }
            else
            {
                this.paint.setStyle(Paint.Style.STROKE);
            }
            return this;
        }
    }

    public static class BrushRect extends BrushBase
    {
        public BrushRect()
        {
            super();
            this.paint.setStyle(Paint.Style.STROKE);
        }

        public BrushRect makeFill(boolean fill)
        {
            if (fill)
            {
                this.paint.setStyle(Paint.Style.FILL);
            }
            else
            {
                this.paint.setStyle(Paint.Style.STROKE);
            }
            return this;
        }

        //设置为虚线样式
        public BrushRect makeDash()
        {
            paint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
            return this;
        }
    }


    public static class BrushText extends BrushBase
    {
        public BrushText()
        {
            super();
        }

        public BrushText setTextSize(float size)
        {
            paint.setTextSize(size);
            return this;
        }
    }


    /*
     * 存储信息
     */

    // protected class LineInfo
    // {
    //     public float startX = 0;
    //     public float startY = 0;
    //     public float stopX = 0;
    //     public float stopY = 0;
    //     public Paint paint = null;
    //
    //     public LineInfo(float startX, float startY, float stopX, float stopY, Paint paint)
    //     {
    //         this.startX = startX;
    //         this.startY = startY;
    //         this.stopX = stopX;
    //         this.stopY = stopY;
    //         this.paint = paint;
    //     }
    //
    //     public void move(float step)
    //     {
    //         startX += step;
    //         startY += step;
    //         stopX += step;
    //         stopY += step;
    //     }
    // }
    //
    // protected class CircleInfo
    // {
    //     public float x = 0;
    //     public float y = 0;
    //     public float radius = 0;
    //     public Paint paint = null;
    //
    //     public CircleInfo(float x, float y, float radius, Paint paint)
    //     {
    //         this.x = x;
    //         this.y = y;
    //         this.radius = radius;
    //         this.paint = paint;
    //     }
    //
    //     public void move(float step)
    //     {
    //         x += step;
    //         y += step;
    //     }
    // }
    //
    // protected class RectangleInfo
    // {
    //     public float left = 0;
    //     public float top = 0;
    //     public float right = 0;
    //     public float bottom = 0;
    //     public Paint paint = null;
    //
    //     public RectangleInfo(float left, float top, float right, float bottom, Paint paint)
    //     {
    //         this.left = left;
    //         this.top = top;
    //         this.right = right;
    //         this.bottom = bottom;
    //         this.paint = paint;
    //     }
    //
    //     public void move(float step)
    //     {
    //         left += step;
    //         top += step;
    //         right += step;
    //         bottom += step;
    //     }
    // }
    //
    // protected class MsgInfo
    // {
    //     public String text = null;
    //     public float x = 0;
    //     public float y = 0;
    //     public Paint paint = null;
    //
    //     public MsgInfo(String text, float x, float y, Paint paint)
    //     {
    //         this.text = text;
    //         this.x = x;
    //         this.y = y;
    //         this.paint = paint;
    //     }
    //
    //     public void move(float step)
    //     {
    //         x += step;
    //         y += step;
    //     }
    // }

}

