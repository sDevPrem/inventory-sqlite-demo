package com.example.inventory;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.example.inventory.data.InventoryContract;
import com.example.inventory.data.utility.ImageUtility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int STORAGE_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;
    private static final int GALLERY_REQUEST_CODE = 102;
    private static final int LOADER_ID = 1;
    EditText itemNameEdit, itemPriceEdit, itemQuantityEdit, itemDescriptionEdit, supplierPhoneEdit, supplierEmailEdit;
    ImageView itemImgView;
    Button addItemPhotoBtn;
    Uri uri;
    File tempImageFile = null;
    Uri itemImgUri;
    Uri tempImgUri;
    String dbImageName = "";
    boolean isImageChanged = false;
    boolean isItemChanged;
    Executor mExecutor = Executors.newSingleThreadExecutor();
    Handler mHandler = new Handler(Looper.getMainLooper());
    TextWatcher itemChangeTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!TextUtils.isEmpty(s.toString()))
                isItemChanged = true;
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };
    private boolean isStoragePermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        isStoragePermissionGranted = checkStoragePermission();

        itemNameEdit = findViewById(R.id.edit_item_name);
        itemPriceEdit = findViewById(R.id.edit_product_price);
        itemDescriptionEdit = findViewById(R.id.edit_product_description);
        itemQuantityEdit = findViewById(R.id.edit_product_quantity);
        supplierPhoneEdit = findViewById(R.id.edit_supplier_phone);
        supplierEmailEdit = findViewById(R.id.edit_supplier_email);
        itemImgView = findViewById(R.id.image_product);
        addItemPhotoBtn = findViewById(R.id.button_add_product_photo);
        View fabContactSupplier = findViewById(R.id.fab_contact_supplier);
        fabContactSupplier.setOnClickListener(v -> contactSupplier());

        addItemPhotoBtn.setOnClickListener(v -> {
            if (checkStoragePermission())
                showAddPhotoDialog();
            else requestStoragePermission();
        });

        itemImgView.setOnClickListener(v -> {
            if (itemImgUri == null || isImageChanged)
                return;
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(itemImgUri, "image/*");
            startActivity(Intent.createChooser(intent, "Choose an image viewer"));
        });

        Intent intent = getIntent();
        Uri intentUri = intent.getData();
        if (intentUri != null) {
            uri = intentUri;
            LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
        } else {
            invalidateOptionsMenu();
            addTextChangeListeners();
            intentUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (intentUri != null &&
                    intent.getType() != null &&
                    intent.getType().startsWith("image/")) {
                itemImgUri = intentUri;
                mExecutor.execute(() -> {
                    Bitmap bitmap;
                    bitmap = getSampledBitmap(itemImgUri);
                    runOnMainThread(() -> {
                        itemImgView.setImageBitmap(bitmap);
                        isImageChanged = true;
                    });
                });
            }
        }

        findViewById(R.id.button_product_quantity_increment).setOnClickListener(v -> {
            String currentQuantityString = itemQuantityEdit.getText().toString();
            long currentQuantity;
            if (TextUtils.isEmpty(currentQuantityString))
                currentQuantity = 0;
            else
                currentQuantity = Long.parseLong(currentQuantityString);
            itemQuantityEdit.setText(String.valueOf(currentQuantity + 1));
        });
        findViewById(R.id.button_product_quantity_decrement).setOnClickListener(v -> {
            String currentQuantityString = itemQuantityEdit.getText().toString();
            long currentQuantity;
            if (TextUtils.isEmpty(currentQuantityString))
                currentQuantity = 0;
            else
                currentQuantity = Long.parseLong(currentQuantityString);
            if (currentQuantity > 0)
                itemQuantityEdit.setText(String.valueOf(currentQuantity - 1));
        });
    }

    private void contactSupplier() {
        String supplierPhone = supplierPhoneEdit.getText().toString();
        String supplierEmail = supplierEmailEdit.getText().toString();
        if (TextUtils.isEmpty(supplierEmail) && TextUtils.isEmpty(supplierPhone)) {
            Toast
                    .makeText(this, R.string.no_supplier_contact_msg, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        if (TextUtils.isEmpty(supplierEmail) && !TextUtils.isEmpty(supplierPhone)) {
            dialSupplierIntent();
            return;
        }
        if (!TextUtils.isEmpty(supplierEmail) && TextUtils.isEmpty(supplierPhone)) {
            emailSupplierIntent();
            return;
        }
        AlertDialog.Builder contactSupplierDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.contact_supplier_msg)
                .setPositiveButton(R.string.supplier_contact_call, (dialog, which) -> dialSupplierIntent())
                .setNegativeButton(R.string.supplier_contact_email, (dialog, which) -> emailSupplierIntent());
        contactSupplierDialog.show();
    }

    private void dialSupplierIntent() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + supplierPhoneEdit.getText().toString()));
        startActivity(callIntent);
    }

    private void emailSupplierIntent() {
        Intent email = new Intent(Intent.ACTION_SEND);
        email.putExtra(Intent.EXTRA_EMAIL, new String[]{supplierEmailEdit.getText().toString()});
        email.putExtra(Intent.EXTRA_SUBJECT, "");
        email.putExtra(Intent.EXTRA_TEXT, "");

        //need this to prompts email client only
        email.setType("message/rfc822");
        startActivity(Intent.createChooser(email, "Choose an Email client :"));
    }

    private void addTextChangeListeners() {
        itemNameEdit.addTextChangedListener(itemChangeTextWatcher);
        itemPriceEdit.addTextChangedListener(itemChangeTextWatcher);
        itemQuantityEdit.addTextChangedListener(itemChangeTextWatcher);
        itemDescriptionEdit.addTextChangedListener(itemChangeTextWatcher);
        supplierEmailEdit.addTextChangedListener(itemChangeTextWatcher);
        supplierPhoneEdit.addTextChangedListener(itemChangeTextWatcher);
    }

    private void removeTextChangeListeners() {
        itemNameEdit.removeTextChangedListener(itemChangeTextWatcher);
        itemPriceEdit.removeTextChangedListener(itemChangeTextWatcher);
        itemQuantityEdit.removeTextChangedListener(itemChangeTextWatcher);
        itemDescriptionEdit.removeTextChangedListener(itemChangeTextWatcher);
        supplierEmailEdit.removeTextChangedListener(itemChangeTextWatcher);
        supplierPhoneEdit.removeTextChangedListener(itemChangeTextWatcher);
    }

    private void showDiscardChangesDialog() {
        AlertDialog.Builder discardChangeDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.discard_dialog_msg)
                .setPositiveButton(R.string.discard_change, (dialog, which) -> finish())
                .setNegativeButton(R.string.keep_editing, (dialog, which) -> {
                    if (dialog != null)
                        dialog.dismiss();
                });
        discardChangeDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (isItemChanged || isImageChanged) {
            showDiscardChangesDialog();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isItemChanged || isImageChanged) {
                    showDiscardChangesDialog();
                    return true;
                }
                // Navigate back to parent activity (CatalogActivity)
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_save:
                if (!checkValues())
                    return true;
                saveDataIntoDb();
                return true;
            case R.id.action_delete_item:
                showDeleteConfirmationDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveDataIntoDb() {
        mExecutor.execute(() -> {
            String imgName = saveItemImg(dbImageName, itemImgUri);
            runOnMainThread(() ->{
                saveInventoryData(imgName);
                finish();
            });

        });
    }

    private void saveInventoryData(String imgName) {
        int rowsUpdated = 0;
        long insertedItemId = -1;
        ContentValues values = getValues();
        values.put(InventoryContract.ItemEntry.COLUMN_ITEM_IMG_NAME, imgName);

        if (uri == null) {
            Uri itemUri = getContentResolver().insert(InventoryContract.ItemEntry.CONTENT_URI, values);
            insertedItemId = ContentUris.parseId(itemUri);
        } else rowsUpdated = getContentResolver().update(uri, values, null, null);
        if (rowsUpdated != 0 || insertedItemId > 0) {
            Toast.makeText(this, "Item Saved", Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(this, "Failed to save item", Toast.LENGTH_SHORT).show();
    }

    private void runOnMainThread(Runnable run) {
        mHandler.post(run);
    }

    private boolean checkValues() {
        String name = itemNameEdit.getText().toString();
        if (TextUtils.isEmpty(name)) {
            itemNameEdit.setError("Enter a valid name");
            return false;
        }
        return true;
    }

    private ContentValues getValues() {
        ContentValues values = new ContentValues();
        String price = itemPriceEdit.getText().toString();
        String quantity = itemQuantityEdit.getText().toString();
        if (TextUtils.isEmpty(price))
            price = "0";
        if (TextUtils.isEmpty(quantity))
            quantity = "0";
        values.put(InventoryContract.ItemEntry.COLUMN_ITEM_NAME, itemNameEdit.getText().toString());
        values.put(InventoryContract.ItemEntry.COLUMN_ITEM_PRICE, price);
        values.put(InventoryContract.ItemEntry.COLUMN_ITEM_QUANTITY, quantity);
        values.put(InventoryContract.ItemEntry.COLUMN_ITEM_DESCRIPTION, itemDescriptionEdit.getText().toString());
        values.put(InventoryContract.ItemEntry.COLUMN_SUPPLIER_PHONE, supplierPhoneEdit.getText().toString());
        values.put(InventoryContract.ItemEntry.COLUMN_SUPPLIER_EMAIL, supplierEmailEdit.getText().toString());
        return values;
    }

    private void deleteItem() {
        if (!TextUtils.isEmpty(dbImageName))
            ImageUtility.deleteImg(dbImageName, this);
        int rowsDeleted = getContentResolver().delete(uri, null, null);
        if (rowsDeleted == 0)
            Toast.makeText(this, "failed to delete item", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "Item Deleted", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_item_msg);
        builder.setPositiveButton(R.string.delete, (dialog, which) -> deleteItem());
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
            if (dialog != null)
                dialog.dismiss();
        });
        builder.show();
    }

    private boolean checkStoragePermission() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        //Take permission only to read storage for Android 10+
        //And for lower Android Versions take permission to write in storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
        else
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
                isStoragePermissionGranted = true;
            } else
                Toast.makeText(this, "Storage Permission Not Granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddPhotoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("Open Camera", (dialog, which) -> openCamera());
        builder.setNegativeButton("Open Gallery", (dialog, which) -> openGallery());
        builder.show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File privateImageDir = new File(getCacheDir(), "temp_image");
        privateImageDir.mkdirs();
        try {
            if (tempImageFile == null)
                tempImageFile = File.createTempFile("temp", ".jpg", privateImageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (tempImageFile != null)
            tempImgUri = FileProvider.getUriForFile(this,
                    "com.example.android.InventoryFileProvider",
                    tempImageFile
            );
        if (tempImgUri != null) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, tempImgUri);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            intent.setDataAndType(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), "image/*");
        else intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE && tempImgUri != null) {
                itemImgUri = tempImgUri;
            } else if (requestCode == GALLERY_REQUEST_CODE && data != null) {
                itemImgUri = data.getData();
                getContentResolver().takePersistableUriPermission(itemImgUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            if (itemImgUri != null) {
                mExecutor.execute(() -> {
                    Bitmap bitmap = getSampledBitmap(itemImgUri);
                    if (bitmap == null)
                        return;
                    runOnMainThread(() -> {
                        itemImgView.setImageBitmap(bitmap);
                        addItemPhotoBtn.setText(R.string.photo_btn_change);
                        isImageChanged = true;
                    });
                });
            } else Toast.makeText(this, "Can't load photo", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap getSampledBitmap(Uri imgUri) {
        Bitmap sampledBitmap = ImageUtility.
                decodeSampledBitmapFromResource(
                        (int) getResources().getDimension(R.dimen.details_item_img_width),
                        (int) getResources().getDimension(R.dimen.details_item_img_height),
                        imgUri,
                        this);
        if (sampledBitmap == null)
            return null;
        ExifInterface ei;
        try {
            ei = new ExifInterface(getContentResolver().openInputStream(imgUri));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return ImageUtility.rotateBitmapToPortrait(sampledBitmap, ei);
    }

    private String saveItemImg(String imgName, Uri imgUri) {
        if (imgUri != null && isImageChanged) {
            if (TextUtils.isEmpty(imgName)) {
                imgName = itemNameEdit.getText().toString() + "_" + System.currentTimeMillis() + ".jpg";
                try {
                    ImageUtility.saveImageIntoStorage(imgName, imgUri, this);
                } catch (FileNotFoundException e) {
                    Toast.makeText(this, "Unable to save photo", Toast.LENGTH_SHORT).show();
                } finally {
                    if (tempImageFile != null)
                        tempImageFile.delete();
                }
            } else {
                try {
                    ImageUtility.updateImageInStorage(imgName, imgUri, this);
                } catch (FileNotFoundException e) {
                    Toast.makeText(this, "Unable to save photo", Toast.LENGTH_SHORT).show();
                } finally {
                    if (tempImageFile != null)
                        tempImageFile.delete();
                }
            }
        }
        return imgName;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_details, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_delete_item).setVisible(uri != null);
        return super.onPrepareOptionsMenu(menu);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        String[] projection = {
                InventoryContract.ItemEntry.COLUMN_ITEM_NAME,
                InventoryContract.ItemEntry.COLUMN_ITEM_PRICE,
                InventoryContract.ItemEntry.COLUMN_ITEM_QUANTITY,
                InventoryContract.ItemEntry.COLUMN_ITEM_DESCRIPTION,
                InventoryContract.ItemEntry.COLUMN_SUPPLIER_PHONE,
                InventoryContract.ItemEntry.COLUMN_SUPPLIER_EMAIL,
                InventoryContract.ItemEntry.COLUMN_ITEM_IMG_NAME
        };
        return new CursorLoader(this, uri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor c) {
        if (c == null || c.getCount() < 1)
            return;

        c.moveToPosition(0);
        int itemNameColIndex = c.getColumnIndex(InventoryContract.ItemEntry.COLUMN_ITEM_NAME);
        int itemQuantityColIndex = c.getColumnIndex(InventoryContract.ItemEntry.COLUMN_ITEM_QUANTITY);
        int itemPriceColIndex = c.getColumnIndex(InventoryContract.ItemEntry.COLUMN_ITEM_PRICE);
        int itemDescriptionColIndex = c.getColumnIndex(InventoryContract.ItemEntry.COLUMN_ITEM_DESCRIPTION);
        int supplierPhoneColIndex = c.getColumnIndex(InventoryContract.ItemEntry.COLUMN_SUPPLIER_PHONE);
        int supplierEmailColIndex = c.getColumnIndex(InventoryContract.ItemEntry.COLUMN_SUPPLIER_EMAIL);
        int itemImgNameColIndex = c.getColumnIndex(InventoryContract.ItemEntry.COLUMN_ITEM_IMG_NAME);

        removeTextChangeListeners();
        itemNameEdit.setText(c.getString(itemNameColIndex));
        itemPriceEdit.setText(c.getString(itemPriceColIndex));
        itemQuantityEdit.setText(String.valueOf(c.getInt(itemQuantityColIndex)));
        itemDescriptionEdit.setText(c.getString(itemDescriptionColIndex));
        supplierPhoneEdit.setText(c.getString(supplierPhoneColIndex));
        supplierEmailEdit.setText(c.getString(supplierEmailColIndex));
        addTextChangeListeners();
        dbImageName = c.getString(itemImgNameColIndex);

        if (!isStoragePermissionGranted)
            return;

        mExecutor.execute(() -> {
            if (!TextUtils.isEmpty(dbImageName) && itemImgUri == null) {
                itemImgUri = ImageUtility.getImageUri(dbImageName, this);
            }
            if (itemImgUri != null) {
                Uri finalImgUri = itemImgUri;
                runOnMainThread(() -> {
                    final Bitmap bitmap = getSampledBitmap(finalImgUri);
                    itemImgView.setImageBitmap(bitmap);
                    addItemPhotoBtn.setText(R.string.photo_btn_change);
                });
            }
        });
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }
}