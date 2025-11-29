package com.wazuh.mobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ViewHolder> {

    private Context context;
    private List<WazuhServer> serverList;
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(String id, String name);
    }

    public ServerAdapter(Context context, List<WazuhServer> serverList, OnDeleteClickListener listener) {
        this.context = context;
        this.serverList = serverList;
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_server, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WazuhServer server = serverList.get(position);
        holder.tvName.setText(server.getName());
        holder.tvHost.setText(server.getHost() + ":" + server.getPort());

        holder.btnDelete.setOnClickListener(v -> {
            deleteListener.onDeleteClick(server.getId(), server.getName());
        });
    }

    @Override
    public int getItemCount() {
        return serverList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvHost;
        ImageButton btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvServerName);
            tvHost = itemView.findViewById(R.id.tvServerHost);
            btnDelete = itemView.findViewById(R.id.btnDeleteServer);
        }
    }
}