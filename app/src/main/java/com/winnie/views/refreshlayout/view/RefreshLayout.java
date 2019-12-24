package com.winnie.views.refreshlayout.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;

import com.winnie.views.refreshlayout.constant.RefreshMode;
import com.winnie.views.refreshlayout.constant.RefreshState;


/**
 * @author : winnie
 * @date : 2019/12/23
 * @desc 下拉刷新布局
 */
@SuppressWarnings("unused")
public class RefreshLayout extends LinearLayout {
    public static final int SMOOTH_SCROLL_DURATION_MS = 200;
    private final static float FRICTION = 2.0f;

    private RefreshHeaderView mHeaderView;
    private NestedScrollView mContentView;

    /**
     * 最小滑动阈值
     */
    private int mTouchSlop;

    /**
     * 一次touch中，最新的X,Y坐标值
     */
    private float mLastMotionPix, mLastMotionPiy;
    /**
     * 一次touch中，Action_Down时的原始X,Y坐标值
     */
    private float mInitialMotionPix, mInitialMotionPiy;


    /**
     * View被拖拽的标识
     */
    private boolean mIsBeingDragged = false;

    private RefreshState mState = RefreshState.getDefault();
    private RefreshMode mMode = RefreshMode.getDefault();

    private OnRefreshListener mOnRefreshListener;
    private OnPullEventListener mOnPullEventListener;
    private OnPullDownListener mDownListener;

    private Interpolator mScrollAnimationInterpolator;
    private SmoothScrollRunnable mCurrentSmoothScrollRunnable;

    public RefreshLayout(Context context) {
        super(context);
        initView(context);
    }

    public RefreshLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public RefreshLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        setOrientation(LinearLayout.VERTICAL);

        ViewConfiguration config = ViewConfiguration.get(context);
        mTouchSlop = config.getScaledTouchSlop();

        mContentView = createContentView(context);
        addViewInternal(mContentView, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mHeaderView = createLoadingView(context);

        updateHeaderFormMode();

        if(isInEditMode()) {
            refreshHeaderViewsSize();
        }
    }

    private NestedScrollView createContentView(Context context) {
        return new NestedScrollView(context);
    }

    private RefreshHeaderView createLoadingView(Context context) {
        RefreshHeaderView headerView = new RefreshHeaderView(context);
        headerView.setVisibility(View.VISIBLE);
        return headerView;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        post(() -> {
            refreshHeaderViewsSize();
            requestLayout();
        });
    }

