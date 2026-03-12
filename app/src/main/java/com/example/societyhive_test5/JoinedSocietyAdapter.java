package com.example.societyhive_test5;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class JoinedSocietyAdapter extends RecyclerView.Adapter<JoinedSocietyAdapter.SocietyVH> {

    public interface OnManageClickListener {
        void onManageClick(@NonNull Society society);
    }

    private final List<Society> societies;
    private final OnManageClickListener listener;

    public JoinedSocietyAdapter(@NonNull List<Society> societies,
                                @NonNull OnManageClickListener listener) {
        this.societies = societies;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SocietyVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_joined_society, parent, false);
        return new SocietyVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SocietyVH holder, int position) {
        holder.bind(societies.get(position));
    }

    @Override
    public int getItemCount() {
        return societies.size();
    }

    public void updateList(@NonNull List<Society> newSocieties) {
        societies.clear();
        societies.addAll(new ArrayList<>(newSocieties));
        notifyDataSetChanged();
    }

    class SocietyVH extends RecyclerView.ViewHolder {
        private final View accent;
        private final TextView tvName;
        private final TextView tvSubtitle;
        private final MaterialButton btnManage;

        SocietyVH(@NonNull View itemView) {
            super(itemView);
            accent = itemView.findViewById(R.id.viewSocietyAccent);
            tvName = itemView.findViewById(R.id.tvSocietyName);
            tvSubtitle = itemView.findViewById(R.id.tvSocietySubtitle);
            btnManage = itemView.findViewById(R.id.btnManageSociety);
        }

        void bind(@NonNull Society society) {
            tvName.setText(society.getName());
            tvSubtitle.setText(society.getSubtitle());

            try {
                accent.setBackgroundColor(Color.parseColor(society.getColorHex()));
            } catch (IllegalArgumentException e) {
                accent.setBackgroundColor(Color.parseColor("#8D2E3A"));
            }

            btnManage.setOnClickListener(v -> listener.onManageClick(society));
            itemView.setOnClickListener(v -> listener.onManageClick(society));
        }
    }
}
