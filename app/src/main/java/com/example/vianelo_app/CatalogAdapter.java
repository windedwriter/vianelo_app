package com.example.vianelo_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class CatalogAdapter extends RecyclerView.Adapter<CatalogAdapter.VH> {


    public interface Actions {
        void onAdd(Product p);
    }

    private final List<Product> data = new ArrayList<>();
    private final Actions actions;

    public CatalogAdapter(Actions actions) {
        this.actions = actions;
    }


    public void submit(List<Product> items) {
        data.clear();
        if (items != null) {
            data.addAll(items);
        }
        notifyDataSetChanged();
    }

    // ---------- ViewHolder ----------
    static class VH extends RecyclerView.ViewHolder {
        TextView name;
        TextView price;
        ImageView img;
        TextView btnAdd;

        VH(@NonNull View v) {
            super(v);
            name   = v.findViewById(R.id.tvName);
            price  = v.findViewById(R.id.tvPrice);
            img    = v.findViewById(R.id.imgProduct);
            btnAdd = v.findViewById(R.id.btnAdd);
        }
    }

    // ---------- Adapter overrides ----------
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_catalog_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = data.get(position);

        h.name.setText(p.name);
        h.price.setText("$" + p.price);


         Glide.with(h.img.getContext()).load(p.imageUrl).into(h.img);

        h.btnAdd.setOnClickListener(v -> actions.onAdd(p));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
