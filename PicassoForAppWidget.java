package com.m039.base.library;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.widget.RemoteViews;
import com.m039.kula.tech.gwidget.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Created by G.Olis.
 */
public class PicassoForAppWidget {
    private Picasso mPic;
    private LruCache<String, Bitmap> mMemoryCache;
    private Context mContext;
    private String[] URLs;          // URLs massive.
    private int mFrontStockBitmaps; // This counter determine how kind of pages,
                                    // whose URLs in URLs massive are located
                                    // to the right relative to the current position,
                                    // not noticeable for the app widget user,
                                    // PicassoForAppWidgets will be downloaded and placed in a LRU cache.
    private int mWorkingLayout;
    private int mBackStockBitmaps;
    private Target mTarget;
    private int mWidgetHeight;
    private int mWidgetWidth;
    private Bitmap mPlaceHolder;
    private int mWidgetID;
    private int mCurrentPosition;
    private int mNumberOfLastItem;
    private int mTopCut;

    public PicassoForAppWidget(Context context, int mainLayout, String[] urls, int widgetID){
        mContext = context;
        mWorkingLayout = mainLayout;
        URLs = urls;
        mFrontStockBitmaps = 2;
        mBackStockBitmaps = 1;
        mCurrentPosition = 0;
        mNumberOfLastItem = URLs.length -1;
        mWidgetID = widgetID;
        mTopCut =mFrontStockBitmaps > mBackStockBitmaps?mFrontStockBitmaps:mBackStockBitmaps;
        mMemoryCache = new LruCache<String, Bitmap>(mFrontStockBitmaps + mBackStockBitmaps + 1);
        mWidgetHeight = (int) (mContext.getResources().getDisplayMetrics().density * 160f * 1);
        mWidgetWidth = (int) (mContext.getResources().getDisplayMetrics().density * 160f * 1);
        mPic = new Picasso.Builder(mContext).build();
    }
    public void setBackStockBitmaps(int backStockBitmaps){
        mBackStockBitmaps = backStockBitmaps;
        mMemoryCache = new LruCache<String, Bitmap>(mFrontStockBitmaps + mBackStockBitmaps + 1);
    }

    public void setFrontStockBitmaps(int frontStockBitmaps){
        mFrontStockBitmaps = frontStockBitmaps;
        mMemoryCache = new LruCache<String, Bitmap>(mFrontStockBitmaps + mBackStockBitmaps + 1);
    }

    public void setBitmapDimension(int quality){
        mWidgetHeight = (int) (mContext.getResources().getDisplayMetrics().density * 160f * quality);//quality equal 1 this means that bitmaps will be
                                                                                                     //resized to the size of the widget.
        mWidgetWidth = (int) (mContext.getResources().getDisplayMetrics().density * 160f * quality);
    }

    public void setPlaceHolder (Bitmap placeHolder){
        mPlaceHolder = placeHolder;
    }

    public void start(final int viewID){
        downloadBitmap(viewID,0);
    }

    public void nextPosition(final int viewID){
        if(mCurrentPosition < URLs.length-1){
            mCurrentPosition += 1;
            downloadBitmap(viewID,mCurrentPosition);
        }
    }

    public void previousPosition(final int viewID){
        if(mCurrentPosition > 0){
            mCurrentPosition -= 1;
            downloadBitmap(viewID,mCurrentPosition);
        }
    }

    public void customPosition(final int viewID, final int position){
        mCurrentPosition = position;
        downloadBitmap(viewID,position);
    }

    private void downloadBitmap(final int viewID, final int position){

        mTarget = new Target()
        {

            @Override
            public void onPrepareLoad(Drawable d) {
                RemoteViews rw = new RemoteViews(mContext.getPackageName(),mWorkingLayout);
                if(d == null)rw.setImageViewResource(viewID,R.color.transparent);
                else rw.setImageViewBitmap(viewID, mPlaceHolder);
                AppWidgetManager.getInstance(mContext).partiallyUpdateAppWidget(mWidgetID, rw);
            }

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                RemoteViews rw = new RemoteViews(mContext.getPackageName(),mWorkingLayout);
                rw.setImageViewBitmap(viewID, bitmap);
                AppWidgetManager.getInstance(mContext).partiallyUpdateAppWidget(mWidgetID, rw);
                addBitmapToMemoryCache(URLs[position],bitmap);
                shadowDownloadNextBitmap(position+1);
            }

            @Override
            public void onBitmapFailed(Drawable f){}

        };
       if(position <= mNumberOfLastItem) mPic.load(URLs[position])
                .skipMemoryCache()
                .resize(mWidgetWidth, mWidgetHeight)
                .centerCrop()
                .into(mTarget);
    }
    Target target;
    private void shadowDownloadNextBitmap(final int position){
        target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
               addBitmapToMemoryCache(URLs[position],bitmap);
               shadowDownloadPreviousBitmap(2*mCurrentPosition - position);
            }
            @Override
            public void onBitmapFailed(Drawable drawable) {}
            @Override
            public void onPrepareLoad(Drawable drawable) {}
        };
        if(position <= mNumberOfLastItem && (position - mCurrentPosition)<=mFrontStockBitmaps){
            mPic.load(URLs[position])
                    .skipMemoryCache()
                    .resize(mWidgetWidth, mWidgetHeight)
                    .centerCrop()
                    .into(target);
        }
        else if((position-mCurrentPosition)<=mTopCut){
            shadowDownloadPreviousBitmap(2*mCurrentPosition - position);
        }
    }
    private void shadowDownloadPreviousBitmap(final int position){
        target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
                addBitmapToMemoryCache(URLs[position],bitmap);
                shadowDownloadNextBitmap(2*mCurrentPosition-position+1);
            }
            @Override
            public void onBitmapFailed(Drawable drawable) {}
            @Override
            public void onPrepareLoad(Drawable drawable) {}
        };
        if(position > 0 && (mCurrentPosition-position)<=mBackStockBitmaps){
            mPic.load(URLs[position])
                    .skipMemoryCache()
                    .resize(mWidgetWidth, mWidgetHeight)
                    .centerCrop()
                    .into(target);
        }
        else if((mCurrentPosition-position)<=mTopCut){
            shadowDownloadNextBitmap(2*mCurrentPosition-position+1);
        }
    }
    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
           mMemoryCache.put(key, bitmap);
           android.util.Log.i("wid777", mMemoryCache.size() + "");
        }
    }

    private Bitmap getBitmapFromMemCache(String key) {
        return key != null? mMemoryCache.get(key):null;
    }
}
