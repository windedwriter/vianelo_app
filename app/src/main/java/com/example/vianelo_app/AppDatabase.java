package com.example.vianelo_app;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.content.Context;

@Database(entities = {CartItem.class}, version = 2) // 👈 sube versión
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    public abstract CartDao cartDao();

    // MIGRACIÓN 1 → 2 (ajusta si tu versión previa no era 1)
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE cart_items ADD COLUMN nameBase TEXT");
            db.execSQL("ALTER TABLE cart_items ADD COLUMN size TEXT");
            db.execSQL("ALTER TABLE cart_items ADD COLUMN milk TEXT");
            db.execSQL("ALTER TABLE cart_items ADD COLUMN extras TEXT");
            db.execSQL("ALTER TABLE cart_items ADD COLUMN note TEXT");
            db.execSQL("ALTER TABLE cart_items ADD COLUMN optionsKey TEXT NOT NULL DEFAULT ''");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cart_items_productId_optionsKey ON cart_items(productId, optionsKey)");
        }
    };

    public static AppDatabase get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(ctx.getApplicationContext(),
                                    AppDatabase.class, "vianelo.db")
                            .addMigrations(MIGRATION_1_2) // 👈 usar migración
                            // .fallbackToDestructiveMigration() // (solo si puedes perder datos en DEV)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
