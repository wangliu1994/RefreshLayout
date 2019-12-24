package com.winnie.views.refreshlayout;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.winnie.views.refreshlayout.view.RefreshLayout;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author winnie
 */
public class MainActivity extends AppCompatActivity {
    @BindView(R.id.refresh_layout)
    RefreshLayout mRefreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initView();
    }

    private void initView() {
        mRefreshLayout.setRefreshing();
        mRefreshLayout.setOnRefreshListener(layout -> mRefreshLayout.postDelayed(
                this::refreshData, 3000));
    }

    private void refreshData() {
        Toast.makeText(this, "刷新结束", Toast.LENGTH_SHORT).show();
        mRefreshLayout.onRefreshComplete();
    }
}
