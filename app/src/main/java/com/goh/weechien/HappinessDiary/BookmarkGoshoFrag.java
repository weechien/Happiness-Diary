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
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yayandroid.parallaxrecyclerview.ParallaxRecyclerView;
import com.yayandroid.parallaxrecyclerview.ParallaxViewHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

import static android.content.Context.CLIPBOARD_SERVICE;

public class BookmarkGoshoFrag extends Fragment implements ViewTreeObserver.OnGlobalLayoutListener {
    public ParallaxRecyclerView recyclerView_Gosho;
    RecyclerView.LayoutManager layoutManager;
    public View fragView_Gosho;
    String[] myContent, myDate, mySource;
    String firePath;
    HashMap<String, RecyclerView.ViewHolder> redrawHashMap;
    RecyclerView.Adapter mAdapter;
    StorageReference storageRef;
    Typeface typefaceMountain, typefaceKaiTi, typefaceRoboto;
    FirebaseAuth mAuth;
    FirebaseDatabase mData;
    ArrayList<String> bookmarkList;
    BookmarkDays bookmarkDays;
    int expandedPos = -1; // Show which position is currently expanded
    boolean colorEnabled;
    public static final String GUIDANCE_CARD_COLOR = "pref_cardColor";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance(); // Get an instance of firebase authentication
        mData = FirebaseDatabase.getInstance(); // Get an instance of firebase database

        // Get the current preference of the card colors
        SharedPreferences cardColorPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        colorEnabled = cardColorPref.getBoolean(GUIDANCE_CARD_COLOR, true);

        // Inflate the fragView
        fragView_Gosho = inflater.inflate(R.layout.frag_recyclerview_bookmark, container, false);

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
        myContent = getResources().getStringArray(R.array.daily_gosho_content);
        mySource = getResources().getStringArray(R.array.daily_gosho_source);

        // Trim the date
        for (int i = 0; i < myDate.length; i++) {
            myDate[i] = myDate[i].trim();
        }

        // Get the array list of bookmarks saved in firebase database
        // The array reference depends on the language context
        if (LocaleHelper.getLanguage(getActivity()).equals("en")) {
            bookmarkList = ((Bookmark) getActivity()).bookmarkListDGeng;
        } else {
            bookmarkList = ((Bookmark) getActivity()).bookmarkListDGchi;
        }

        if (bookmarkList.isEmpty()) {
            LinearLayout linearLayout = fragView_Gosho.findViewById(R.id.dummyView_bookmark);
            linearLayout.setVisibility(View.VISIBLE);
            linearLayout.setGravity(Gravity.CENTER);
            TextView tv = linearLayout.findViewById(R.id.dummyView_textView_bookmark);
            tv.setText(R.string.favourite_gosho);
        }

        // Use this class to adjust the string arrays by calculating the correct days (365/366)
        // Only list out the bookmarks
        bookmarkDays = new BookmarkDays();
        bookmarkDays.init(bookmarkList, myDate, myContent, mySource);
        sortBookmark(); // Sort the bookmarks

        // Add the recycler fragView and fix its size
        recyclerView_Gosho = fragView_Gosho.findViewById(R.id.frag_recycler_bookmark);
        recyclerView_Gosho.setHasFixedSize(true);
        recyclerView_Gosho.getViewTreeObserver().addOnGlobalLayoutListener(this);

        // Add the layout manager
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView_Gosho.setLayoutManager(layoutManager);

        // Create a storage reference from firebase
        storageRef = FirebaseStorage.getInstance().getReference();

