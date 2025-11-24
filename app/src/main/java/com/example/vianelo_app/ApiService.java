package com.example.vianelo_app;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
        @POST("cart/sync")
        Call<CartPayload> syncCart(@Body CartPayload localCart);

        @GET("cart")
        Call<CartPayload> getCart();

        @POST("cart/add")
        Call<Void> addItem(@Body CartPayload.CartLine line);

        @POST("cart/update")
        Call<Void> updateItem(@Body CartPayload.CartLine line);

        @POST("cart/remove")
        Call<Void> removeItem(@Body Map<String, String> body);
    }


