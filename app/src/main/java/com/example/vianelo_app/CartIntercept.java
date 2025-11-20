package com.example.vianelo_app;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class CartIntercept {
    private final CartDao dao;
    private final ApiService api;
    private final Executor exec = Executors.newSingleThreadExecutor();

    public CartIntercept(Context ctx) {
        this.dao = AppDatabase.get(ctx).cartDao();
        this.api = ApiClient.get();
    }

    public LiveData<List<CartItem>> observeCart() { return dao.observeAll(); }
    public void clearLocal() { exec.execute(dao::clear); }

    public void addToCart(Product p, int qty) {
        exec.execute(() -> {
            CartItem item = findByProductId(p.id);
            if (item == null) {
                item = new CartItem();
                item.productId = p.id;
                item.nameBase = p.name;
                item.price = p.price;
                item.quantity = qty;
                item.imageUrl = p.imageUrl;
                dao.insert(item);
            } else {
                item.quantity += qty;
                dao.update(item);
            }
            // opcional: enviar a API rápido (fire-and-forget)
            CartPayload.CartLine line = new CartPayload.CartLine();
            line.productId = p.id; line.quantity = qty; line.price = p.price;
            api.addItem(line).enqueue(new Callback<Void>() { public void onResponse(Call<Void> c, Response<Void> r) {}
                public void onFailure(Call<Void> c, Throwable t) {} });
        });
    }

    public void updateQty(String productId, int qty) {
        exec.execute(() -> {
            dao.updateQty(productId, qty);
            CartPayload.CartLine line = new CartPayload.CartLine();
            line.productId = productId; line.quantity = qty; line.price = 0;
            api.updateItem(line).enqueue(new Callback<Void>() { public void onResponse(Call<Void> c, Response<Void> r) {}
                public void onFailure(Call<Void> c, Throwable t) {} });
        });
    }

    public void remove(String productId) {
        exec.execute(() -> {
            dao.deleteByProductId(productId);
            Map<String,String> body = new HashMap<>();
            body.put("productId", productId);
            api.removeItem(body).enqueue(new Callback<Void>() { public void onResponse(Call<Void> c, Response<Void> r) {}
                public void onFailure(Call<Void> c, Throwable t) {} });
        });
    }

    public void pullFromServer(Consumer<String> uiFeedback) {
        api.getCart().enqueue(new Callback<CartPayload>() {
            @Override public void onResponse(Call<CartPayload> c, Response<CartPayload> r) {
                if (!r.isSuccessful() || r.body()==null) { if (uiFeedback!=null) uiFeedback.accept("No se pudo descargar carrito"); return; }
                exec.execute(() -> {
                    dao.clear();
                    for (CartPayload.CartLine line : r.body().items) {
                        CartItem ci = new CartItem();
                        ci.productId = line.productId;
                        ci.nameBase = "";
                        ci.price = line.price;
                        ci.quantity = line.quantity;
                        dao.insert(ci);
                    }
                    if (uiFeedback!=null) uiFeedback.accept("Carrito descargado");
                });
            }
            @Override public void onFailure(Call<CartPayload> c, Throwable t) {
                if (uiFeedback!=null) uiFeedback.accept("Error red: " + t.getMessage());
            }
        });
    }

    public void pushLocalToServer(Consumer<String> uiFeedback) {
        exec.execute(() -> {
            List<CartItem> local = dao.getAllNow();
            CartPayload payload = new CartPayload();
            payload.items = new ArrayList<>();
            double total = 0;
            for (CartItem it : local) {
                CartPayload.CartLine l = new CartPayload.CartLine();
                l.productId = it.productId;
                l.quantity = it.quantity;
                l.price = it.price;
                payload.items.add(l);
                total += it.getSubtotal();
            }
            payload.total = total;

            api.syncCart(payload).enqueue(new Callback<CartPayload>() {
                @Override public void onResponse(Call<CartPayload> c, Response<CartPayload> r) {
                    if (uiFeedback!=null)
                        uiFeedback.accept(r.isSuccessful() ? "Carrito sincronizado" : "Fallo al sincronizar");
                }
                @Override public void onFailure(Call<CartPayload> c, Throwable t) {
                    if (uiFeedback!=null) uiFeedback.accept("Error red: " + t.getMessage());
                }
            });
        });
    }

    private CartItem findByProductId(String pid) {
        for (CartItem ci : dao.getAllNow()) if (ci.productId.equals(pid)) return ci;
        return null;
    }
}
