package com.example.vianelo_app;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CartAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cart);

        recyclerView = findViewById(R.id.rvCart);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CartAdapter(new CartAdapter.Actions() {
            @Override
            public void onPlus(@NonNull CartItem it) {
                updateQty(it, it.quantity + 1);
            }

            @Override
            public void onMinus(@NonNull CartItem it) {
                if (it.quantity > 1) {
                    updateQty(it, it.quantity - 1);
                }
            }

            @Override
            public void onRemove(@NonNull CartItem it) {
                removeItem(it);
            }
        });

        recyclerView.setAdapter(adapter);


        loadCartFromFirebase();
    }


    private void loadCartFromFirebase() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("carts")
                .document(uid)
                .collection("items")
                .get()
                .addOnSuccessListener(snaps -> {
                    List<CartItem> list = new ArrayList<>();

                    for (DocumentSnapshot d : snaps) {
                        CartItem ci = d.toObject(CartItem.class);
                        if (ci != null) {
                            list.add(ci);
                        }
                    }

                    adapter.submit(list);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });
    }


    private void updateQty(@NonNull CartItem it, int newQty) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("carts")
                .document(uid)
                .collection("items")
                .document(it.productId)
                .update("quantity", newQty)
                .addOnSuccessListener(v -> loadCartFromFirebase());
    }


    private void removeItem(@NonNull CartItem it) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("carts")
                .document(uid)
                .collection("items")
                .document(it.productId)
                .delete()
                .addOnSuccessListener(v -> loadCartFromFirebase());
    }
}
