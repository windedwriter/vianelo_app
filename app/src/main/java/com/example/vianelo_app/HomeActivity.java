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
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;


public class HomeActivity extends AppCompatActivity {

    private RecyclerView rv;
    private CatalogAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.catalogo);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);



        // FIREBASE
        db = FirebaseFirestore.getInstance();

        // RECYCLER DEL CATALOGO
        rv = findViewById(R.id.rvCatalog);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CatalogAdapter(p -> addToCart(p));
        rv.setAdapter(adapter);

        loadCatalog();
    }

    //  CARGA CATALOGO
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

    /**
     Agrega un producto seleccionado al carrito de compras del usuario en Firestore.

     Este método primero obtiene el UID del usuario actual. Luego verifica si el producto ya existe en el carrito del usuario. Si existe, la cantidad del artículo se incrementa en uno. Si no existe, se crea un nuevo elemento en el carrito con una cantidad de uno.

     El artículo nuevo o actualizado —que incluye detalles del producto como ID, nombre, precio y URL de la imagen— se guarda o actualiza en la subcolección "items" dentro del documento específico del carrito del usuario en la colección "carts".

     @param p El objeto {@link Product} que se agregará al carrito.
     */ //  AGREGAR AL CARRITO
    private void addToCart(Product p) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("carts")
                .document(uid)
                .collection("items")
                .document(p.id)
                .get()
                .addOnSuccessListener(doc -> {
                    Toast("Producto agregado al carrito");
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
    private void Toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_cart) {
            startActivity(new Intent(this, CartActivity.class));
            return true;
        }

        if (item.getItemId() == R.id.profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_orders) {
            startActivity(new Intent(this, OrdersActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
