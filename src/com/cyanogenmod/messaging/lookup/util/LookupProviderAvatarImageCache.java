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

package com.cyanogenmod.messaging.lookup.util;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <pre>
 *      Handle 3 level image caching for remote avatar images
 * </pre>
 */
public class LookupProviderAvatarImageCache {

    // Cache maps
    private static final ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftCache = new
            ConcurrentHashMap<String, SoftReference<Bitmap>>();
    private static final LinkedHashMap<String, Bitmap> sHardCache = new LinkedHashMap<String,
            Bitmap>(50, 0.75f, true) {
        @Override
        public boolean removeEldestEntry(Entry<String, Bitmap> entry) {
            return true;
        }
    };

    private static final Handler mUiHandler = new Handler(Looper.getMainLooper());

    private static ExecutorService sThreadPool;

    public static void addBitmap(String key, Bitmap bitmap) {
        if (!TextUtils.isEmpty(key) && bitmap != null) {
            // [TODO][MSB]: Check if we already have this in the hard cache and remove it, need
            // to update it

            if (sHardCache.containsKey(key)) {
                sHardCache.remove(key);
            }
            if (sSoftCache.containsKey(key)) {
                sSoftCache.remove(key);
            }

            sHardCache.put(key, bitmap);

        }
    }

    public static void initialize() {
        sThreadPool = Executors.newCachedThreadPool();
    }

    public static void terminate() {
        sThreadPool.shutdownNow();
    }

    public static void fetchBitmap(String key, LookupProviderAvatarImageCacheCallback callback) {
        if (!TextUtils.isEmpty(key) && callback != null) {
            sThreadPool.submit(new FetchRunnable(key, callback));
        }
    }

    private static class FetchRunnable implements Runnable {

        private String mKey;
        private LookupProviderAvatarImageCacheCallback mCallback;

        FetchRunnable(String key, LookupProviderAvatarImageCacheCallback callback) {
            mKey = key;
            mCallback = callback;
        }

        @Override
        public void run() {
            Bitmap bitmap = null;
            if (sHardCache.containsKey(mKey)) {
                bitmap = sHardCache.remove(mKey); // cycle it back to the front
                sHardCache.put(mKey, bitmap);
            } else if (sSoftCache.containsKey(mKey)) {
                SoftReference<Bitmap> ref = sSoftCache.remove(mKey); // cycle it back to the front
                bitmap = ref.get();
                if (bitmap != null) {
                    sHardCache.put(mKey, bitmap);
                } else {
                    // [TODO][MSB]: Resurrect from disk
                }
            }
            fireCallback(bitmap);
        }

        private void fireCallback(final Bitmap bitmap) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onImageFound(mKey, bitmap);
                }
            });
        }

    }

    public interface LookupProviderAvatarImageCacheCallback {
        void onImageFound(String key, Bitmap bitmap);
    }

}
