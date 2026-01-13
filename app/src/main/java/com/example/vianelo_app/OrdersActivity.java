package com.example.vianelo_app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OrdersAdapter adapter;
    private ProgressBar progressBar;
    private FrameLayout emptyStateContainer;
    private TabLayout tabLayout;

    private List<Order> orders = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentFilter = "all"; // "all", "pending", "processing", "completed"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Mis Pedidos");
        }

        // Inicializar vistas
        recyclerView = findViewById(R.id.rvOrders);
        progressBar = findViewById(R.id.progressBar);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        tabLayout = findViewById(R.id.tabLayout);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrdersAdapter(orders);
        recyclerView.setAdapter(adapter);

        // Firestore
        db = FirebaseFirestore.getInstance();

        // Configurar Tabs
        setupTabs();

        // Cargar pedidos
        loadOrders(currentFilter);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Todos"));
        tabLayout.addTab(tabLayout.newTab().setText("Activos"));
        tabLayout.addTab(tabLayout.newTab().setText("Completados"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "active"; // pending + processing
                        break;
                    case 2:
                        currentFilter = "completed";
                        break;
                }
                loadOrders(currentFilter);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadOrders(String filter) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Inicia sesión para ver tus pedidos", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.GONE);

        Log.d("ORDERS", "Cargando pedidos para usuario: " + userId + " con filtro: " + filter);

        Query query = db.collection("orders")
                .whereEqualTo("userId", userId);

        // Aplicar filtro según la tab seleccionada
        if ("completed".equals(filter)) {
            query = query.whereEqualTo("status", "completed");
        } else if ("active".equals(filter)) {
            query = query.whereIn("status", java.util.Arrays.asList("pending", "processing"));
        }
        // Si es "all", no agregamos filtro de status

        query.orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);

                    orders.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Order order = new Order();
                            order.id = document.getId();
                            order.orderId = document.getString("orderId");

                            // Manejar amount
                            Object amountObj = document.get("amount");
                            if (amountObj instanceof Double) {
                                order.amount = (Double) amountObj;
                            } else if (amountObj instanceof Long) {
                                order.amount = ((Long) amountObj).doubleValue();
                            } else if (amountObj instanceof String) {
                                try {
                                    order.amount = Double.parseDouble((String) amountObj);
                                } catch (NumberFormatException e) {
                                    order.amount = 0.0;
                                }
                            }

                            order.currency = document.getString("currency");
                            order.status = document.getString("status");
                            order.createdAt = document.getTimestamp("createdAt");
                            order.paypalData = document.getString("paypalData");

                            // Filtrar manualmente para "active" si es necesario
                            if ("active".equals(filter)) {
                                if ("pending".equalsIgnoreCase(order.status) ||
                                        "processing".equalsIgnoreCase(order.status)) {
                                    orders.add(order);
                                }
                            } else {
                                orders.add(order);
                            }

                            Log.d("ORDERS", "Pedido cargado: " + order.id + " - $" + order.amount + " - " + order.status);
                        } catch (Exception e) {
                            Log.e("ORDERS", "Error parseando pedido", e);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (orders.isEmpty()) {
                        emptyStateContainer.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyStateContainer.setVisibility(View.GONE);
                    }

                    Log.d("ORDERS", "Total pedidos cargados: " + orders.size());
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    emptyStateContainer.setVisibility(View.VISIBLE);

                    Log.e("ORDERS", "Error cargando pedidos", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}