package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ArticleDetailActivity";

    private Cursor mCursor;
    private long mStartId;

    private ProgressBar mProgressBar;
    private NestedScrollView mNestedScrollView;
    private AppBarLayout mAppBarLayout;
    private FloatingActionButton mFloatingActionButton;
    private Toolbar mToolbar;
    private ImageView mPhotoView;
    private TextView mArticleTitle;
    private TextView mArticleInfo;
    private TextView mArticleBody;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        getLoaderManager().initLoader(0, null, this);

        bindViews();

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
            }
        }
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void hideProgress() {
        mProgressBar.setVisibility(View.GONE);
        mAppBarLayout.setVisibility(View.VISIBLE);
        mNestedScrollView.setVisibility(View.VISIBLE);
        mFloatingActionButton.setVisibility(View.VISIBLE);
    }

    private void bindViews() {
        mToolbar = findViewById(R.id.toolbar);
        mPhotoView = findViewById(R.id.thumbnail);

        mFloatingActionButton = findViewById(R.id.share_fab);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(ArticleDetailActivity.this)
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        mProgressBar = findViewById(R.id.progress);
        mAppBarLayout = findViewById(R.id.appbar);
        mNestedScrollView = findViewById(R.id.nested_scroll_view);

        mArticleTitle = findViewById(R.id.article_title);
        mArticleInfo = findViewById(R.id.article_info);
        mArticleBody = findViewById(R.id.article_body);
    }

    private void updateData() {
        if (mCursor != null) {
            mToolbar.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            Date publishedDate = parsePublishedDate();
            String date;
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                date = DateUtils.getRelativeTimeSpanString(
                        publishedDate.getTime(),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString();
            } else {
                // If date is before 1902, just show the string
                date = outputFormat.format(publishedDate);
            }
            mArticleTitle.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            mArticleInfo.setText(date + " by " + mCursor.getString(ArticleLoader.Query.AUTHOR));
            mArticleBody.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")));
            ImageLoaderHelper.getInstance(this).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });
        }
        hideProgress();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
            updateData();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        updateData();
    }
}
