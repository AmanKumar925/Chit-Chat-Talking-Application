package com.aman.talkingcommunity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlogRecyclerAdapter
        extends RecyclerView.Adapter<BlogRecyclerAdapter.ViewHolder> {

    private final List<BlogPost> blogList;
    private final FirebaseFirestore firebaseFirestore;
    private final FirebaseAuth firebaseAuth;

    public BlogRecyclerAdapter(List<BlogPost> blogList) {
        this.blogList = blogList;
        this.firebaseFirestore = FirebaseFirestore.getInstance();
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    // =============================================================
    // CONTEXT
    // =============================================================

    private Context getContext(ViewHolder holder) {
        return holder.itemView.getContext();
    }

    // =============================================================
    // CREATE VIEW HOLDER
    // =============================================================

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.blog_list_item, parent, false);

        return new ViewHolder(view);
    }

    // =============================================================
    // BIND VIEW HOLDER
    // =============================================================

    @Override
    public void onBindViewHolder(
            @NonNull final ViewHolder holder,
            int position
    ) {

        if (blogList == null
                || position < 0
                || position >= blogList.size()) {
            return;
        }

        BlogPost post = blogList.get(position);

        if (post == null) {
            return;
        }

        // ---------------------------------------------------------
        // CURRENT USER
        // ---------------------------------------------------------

        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        final String currentUserId =
                firebaseAuth.getCurrentUser().getUid();

        final String blogPostId = post.BlogPostId;

        if (blogPostId == null
                || blogPostId.trim().isEmpty()) {
            return;
        }

        // =========================================================
        // POST TITLE
        // =========================================================

        String title = post.getDesc();

        if (title == null || title.trim().isEmpty()) {
            title = "Untitled post";
        }

        holder.titleView.setText(title);

        // =========================================================
        // USER DATA
        // =========================================================

        String userId = post.getUser_id();

        // Reset delete button because RecyclerView reuses views.
        holder.blogDeleteBtn.setVisibility(View.GONE);
        holder.blogDeleteBtn.setEnabled(false);

        if (userId != null
                && userId.equals(currentUserId)) {

            holder.blogDeleteBtn.setVisibility(View.VISIBLE);
            holder.blogDeleteBtn.setEnabled(true);
        }

        if (userId != null
                && !userId.trim().isEmpty()) {

            firebaseFirestore
                    .collection("Users")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()
                                && task.getResult() != null) {

                            DocumentSnapshot snapshot =
                                    task.getResult();

                            String userName =
                                    snapshot.getString("name");

                            String userImage =
                                    snapshot.getString("image");

                            holder.setUserData(
                                    userName == null
                                            ? "User"
                                            : userName,
                                    userImage
                            );

                        } else if (task.getException() != null) {

                            Log.w(
                                    "BlogRecyclerAdapter",
                                    "Unable to load user",
                                    task.getException()
                            );
                        }
                    });

        } else {

            holder.setUserData("User", null);
        }

        // =========================================================
        // POST DATE
        // =========================================================

        Date timestamp = post.getTimestamp();

        if (timestamp != null) {

            String dateString =
                    new SimpleDateFormat(
                            "MM/dd/yyyy",
                            Locale.getDefault()
                    ).format(timestamp);

            holder.setTime(dateString);

        } else {

            holder.setTime("");
        }

        // =========================================================
        // LIKE COUNT
        // =========================================================

        firebaseFirestore
                .collection("Posts/" + blogPostId + "/Likes")
                .addSnapshotListener(
                        new EventListener<QuerySnapshot>() {

                            @Override
                            public void onEvent(
                                    @Nullable QuerySnapshot snapshots,
                                    @Nullable FirebaseFirestoreException e
                            ) {

                                if (e != null
                                        || snapshots == null) {
                                    return;
                                }

                                holder.updateLikesCount(
                                        snapshots.size()
                                );
                            }
                        }
                );

        // =========================================================
        // COMMENT COUNT
        // =========================================================

        firebaseFirestore
                .collection("Posts/" + blogPostId + "/Comments")
                .addSnapshotListener(
                        new EventListener<QuerySnapshot>() {

                            @Override
                            public void onEvent(
                                    @Nullable QuerySnapshot snapshots,
                                    @Nullable FirebaseFirestoreException e
                            ) {

                                if (e != null
                                        || snapshots == null) {
                                    return;
                                }

                                holder.updateCommentCount(
                                        snapshots.size()
                                );
                            }
                        }
                );

        // =========================================================
        // CHECK CURRENT USER LIKE
        // =========================================================

        firebaseFirestore
                .collection("Posts/" + blogPostId + "/Likes")
                .document(currentUserId)
                .addSnapshotListener(
                        new EventListener<DocumentSnapshot>() {

                            @Override
                            public void onEvent(
                                    @Nullable DocumentSnapshot snapshot,
                                    @Nullable FirebaseFirestoreException e
                            ) {

                                if (e != null
                                        || snapshot == null) {
                                    return;
                                }

                                Drawable drawable;

                                if (snapshot.exists()) {

                                    drawable =
                                            getContext(holder)
                                                    .getResources()
                                                    .getDrawable(
                                                            R.drawable
                                                                    .action_like_accent
                                                    );

                                } else {

                                    drawable =
                                            getContext(holder)
                                                    .getResources()
                                                    .getDrawable(
                                                            R.drawable
                                                                    .action_like_gray
                                                    );
                                }

                                holder.blogLikeBtn
                                        .setImageDrawable(drawable);
                            }
                        }
                );

        // =========================================================
        // LIKE ACTION
        // =========================================================

        holder.blogLikeAction.setOnClickListener(v -> {

            if (currentUserId.equals(
                    v.getResources()
                            .getString(R.string.demoid))) {

                Toast.makeText(
                        getContext(holder),
                        "Login With Official Account",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            toggleLike(
                    holder,
                    blogPostId,
                    currentUserId
            );
        });

        // =========================================================
        // COMMENT ACTION
        // =========================================================

        holder.blogCommentAction.setOnClickListener(v -> {

            if (currentUserId.equals(
                    v.getResources()
                            .getString(R.string.demoid))) {

                Toast.makeText(
                        getContext(holder),
                        "Login With Official Account",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Intent commentIntent =
                    new Intent(
                            getContext(holder),
                            CommentsActivity.class
                    );

            commentIntent.putExtra(
                    "blog_post_id",
                    blogPostId
            );

            getContext(holder)
                    .startActivity(commentIntent);
        });

        // =========================================================
        // TITLE CLICK
        // =========================================================

        holder.titleView.setOnClickListener(v -> {

            Intent descriptionIntent =
                    new Intent(
                            getContext(holder),
                            DescriptionActivity.class
                    );

            descriptionIntent.putExtra(
                    "blog_post_id",
                    blogPostId
            );

            getContext(holder)
                    .startActivity(descriptionIntent);
        });

        // =========================================================
        // DELETE POST
        // =========================================================
        //
        // IMPORTANT:
        // Capture the exact adapter position BEFORE starting the
        // asynchronous Firestore operation.
        //
        // We do NOT finish the Activity after deletion.
        // Only the clicked item is removed.
        // =========================================================

        holder.blogDeleteBtn.setOnClickListener(v -> {

            // Get the position at the moment the user clicks.
            final int clickedPosition =
                    holder.getBindingAdapterPosition();

            // Safety check.
            if (clickedPosition == RecyclerView.NO_POSITION
                    || clickedPosition < 0
                    || clickedPosition >= blogList.size()) {

                return;
            }

            // Get the exact post from the clicked position.
            BlogPost clickedPost =
                    blogList.get(clickedPosition);

            if (clickedPost == null) {
                return;
            }

            final String clickedPostId =
                    clickedPost.BlogPostId;

            if (clickedPostId == null
                    || clickedPostId.trim().isEmpty()) {

                Toast.makeText(
                        getContext(holder),
                        "Unable to delete post.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // Prevent multiple clicks while deleting.
            holder.blogDeleteBtn.setEnabled(false);

            firebaseFirestore
                    .collection("Posts")
                    .document(clickedPostId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {

                        /*
                         * The RecyclerView may have changed while
                         * Firestore was processing the request.
                         *
                         * Therefore find the post by its ID instead
                         * of blindly using the old position.
                         */

                        int removePosition = -1;

                        for (int i = 0;
                             i < blogList.size();
                             i++) {

                            BlogPost currentPost =
                                    blogList.get(i);

                            if (currentPost != null
                                    && clickedPostId.equals(
                                    currentPost.BlogPostId)) {

                                removePosition = i;
                                break;
                            }
                        }

                        // Remove only the clicked post.
                        if (removePosition != -1) {

                            blogList.remove(removePosition);

                            notifyItemRemoved(removePosition);
                        }

                        Toast.makeText(
                                getContext(holder),
                                "Post Deleted !!!",
                                Toast.LENGTH_SHORT
                        ).show();

                    })
                    .addOnFailureListener(e -> {

                        // Re-enable if deletion failed.
                        holder.blogDeleteBtn.setEnabled(true);

                        String error =
                                e.getMessage();

                        if (error == null
                                || error.trim().isEmpty()) {

                            error = "Unknown error";
                        }

                        Toast.makeText(
                                getContext(holder),
                                "Delete failed: " + error,
                                Toast.LENGTH_LONG
                        ).show();

                        Log.e(
                                "BlogRecyclerAdapter",
                                "Failed to delete post: "
                                        + clickedPostId,
                                e
                        );
                    });
        });
    }

    // =============================================================
    // TOGGLE LIKE
    // =============================================================

    private void toggleLike(
            ViewHolder holder,
            String blogPostId,
            String currentUserId
    ) {

        firebaseFirestore
                .collection(
                        "Posts/" + blogPostId + "/Likes"
                )
                .document(currentUserId)
                .get()
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()
                            || task.getResult() == null) {
                        return;
                    }

                    if (!task.getResult().exists()) {

                        // -------------------------------
                        // ADD LIKE
                        // -------------------------------

                        Map<String, Object> likesMap =
                                new HashMap<>();

                        likesMap.put(
                                "timestamp",
                                FieldValue.serverTimestamp()
                        );

                        firebaseFirestore
                                .collection(
                                        "Posts/"
                                                + blogPostId
                                                + "/Likes"
                                )
                                .document(currentUserId)
                                .set(likesMap);

                    } else {

                        // -------------------------------
                        // REMOVE LIKE
                        // -------------------------------

                        firebaseFirestore
                                .collection(
                                        "Posts/"
                                                + blogPostId
                                                + "/Likes"
                                )
                                .document(currentUserId)
                                .delete();
                    }
                });
    }

    // =============================================================
    // ITEM COUNT
    // =============================================================

    @Override
    public int getItemCount() {

        return blogList == null
                ? 0
                : blogList.size();
    }

    // =============================================================
    // VIEW HOLDER
    // =============================================================

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        // ---------------------------------------------------------
        // POST
        // ---------------------------------------------------------

        private final TextView titleView;

        // ---------------------------------------------------------
        // USER
        // ---------------------------------------------------------

        private final TextView blogDate;
        private final TextView blogUserName;
        private final CircleImageView blogUserImage;

        // ---------------------------------------------------------
        // LIKE
        // ---------------------------------------------------------

        private final LinearLayout blogLikeAction;
        private final ImageView blogLikeBtn;
        private final TextView blogLikeCount;

        // ---------------------------------------------------------
        // COMMENT
        // ---------------------------------------------------------

        private final LinearLayout blogCommentAction;
        private final ImageView blogCommentBtn;
        private final TextView blogCommentCount;

        // ---------------------------------------------------------
        // DELETE
        // ---------------------------------------------------------

        private final ImageView blogDeleteBtn;

        public ViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);

            // =====================================================
            // POST
            // =====================================================

            titleView =
                    itemView.findViewById(
                            R.id.blog_title
                    );

            // =====================================================
            // USER
            // =====================================================

            blogDate =
                    itemView.findViewById(
                            R.id.blog_date
                    );

            blogUserImage =
                    itemView.findViewById(
                            R.id.blog_user_image
                    );

            blogUserName =
                    itemView.findViewById(
                            R.id.blog_user_name
                    );

            // =====================================================
            // LIKE
            // =====================================================

            blogLikeAction =
                    itemView.findViewById(
                            R.id.blog_like_action
                    );

            blogLikeBtn =
                    itemView.findViewById(
                            R.id.blog_like_btn
                    );

            blogLikeCount =
                    itemView.findViewById(
                            R.id.blog_like_count
                    );

            // =====================================================
            // COMMENT
            // =====================================================

            blogCommentAction =
                    itemView.findViewById(
                            R.id.blog_comment_action
                    );

            blogCommentBtn =
                    itemView.findViewById(
                            R.id.blog_comment_icon
                    );

            blogCommentCount =
                    itemView.findViewById(
                            R.id.blog_comment_count
                    );

            // =====================================================
            // DELETE
            // =====================================================

            blogDeleteBtn =
                    itemView.findViewById(
                            R.id.blog_delete_btn
                    );
        }

        // =========================================================
        // SET TIME
        // =========================================================

        public void setTime(String date) {

            blogDate.setText(
                    date == null ? "" : date
            );
        }

        // =========================================================
        // SET USER DATA
        // =========================================================

        public void setUserData(
                String name,
                String image
        ) {

            blogUserName.setText(
                    name == null
                            ? "User"
                            : name
            );

            RequestOptions options =
                    new RequestOptions()
                            .placeholder(
                                    R.drawable.empty_fav
                            )
                            .error(
                                    R.drawable.empty_fav
                            );

            Glide.with(
                            itemView.getContext()
                    )
                    .applyDefaultRequestOptions(options)
                    .load(image)
                    .into(blogUserImage);
        }

        // =========================================================
        // UPDATE LIKE COUNT
        // =========================================================

        public void updateLikesCount(int count) {

            blogLikeCount.setText(
                    count + " Likes"
            );
        }

        // =========================================================
        // UPDATE COMMENT COUNT
        // =========================================================

        public void updateCommentCount(int count) {

            blogCommentCount.setText(
                    count + " Comments"
            );
        }
    }
}