package com.example.anandwan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private List<Item> itemList;
    private boolean isInventoryView = false;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ItemAdapter(List<Item> itemList) {
        this.itemList = itemList;
    }

    public ItemAdapter(List<Item> itemList, boolean isInventoryView) {
        this.itemList = itemList;
        this.isInventoryView = isInventoryView;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isInventoryView ? R.layout.item_row_inventory : R.layout.item_row;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        holder.tvName.setText(item.getName());
        holder.tvPrice.setText("₹" + item.getPrice());
        holder.tvStock.setText("Stock: " + item.getStock());
        holder.tvCategory.setText(item.getCategory() != null ? item.getCategory() : "Uncategorized");

        if (isInventoryView && holder.tvDescription != null) {
            holder.tvDescription.setText(item.getDescription());
        }

        if (item.getImageEncoded() != null) {
            byte[] decodedBytes = Base64.decode(item.getImageEncoded(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            holder.ivThumbnail.setImageBitmap(bitmap);
        } else {
            holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.btnMenu.setOnClickListener(v -> showPopupMenu(v, item, position));
    }

    private void showPopupMenu(View view, Item item, int position) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.getMenu().add("Edit");
        popup.getMenu().add("Delete");

        popup.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getTitle().equals("Edit")) {
                Intent intent = new Intent(view.getContext(), AddItemActivity.class);
                intent.putExtra("edit_item_id", item.getId());
                view.getContext().startActivity(intent);
            } else if (menuItem.getTitle().equals("Delete")) {
                confirmDelete(view.getContext(), item, position);
            }
            return true;
        });
        popup.show();
    }

    private void confirmDelete(Context context, Item item, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Listing")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("items").document(item.getId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                itemList.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, itemList.size());
                                Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvName, tvPrice, tvStock, tvCategory, tvDescription;
        ImageButton btnMenu;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivItemThumbnail);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvPrice = itemView.findViewById(R.id.tvItemPrice);
            tvStock = itemView.findViewById(R.id.tvItemStock);
            tvCategory = itemView.findViewById(R.id.tvItemCategory);
            tvDescription = itemView.findViewById(R.id.tvItemDescription);
            btnMenu = itemView.findViewById(R.id.btnItemMenu);
        }
    }
}