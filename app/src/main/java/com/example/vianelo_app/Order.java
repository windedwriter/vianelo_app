package com.example.vianelo_app;

import com.google.firebase.Timestamp;

public class Order {
    public String id;
    public String orderId;
    public double amount;
    public String currency;
    public String status; // "pending", "processing", "completed", "cancelled"
    public Timestamp createdAt;
    public String paypalData;

    public Order() {
        // Constructor vacío requerido por Firestore
    }

    public String getFormattedAmount() {
        java.text.NumberFormat formatter =
                java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es", "MX"));
        return formatter.format(amount);
    }

    public String getFormattedDate() {
        if (createdAt == null) {
            return "Fecha desconocida";
        }

        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", new java.util.Locale("es", "MX"));
        return sdf.format(createdAt.toDate());
    }

    public String getShortOrderId() {
        if (orderId == null || orderId.isEmpty()) {
            return id != null ? id.substring(0, Math.min(8, id.length())) : "N/A";
        }
        return orderId.length() > 10 ? orderId.substring(0, 10) + "..." : orderId;
    }

    public String getStatusText() {
        if (status == null) return "Desconocido";

        switch (status.toLowerCase()) {
            case "pending":
                return "Pendiente";
            case "processing":
                return "En proceso";
            case "completed":
                return "Completado";
            case "cancelled":
                return "Cancelado";
            default:
                return status;
        }
    }

    public int getStatusColor() {
        if (status == null) return android.R.color.darker_gray;

        switch (status.toLowerCase()) {
            case "pending":
                return android.R.color.holo_orange_light;
            case "processing":
                return android.R.color.holo_blue_light;
            case "completed":
                return android.R.color.holo_green_dark;
            case "cancelled":
                return android.R.color.holo_red_dark;
            default:
                return android.R.color.darker_gray;
        }
    }

    public int getStatusBackgroundColor() {
        if (status == null) return 0xFFEEEEEE;

        switch (status.toLowerCase()) {
            case "pending":
                return 0xFFFFF3E0; // Naranja claro
            case "processing":
                return 0xFFE3F2FD; // Azul claro
            case "completed":
                return 0xFFE8F5E9; // Verde claro
            case "cancelled":
                return 0xFFFFEBEE; // Rojo claro
            default:
                return 0xFFEEEEEE; // Gris claro
        }
    }
}