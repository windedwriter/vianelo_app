package com.example.vianelo_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {

    private EditText edtNombre, edtEmail, edtPassword, edtConfirm;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        edtNombre   = findViewById(R.id.Et_nombre);
        edtEmail    = findViewById(R.id.Et_email);
        edtPassword = findViewById(R.id.Et_password);
        edtConfirm  = findViewById(R.id.Et_confirmpass);

        Button btnRegistrar = findViewById(R.id.btn_resgistro);
        Button btnIrLogin   = findViewById(R.id.btn_login);

        btnRegistrar.setOnClickListener(v -> crearCuenta());
        btnIrLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegistroActivity.this, MainActivity.class));
            finish();
        });
    }

    private void crearCuenta() {
        String nombre = edtNombre.getText().toString().trim();
        String email  = edtEmail.getText().toString().trim();
        String pass   = edtPassword.getText().toString();
        String conf   = edtConfirm.getText().toString();


        if (nombre.isEmpty()) { edtNombre.setError("Ingresa tu nombre"); return; }
        if (email.isEmpty())  { edtEmail.setError("Ingresa tu email"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email inválido"); return;
        }
        if (pass.length() < 6) {
            edtPassword.setError("Mínimo 6 caracteres"); return;
        }
        if (!pass.equals(conf)) {
            edtConfirm.setError("No coincide"); return;
        }


        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        Toast.makeText(this, "Error inesperado", Toast.LENGTH_SHORT).show();
                        return;
                    }


                    Map<String, Object> perfil = new HashMap<>();
                    perfil.put("uid", user.getUid());
                    perfil.put("nombre", nombre);
                    perfil.put("email", email);
                    perfil.put("createdAt", System.currentTimeMillis());

                    db.collection("users").document(user.getUid())
                            .set(perfil)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Cuenta creada. Ahora inicia sesión.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegistroActivity.this, MainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error guardando perfil: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error creando cuenta: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
