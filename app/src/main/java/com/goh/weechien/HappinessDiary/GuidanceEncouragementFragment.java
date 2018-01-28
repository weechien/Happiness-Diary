package com.goh.weechien.HappinessDiary;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.PointF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.yayandroid.parallaxrecyclerview.ParallaxRecyclerView;
import com.yayandroid.parallaxrecyclerview.ParallaxViewHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

import static android.content.Context.CLIPBOARD_SERVICE;

public class GuidanceEncouragementFragment extends Fragment implements ViewTreeObserver.OnGlobalLayoutListener {
    public ParallaxRecyclerView recyclerView_Encouragement;
    RecyclerView.LayoutManager layoutManager;
    public View fragView_Encouragement;
    String[] myContent, myDate;
    String mySource;
    List<PopupMenu> popupList;
    HashMap<String, RecyclerView.ViewHolder> hashMap;
    ArrangeDays arrangeDays;
    RecyclerView.Adapter mAdapter;
    Typeface typefaceMountain, typefaceKaiTi, typefaceRoboto;
    FirebaseAuth mAuth;
    FirebaseDatabase mData;
    long searchResult;
    int timeToScroll, offsetPos;
    final int posToScroll = 7;
    int expandedPos = -1; // Show which position is currently expanded
    boolean rotationChanged, colorEnabled;
    public static final String GUIDANCE_CARD_COLOR = "pref_cardColor";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final float[] speedPerPix = new float[1];

        mAuth = FirebaseAuth.getInstance(); // Get an instance of firebase authentication
        mData = FirebaseDatabase.getInstance(); // Get an instance of firebase database

        // Get the current preference of the card colors
        SharedPreferences cardColorPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        colorEnabled = cardColorPref.getBoolean(GUIDANCE_CARD_COLOR, true);

        // Inflate the fragView
        fragView_Encouragement = inflater.inflate(R.layout.frag_recyclerview, container, false);

        // Set the custom fonts for the date, content and source
        typefaceMountain = Typeface.createFromAsset(getActivity()
                .getAssets(), "fonts/beyond_the_mountains.otf");
        typefaceKaiTi = Typeface.createFromAsset(getActivity()
                .getAssets(), "fonts/KaiTi.ttf");
        typefaceRoboto = Typeface.createFromAsset(getActivity()
                .getAssets(), "fonts/Roboto-Regular.ttf");

        // Add the string array
        // The string arrays includes 29th Feb
        myDate = getResources().getStringArray(R.array.daily_encouragement_and_gosho_date);
        myContent = getResources().getStringArray(R.array.daily_encouragement_content);
        mySource = getString(R.string.daisaku_ikeda);

        // Use this class to adjust the string arrays by calculating the correct days (365/366)
        arrangeDays = new ArrangeDays();
        arrangeDays.init(myDate, myContent, mySource);

        // Add the recycler fragView and fix its size
        recyclerView_Encouragement = fragView_Encouragement.findViewById(R.id.frag_recycler);
        recyclerView_Encouragement.setHasFixedSize(true);
        recyclerView_Encouragement.getViewTreeObserver().addOnGlobalLayoutListener(this);

