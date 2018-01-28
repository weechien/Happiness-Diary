package com.goh.weechien.HappinessDiary;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.goh.weechien.HappinessDiary.View.CircleRectView;
import com.goh.weechien.HappinessDiary.View.CircleToRectTransition;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Activity showing the profile pic of the user and enable user to edit their profile pic
public class UserAccountProfilePic extends AppCompatActivity {
    private static final int REQUEST_CODE_PICKER = 59482;
    List<Image> images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Start a shared element transition if the API is >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int startCircleRadius = getIntent().getIntExtra("CIRCLE_RADIUS", 0);
            getWindow().setSharedElementEnterTransition(
                    new CircleToRectTransition(startCircleRadius).setDuration(500));
            getWindow().setSharedElementExitTransition(
                    new CircleToRectTransition(startCircleRadius).setDuration(500));
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_account_profile_pic);

        // Setup the appbar
        Toolbar toolbar = findViewById(R.id.view_account_image_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Title of the activity
        TextView title = findViewById(R.id.view_account_image_toolbar_textview);
        title.setText(getString(R.string.title_activity_view_user_account_image));
    }

    @Override
    // Reverse the entrance animation
    public void onBackPressed() {
        supportFinishAfterTransition();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Run async task to read the stored profile pic path from the database
        new ReadProfilePic().execute(this, null, null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                supportFinishAfterTransition(); // Reverse the entrance animation
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Start the image picker activity when the edit button is clicked
    public void onEditImage(View view) {
        ImagePicker.create(this).single()
                .theme(R.style.UserAccount_NoActionBar)
                .start(REQUEST_CODE_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICKER && resultCode == RESULT_OK && data != null) {
            if (images == null) {
                images = new ArrayList<>();
            }
            // Get the new image and save the path to the database
            images = ImagePicker.getImages(data);
            new WriteProfilePic().execute(this, null, null);

            // Send the result back to the UserAccount activity
            // The result is set but the onActivity Result of the UserAccount activity will
            // only be called when this activity finishes
            Intent intent = new Intent();
            intent.putParcelableArrayListExtra("IMAGES", (ArrayList<Image>) images);
            setResult(RESULT_OK, intent);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
    }

    // Read the database and get the image path
    private class ReadProfilePic extends AsyncTask<Context, Void, Void> {
        ProfilePicDbHelper dbHelper;
        List<String> imageList;

        @Override
        // Before entering the background thread
        protected void onPreExecute() {
            // Get the database helper
            dbHelper = new ProfilePicDbHelper(getApplicationContext());
        }

        @Override
        // Background thread
        protected Void doInBackground(Context... contexts) {
            // Gets the data repository in read mode
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // Define a projection that specifies which columns from the database
            // you will actually use after this query.
            String[] projection = {
                    ProfilePicContract.ProfilePic._ID,
                    ProfilePicContract.ProfilePic.COLUMN_IMAGE_PATH,
            };

            // Filter results WHERE "id" = '1'
            String selection = ProfilePicContract.ProfilePic._ID + " = ?";
            String[] selectionArgs = {"1"};

            // Cursor to query and fetch data
            Cursor mCursor = db.query(
                    ProfilePicContract.ProfilePic.TABLE_NAME, // The table to query
                    projection,                               // The columns to return
                    selection,                                // The columns for the WHERE clause
                    selectionArgs,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    null                                      // The sort order
            );

            // Loop through the cursor and read the image path
            while (mCursor.moveToNext()) {
                imageList = new ArrayList<>();
                String imagePath = mCursor.getString(
                        mCursor.getColumnIndexOrThrow(ProfilePicContract.ProfilePic.COLUMN_IMAGE_PATH)
                );
                imageList.add(imagePath);
            }
            mCursor.close();
            return null;
        }

        @Override
        // Update the UI
        protected void onPostExecute(Void aVoid) {
            dbHelper.close();
            CircleRectView imageView = findViewById(R.id.expanded_profile_pic);

            // Check if the image path is valid or not
            File file = new File(imageList.get(0));
            if (file.exists()) {
                // Load the image with Glide
                Glide.with(getApplicationContext()).load(imageList.get(0)).into(imageView);
            } else {
                // If image path does not have any image, use the default image
                Uri path = Uri.parse("android.resource://" + getPackageName() + "/"
                        + R.drawable.jumping_girl);
                String imagePath = path.toString();
                Glide.with(getApplicationContext()).load(imagePath).into(imageView);
            }
        }
    }

    // Write the database and get the image path
    private class WriteProfilePic extends AsyncTask<Context, Void, Void> {
        ProfilePicDbHelper dbHelper;

        @Override
        // Before entering the background thread
        protected void onPreExecute() {
            // Get the database helper
            dbHelper = new ProfilePicDbHelper(getApplicationContext());
        }

        @Override
        // Background thread
        protected Void doInBackground(Context... contexts) {
            // Gets the data repository in write mode
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // New value for one column
            ContentValues values = new ContentValues();
            values.put(ProfilePicContract.ProfilePic.COLUMN_IMAGE_PATH, images.get(0).getPath());

            // Which row to update, based on the title
            String selection = ProfilePicContract.ProfilePic._ID + " LIKE ?";
            String[] selectionArgs = {"1"};

            db.update(ProfilePicContract.ProfilePic.TABLE_NAME, values, selection, selectionArgs);
            return null;
        }

        @Override
        // Update the UI
        protected void onPostExecute(Void aVoid) {
            dbHelper.close();
            CircleRectView imageView = findViewById(R.id.expanded_profile_pic);

            // Check if the image path is valid or not
            File file = new File(images.get(0).getPath());
            if (file.exists()) {
                // Load the image with Glide
                Glide.with(getApplicationContext()).load(images.get(0).getPath()).into(imageView);
            } else {
                // If image path does not have any image, use the default image
                Uri path = Uri.parse("android.resource://" + getPackageName() + "/"
                        + R.drawable.jumping_girl);
                String imagePath = path.toString();
                Glide.with(getApplicationContext()).load(imagePath).into(imageView);
            }
        }
    }
}