    /**
     * 把headerView隐藏起来，其实用的是padding的方式 设置为负值 就到屏幕顶部的外面了
     */
    private void refreshHeaderViewsSize() {
        final int maximumPullScroll = (int) (getHeaderSize() * 1f);
        if (mMode == RefreshMode.PULL_FROM_START) {
            LayoutParams lp = (LayoutParams) mHeaderView.getLayoutParams();
            int pLeft = lp.leftMargin;
            int pTop = -maximumPullScroll;
            int pRight = lp.rightMargin;
            int pBottom = lp.bottomMargin;

            lp.height = maximumPullScroll;
            lp.setMargins(pLeft, pTop, pRight, pBottom);
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        mContentView.addView(child, index, params);
    }

    private int getHeaderSize() {
        //使用measure(0, 0)测量之后，获取的是头部内容部分的宽高
        mHeaderView.measure(0, 0);
        return mHeaderView.getMeasuredHeight();
    }

    /**
     * 最大下拉值
     */
    private int getMaxPullScroll() {
        return Math.round(getHeight() / FRICTION);
    }

    /**
     * 使用addViewInternal()是因为 addView()被我重载了
     */
    private void addViewInternal(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
    }

    private void addViewInternal(View child, ViewGroup.LayoutParams params) {
        super.addView(child, -1, params);
    }

    private void updateHeaderFormMode() {
        final LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        if (this == mHeaderView.getParent()) {
            removeView(mHeaderView);
        }
        if (mMode == RefreshMode.PULL_FROM_START) {
            addViewInternal(mHeaderView, 0, params);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!isPullToRefreshEnabled()) {
            return false;
        }

        final int action = event.getAction();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mIsBeingDragged = false;
            return false;
        }

        if (action != MotionEvent.ACTION_DOWN && mIsBeingDragged) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                if (isReadyForPull()) {
                    mInitialMotionPix = mLastMotionPix = event.getX();
                    mInitialMotionPiy = mLastMotionPiy = event.getY();
                    mIsBeingDragged = false;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (isRefreshing()) {
                    return true;
                }
                if (isReadyForPull()) {
                    float x = event.getX();
                    float y = event.getY();

                    float diffPix = x - mLastMotionPix;
                    float diffPiy = y - mLastMotionPiy;

                    float absDiffPix = Math.abs(diffPix);
                    float absDiffPiy = Math.abs(diffPiy);
                    //判断手势X轴和Y轴的位移差值，Y轴位移大才当做上下滑动处理
                    if (absDiffPiy > absDiffPix && absDiffPiy > mTouchSlop) {
                        //只处理下滑事件
                        if (diffPiy > 0) {
                            mLastMotionPix = x;
                            mLastMotionPiy = y;
                            mIsBeingDragged = true;
                        }
                    }
                }
                break;
            }
            default:
                break;
        }
        return mIsBeingDragged;
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isPullToRefreshEnabled()) {
            return false;
        }

        if (isRefreshing()) {
            return true;
        }

        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                if (isReadyForPull()) {
                    mLastMotionPix = mInitialMotionPix = event.getX();
                    mLastMotionPiy = mInitialMotionPiy = event.getY();
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mIsBeingDragged) {
                    mLastMotionPix = event.getX();
                    mLastMotionPiy = event.getY();
                    //开始下拉，移动
                    pullEvent();
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (mIsBeingDragged) {
                    mIsBeingDragged = false;
                    if (mState == RefreshState.RELEASE_TO_REFRESH && (null != mOnRefreshListener)) {
                        //放下手指开始回调，执行我们的回调任务
                        setState(RefreshState.REFRESHING);
                        return true;
                    }

                    if (isRefreshing()) {
                        smoothScrollTo(0);
                        return true;
                    }
                    //恢复到原来的UI状态
                    setState(RefreshState.RESET);
                    return true;
                }
                break;
            }
            default:
                break;
        }
        return false;
    }

    private void pullEvent() {
        float initialMotionValue = mInitialMotionPiy;
        float lastMotionValue = mLastMotionPiy;
        int newScrollValue = Math.round(Math.min(initialMotionValue - lastMotionValue, 0) / FRICTION);
        int itemDimension = getHeaderSize();

        setHeaderScroll(newScrollValue);
        if (newScrollValue != 0 && !isRefreshing()) {
            float scale = Math.abs(newScrollValue) / (float) itemDimension;
            mHeaderView.onPull(scale);
        }

        //更新状态 释放按下触摸 与 没释放手的触摸
        if (mState != RefreshState.PULL_TO_REFRESH && itemDimension >= Math.abs(newScrollValue)) {
            setState(RefreshState.PULL_TO_REFRESH);
        } else if (mState == RefreshState.PULL_TO_REFRESH && itemDimension < Math.abs(newScrollValue)) {
            //下拉松手 可以松手了
            setState(RefreshState.RELEASE_TO_REFRESH);
        }
    }

    private void setHeaderScroll(int value) {
        if (mDownListener != null) {
            mDownListener.onPullDown(value);
        }

        int maxPullScroll = getMaxPullScroll();
        value = Math.max(-maxPullScroll, value);
        if (value < 0) {
            mHeaderView.setVisibility(View.VISIBLE);
        } else {
            mHeaderView.setVisibility(View.INVISIBLE);
        }
        scrollTo(0, value);
    }

    private boolean isPullToRefreshEnabled() {
        return mMode == RefreshMode.PULL_FROM_START;
    }

    private boolean isRefreshing() {
        return mState == RefreshState.REFRESHING || mState == RefreshState.MANUAL_REFRESHING;
    }

    private boolean isReadyForPull() {
        if (mMode == RefreshMode.PULL_FROM_START) {
            return mContentView.getScrollY() == 0;
        }
        return false;
    }

    /**
     * 手动置View为刷新状态
     */
    public final void setRefreshing() {
        if (!isRefreshing()) {
            setState(RefreshState.MANUAL_REFRESHING);
        }
    }

    final void setState(RefreshState state) {
        mState = state;
        switch (mState) {
            case RESET:
                onReset();
                break;
            case PULL_TO_REFRESH:
                onPullToRefresh();
                break;
            case RELEASE_TO_REFRESH:
                onReleaseToRefresh();
                break;
            case REFRESHING:
            case MANUAL_REFRESHING:
                onRefreshing();
                break;
            case OVER_SCROLLING:
            default:
                break;
        }

        if (null != mOnPullEventListener) {
            mOnPullEventListener.onPullEvent(mState, mMode);
        }
    }

    private void onReset() {
        mIsBeingDragged = false;
        mHeaderView.reset();
        smoothScrollTo(0);
    }

    private void onPullToRefresh() {
        mHeaderView.pullToRefresh();
    }

    private void onReleaseToRefresh() {
        mHeaderView.releaseToRefresh();
    }

    private void onRefreshing() {
        mHeaderView.refreshing();
        smoothScrollTo(-getHeaderSize(), () -> {
            if (null != mOnRefreshListener) {
                mOnRefreshListener.onRefresh(RefreshLayout.this);
            }
        });
    }

    public void onRefreshComplete() {
        if (isRefreshing()) {
            setState(RefreshState.RESET);
        }
        if (mMode == RefreshMode.MANUAL_REFRESH_ONLY) {
            mMode = RefreshMode.PULL_FROM_START;
            updateHeaderFormMode();
        }
    }


    protected final void smoothScrollTo(int scrollValue) {
        smoothScrollTo(scrollValue, SMOOTH_SCROLL_DURATION_MS);
    }

    private void smoothScrollTo(int scrollValue, long duration) {
        smoothScrollTo(scrollValue, duration, 0, null);
    }

    protected final void smoothScrollTo(int scrollValue, OnSmoothScrollFinishedListener listener) {
        smoothScrollTo(scrollValue, SMOOTH_SCROLL_DURATION_MS, 0, listener);
    }

    private void smoothScrollTo(int newScrollValue, long duration, long delayMillis,
                                OnSmoothScrollFinishedListener listener) {
        if (null != mCurrentSmoothScrollRunnable) {
            mCurrentSmoothScrollRunnable.stop();
        }

        final int oldScrollValue;
        oldScrollValue = getScrollY();

        if (oldScrollValue != newScrollValue) {
            if (null == mScrollAnimationInterpolator) {
                mScrollAnimationInterpolator = new DecelerateInterpolator();
            }
            mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(
                    oldScrollValue, newScrollValue, duration, listener);

            if (delayMillis > 0) {
                postDelayed(mCurrentSmoothScrollRunnable, delayMillis);
            } else {
                post(mCurrentSmoothScrollRunnable);
            }
        }
    }

    final class SmoothScrollRunnable implements Runnable {
        private final Interpolator mInterpolator;
        private final int mScrollToPiy;
        private final int mScrollFromPiy;
        private final long mDuration;
        private OnSmoothScrollFinishedListener mListener;

        private boolean mContinueRunning = true;
        private long mStartTime = -1;
        private int mCurrentPiy = -1;

        SmoothScrollRunnable(int fromPiy, int toPiy, long duration, OnSmoothScrollFinishedListener listener) {
            mScrollFromPiy = fromPiy;
            mScrollToPiy = toPiy;
            mInterpolator = mScrollAnimationInterpolator;
            mDuration = duration;
            mListener = listener;
        }

        @Override
        public void run() {
            if (mStartTime == -1) {
                mStartTime = System.currentTimeMillis();
            } else {
                long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime)) / mDuration;
                normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

                final int deltaPiy = Math.round((mScrollFromPiy - mScrollToPiy) * mInterpolator.getInterpolation(normalizedTime / 1000f));
                mCurrentPiy = mScrollFromPiy - deltaPiy;
                setHeaderScroll(mCurrentPiy);
            }

            if (mContinueRunning && mScrollToPiy != mCurrentPiy) {
                ViewCompat.postOnAnimation(RefreshLayout.this, this);
            } else {
                if (null != mListener) {
                    mListener.onSmoothScrollFinished();
                }
            }
        }

        void stop() {
            mContinueRunning = false;
            removeCallbacks(this);
        }
    }

    /**
     * 内部滑动监听
     */
    interface OnSmoothScrollFinishedListener {
        void onSmoothScrollFinished();
    }

    /*-----------------------------------------------------------各种手势监听------------------------------------------------------------*/

    /**
     * 手动下拉监听
     */
    public interface OnPullDownListener {
        /**
         * @param distance 下拉距离
         */
        void onPullDown(int distance);
    }

    public void setOnPullDownListener(OnPullDownListener downListener) {
        this.mDownListener = downListener;
    }


    /**
     * 刷新监听
     */
    public interface OnRefreshListener {

        /**
         * 触发刷新
         *
         * @param layout layout
         */
        void onRefresh(RefreshLayout layout);
    }

    public final void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }


    /**
     * 拖动监听
     */
    public interface OnPullEventListener {

        /**
         * 拖动
         *
         * @param state     状态
         * @param direction 方向
         */
        void onPullEvent(RefreshState state, RefreshMode direction);
    }

    public void setOnPullEventListener(OnPullEventListener listener) {
        mOnPullEventListener = listener;
    }
}
