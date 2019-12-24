package com.winnie.views.refreshlayout.constant;

/**
 * @author : winnie
 * @date : 2019/12/23
 * @desc
 */
public enum RefreshState {
    /**
     *初始状态
     */
    RESET(0x0),

    /**
     * 下拉中，下拉的距离还没达到刷新要求的值
     */
    PULL_TO_REFRESH(0x1),

    /**
     *下拉中，下拉的距离达到刷新要求的值，松开手既可以刷新
     */
    RELEASE_TO_REFRESH(0x2),

    /**
     * 正在刷新
     */
    REFRESHING(0x8),

    /**
     * 调用 setRefreshing()方法
     * {@link com.winnie.views.refreshlayout.view.RefreshLayout#setRefreshing()}方法
     */
    MANUAL_REFRESHING(0x9),

    /**
     * RefreshableView 调用fling()，，过度滚动
     */
    OVER_SCROLLING(0x10);


    private int mIntValue;

    RefreshState(int intValue) {
        mIntValue = intValue;
    }

    int getIntValue() {
        return mIntValue;
    }

    public static RefreshState getDefault() {
        return RESET;
    }
}
