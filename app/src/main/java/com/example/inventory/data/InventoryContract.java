package com.example.inventory.data;

import android.net.Uri;
import android.provider.BaseColumns;

public class InventoryContract {
    public static final String CONTENT_AUTHORITY = "com.example.item";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_ITEMS = "items";

    private InventoryContract() {
    }

    static public final class ItemEntry implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_ITEMS);
        public static final String COLUMN_ITEM_ID = "_id";
        public static final String TABLE_NAME = "items";
        public static final String COLUMN_ITEM_NAME = "name";
        public static final String COLUMN_ITEM_PRICE = "price";
        public static final String COLUMN_ITEM_QUANTITY = "quantity";
        public static final String COLUMN_ITEM_DESCRIPTION = "description";
        public static final String COLUMN_ITEM_IMG_NAME = "img_name";
        public static final String COLUMN_SUPPLIER_PHONE = "supplier_phone";
        public static final String COLUMN_SUPPLIER_EMAIL = "supplier_email";

    }
}
