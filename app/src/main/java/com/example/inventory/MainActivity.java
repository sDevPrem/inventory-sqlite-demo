package com.example.inventory;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.example.inventory.data.InventoryContract;
import com.example.inventory.data.ItemListAdapter;
import com.example.inventory.data.utility.ImageUtility;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int LOADER_ID = 1;
    ListView itemList;
    ItemListAdapter adapter;

    boolean isStoragePermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.fab_add_new_item).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
            startActivity(intent);
        });

        isStoragePermissionGranted = checkStoragePermission();

        adapter = new ItemListAdapter(this, null,isStoragePermissionGranted);
        itemList = findViewById(R.id.item_list);
        itemList.setAdapter(adapter);
        itemList.setEmptyView(findViewById(R.id.empty_view));
        itemList.setOnItemClickListener((parent, view, position, id) -> {
            Uri uri = ContentUris.withAppendedId(InventoryContract.ItemEntry.CONTENT_URI, id);
            Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
            intent.setData(uri);
            startActivity(intent);
        });
        LoaderManager.getInstance(this).initLoader(1, null, this);
    }

    private void displayDatabaseInfo() {
        String[] projection = {
                InventoryContract.ItemEntry.COLUMN_ITEM_ID,
                InventoryContract.ItemEntry.COLUMN_ITEM_NAME,
                InventoryContract.ItemEntry.COLUMN_ITEM_QUANTITY,
                InventoryContract.ItemEntry.COLUMN_ITEM_PRICE,

        };
        Cursor c = getContentResolver().query(InventoryContract.ItemEntry.CONTENT_URI,
                projection,
                null,
                null,
                null,
                null
        );
        itemList.setAdapter(new ItemListAdapter(this, c,isStoragePermissionGranted));
    }

    private void insertDummyItem() {
        ContentValues values = new ContentValues();
        values.put(InventoryContract.ItemEntry.COLUMN_ITEM_NAME, "Soap");
        values.put(InventoryContract.ItemEntry.COLUMN_ITEM_PRICE, "99");
        values.put(InventoryContract.ItemEntry.COLUMN_ITEM_QUANTITY, 0);

        getContentResolver().insert(InventoryContract.ItemEntry.CONTENT_URI, values);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete_all_item) {
            showDeleteConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_item);
        builder.setPositiveButton(R.string.delete, (dialog, which) -> {
            if (adapter.isEmpty())
                return;
            Executors.newSingleThreadExecutor().execute(() -> {
                Cursor c = getContentResolver().query(InventoryContract.ItemEntry.CONTENT_URI,
                        new String[]{InventoryContract.ItemEntry.COLUMN_ITEM_IMG_NAME},
                        null,
                        null,
                        null);
                deleteAllItemImage(c);
                getContentResolver().delete(InventoryContract.ItemEntry.CONTENT_URI,
                        null,
                        null);

            });
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
            if (dialog != null)
                dialog.dismiss();
        });
        builder.show();
    }

    private void deleteAllItemImage(Cursor c) {
        if(!isStoragePermissionGranted)
            return;

        if (!c.moveToFirst())
            return;
        int imgNameColIndex = c.getColumnIndex(InventoryContract.ItemEntry.COLUMN_ITEM_IMG_NAME);
        do {
            String imgName = c.getString(imgNameColIndex);
            if (!TextUtils.isEmpty(imgName))
                ImageUtility.deleteImg(imgName, this);
        } while (c.moveToNext());
        c.close();
    }
    private boolean checkStoragePermission() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                || ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        String[] projection = {
                InventoryContract.ItemEntry.COLUMN_ITEM_ID,
                InventoryContract.ItemEntry.COLUMN_ITEM_NAME,
                InventoryContract.ItemEntry.COLUMN_ITEM_QUANTITY,
                InventoryContract.ItemEntry.COLUMN_ITEM_PRICE,
                InventoryContract.ItemEntry.COLUMN_ITEM_IMG_NAME

        };
        return new CursorLoader(this,
                InventoryContract.ItemEntry.CONTENT_URI, projection,
                null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data != null)
            adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader loader) {
        adapter.swapCursor(null);
    }
}