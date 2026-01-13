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
    private boolean isProcessingReturn = false;

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

        Log.d("FIREBASE", "Project ID: " +
                com.google.firebase.FirebaseApp.getInstance().getOptions().getProjectId());

        // Inicializar Functions con la región correcta
        functions = FirebaseFunctions.getInstance("us-central1");

        // IMPORTANTE: Comentar esta línea si NO estás usando el emulador
        // functions.useEmulator("10.0.2.2", 5001);

        Log.d("PAYPAL", "Functions initialized for region: us-central1");
        Log.d("PAYPAL", "Functions instance: " + functions);
        Log.d("PAYPAL", "Firebase Project ID: " +
                com.google.firebase.FirebaseApp.getInstance().getOptions().getProjectId());
        Log.d("PAYPAL", "Firebase App Name: " +
                com.google.firebase.FirebaseApp.getInstance().getName());

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
            Log.w("CART", "Usuario no autenticado. No se puede cargar el carrito.");
            Toast.makeText(this, "Inicia sesión para ver tu carrito", Toast.LENGTH_SHORT).show();
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

                    Log.d("CART", "Carrito cargado: " + list.size() + " items");
                })
                .addOnFailureListener(e -> {
                    Log.e("CART", "Error cargando carrito", e);
                    Toast.makeText(this, "Error al cargar carrito: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                .addOnFailureListener(e -> {
                    Log.e("CART", "Error actualizando cantidad", e);
                    Toast.makeText(this, "Error al actualizar cantidad", Toast.LENGTH_SHORT).show();
                });
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
                .addOnSuccessListener(v -> {
                    loadCartFromFirebase();
                    Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("CART", "Error eliminando item", e);
                    Toast.makeText(this, "Error al eliminar producto", Toast.LENGTH_SHORT).show();
                });
    }

    private void renderTotals(List<CartItem> items) {
        double subtotal = 0;
        for (CartItem it : items) {
            subtotal += it.price * it.quantity;
        }

        lastTotal = subtotal;

        tvSubtotal.setText("Subtotal: " + toMXN(subtotal));
        tvTotal.setText("Total: " + toMXN(subtotal));

        // Deshabilitar botón si el carrito está vacío
        btnCheckout.setEnabled(subtotal > 0);
    }

    private String toMXN(double value) {
        java.text.NumberFormat f = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es", "MX"));
        return f.format(value);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void startPaypalCheckout() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Inicia sesión para continuar", Toast.LENGTH_SHORT).show();
            return;
        }

        if (lastTotal <= 0) {
            Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        // Deshabilitar botón mientras se procesa
        btnCheckout.setEnabled(false);
        btnCheckout.setText("Procesando...");

        String amount = String.format(Locale.US, "%.2f", lastTotal);

        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("currency", "MXN");
        data.put("returnUrl", "vianelo://paypal-return");
        data.put("cancelUrl", "vianelo://paypal-cancel");

        Log.d("PAYPAL", "Llamando a paypalCreateOrder con amount=" + amount);

        functions.getHttpsCallable("paypalCreateOrder")
                .call(data)
                .addOnSuccessListener(res -> {
                    btnCheckout.setEnabled(true);
                    btnCheckout.setText("Pagar con PayPal");

                    Log.d("PAYPAL", "✅ Respuesta recibida");
                    Log.d("PAYPAL", "Raw response data class: " +
                            (res.getData() != null ? res.getData().getClass().getName() : "null"));
                    Log.d("PAYPAL", "Raw response data: " + res.getData());

                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) res.getData();

                    if (result == null) {
                        Toast.makeText(this, "Respuesta vacía del servidor", Toast.LENGTH_LONG).show();
                        Log.e("PAYPAL", "❌ result es null");
                        return;
                    }

                    String approvalUrl = (String) result.get("approvalUrl");
                    String orderId = (String) result.get("orderId");
                    Object status = result.get("status");

                    Log.d("PAYPAL", "Orden creada: " + orderId);
                    Log.d("PAYPAL", "URL de aprobación: " + approvalUrl);
                    Log.d("PAYPAL", "Status: " + status);
                    Log.d("PAYPAL", "Respuesta completa: " + result);
                    Log.d("PAYPAL", "Todas las keys: " + result.keySet());

                    if (approvalUrl != null && !approvalUrl.isEmpty()) {
                        openPaypalApproval(approvalUrl);
                    } else {
                        Toast.makeText(this, "Error: No se recibió URL de pago", Toast.LENGTH_LONG).show();
                        Log.e("PAYPAL", "❌ approvalUrl es null o vacía");
                    }
                })
                .addOnFailureListener(e -> {
                    btnCheckout.setEnabled(true);
                    btnCheckout.setText("Pagar con PayPal");

                    Log.e("PAYPAL", "Error en paypalCreateOrder", e);

                    String errorMsg = "Error al crear orden";

                    if (e instanceof FirebaseFunctionsException) {
                        FirebaseFunctionsException fex = (FirebaseFunctionsException) e;
                        FirebaseFunctionsException.Code code = fex.getCode();
                        String message = fex.getMessage();
                        Object details = fex.getDetails();

                        Log.e("PAYPAL", "Code: " + code);
                        Log.e("PAYPAL", "Message: " + message);
                        Log.e("PAYPAL", "Details: " + details);

                        switch (code) {
                            case UNAUTHENTICATED:
                                errorMsg = "Error de autenticación. Vuelve a iniciar sesión.";
                                break;
                            case PERMISSION_DENIED:
                                errorMsg = "Permisos insuficientes. Contacta a soporte.";
                                break;
                            case INVALID_ARGUMENT:
                                errorMsg = "Datos inválidos: " + message;
                                break;
                            case INTERNAL:
                                errorMsg = "Error del servidor: " + message;
                                break;
                            default:
                                errorMsg = "Error: " + message;
                        }
                    }

                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                });
    }

    private void openPaypalApproval(String approvalUrl) {
        try {
            // Usar navegador externo en lugar de Custom Tabs
            // Custom Tabs tiene problemas con deep links en PayPal Sandbox
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(approvalUrl));

            // Asegurar que se abra en navegador externo, no en la app
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(browserIntent);

            Log.d("PAYPAL", "✅ Abriendo PayPal en navegador externo: " + approvalUrl);
            Toast.makeText(this, "Redirigiendo a PayPal...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("PAYPAL", "❌ Error abriendo navegador", e);
            Toast.makeText(this, "Error al abrir PayPal: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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

        // Debug: Log del intent
        Intent intent = getIntent();
        Log.d("PAYPAL", "onResume - Intent action: " + intent.getAction());
        Log.d("PAYPAL", "onResume - Intent data: " + intent.getData());
        Log.d("PAYPAL", "onResume - Intent extras: " + intent.getExtras());

        if (!isProcessingReturn) {
            handlePaypalReturn(intent);
        }
    }

    private void handlePaypalReturn(Intent intent) {
        if (isProcessingReturn) {
            Log.d("PAYPAL", "Ya se está procesando un retorno, ignorando...");
            return;
        }

        Uri data = intent.getData();
        if (data == null) {
            Log.d("PAYPAL", "Intent data es null, no hay deep link");
            return;
        }

        String scheme = data.getScheme();
        String host = data.getHost();

        Log.d("PAYPAL", "Deep link recibido - Scheme: " + scheme + ", Host: " + host);
        Log.d("PAYPAL", "URI completo: " + data.toString());

        if (!"vianelo".equals(scheme) || host == null) {
            Log.d("PAYPAL", "Scheme o host no coincide, ignorando");
            return;
        }

        // Marcar como procesando
        isProcessingReturn = true;

        // Limpiar el intent inmediatamente para evitar reprocesamiento
        intent.setData(null);

        if ("paypal-return".equals(host)) {
            // PayPal envía el orderId como parámetro "token"
            String orderId = data.getQueryParameter("token");

            // También puede venir como "ba_token" en algunos casos
            if (orderId == null || orderId.isEmpty()) {
                orderId = data.getQueryParameter("ba_token");
            }

            Log.d("PAYPAL", "🔄 Retorno exitoso de PayPal con token: " + orderId);

            if (orderId != null && !orderId.isEmpty()) {
                // Mostrar mensaje de procesamiento
                Toast.makeText(this, "Procesando pago...", Toast.LENGTH_SHORT).show();
                capturePaypalOrder(orderId);
            } else {
                Log.e("PAYPAL", "❌ orderId es null o vacío en el retorno");
                Toast.makeText(this, "Error: No se recibió ID de orden", Toast.LENGTH_SHORT).show();
                isProcessingReturn = false;
            }
        } else if ("paypal-cancel".equals(host)) {
            Log.d("PAYPAL", "❌ Pago cancelado por el usuario");
            Toast.makeText(this, "Pago cancelado", Toast.LENGTH_SHORT).show();
            isProcessingReturn = false;
        } else {
            Log.w("PAYPAL", "Host desconocido: " + host);
            isProcessingReturn = false;
        }
    }

    private void capturePaypalOrder(String orderId) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);

        Log.d("PAYPAL", "Capturando orden: " + orderId);

        functions.getHttpsCallable("paypalCaptureOrder")
                .call(data)
                .addOnSuccessListener(res -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) res.getData();

                    if (result == null) {
                        Toast.makeText(this, "Respuesta vacía del servidor", Toast.LENGTH_LONG).show();
                        Log.e("PAYPAL", "result es null en capture");
                        isProcessingReturn = false;
                        return;
                    }

                    String status = (String) result.get("status");
                    Log.d("PAYPAL", "Estado de captura: " + status);
                    Log.d("PAYPAL", "Respuesta completa: " + result);

                    if ("COMPLETED".equals(status)) {
                        Toast.makeText(this, "✅ Pago completado exitosamente", Toast.LENGTH_LONG).show();
                        clearCart();
                    } else {
                        Toast.makeText(this, "Estado del pago: " + status, Toast.LENGTH_LONG).show();
                    }

                    isProcessingReturn = false;
                })
                .addOnFailureListener(e -> {
                    Log.e("PAYPAL", "Error en paypalCaptureOrder", e);

                    String errorMsg = "Error al procesar el pago";

                    if (e instanceof FirebaseFunctionsException) {
                        FirebaseFunctionsException fex = (FirebaseFunctionsException) e;
                        errorMsg = "Error: " + fex.getMessage();
                        Log.e("PAYPAL", "Code: " + fex.getCode());
                        Log.e("PAYPAL", "Details: " + fex.getDetails());
                    }

                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    isProcessingReturn = false;
                });
    }

    private void clearCart() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d("CART", "Limpiando carrito del usuario: " + uid);

        db.collection("carts")
                .document(uid)
                .collection("items")
                .get()
                .addOnSuccessListener(snaps -> {
                    for (DocumentSnapshot d : snaps) {
                        d.getReference().delete();
                    }
                    Log.d("CART", "Carrito limpiado: " + snaps.size() + " items eliminados");
                    loadCartFromFirebase();
                    Toast.makeText(this, "Carrito limpiado", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("CART", "Error limpiando carrito", e);
                    Toast.makeText(this, "Error al limpiar carrito", Toast.LENGTH_SHORT).show();
                });
    }
}