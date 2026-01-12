package com.example.vianelo_app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.ArrayAdapter;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import android.content.Intent;
import android.net.Uri;
import androidx.browser.customtabs.CustomTabsIntent;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {
    private TextView tvSubtotal, tvTotal, tvDeliveryFee;
    private MaterialButtonToggleGroup toggleFulfillment;

    private List<CartItem> currentItems = new ArrayList<>();

    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private double lastTotal = 0;
    private MaterialButton btnCheckout;
    private FirebaseFunctions functions;





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
        functions = FirebaseFunctions.getInstance();

        btnCheckout = findViewById(R.id.btnCheckout);
        btnCheckout.setOnClickListener(v -> startPaypalCheckout());
        handlePaypalReturn(getIntent());
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
                    if (e instanceof com.google.firebase.functions.FirebaseFunctionsException) {
                        com.google.firebase.functions.FirebaseFunctionsException fex =
                                (com.google.firebase.functions.FirebaseFunctionsException) e;

                        String msg = "Functions error: " + fex.getCode() + " | " + fex.getMessage();
                        android.util.Log.e("PAYPAL", msg);
                        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();
                    } else {
                        android.util.Log.e("PAYPAL", "Other error: " + e.getMessage());
                        android.widget.Toast.makeText(this, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                    }
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
        lastTotal = total;

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
    private void startPaypalCheckout() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            android.widget.Toast.makeText(this, "Inicia sesión para pagar", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (lastTotal <= 0) return;

        String amount = String.format(Locale.US, "%.2f", lastTotal);

        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("currency", "MXN");
        data.put("returnUrl", "vianelo://paypal-return");
        data.put("cancelUrl", "vianelo://paypal-cancel");

        functions.getHttpsCallable("paypalCreateOrder")
                .call(data)
                .addOnSuccessListener(res -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> r = (Map<String, Object>) res.getData();

                    String approvalUrl = (String) r.get("approvalUrl");
                    String orderId = (String) r.get("orderId");

                    if (approvalUrl != null && orderId != null) {
                        openPaypalApproval(approvalUrl);
                    }
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }
    private void openPaypalApproval(String approvalUrl) {
        CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
        intent.launchUrl(this, Uri.parse(approvalUrl));
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handlePaypalReturn(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handlePaypalReturn(getIntent());
    }

    private void handlePaypalReturn(Intent intent) {
        Uri data = intent.getData();
        if (data == null) return;


        if ("vianelo".equals(data.getScheme()) && data.getHost() != null && data.getHost().equals("paypal-return")) {
            String orderId = data.getQueryParameter("token");
            if (orderId != null) {
                intent.setData(null);
                capturePaypalOrder(orderId);
            }
        }
            if ("vianelo".equals(data.getScheme()) && "paypal-cancel".equals(data.getHost())) {
                intent.setData(null);
        }
    }
    private void capturePaypalOrder(String orderId) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);

        functions.getHttpsCallable("paypalCaptureOrder")
                .call(data)
                .addOnSuccessListener(res -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> r = (Map<String, Object>) res.getData();
                    String status = (String) r.get("status");

                    if ("COMPLETED".equals(status)) {
                        clearCart();
                    }
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }
    private void clearCart() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("carts").document(uid).collection("items").get()
                .addOnSuccessListener(snaps -> {
                    for (DocumentSnapshot d : snaps) {
                        d.getReference().delete();
                    }
                    loadCartFromFirebase();
                });
    }




}
