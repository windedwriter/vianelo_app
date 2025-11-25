package com.example.vianelo_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import android.widget.Button;
import com.google.firebase.auth.FirebaseAuth;


public class HomeActivity extends AppCompatActivity {

    private RecyclerView rv;
    private CatalogAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.catalogo);

        // TOOLBAR (con carrito)
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_cart) {
                startActivity(new Intent(HomeActivity.this, CartActivity.class));
                return true;
            }
            return false;
        });
        Button btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            Intent i = new Intent(HomeActivity.this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });


        // FIREBASE
        db = FirebaseFirestore.getInstance();

        // RECYCLER DEL CATALOGO
        rv = findViewById(R.id.rvCatalog);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CatalogAdapter(p -> addToCart(p));
        rv.setAdapter(adapter);

        loadCatalog();
    }

    // =================== CARGA CATALOGO ===================
    private void loadCatalog() {
        db.collection("products")
                .get()
                .addOnSuccessListener(snaps -> {
                    List<Product> list = new ArrayList<>();

                    for (DocumentSnapshot d : snaps) {
                        Product p = d.toObject(Product.class);
                        if (p != null) {
                            p.id = d.getId();
                            list.add(p);
                        }
                    }

                    adapter.submit(list);
                });
    }

    // =================== AGREGAR AL CARRITO ===================
    private void addToCart(Product p) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("carts")
                .document(uid)
                .collection("items")
                .document(p.id)
                .get()
                .addOnSuccessListener(doc -> {
                    int newQty = 1;
                    if (doc.exists()) {
                        Long q = doc.getLong("quantity");
                        if (q != null) newQty = q.intValue() + 1;
                    }

                    CartItem item = new CartItem();
                    item.productId = p.id;
                    item.nameBase = p.name;
                    item.price = p.price;
                    item.quantity = newQty;
                    item.imageUrl = p.imageUrl;

                    db.collection("carts")
                            .document(uid)
                            .collection("items")
                            .document(p.id)
                            .set(item);
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

}
