package com.broadpaper;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.WallpaperManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.IOException;
import java.io.File;

public class Main extends BroadcastReceiver {
    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onReceive(Context context, Intent intent) {
        String file = intent.getDataString();
        if (file == null) {
            this.setResult(1, "No file specified!", null);
            return;
        }
        File wp = new File(file);
        if (!wp.canRead()) {
            boolean isReadImages = context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED;
            if (!isReadImages) {
                this.setResult(13, "We need READ_MEDIA_IMAGES permission.", null);
                return;
            }
            this.setResult(13, "Cannot read image! " + file, null);
            return;
        }
        Bitmap wallpaper = (intent.hasExtra("center") || intent.hasExtra("nocrop")) ?
                resizeImage(context, intent, BitmapFactory.decodeFile(file)) : BitmapFactory.decodeFile(file);
        if (wallpaper == null) {
            this.setResult(2, "Invalid image file! " + file, null);
            return;
        }
        int flag = intent.hasExtra("home") ? WallpaperManager.FLAG_SYSTEM :
                intent.hasExtra("lock") ? WallpaperManager.FLAG_LOCK : -1;
        WallpaperManager wm = WallpaperManager.getInstance(context.getApplicationContext());
        try {
            if (flag != -1) {
                wm.setBitmap(wallpaper, null, true, flag);
            } else {
                wm.setBitmap(wallpaper, null, true);
            }
            this.setResult(0, "Successfully changed wallpaper.", null);
        } catch(IOException e) {
            this.setResult(5, "An error occurred while setting wallpaper! " + e, null);
        }
    }

    private Bitmap resizeImage(Context context, final Intent intent, Bitmap originalImage) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        int height = metrics.heightPixels;
        int width = metrics.widthPixels;
        Bitmap newImage = Bitmap.createBitmap(width, height, originalImage.getConfig());
        Canvas canvas = new Canvas(newImage);
        float imageWidth = originalImage.getWidth();
        float imageHeight = originalImage.getHeight();
        float scaleX = width / imageWidth;
        float scaleY = height / imageHeight;
        if (intent.hasExtra("center")) {
            float scale = Math.max(scaleY, scaleX);
            float scaledWidth = scale * imageWidth;
            float scaledHeight = scale * imageHeight;
            float left = (width - scaledWidth) / 2;
            float top = (height - scaledHeight) / 2;
            RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);
            Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            canvas.drawBitmap(originalImage, null, targetRect, paint);
        } else {
            float scale = Math.min(scaleY, scaleX);
            float xTranslation = (width - imageWidth * scale) / 2.0f;
            float yTranslation = (height - imageHeight * scale) / 2.0f;
            Matrix matrix = new Matrix();
            matrix.postTranslate(xTranslation, yTranslation);
            matrix.preScale(scale, scale);
            Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            canvas.drawBitmap(originalImage, matrix, paint);
        }
        return newImage;
    }
}