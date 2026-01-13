package com.example.vianelo_app;

import com.google.firebase.Timestamp;
import java.util.List;
import java.util.Map;

public class Order {
    public String id;
    public String branchId;
    public String paypalOrderId;
    public double total;
    public String status; // "recibido", "en_preparacion", "listo", "entregado", "cancelado"
    public String paymentStatus; // "paid", "pending"
    public String userId;
    public Timestamp createdAt;
    public Timestamp receivedAt;
    public Timestamp readyAt;
    public Timestamp deliveredAt;
    public Timestamp updatedAt;
    public List<Map<String, Object>> items;

    public Order() {
        // Constructor vacío requerido por Firestore
    }

    public String getFormattedAmount() {
        java.text.NumberFormat formatter =
                java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es", "MX"));
        return formatter.format(total);
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
        if (paypalOrderId == null || paypalOrderId.isEmpty()) {
            return id != null ? id.substring(0, Math.min(8, id.length())) : "N/A";
        }
        return paypalOrderId.length() > 10 ? paypalOrderId.substring(0, 10) + "..." : paypalOrderId;
    }

    public String getBranchName() {
        if (branchId == null) return "Sucursal";

        switch (branchId.toLowerCase()) {
            case "quintas":
                return "Sucursal Quintas";
            case "chapule":
                return "Sucursal Chapule";
            default:
                return "Sucursal " + branchId;
        }
    }

    public String getStatusText() {
        if (status == null) return "Desconocido";

        switch (status.toLowerCase()) {
            case "recibido":
                return "Recibido";
            case "en_preparacion":
                return "En preparación";
            case "listo":
                return "Listo para recoger";
            case "entregado":
                return "Entregado";
            case "cancelado":
                return "Cancelado";
            default:
                return status;
        }
    }

    public int getStatusColor() {
        if (status == null) return android.R.color.darker_gray;

        switch (status.toLowerCase()) {
            case "recibido":
                return android.R.color.holo_orange_light;
            case "en_preparacion":
                return android.R.color.holo_blue_light;
            case "listo":
                return android.R.color.holo_purple;
            case "entregado":
                return android.R.color.holo_green_dark;
            case "cancelado":
                return android.R.color.holo_red_dark;
            default:
                return android.R.color.darker_gray;
        }
    }

    public int getStatusBackgroundColor() {
        if (status == null) return 0xFFEEEEEE;

        switch (status.toLowerCase()) {
            case "recibido":
                return 0xFFFFF3E0; // Naranja claro
            case "en_preparacion":
                return 0xFFE3F2FD; // Azul claro
            case "listo":
                return 0xFFF3E5F5; // Morado claro
            case "entregado":
                return 0xFFE8F5E9; // Verde claro
            case "cancelado":
                return 0xFFFFEBEE; // Rojo claro
            default:
                return 0xFFEEEEEE; // Gris claro
        }
    }

    public boolean isActive() {
        if (status == null) return false;
        String statusLower = status.toLowerCase();
        return statusLower.equals("recibido") ||
                statusLower.equals("en_preparacion") ||
                statusLower.equals("listo");
    }

    public boolean isCompleted() {
        return "entregado".equalsIgnoreCase(status);
    }

    public int getItemCount() {
        if (items == null) return 0;
        int count = 0;
        for (Map<String, Object> item : items) {
            Object qtyObj = item.get("qty");
            if (qtyObj instanceof Long) {
                count += ((Long) qtyObj).intValue();
            } else if (qtyObj instanceof Integer) {
                count += (Integer) qtyObj;
            } else if (qtyObj instanceof Double) {
                count += ((Double) qtyObj).intValue();
            }
        }
        return count;
    }

    public String getItemsSummary() {
        if (items == null || items.isEmpty()) return "Sin items";

        int totalItems = getItemCount();
        if (items.size() == 1) {
            Map<String, Object> item = items.get(0);
            String name = (String) item.get("name");
            return name != null ? name : "1 item";
        }

        return totalItems + " item" + (totalItems != 1 ? "s" : "");
    }
}