package com.example.anandwan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
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
import java.util.UUID;

public class AddItemActivity extends AppCompatActivity {

    private ImageView ivItemImage;
    private EditText etItemName, etItemDescription, etItemPrice, etItemStock;
    private Button btnSelectImage, btnSaveItem;
    private Bitmap selectedBitmap;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        selectedBitmap = BitmapFactory.decodeStream(imageStream);
                        ivItemImage.setImageBitmap(selectedBitmap);
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
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
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSaveItem = findViewById(R.id.btnSaveItem);

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnSaveItem.setOnClickListener(v -> uploadData());
    }

    private void uploadData() {
        String name = etItemName.getText().toString().trim();
        String description = etItemDescription.getText().toString().trim();
        String priceStr = etItemPrice.getText().toString().trim();
        String stockStr = etItemStock.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(description) || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(stockStr) || selectedBitmap == null) {
            Toast.makeText(this, "All fields and image are required", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        int stock = Integer.parseInt(stockStr);

        btnSaveItem.setEnabled(false);
        Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show();

        // Convert Bitmap to Base64
        String encodedImage = encodeImage(selectedBitmap);
        if (encodedImage == null) {
            btnSaveItem.setEnabled(true);
            Toast.makeText(this, "Image too large, please pick a smaller one.", Toast.LENGTH_LONG).show();
            return;
        }

        saveToFirestore(name, description, price, stock, encodedImage);
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 400; // Smaller resolution for product preview
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream); // 70% quality
        byte[] bytes = byteArrayOutputStream.toByteArray();
        
        // Firestore limit is 1MB. Base64 is ~1.37x larger than raw bytes.
        // Keeping it under 600KB raw ensures the string is safely under 1MB.
        if (bytes.length > 700000) {
            return null;
        }
        
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private void saveToFirestore(String name, String description, double price, int stock, String encodedImage) {
        String id = db.collection("items").document().getId();
        String sellerId = mAuth.getCurrentUser().getUid();

        Item item = new Item(id, name, description, price, encodedImage, sellerId, stock);

        db.collection("items").document(id).set(item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddItemActivity.this, "Item posted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveItem.setEnabled(true);
                    Toast.makeText(AddItemActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}