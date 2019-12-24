package com.winnie.views.refreshlayout.constant;

/**
 * @author : winnie
 * @date : 2019/12/23
 * @desc
 */
public enum RefreshMode {
    /**
     *禁用下拉刷新
     */
    DISABLED(0x0),

    /**
     *下拉刷新
     */
    PULL_FROM_START(0x1),

    /**
     * 调用 setRefreshing()方法
     * {@link com.winnie.views.refreshlayout.view.RefreshLayout#setRefreshing()}方法
     */
    MANUAL_REFRESH_ONLY(0x4);


    private int mIntValue;

    RefreshMode(int mode) {
        mIntValue = mode;
    }

    public static RefreshMode getDefault(){
        return PULL_FROM_START;
    }

    public int getIntValue() {
        return mIntValue;
    }
}
