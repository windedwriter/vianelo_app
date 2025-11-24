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

public class CartRepository {
    private final CartDao dao;
    private final ApiService api;
    private final Executor exec = Executors.newSingleThreadExecutor();

    public CartRepository(Context ctx) {
        this.dao = AppDatabase.get(ctx).cartDao();
        this.api = ApiClient.get();
    }

    public LiveData<List<CartItem>> observeCart() { return dao.observeAll(); }
    public void clearLocal() { exec.execute(dao::clear); }

    private String buildOptionsKey(String size, String milk, String extras) {
        String s = size  == null ? "" : size.trim().toLowerCase();
        String m = milk  == null ? "" : milk.trim().toLowerCase();
        String e = extras== null ? "" : extras.trim().toLowerCase();
        return s + "|" + m + "|" + e;
    }


    public void addToCart(Product p, int qty, String size, String milk, String extras, String note, double finalUnitPrice) {
        exec.execute(() -> {
            String key = buildOptionsKey(size, milk, extras);
            CartItem item = dao.findByProductIdAndKey(p.id, key);
            if (item == null) {
                item = new CartItem();
                item.productId = p.id;
                item.nameBase  = p.name;
                item.size      = size;
                item.milk      = milk;
                item.extras    = extras;
                item.note      = note;
                item.optionsKey= key;
                item.price     = finalUnitPrice;
                item.quantity  = qty;
                item.imageUrl  = p.imageUrl;
                dao.insert(item);
            } else {
                item.quantity += qty;
                item.price = finalUnitPrice;
                dao.update(item);
            }
            // (Opcional) api.addItem(...)
        });
    }


    public void updateOptions(long cartId, String size, String milk, String extras, String note, double newUnitPrice) {
        exec.execute(() -> {
            CartItem item = dao.findById(cartId);
            if (item == null) return;

            String newKey = buildOptionsKey(size, milk, extras);
            CartItem existing = dao.findByProductIdAndKey(item.productId, newKey);

            if (existing != null && existing.id != item.id) {
                existing.quantity += item.quantity;
                existing.price     = newUnitPrice;
                existing.note      = note;
                dao.update(existing);
                dao.deleteById(item.id);
            } else {
                item.size = size;
                item.milk = milk;
                item.extras = extras;
                item.note = note;
                item.optionsKey = newKey;
                item.price = newUnitPrice;
                dao.update(item);
            }
            // (Opcional) api.updateItemOptions(...)
        });
    }

    public void updateQty(String productId, int qty) {
        exec.execute(() -> {
            dao.updateQty(productId, qty);
            CartPayload.CartLine line = new CartPayload.CartLine();
            line.productId = productId;
            line.quantity = qty;
            line.price = 0;
            api.updateItem(line).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                @Override public void onFailure(Call<Void> c, Throwable t) {}
            });
        });
    }

    public void remove(String productId) {
        exec.execute(() -> {
            dao.deleteByProductId(productId);
            Map<String,String> body = new HashMap<>();
            body.put("productId", productId);
            api.removeItem(body).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                @Override public void onFailure(Call<Void> c, Throwable t) {}
            });
        });
    }

    public void pullFromServer(Consumer<String> uiFeedback) {
        api.getCart().enqueue(new Callback<CartPayload>() {
            @Override public void onResponse(Call<CartPayload> c, Response<CartPayload> r) {
                if (!r.isSuccessful() || r.body()==null) {
                    if (uiFeedback!=null) uiFeedback.accept("No se pudo descargar carrito");
                    return;
                }
                exec.execute(() -> {
                    dao.clear();
                    for (CartPayload.CartLine line : r.body().items) {
                        CartItem ci = new CartItem();
                        ci.productId = line.productId;
                        ci.nameBase  = "";
                        ci.size      = null;
                        ci.milk      = null;
                        ci.extras    = null;
                        ci.note      = null;
                        ci.optionsKey= "";
                        ci.price     = line.price;
                        ci.quantity  = line.quantity;
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
    public void updateQty(String productId, String optionsKey, int qty) {
        exec.execute(() -> {
            dao.updateQtyByKey(productId, optionsKey, qty);
        });
    }


    private CartItem findByProductId(String pid) {
        for (CartItem ci : dao.getAllNow()) if (ci.productId.equals(pid)) return ci;
        return null;
    }
}
