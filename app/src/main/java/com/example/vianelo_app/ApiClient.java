package com.example.vianelo_app;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static ApiService API;

    public static ApiService get() {

        if (API == null) {

            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient ok = new OkHttpClient.Builder()
                    .addInterceptor(new authInterceptor())
                    .addInterceptor(log)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://tu-api.com/")          //  cambiala
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(ok)
                    .build();

            API = retrofit.create(ApiService.class);
        }

        return API;
    }
}

