package com.example.vianelo_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {

    private final List<Order> orders;

    public OrdersAdapter(List<Order> orders) {
        this.orders = orders;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);

        holder.tvOrderId.setText("Pedido #" + order.getShortOrderId());
        holder.tvAmount.setText(order.getFormattedAmount());
        holder.tvDate.setText(order.getFormattedDate());
        holder.tvStatus.setText(order.getStatusText());

        // Color del badge según el estado
        int textColor = holder.itemView.getContext().getResources().getColor(order.getStatusColor(), null);
        int backgroundColor = order.getStatusBackgroundColor();

        holder.tvStatus.setTextColor(textColor);
        holder.tvStatus.setBackgroundColor(backgroundColor);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvOrderId;
        TextView tvAmount;
        TextView tvDate;
        TextView tvStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}