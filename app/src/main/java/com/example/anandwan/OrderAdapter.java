package com.example.anandwan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public OrderAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_row, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.tvOrderTitle.setText(order.getItemName());
        holder.tvOrderDetails.setText("Buyer: " + order.getBuyerName() + " | Qty: " + order.getQuantity());
        holder.tvOrderTotal.setText("Total: ₹" + order.getTotalPrice());
        
        if (order.getTimestamp() != null) {
            holder.tvOrderDate.setText("Date: " + dateFormat.format(order.getTimestamp().toDate()));
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderDate, tvOrderTitle, tvOrderDetails, tvOrderTotal;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderTitle = itemView.findViewById(R.id.tvOrderTitle);
            tvOrderDetails = itemView.findViewById(R.id.tvOrderDetails);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
        }
    }
}