        // Download the images based on the device's density
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        if (metrics.densityDpi <= DisplayMetrics.DENSITY_LOW) {
            firePath = "Daily Gosho/ldpi/";
        } else if (metrics.densityDpi > DisplayMetrics.DENSITY_LOW &&
                metrics.densityDpi <= DisplayMetrics.DENSITY_MEDIUM) {
            firePath = "Daily Gosho/mdpi/";
        } else if (metrics.densityDpi > DisplayMetrics.DENSITY_MEDIUM &&
                metrics.densityDpi <= DisplayMetrics.DENSITY_HIGH) {
            firePath = "Daily Gosho/hdpi/";
        } else if (metrics.densityDpi > DisplayMetrics.DENSITY_HIGH) {
            firePath = "Daily Gosho/xhdpi/";
        }
        // Add the adapter
        mAdapter = new RecyclerViewAdapter();
        recyclerView_Gosho.setAdapter(mAdapter);
        return fragView_Gosho;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh the cards whenever the color preference in the settings has changed
        SharedPreferences cardColorPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (colorEnabled != cardColorPref.getBoolean(GUIDANCE_CARD_COLOR, true)) {
            colorEnabled = cardColorPref.getBoolean(GUIDANCE_CARD_COLOR, true);
            recyclerView_Gosho.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    // Called when the activity has been created
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Add the custom snap helper and attach it to the recycler view
        GuidanceSnapHelper snapHelper = new GuidanceSnapHelper(getActivity(),
                getActivity().getClass().getSimpleName());
        snapHelper.attachToRecyclerView(recyclerView_Gosho);
        // Enable the recycler view to over scroll
        OverScrollDecoratorHelper.setUpOverScroll(recyclerView_Gosho,
                OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
    }

    @Override
    // Run this when the recycler view is ready to be displayed
    // Might be called several times
    public void onGlobalLayout() {
        // Make sure the images are positioned correctly
        if (getViewHolder() != null && !getViewHolder().isEmpty()) {
            for (HashMap.Entry<String, RecyclerView.ViewHolder> hash : getViewHolder().entrySet()) {
                // Notify ParallaxImageView that it will be displayed, so it will re-center itself
                ((ParallaxViewHolder) hash.getValue()).getBackgroundImage().doTranslate();
            }
        }
    }

    // Add view holders into an array
    void setViewHolder(String key, RecyclerView.ViewHolder holder) {
        if (redrawHashMap == null) {
            redrawHashMap = new HashMap<>();
        }
        redrawHashMap.put(key, holder);
    }

    // Get view holders from an array
    HashMap<String, RecyclerView.ViewHolder> getViewHolder() {
        return redrawHashMap;
    }

    // Remove view holders from an array
    void removeViewHolder(String key) {
        redrawHashMap.remove(key);
    }

    // Change the expandedPos variable to -1 to make sure no cards are expanded
    void resetExpandedPos() {
        expandedPos = -1;
    }

    @Override
    // Called when the device rotates
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        resetExpandedPos(); // Reset all expanded card views to its original size
        recyclerView_Gosho.getAdapter().notifyDataSetChanged();
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
                return R.id.image_gosho;
            }

            @Override
            // Setup the values for the parallax effect
            public int[] requireValuesForTranslate() {
                ImageView image = itemView.findViewById(R.id.image_gosho);

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
                    .inflate(R.layout.frag_gosho_card, parent, false);

            // Add the card menu and set up the popup menu
            final FrameLayout cardMenu = v.findViewById(R.id.dropdown_gosho);
            final PopupMenu popup = new PopupMenu(getContext(), cardMenu);

            // Refresh the user
            if (mAuth.getCurrentUser() != null) {
                mAuth.getCurrentUser().reload();
            }

            // Proceed if user is signed in
            if (mAuth.getCurrentUser() != null) {

                popup.getMenuInflater().inflate(R.menu.guidance_card_signin, popup.getMenu()); // Inflate menu

                // Set the text of the popup menu when it's first created
                Context context = LocaleHelper.setLocale(getActivity(), LocaleHelper.getLanguage(getActivity()));
                Resources resources = context.getResources();
                popup.getMenu().findItem(R.id.guidance_share).setTitle(resources.getString(R.string.guidance_share));
                popup.getMenu().findItem(R.id.guidance_copy).setTitle(resources.getString(R.string.guidance_copy));
                popup.getMenu().findItem(R.id.guidance_bookmark).setTitle(resources.getString(R.string.guidance_bookmark));

                // Set up a listener to the card menu
                onCardMenuClicked(cardMenu, popup);

                // Exit to the login activity if user is not signed in
            } else {
                Toast.makeText(getActivity(), getString(R.string.please_signin_again),
                        Toast.LENGTH_LONG).show();
                Intent i = new Intent(getActivity(), LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                getActivity().finish();
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
            final ImageButton retryImage = holder.view.findViewById(R.id.retry_image_gosho);
            retryImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadGlide(holder, holder.getAdapterPosition());
                }
            });

            // Load the image via Glide
            loadGlide(holder, position);

            // Pass the position of the BookmarkDays to get the position of the ArrangeDays
            int index = bookmarkDays.getIndex(position);

            // Return if there is no data
            if (index == -1) {
                TextView tv = fragView_Gosho.findViewById(R.id.dummyView);
                tv.setVisibility(View.VISIBLE);
                return;
            }

            // Set the custom font for the date text view
            TextView date = holder.view.findViewById(R.id.textview_gosho_date);
            date.setText(bookmarkDays.getDateBookmark(index));  // Add the date into the text view

