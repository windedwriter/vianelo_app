package com.example.vianelo_app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.ArrayAdapter;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {
    private TextView tvSubtotal, tvTotal, tvDeliveryFee;
    private MaterialButtonToggleGroup toggleFulfillment;

    private List<CartItem> currentItems = new ArrayList<>();

    private RecyclerView recyclerView;
    private CartAdapter adapter;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cart);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        tvSubtotal=findViewById(R.id.tvSubtotal);
        tvTotal=findViewById(R.id.tvTotal);
        toggleFulfillment=findViewById(R.id.toggleFulfillment);
        MaterialAutoCompleteTextView spinnerAddress = findViewById(R.id.spinnerAddress);

        String[] sucursales = new String[]{
                "Sucursal Chapule",
                "Sucursal Quintas"
        };

        ArrayAdapter<String> ad = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                sucursales
        );
        spinnerAddress.setAdapter(ad);


        spinnerAddress.setText(sucursales[0], false);

        spinnerAddress.setOnItemClickListener((parent, view, position, id) -> {
            String seleccion = sucursales[position];

        });


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

                    currentItems = list;
                    adapter.submit(list);
                    renderTotals(list);
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
    private void renderTotals(List<CartItem> items) {
        double subtotal = 0;
        for (CartItem it : items) {
            subtotal += it.price * it.quantity;
        }

        double total = subtotal ;

        tvSubtotal.setText("Subtotal: " + toMXN(subtotal));
        tvTotal.setText("Total: " + toMXN(total));
    }

    private String toMXN(double value){
        java.text.NumberFormat f =
                java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es","MX"));
        return f.format(value);
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }


}
