package com.goh.weechien.HappinessDiary;

import android.provider.BaseColumns;

// Contract class for the schema of SQLite
public final class ProfilePicContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private ProfilePicContract() {}

    /* Inner class that defines the table contents */
    public static class ProfilePic implements BaseColumns {
        public static final String TABLE_NAME = "myProfilePic";
        public static final String COLUMN_IMAGE_PATH = "imagePath";
    }
}
