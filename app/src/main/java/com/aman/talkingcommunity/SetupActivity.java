package com.aman.talkingcommunity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private static final int IMAGE_PICK_REQUEST_CODE = 100;

    private CircleImageView setupImage;
    private Uri mainImageUri;

    private String userId;
    private boolean isChanged = false;

    private EditText setupName;
    private Button setupBtn;
    private ProgressBar setupProgress;

    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        // ---------------------------------------------------------
        // Firebase
        // ---------------------------------------------------------

        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        userId = firebaseAuth.getCurrentUser().getUid();

        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance()
                .getReference();

        // ---------------------------------------------------------
        // Toolbar
        // ---------------------------------------------------------

        Toolbar toolbar = findViewById(R.id.setupToolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Account Setup");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        // ---------------------------------------------------------
        // Views
        // ---------------------------------------------------------

        setupImage = findViewById(R.id.setup_image);
        setupName = findViewById(R.id.setup_name);
        setupBtn = findViewById(R.id.setup_btn);
        setupProgress = findViewById(R.id.setup_progress);

        // ---------------------------------------------------------
        // Initial State
        // ---------------------------------------------------------

        if (setupProgress != null) {
            setupProgress.setVisibility(View.VISIBLE);
        }

        if (setupBtn != null) {
            setupBtn.setEnabled(false);
        }

        // ---------------------------------------------------------
        // Load Existing User Data
        // ---------------------------------------------------------

        loadUserData();

        // ---------------------------------------------------------
        // Save Button
        // ---------------------------------------------------------

        if (setupBtn != null) {
            setupBtn.setOnClickListener(v ->
                    saveUserSettings()
            );
        }

        // ---------------------------------------------------------
        // Profile Image
        // ---------------------------------------------------------

        if (setupImage != null) {
            setupImage.setOnClickListener(v ->
                    BringImagePicker()
            );
        }
    }

    // =========================================================
    // LOAD USER DATA
    // =========================================================

    private void loadUserData() {

        firebaseFirestore
                .collection("Users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        DocumentSnapshot snapshot =
                                task.getResult();

                        if (snapshot != null
                                && snapshot.exists()) {

                            // -------------------------------
                            // Name
                            // -------------------------------

                            String name =
                                    snapshot.getString("name");

                            if (name != null
                                    && setupName != null) {

                                setupName.setText(name);
                            }

                            // -------------------------------
                            // Existing Image
                            // -------------------------------

                            String image =
                                    snapshot.getString("image");

                            /*
                             * The image URL can be loaded here
                             * using your preferred image-loading
                             * solution.
                             *
                             * No cropper library is required.
                             */
                        }

                    } else if (task.getException() != null) {

                        Toast.makeText(
                                SetupActivity.this,
                                "(FIRESTORE Retrieve Error) : "
                                        + task.getException().getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    if (setupProgress != null) {
                        setupProgress.setVisibility(
                                View.INVISIBLE
                        );
                    }

                    if (setupBtn != null) {
                        setupBtn.setEnabled(true);
                    }
                });
    }

    // =========================================================
    // SAVE USER SETTINGS
    // =========================================================

    private void saveUserSettings() {

        if (setupName == null) {
            return;
        }

        String userName =
                setupName
                        .getText()
                        .toString()
                        .trim();

        // ---------------------------------------------------------
        // Validate Name
        // ---------------------------------------------------------

        if (TextUtils.isEmpty(userName)) {

            setupName.setError(
                    "Name is required."
            );

            setupName.requestFocus();

            return;
        }

        // ---------------------------------------------------------
        // Check Authentication
        // ---------------------------------------------------------

        if (firebaseAuth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();

            return;
        }

        userId =
                firebaseAuth
                        .getCurrentUser()
                        .getUid();

        // ---------------------------------------------------------
        // Show Progress
        // ---------------------------------------------------------

        if (setupProgress != null) {
            setupProgress.setVisibility(View.VISIBLE);
        }

        if (setupBtn != null) {
            setupBtn.setEnabled(false);
        }

        // ---------------------------------------------------------
        // Upload Image If Changed
        // ---------------------------------------------------------

        if (isChanged && mainImageUri != null) {

            uploadProfileImage(userName);

        } else {

            storeFirestore(
                    userName,
                    null
            );
        }
    }

    // =========================================================
    // UPLOAD PROFILE IMAGE
    // =========================================================

    private void uploadProfileImage(String userName) {

        StorageReference imagePath =
                storageReference
                        .child("profile_images")
                        .child(userId + ".jpg");

        imagePath
                .putFile(mainImageUri)
                .continueWithTask(
                        new Continuation<
                                UploadTask.TaskSnapshot,
                                Task<Uri>
                                >() {

                            @Override
                            public Task<Uri> then(
                                    @NonNull Task<
                                            UploadTask.TaskSnapshot
                                            > task
                            ) throws Exception {

                                if (!task.isSuccessful()) {

                                    if (task.getException() != null) {
                                        throw task.getException();
                                    }

                                    throw new Exception(
                                            "Image upload failed."
                                    );
                                }

                                return imagePath.getDownloadUrl();
                            }
                        }
                )
                .addOnCompleteListener(
                        task -> {

                            if (task.isSuccessful()
                                    && task.getResult() != null) {

                                storeFirestore(
                                        userName,
                                        task.getResult()
                                );

                            } else {

                                String error;

                                if (task.getException() != null) {

                                    error =
                                            task.getException()
                                                    .getMessage();

                                } else {

                                    error =
                                            "Image upload failed.";
                                }

                                Toast.makeText(
                                        SetupActivity.this,
                                        "(IMAGE Error) : " + error,
                                        Toast.LENGTH_LONG
                                ).show();

                                if (setupProgress != null) {
                                    setupProgress.setVisibility(
                                            View.INVISIBLE
                                    );
                                }

                                if (setupBtn != null) {
                                    setupBtn.setEnabled(true);
                                }
                            }
                        }
                );
    }

    // =========================================================
    // STORE USER DATA IN FIRESTORE
    // =========================================================

    private void storeFirestore(
            String userName,
            Uri downloadUri
    ) {

        Map<String, Object> userMap =
                new HashMap<>();

        userMap.put(
                "name",
                userName
        );

        if (downloadUri != null) {

            userMap.put(
                    "image",
                    downloadUri.toString()
            );
        }

        firebaseFirestore
                .collection("Users")
                .document(userId)
                .set(userMap)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {

                            @Override
                            public void onComplete(
                                    @NonNull Task<Void> task
                            ) {

                                if (task.isSuccessful()) {

                                    Toast.makeText(
                                            SetupActivity.this,
                                            "The user settings are updated.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    Intent intent =
                                            new Intent(
                                                    SetupActivity.this,
                                                    MainActivity.class
                                            );

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
                                                "Unable to save settings.";
                                    }

                                    Toast.makeText(
                                            SetupActivity.this,
                                            "(FIRESTORE Error) : "
                                                    + error,
                                            Toast.LENGTH_LONG
                                    ).show();

                                    if (setupProgress != null) {
                                        setupProgress.setVisibility(
                                                View.INVISIBLE
                                        );
                                    }

                                    if (setupBtn != null) {
                                        setupBtn.setEnabled(true);
                                    }
                                }
                            }
                        }
                );
    }

    // =========================================================
    // OPEN ANDROID IMAGE PICKER
    // =========================================================

    private void BringImagePicker() {

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

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

            // Fallback for older/device-specific systems

            Intent fallbackIntent =
                    new Intent(Intent.ACTION_GET_CONTENT);

            fallbackIntent.setType("image/*");

            fallbackIntent.addCategory(
                    Intent.CATEGORY_OPENABLE
            );

            fallbackIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivityForResult(
                    fallbackIntent,
                    IMAGE_PICK_REQUEST_CODE
            );
        }
    }

    // =========================================================
    // IMAGE PICKER RESULT
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

        if (requestCode != IMAGE_PICK_REQUEST_CODE) {
            return;
        }

        if (resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            mainImageUri =
                    data.getData();

            isChanged =
                    mainImageUri != null;

            // Keep permission to read selected image
            try {

                getContentResolver()
                        .takePersistableUriPermission(
                                mainImageUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );

            } catch (SecurityException ignored) {
                // Some image providers don't support
                // persistable URI permissions.
            }

            // Display selected profile image
            if (setupImage != null) {

                setupImage.setImageURI(
                        mainImageUri
                );
            }

            Toast.makeText(
                    this,
                    "Profile image selected",
                    Toast.LENGTH_SHORT
            ).show();
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
