package com.example.vianelo_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        Log.d("APP_CHECK", "applicationId=" + BuildConfig.APPLICATION_ID);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.cart);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTotal = findViewById(R.id.tvTotal);
        toggleFulfillment = findViewById(R.id.toggleFulfillment);
        Log.d("FBASE", "androidProjectId=" +
                com.google.firebase.FirebaseApp.getInstance()
                        .getOptions().getProjectId());



        functions = FirebaseFunctions.getInstance("us-central1");

        // Spinner sucursales
        MaterialAutoCompleteTextView spinnerAddress = findViewById(R.id.spinnerAddress);
        String[] sucursales = new String[]{"Sucursal Chapule", "Sucursal Quintas"};
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sucursales);
        spinnerAddress.setAdapter(ad);
        spinnerAddress.setText(sucursales[0], false);

        // Recycler
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

        btnCheckout = findViewById(R.id.btnCheckout);
        btnCheckout.setOnClickListener(v -> startPaypalCheckout());


        handlePaypalReturn(getIntent());

        loadCartFromFirebase();
    }

    private void loadCartFromFirebase() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w("CART", "Usuario null. No se carga carrito.");
            return;
        }

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
                        if (ci != null) list.add(ci);
                    }

                    currentItems = list;
                    adapter.submit(list);
                    renderTotals(list);
                })
                .addOnFailureListener(e -> {
                    Log.e("CART", "Firestore error cargando carrito", e);
                    Toast.makeText(this, "Error cargando carrito: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateQty(@NonNull CartItem it, int newQty) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("carts")
                .document(uid)
                .collection("items")
                .document(it.productId)
                .update("quantity", newQty)
                .addOnSuccessListener(v -> loadCartFromFirebase())
                .addOnFailureListener(e -> Log.e("CART", "updateQty error", e));
    }

    private void removeItem(@NonNull CartItem it) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("carts")
                .document(uid)
                .collection("items")
                .document(it.productId)
                .delete()
                .addOnSuccessListener(v -> loadCartFromFirebase())
                .addOnFailureListener(e -> Log.e("CART", "removeItem error", e));
    }

    private void renderTotals(List<CartItem> items) {
        double subtotal = 0;
        for (CartItem it : items) subtotal += it.price * it.quantity;

        lastTotal = subtotal;

        tvSubtotal.setText("Subtotal: " + toMXN(subtotal));
        tvTotal.setText("Total: " + toMXN(subtotal));
    }

    private String toMXN(double value) {
        java.text.NumberFormat f =
                java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es", "MX"));
        return f.format(value);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void startPaypalCheckout() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Inicia sesión para pagar", Toast.LENGTH_SHORT).show();
            return;
        }
        if (lastTotal <= 0) {
            Toast.makeText(this, "Total inválido", Toast.LENGTH_SHORT).show();
            return;
        }

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

                    Log.d("PAYPAL", "raw=" + r);
                    Log.d("PAYPAL", "version=" + r.get("version"));
                    Log.d("PAYPAL", "project=" + r.get("project"));
                    Log.d("PAYPAL", "debugTokenInUrl=" + r.get("debugTokenInUrl"));
                    Log.d("PAYPAL", "approvalUrl=" + approvalUrl);

                    if (approvalUrl != null && !approvalUrl.isEmpty()) {
                        openPaypalApproval(approvalUrl);
                    } else {
                        Toast.makeText(this, "approvalUrl null. Revisa Logcat.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PAYPAL", "paypalCreateOrder FAIL", e);

                    if (e instanceof FirebaseFunctionsException) {
                        FirebaseFunctionsException fex = (FirebaseFunctionsException) e;
                        String msg = "Functions error: " + fex.getCode() + " | " + fex.getMessage();
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
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

        String scheme = data.getScheme();
        String host = data.getHost();

        if (!"vianelo".equals(scheme) || host == null) return;

        if ("paypal-return".equals(host)) {
            String orderId = data.getQueryParameter("token"); // token = orderId
            Log.d("PAYPAL", "return token(orderId)=" + orderId);

            // ✅ evita re-disparo
            intent.setData(null);

            if (orderId != null) capturePaypalOrder(orderId);
        } else if ("paypal-cancel".equals(host)) {
            Log.d("PAYPAL", "Pago cancelado por el usuario");
            intent.setData(null);
            Toast.makeText(this, "Pago cancelado", Toast.LENGTH_SHORT).show();
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

                    Log.d("PAYPAL", "capture status=" + status + " raw=" + r);

                    if ("COMPLETED".equals(status)) {
                        Toast.makeText(this, "Pago COMPLETADO ✅", Toast.LENGTH_SHORT).show();
                        clearCart();
                    } else {
                        Toast.makeText(this, "Pago status: " + status, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PAYPAL", "paypalCaptureOrder FAIL", e);

                    if (e instanceof FirebaseFunctionsException) {
                        FirebaseFunctionsException fex = (FirebaseFunctionsException) e;
                        String msg = "Functions error: " + fex.getCode() + " | " + fex.getMessage();
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void clearCart() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("carts").document(uid).collection("items").get()
                .addOnSuccessListener(snaps -> {
                    for (DocumentSnapshot d : snaps) {
                        d.getReference().delete();
                    }
                    loadCartFromFirebase();
                })
                .addOnFailureListener(e -> Log.e("CART", "clearCart error", e));
    }
}
