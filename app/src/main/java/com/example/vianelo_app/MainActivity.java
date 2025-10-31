package com.example.vianelo_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.inicio_sesion); // Tu layout de login

        auth        = FirebaseAuth.getInstance();
        edtEmail    = findViewById(R.id.Et_email);
        edtPassword = findViewById(R.id.Et_password);

        Button btnLogin    = findViewById(R.id.btn_login);
        Button btnRegistro = findViewById(R.id.btn_resgistro);

        btnLogin.setOnClickListener(v -> iniciarSesion());
        btnRegistro.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RegistroActivity.class))
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Si ya hay sesión iniciada, ir directo a Home
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current != null) {
            irAHome();
        }
    }

    private void iniciarSesion() {
        String email = edtEmail.getText().toString().trim();
        String pass  = edtPassword.getText().toString();

        if (email.isEmpty()) { edtEmail.setError("Ingresa tu email"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email inválido"); return;
        }
        if (pass.isEmpty()) { edtPassword.setError("Ingresa tu contraseña"); return; }

        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(res -> {
                    Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                    irAHome();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error de login: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void irAHome() {
        startActivity(new Intent(MainActivity.this, HomeActivity.class));
        finish();
    }
}
