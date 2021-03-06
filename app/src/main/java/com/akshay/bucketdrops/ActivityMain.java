package com.akshay.bucketdrops;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.akshay.bucketdrops.Adapters.AdapterDrops;
import com.akshay.bucketdrops.Adapters.AddListener;
import com.akshay.bucketdrops.Adapters.CompleteListener;
import com.akshay.bucketdrops.Adapters.Divider;
import com.akshay.bucketdrops.Adapters.Filter;
import com.akshay.bucketdrops.Adapters.MarkListener;
import com.akshay.bucketdrops.Adapters.ResetListener;
import com.akshay.bucketdrops.Adapters.SimpleTouchCallback;
import com.akshay.bucketdrops.Extras.Util;
import com.akshay.bucketdrops.beans.Drop;
import com.akshay.bucketdrops.widgets.BucketRecyclerView;
import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;


public class ActivityMain extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.btn_add)
    Button mBtnAdd;

    @BindView(R.id.rv_drops)
    BucketRecyclerView mRecycler;

    Realm mRealm;
    RealmResults<Drop> mResults;

    @BindView(R.id.empty_drops)
    View mEmptyView;

    @BindView(R.id.iv_background)
    ImageView background;

    AdapterDrops mAdapter;

    private View.OnClickListener mBtnAddListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showDialogAdd();
        }
    };
    private AddListener mAddListener = new AddListener() {
        @Override
        public void add() {
            showDialogAdd();
        }
    };
    private RealmChangeListener mChangeListener = new RealmChangeListener() {
        @Override
        public void onChange(Object element) {
            mAdapter.update(mResults);
        }
    };
    private MarkListener mMarkListener = new MarkListener() {
        @Override
        public void onMark(int position) {
            showDialogMark(position);
        }
    };
    private CompleteListener mCompleteListener = new CompleteListener() {
        @Override
        public void onComplete(int position) {
            mAdapter.markComplete(position);
        }
    };
    private ResetListener mResetListener = new ResetListener() {
        @Override
        public void onReset() {
            AppBucketDrops.save(ActivityMain.this, Filter.NONE);
            loadResults(Filter.NONE);
        }
    };
    private void showDialogAdd() {
        DialogAdd dialog = new DialogAdd();
        dialog.show(getSupportFragmentManager(), "Add");
    }
    private void showDialogMark(int position) {
        DialogMark dialog = new DialogMark();
        Bundle bundle = new Bundle();
        bundle.putInt("POSITION", position);
        dialog.setArguments(bundle);
        dialog.setCompleteListener(mCompleteListener);
        dialog.show(getSupportFragmentManager(), "Mark");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mRealm = Realm.getDefaultInstance();
        setSupportActionBar(mToolbar);
        mBtnAdd.setOnClickListener(mBtnAddListener);
        int filterOption = AppBucketDrops.load(this);
        loadResults(filterOption);
        mRecycler.addItemDecoration(new Divider(this, LinearLayoutManager.VERTICAL));
        mRecycler.setItemAnimator(new DefaultItemAnimator());
        mRecycler.hideIfEmpty(mToolbar);
        mRecycler.showIfEmpty(mEmptyView);
        mAdapter = new AdapterDrops(this, mRealm, mResults, mAddListener, mMarkListener, mResetListener);
        mAdapter.setHasStableIds(true);
        mRecycler.setAdapter(mAdapter);
        SimpleTouchCallback callback = new SimpleTouchCallback(mAdapter);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(mRecycler);
        initBackgroundImage();
        Util.scheduleAlarm(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        boolean handled = true;
        int filterOption = Filter.NONE;
        switch (id) {
            case R.id.action_add:
                showDialogAdd();
                break;
            case R.id.action_sort_none:
                filterOption = Filter.NONE;
                break;
            case R.id.action_sort_ascending_date:
                filterOption = Filter.LEAST_TIME_LEFT;
                break;
            case R.id.action_sort_descending_date:
                filterOption = Filter.MOST_TIME_LEFT;
                break;
            case R.id.action_show_complete:
                filterOption = Filter.COMPLETE;
                break;
            case R.id.action_show_incomplete:
                filterOption = Filter.INCOMPLETE;
                break;
            default:
                handled = false;
                break;
        }
        loadResults(filterOption);
        AppBucketDrops.save(this, filterOption);
        return handled;
    }

    private void loadResults(int filterOption) {
        switch (filterOption) {
            case Filter.NONE:
                mResults = mRealm.where(Drop.class).findAllAsync();
                break;
            case Filter.LEAST_TIME_LEFT:
                mResults = mRealm.where(Drop.class).findAllSortedAsync("when");
                break;
            case Filter.MOST_TIME_LEFT:
                mResults = mRealm.where(Drop.class).findAllSortedAsync("when", Sort.DESCENDING);
                break;
            case Filter.COMPLETE:
                mResults = mRealm.where(Drop.class).equalTo("completed", true).findAllAsync();
                break;
            case Filter.INCOMPLETE:
                mResults = mRealm.where(Drop.class).equalTo("completed", false).findAllAsync();
                break;
        }
        mResults.addChangeListener(mChangeListener);
    }


    @Override
    protected void onStart() {
        super.onStart();
        mResults.addChangeListener(mChangeListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mResults.removeChangeListener(mChangeListener);
    }

    private void initBackgroundImage() {
        Glide.with(this)
                .load(R.drawable.background)
                .centerCrop()
                .into(background);
    }

}