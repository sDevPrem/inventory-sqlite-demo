package com.example.inventory.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ItemProvider extends ContentProvider {
    private static final String LOG_TAG = ContentProvider.class.getSimpleName();
    private static final int ITEM = 100;
    private static final int ITEM_ID = 101;
    private static final UriMatcher uriMatcher;
    ItemDbHelper mItemDbHelper;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_ITEMS, ITEM);
        uriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_ITEMS + "/#", ITEM_ID);
    }

    @Override
    public boolean onCreate() {
        mItemDbHelper = new ItemDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        int match = uriMatcher.match(uri);
        Cursor c = null;
        switch (match) {
            case ITEM:
                c = mItemDbHelper.getReadableDatabase().query(
                        InventoryContract.ItemEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null, null, null
                );
                break;
            case ITEM_ID:
                selection = InventoryContract.ItemEntry.COLUMN_ITEM_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                c = mItemDbHelper.getReadableDatabase().query(
                        InventoryContract.ItemEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null, null, null
                );
                break;
            default:
                throw new IllegalArgumentException("Cannot query Unknown Uri");
        }
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        int match = uriMatcher.match(uri);
        if (match != 100) throw new IllegalArgumentException("Cannot insert unknown uri");
        validateValues(values, false);
        long id = mItemDbHelper.getWritableDatabase().insert(
                InventoryContract.ItemEntry.TABLE_NAME,
                null,
                values
        );
        if (id == -1)
            Log.e(LOG_TAG, "failed to insert rows for " + uri);
        else
            getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int rowsDeleted = 0;
        int match = uriMatcher.match(uri);
        switch (match) {
            case ITEM:
                rowsDeleted = mItemDbHelper.getWritableDatabase().delete(
                        InventoryContract.ItemEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            case ITEM_ID:
                selection = InventoryContract.ItemEntry.COLUMN_ITEM_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = mItemDbHelper.getWritableDatabase().delete(
                        InventoryContract.ItemEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            default:
                throw new IllegalArgumentException("Cannot delete Unknown Uri");
        }
        if (rowsDeleted != 0)
            getContext().getContentResolver().notifyChange(uri, null);
        else
            Log.e(LOG_TAG, "failed to delete rows for " + uri);
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        int match = uriMatcher.match(uri);
        int rowsUpdated;
        switch (match) {
            case ITEM:
                validateValues(values, false);
                rowsUpdated = mItemDbHelper.getWritableDatabase().update(
                        InventoryContract.ItemEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs
                );
                break;
            case ITEM_ID:
                validateValues(values, false);
                selection = InventoryContract.ItemEntry.COLUMN_ITEM_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsUpdated = mItemDbHelper.getWritableDatabase().update(
                        InventoryContract.ItemEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs
                );
                break;
            default:
                throw new IllegalArgumentException("Cannot update Unknown Uri");
        }
        if (rowsUpdated != 0)
            getContext().getContentResolver().notifyChange(uri, null);
        else
            Log.e(LOG_TAG, "failed to update rows for " + uri);
        return rowsUpdated;
    }

    private void validateValues(ContentValues values, boolean areAllValuesRequired) {
        if (areAllValuesRequired && !areAllValuesContained(values))
            throw new IllegalArgumentException("name and price is required");

        if (values.containsKey(InventoryContract.ItemEntry.COLUMN_ITEM_NAME)) {
            String name = values.getAsString(InventoryContract.ItemEntry.COLUMN_ITEM_NAME);
            if (name == null || TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException("Item requires valid a name");
            }
        }
        if (values.containsKey(InventoryContract.ItemEntry.COLUMN_ITEM_PRICE)) {
            String price = values.getAsString(InventoryContract.ItemEntry.COLUMN_ITEM_PRICE);
            if (price == null || TextUtils.isEmpty(price)) {
                throw new IllegalArgumentException("Item requires valid price");
            }
        }
    }

    private boolean areAllValuesContained(ContentValues values) {
        return values.containsKey(InventoryContract.ItemEntry.COLUMN_ITEM_NAME) &&
                values.containsKey(InventoryContract.ItemEntry.COLUMN_ITEM_PRICE);
    }
}
