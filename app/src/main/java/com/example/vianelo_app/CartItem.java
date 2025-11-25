package com.example.vianelo_app;

import androidx.annotation.NonNull;

public class CartItem {


    public long id;

    @NonNull public String productId;
    @NonNull public String nameBase;
    public String size;
    public String milk;
    public String extras;
    public String note;
    @NonNull public String optionsKey;
    public double price;
    public int quantity;
    public String imageUrl;


    public CartItem() {
    }


    public String getDisplayName() {
        StringBuilder sb = new StringBuilder(nameBase);
        if (size != null && !size.isEmpty()) sb.append(" • ").append(size);
        if (milk != null && !milk.isEmpty()) sb.append(" • ").append(milk);
        if (extras != null && !extras.isEmpty()) sb.append(" • ").append(extras);
        return sb.toString();
    }

    public double getSubtotal(){ return price * quantity; }
}
