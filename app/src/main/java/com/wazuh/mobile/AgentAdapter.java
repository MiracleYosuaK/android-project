package com.wazuh.mobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AgentAdapter extends RecyclerView.Adapter<AgentAdapter.AgentViewHolder> {

    private Context context;
    private List<Agent> agents;

    public AgentAdapter(Context context, List<Agent> agents) {
        this.context = context;
        this.agents = agents;
    }

    @NonNull
    @Override
    public AgentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_agent, parent, false);
        return new AgentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AgentViewHolder holder, int position) {
        Agent agent = agents.get(position);
        
        holder.tvAgentName.setText(agent.getName());
        holder.tvAgentIp.setText(agent.getIpAddress());
        holder.tvAgentStatus.setText(agent.getStatusText());
        
        // Set status background and color
        holder.tvAgentStatus.setBackgroundResource(agent.getStatusBackground());
        holder.tvAgentStatus.setTextColor(ContextCompat.getColor(context, R.color.white));
        
        // Set agent icon
        holder.ivAgentIcon.setImageResource(agent.getTypeIcon());
    }

    @Override
    public int getItemCount() {
        return agents.size();
    }

    public void updateAgents(List<Agent> newAgents) {
        this.agents = newAgents;
        notifyDataSetChanged();
    }

    static class AgentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAgentIcon;
        TextView tvAgentName, tvAgentIp, tvAgentStatus;

        public AgentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAgentIcon = itemView.findViewById(R.id.ivAgentIcon);
            tvAgentName = itemView.findViewById(R.id.tvAgentName);
            tvAgentIp = itemView.findViewById(R.id.tvAgentIp);
            tvAgentStatus = itemView.findViewById(R.id.tvAgentStatus);
        }
    }
}
