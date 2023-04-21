package com.example.inventory.data.utility;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.example.inventory.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ImageUtility {
    public static void saveImageIntoStorage(String imgName, Bitmap imgBitmap, Context context) {
        Uri mediaContentUri;
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, imgName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String imgRelativePath = Environment.DIRECTORY_PICTURES
                    + File.separator
                    + context.getResources().getString(R.string.app_name);
            values.put(MediaStore.Images.Media.RELATIVE_PATH, imgRelativePath);
            mediaContentUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            File imgDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath()
                            + File.separator
                            + context.getResources().getString(R.string.app_name)
            );
            if (!imgDir.exists())
                imgDir.mkdirs();
            String imgPath = imgDir.getAbsolutePath()
                    + File.separator
                    + imgName;
            values.put(MediaStore.Images.ImageColumns.DATA, imgPath);
            mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
        Uri imgMediaUri = context.getContentResolver().insert(mediaContentUri, values);
        try (OutputStream os = context.getContentResolver().openOutputStream(imgMediaUri)) {
            imgBitmap.compress(Bitmap.CompressFormat.JPEG, 50, os);
        } catch (IOException e) {
            Toast.makeText(context, "Can't save image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public static void saveImageIntoStorage(String imgName, Uri imgUri, Context context) throws FileNotFoundException {
        saveImageIntoStorage(imgName, getPortraitBitmap(imgUri, context), context);
    }


    public static Bitmap getBitmapFromAppDir(String imgName, Context context) {
        Uri imgUri = getImageUri(imgName, context);
        return getBitmapFromAppDir(imgUri, context);
    }

    public static Bitmap getBitmapFromAppDir(Uri imgUri, Context context) {
        if (imgUri == null)
            return null;
        try (InputStream is = context.getContentResolver().openInputStream(imgUri)) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void updateImageInStorage(String imgName, Bitmap imgBimap, Context context) {
//        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath()
//                + File.separator
//                + context.getResources().getString(R.string.app_name)
//                + File.separator
//                + imgName);
        deleteImg(imgName, context);
        saveImageIntoStorage(imgName, imgBimap, context);
//        try {
//            imgBimap.compress(Bitmap.CompressFormat.JPEG,50,new FileOutputStream(file));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        deleteThumbnail(imgName,context);
    }

    public static void updateImageInStorage(String imgName, Uri sourceImageUri, Context context) throws FileNotFoundException {
        updateImageInStorage(imgName, getPortraitBitmap(sourceImageUri, context), context);
    }

    public static Bitmap getPortraitBitmap(String photoPath) {
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
        try {
            return rotateBitmapToPortrait(bitmap, new ExifInterface(photoPath));
        } catch (IOException e) {
            return bitmap;
        }
    }

    public static Bitmap getPortraitBitmap(Uri uri, Context context) throws FileNotFoundException {
        Bitmap bitmap;
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            return null;
        }
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            return rotateBitmapToPortrait(bitmap, new ExifInterface(is));
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            return bitmap;
        }
    }

    public static Bitmap rotateBitmapToPortrait(Bitmap bitmap, ExifInterface ei) {
        if (ei == null)
            return bitmap;
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap;
        switch (orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                rotatedBitmap = rotateBitmap(bitmap, 90);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                rotatedBitmap = rotateBitmap(bitmap, 180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                rotatedBitmap = rotateBitmap(bitmap, 270);
                break;

            case ExifInterface.ORIENTATION_NORMAL:
            default:
                rotatedBitmap = bitmap;
        }
        return rotatedBitmap;
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    @Nullable
    public static Uri getImageUri(String imgName, Context context) {
        String[] projection = {MediaStore.Images.Media._ID};
        Cursor c;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String relativeImagePath = Environment.DIRECTORY_PICTURES
                    + File.separator
                    + context.getResources().getString(R.string.app_name)
                    + File.separator;
            c = context.getContentResolver().query(
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    projection,
                    MediaStore.Images.Media.DISPLAY_NAME + "=? AND "
                            + MediaStore.Images.Media.RELATIVE_PATH + "=?",
                    new String[]{imgName, relativeImagePath},
                    null
            );
        } else {
            String imgPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath()
                    + File.separator
                    + context.getResources().getString(R.string.app_name)
                    + File.separator
                    + imgName;
            c = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    MediaStore.Images.Media.DATA + "=?",
                    new String[]{imgPath},
                    null
            );
            //if img file is present but the MediaContentProvider does not know about the file
            //then give it the img File info and return the uri
            if (!c.moveToFirst()) {
                if (new File(imgPath).exists()) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DATA, imgPath);
                    return context.getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                } else return null;
            }
        }
        if (c.getCount() == 0)
            return null;
        c.moveToFirst();
        int pos = c.getColumnIndex(projection[0]);
        int id = c.getInt(pos);
        c.close();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), id);
        }
        return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
    }

    public static void deleteImg(String imgName, @NonNull Context context) {
        Uri imgUri = getImageUri(imgName, context);
        if (imgUri != null)
            context.getContentResolver().delete(getImageUri(imgName, context), null, null);
    }

    public static Bitmap getThumbBitmap(String originalImageName, Context context) {
        Bitmap thumbBitmap = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            try {
                Uri imgUri = ImageUtility.getImageUri(originalImageName, context);
                if (imgUri != null)
                    thumbBitmap = context
                            .getContentResolver()
                            .loadThumbnail(imgUri
                                    , new Size(100, 100)
                                    , null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        else {
            try {
                Uri imgUri = ImageUtility.getImageUri(originalImageName, context);
                if (imgUri != null)
                    thumbBitmap = MediaStore.Images.Thumbnails.getThumbnail(
                            context.getContentResolver(),
                            ContentUris.parseId(imgUri),
                            MediaStore.Images.Thumbnails.MINI_KIND,
                            new BitmapFactory.Options()
                    );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return thumbBitmap;
    }

    public static Bitmap decodeSampledBitmapFromResource(int reqWidth, int reqHeight, Uri uri, Context context) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(is, null, options);
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(is, null, options);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
