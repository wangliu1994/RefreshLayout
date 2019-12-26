package com.winnie.views.refreshlayout.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.winnie.views.refreshlayout.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * @author : winnie
 * @date : 2019/12/24
 * @desc 下拉刷新Header
 */
public class RefreshHeaderView extends FrameLayout {
    private static final int ROTATION_ANIMATION_DURATION = 1200;
    private static String KEY_LAST_REFRESH_TIME = "KEY_LAST_REFRESH_TIME";

    private ImageView mImageProgress;
    private TextView mTextViewTitle;
    private TextView mTextViewTime;

    private CharSequence mPullLabel;
    private CharSequence mRefreshingLabel;
    private CharSequence mReleaseLabel;

    protected DateFormat mLastUpdateFormat;
    protected SharedPreferences mSharedPreferences;

    private static final Interpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();
    private Animation mRotateAnimation;

    public RefreshHeaderView(Context context) {
        super(context);
        initView(context);
    }

    public RefreshHeaderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public RefreshHeaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_refresh_header, this);

        mImageProgress = findViewById(R.id.iv_header_progress);
        mTextViewTitle = findViewById(R.id.tv_header_title);
        mTextViewTime = findViewById(R.id.tv_header_time);

        mPullLabel = context.getString(R.string.pull_to_refresh_pull_label);
        mRefreshingLabel = context.getString(R.string.pull_to_refresh_refreshing_label);
        mReleaseLabel = context.getString(R.string.pull_to_refresh_release_label);
        String refreshTimeLabel = context.getString(R.string.pull_to_refresh_time_label);

        mLastUpdateFormat = new SimpleDateFormat(refreshTimeLabel, Locale.getDefault());
        KEY_LAST_REFRESH_TIME += context.getClass().getName();
        mSharedPreferences = context.getSharedPreferences("RefreshHeader", Context.MODE_PRIVATE);
        setLastUpdateTime(mSharedPreferences.getLong(KEY_LAST_REFRESH_TIME, System.currentTimeMillis()));

        initProgress(context);

        reset();
    }

    private void initProgress(Context context) {
        Drawable imageDrawable = context.getResources().getDrawable(R.drawable.ic_waiting);
        if (null != imageDrawable) {
            mImageProgress.setImageDrawable(imageDrawable);
        }
        mRotateAnimation = new RotateAnimation(0, 720, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
        mRotateAnimation.setDuration(ROTATION_ANIMATION_DURATION);
        mRotateAnimation.setRepeatCount(Animation.INFINITE);
        mRotateAnimation.setRepeatMode(Animation.RESTART);
    }

    public void setLastUpdateTime(long time) {
        mTextViewTime.setText(mLastUpdateFormat.format(time));
        if (mSharedPreferences != null && !isInEditMode()) {
            mSharedPreferences.edit().putLong(KEY_LAST_REFRESH_TIME, time).apply();
        }
    }

    /**
     * 旋转左边的加载图片，显示文字和图片
     * @param scale 下拉百分比
     */
    public final void onPull(float scale) {
        float angle = scale * 90f;
        mImageProgress.setPivotX(mImageProgress.getWidth()/2);
        mImageProgress.setPivotY(mImageProgress.getHeight()/2);
        mImageProgress.setRotation(angle);
    }

    public void pullToRefresh() {
        mTextViewTitle.setText(mPullLabel);
    }

    public void releaseToRefresh() {
        mTextViewTitle.setText(mReleaseLabel);
    }

    public void refreshing() {
        mTextViewTitle.setText(mRefreshingLabel);
        mImageProgress.startAnimation(mRotateAnimation);
    }

    public void reset() {
        mTextViewTitle.setText(mPullLabel);
        mImageProgress.clearAnimation();

        setLastUpdateTime(System.currentTimeMillis());

        mImageProgress.setPivotX(mImageProgress.getWidth()/2);
        mImageProgress.setPivotY(mImageProgress.getHeight()/2);
        mImageProgress.setRotation(0);
    }
}
