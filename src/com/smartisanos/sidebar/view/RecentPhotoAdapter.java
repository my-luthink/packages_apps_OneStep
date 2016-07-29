package com.smartisanos.sidebar.view;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.smartisanos.sidebar.R;
import com.smartisanos.sidebar.util.BitmapCache;
import com.smartisanos.sidebar.util.BitmapUtils;
import com.smartisanos.sidebar.util.IEmpty;
import com.smartisanos.sidebar.util.ImageInfo;
import com.smartisanos.sidebar.util.ImageLoader;
import com.smartisanos.sidebar.util.LOG;
import com.smartisanos.sidebar.util.RecentPhotoManager;
import com.smartisanos.sidebar.util.RecentUpdateListener;
import com.smartisanos.sidebar.util.Utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.drawable.BitmapDrawable;

import smartisanos.util.SidebarUtils;

public class RecentPhotoAdapter extends BaseAdapter {
    private static final LOG log = LOG.getInstance(RecentPhotoAdapter.class);

    private Context mContext;
    private RecentPhotoManager mPhotoManager;
    private List<ImageInfo> mList = new ArrayList<ImageInfo>();

    private ImageLoader mImageLoader;
    private Handler mHandler;
    private View mOpenGalleryView;
    private IEmpty mEmpty;
    public RecentPhotoAdapter(Context context, IEmpty empty) {
        mContext = context;
        mEmpty = empty;
        mHandler = new Handler(Looper.getMainLooper());
        mPhotoManager = RecentPhotoManager.getInstance(mContext);
        int maxPhotoSize = mContext.getResources().getDimensionPixelSize(R.dimen.recent_photo_size);
        mImageLoader = new ImageLoader(maxPhotoSize);
        mList = mPhotoManager.getImageList();
        mPhotoManager.addListener(new RecentUpdateListener() {
            @Override
            public void onUpdate() {
                mHandler.post(new Runnable(){
                    @Override
                    public void run() {
                        mList = mPhotoManager.getImageList();
                        notifyDataSetChanged();
                    }
                });
            }
        });
        notifyEmpty();
    }

    public void updateUI(){
        TextView text = (TextView) getOpenGalleryView().findViewById(R.id.text);
        text.setText(R.string.open_gallery);
    }

    private View getOpenGalleryView() {
        if (mOpenGalleryView == null) {
            mOpenGalleryView = LayoutInflater.from(mContext).inflate(R.layout.open_gallery_item, null);
            mOpenGalleryView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.setPackage("com.android.gallery3d");
                        intent.putExtra("package_name", "com.smartisanos.sidebar");
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                        Utils.dismissAllDialog(mContext);
                    } catch (ActivityNotFoundException e) {
                        // NA
                    }
                }
            });
        }
        return mOpenGalleryView;
    }

    private void notifyEmpty() {
        if (mEmpty != null) {
            mEmpty.setEmpty(mList.size() == 0);
        }
    }

    @Override
    public void notifyDataSetChanged() {
        notifyEmpty();
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mList.size() + 1;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView,
            ViewGroup parent) {
        if(position == 0){
            return getOpenGalleryView();
        }

        final ImageInfo ii = mList.get(position - 1);
        View ret = null;
        final ImageView iv;
        if (convertView != null && convertView.getTag() != null) {
            ret = convertView;
            iv = (ImageView) convertView.getTag();
        } else {
            ret = LayoutInflater.from(mContext).inflate(R.layout.recentphotoitem, null);
            iv = (ImageView) ret.findViewById(R.id.image);
            ret.setTag(iv);
        }
        iv.setTag(ii.filePath);
        mImageLoader.loadImage(ii.filePath, iv, new ImageLoader.Callback() {
            @Override
            public void onLoadComplete(final Bitmap bitmap) {
                if (bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
                    return;
                }
                iv.post(new Runnable() {
                    @Override
                    public void run() {
                        if (ii.filePath != null && ii.filePath.equals(iv.getTag())) {
                            Drawable oldBg = iv.getBackground();
                            Drawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);
                            if (drawable.getIntrinsicWidth() > 0 && drawable.getIntrinsicHeight() > 0) {
                                Bitmap bmp = BitmapUtils.drawableToBitmap(drawable);
                                iv.setBackground(new BitmapDrawable(mContext.getResources(), bmp));
                                iv.setImageBitmap(bmp);
                            }
                            if (oldBg != null) {
                                if (oldBg instanceof BitmapDrawable) {
                                    ((BitmapDrawable) oldBg).getBitmap().recycle();
                                }
                            }
                        }
                    }
                });
            }
        });

        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.dismissAllDialog(mContext);
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setPackage("com.android.gallery3d");
                    intent.putExtra("package_name", "com.smartisanos.sidebar");
                    intent.setDataAndType(ii.getContentUri(mContext), ii.mimeType);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    // NA
                }
            }
        });

        iv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                SidebarUtils.dragImage(v, mContext, new File(ii.filePath), ii.mimeType);
                return true;
            }
        });
        return ret;
    }

    public void clearCache() {
        if (mImageLoader != null) {
            mImageLoader.clearCache();
        }
    }
}