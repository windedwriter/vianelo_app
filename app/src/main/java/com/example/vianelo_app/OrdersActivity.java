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
import java.util.Map;

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
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);

                    orders.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Order order = new Order();
                            order.id = document.getId();
                            order.branchId = document.getString("branchId");
                            order.paypalOrderId = document.getString("paypalOrderId");
                            order.status = document.getString("status");
                            order.paymentStatus = document.getString("paymentStatus");
                            order.userId = document.getString("userId");

                            // Manejar total
                            Object totalObj = document.get("total");
                            if (totalObj instanceof Double) {
                                order.total = (Double) totalObj;
                            } else if (totalObj instanceof Long) {
                                order.total = ((Long) totalObj).doubleValue();
                            }

                            // Timestamps
                            order.createdAt = document.getTimestamp("createdAt");
                            order.receivedAt = document.getTimestamp("receivedAt");
                            order.readyAt = document.getTimestamp("readyAt");
                            order.deliveredAt = document.getTimestamp("deliveredAt");
                            order.updatedAt = document.getTimestamp("updatedAt");

                            // Items
                            order.items = (List<Map<String, Object>>) document.get("items");

                            // Filtrar según la tab seleccionada
                            boolean shouldAdd = false;
                            if ("all".equals(filter)) {
                                shouldAdd = true;
                            } else if ("active".equals(filter)) {
                                shouldAdd = order.isActive();
                            } else if ("completed".equals(filter)) {
                                shouldAdd = order.isCompleted();
                            }

                            if (shouldAdd) {
                                orders.add(order);
                            }

                            Log.d("ORDERS", "Pedido: " + order.id + " - $" + order.total + " - " + order.status);
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

                    Log.d("ORDERS", "Total pedidos mostrados: " + orders.size());
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