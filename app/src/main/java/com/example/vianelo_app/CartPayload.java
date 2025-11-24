package com.example.vianelo_app;

import java.util.List;

public class CartPayload {
        public List<CartLine> items;
        public double total;

        public static class CartLine {
            public String productId;
            public int quantity;
            public double price;
        }
    }


