package com.wazuh.mobile;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.wazuh.mobile.Alert;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.ViewHolder> {

    private Context context;
    private List<Alert> alerts;

    public AlertAdapter(Context context, List<Alert> alerts) {
        this.context = context;
        this.alerts = alerts;
    }

    public void updateAlerts(List<Alert> newAlerts) {
        this.alerts = newAlerts;
        notifyDataSetChanged();
    }

    @Override
    public AlertAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AlertAdapter.ViewHolder holder, int position) {
        Alert alert = alerts.get(position);
        holder.title.setText(alert.getTitle());
        holder.description.setText(alert.getDescription());
        holder.timestamp.setText(alert.getTimeAgo());
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, timestamp;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.AlertTitle);
            description = itemView.findViewById(R.id.AlertDescription);
            timestamp = itemView.findViewById(R.id.AlertTime);
        }
    }
}