package com.wazuh.mobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.ViewHolder> {

    private final Context context;
    private List<Alert> alerts;
    private final OnItemClickListener listener; // <--- Listener Baru

    // Interface untuk komunikasi ke Fragment
    public interface OnItemClickListener {
        void onItemClick(Alert alert);
    }

    // Constructor Diupdate: Tambah listener
    public AlertAdapter(Context context, List<Alert> alerts, OnItemClickListener listener) {
        this.context = context;
        this.alerts = alerts;
        this.listener = listener;
    }

    // Constructor Lama (Overload) biar DashboardFragment & AlertsFragment gak error
    public AlertAdapter(Context context, List<Alert> alerts) {
        this(context, alerts, null);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alert alert = alerts.get(position);

        holder.tvTitle.setText(alert.getTitle());
        holder.tvTime.setText(alert.getTimeAgo());
        holder.tvLevel.setText("Lvl " + alert.getLevel());
        holder.tvSource.setText(alert.getAgentName());

        int color = alert.getSeverityColor();
        holder.tvLevel.setTextColor(color);
        if (holder.viewStrip != null) {
            holder.viewStrip.setBackgroundColor(color);
        }

        // --- PASANG CLICK LISTENER ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(alert);
            }
        });
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLevel, tvSource, tvTime;
        View viewStrip;

        public ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAlertTitle);
            tvLevel = itemView.findViewById(R.id.tvAlertLevel);
            tvSource = itemView.findViewById(R.id.tvAlertSource);
            tvTime = itemView.findViewById(R.id.tvAlertTime);

            ViewGroup vg = (ViewGroup) ((ViewGroup) itemView).getChildAt(0);
            if (vg != null && vg.getChildAt(0) instanceof View) {
                viewStrip = vg.getChildAt(0);
            }
        }
    }
}