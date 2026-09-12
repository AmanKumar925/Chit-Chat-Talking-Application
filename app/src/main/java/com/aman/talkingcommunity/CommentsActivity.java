package com.aman.talkingcommunity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentsActivity extends AppCompatActivity {

    private Toolbar commentToolbar;
    private EditText commentField;
    private ImageView commentPostBtn;
    private RecyclerView commentList;
    private View commentInputContainer;

    private CommentsRecyclerAdapter commentsRecyclerAdapter;
    private List<Comments> commentsList;

    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    private String blogPostId;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Enable edge-to-edge so status/navigation bars
         * can be handled safely using WindowInsets.
         */
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_comments);

        /*
         * System bar colors.
         */
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);

        /*
         * Dark icons on light system bars.
         */
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        setupToolbar();
        setupFirebase();
        setupViews();
        setupRecyclerView();

        /*
         * Stop if Firebase/user/post validation failed.
         */
        if (firebaseAuth == null
                || firebaseAuth.getCurrentUser() == null
                || firebaseFirestore == null
                || blogPostId == null
                || blogPostId.trim().isEmpty()) {
            return;
        }

        loadComments();

        if (commentPostBtn != null) {
            commentPostBtn.setOnClickListener(v -> postComment());
        }
    }

    /**
     * Setup toolbar and system-bar insets.
     */
    private void setupToolbar() {

        commentToolbar = findViewById(R.id.comment_toolbar);

        if (commentToolbar == null) {
            return;
        }

        /*
         * Use Toolbar as the Activity ActionBar.
         */
        setSupportActionBar(commentToolbar);

        /*
         * Toolbar appearance.
         */
        commentToolbar.setBackgroundColor(Color.WHITE);
        commentToolbar.setTitleTextColor(Color.BLACK);

        /*
         * Normal ActionBar height.
         */
        commentToolbar.setMinimumHeight(
                getResources().getDimensionPixelSize(
                        androidx.appcompat.R.dimen.abc_action_bar_default_height_material
                )
        );

        /*
         * Toolbar title and back button.
         */
        if (getSupportActionBar() != null) {

            getSupportActionBar().setTitle("Comments");

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        /*
         * Black back arrow.
         */
        if (commentToolbar.getNavigationIcon() != null) {

            commentToolbar.getNavigationIcon().setTint(Color.BLACK);
        }

        /*
         * Status-bar inset.
         *
         * The status_bar_space View sits above the toolbar,
         * preventing the toolbar from going underneath
         * the status bar.
         */
        View statusBarSpace =
                findViewById(R.id.status_bar_space);

        if (statusBarSpace != null) {

            ViewCompat.setOnApplyWindowInsetsListener(
                    statusBarSpace,
                    (view, windowInsets) -> {

                        Insets statusInsets =
                                windowInsets.getInsets(
                                        WindowInsetsCompat.Type.statusBars()
                                );

                        view.getLayoutParams().height =
                                statusInsets.top;

                        view.requestLayout();

                        return windowInsets;
                    }
            );

            ViewCompat.requestApplyInsets(statusBarSpace);
        }

        /*
         * Bottom comment input.
         *
         * This applies the navigation-bar inset so that
         * the EditText and Send button remain ABOVE the
         * Android navigation buttons / gesture area.
         */
        commentInputContainer =
                findViewById(R.id.comment_input_container);

        if (commentInputContainer != null) {

            ViewCompat.setOnApplyWindowInsetsListener(
                    commentInputContainer,
                    (view, windowInsets) -> {

                        Insets navigationInsets =
                                windowInsets.getInsets(
                                        WindowInsetsCompat.Type.navigationBars()
                                );

                        /*
                         * Preserve horizontal/top padding.
                         *
                         * Add navigation-bar height + 12dp
                         * to the bottom.
                         */
                        int extraBottomPadding =
                                navigationInsets.bottom
                                        + dpToPx(12);

                        view.setPadding(
                                view.getPaddingLeft(),
                                view.getPaddingTop(),
                                view.getPaddingRight(),
                                extraBottomPadding
                        );

                        return windowInsets;
                    }
            );

            ViewCompat.requestApplyInsets(commentInputContainer);
        }
    }

    /**
     * Firebase initialization.
     */
    private void setupFirebase() {

        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Please log in first.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        currentUserId =
                firebaseAuth.getCurrentUser().getUid();

        firebaseFirestore =
                FirebaseFirestore.getInstance();

        blogPostId =
                getIntent().getStringExtra("blog_post_id");

        if (blogPostId == null
                || blogPostId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Post not found.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
        }
    }

    /**
     * Find views.
     */
    private void setupViews() {

        commentField =
                findViewById(R.id.comment_field);

        commentPostBtn =
                findViewById(R.id.comment_post_btn);

        commentList =
                findViewById(R.id.comment_list);
    }

    /**
     * RecyclerView setup.
     */
    private void setupRecyclerView() {

        if (commentList == null) {
            return;
        }

        commentsList =
                new ArrayList<>();

        commentsRecyclerAdapter =
                new CommentsRecyclerAdapter(
                        commentsList
                );

        commentList.setLayoutManager(
                new LinearLayoutManager(this)
        );

        /*
         * Don't force fixed item sizes because
         * comments can have different heights.
         */
        commentList.setHasFixedSize(false);

        commentList.setAdapter(
                commentsRecyclerAdapter
        );
    }

    /**
     * Load comments from Firestore.
     */
    private void loadComments() {

        if (firebaseFirestore == null
                || blogPostId == null
                || commentsRecyclerAdapter == null) {
            return;
        }

        firebaseFirestore
                .collection(
                        "Posts/"
                                + blogPostId
                                + "/Comments"
                )
                .addSnapshotListener(
                        this,
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

                                /*
                                 * Add only newly-added comments.
                                 */
                                for (DocumentChange change :
                                        snapshots.getDocumentChanges()) {

                                    if (change.getType()
                                            == DocumentChange.Type.ADDED) {

                                        Comments comment =
                                                change.getDocument()
                                                        .toObject(
                                                                Comments.class
                                                        );

                                        if (comment != null) {

                                            commentsList.add(
                                                    comment
                                            );
                                        }
                                    }
                                }

                                commentsRecyclerAdapter
                                        .notifyDataSetChanged();
                            }
                        }
                );
    }

    /**
     * Post a new comment.
     */
    private void postComment() {

        if (commentField == null) {
            return;
        }

        String message =
                commentField
                        .getText()
                        .toString()
                        .trim();

        /*
         * Don't allow empty comments.
         */
        if (message.isEmpty()) {

            commentField.setError(
                    "Comment cannot be empty."
            );

            return;
        }

        /*
         * Validate Firebase state.
         */
        if (firebaseFirestore == null
                || blogPostId == null
                || currentUserId == null) {

            Toast.makeText(
                    this,
                    "Unable to post comment.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> commentsMap =
                new HashMap<>();

        commentsMap.put(
                "message",
                message
        );

        commentsMap.put(
                "user_id",
                currentUserId
        );

        commentsMap.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        /*
         * Add comment to:
         *
         * Posts/{blogPostId}/Comments
         */
        firebaseFirestore
                .collection(
                        "Posts/"
                                + blogPostId
                                + "/Comments"
                )
                .add(commentsMap)
                .addOnCompleteListener(
                        new com.google.android.gms.tasks.OnCompleteListener<DocumentReference>() {

                            @Override
                            public void onComplete(
                                    @NonNull com.google.android.gms.tasks.Task<DocumentReference> task
                            ) {

                                if (task.isSuccessful()) {

                                    /*
                                     * Clear input after successful post.
                                     */
                                    commentField.setText("");

                                } else {

                                    String error =
                                            task.getException() != null
                                                    ? task.getException()
                                                    .getMessage()
                                                    : "Unable to post comment.";

                                    Toast.makeText(
                                            CommentsActivity.this,
                                            "Error Posting Comment: "
                                                    + error,
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }
                        }
                );
    }

    /**
     * Convert dp to pixels.
     */
    private int dpToPx(int dp) {

        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    /**
     * Toolbar back button.
     */
    @Override
    public boolean onSupportNavigateUp() {

        getOnBackPressedDispatcher()
                .onBackPressed();

        return true;
    }
}