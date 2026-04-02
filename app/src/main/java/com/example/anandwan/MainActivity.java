package com.example.anandwan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvItems;
    private ItemAdapter adapter;
    private List<Item> itemList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FloatingActionButton fabAddItem;
    private TextView tvSellerName, tvSellerCategory, tvSellerStory;
    private ImageView ivAvatar;
    private Button btnAddListing, btnInventory;
    private View statTotalListings, statTotalStock, statTotalOrders, statAvgPrice, statTotalRevenue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Bind Views
        ivAvatar = findViewById(R.id.ivAvatar);
        tvSellerName = findViewById(R.id.tvSellerName);
        tvSellerCategory = findViewById(R.id.tvSellerCategory);
        tvSellerStory = findViewById(R.id.tvSellerStory);
        btnAddListing = findViewById(R.id.btnAddListing);
        btnInventory = findViewById(R.id.btnInventory);
        rvItems = findViewById(R.id.rvItems);
        fabAddItem = findViewById(R.id.fabAddItem);

        // Stat Views
        statTotalListings = findViewById(R.id.statTotalListings);
        statTotalStock = findViewById(R.id.statTotalStock);
        statTotalOrders = findViewById(R.id.statTotalOrders);
        statAvgPrice = findViewById(R.id.statAvgPrice);
        statTotalRevenue = findViewById(R.id.statTotalRevenue);

        setupRecyclerView();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            loadSellerProfile();
            loadItems();
        }
    }

    private void setupRecyclerView() {
        rvItems.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        itemList = new ArrayList<>();
        adapter = new ItemAdapter(itemList);
        rvItems.setAdapter(adapter);
    }

    private void setupClickListeners() {
        View.OnClickListener addListingListener = v -> startActivity(new Intent(MainActivity.this, AddItemActivity.class));
        btnAddListing.setOnClickListener(addListingListener);
        fabAddItem.setOnClickListener(addListingListener);

        btnInventory.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, InventoryActivity.class));
        });

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, EditProfileActivity.class));
        });

        findViewById(R.id.btnSeeAllListings).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, InventoryActivity.class));
        });
    }

    private void loadSellerProfile() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String category = documentSnapshot.getString("category");
                        String story = documentSnapshot.getString("story");
                        String encodedImage = documentSnapshot.getString("profileImage");

                        tvSellerName.setText(name != null && !name.isEmpty() ? name : "Seller Name");
                        tvSellerCategory.setText(category != null && !category.isEmpty() ? category : "Add your category");
                        tvSellerStory.setText(story != null && !story.isEmpty() ? story : "Share your story with customers...");
                        
                        if (encodedImage != null) {
                            byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            ivAvatar.setImageBitmap(bitmap);
                            ivAvatar.setPadding(0, 0, 0, 0); // Remove padding if image is set
                        }
                    }
                });
    }

    private void loadItems() {
        String sellerId = mAuth.getCurrentUser().getUid();
        db.collection("items")
                .whereEqualTo("sellerId", sellerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    itemList.clear();
                    int totalStock = 0;
                    double totalValue = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Item item = document.toObject(Item.class);
                        itemList.add(item);
                        totalStock += item.getStock();
                        totalValue += item.getPrice();
                    }
                    adapter.notifyDataSetChanged();
                    updateStats(itemList.size(), totalStock, totalValue);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateStats(int count, int stock, double totalValue) {
        setStatCard(statTotalListings, String.valueOf(count), "LISTINGS");
        setStatCard(statTotalStock, String.valueOf(stock), "STOCK");
        setStatCard(statTotalOrders, "0", "ORDERS");

        double avg = count > 0 ? totalValue / count : 0;
        setStatCard(statAvgPrice, String.format(Locale.getDefault(), "₹%.0f", avg), "AVG. PRICE");
        setStatCard(statTotalRevenue, "₹0", "REVENUE");
    }

    private void setStatCard(View cardView, String value, String label) {
        TextView tvValue = cardView.findViewById(R.id.tvStatValue);
        TextView tvLabel = cardView.findViewById(R.id.tvStatLabel);
        tvValue.setText(value);
        tvLabel.setText(label);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return true;
        } else if (id == R.id.action_sales) {
            startActivity(new Intent(MainActivity.this, OrdersActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}