package com.example.vianelo_app;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class VianeloApp extends Application {
    @Override public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);

        boolean isDebug =
                (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance();

        if (isDebug) {
            appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
            );
            Log.d("APP_CHECK", "USANDO DebugAppCheckProviderFactory ✅");
        } else {
            appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
            );
            Log.d("APP_CHECK", "USANDO PlayIntegrityAppCheckProviderFactory ✅");
        }
    }
}
