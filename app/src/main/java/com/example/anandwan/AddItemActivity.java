package com.example.anandwan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class AddItemActivity extends AppCompatActivity {

    private ImageView ivItemImage;
    private EditText etItemName, etItemDescription, etItemPrice, etItemStock;
    private AutoCompleteTextView actvCategory;
    private Button btnSelectImage, btnCaptureImage, btnSaveItem;
    private Bitmap selectedBitmap;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String editingItemId = null;
    private String existingImageEncoded = null;

    private static final String[] CATEGORIES = {"Jewelry", "Clothing", "Home Decor", "Paintings", "Accessories", "Food", "Other"};

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        selectedBitmap = BitmapFactory.decodeStream(imageStream);
                        ivItemImage.setImageBitmap(selectedBitmap);
                        existingImageEncoded = null; // New image selected
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedBitmap = (Bitmap) result.getData().getExtras().get("data");
                    ivItemImage.setImageBitmap(selectedBitmap);
                    existingImageEncoded = null;
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivItemImage = findViewById(R.id.ivItemImage);
        etItemName = findViewById(R.id.etItemName);
        etItemDescription = findViewById(R.id.etItemDescription);
        etItemPrice = findViewById(R.id.etItemPrice);
        etItemStock = findViewById(R.id.etItemStock);
        actvCategory = findViewById(R.id.actvCategory);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnCaptureImage = findViewById(R.id.btnCaptureImage);
        btnSaveItem = findViewById(R.id.btnSaveItem);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, CATEGORIES);
        actvCategory.setAdapter(adapter);

        // Check if editing
        editingItemId = getIntent().getStringExtra("edit_item_id");
        if (editingItemId != null) {
            loadItemData(editingItemId);
            btnSaveItem.setText("Update Item");
        }

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnCaptureImage.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        });

        btnSaveItem.setOnClickListener(v -> uploadData());
    }

    private void loadItemData(String id) {
        db.collection("items").document(id).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        etItemName.setText(doc.getString("name"));
                        etItemDescription.setText(doc.getString("description"));
                        etItemPrice.setText(String.valueOf(doc.getDouble("price")));
                        etItemStock.setText(String.valueOf(doc.getLong("stock")));
                        actvCategory.setText(doc.getString("category"), false);
                        
                        existingImageEncoded = doc.getString("imageEncoded");
                        if (existingImageEncoded != null) {
                            byte[] decodedBytes = Base64.decode(existingImageEncoded, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            ivItemImage.setImageBitmap(bitmap);
                        }
                    }
                });
    }

    private void uploadData() {
        String name = etItemName.getText().toString().trim();
        String description = etItemDescription.getText().toString().trim();
        String priceStr = etItemPrice.getText().toString().trim();
        String stockStr = etItemStock.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(description) || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(stockStr) || TextUtils.isEmpty(category)) {
            Toast.makeText(this, "All text fields are required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedBitmap == null && existingImageEncoded == null) {
            Toast.makeText(this, "Image is required", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        int stock = Integer.parseInt(stockStr);

        btnSaveItem.setEnabled(false);
        
        String encodedImage = existingImageEncoded;
        if (selectedBitmap != null) {
            encodedImage = encodeImage(selectedBitmap);
        }

        if (encodedImage == null) {
            btnSaveItem.setEnabled(true);
            Toast.makeText(this, "Image processing failed.", Toast.LENGTH_LONG).show();
            return;
        }

        saveToFirestore(name, description, price, stock, category, encodedImage);
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 400;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        if (bytes.length > 700000) return null;
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private void saveToFirestore(String name, String description, double price, int stock, String category, String encodedImage) {
        String id = editingItemId != null ? editingItemId : db.collection("items").document().getId();
        String sellerId = mAuth.getCurrentUser().getUid();

        Item item = new Item(id, name, description, price, encodedImage, sellerId, stock, category);

        db.collection("items").document(id).set(item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, editingItemId != null ? "Item updated" : "Item posted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveItem.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}