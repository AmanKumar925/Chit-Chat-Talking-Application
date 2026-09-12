package com.aman.talkingcommunity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private Toolbar mainToolbar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAnalytics firebaseAnalytics;

    private String current_user_id;

    private FloatingActionButton addPostBtn;
    private BottomNavigationView mainbottomNav;

    private HomeFragment homeFragment;
    private NotificationFragment notificationFragment;
    private AccountFragment accountFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ---------------------------------------------------------
        // Firebase
        // ---------------------------------------------------------

        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // ---------------------------------------------------------
        // Toolbar
        // ---------------------------------------------------------

        mainToolbar = findViewById(R.id.desc_toolbar);

        if (mainToolbar != null) {

            setSupportActionBar(mainToolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Mans Community");
            }

            /*
             * Fix toolbar overlapping the status bar.
             *
             * The top padding is automatically taken from the
             * status-bar height, so this works on different devices.
             */
            ViewCompat.setOnApplyWindowInsetsListener(
                    mainToolbar,
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

            ViewCompat.requestApplyInsets(mainToolbar);
        }

        // ---------------------------------------------------------
        // Check logged-in user
        // ---------------------------------------------------------

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            sendToLogin();
            return;
        }

        current_user_id = currentUser.getUid();

        // ---------------------------------------------------------
        // Bottom Navigation
        // ---------------------------------------------------------

        mainbottomNav = findViewById(R.id.mainBottomNav);

        homeFragment = new HomeFragment();
        notificationFragment = new NotificationFragment();
        accountFragment = new AccountFragment();

        initializeFragment();

        if (mainbottomNav != null) {

            mainbottomNav.setOnNavigationItemSelectedListener(
                    new BottomNavigationView.OnNavigationItemSelectedListener() {

                        @Override
                        public boolean onNavigationItemSelected(
                                @NonNull MenuItem item) {

                            Fragment currentFragment =
                                    getSupportFragmentManager()
                                            .findFragmentById(
                                                    R.id.main_container
                                            );

                            if (item.getItemId()
                                    == R.id.bottom_action_home) {

                                replaceFragment(
                                        homeFragment,
                                        currentFragment
                                );

                                return true;

                            } else if (item.getItemId()
                                    == R.id.bottom_my_post) {

                                replaceFragment(
                                        notificationFragment,
                                        currentFragment
                                );

                                return true;

                            } else {

                                return false;
                            }
                        }
                    }
            );
        }

        // ---------------------------------------------------------
        // Add Post Button
        // ---------------------------------------------------------

        addPostBtn = findViewById(R.id.add_post_btn);

        if (addPostBtn != null) {

            addPostBtn.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            if (current_user_id != null
                                    && current_user_id.equals(
                                    getResources().getString(
                                            R.string.demoid
                                    ))) {

                                AlertDialog.Builder builder =
                                        new AlertDialog.Builder(
                                                MainActivity.this
                                        );

                                builder.setTitle("Log In")
                                        .setMessage(
                                                "Log In With Official "
                                                        + "Account To Post."
                                        )
                                        .setNegativeButton(
                                                "No",
                                                (dialog, which) ->
                                                        dialog.dismiss()
                                        )
                                        .setPositiveButton(
                                                "OK",
                                                (dialog, which) ->
                                                        logOut()
                                        );

                                builder.create().show();

                            } else {

                                startActivity(
                                        new Intent(
                                                MainActivity.this,
                                                NewPostActivity.class
                                        )
                                );
                            }
                        }
                    }
            );
        }
    }

    // ---------------------------------------------------------
    // Check User Profile
    // ---------------------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            sendToLogin();
            return;
        }

        current_user_id = currentUser.getUid();

        if (firebaseFirestore == null) {
            firebaseFirestore =
                    FirebaseFirestore.getInstance();
        }

        firebaseFirestore.collection("Users")
                .document(current_user_id)
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        DocumentSnapshot snapshot =
                                task.getResult();

                        if (snapshot != null
                                && !snapshot.exists()
                                && !isFinishing()) {

                            startActivity(
                                    new Intent(
                                            MainActivity.this,
                                            SetupActivity.class
                                    )
                            );

                            finish();
                        }

                    } else {

                        Exception exception =
                                task.getException();

                        if (exception != null
                                && !isFinishing()) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Error : "
                                            + exception.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                });
    }

    // ---------------------------------------------------------
    // Options Menu
    // ---------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(
                R.menu.main_menu,
                menu
        );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(
            MenuItem item) {

        if (item.getItemId()
                == R.id.action_logout_btn) {

            logOut();

            return true;

        } else if (item.getItemId()
                == R.id.action_settings_btn) {

            startActivity(
                    new Intent(
                            MainActivity.this,
                            SetupActivity.class
                    )
            );

            return true;

        } else {

            return super.onOptionsItemSelected(item);
        }
    }

    // ---------------------------------------------------------
    // Logout
    // ---------------------------------------------------------

    private void logOut() {

        if (mAuth != null) {
            mAuth.signOut();
        }

        sendToLogin();
    }

    // ---------------------------------------------------------
    // Send User To Login
    // ---------------------------------------------------------

    private void sendToLogin() {

        Intent intent =
                new Intent(
                        MainActivity.this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    // ---------------------------------------------------------
    // Initialize Fragments
    // ---------------------------------------------------------

    private void initializeFragment() {

        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();

        transaction.add(
                R.id.main_container,
                homeFragment
        );

        transaction.add(
                R.id.main_container,
                notificationFragment
        );

        transaction.add(
                R.id.main_container,
                accountFragment
        );

        transaction.hide(notificationFragment);
        transaction.hide(accountFragment);

        transaction.commit();
    }

    // ---------------------------------------------------------
    // Replace / Show Fragment
    // ---------------------------------------------------------

    private void replaceFragment(
            Fragment fragment,
            Fragment currentFragment
    ) {

        if (fragment == null) {
            return;
        }

        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();

        if (fragment == homeFragment) {

            transaction.hide(accountFragment);
            transaction.hide(notificationFragment);

        } else if (fragment == accountFragment) {

            transaction.hide(homeFragment);
            transaction.hide(notificationFragment);

        } else if (fragment == notificationFragment) {

            transaction.hide(homeFragment);
            transaction.hide(accountFragment);
        }

        transaction.show(fragment);

        transaction.commit();
    }
}