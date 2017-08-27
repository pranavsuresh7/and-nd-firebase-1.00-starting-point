/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.udacity.friendlychat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN=1;
    private FirebaseDatabase mFirebasedatabase;
    private DatabaseReference mMessagesDataBaseReference;
    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    protected ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    //wheneveer a data is added in database childevent listener takes data at that instance

    private String mUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);
        mFirebasedatabase= FirebaseDatabase.getInstance();
        //main acess point to database
        mFirebaseAuth = FirebaseAuth.getInstance();
 //getting instance of firebaseAutheeentication
        mMessagesDataBaseReference= mFirebasedatabase.getReference().child("message");
        //getting to root node and message portion of database
        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null);
              //Friendly message oject has 3 variable
                //This object has all the keys that we’ll store as a message in the realtime database.
                mMessagesDataBaseReference.push().setValue(friendlyMessage);
                //we’re only sending text messages for now  so we’ll create a FriendlyMessage object with all the fields except for photoUrl, which will be null
                //creating a new username key for messages in realtime database
                // Clear input box
                mMessageEditText.setText("");
            }
        });

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
//has two statess signed in or signed out
                FirebaseUser user=firebaseAuth.getCurrentUser();
                if(user!=null){
                   onSignedinInitialize(user.getDisplayName());

                }
                else {
                    onSinedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance().createSignInIntentBuilder().setIsSmartLockEnabled(false).setProviders(AuthUI.EMAIL_PROVIDER,
                                    AuthUI.GOOGLE_PROVIDER).build(),RC_SIGN_IN);
                                    //smartlock is used to save  password on users credentials and automativ=cally sugn in to the app
                }
            }
        };
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        mMessageAdapter.clear();
        detachDatbaseReadListener();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
                super.onActivityResult(requestCode, resultCode, data);
                if (requestCode == RC_SIGN_IN) {
                        if (resultCode == RESULT_OK) {
                                // Sign-in succeeded, set up the UI
                                        Toast.makeText(this, "Welcome to IOF Agriculture!", Toast.LENGTH_SHORT).show();
                            } else if (resultCode == RESULT_CANCELED) {
                                // Sign in was canceled by the user, finish the activity
                                        Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                    }
            }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
                        case R.id.sign_out_menu:
                                AuthUI.getInstance().signOut(this);
                            return true;
                        default:
                                return super.onOptionsItemSelected(item);
                    }
    }



    private void onSignedinInitialize(String username)
    {
      mUsername=username;
        attachDatabaseReadlistener();

    }
    private void onSinedOutCleanup()
    {
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachDatbaseReadListener();

    }
    private  void  attachDatabaseReadlistener(){
        if (mChildEventListener==null){
        mChildEventListener =new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                //this is the method which calls whenever a message is added in the realtime datbase
                //always contains the message that has been added
                FriendlyMessage friendlyMessage
                        =  dataSnapshot.getValue(FriendlyMessage.class);

                //this will take the message data and desirialize and passes FriendlyMessage class
                mMessageAdapter.add(friendlyMessage);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                //this gets called whenever a meassage is changed
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //whenever a message is deleted

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // you don't have permisiion to read the data

            }
        };
        mMessagesDataBaseReference.addChildEventListener(mChildEventListener);
    }}
        private void detachDatbaseReadListener() {
            if (mChildEventListener != null) {
                mMessagesDataBaseReference.removeEventListener(mChildEventListener);
                mChildEventListener = null;
            }
        }
}
