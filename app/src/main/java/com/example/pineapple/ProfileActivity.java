package com.example.pineapple;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;


public class ProfileActivity extends AppCompatActivity {

    private TextView username, postsBtn, commentsBtn, likesBtn;
    private ImageView profilePic, backBtn, signoutBtn;
    private TextView description;
    private Button editProfile;
    private ActivityResultLauncher<Intent> addEditPostLauncher;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ActivityResultLauncher<Intent> editProfileLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        username = findViewById(R.id.userName);
        profilePic = findViewById(R.id.profilePic);
        description = findViewById(R.id.userDescription);
        editProfile = findViewById(R.id.editProfileButton);
        backBtn = findViewById(R.id.backButton);
        signoutBtn = findViewById(R.id.signOut);

        editProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String updatedUsername = data.getStringExtra("updatedUsername");
                            String updatedDescription = data.getStringExtra("updatedDescription");
                            int updatedProfilePic = data.getIntExtra("updatedProfilePic", R.drawable.goombusken);

                            // Update UI with new data
                            username.setText(updatedUsername);
                            description.setText(updatedDescription);
                            profilePic.setImageResource(updatedProfilePic);

                            // Update Firestore with new profile data
                            updateUserProfile(updatedUsername, updatedDescription, updatedProfilePic);
                        }
                    }
                });

        setupBtns();
        // Load profile data from Firestore
        loadUserProfile();

        // Set up click listener for editing profile
        editProfile.setOnClickListener(this::editProfile);
    }

    protected void setActivityLayout(@LayoutRes int layoutResID) {
        FrameLayout contentFrame = findViewById(R.id.profileContent);
        View view = getLayoutInflater().inflate(layoutResID, contentFrame, false);
        contentFrame.addView(view);
    }

    private void loadUserProfile() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get data from Firestore document
                        String usernameFromFirestore = documentSnapshot.getString("username");
                        String descriptionFromFirestore = documentSnapshot.getString("description");
                        String profilePicFromFirestore = documentSnapshot.getString("profilePic");

                        // Update UI with Firestore data
                        username.setText(usernameFromFirestore);
                        description.setText(descriptionFromFirestore);
                        // Update profile picture (use a placeholder if none exists)
                        profilePic.setImageResource(profilePicFromFirestore != null ? Integer.parseInt(profilePicFromFirestore) : R.drawable.goombusken);
                    } else {
                        // Handle the case when no profile data is found
                        Toast.makeText(ProfileActivity.this, "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserProfile(String updatedUsername, String updatedDescription, int updatedProfilePic) {
        String uid = mAuth.getCurrentUser().getUid();

        // Create a map to hold the updated profile data
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("username", updatedUsername);
        updatedData.put("description", updatedDescription);
        updatedData.put("profilePic", String.valueOf(updatedProfilePic)); // Store resource ID as string (or save an actual URL)

        db.collection("users").document(uid)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }

    public void editProfile(View view) {
        Intent intent = new Intent(getApplicationContext(), EditProfileActivity.class);
        intent.putExtra("username", username.getText().toString());
        intent.putExtra("profilePic", R.drawable.goombusken);  // Replace with actual drawable or resource
        intent.putExtra("userDescription", description.getText().toString());

        editProfileLauncher.launch(intent);
    }

    private void setupBtns() {
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent( getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        signoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPreferences = sharedPreferences = getSharedPreferences("PineapplePrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear(); // This removes all stored preferences
                editor.apply();

                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                mAuth.signOut();

                // Redirect to Login Activity or show a Toast
                Intent intent = new Intent( getApplicationContext(), Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();

                Toast.makeText(getApplicationContext(), "You have been signed out.", Toast.LENGTH_SHORT).show();
            }
        });

        postsBtn = findViewById(R.id.userPostsButton);
        postsBtn.setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, ProfilePosts.class);
            startActivity(intent);
        });

        commentsBtn = findViewById(R.id.userCommentsButton);
        commentsBtn.setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, ProfileComments.class);
            startActivity(intent);
        });

//        likesBtn = findViewById(R.id.userLikesButton);
//        likesBtn.setOnClickListener(view -> {
//            Intent intent = new Intent(ProfileActivity.this, ProfileLikes.class);
//            startActivity(intent);
//        });
    }
}
