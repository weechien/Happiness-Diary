package io.branch.branchandroiddemo;

import android.app.Application;

import io.branch.referral.Branch;

public final class BranchAppClass extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the Branch object
        Branch.getAutoInstance(this);
    }
}