            // Content text view
            final TextView content = holder.view.findViewById(R.id.textview_gosho_content);
            final String contentText = bookmarkDays.getContentBookmark(index);
            content.setText(contentText); // Add the content into the text view
            content.addOnLayoutChangeListener(this); // Listen to layout changes

            // Source text view
            TextView source = holder.view.findViewById(R.id.textview_gosho_source);
            source.setText(bookmarkDays.getSourceBookmark(index));  // Add the source into the text view

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
            Guideline guideline = holder.view.findViewById(R.id.guideline_gosho);
            ConstraintLayout.LayoutParams guideParam = (ConstraintLayout.LayoutParams) guideline.getLayoutParams();
            guideParam.guideBegin = (int) (deviceHeight * 0.25);
            guideline.setLayoutParams(guideParam);

            // Extra padding is needed if the text view's height is 0, which excludes the padding
            extraPadding = content.getHeight() == 0 ?
                    content.getPaddingBottom() + content.getPaddingTop() : 0;

            // Set the layout params of the card view to wrap content
            RecyclerView.LayoutParams wrap = new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    recyclerView_Gosho.getHeight() + getTextHeight(content, contentText)
                            - content.getHeight() + content.getPaddingBottom()
                            + content.getPaddingTop() + extraPadding);

            // Set the layout params of the card view to match parent
            RecyclerView.LayoutParams match = new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT);

            // Expand frame and expand button
            FrameLayout expandFrame = holder.view.findViewById(R.id.expand_gosho);
            ImageView expandIcon = holder.view.findViewById(R.id.expand_icon_gosho);

            // Check if the card was clicked
            if (posClicked == position) {
                posClicked = -1; // Reset the click position

                // Animate the card view when the layout changes
                ValueAnimator anim = isExpanded ? ValueAnimator.ofInt(
                        // Change from match parent to wrap content if isExpanded is true
                        recyclerView_Gosho.getHeight(), recyclerView_Gosho.getHeight()
                                + clickedTextHeight - clickedTVHeight
                                + content.getPaddingBottom() + content.getPaddingTop() + extraPadding)
                        // Change from wrap content to match parent if isExpanded is false
                        : ValueAnimator.ofInt(holder.view.getHeight(), recyclerView_Gosho.getHeight());

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
                    clickedTVHeight = holder.view.findViewById(R.id.textview_gosho_content).getHeight();
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

            TextView content = holder.view.findViewById(R.id.textview_gosho_content);
            content.removeOnLayoutChangeListener(this); // Remove the layout change listener

            FrameLayout expandFrame = holder.view.findViewById(R.id.expand_gosho);
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
                    .getParent().getParent()).findViewById(R.id.expand_gosho);

            // Hide the expand frame if the string's height can fit into the text view's height
            // Only take into account cards that have not been expanded by making sure the
            // height of the card matches the recycler view's height
            if (textHeight <= vHeight && cardHeight
                    == recyclerView_Gosho.getHeight()) {
                expandFrame.setVisibility(View.INVISIBLE); // Hide the expand frame
            } else {
                expandFrame.setVisibility(View.VISIBLE); // Show the expand frame

                // Fade the bottom part of the text only when the card view is not expanded
                if (cardHeight == recyclerView_Gosho.getHeight()) {
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
        private void setCardBackgroundColor(ViewHolder holder, int index) {
            boolean isColorEnabled;

            // Check the shared preference to set the color of the cards
            SharedPreferences cardColorPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            isColorEnabled = cardColorPref.getBoolean(GUIDANCE_CARD_COLOR, true);

            CardView cardView = holder.view.findViewById(R.id.cardview_gosho);

            Calendar calendar = Calendar.getInstance();
            int calendarMaxDays = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
            int leap;

            // Get the correct index first
            int position = bookmarkDays.getIndex(index);

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
        void loadGlide(final ViewHolder holder, int index) {
            // Get the correct index first
            int position = bookmarkDays.getIndex(index);

            // Get the firebase storage reference
            StorageReference pathRef =
                    Guidance.getFirebaseStorageRef(
                            getActivity(), position, Guidance.DGuidance.DG, true);

            // Card image view
            final ImageView cardImage = holder.view.findViewById(R.id.image_gosho);

            // Progress bar running while image loads
            final ProgressBar circularLoader = holder.view.findViewById(R.id.image_loading_gosho);
            circularLoader.bringToFront(); // Bring the progress bar to the front
            circularLoader.setVisibility(View.VISIBLE); // Reveal the progress bar

            // Only show the error image when the image is unable to load
            final ImageView errorImage = holder.view.findViewById(R.id.error_image_gosho);
            errorImage.setVisibility(View.INVISIBLE);

            // Only show the retry image when the image is unable to load
            final ImageButton retryImage = holder.view.findViewById(R.id.retry_image_gosho);
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
            return bookmarkList.size();
        }

        // Set up a listener to the card menu
        private void onCardMenuClicked(final FrameLayout cardMenu, final PopupMenu popup) {
            cardMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    // Get the language context
                    Context context = LocaleHelper.setLocale(getActivity(),
                            LocaleHelper.getLanguage(getActivity()));
                    Resources resources = context.getResources();

                    // Set the option to remove the bookmark
                    popup.getMenu().findItem(R.id.guidance_bookmark)
                            .setTitle(resources.getString(R.string.guidance_unbookmark));

                    // Set a click listener on the menu items
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int id = item.getItemId();

                            if (id == R.id.guidance_share_text) { // When the share menu is clicked
                                shareGosho(); // Share the daily gosho
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
                                copyGosho(v); // Copy the selected daily gosho into the clipboard
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
            String lang;

            // Get a reference to the firebase database
            DatabaseReference mDataRef = mData.getReference(); // Base reference
            DatabaseReference mDataChild; // Child reference

            // Get the array list of bookmarks saved in firebase database
            // The array reference depends on the language context
            if (LocaleHelper.getLanguage(getActivity()).equals("en")) {
                lang = "Eng";
            } else {
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

                // Remove the bookmark from the database if it already exists
                if (title.equals(resources.getString(R.string.guidance_unbookmark))) {
                    // Change the text of the bookmark menu
                    item.setTitle(resources.getString(R.string.please_wait));

                    // Get the correct index first
                    final int index = bookmarkDays.getIndex(visibleChild);

                    // Remove from the array list
                    for (int i = 0; i < bookmarkList.size(); i++) {
                        if (bookmarkList.get(i).equals("DG " + lang + " "
                                + bookmarkDays.getDateBookmark(index).trim())) {
                            bookmarkList.remove(i);
                            break;
                        }
                    }

                    // Remove from firebase database
                    mDataChild.child("DG " + lang + " "
                            + bookmarkDays.getDateBookmark(index).trim())
                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
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
                                // Run this when there is no bookmark
                                if (bookmarkList.isEmpty()) {
                                    LinearLayout linearLayout = fragView_Gosho
                                            .findViewById(R.id.dummyView_bookmark);
                                    linearLayout.setVisibility(View.VISIBLE);
                                    linearLayout.setGravity(Gravity.CENTER);
                                    TextView tv = linearLayout
                                            .findViewById(R.id.dummyView_textView_bookmark);
                                    tv.setText(R.string.favourite_gosho);
                                }

                                //Get the date
                                String date = bookmarkDays.getDateBookmark(index);
                                // Trim the date to remove the spacing at the beginning
                                date = date.trim();
                                // Update the recycler view
                                notifyDataSetChanged();

                                // Show a message informing the guidance has been bookmarked
                                Snackbar.make(v, resources.getString(R.string.daily_gosho)
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
                    });
                }
            }
        }

        // Share the daily gosho
        private void shareGosho() {
            String date, content, source;

            // Get the visible view on screen
            int visibleChild = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();

            // Language context
            Context context = LocaleHelper.setLocale(getActivity(), LocaleHelper.getLanguage(getActivity()));
            Resources resources = context.getResources();

            // Get the correct index first
            int index = bookmarkDays.getIndex(visibleChild);

            //Get the date, content and source
            date = bookmarkDays.getDateBookmark(index);
            date = date.trim(); // Trim the date to remove the spacing at the beginning
            content = bookmarkDays.getContentBookmark(index);
            source = bookmarkDays.getSourceBookmark(index);

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.daily_gosho)
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
            int visibleChild = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();

            // Get the correct index first
            int position = bookmarkDays.getIndex(visibleChild);

            // Get the firebase storage reference
            StorageReference pathRef = Guidance.getFirebaseStorageRef(
                    getActivity(), position, Guidance.DGuidance.DG, true);

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

        // Copy the selected daily gosho into the clipboard
        private void copyGosho(View v) {
            String date, content, source;

            // Get the visible view on screen
            int visibleChild = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();

            // Language context
            Context context = LocaleHelper.setLocale(getActivity(), LocaleHelper.getLanguage(getActivity()));
            Resources resources = context.getResources();

            // Get the correct index first
            int index = bookmarkDays.getIndex(visibleChild);

            //Get the date, content and source
            date = bookmarkDays.getDateBookmark(index);
            date = date.trim(); // Trim the date to remove the spacing at the beginning
            content = bookmarkDays.getContentBookmark(index);
            source = bookmarkDays.getSourceBookmark(index);

            // Copy the information into the clipboard
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(resources.getString(R.string.daily_gosho) + " " + date,
                    resources.getString(R.string.daily_gosho)
                            + " " + Guidance.getSunriseEmoji()
                            + "\n" + date + "\n\n" + content + "\n\n" + source + "\n\n"
                            + resources.getString(R.string.share_promotion1)
                            + new String(Character.toChars(0x1F54A))
                            + resources.getString(R.string.share_promotion2));
            clipboard.setPrimaryClip(clip);

            // Show a message informing the guidance has been copied
            Snackbar.make(v, resources.getString(R.string.daily_gosho) + " (" + date + ") "
                    + resources.getString(R.string.copy) + "!", Snackbar.LENGTH_LONG).show();
        }
    }

    // Sort the bookmark array list in ascending order
    private void sortBookmark() {
        Collections.sort(bookmarkList, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                int returnInt = 0;
                String aDate = a.substring(7, a.length()); // Get the date from the list item
                String bDate = b.substring(7, b.length()); // Get the date from the list item

                // Get the index of the date from the ArrangeDays class
                int aPos = bookmarkDays.getDateBookmarkList().indexOf(aDate);
                int bPos = bookmarkDays.getDateBookmarkList().indexOf(bDate);

                // Return the sorting positions
                if (aPos - bPos > 0) {
                    returnInt = 1;
                } else if (aPos - bPos < 0) {
                    returnInt = -1;
                } else if (aPos - bPos == 0) {
                    returnInt = 0;
                }
                return returnInt;
            }
        });
    }

    // Class to list only the bookmarks by acting as an intermediary between the view and the
    // ArrangeDays class. This class gets its content from the ArrangeDays class and displays
    // only the bookmark list
    static class BookmarkDays {
        ArrayList<String> bookmarkList;
        ArrangeDays arrangeDays;

        // Initialize the class
        private void init(ArrayList<String> bookmarkList,
                          String[] dateList,
                          String[] contentList,
                          String[] sourceList) {

            this.bookmarkList = bookmarkList;
            arrangeDays = new ArrangeDays();
            arrangeDays.init(dateList, contentList, sourceList);
        }

        // Search the bookmarkList with the position given to get the date
        // Get the index of the date from the arrangeDays class
        private int getIndex(int position) {
            // Return -1 to indicate that the list is empty
            if (bookmarkList.isEmpty()) {
                return -1;
            }

            String item = bookmarkList.get(position); // Get the list item
            String itemDate = item.substring(7, item.length()); // Get the date from the list item
            // Get the index of the date from the ArrangeDays class
            return arrangeDays.getFullDateList().indexOf(itemDate);
        }

        // Return the date
        private String getDateBookmark(int position) {
            return arrangeDays.getFullDateList().get(position);
        }

        // Return the date list
        private List<String> getDateBookmarkList() {
            return arrangeDays.getFullDateList();
        }

        // Return the content
        private String getContentBookmark(int position) {
            return arrangeDays.getFullContentList().get(position);
        }

        // Return the source
        private String getSourceBookmark(int position) {
            return arrangeDays.getFullSourceList().get(position);
        }
    }

    // Get the date, content and source in the string array after deciding if it's a leap year
    static class ArrangeDays {
        List<String> fullDateList, fullContentList, fullsourceList;

        // Get the date, content and source depending if it's a leap year and if the day is after 28th Feb
        void init(String[] dateList, String[] contentList, String[] sourceList) {
            // Special full list for the bookmarks to add the days into the list
            fullDateList = new ArrayList<>();
            fullContentList = new ArrayList<>();
            fullsourceList = new ArrayList<>();

            // Loop through the entire 366 days and add the string array into the list
            for (int i = 0; i < dateList.length; i++) {
                fullDateList.add(dateList[i]);
                fullContentList.add(contentList[i]);
                fullsourceList.add(sourceList[i]);
            }
        }

        List<String> getFullDateList() {
            return fullDateList;
        }

        List<String> getFullContentList() {
            return fullContentList;
        }

        List<String> getFullSourceList() {
            return fullsourceList;
        }
    }
}

