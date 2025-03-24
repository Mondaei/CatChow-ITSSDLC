package com.mobdeve.s17.catchow.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.mobdeve.s17.catchow.R;
import com.mobdeve.s17.catchow.models.Users;

import java.util.ArrayList;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private ArrayList<Users> usersList;
    private String currentUserEmail;
    private FirebaseFirestore db;

    public UserAdapter(Context context, ArrayList<Users> usersList, String currentUserEmail) {
        this.context = context;
        this.usersList = usersList;
        this.currentUserEmail = currentUserEmail;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Users user = usersList.get(position);

        holder.nameTextView.setText(user.getName());
        holder.emailTextView.setText(user.getEmail());
        holder.roleTextView.setText(user.getRole() != null ? user.getRole() : "user");

        // Disable delete button for current user (admin can't delete themselves)
        if (user.getEmail().equals(currentUserEmail)) {
            holder.deleteButton.setEnabled(false);
            holder.deleteButton.setAlpha(0.5f);
        } else {
            holder.deleteButton.setEnabled(true);
            holder.deleteButton.setAlpha(1.0f);

            holder.deleteButton.setOnClickListener(v -> deleteUser(user, position));
        }
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    private void deleteUser(Users user, int position) {
        db.collection("users")
                .whereEqualTo("email", user.getEmail())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        db.collection("users")
                                .document(queryDocumentSnapshots.getDocuments().get(0).getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(context, "User deleted successfully", Toast.LENGTH_SHORT).show();
                                    usersList.remove(position);
                                    notifyDataSetChanged();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Failed to delete user", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, emailTextView, roleTextView;
        Button deleteButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.user_name);
            emailTextView = itemView.findViewById(R.id.user_email);
            roleTextView = itemView.findViewById(R.id.user_role);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}