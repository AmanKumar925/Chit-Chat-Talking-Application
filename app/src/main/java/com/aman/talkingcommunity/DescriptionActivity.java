package com.aman.talkingcommunity;

import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class DescriptionActivity extends AppCompatActivity {

    private Toolbar descriptionToolbar;

    private TextView descActivityTitle;
    private TextView descActivityDesc;

    private ImageView descActivityImage;

    private Button descActivityAudioBtn;

    private View statusBarSpace;

    private String blogPostId;

    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Enable edge-to-edge.
         *
         * The status bar and navigation bar are handled
         * separately using WindowInsets.
         */
        WindowCompat.setDecorFitsSystemWindows(
                getWindow(),
                false
        );

        setContentView(R.layout.activity_description);

        /*
         * White system bars.
         */
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);

        /*
         * Black system-bar icons on white background.
         */
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        // ---------------------------------------------------------
        // TOOLBAR / SYSTEM BAR
        // ---------------------------------------------------------

        setupToolbar();

        // ---------------------------------------------------------
        // GET BLOG POST ID
        // ---------------------------------------------------------

        blogPostId =
                getIntent().getStringExtra("blog_post_id");

        // ---------------------------------------------------------
        // FIND VIEWS
        // ---------------------------------------------------------

        descActivityTitle =
                findViewById(R.id.desc_activity_title);

        descActivityDesc =
                findViewById(R.id.desc_activity_desc);

        descActivityImage =
                findViewById(R.id.desc_activity_image);

        descActivityAudioBtn =
                findViewById(R.id.desc_activity_audio_btn);

        // ---------------------------------------------------------
        // FIREBASE
        // ---------------------------------------------------------

        firebaseFirestore =
                FirebaseFirestore.getInstance();

        firebaseAuth =
                FirebaseAuth.getInstance();

        // ---------------------------------------------------------
        // VALIDATE POST ID
        // ---------------------------------------------------------

        if (blogPostId == null
                || blogPostId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Post not found",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }

        // ---------------------------------------------------------
        // VALIDATE USER
        // ---------------------------------------------------------

        if (firebaseAuth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Please login first",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }

        // ---------------------------------------------------------
        // AUDIO BUTTON
        // ---------------------------------------------------------

        setupAudioButton();

        // ---------------------------------------------------------
        // TEXT TO SPEECH
        // ---------------------------------------------------------

        setupTextToSpeech();

        // ---------------------------------------------------------
        // LOAD POST
        // ---------------------------------------------------------

        loadBlogPost();
    }

    // =============================================================
    // TOOLBAR SETUP
    // =============================================================

    private void setupToolbar() {

        descriptionToolbar =
                findViewById(R.id.desc_toolbar);

        if (descriptionToolbar == null) {
            return;
        }

        /*
         * Use Toolbar as the Activity ActionBar.
         */
        setSupportActionBar(descriptionToolbar);

        /*
         * White toolbar.
         */
        descriptionToolbar.setBackgroundColor(
                Color.WHITE
        );

        descriptionToolbar.setTitleTextColor(
                Color.BLACK
        );

        /*
         * Keep normal ActionBar height.
         */
        descriptionToolbar.setMinimumHeight(
                getResources().getDimensionPixelSize(
                        androidx.appcompat.R.dimen
                                .abc_action_bar_default_height_material
                )
        );

        /*
         * Toolbar title and back button.
         */
        if (getSupportActionBar() != null) {

            getSupportActionBar().setTitle("Post");

            getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(true);

            getSupportActionBar()
                    .setDisplayShowHomeEnabled(true);
        }

        /*
         * Black back arrow.
         */
        if (descriptionToolbar.getNavigationIcon() != null) {

            descriptionToolbar
                    .getNavigationIcon()
                    .setTint(Color.BLACK);
        }

        // ---------------------------------------------------------
        // STATUS BAR
        // ---------------------------------------------------------

        statusBarSpace =
                findViewById(R.id.status_bar_space);

        if (statusBarSpace != null) {

            ViewCompat.setOnApplyWindowInsetsListener(
                    statusBarSpace,
                    (view, windowInsets) -> {

                        Insets statusInsets =
                                windowInsets.getInsets(
                                        WindowInsetsCompat.Type
                                                .statusBars()
                                );

                        ViewGroup.LayoutParams params =
                                view.getLayoutParams();

                        params.height =
                                statusInsets.top;

                        view.setLayoutParams(params);

                        return windowInsets;
                    }
            );

            ViewCompat.requestApplyInsets(
                    statusBarSpace
            );
        }
    }

    // =============================================================
    // AUDIO BUTTON
    // =============================================================

    private void setupAudioButton() {

        if (descActivityAudioBtn == null) {
            return;
        }

        descActivityAudioBtn.setOnClickListener(v -> {

            String description = "";

            if (descActivityDesc != null) {

                description =
                        descActivityDesc
                                .getText()
                                .toString();
            }

            if (description.trim().isEmpty()) {

                Toast.makeText(
                        DescriptionActivity.this,
                        "No description available",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (textToSpeech != null) {

                textToSpeech.speak(
                        description,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "description"
                );
            }
        });
    }

    // =============================================================
    // TEXT TO SPEECH
    // =============================================================

    private void setupTextToSpeech() {

        textToSpeech =
                new TextToSpeech(
                        getApplicationContext(),
                        status -> {

                            if (status
                                    == TextToSpeech.SUCCESS) {

                                int result =
                                        textToSpeech.setLanguage(
                                                Locale.ENGLISH
                                        );

                                if (result
                                        == TextToSpeech
                                        .LANG_MISSING_DATA
                                        || result
                                        == TextToSpeech
                                        .LANG_NOT_SUPPORTED) {

                                    Toast.makeText(
                                            DescriptionActivity.this,
                                            "English language is not supported",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }
                        }
                );
    }

    // =============================================================
    // LOAD BLOG POST
    // =============================================================

    private void loadBlogPost() {

        if (firebaseFirestore == null
                || blogPostId == null) {

            return;
        }

        firebaseFirestore
                .collection("Posts")
                .document(blogPostId)
                .get()
                .addOnSuccessListener(
                        this::setBlogPost
                )
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            DescriptionActivity.this,
                            "Failed to load post",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // =============================================================
    // SET BLOG POST DATA
    // =============================================================

    private void setBlogPost(
            @NonNull DocumentSnapshot documentSnapshot
    ) {

        if (!documentSnapshot.exists()) {

            Toast.makeText(
                    this,
                    "Post not found",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }

        // ---------------------------------------------------------
        // TITLE
        // ---------------------------------------------------------

        String title =
                documentSnapshot.getString("title");

        if (title == null
                || title.trim().isEmpty()) {

            title = "Post";
        }

        if (descActivityTitle != null) {

            descActivityTitle.setText(title);
        }

        // ---------------------------------------------------------
        // DESCRIPTION
        // ---------------------------------------------------------

        String description =
                documentSnapshot.getString("desc");

        if (description == null
                || description.trim().isEmpty()) {

            description =
                    "No description available.";
        }

        if (descActivityDesc != null) {

            descActivityDesc.setText(
                    description
            );
        }

        // ---------------------------------------------------------
        // IMAGE
        // ---------------------------------------------------------

        String imageUrl =
                documentSnapshot.getString(
                        "image_url"
                );

        if (descActivityImage != null) {

            if (imageUrl != null
                    && !imageUrl.trim().isEmpty()) {

                descActivityImage.setVisibility(
                        View.VISIBLE
                );

                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(
                                R.drawable.default_image
                        )
                        .error(
                                R.drawable.default_image
                        )
                        .into(descActivityImage);

            } else {

                descActivityImage.setVisibility(
                        View.GONE
                );
            }
        }
    }

    // =============================================================
    // TOOLBAR BACK BUTTON
    // =============================================================

    @Override
    public boolean onSupportNavigateUp() {

        getOnBackPressedDispatcher()
                .onBackPressed();

        return true;
    }

    // =============================================================
    // DESTROY
    // =============================================================

    @Override
    protected void onDestroy() {

        if (textToSpeech != null) {

            textToSpeech.stop();
            textToSpeech.shutdown();

            textToSpeech = null;
        }

        super.onDestroy();
    }
}