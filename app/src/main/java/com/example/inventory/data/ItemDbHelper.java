package com.example.inventory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.example.inventory.data.InventoryContract.ItemEntry;

public class ItemDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "inventory.db";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + ItemEntry.TABLE_NAME
                    + "("
                    + ItemEntry.COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + ItemEntry.COLUMN_ITEM_NAME + " TEXT NOT NULL,"
                    + ItemEntry.COLUMN_ITEM_PRICE + " TEXT NOT NULL,"
                    + ItemEntry.COLUMN_ITEM_QUANTITY + " INTEGER NOT NULL DEFAULT 0,"
                    + ItemEntry.COLUMN_ITEM_DESCRIPTION + " TEXT NOT NULL DEFAULT \"\","
                    + ItemEntry.COLUMN_SUPPLIER_PHONE + " TEXT NOT NULL DEFAULT \"\","
                    + ItemEntry.COLUMN_SUPPLIER_EMAIL + " TEXT NOT NULL DEFAULT \"\","
                    + ItemEntry.COLUMN_ITEM_IMG_NAME + " TEXT NOT NULL DEFAULT \"\""
                    + ");";

    public ItemDbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
