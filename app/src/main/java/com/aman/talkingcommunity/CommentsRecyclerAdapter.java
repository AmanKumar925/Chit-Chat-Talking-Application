package com.aman.talkingcommunity;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentsRecyclerAdapter
        extends RecyclerView.Adapter<CommentsRecyclerAdapter.ViewHolder> {

    private final List<Comments> commentsList;
    private final FirebaseFirestore firebaseFirestore;

    public CommentsRecyclerAdapter(List<Comments> commentsList) {
        this.commentsList = commentsList;
        this.firebaseFirestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.comment_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        if (position < 0 || position >= commentsList.size()) {
            return;
        }

        Comments comment = commentsList.get(position);
        if (comment == null) {
            return;
        }

        holder.setCommentMessage(comment.getMessage());

        String userId = comment.getUser_id();

        if (userId == null || userId.trim().isEmpty()) {
            holder.setUserData("User", null);
            return;
        }

        firebaseFirestore.collection("Users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot snapshot = task.getResult();

                        String name = snapshot.getString("name");
                        String image = snapshot.getString("image");

                        holder.setUserData(
                                name == null ? "User" : name,
                                image
                        );
                    } else if (task.getException() != null) {
                        Log.w(
                                "CommentsAdapter",
                                "Unable to load comment user",
                                task.getException()
                        );
                    }
                });
    }

    @Override
    public int getItemCount() {
        return commentsList == null ? 0 : commentsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView commentUsername;
        private final TextView commentMessage;
        private final CircleImageView commentUserImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            commentUsername =
                    itemView.findViewById(R.id.comment_username);
            commentMessage =
                    itemView.findViewById(R.id.comment_message);
            commentUserImage =
                    itemView.findViewById(R.id.comment_image);
        }

        public void setCommentMessage(String message) {
            commentMessage.setText(message == null ? "" : message);
        }

        public void setUserData(String name, String image) {
            commentUsername.setText(name == null ? "User" : name);

            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.default_image)
                    .error(R.drawable.default_image);

            Glide.with(itemView.getContext())
                    .applyDefaultRequestOptions(options)
                    .load(image)
                    .into(commentUserImage);
        }
    }
}
