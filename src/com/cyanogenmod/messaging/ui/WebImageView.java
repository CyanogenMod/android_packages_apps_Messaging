/*
* Copyright (C) 2015 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.cyanogenmod.messaging.ui;

import android.annotation.Nullable;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.messaging.util.ImageUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

/**
 * <pre>
 *     Logic for fetching images from web sites
 * </pre>
 *
 * @see {@link ImageView}
 */
public class WebImageView extends ImageView {

    private BitmapImageViewTarget mBitmapImageViewTarget;

    public WebImageView(Context context) {
        super(context);
    }

    public WebImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WebImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WebImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    //------ Interface methods
    public void setImageUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        mBitmapImageViewTarget = new Bullseye(this);

        Glide.with(getContext())
                .load(url)
                .asBitmap()
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .into(mBitmapImageViewTarget);
    }

    private static class Bullseye extends BitmapImageViewTarget {

        public Bullseye(ImageView view) {
            super(view);
        }

        @Override
        protected void setResource(Bitmap src) {
            Bitmap tgt = Bitmap.createBitmap(
                    src.getWidth(), src.getHeight(), Config.ARGB_8888);
            // Reusing rects or target bitmaps for drawing might be a possibility
            // but this should only happen once per url as long as it is in the cache
            int w = src.getWidth();
            int h = src.getHeight();
            w = (w > 300) ? w = 300 : w;
            h = (h > 300) ? h = 300 : h;
            RectF dest = new RectF(0, 0, w, h);
            ImageUtils.drawBitmapWithCircleOnCanvas(src, new Canvas(tgt), dest, dest,
                    null, false, 0, 0);
            this.view.setImageBitmap(tgt);
        }

    }

}
