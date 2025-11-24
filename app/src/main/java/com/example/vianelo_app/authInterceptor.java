package com.example.vianelo_app;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class authInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request orig = chain.request();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String token = null;

        if (user != null) {
            try {
                token = Tasks.await(user.getIdToken(false)).getToken();
            } catch (Exception ignored) {}
        }

        Request.Builder builder = orig.newBuilder();

        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        return chain.proceed(builder.build());
    }
}
