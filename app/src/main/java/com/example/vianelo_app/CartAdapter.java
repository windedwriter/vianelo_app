package com.example.vianelo_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {
    public interface Actions {
        void onPlus(CartItem item);
        void onMinus(CartItem item);
        void onRemove(CartItem item);
    }
    private List<CartItem> data = new ArrayList<>();
    private final Actions actions;

    public CartAdapter(Actions a){ this.actions = a; }
    
    public void submit(List<CartItem> items){ this.data = items; notifyDataSetChanged(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;                // @id/imgProduct
        TextView name;                // @id/tvName
        TextView variant;             // @id/tvVariant   (opcional)
        TextView note;                // @id/tvNote      (opcional)
        TextView qty;                 // @id/tvQty
        TextView subtotal;            // @id/tvSubtotal
        ImageButton plus, minus;      // @id/btnPlus, @id/btnMinus
        TextView remove;              // @id/btnRemove  (texto "Eliminar")

        VH(@NonNull View v) {
            super(v);
            img      = v.findViewById(R.id.imgProduct);
            name     = v.findViewById(R.id.tvName);
            variant  = v.findViewById(R.id.tvVariant);
            note     = v.findViewById(R.id.tvNote);
            qty      = v.findViewById(R.id.tvQty);
            subtotal = v.findViewById(R.id.tvSubtotal);
            plus     = v.findViewById(R.id.btnPlus);
            minus    = v.findViewById(R.id.btnMinus);
            remove   = v.findViewById(R.id.btnRemove);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_cart_item, parent, false);
        return new VH(v);
    }
    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        CartItem it = data.get(position);

        h.name.setText(it.getDisplayName());


        if (h.variant != null) {
            String variantText = buildVariant(it.size, it.milk, it.extras);
            if (!variantText.isEmpty()) {
                h.variant.setText(variantText);
                h.variant.setVisibility(View.VISIBLE);
            } else {
                h.variant.setVisibility(View.GONE);
            }
        }

        // Nota opcional
        if (h.note != null) {
            if (it.note != null && !it.note.isEmpty()) {
                h.note.setText("Nota: " + it.note);
                h.note.setVisibility(View.VISIBLE);
            } else {
                h.note.setVisibility(View.GONE);
            }
        }

        // Cantidad y subtotal
        h.qty.setText(String.valueOf(it.quantity));
        h.subtotal.setText(toMXN(it.getSubtotal()));

        // Acciones
        h.plus.setOnClickListener(v -> actions.onPlus(it));
        h.minus.setOnClickListener(v -> actions.onMinus(it));
        h.remove.setOnClickListener(v -> actions.onRemove(it));
    }


    private String buildVariant(String size, String milk, String extras) {
        StringBuilder sb = new StringBuilder();
        if (size != null && !size.isEmpty()) sb.append(size);
        if (milk != null && !milk.isEmpty()) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(milk);
        }
        if (extras != null && !extras.isEmpty()) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(extras);
        }
        return sb.toString();
    }


    private String toMXN(double value){
        java.text.NumberFormat f = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es","MX"));
        return f.format(value);
    }
    

    @Override public int getItemCount(){ return data.size(); }

}
