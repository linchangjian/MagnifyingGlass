package com.meitu.mopiview.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.File;

public class FileUtils {

    public static String getExternalCacheDir(Context context) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return null;
        }

        String cacheDir = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            File fCacheDir = context.getExternalCacheDir();
            if (fCacheDir != null) {
                cacheDir = fCacheDir.getPath();
            }
        } else {

            String baseDir = Environment.getExternalStorageDirectory().getPath();
            cacheDir = baseDir + "/Android/data/com.meitu/cache/";
        }
        return cacheDir;

    }

}