        // Add the layout manager
        layoutManager = new LinearLayoutManager(getContext()) {
            @Override
            // Use this method to smooth scroll to the target position
            // The smooth scroll will run when the search result activity ends and return a result
            public void smoothScrollToPosition(final RecyclerView recyclerView, RecyclerView.State state, int position) {
                LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getActivity().getApplicationContext()) {
                    @Override
                    // Calculate the distance to the position
                    public PointF computeScrollVectorForPosition(int targetPosition) {
                        return ((LinearLayoutManager) layoutManager).computeScrollVectorForPosition(targetPosition);
                    }

                    @Override
                    // Return the amount of time in millisecond it takes to scroll 1 pixel.
                    protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                        speedPerPix[0] = 50f / displayMetrics.densityDpi;
                        return speedPerPix[0];
                    }

                    @Override
                    // Calculate the time needed to scroll
                    // dx is the distance in pixels to scroll
                    protected int calculateTimeForScrolling(int dx) {
                        // Manually calculate the total time to scroll
                        // This is used as a timer to stop touch event during smooth scroll
                        timeToScroll = (int) (speedPerPix[0] * dx);
                        // When the smooth scroll is running, use a transparent view to overlap
                        // the recycler view to prevent the user from interrupting the smooth scroll
                        final TextView dummyView = fragView_Encouragement.findViewById(R.id.dummyView);
                        dummyView.setVisibility(View.VISIBLE);
                        // Ignore any touch event by telling android the touch has been handled
                        // by returning true without actually doing anything
                        dummyView.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                return true;
                            }
                        });
                        // Enable the recycle view to receive touch event again  by hiding
                        // the transparent view after scrolling is complete
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                dummyView.setVisibility(View.GONE);
                            }
                        };
                        Handler handler = new Handler();
                        handler.postDelayed(r, timeToScroll);
                        return super.calculateTimeForScrolling(dx);
                    }
                };
                smoothScroller.setTargetPosition(position); // Set the position to scroll to
                startSmoothScroll(smoothScroller); // Start the smooth scroll
            }
        };
        recyclerView_Encouragement.setLayoutManager(layoutManager);

        // Add the adapter
        mAdapter = new RecyclerViewAdapter();
        recyclerView_Encouragement.setAdapter(mAdapter);
        return fragView_Encouragement;
    }

    // Scroll animation serves to cover the matrix adjustment done to the image view which is
    // visible to the user. The gosho guidance does not need this because it is still not visible.
    public void entranceScrollAnim() { // Scroll to today's guidance when the app starts
        // Get the day of the year to scroll to that position
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

        // Scroll to the date position
        // Set the position to the day of the year
        ((Guidance) getActivity()).setSearchResult(dayOfYear - 1);
        scrollToCardEncouragement(); // Scroll to the position
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh the cards whenever the color preference in the settings has changed
        SharedPreferences cardColorPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (colorEnabled != cardColorPref.getBoolean(GUIDANCE_CARD_COLOR, true)) {
            colorEnabled = cardColorPref.getBoolean(GUIDANCE_CARD_COLOR, true);
            recyclerView_Encouragement.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    // Called when the activity has been created
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        entranceScrollAnim(); // Scroll to today's guidance when the app starts

        // Add the custom snap helper and attach it to the recycler view
        GuidanceSnapHelper snapHelper = new GuidanceSnapHelper(getActivity(),
                getActivity().getClass().getSimpleName());
        snapHelper.attachToRecyclerView(recyclerView_Encouragement);
        // Enable the recycler view to over scroll
        OverScrollDecoratorHelper.setUpOverScroll(recyclerView_Encouragement, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
    }

    // Run the scroll animation to the specified card
    public void scrollToCardEncouragement() {
        // This method is forced to run in the UI thread because the Guidance activity
        // will call this method in a background thread
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                searchResult = ((Guidance) getActivity()).getSearchResult();
                // Get the selected tab
                // The tab position is required because whenever the search result activity ends and the
                // Guidance activity resumes, the onResume method of all the fragments will be called
                TabLayout tabLayout = getActivity().findViewById(R.id.guidance_tablayout);
                int tabPosition = tabLayout.getSelectedTabPosition();
                // Ensure that there is a searchResult
                if (searchResult != -1 && tabPosition == 0) {
                    ((Guidance) getActivity()).setSearchResult(-1); // Reset the search result
                    // Get the visible view on screen
                    int visibleChild = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                    // Find the distance between the visible view and the target position
                    offsetPos = (int) (searchResult - visibleChild);
                    // If scrolling distance is greater than posToScroll, then decide whether to scroll
                    // up or down then immediately move to a position with a fixed distance away from
                    // the target position without animation, then only smooth scroll to the target position
                    if (offsetPos > 0 && offsetPos >= posToScroll) {
                        ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(
                                (int) searchResult - posToScroll, 0);
                        recyclerView_Encouragement.smoothScrollToPosition((int) searchResult);

                    } else if (offsetPos < 0 && (offsetPos * -1) >= posToScroll) {
                        ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(
                                (int) searchResult + posToScroll, 0);
                        recyclerView_Encouragement.smoothScrollToPosition((int) searchResult);

                    } else {
                        ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset((int) searchResult, 0);
                    }
                }
            }
        });
    }

    @Override
    // Run this when the recycler view is ready to be displayed
    // Might be called several times
    public void onGlobalLayout() {
        // Make sure the images are positioned correctly
        if (!getViewHolder().isEmpty()) {
            for (HashMap.Entry<String, RecyclerView.ViewHolder> hash : getViewHolder().entrySet()) {
                // Notify ParallaxImageView that it will be displayed, so it will re-center itself
                ((ParallaxViewHolder) hash.getValue()).getBackgroundImage().doTranslate();
            }
        }

        // Refresh the recycler view when the device rotates
        if (rotationChanged) {
            rotationChanged = false;

            // Get the first visible position and scroll to there
            int visibleChild = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
            ((Guidance) getActivity()).setSearchResult(visibleChild);
            scrollToCardEncouragement(); // Scroll to the position
            recyclerView_Encouragement.getAdapter().notifyDataSetChanged();
        }
    }

    // Refresh the recycler view
    void refreshRecyclerView() { // Refresh the data in the recycler view
        mAdapter.notifyDataSetChanged();
    }

    // Add popup menus into an array
    void setPopupMenu(PopupMenu popupMenu) {
        if (popupList == null) {
            popupList = new ArrayList<>();
        }
        popupList.add(popupMenu);
    }

    // Get popup menus from an array
    List<PopupMenu> getPopupMenu() {
        return popupList;
    }

    // Add view holders into an array
    void setViewHolder(String key, RecyclerView.ViewHolder holder) {
        if (hashMap == null) {
            hashMap = new HashMap<>();
        }
        hashMap.put(key, holder);
    }

    // Get view holders from an array
    HashMap<String, RecyclerView.ViewHolder> getViewHolder() {
        return hashMap;
    }

    // Remove view holders from an array
    void removeViewHolder(String key) {
        hashMap.remove(key);
    }

    // Change the expandedPos variable to -1 to make sure no cards are expanded
    void resetExpandedPos() {
        expandedPos = -1;
    }

    @Override
    // Called when the device rotates
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        rotationChanged = true; // Rotation has changed
        resetExpandedPos(); // Reset all expanded card views to its original size
        recyclerView_Encouragement.getAdapter().notifyDataSetChanged();
    }

    // Recycler View's Adapter to manage the content
    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>
            implements View.OnLayoutChangeListener {
        ViewHolder holder;
        int posClicked = -1; // Get the position of the card clicked
        int clickedTVHeight = -1; // Height of the text view clicked
        int clickedTextHeight = -1; // Height of the text clicked


        class ViewHolder extends ParallaxViewHolder {
            ConstraintLayout view;

            ViewHolder(ConstraintLayout v) {
                super(v);
                view = v; // Used internally to bind the view
            }

            @Override
            // Get the ID of the card image view
            public int getParallaxImageId() {
                return R.id.image_encouragement;
            }

            @Override
            // Setup the values for the parallax effect
            public int[] requireValuesForTranslate() {
                ImageView image = itemView.findViewById(R.id.image_encouragement);

                if (itemView.getParent() == null) {
                    // Not added to parent yet!
                    return null;
                } else {
                    int[] itemPosition = new int[2];
                    // Get the location (x,y) of the image view
                    image.getLocationOnScreen(itemPosition);

                    int[] recyclerPosition = new int[2];
                    // Get the location (x,y) of the recycler view
                    ((RecyclerView) itemView.getParent()).getLocationOnScreen(recyclerPosition);

                    return new int[]{image.getMeasuredHeight(), itemPosition[1],
                            ((RecyclerView) itemView.getParent()).getMeasuredHeight(), recyclerPosition[1]};
                }
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // Create a new view
            final ConstraintLayout v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.frag_encouragement_card, parent, false);

            // Add the card menu and set up the popup menu
            final FrameLayout cardMenu = v.findViewById(R.id.dropdown_encouragement);
            final PopupMenu popup = new PopupMenu(getContext(), cardMenu);

            // Refresh the user
            if (mAuth.getCurrentUser() != null) {
                mAuth.getCurrentUser().reload();
            }

            // Proceed if user is signed in
            if (mAuth.getCurrentUser() != null) {

                popup.getMenuInflater().inflate(R.menu.guidance_card_signin, popup.getMenu()); // Inflate menu
                setPopupMenu(popup); // Pass the popup to an array list to prepare for language change

                // Set the text of the popup menu when it's first created
                Context context = LocaleHelper.setLocale(getActivity(), LocaleHelper.getLanguage(getActivity()));
                Resources resources = context.getResources();
                popup.getMenu().findItem(R.id.guidance_share).setTitle(resources.getString(R.string.guidance_share));
                popup.getMenu().findItem(R.id.guidance_copy).setTitle(resources.getString(R.string.guidance_copy));
                popup.getMenu().findItem(R.id.guidance_bookmark).setTitle(resources.getString(R.string.guidance_bookmark));

                // Set up a listener to the card menu
                onCardMenuClicked(cardMenu, popup);

            } else { // Proceed if user is not signed in
                popup.getMenuInflater().inflate(R.menu.guidance_card_guest, popup.getMenu()); // Inflate menu
                setPopupMenu(popup); // Pass the popup to an array list to prepare for language change

                // Set the text of the popup menu when it's first created
                Context context = LocaleHelper.setLocale(getActivity(), LocaleHelper.getLanguage(getActivity()));
                Resources resources = context.getResources();
                popup.getMenu().findItem(R.id.guidance_share).setTitle(resources.getString(R.string.guidance_share));
                popup.getMenu().findItem(R.id.guidance_copy).setTitle(resources.getString(R.string.guidance_copy));

                // Set up a listener to the card menu
                onCardMenuClicked(cardMenu, popup);
            }

            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            setViewHolder(String.valueOf(holder.getItemId()), holder); // Add the holders into a hash map

            this.holder = holder;
            holder.getBackgroundImage().reuse();
            // Check if the card is expanded
            final boolean isExpanded = holder.getAdapterPosition() == expandedPos;
            int extraPadding;

            // Set the background color of the cards depending on the day
            setCardBackgroundColor(holder, position);

            // Setup a listener to retry loading via Glide
            final ImageButton retryImage = holder.view.findViewById(R.id.retry_image_encouragement);
            retryImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadGlide(holder, holder.getAdapterPosition());
                }
            });

            // Load the image via Glide
            loadGlide(holder, position);

            // Set the custom font for the date text view
            TextView date = holder.view.findViewById(R.id.textview_encouragement_date);
            date.setText(arrangeDays.getDateList().get(position));  // Add the date into the text view

            // Content text view
            final TextView content = holder.view.findViewById(R.id.textview_encouragement_content);
            final String contentText = arrangeDays.getContentList().get(position);
            content.setText(contentText); // Add the content into the text view
            content.addOnLayoutChangeListener(this); // Listen to layout changes

            // Source text view
            TextView source = holder.view.findViewById(R.id.textview_encouragement_source);
            source.setText(arrangeDays.getSourceList());  // Add the source into the text view

            // Change the font type and size depending on the language
            if (LocaleHelper.getLanguage(getActivity()).equals("en")) {
                date.setTypeface(typefaceMountain);
                date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                content.setTypeface(typefaceRoboto);
                content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                source.setTypeface(typefaceRoboto);
                source.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            } else {
                date.setTypeface(typefaceKaiTi);
                date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
                content.setTypeface(typefaceKaiTi);
                content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                source.setTypeface(typefaceKaiTi);
                source.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            }

            // Get the device's height
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int deviceHeight = displayMetrics.heightPixels;

            // Set the height of the guideline which holds the constraint to the image view
            // The guideline's height is set based on a certain % of the device's height
            Guideline guideline = holder.view.findViewById(R.id.guideline_encouragement);
            ConstraintLayout.LayoutParams guideParam = (ConstraintLayout.LayoutParams) guideline.getLayoutParams();
            guideParam.guideBegin = (int) (deviceHeight * 0.25);
            guideline.setLayoutParams(guideParam);

            // Extra padding is needed if the text view's height is 0, which excludes the padding
            extraPadding = content.getHeight() == 0 ?
                    content.getPaddingBottom() + content.getPaddingTop() : 0;

            // Set the layout params of the card view to wrap content
            RecyclerView.LayoutParams wrap = new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    recyclerView_Encouragement.getHeight() + getTextHeight(content, contentText)
                            - content.getHeight() + content.getPaddingBottom()
                            + content.getPaddingTop() + extraPadding);

            // Set the layout params of the card view to match parent
            RecyclerView.LayoutParams match = new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT);

            // Expand frame and expand button
            FrameLayout expandFrame = holder.view.findViewById(R.id.expand_encouragement);
            ImageView expandIcon = holder.view.findViewById(R.id.expand_icon_encouragement);

            // Check if the card was clicked
            if (posClicked == position) {
                posClicked = -1; // Reset the click position

                // Animate the card view when the layout changes
                ValueAnimator anim = isExpanded ? ValueAnimator.ofInt(
                        // Change from match parent to wrap content if isExpanded is true
                        recyclerView_Encouragement.getHeight(), recyclerView_Encouragement.getHeight()
                                + clickedTextHeight - clickedTVHeight
                                + content.getPaddingBottom() + content.getPaddingTop() + extraPadding)
                        // Change from wrap content to match parent if isExpanded is false
                        : ValueAnimator.ofInt(holder.view.getHeight(), recyclerView_Encouragement.getHeight());

                clickedTVHeight = -1; // Reset the text view height
                clickedTextHeight = -1; // Reset the text height

                // Update the layout by setting the height of the card gradually
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int val = (int) animation.getAnimatedValue();
                        RecyclerView.LayoutParams layout =
                                (RecyclerView.LayoutParams) holder.view.getLayoutParams();
                        layout.height = val;
                        holder.view.setLayoutParams(layout);
                    }
                });
                // Listen when the animation ends and apply the shader as onLayoutChange might not
                // be always called if the card view is not fully visible
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!isExpanded && content.getPaint().getShader() == null) {
                            Shader shader = new LinearGradient(0, 0, 0, content.getHeight(),
                                    new int[]{Color.BLACK, Color.TRANSPARENT},
                                    new float[]{0, 1}, Shader.TileMode.CLAMP);
                            content.getPaint().setShader(shader);
                        }
                        super.onAnimationEnd(animation);
                    }
                });
                anim.setDuration(200);
                anim.start();
            } else {
                // If the card is not clicked then set the layout without animation
                holder.view.setLayoutParams(isExpanded ? wrap : match);
            }
            // Set the expand button icon depending on whether the text view is expanded or not
            expandIcon.setBackgroundResource(isExpanded ? R.drawable.ic_drop_down_inverse_layer
                    : R.drawable.ic_drop_down_grey);

            // Expand/Contract the text view on click
            expandFrame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    posClicked = holder.getAdapterPosition(); // Boolean to animate the card on click
                    expandedPos = isExpanded ? -1 : holder.getAdapterPosition();

                    // Get the height of the text view clicked
                    clickedTVHeight = holder.view.findViewById(R.id.textview_encouragement_content).getHeight();
                    clickedTextHeight = getTextHeight(content, contentText);

                    // Set the shader on the text view depending if it's expanded or not
                    content.getPaint().setShader(isExpanded ? new LinearGradient(0, 0, 0, content.getHeight(),
                            new int[]{Color.BLACK, Color.TRANSPARENT},
                            new float[]{0, 1}, Shader.TileMode.CLAMP) : null);
                    notifyDataSetChanged(); // Refresh the layout and reset any expanded cards
                }
            });
        }

        @Override
        // Clean up items
        public void onViewRecycled(ViewHolder holder) {
            super.onViewRecycled(holder);
            removeViewHolder(String.valueOf(holder.getItemId())); // Remove the view holder

            TextView content = holder.view.findViewById(R.id.textview_encouragement_content);
            content.removeOnLayoutChangeListener(this); // Remove the layout change listener

            FrameLayout expandFrame = holder.view.findViewById(R.id.expand_encouragement);
            expandFrame.setOnClickListener(null); // Remove the click listener
        }

        @Override
        // Called whenever the layout of the text view changes
        public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                   int oldLeft, int oldTop, int oldRight, int oldBottom) {
            // Change the visibility of the expand frame after the card's layout has been drawn
            adjustExpandVisibility((TextView) v);
        }

        // Adjust the visibility of the expand frame depending on the height of the text view and its string
        private void adjustExpandVisibility(TextView v) {
            // Get the height of the text view and the string of the text view
            v.getPaint().setShader(null); // Reset the shader

            int vHeight = v.getHeight() - v.getPaddingTop() - v.getPaddingBottom();
            vHeight = vHeight < 0 ? 0 : vHeight;

            String vText = v.getText().toString();

            // Height of the text
            int textHeight = getTextHeight(v, vText);

            // Get the height of the card which contains the view
            int cardHeight = ((ConstraintLayout) v
                    .getParent().getParent().getParent().getParent().getParent()).getHeight();

            FrameLayout expandFrame = ((ConstraintLayout) v
                    .getParent().getParent()).findViewById(R.id.expand_encouragement);

            // Hide the expand frame if the string's height can fit into the text view's height
            // Only take into account cards that have not been expanded by making sure the
            // height of the card matches the recycler view's height
            if (textHeight <= vHeight && cardHeight
                    == recyclerView_Encouragement.getHeight()) {
                expandFrame.setVisibility(View.INVISIBLE); // Hide the expand frame
            } else {
                expandFrame.setVisibility(View.VISIBLE); // Show the expand frame

                // Fade the bottom part of the text only when the card view is not expanded
                if (cardHeight == recyclerView_Encouragement.getHeight()) {
                    Shader shader = new LinearGradient(0, 0, 0, v.getHeight(),
                            new int[]{Color.BLACK, Color.TRANSPARENT},
                            new float[]{0, 1}, Shader.TileMode.CLAMP);
                    v.getPaint().setShader(shader);
                }
            }
        }

        // Method to measure the height of the string in a text view
        private int getTextHeight(TextView textView, String text) {
            TextPaint paint = textView.getPaint(); // Get the text format of the text view

            // Get the width of the text view and return 0 if the width is negative after
            // taking into account of the padding
            int tvWidth = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
            tvWidth = tvWidth < 0 ? 0 : tvWidth;

            // Draw the text on a canvas to get the height of the text after taking into account
            // the width of the text view
            Layout.Alignment alignment = Layout.Alignment.ALIGN_NORMAL;
            StaticLayout layout = new StaticLayout(text, paint, tvWidth, alignment,
                    textView.getLineSpacingMultiplier(), textView.getLineSpacingExtra(), true);

            return layout.getHeight();
        }

        // Set the background color of the cards depending on the day
        private void setCardBackgroundColor(ViewHolder holder, int position) {
            boolean isColorEnabled;

            // Check the shared preference to set the color of the cards
            SharedPreferences cardColorPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            isColorEnabled = cardColorPref.getBoolean(GUIDANCE_CARD_COLOR, true);

            CardView cardView = holder.view.findViewById(R.id.cardview_encouragement);

            Calendar calendar = Calendar.getInstance();
            int calendarMaxDays = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
            int leap;

            // Determine whether to add an extra day for a leap year
            if (calendarMaxDays == 365 && position >= 59) {
                leap = 1; // Not leap year
            } else {
                leap = 0; // Leap year
            }

            if (0 <= position && position <= 30) { // Jan
                cardView.setCardBackgroundColor(isColorEnabled ?
                        ContextCompat.getColor(getActivity(), R.color.Month1) : Color.WHITE);
            } else if (31 <= position && position <= (59 - leap)) { // Feb
                cardView.setCardBackgroundColor(isColorEnabled ?
                        ContextCompat.getColor(getActivity(), R.color.Month2) : Color.WHITE);
            } else if ((60 - leap) <= (position) && (position) <= (90 - leap)) { // Mar
                cardView.setCardBackgroundColor(isColorEnabled ?
                        ContextCompat.getColor(getActivity(), R.color.Month3) : Color.WHITE);
            } else if ((91 - leap) <= (position) && (position) <= (120 - leap)) { // Apr
                cardView.setCardBackgroundColor(isColorEnabled ?
                        ContextCompat.getColor(getActivity(), R.color.Month4) : Color.WHITE);
            } else if ((121 - leap) <= (position) && (position) <= (151 - leap)) { // May
                cardView.setCardBackgroundColor(isColorEnabled ?
                        ContextCompat.getColor(getActivity(), R.color.Month5) : Color.WHITE);
            } else if ((152 - leap) <= (position) && (position) <= (181 - leap)) { // Jun
                cardView.setCardBackgroundColor(isColorEnabled ?
                        ContextCompat.getColor(getActivity(), R.color.Month6) : Color.WHITE);
            } else if ((182 - leap) <= (position) && (position) <= (212 - leap)) { // Jul
                cardView.setCardBackgroundColor(isColorEnabled ?
                        ContextCompat.getColor(getActivity(), R.color.Month7) : Color.WHITE);
            } else if ((213 - leap) <= (position) && (position) <= (243 - leap)) { // Aug
                cardView.setCardBackgroundColor(isColorEnabled ?
                        ContextCompat.getColor(getActivity(), R.color.Month8) : Color.WHITE);
            } else if ((244 - leap) <= (position) && (position) <= (273 - leap)) { // Sep
                cardView.setCardBackgroundColor(isColorEnabled ?
                        ContextCompat.getColor(getActivity(), R.color.Month9) : Color.WHITE);
            } else if ((274 - leap) <= (position) && (position) <= (304 - leap)) { // Oct
                cardView.setCardBackgroundColor(isColorEnabled ?
                        ContextCompat.getColor(getActivity(), R.color.Month10) : Color.WHITE);
            } else if ((305 - leap) <= (position) && (position) <= (334 - leap)) { // Nov
                cardView.setCardBackgroundColor(isColorEnabled ?
                        ContextCompat.getColor(getActivity(), R.color.Month11) : Color.WHITE);
            } else if ((335 - leap) <= (position) && (position) <= (365 - leap)) { // Dec
                cardView.setCardBackgroundColor(isColorEnabled ?
                        ContextCompat.getColor(getActivity(), R.color.Month12) : Color.WHITE);
            }
        }

        // Load the image via Glide
        void loadGlide(final ViewHolder holder, int position) {
            // Get the firebase storage reference
            StorageReference pathRef =
                    Guidance.getFirebaseStorageRef(
                            getActivity(), position, Guidance.DGuidance.DE, false);

            // Card image view
            final ImageView cardImage = holder.view.findViewById(R.id.image_encouragement);

            // Progress bar running while image loads
            final ProgressBar circularLoader = holder.view.findViewById(R.id.image_loading_encouragement);
            circularLoader.bringToFront(); // Bring the progress bar to the front
            circularLoader.setVisibility(View.VISIBLE); // Reveal the progress bar

            // Only show the error image when the image is unable to load
            final ImageView errorImage = holder.view.findViewById(R.id.error_image_encouragement);
            errorImage.setVisibility(View.INVISIBLE);

            // Only show the retry image when the image is unable to load
            final ImageButton retryImage = holder.view.findViewById(R.id.retry_image_encouragement);
            retryImage.setVisibility(View.INVISIBLE);

            // Use firebase integration with glide to load the image
            Glide.with(getActivity()).using(new FirebaseImageLoader()).load(pathRef)
                    .placeholder(new ColorDrawable(0xFF666666)).crossFade()
                    // New listener to stop unnecessary animation when the image finished loading
                    .listener(new RequestListener<StorageReference, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, StorageReference model,
                                                   Target<GlideDrawable> target, boolean isFirstResource) {
                            circularLoader.setVisibility(View.GONE); // Hide the loading progress bar
                            errorImage.setVisibility(View.VISIBLE); // Show the error image
                            errorImage.bringToFront(); // Bring the error image to the front
                            retryImage.setVisibility(View.VISIBLE); // Show the retry image
                            retryImage.bringToFront(); // Bring the retry image to the front
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, StorageReference model,
                                                       Target<GlideDrawable> target,
                                                       boolean isFromMemoryCache, boolean isFirstResource) {
                            circularLoader.setVisibility(View.GONE); // Hide the loading progress bar
                            holder.getBackgroundImage().reuse();
                            return false;
                        }
                    }).into(cardImage);
        }

        @Override
        public int getItemCount() {
            return arrangeDays.ListLength;
        }

        // Set up a listener to the card menu
        private void onCardMenuClicked(final FrameLayout cardMenu, final PopupMenu popup) {
            cardMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    ArrayList<String> list;
                    String lang;

                    // Get the array list of bookmarks saved in firebase database
                    // The array reference depends on the language context
                    if (LocaleHelper.getLanguage(getActivity()).equals("en")) {
                        list = ((Guidance) getActivity()).bookmarkListDEeng;
                        lang = "Eng";
                    } else {
                        list = ((Guidance) getActivity()).bookmarkListDEchi;
                        lang = "Chi";
                    }

                    // Get the visible view on screen
                    int visibleChild = ((LinearLayoutManager) layoutManager)
                            .findFirstVisibleItemPosition();

                    // Get the language context
                    Context context = LocaleHelper.setLocale(getActivity(),
                            LocaleHelper.getLanguage(getActivity()));
                    Resources resources = context.getResources();

                    MenuItem itemBookmark = popup.getMenu().findItem(R.id.guidance_bookmark);

                    // Proceed if the user is signed in and the bookmark menu is present
                    if (itemBookmark != null) {
                        // Proceed if the list is empty
                        if (list.isEmpty()) {
                            itemBookmark.setTitle(resources.getString(R.string.guidance_bookmark));
                        } else {
                            // Loop the array if it is not empty
                            for (int i = 0; i < list.size(); i++) {
                                String bookmarkItem = list.get(i);

                                // Proceed if the item is under daily encouragement and
                                // the item has the same number as the card position
                                if (bookmarkItem.substring(0, 6).equals("DE " + lang) &&
                                        bookmarkItem.substring(7, bookmarkItem.length()).equals(
                                                arrangeDays.getDateList().get(visibleChild).trim())) {
                                    // The bookmark is stored in the database
                                    // Change the text to remove the bookmark from the database
                                    itemBookmark.setTitle(resources.getString(R.string.guidance_unbookmark));
                                    break;

                                    // The item doesn't exist in the database
                                    // Change the text to add the bookmark to the database
                                } else if (i == list.size() - 1) {
                                    itemBookmark.setTitle(resources.getString(R.string.guidance_bookmark));
                                }
                            }
                        }
                    }

                    // Set a click listener on the menu items
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int id = item.getItemId();

                            if (id == R.id.guidance_share_text) { // When the share menu is clicked
                                shareEncouragement(); // Share the daily encouragement
                                return true;
                            } else if (id == R.id.guidance_share_image) {
                                // Disable the menu items
                                popup.getMenu().findItem(R.id.guidance_share_image)
                                        .setEnabled(false);
                                popup.getMenu().findItem(R.id.guidance_share_text)
                                        .setEnabled(false);

                                shareImage(item, popup); // Share the image of the guidance
                                // Keep the popup menu open
                                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                                item.setActionView(new View(getActivity()));
                                return false;

                            } else if (id == R.id.guidance_copy) { // When the copy menu is clicked
                                // Copy the selected daily encouragement into the clipboard
                                copyEncouragement(v);
                                return true;
                                // When the bookmark menu is clicked
                            } else if (id == R.id.guidance_bookmark) {
                                // Disable the menu items
                                popup.getMenu().findItem(R.id.guidance_share)
                                        .setEnabled(false);
                                popup.getMenu().findItem(R.id.guidance_copy)
                                        .setEnabled(false);
                                popup.getMenu().findItem(R.id.guidance_bookmark)
                                        .setEnabled(false);

                                updateBookmark(v, item, item.getTitle().toString(), popup);
                                // Keep the popup menu open
                                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                                item.setActionView(new View(getActivity()));
                                return false;
                            }
                            return false;
                        }
                    });
                    popup.show();
                }
            });
        }

        // Save the bookmark of the guidance card
        private void updateBookmark(final View v, MenuItem item, String title, final PopupMenu popup) {
            ArrayList<String> list;
            String lang;

            // Get a reference to the firebase database
            DatabaseReference mDataRef = mData.getReference(); // Base reference
            DatabaseReference mDataChild; // Child reference

            // Get the array list of bookmarks saved in firebase database
            // The array reference depends on the language context
            if (LocaleHelper.getLanguage(getActivity()).equals("en")) {
                list = ((Guidance) getActivity()).bookmarkListDEeng;
                lang = "Eng";
            } else {
                list = ((Guidance) getActivity()).bookmarkListDEchi;
                lang = "Chi";
            }

            // Refresh the user
            if (mAuth.getCurrentUser() != null) {
                mAuth.getCurrentUser().reload();
            }

            if (mAuth.getCurrentUser() != null) { // Proceed if user is signed in
                // Get the language context
                Context context = LocaleHelper.setLocale(getActivity()
                        , LocaleHelper.getLanguage(getActivity()));
                final Resources resources = context.getResources();

                // Get the visible view on screen
                final int visibleChild = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                // Firebase database child
                mDataChild = mDataRef.child("users-bookmarks").child(mAuth.getCurrentUser().getUid());

                // Proceed to add the bookmark if the database has no record of this bookmark
                if (title.equals(resources.getString(R.string.guidance_bookmark))) {
                    // Change the text of the bookmark menu
                    item.setTitle(resources.getString(R.string.please_wait));

                    // Update the bookmark array list
                    list.add("DE " + lang + " " + arrangeDays.getDateList().get(visibleChild).trim());

                    // Update the firebase database
                    mDataChild.child("DE " + lang + " "
                            + arrangeDays.getDateList().get(visibleChild).trim())
                            .setValue(true)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                // Run when the update is complete
                                public void onComplete(@NonNull Task<Void> task) {
                                    popup.getMenu().close(); // Close the popup menu
                                    // Re-enable the menu items
                                    popup.getMenu().findItem(R.id.guidance_share)
                                            .setEnabled(true);
                                    popup.getMenu().findItem(R.id.guidance_copy)
                                            .setEnabled(true);
                                    popup.getMenu().findItem(R.id.guidance_bookmark)
                                            .setEnabled(true);

                                    if (task.isSuccessful()) {
                                        //Get the date
                                        String date = arrangeDays.getDateList().get(visibleChild);
                                        // Trim the date to remove the spacing at the beginning
                                        date = date.trim();

                                        // Show a message informing the guidance has been bookmarked
                                        Snackbar.make(v, resources.getString(R.string.daily_encouragement)
                                                + " (" + date + ") "
                                                + resources.getString(R.string.bookmarked)
                                                + "!", Snackbar.LENGTH_LONG).show();

                                    } else { // Show an error if bookmark is unsuccessful
                                        Toast.makeText(getActivity(),
                                                getString(R.string.unknown_error),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });


                    // Remove the bookmark from the database if it already exists
                } else if (title.equals(resources.getString(R.string.guidance_unbookmark))) {
                    // Change the text of the bookmark menu
                    item.setTitle(resources.getString(R.string.please_wait));

                    // Remove from the array list
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).equals("DE " + lang + " "
                                + arrangeDays.getDateList().get(visibleChild).trim())) {
                            list.remove(i);
                            break;
                        }
                    }

                    // Remove from firebase database
                    mDataChild.child("DE " + lang + " "
                            + arrangeDays.getDateList().get(visibleChild).trim())
                            .removeValue().addOnCompleteListener(
                            new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    popup.getMenu().close(); // Close the popup menu
                                    // Re-enable the menu items
                                    popup.getMenu().findItem(R.id.guidance_share)
                                            .setEnabled(true);
                                    popup.getMenu().findItem(R.id.guidance_copy)
                                            .setEnabled(true);
                                    popup.getMenu().findItem(R.id.guidance_bookmark)
                                            .setEnabled(true);

                                    if (task.isSuccessful()) {
                                        //Get the date
                                        String date = arrangeDays.getDateList().get(visibleChild);
                                        // Trim the date to remove the spacing at the beginning
                                        date = date.trim();

                                        // Show a message informing the guidance has been bookmarked
                                        Snackbar.make(v, resources.getString(R.string.daily_encouragement)
                                                + " (" + date + ") "
                                                + resources.getString(R.string.bookmark_removed)
                                                + "!", Snackbar.LENGTH_LONG).show();

                                    } else {
                                        // Show an error if bookmark is unsuccessful
                                        Toast.makeText(getActivity(),
                                                getString(R.string.unknown_error),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                    );
                }
            }
        }

        // Share the daily encouragement
        private void shareEncouragement() {
            String date, content, source;

            // Get the visible view on screen
            int visibleChild = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();

            Context context = LocaleHelper.setLocale(getActivity(), LocaleHelper.getLanguage(getActivity()));
            Resources resources = context.getResources();

            //Get the date, content and source
            date = arrangeDays.getDateList().get(visibleChild);
            date = date.trim(); // Trim the date to remove the spacing at the beginning
            content = arrangeDays.getContentList().get(visibleChild);
            source = arrangeDays.getSourceList();

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.daily_encouragement)
                    + " " + Guidance.getSunriseEmoji()
                    + "\n" + date + "\n\n" + content + "\n\n" + source + "\n\n"
                    + resources.getString(R.string.share_promotion1)
                    + new String(Character.toChars(0x1F54A))
                    + resources.getString(R.string.share_promotion2));

            startActivity(Intent.createChooser(shareIntent, resources.getString(R.string.share_with)));
        }

        // Share the image of the guidance
        private void shareImage(MenuItem item, final PopupMenu popup) {
            final String[] fileName = new String[1];

            // Language context
            Context context = LocaleHelper.setLocale(getActivity(), LocaleHelper.getLanguage(getActivity()));
            final Resources resources = context.getResources();

            // Set the text
            item.setTitle(resources.getString(R.string.please_wait));

            // Get the visible view on screen
            int position = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();

            // Get the firebase storage reference
            StorageReference pathRef = Guidance.getFirebaseStorageRef(
                    getActivity(), position, Guidance.DGuidance.DE, false);

            // Use firebase integration with glide to load the image
            Glide.with(getActivity()).using(new FirebaseImageLoader())
                    .load(pathRef).asBitmap().into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                    popup.getMenu().findItem(R.id.guidance_share).getSubMenu().close(); // Close the popup menu
                    // Re-enable the menu items
                    popup.getMenu().findItem(R.id.guidance_share_image)
                            .setEnabled(true)
                            .setTitle(resources.getString(R.string.guidance_share_image));
                    popup.getMenu().findItem(R.id.guidance_share_text)
                            .setEnabled(true);

                    // Save bitmap to cache directory
                    try {
                        File cachePath = new File(getActivity().getCacheDir(), "images");
                        cachePath.mkdirs(); // Make the directory

                        // Delete the old image
                        for (File f : cachePath.listFiles()) {
                            f.delete();
                        }

                        // Set the file name using a time stamp
                        SimpleDateFormat formatter =
                                new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
                        Date now = new Date();
                        fileName[0] = formatter.format(now) + ".png";

                        // Save the image with the time stamp name
                        FileOutputStream stream = new FileOutputStream(cachePath + "/" + fileName[0]);
                        resource.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        stream.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Get the image path
                    File imagePath = new File(getActivity().getCacheDir(), "images");
                    File newFile = new File(imagePath, fileName[0]);

                    // Get the Uri
                    Uri contentUri = FileProvider.getUriForFile(getActivity(),
                            "com.goh.weechien.HappinessDiary.fileprovider", newFile);

                    if (contentUri != null) {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        // Temp permission for receiving app to read this file
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        shareIntent.setDataAndType(
                                contentUri, getActivity().getContentResolver().getType(contentUri));
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        startActivity(Intent.createChooser(shareIntent,
                                resources.getString(R.string.share_with)));
                    }
                }
            });
        }

        // Copy the selected daily encouragement into the clipboard
        private void copyEncouragement(View v) {
            String date, content, source;

            // Get the visible view on screen
            int visibleChild = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();

            // Language context
            Context context = LocaleHelper.setLocale(getActivity(), LocaleHelper.getLanguage(getActivity()));
            Resources resources = context.getResources();

            //Get the date, content and source
            date = arrangeDays.getDateList().get(visibleChild);
            date = date.trim(); // Trim the date to remove the spacing at the beginning
            content = arrangeDays.getContentList().get(visibleChild);
            source = arrangeDays.getSourceList();

            // Copy the information into the clipboard
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(resources.getString(R.string.daily_encouragement)
                    + " " + date, resources.getString(R.string.daily_encouragement)
                    + " " + Guidance.getSunriseEmoji() + "\n"
                    + date + "\n\n" + content + "\n\n" + source + "\n\n"
                    + resources.getString(R.string.share_promotion1)
                    + new String(Character.toChars(0x1F54A))
                    + resources.getString(R.string.share_promotion2));
            clipboard.setPrimaryClip(clip);

            // Show a message informing the guidance has been copied
            Snackbar.make(v, resources.getString(R.string.daily_encouragement) + " (" + date + ") "
                    + resources.getString(R.string.copy) + "!", Snackbar.LENGTH_LONG).show();
        }
    }

    // Get the date, content and source in the string array after deciding if it's a leap year
    static class ArrangeDays {
        List<String> dateList, contentList;
        String sourceList;
        private int ListLength;

        // Get the date, content and source depending if it's a leap year and if the day is after 28th Feb
        void init(String[] dateList, String[] contentList, String sourceList) {
            Calendar calendar = Calendar.getInstance();
            int calendarMaxDays = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);

            this.dateList = new ArrayList<>();
            this.contentList = new ArrayList<>();
            this.sourceList = sourceList;

            // Determine the number of loops to iterate based on the number of days in a year
            if (calendarMaxDays == 365) {
                ListLength = dateList.length - 1;
            } else {
                ListLength = dateList.length;
            }

            // Loop and add the correct number of days into the list
            for (int i = 0; i < ListLength; i++) {
                if (calendarMaxDays == 365 && i >= 59) {
                    this.dateList.add(dateList[i + 1]);
                    this.contentList.add(contentList[i + 1]);
                } else {
                    this.dateList.add(dateList[i]);
                    this.contentList.add(contentList[i]);
                }
            }
        }

        List<String> getDateList() {
            return dateList;
        }

        List<String> getContentList() {
            return contentList;
        }

        String getSourceList() {
            return sourceList;
        }
    }
}
