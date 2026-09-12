package com.aman.talkingcommunity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NewPostActivity extends AppCompatActivity {

    private static final int SPEECH_REQUEST_CODE = 1;
    private static final int IMAGE_PICK_REQUEST_CODE = 100;

    private Toolbar newPostToolbar;
    private ImageView newPostImage;
    private EditText newPostDesc;
    private EditText newPostTitle;
    private Button newPostBtn;
    private ProgressBar newPostProgress;
    private Button speechToTextBtn;

    private Uri postImageUri;

    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        // =========================================================
        // FIREBASE
        // =========================================================

        firebaseFirestore = FirebaseFirestore.getInstance();
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

        // =========================================================
        // TOOLBAR
        // =========================================================

        newPostToolbar =
                findViewById(R.id.new_post_toolbar);

        if (newPostToolbar != null) {

            setSupportActionBar(newPostToolbar);

            if (getSupportActionBar() != null) {

                getSupportActionBar().setTitle(
                        "Add New Post"
                );

                getSupportActionBar()
                        .setDisplayHomeAsUpEnabled(true);
            }

            // -----------------------------------------------------
            // FIX STATUS BAR / TOOLBAR OVERLAP
            // -----------------------------------------------------

            ViewCompat.setOnApplyWindowInsetsListener(
                    newPostToolbar,
                    (view, windowInsets) -> {

                        Insets systemBars =
                                windowInsets.getInsets(
                                        WindowInsetsCompat.Type.systemBars()
                                );

                        view.setPadding(
                                view.getPaddingLeft(),
                                systemBars.top,
                                view.getPaddingRight(),
                                view.getPaddingBottom()
                        );

                        return windowInsets;
                    }
            );

            ViewCompat.requestApplyInsets(
                    newPostToolbar
            );
        }

        // =========================================================
        // VIEWS
        // =========================================================

        newPostImage =
                findViewById(R.id.new_post_image);

        newPostDesc =
                findViewById(R.id.new_post_desc);

        newPostTitle =
                findViewById(R.id.new_post_title);

        newPostBtn =
                findViewById(R.id.post_btn);

        newPostProgress =
                findViewById(R.id.new_post_progress);

        speechToTextBtn =
                findViewById(R.id.speechToText_btn);

        // =========================================================
        // SPEECH TO TEXT
        // =========================================================

        if (speechToTextBtn != null) {

            speechToTextBtn.setOnClickListener(
                    v -> startSpeechRecognition()
            );
        }

        // =========================================================
        // SELECT IMAGE
        // =========================================================

        if (newPostImage != null) {

            newPostImage.setOnClickListener(
                    v -> openImagePicker()
            );
        }

        // =========================================================
        // SUBMIT POST
        // =========================================================

        if (newPostBtn != null) {

            newPostBtn.setOnClickListener(
                    v -> submitPost()
            );
        }
    }

    // =========================================================
    // IMAGE PICKER
    // =========================================================

    private void openImagePicker() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.setType("image/*");

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        try {

            startActivityForResult(
                    intent,
                    IMAGE_PICK_REQUEST_CODE
            );

        } catch (ActivityNotFoundException e) {

            // -----------------------------------------------------
            // FALLBACK
            // -----------------------------------------------------

            Intent fallbackIntent =
                    new Intent(
                            Intent.ACTION_GET_CONTENT
                    );

            fallbackIntent.setType("image/*");

            fallbackIntent.addCategory(
                    Intent.CATEGORY_OPENABLE
            );

            startActivityForResult(
                    fallbackIntent,
                    IMAGE_PICK_REQUEST_CODE
            );
        }
    }

    // =========================================================
    // SPEECH RECOGNITION
    // =========================================================

    private void startSpeechRecognition() {

        Intent intent =
                new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Listening..."
        );

        try {

            startActivityForResult(
                    intent,
                    SPEECH_REQUEST_CODE
            );

        } catch (ActivityNotFoundException e) {

            Toast.makeText(
                    this,
                    "Speech recognition is not available on this device.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // SUBMIT POST
    // =========================================================

    private void submitPost() {

        if (newPostDesc == null) {
            return;
        }

        String description =
                newPostDesc
                        .getText()
                        .toString()
                        .trim();

        String title =
                newPostTitle == null
                        ? ""
                        : newPostTitle
                        .getText()
                        .toString()
                        .trim();

        // ---------------------------------------------------------
        // VALIDATE DESCRIPTION
        // ---------------------------------------------------------

        if (TextUtils.isEmpty(description)) {

            newPostDesc.setError(
                    "Description is required."
            );

            newPostDesc.requestFocus();

            return;
        }

        // ---------------------------------------------------------
        // CHECK AUTHENTICATION
        // ---------------------------------------------------------

        if (firebaseAuth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        currentUserId =
                firebaseAuth
                        .getCurrentUser()
                        .getUid();

        // ---------------------------------------------------------
        // SHOW PROGRESS
        // ---------------------------------------------------------

        if (newPostProgress != null) {

            newPostProgress.setVisibility(
                    View.VISIBLE
            );
        }

        if (newPostBtn != null) {

            newPostBtn.setEnabled(false);
        }

        // =========================================================
        // CREATE POST DATA
        // =========================================================

        Map<String, Object> postMap =
                new HashMap<>();

        postMap.put(
                "title",
                title
        );

        postMap.put(
                "desc",
                description
        );

        postMap.put(
                "user_id",
                currentUserId
        );

        postMap.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        // ---------------------------------------------------------
        // SAVE IMAGE URI
        // ---------------------------------------------------------

        if (postImageUri != null) {

            postMap.put(
                    "image_uri",
                    postImageUri.toString()
            );
        }

        // =========================================================
        // ADD POST TO FIRESTORE
        // =========================================================

        firebaseFirestore
                .collection("Posts")
                .add(postMap)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                NewPostActivity.this,
                                "Post was added",
                                Toast.LENGTH_LONG
                        ).show();

                        Intent intent =
                                new Intent(
                                        NewPostActivity.this,
                                        MainActivity.class
                                );

                        // Prevent multiple MainActivity instances
                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        );

                        startActivity(intent);

                        finish();

                    } else {

                        String error;

                        if (task.getException() != null) {

                            error =
                                    task.getException()
                                            .getMessage();

                        } else {

                            error =
                                    "Unable to add post.";
                        }

                        Toast.makeText(
                                NewPostActivity.this,
                                "(FIRESTORE Error) : "
                                        + error,
                                Toast.LENGTH_LONG
                        ).show();

                        if (newPostProgress != null) {

                            newPostProgress.setVisibility(
                                    View.INVISIBLE
                            );
                        }

                        if (newPostBtn != null) {

                            newPostBtn.setEnabled(true);
                        }
                    }
                });
    }

    // =========================================================
    // ACTIVITY RESULT
    // =========================================================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        // =========================================================
        // SPEECH RESULT
        // =========================================================

        if (requestCode == SPEECH_REQUEST_CODE) {

            if (resultCode == RESULT_OK
                    && data != null) {

                ArrayList<String> results =
                        data.getStringArrayListExtra(
                                RecognizerIntent.EXTRA_RESULTS
                        );

                if (results != null
                        && !results.isEmpty()
                        && newPostDesc != null) {

                    String spokenText =
                            results.get(0);

                    newPostDesc.setText(
                            spokenText
                    );

                    newPostDesc.setSelection(
                            newPostDesc.length()
                    );
                }
            }

            return;
        }

        // =========================================================
        // IMAGE PICKER RESULT
        // =========================================================

        if (requestCode == IMAGE_PICK_REQUEST_CODE) {

            if (resultCode == RESULT_OK
                    && data != null
                    && data.getData() != null) {

                postImageUri =
                        data.getData();

                // -------------------------------------------------
                // PERSIST IMAGE PERMISSION
                // -------------------------------------------------

                try {

                    getContentResolver()
                            .takePersistableUriPermission(
                                    postImageUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );

                } catch (SecurityException ignored) {
                    // Provider does not support persistable permission.
                }

                // -------------------------------------------------
                // DISPLAY SELECTED IMAGE
                // -------------------------------------------------

                if (newPostImage != null) {

                    newPostImage.setImageURI(
                            postImageUri
                    );
                }

                Toast.makeText(
                        this,
                        "Image selected",
                        Toast.LENGTH_SHORT
                ).show();

            } else if (
                    resultCode == Activity.RESULT_CANCELED
            ) {

                // User cancelled image selection.
            }
        }
    }

    // =========================================================
    // TOOLBAR BACK BUTTON
    // =========================================================

    @Override
    public boolean onSupportNavigateUp() {

        finish();

        return true;
    }
}