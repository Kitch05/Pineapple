package com.example.pineapple;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.CommunityViewHolder> {

    private List<Community> communityList;
    private Context context;
    private OnCommunityClickListener onCommunityClickListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance(); // Firestore instance

    public CommunityAdapter(Context context, List<Community> communityList,
                            OnCommunityClickListener onCommunityClickListener) {
        this.context = context;
        this.communityList = communityList;
        this.onCommunityClickListener = onCommunityClickListener;
    }

    @NonNull
    @Override
    public CommunityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.community_item, parent, false);
        return new CommunityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommunityViewHolder holder, int position) {
        Community community = communityList.get(position);

        holder.textViewName.setText(community.getName());
        holder.textViewDescription.setText(community.getDescription());
        holder.membersCountTextView.setText(community.getMemberCount() + " Members"); // Update members count

        holder.joinButton.setText(community.isJoined() ? "Joined" : "Join");

        holder.joinButton.setOnClickListener(v -> {
            if (community.isJoined()) {
                community.leaveCommunity();
                updateCommunityStatusInFirestore(community, false); // Update Firestore
            } else {
                community.joinCommunity();
                updateCommunityStatusInFirestore(community, true); // Update Firestore
            }
            notifyItemChanged(position); // Refresh UI for the updated item
        });

        holder.itemView.setOnClickListener(v -> {
            if (context instanceof CommunityActivity) {
                ((CommunityActivity) context).setCurrentCommunityPosition(position);
            }
            onCommunityClickListener.onCommunityClick(position);
        });
    }



    @Override
    public int getItemCount() {
        return communityList.size();
    }

    public void updateList(List<Community> newList) {
        communityList.clear();
        communityList.addAll(newList);
        notifyDataSetChanged();
    }

    public static class CommunityViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewDescription;
        TextView membersCountTextView;
        Button joinButton;

        public CommunityViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.communityName);
            textViewDescription = itemView.findViewById(R.id.communityDescription);
            membersCountTextView = itemView.findViewById(R.id.membersCount);
            joinButton = itemView.findViewById(R.id.joinCommunityButton);
        }
    }

    public interface OnCommunityClickListener {
        void onCommunityClick(int position);
    }

    private void updateCommunityStatusInFirestore(Community community, boolean isJoined) {
        // Determine increment or decrement
        int incrementValue = isJoined ? 1 : -1;

        // Firestore update with atomic increment
        DocumentReference communityRef = db.collection("community").document(community.getId());
        communityRef.update(
                "joined", isJoined,
                "memberCount", FieldValue.increment(incrementValue)
        ).addOnSuccessListener(aVoid -> {
            community.setJoined(isJoined); // Update local state
            community.setMemberCount(community.getMemberCount() + incrementValue); // Update local count
            notifyDataSetChanged(); // Refresh the UI
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Failed to update membership: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


}


