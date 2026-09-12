package com.aman.talkingcommunity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final int PAGE_SIZE = 5;

    private RecyclerView blogListView;
    private List<BlogPost> blogList;

    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private BlogRecyclerAdapter blogRecyclerAdapter;

    // Current window size of the live query (grows as user scrolls).
    private int currentLimit = PAGE_SIZE;

    private boolean isLoadingMore = false;
    private boolean hasMoreData = true;

    // ---------------------------------------------------------
    // Manually-managed listener registration.
    // Using the Activity-scoped addSnapshotListener(Activity, ...)
    // overload caused a crash:
    //   IllegalStateException: FragmentManager is already executing
    //   transactions
    // because Firestore internally tries to attach a headless
    // fragment via executePendingTransactions() while this
    // fragment's own onCreateView() is already being dispatched
    // inside MainActivity's FragmentManager transaction.
    //
    // Using the plain addSnapshotListener(listener) overload avoids
    // that FragmentManager dependency entirely. We remove the
    // listener ourselves in onDestroyView().
    // ---------------------------------------------------------
    private ListenerRegistration postsListenerRegistration;

    public HomeFragment() {
        // Required empty public constructor
    }

    // =============================================================
    // CREATE VIEW
    // =============================================================

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.fragment_home,
                container,
                false
        );

        blogList = new ArrayList<>();

        blogListView =
                view.findViewById(R.id.blog_post_view);

        firebaseAuth =
                FirebaseAuth.getInstance();

        blogRecyclerAdapter =
                new BlogRecyclerAdapter(blogList);

        blogListView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        blogListView.setAdapter(
                blogRecyclerAdapter
        );

        if (firebaseAuth.getCurrentUser() == null) {
            return view;
        }

        firebaseFirestore =
                FirebaseFirestore.getInstance();

        // ---------------------------------------------------------
        // PAGINATION SCROLL
        // ---------------------------------------------------------

        blogListView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {

                    @Override
                    public void onScrolled(
                            @NonNull RecyclerView recyclerView,
                            int dx,
                            int dy
                    ) {

                        super.onScrolled(
                                recyclerView,
                                dx,
                                dy
                        );

                        if (dy > 0
                                && !isLoadingMore
                                && hasMoreData
                                && !recyclerView
                                .canScrollVertically(1)) {

                            loadMorePost();
                        }
                    }
                }
        );

        // ---------------------------------------------------------
        // START REAL-TIME LISTENER
        // ---------------------------------------------------------

        attachPostsListener();

        return view;
    }

    // =============================================================
    // ATTACH / RE-ATTACH LIVE LISTENER
    // =============================================================
    //
    // A single snapshot listener drives the whole currently-loaded
    // window (0..currentLimit). Every post the user has scrolled to
    // stays live: adding a post anywhere inserts it here immediately
    // (it's newest, so always inside the window), and deleting a
    // post removes it here immediately too - no matter which page
    // it was loaded on, and no matter whether the delete happened
    // from this screen or another one (e.g. NotificationFragment).
    //
    // Growing currentLimit and re-attaching is what "loading more"
    // means now - there's no separate one-shot fetch anymore.
    // =============================================================

    private void attachPostsListener() {

        if (firebaseFirestore == null) {
            return;
        }

        if (postsListenerRegistration != null) {

            postsListenerRegistration.remove();

            postsListenerRegistration = null;
        }

        Query query =
                firebaseFirestore
                        .collection("Posts")
                        .orderBy(
                                "timestamp",
                                Query.Direction.DESCENDING
                        )
                        .limit(currentLimit);

        /*
         * IMPORTANT:
         *
         * Do NOT use:
         *
         * addSnapshotListener(this, ...)
         *
         * because FragmentManager can already be executing a
         * transaction when this Fragment is being created.
         *
         * We use the plain listener and manually remove it in
         * onDestroyView() (and whenever we re-attach with a new
         * limit).
         */

        postsListenerRegistration =
                query.addSnapshotListener(
                        new EventListener<QuerySnapshot>() {

                            @Override
                            public void onEvent(
                                    @Nullable QuerySnapshot snapshots,
                                    @Nullable FirebaseFirestoreException e
                            ) {

                                if (e != null
                                        || snapshots == null
                                        || !isAdded()) {

                                    isLoadingMore = false;

                                    return;
                                }

                                updatePosts(snapshots);

                                /*
                                 * If Firestore returned fewer docs
                                 * than we asked for, we've reached
                                 * the end of the collection.
                                 */

                                hasMoreData =
                                        snapshots.size()
                                                >= currentLimit;

                                isLoadingMore = false;
                            }
                        }
                );
    }

    // =============================================================
    // UPDATE POSTS FROM SNAPSHOT
    // =============================================================

    private void updatePosts(
            QuerySnapshot snapshots
    ) {

        /*
         * Process every Firestore change.
         *
         * ADDED    -> add post
         * MODIFIED -> update post
         * REMOVED  -> remove post
         */

        for (DocumentChange change :
                snapshots.getDocumentChanges()) {

            DocumentSnapshot document =
                    change.getDocument();

            String documentId =
                    document.getId();

            // =====================================================
            // ADDED
            // =====================================================

            if (change.getType()
                    == DocumentChange.Type.ADDED) {

                BlogPost post =
                        document.toObject(
                                BlogPost.class
                        );

                if (post == null) {
                    continue;
                }

                post.withId(documentId);

                /*
                 * Don't add duplicate posts.
                 */

                if (findPostIndex(documentId) == -1) {

                    int newIndex =
                            change.getNewIndex();

                    /*
                     * Firestore provides the correct index for the
                     * current query ordering.
                     */

                    if (newIndex >= 0
                            && newIndex <= blogList.size()) {

                        blogList.add(
                                newIndex,
                                post
                        );

                    } else {

                        blogList.add(
                                0,
                                post
                        );
                    }

                    blogRecyclerAdapter
                            .notifyItemInserted(
                                    Math.max(
                                            0,
                                            Math.min(
                                                    newIndex,
                                                    blogList.size() - 1
                                            )
                                    )
                            );
                }
            }

            // =====================================================
            // MODIFIED
            // =====================================================

            else if (change.getType()
                    == DocumentChange.Type.MODIFIED) {

                int oldIndex =
                        findPostIndex(documentId);

                BlogPost post =
                        document.toObject(
                                BlogPost.class
                        );

                if (post == null) {
                    continue;
                }

                post.withId(documentId);

                /*
                 * If the post already exists, update it.
                 */

                if (oldIndex != -1) {

                    blogList.set(
                            oldIndex,
                            post
                    );

                    /*
                     * If Firestore changed its position because the
                     * timestamp/order changed, move it.
                     */

                    int newIndex =
                            change.getNewIndex();

                    if (newIndex >= 0
                            && newIndex != oldIndex
                            && newIndex < blogList.size()) {

                        BlogPost updatedPost =
                                blogList.remove(
                                        oldIndex
                                );

                        /*
                         * Adjust index after removal.
                         */

                        if (newIndex > oldIndex) {
                            newIndex--;
                        }

                        blogList.add(
                                newIndex,
                                updatedPost
                        );

                        blogRecyclerAdapter
                                .notifyItemMoved(
                                        oldIndex,
                                        newIndex
                                );

                    } else {

                        blogRecyclerAdapter
                                .notifyItemChanged(
                                        oldIndex
                                );
                    }

                } else {

                    /*
                     * Safety fallback if the post wasn't found.
                     */

                    int newIndex =
                            change.getNewIndex();

                    if (newIndex < 0
                            || newIndex > blogList.size()) {

                        newIndex =
                                blogList.size();
                    }

                    blogList.add(
                            newIndex,
                            post
                    );

                    blogRecyclerAdapter
                            .notifyItemInserted(
                                    newIndex
                            );
                }
            }

            // =====================================================
            // REMOVED
            // =====================================================

            else if (change.getType()
                    == DocumentChange.Type.REMOVED) {

                int removeIndex =
                        findPostIndex(documentId);

                if (removeIndex != -1) {

                    blogList.remove(
                            removeIndex
                    );

                    blogRecyclerAdapter
                            .notifyItemRemoved(
                                    removeIndex
                            );
                }
            }
        }
    }

    // =============================================================
    // FIND POST BY FIRESTORE ID
    // =============================================================

    private int findPostIndex(
            String postId
    ) {

        if (postId == null) {
            return -1;
        }

        for (int i = 0;
             i < blogList.size();
             i++) {

            BlogPost post =
                    blogList.get(i);

            if (post != null
                    && postId.equals(
                    post.BlogPostId
            )) {

                return i;
            }
        }

        return -1;
    }

    // =============================================================
    // LOAD MORE POSTS
    // =============================================================
    //
    // "Loading more" widens the live query's limit and re-attaches
    // the listener, instead of doing a separate one-shot fetch. This
    // keeps every loaded post - old and new pages alike - synced to
    // add/modify/delete events, regardless of which screen triggers
    // them.
    // =============================================================

    private void loadMorePost() {

        if (firebaseFirestore == null
                || !hasMoreData
                || isLoadingMore) {

            return;
        }

        isLoadingMore = true;

        currentLimit += PAGE_SIZE;

        attachPostsListener();
    }

    // =============================================================
    // DESTROY VIEW
    // =============================================================

    @Override
    public void onDestroyView() {

        /*
         * Remove the manually-managed Firestore listener.
         */

        if (postsListenerRegistration != null) {

            postsListenerRegistration.remove();

            postsListenerRegistration = null;
        }

        if (blogListView != null) {

            blogListView.clearOnScrollListeners();
        }

        super.onDestroyView();
    }
}