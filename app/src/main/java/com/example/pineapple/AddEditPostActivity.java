package com.example.pineapple;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddEditPostActivity extends AppCompatActivity {

    private EditText postTitleInput;
    private EditText postContentInput;
    private Spinner communitySpinner;  // Added as a class field
    private Button savePostButton;
    private ImageView backButton;
    private int position = -1;
    private FirebaseFirestore db;

    // Map to store the community name and its ID
    private Map<String, String> communityMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_post);
        

        // Initialize UI components
        postTitleInput = findViewById(R.id.postTitleInput);
        postContentInput = findViewById(R.id.postContentInput);
        savePostButton = findViewById(R.id.savePostButton);
        backButton = findViewById(R.id.backButton);
        communitySpinner = findViewById(R.id.communitySpinner);

        db = FirebaseFirestore.getInstance();

        backButton.setOnClickListener(v -> onBackPressed());

        populateCommunitiesSpinner();

        Intent intent = getIntent();
        String defaultCommunityId = intent.getStringExtra("communityId");
        String postId = intent.getStringExtra("postId"); // Debugging postId

        // Log the postId to check if it is received
        if (postId != null) {
            Toast.makeText(this, "Editing Post ID: " + postId, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Creating New Post", Toast.LENGTH_SHORT).show();
        }

        if (intent.hasExtra("title") && intent.hasExtra("content")) {
            position = intent.getIntExtra("position", -1);
            String title = intent.getStringExtra("title");
            String content = intent.getStringExtra("content");
            String communityId = intent.getStringExtra("community");

            Post post = new Post(title, content, null, communityId);
            displayPost(post);

            communitySpinner.post(() -> setDefaultCommunity(communityId));

            savePostButton.setText("Update");
        } else {
            savePostButton.setText("Save");
            if (defaultCommunityId != null) {
                setDefaultCommunity(defaultCommunityId);
            }
        }

        savePostButton.setOnClickListener(v -> saveOrUpdatePost());
    }

    private void setDefaultCommunity(String communityId) {
        db.collection("community").document(communityId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String communityName = documentSnapshot.getString("name");
                        if (communityName != null) {
                            // Wait until the spinner adapter is populated
                            ArrayAdapter<String> adapter = (ArrayAdapter<String>) communitySpinner.getAdapter();
                            if (adapter != null) {
                                int spinnerPosition = adapter.getPosition(communityName);
                                if (spinnerPosition >= 0) {
                                    communitySpinner.setSelection(spinnerPosition);
                                } else {
                                    Toast.makeText(this, "Community not found in the list.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error setting default community: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void populateCommunitiesSpinner() {
        db.collection("community")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> communityList = new ArrayList<>();
                        communityList.add("Select Community");

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String communityName = document.getString("name");
                            String communityId = document.getId();
                            if (communityName != null && communityId != null) {
                                communityList.add(communityName);
                                communityMap.put(communityName, communityId);
                            }
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, communityList);
                        adapter.setDropDownViewResource(R.layout.spinner_item);
                        communitySpinner.setAdapter(adapter);

                        // Set default community after the spinner is populated
                        Intent intent = getIntent();
                        if (intent.hasExtra("communityId")) {
                            String defaultCommunityId = intent.getStringExtra("communityId");
                            if (defaultCommunityId != null) {
                                setDefaultCommunity(defaultCommunityId);
                            }
                        }

                    } else {
                        Toast.makeText(this, "Error fetching communities: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void saveOrUpdatePost() {
        String title = postTitleInput.getText().toString().trim();
        String content = postContentInput.getText().toString().trim();
        String communityName = communitySpinner.getSelectedItem().toString(); // Get selected community name
        String currentUserId = FirebaseAuth.getInstance().getUid(); // Get the current user's UID

        if (!title.isEmpty() && !content.isEmpty() && !communityName.equals("Select Community")) {
            if (currentUserId == null) {
                Toast.makeText(this, "User not logged in. Cannot save post.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the community ID from the map
            String communityId = communityMap.get(communityName);

            // Check if editing an existing post
            Intent intent = getIntent();
            String postId = intent.getStringExtra("postId"); // Get the postId from intent

            if (postId != null && !postId.isEmpty()) {
                // Update the existing post
                Map<String, Object> postUpdates = new HashMap<>();
                postUpdates.put("title", title);
                postUpdates.put("content", content);
                postUpdates.put("community", communityId);

                db.collection("posts").document(postId)
                        .update(postUpdates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Post updated successfully!", Toast.LENGTH_SHORT).show();
                            finish(); // Close the activity
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error updating post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                // Create a new post
                Post newPost = new Post(title, content, currentUserId, communityId);

                db.collection("posts")
                        .add(newPost) // Add the post to Firestore
                        .addOnSuccessListener(documentReference -> {
                            // Set the Firestore document ID as the postId
                            String newPostId = documentReference.getId();
                            newPost.setId(newPostId); // Update the Post object with the ID

                            // Update the post in Firestore with the postId
                            db.collection("posts").document(newPostId)
                                    .update("id", newPostId)
                                    .addOnSuccessListener(aVoid -> {
                                        // Increment the post count in the community document
                                        incrementCommunityPostCount(communityId);

                                        Toast.makeText(this, "Post saved successfully!", Toast.LENGTH_SHORT).show();
                                        finish(); // Close the activity
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Error updating post ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error saving post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        } else {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
        }
    }

    private void incrementCommunityPostCount(String communityId) {
        db.collection("community").document(communityId)
                .update("postCount", FieldValue.increment(1))  // Increment the post count by 1
                .addOnSuccessListener(aVoid -> {
                    // Log success
                    Toast.makeText(this, "Community post count updated.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Log failure
                    Toast.makeText(this, "Failed to update community post count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void displayPost(Post post) {
        postTitleInput.setText(post.getTitle());
        postContentInput.setText(post.getContent());

        // Fetch the community name using the community ID
        String communityId = post.getCommunity();
        db.collection("community").document(communityId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String communityName = documentSnapshot.getString("name");
                        if (communityName != null) {
                            if (communitySpinner.getAdapter() == null) {
                                Toast.makeText(this, "Spinner adapter is null", Toast.LENGTH_SHORT).show();
                                return; // Prevent further execution
                            }

                            ArrayAdapter<String> adapter = (ArrayAdapter<String>) communitySpinner.getAdapter();
                            int spinnerPosition = adapter.getPosition(communityName);
                            communitySpinner.setSelection(spinnerPosition);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching community name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


}
