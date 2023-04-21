package com.example.inventory.data;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.inventory.R;
import com.example.inventory.data.utility.ImageUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ItemListAdapter extends CursorAdapter {
    Map<Long, Bitmap> thumbCache = new HashMap<>();
    Executor imgExecutor = Executors.newSingleThreadExecutor();
    Handler imgHandler = new Handler(Looper.getMainLooper());

    boolean isStoragePermissionGranted;

    public ItemListAdapter(Context context, Cursor c, boolean isStoragePermissionGranted) {
        super(context, c, 0);
        this.isStoragePermissionGranted = isStoragePermissionGranted;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int itemNameColIndex = cursor.getColumnIndex(InventoryContract.ItemEntry.COLUMN_ITEM_NAME);
        int itemQuantityColIndex = cursor.getColumnIndex(InventoryContract.ItemEntry.COLUMN_ITEM_QUANTITY);
        int itemPriceColIndex = cursor.getColumnIndex(InventoryContract.ItemEntry.COLUMN_ITEM_PRICE);
        int itemImageNameColIndex = cursor.getColumnIndex(InventoryContract.ItemEntry.COLUMN_ITEM_IMG_NAME);
        int itemIdIndex = cursor.getColumnIndex(InventoryContract.ItemEntry.COLUMN_ITEM_ID);
        final long id = cursor.getLong(itemIdIndex);
        ImageView itemImage = view.findViewById(R.id.list_item_image);
        Button saleBtn = view.findViewById(R.id.item_sale_btn);

        ((TextView) view.findViewById(R.id.category_item_name))
                .setText(cursor.getString(itemNameColIndex));
        ((TextView) view.findViewById(R.id.price))
                .setText(String.valueOf(cursor.getString(itemPriceColIndex)));
        String itemImageName = cursor.getString((itemImageNameColIndex));

        TextView categoryQuantityTextView = (TextView) view.findViewById(R.id.category_quantity);
        TextView quantityTextView = (TextView) view.findViewById(R.id.quantity_count);
        int quantity = cursor.getInt(itemQuantityColIndex);
        quantityTextView.setText(String.valueOf(quantity));
        if (quantity > 0) {
            categoryQuantityTextView.setTextColor(context
                    .getResources()
                    .getColor(R.color.stockColor, null));
            quantityTextView.setTextColor(context
                    .getResources()
                    .getColor(R.color.stockColor, null));
        } else {
            categoryQuantityTextView.setTextColor(context
                    .getResources()
                    .getColor(R.color.empty_stock_color, null));
            quantityTextView.setTextColor(context
                    .getResources()
                    .getColor(R.color.empty_stock_color, null));
        }
        saleBtn.setOnClickListener((v) -> {
            if (quantity < 1)
                return;
            Uri uri = ContentUris.withAppendedId(InventoryContract.ItemEntry.CONTENT_URI, id);
            ContentValues values = new ContentValues();
            values.put(InventoryContract.ItemEntry.COLUMN_ITEM_QUANTITY, quantity - 1);
            int rowsUpdated = context.getContentResolver().update(uri, values, null, null);
            if (rowsUpdated < 1) {
                Toast.makeText(context, "Can't sale item", Toast.LENGTH_SHORT).show();
            }
        });

        if(!isStoragePermissionGranted)
            return;

        if (thumbCache.get(id) != null) {
            itemImage.setImageBitmap(thumbCache.get(id));
        } else {
            itemImage.setImageResource(R.drawable.ic_baseline_image_24);
            imgExecutor.execute(() -> {
                Bitmap thumbBitmap;
                thumbBitmap = ImageUtility.getThumbBitmap(itemImageName, context);
                imgHandler.post(() -> {
                    if (thumbBitmap != null) {
                        thumbCache.put(id, thumbBitmap);
                        itemImage.setImageBitmap(thumbBitmap);
                    }
                });
            });
        }
    }
}
