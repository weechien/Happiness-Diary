package com.goh.weechien.HappinessDiary;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

// Use this class to adjust the snapping of the recycler view's items in Guidance
class GuidanceSnapHelper extends LinearSnapHelper {
    private OrientationHelper mVerticalHelper, mHorizontalHelper;
    private Context context;
    private String className;
    private boolean snapBottom, snapTop; // Boolean to decide where to snap to
    private static final String guidance = "Guidance";
    private static final String bookmark = "Bookmark";

    GuidanceSnapHelper(Context context, String className) {
        super();
        this.className = className;
        this.context = context; // Get the context from the fragment
    }

    @Override
    // Calculate the distance to snap to
    public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager,
                                              @NonNull View targetView) {
        int[] out = new int[2];

        if (snapTop) { // Calculate the distance to snap to the top
            snapTop = false;
            if (layoutManager.canScrollHorizontally()) {
                out[0] = distanceToStart(targetView, getHorizontalHelper(layoutManager));
            } else {
                out[0] = 0;
            }

            if (layoutManager.canScrollVertically()) {
                out[1] = distanceToStart(targetView, getVerticalHelper(layoutManager));
            } else {
                out[1] = 0;
            }
        } else if (snapBottom) { // Calculate the distance to snap to the bottom
            snapBottom = false;
            if (layoutManager.canScrollHorizontally()) {
                out[0] = distanceToEnd(targetView, getHorizontalHelper(layoutManager));
            } else {
                out[0] = 0;
            }

            if (layoutManager.canScrollVertically()) {
                out[1] = distanceToEnd(targetView, getVerticalHelper(layoutManager));
            } else {
                out[1] = 0;
            }
        }
        return out;
    }

    @Override
    // Get the view to snap to (using getViewToAttach)
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {

        if (layoutManager instanceof LinearLayoutManager) {

            if (layoutManager.canScrollHorizontally()) {
                return getViewToAttach(layoutManager);
            } else {
                return getViewToAttach(layoutManager);
            }
        }
        return super.findSnapView(layoutManager);
    }

    // Get the distance to the top
    private int distanceToStart(View targetView, OrientationHelper helper) {
        return helper.getDecoratedStart(targetView) - helper.getStartAfterPadding();
    }

    // Get the distance to the bottom
    private int distanceToEnd(View targetView, OrientationHelper helper) {
        return helper.getDecoratedEnd(targetView) - helper.getEndAfterPadding();
    }

    // Get the first visible view
    private View getViewToAttach(RecyclerView.LayoutManager layoutManager) {
        RecyclerView recyclerView = null;

        // Get the recycler view based on the calling class
        if (className.equals(guidance)) {
            // Get the fragment's recycler view to get its measurements
            recyclerView = ((AppCompatActivity) context).findViewById(R.id.frag_recycler);

        } else if (className.equals(bookmark)) {
            recyclerView = ((AppCompatActivity) context).findViewById(R.id.frag_recycler_bookmark);
        }

        int rvTop = recyclerView.getTop();
        int rvBottom = recyclerView.getBottom();

        if (layoutManager instanceof LinearLayoutManager) {
            // Get the position of the first visible child, i.e. the top child view of the recycler view
            int firstChild = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();

            // Return false if the child view is the last item
            boolean isLastItem = ((LinearLayoutManager) layoutManager)
                    .findLastCompletelyVisibleItemPosition()
                    == layoutManager.getItemCount() - 1;

            if (firstChild == RecyclerView.NO_POSITION || isLastItem) {
                return null;
            }

            // Get the measurement of the first child view
            View child = layoutManager.findViewByPosition(firstChild);
            int childBottom = child.getBottom();

            // Proceed if the end of the child view is visible, i.e. the gap between 2 child views
            if ((rvBottom > childBottom) && (childBottom > rvTop)) {
                // Proceed if the end of the child view is at the bottom half of the recycler view
                if (childBottom >= (rvBottom - rvTop) / 2) {
                    snapBottom = true;
                    return child;
                } else {
                    if (((LinearLayoutManager) layoutManager).findLastCompletelyVisibleItemPosition()
                            == layoutManager.getItemCount() - 1) {
                        return null;
                    } else {
                        snapTop = true;
                        return layoutManager.findViewByPosition(firstChild + 1);
                    }
                }
            }
        }
        return super.findSnapView(layoutManager);
    }

    // Return a vertical orientation layout
    private OrientationHelper getVerticalHelper(RecyclerView.LayoutManager layoutManager) {
        if (mVerticalHelper == null) {
            mVerticalHelper = OrientationHelper.createVerticalHelper(layoutManager);
        }
        return mVerticalHelper;
    }

    // Return a horizontal orientation layout
    private OrientationHelper getHorizontalHelper(RecyclerView.LayoutManager layoutManager) {
        if (mHorizontalHelper == null) {
            mHorizontalHelper = OrientationHelper.createHorizontalHelper(layoutManager);
        }
        return mHorizontalHelper;
    }
}

