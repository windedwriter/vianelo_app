package com.example.vianelo_app;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CartDao {

    @Query("SELECT * FROM cart_items ORDER BY id DESC")
    List<CartItem> getAllNow();

    @Query("SELECT * FROM cart_items ORDER BY id DESC")
    LiveData<List<CartItem>> observeAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CartItem item);

    @Update
    void update(CartItem item);

    @Query("DELETE FROM cart_items")
    void clear();

    // Borrado genérico por productId (útil si no usas variantes, pero ojo con ambigüedad)
    @Query("DELETE FROM cart_items WHERE productId = :productId")
    void deleteByProductId(String productId);

    // ✅ Variantes seguras
    @Query("SELECT * FROM cart_items WHERE productId = :pid AND optionsKey = :key LIMIT 1")
    CartItem findByProductIdAndKey(String pid, String key);

    @Query("SELECT * FROM cart_items WHERE id = :id LIMIT 1")
    CartItem findById(long id);

    @Query("DELETE FROM cart_items WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM cart_items WHERE productId = :pid AND optionsKey = :key")
    void deleteByProductAndKey(String pid, String key);

    // Actualización de cantidad
    @Query("UPDATE cart_items SET quantity = :q WHERE productId = :pid")
    void updateQty(String pid, int q);

    // ✅ Usar uno de estos dos:
    @Query("UPDATE cart_items SET quantity = :q WHERE id = :id")
    void updateQtyById(long id, int q);

    @Query("UPDATE cart_items SET quantity = :q WHERE productId = :pid AND optionsKey = :key")
    void updateQtyByKey(String pid, String key, int q);
}

