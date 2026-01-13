package com.example.vianelo_app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class PayPalWebViewActivity extends AppCompatActivity {

    private WebView webView;
    private double amount;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paypal_webview);

        amount = getIntent().getDoubleExtra("amount", 0.0);

        webView = findViewById(R.id.webView);

        // Configuración del WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.addJavascriptInterface(new PayPalInterface(), "Android");

        // WebViewClient para manejar la navegación
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("PAYPAL", "✅ Página cargada");
            }
        });

        // WebChromeClient para ver errores de JavaScript
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("PAYPAL_JS", consoleMessage.message() + " -- From line " +
                        consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return true;
            }
        });

        // Manejar botón de retroceso
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        });

        // Cargar HTML directamente
        String html = buildPayPalHTML();
        webView.loadDataWithBaseURL("https://vianelo.netlify.app/", html, "text/html", "UTF-8", null);

        Log.d("PAYPAL", "💳 Iniciando checkout. Monto: $" + amount);
    }

    private String buildPayPalHTML() {
        // IMPORTANTE: Reemplaza con tu Client ID real
        String clientId = "AVU5E1UGRaSsKCWf0af-NqfvLHeiwKZdeECtoFdUtbxUdF9L0Mk_9SIIHkyvK60oSo7x0-XG1zLqqo_Q";
        String amountStr = String.format("%.2f", amount);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1'>");
        html.append("<script src='https://www.paypal.com/sdk/js?client-id=").append(clientId).append("&currency=MXN'></script>");
        html.append("<style>");
        html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
        html.append("body { font-family: -apple-system, sans-serif; padding: 20px; background: #f5f5f5; }");
        html.append(".container { max-width: 400px; margin: 0 auto; background: white; border-radius: 12px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }");
        html.append("h2 { color: #0070ba; font-size: 24px; margin-bottom: 16px; }");
        html.append(".amount { font-size: 32px; font-weight: bold; color: #333; margin: 20px 0; }");
        html.append(".description { color: #666; font-size: 14px; margin-bottom: 24px; }");
        html.append("#paypal-button-container { margin-top: 20px; min-height: 150px; }");
        html.append(".loading { text-align: center; padding: 20px; color: #666; display: none; }");
        html.append("</style>");
        html.append("</head><body>");
        html.append("<div class='container'>");
        html.append("<h2>Finalizar Compra</h2>");
        html.append("<div class='amount'>$").append(amountStr).append(" MXN</div>");
        html.append("<div class='description'>Selecciona tu método de pago</div>");
        html.append("<div id='paypal-button-container'></div>");
        html.append("<div id='loading' class='loading'>Procesando pago...</div>");
        html.append("</div>");

        // JavaScript para PayPal
        html.append("<script>");
        html.append("paypal.Buttons({");
        html.append("  style: { layout: 'vertical', color: 'blue', shape: 'rect', label: 'paypal' },");
        html.append("  createOrder: function(data, actions) {");
        html.append("    console.log('Creating order...');");
        html.append("    return actions.order.create({");
        html.append("      purchase_units: [{");
        html.append("        amount: { currency_code: 'MXN', value: '").append(amountStr).append("' },");
        html.append("        description: 'Compra en Vianelo'");
        html.append("      }]");
        html.append("    });");
        html.append("  },");
        html.append("  onApprove: function(data, actions) {");
        html.append("    document.getElementById('loading').style.display = 'block';");
        html.append("    document.getElementById('paypal-button-container').style.display = 'none';");
        html.append("    return actions.order.capture().then(function(orderData) {");
        html.append("      console.log('Payment completed:', orderData);");
        html.append("      Android.onPaymentSuccess(JSON.stringify(orderData));");
        html.append("    });");
        html.append("  },");
        html.append("  onCancel: function(data) {");
        html.append("    console.log('Payment cancelled');");
        html.append("    Android.onPaymentCancelled();");
        html.append("  },");
        html.append("  onError: function(err) {");
        html.append("    console.error('Payment error:', err);");
        html.append("    Android.onPaymentError(String(err));");
        html.append("  }");
        html.append("}).render('#paypal-button-container');");
        html.append("</script>");

        html.append("</body></html>");

        return html.toString();
    }

    public class PayPalInterface {

        @JavascriptInterface
        public void onPaymentSuccess(String orderData) {
            runOnUiThread(() -> {
                Log.d("PAYPAL", "✅ Pago exitoso: " + orderData);
                Toast.makeText(PayPalWebViewActivity.this,
                        "¡Pago completado exitosamente!", Toast.LENGTH_LONG).show();

                saveOrderToFirestore(orderData);

                setResult(RESULT_OK);
                finish();
            });
        }

        @JavascriptInterface
        public void onPaymentCancelled() {
            runOnUiThread(() -> {
                Log.d("PAYPAL", "❌ Pago cancelado");
                Toast.makeText(PayPalWebViewActivity.this,
                        "Pago cancelado", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
            });
        }

        @JavascriptInterface
        public void onPaymentError(String error) {
            runOnUiThread(() -> {
                Log.e("PAYPAL", "❌ Error: " + error);
                Toast.makeText(PayPalWebViewActivity.this,
                        "Error en el pago", Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
            });
        }
    }

    private void saveOrderToFirestore(String orderData) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String userId = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser().getUid();

        java.util.Map<String, Object> order = new java.util.HashMap<>();
        order.put("userId", userId);
        order.put("amount", amount);
        order.put("currency", "MXN");
        order.put("status", "pending"); // Estado inicial: pendiente
        order.put("paypalData", orderData);
        order.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("orders")
                .add(order)
                .addOnSuccessListener(ref ->
                        Log.d("PAYPAL", "✅ Orden guardada: " + ref.getId()))
                .addOnFailureListener(e ->
                        Log.e("PAYPAL", "❌ Error guardando orden", e));
    }
}