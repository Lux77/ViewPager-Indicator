package com.crown.viewpagerindicator;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Crown on 2016/4/17.
 */
public class ViewPagerIndictor extends LinearLayout {

    private Paint mPaint;
    private Path mPath;

    private int mTriangleWidth;
    private int mTriangleHeight;

    //初始状态下三角形的偏移位置
    private int mInitTranslationX;
    //移动时
    private int mTranslationX;
    //可见tab的数量
    private int mTabVisibleCount;

    //三角形底边宽度与tab宽度的比例
    private static final float RADIO_TRIANGLE_WIDTH = 1/6f;
    //三角形的最大宽度 底边
    private final int DIMENSION_TRIANGLE_WIDTH_MAX = (int)(getScreenWidth() / 3 * RADIO_TRIANGLE_WIDTH);
    private static final int COUNT_DEFAULT_TAB = 2;
    private static final int COLOR_TEXT_NORMAL = 0x77FFFFFF;
    private static final int COLOR_TEXT_HIGHLIGHT = 0xFFFFFFFF;

    public List<String> mTitles;

    private ViewPager mViewPager;

    public ViewPagerIndictor(Context context) {
        super(context, null);
    }

    public ViewPagerIndictor(Context context, AttributeSet attrs) {
        super(context, attrs);
        //获取可见tab的数量 自定义的属性
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ViewPagerIndicator);
        mTabVisibleCount = array.getInt(R.styleable.ViewPagerIndicator_visible_tab_count, COUNT_DEFAULT_TAB);
        if(mTabVisibleCount < 0) {
            mTabVisibleCount = COUNT_DEFAULT_TAB;
        }
        array.recycle();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.parseColor("#FFFFFFFF"));
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setPathEffect(new CornerPathEffect(3));
    }

    public ViewPagerIndictor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 绘制三角形
     * @param canvas
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        //平移
        canvas.translate(mInitTranslationX+mTranslationX, getHeight()+2);
        canvas.drawPath(mPath, mPaint);

        canvas.restore();
        super.dispatchDraw(canvas);
    }

    /**
     * 空间的宽高发生变化时 回调该方法
     * @param w
     * @param h
     * @param oldw
     * @param oldh
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //三角形的底边宽度
        mTriangleWidth = (int)(w / mTabVisibleCount * RADIO_TRIANGLE_WIDTH);
        //mTriangleWidth = Math.min(mTriangleWidth, DIMENSION_TRIANGLE_WIDTH_MAX);
        mInitTranslationX = w/mTabVisibleCount/2-mTriangleWidth/2;

        initTriangle();
    }

    /**
     * xml布局加载完成后回调
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        int cCount = getChildCount();
        if(cCount == 0) {
            return ;
        }
        for (int i = 0; i < cCount; i++) {
            View view = getChildAt(i);
            LinearLayout.LayoutParams lp = (LayoutParams) view.getLayoutParams();
            lp.weight = 0;
            lp.width = getScreenWidth() / mTabVisibleCount;
            view.setLayoutParams(lp);
        }
        setItemClickEvent();
    }

    private int getScreenWidth() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    /**
     * 初始化三角形
     */
    private void initTriangle() {
        mTriangleHeight = mTriangleWidth / mTabVisibleCount;
        mPath = new Path();
        mPath.moveTo(0, 0);
        mPath.lineTo(mTriangleWidth, 0);//画三角形底边
        mPath.lineTo(mTriangleWidth/2, -mTriangleHeight);//三角形右边的侧边
        mPath.close(); //闭合三角形
    }

    /**
     * 指示器跟随手指移动
     */

    public void scroll(int position, float offset) {
        int tabWidth = getWidth() / mTabVisibleCount;
        mTranslationX = (int)(tabWidth * (offset + position));

        //容器移动 当tab移动至最后一个时
        if(position >= (mTabVisibleCount - 2) && offset > 0 && getChildCount() > mTabVisibleCount) {
            if(mTabVisibleCount != 1) {
                this.scrollTo((position - (mTabVisibleCount - 2)) * tabWidth + (int) (tabWidth * offset), 0);
            }
            else {
                this.scrollTo(position * tabWidth + (int)(tabWidth * offset), 0);
            }
        }

        invalidate();
    }

    public void setTabItemTitles(List<String> titles) {
        if(titles != null && titles.size() > 0) {
            this.removeAllViews();
            mTitles = titles;
            for(String title : mTitles) {
                addView(generateTextView(title));
            }
        }
        setItemClickEvent();
    }

    public void setVisibleTabCount(int count) {
        mTabVisibleCount = count;
    }

    private View generateTextView(String title) {
        TextView textView = new TextView(getContext());
        LinearLayout.LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        lp.width = getScreenWidth() / mTabVisibleCount;
        textView.setText(title);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        textView.setTextColor(COLOR_TEXT_NORMAL);
        textView.setLayoutParams(lp);
        return textView;
    }

    public interface PageOnChangedListener {
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels);
        public void onPageSelected(int position);
        public void onPageScrollStateChanged(int state);
    }

    private PageOnChangedListener mListener;
    public void setOnPageChangedListener(PageOnChangedListener listener) {
        mListener = listener;
    }

    public void setViewPager(ViewPager viewpager, int position) {
        mViewPager = viewpager;
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //tabWidth * positionOffset + position * tabWidth
                scroll(position, positionOffset);
                if(mListener != null) {
                    mListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
                }
            }

            @Override
            public void onPageSelected(int position) {
                if(mListener != null) {
                    mListener.onPageSelected(position);
                }
                highLightTextView(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if(mListener != null) {
                    mListener.onPageScrollStateChanged(state);
                }
            }
        });
        mViewPager.setCurrentItem(position);
        highLightTextView(position);
    }

    public void resetTextViewColor() {
        for(int i= 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if(view instanceof TextView) {
                ((TextView) view).setTextColor(COLOR_TEXT_NORMAL);
            }
        }
    }

    /**
     * 高亮Tab的文本
     * @param position
     */
    public void highLightTextView(int position) {
        resetTextViewColor();
        View view = getChildAt(position);
        if(view instanceof TextView) {
            ((TextView) view).setTextColor(COLOR_TEXT_HIGHLIGHT);
        }
    }

    /**
     * 设置tab的点击事件
     */
    public void setItemClickEvent() {
        int cCount = getChildCount();
        for(int i = 0; i < cCount; i++) {
            final int j = i;
            View view = getChildAt(i);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mViewPager.setCurrentItem(j);
                }
            });
        }
    }
}
