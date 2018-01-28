package com.goh.weechien.HappinessDiary;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

// Search the guidance activity using keywords and direct the user back to the guidance activity when
// a search result is selected
public class SearchResultActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private SearchAdapter mAdapter;
    private RecyclerView recyclerView;
    private List<SearchModel> mModels;
    SearchView searchView;
    CharSequence searchQueryText;
    String language;
    String searchHint;
    static int tabPosition;

    // Class to collect the string arrays and instantiate them as objects
    // The spannable class is used to edit part of the string's color
    // The string class is unable to edit part of the string's color
    private static class SearchModel {
        private final long mId;
        private String mDate, mContent, mSource, mTextString;
        private SpannableStringBuilder mTextSpan;

        SearchModel(long id, String date, String content, String source) {
            mId = id; // Unique identifier
            mDate = date; // Reference the original string date
            mContent = content; // Reference the original string content
            mSource = source; // Reference the original string source
            mTextString = mDate + " " + mContent + " " + mSource; // Combine all
            mTextSpan = new SpannableStringBuilder(mTextString); // Used to edit string color
        }

        long getId() { // Get the ID
            return mId;
        }

        // Get the original string
        String getTextString() { // Used in the filter/ellipsizeText/highlightQuery method
            return mTextString;
        }

        // Get the edited (ellipsized & colored) string
        Spannable getTextSpan() { // Used in the filter/ellipsizeText/highlightQuery method
            return mTextSpan;
        }

        // Set a new edited (ellipsized & colored) string
        void setTextSpan(String mTextSpan) { // Used in the filter/ellipsizeText/highlightQuery method
            this.mTextSpan = this.mTextSpan.replace(0, this.mTextSpan.length(), mTextSpan);
        }

        // Get the date from the edited string
        Spannable getDateSpan() { // Used in the onBindViewHolder method
            return (Spannable) mTextSpan.subSequence(0, mDate.length());
        }

        // Get the content and source from the edited string
        Spannable getContentSpan() { // Used in the onBindViewHolder method
            return (Spannable) mTextSpan.subSequence(mDate.length() + 1, mTextSpan.length());
        }

        // Get the date from the original string
        String getDateString() { // Used in the filter/ellipsizeText/highlightQuery method
            return mTextString.substring(0, mDate.length());
        }

        // Get the content from the original string
        String getContentString() { // Used in the filter/ellipsizeText/highlightQuery method
            return mTextString.substring(mDate.length() + 1, mTextString.length() - mSource.length() - 1);
        }

        // Get the source from the original string
        String getSourceString() { // Used in the filter/ellipsizeText/highlightQuery method
            return mTextString.substring(mTextString.length() - mSource.length(), mTextString.length());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);
        Toolbar toolbar = findViewById(R.id.search_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        String[] myDate, myContent, mySource;

        // Get the position of the tab layout to know which guidance to display and search
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            tabPosition = extras.getInt("TAB_POSITION");
            searchHint = extras.getString("SEARCH_HINT");
        } else {
            tabPosition = savedInstanceState.getInt("TAB_POSITION");
            searchHint = savedInstanceState.getString("SEARCH_HINT");
        }
        // Get the current language
        language = LocaleHelper.getLanguage(this);

        // Add the string arrays which include 29th Feb
        // String array chosen depends on the type of guidance (encouragement or gosho)
        myDate = getResources().getStringArray(R.array.daily_encouragement_and_gosho_date);
        if (tabPosition == 0) { // Get the tab position (Encouragement)
            myContent = getResources().getStringArray(R.array.daily_encouragement_content);
            // The source for encouragement is not an array, so add it as an array
            mySource = new String[366];
            for (int i = 0; i < mySource.length; i++) {
                mySource[i] = getString(R.string.daisaku_ikeda);
            }
        } else { // Else if the tab position is at Gosho
            myContent = getResources().getStringArray(R.array.daily_gosho_content);
            mySource = getResources().getStringArray(R.array.daily_gosho_source);
        }

        // Use this class to adjust the string arrays by calculating the correct days (365/366)
        ArrangeList arrangeDays = new ArrangeList(myDate, myContent, mySource);

        // Add the recycler fragView and fix its size
        recyclerView = findViewById(R.id.search_recycler);
        recyclerView.setHasFixedSize(true);

        // Add the layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);

        // Add the adapter and pass the comparator as an argument
        if (mAdapter == null) {
            mAdapter = new SearchAdapter(COMPARATOR);
        }
        recyclerView.setAdapter(mAdapter);

        // Add all the items in the model class into the sorted list
        mModels = new ArrayList<>();
        for (int i = 0; i < arrangeDays.getContentList().size(); i++) {
            // The model accepts the id, date, content and source parameters
            // The arrangeDays class is used to retrieve the correct days
            mModels.add(new SearchModel(i, arrangeDays.getDateList().get(i),
                    arrangeDays.getContentList().get(i), arrangeDays.getSourceList().get(i)));
        }
        mAdapter.add(mModels);

        // Get the device height
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int deviceHeight = displayMetrics.heightPixels;

        // Place the view outside of the screen
        recyclerView.setTranslationY(deviceHeight);
        // Animate the recycler view when the activity starts
        recyclerView.animate()
                .translationY(0)
                .setInterpolator(new DecelerateInterpolator(3.f))
                .setDuration(700)
                .setStartDelay(0)
                .start();
        // Enable the recycler view to over scroll
        OverScrollDecoratorHelper.setUpOverScroll(recyclerView, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save the query
        outState.putCharSequence("SEARCH_QUERY_TEXT", searchView.getQuery());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Restore the query
            searchQueryText = savedInstanceState.getCharSequence("SEARCH_QUERY_TEXT");
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_result, menu); // Inflate the options menu
        // Reference the search action view in the app bar
        searchView = (SearchView) menu.findItem(R.id.search_result).getActionView();
        searchView.setQueryHint(searchHint); // Set the search hint
        searchView.setIconified(false); // The search action view is always expanded (no icon)
        searchView.setMaxWidth(Integer.MAX_VALUE); // Search view to take up the entire toolbar
        // Disable the full-screen keyboard in landscape mode
        searchView.setImeOptions(searchView.getImeOptions()
                | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        searchView.setOnQueryTextListener(this); // Listen to text changes in the search view
        if (searchQueryText != null) {
            // Get the search query if !null whenever the activity restores its instance state
            searchView.setQuery(searchQueryText, true);
            searchQueryText = null;
        }
        return true;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
    }

    @Override
    // Search is instantaneous, thus there is no submit button
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    // Listen to text changes in the search view
    public boolean onQueryTextChange(final String newText) {
        // Run the filters, ellipses and highlights on a separate thread
        Runnable r = new Runnable() {
            @Override
            public void run() {
                // Filter the items in the model class based on the query text
                List<SearchModel> filteredModelList = filter(mModels, newText.trim());
                mAdapter.replaceAll(filteredModelList);
            }
        };
        Handler handler = new Handler();
        handler.postDelayed(r, 0);

        recyclerView.scrollToPosition(0); // Scroll to the top
        return true;
    }

    // Filter the items in the model class based on the query text
    private List<SearchModel> filter(List<SearchModel> models, String query) {
        String lowerCaseQuery = query.toLowerCase(); // Change the query to lower case

        List<SearchModel> filteredModelList = new ArrayList<>();
        for (SearchModel model : models) {
            // Get the original string of each model object and change it to lower case
            // The original string contains the date, content and source
            // The date is filtered and will be highlighted but will not be ellipsized
            String text = model.getTextString().toLowerCase();
            if (text.contains(lowerCaseQuery)) { // Proceed if the original string contains the query
                // Ellipsize the string but only include the content and source (exclude date)
                // The date will be attached to the ellipsized string at the end to be highlighted
                model = ellipsizeText(model, model.getContentString()
                        + " (" + model.getSourceString() + ")", lowerCaseQuery);
                filteredModelList.add(model); // Add the filtered items into the list array
            }
        }
        return filteredModelList;
    }

    // Ellipsize only the content and source
    private SearchModel ellipsizeText(SearchModel model, String text, String lowerCaseQuery) {
        String ELLIPSIS = getString(R.string.ellipsis);
        String output = ""; // The output text to be returned
        String lowerText = text.toLowerCase();
        final int stringLoop = 6; // No. of loops to run before ellipsize string
        final int stringLength = 2; // No. of characters in a string to loop through (Chinese)
        int queryLength = lowerCaseQuery.length();
        // right/leftFinder: Loop to the right/left of the query and ellipsize the string
        int rightFinder = 0, leftFinder;
        // spaceCount: Count the no. of spaces in a string (English)
        // outputQueryEnd: End position of the query within the output string variable
        // jStart: String position to start searching to the right
        int spaceCount = 0, outputQueryEnd = 0, jStart;

        // Return the original string if there is no query
        if (lowerCaseQuery.trim().equals("")) {
            model.setTextSpan(model.getDateString() + " " + text);
            return model;
        }

        // Find the first position of the original string
        int index = lowerText.indexOf(lowerCaseQuery);
        List<Integer> indexList = new ArrayList<>();

        outerloop:
        // Loop through all the query string in the original string
        for (int i = 0; i < lowerText.length() && index != -1; i = index + 1) {
            index = lowerText.indexOf(lowerCaseQuery, i); // Find the next query position

            if (index == -1) {
                break; // Break the loop if the next query is not found
            } else {
                // Proceed if the current query is already covered within the output
                // The rightFinder loop of the previous query has looped past the position of
                // the current query, so we have to find the current query's position
                // In this case, the leftFinder loop will not be run since the current query is
                // already contained within the output
                // We only have to find out the extra string included by the rightFinder for the
                // previous query so that the rightFinder for the current query do not have to
                // repeat the same loop
                if (rightFinder >= index + queryLength) {
                    output = output.substring(0, output.length() - 1); // Remove the ellipsis
                    // Find the start position of the current query within output
                    int nextOutput = output.toLowerCase().indexOf(lowerCaseQuery, outputQueryEnd);
                    indexList.add(nextOutput); // List of the query positions within the output
                    outputQueryEnd = nextOutput + queryLength; // End position of the current query
                    // Get the string after the end position of the current query
                    // We want to find the extra string so that the rightFinder loop do not have to
                    // re-loop from the current query's position, and to reduce the required loop
                    // of the rightFinder
                    String extraString = output.substring(nextOutput + queryLength, output.length());
                    // Decide the number of loops to reduce by: Calculating the number of
                    // spaces (English)/Dividing the extra string with the stringLength (Chinese)
                    spaceCount = (language.equals("en")) ? extraString.length() - extraString
                            .replace(" ", "").length() + 1 : extraString.length() / stringLength;
                    // Prevent spaceCount == 0 if it's chinese
                    spaceCount = (!language.equals("en")) && spaceCount == 0 ? 1 : spaceCount;
                } else { // Proceed if the current query is not covered within the output
                    // Use a pseudo string which will be attached to the output afterwards
                    String subOutput = text.substring(index, index + queryLength);

                    int leftCounter = 0; // Count the number of left loops
                    // Find the: First space to the left (English)/stringLength offset from the
                    // query's start position to the left (Chinese)
                    leftFinder = (language.equals("en")) ? lowerText.lastIndexOf(" ", index - 1)
                            : index - 1 - stringLength;

                    // Use the leftmost position for the output since the leftFinder is unable 
                    // to move left anymore, and the loop will not be run
                    if (leftFinder <= 0) {
                        output = text.substring(0, index) + subOutput;
                    }

                    // The leftFinder loop: Loop from the query position to the left
                    // Initialize x to the start position of the current query
                    // Continue looping left until we are unable to move left anymore
                    for (int x = index - 1; x >= 0 && !(leftFinder <= 0); x = leftFinder - 1) {
                        leftCounter++;
                        // Find the: First space to the left (English)/stringLength offset from the
                        // query's start position to the left (Chinese)
                        leftFinder = (language.equals("en")) ? lowerText.lastIndexOf(" ", x)
                                : x - stringLength;
                        // Use the leftmost position for the output since the leftFinder is unable
                        // to move left anymore, and the loop will not be run
                        if (leftFinder <= 0) {
                            output = text.substring(0, x + 1) + subOutput;
                            break;
                        } else {
                            // If we are able to move left then combine the new left string with
                            // the existing string and continue building leftward
                            subOutput = text.substring(leftFinder, x + 1) + subOutput;
                            // Proceed if the leftFinder of the current query has reached
                            // the boundary of the rightFinder of the previous query
                            if (rightFinder > 0 && rightFinder >= leftFinder) {
                                // Remove the ellipsis from the output and combine with the pseudo string
                                output = output.substring(0, output.length() - 3) + subOutput
                                        .substring(rightFinder - leftFinder, subOutput.length());
                                break;
                                //  Proceed if the no. of loops matches the the required loop
                            } else if (leftCounter >= stringLoop) {
                                // Add an ellipsis to the beginning of the pseudo string
                                subOutput = ELLIPSIS + subOutput.substring(1, subOutput.length());
                                output = output + " " + subOutput; // Combine the pseudo string with the output
                                break;
                            }
                        }
                    }
                    // The outputQueryEnd variable is used to find the next query string in
                    // output to allow all the queries' positions to be added into the list
                    // The list is used to get the queries' positions and highlight them
                    outputQueryEnd = output.length(); // End position of the current query
                    indexList.add(outputQueryEnd - queryLength); // List of the query positions within the output
                }

                // Proceed if the rightFinder for the previous query has loop past the position
                // of the current query
                // The spaceCount variable will decide how many loops to reduce
                // The jStart variable will get the end position of the string, which is after
                // the end position of the current query, and start looping from there
                if (spaceCount > 0) {
                    jStart = (language.equals("en")) ? rightFinder : rightFinder + 1;
                    // Proceed normally if the rightFinder for the previous query has not loop past
                    // the position of the current query
                } else {
                    jStart = index + queryLength;
                }

                int rightCounter = 0; // Count the number of right loops
                // Find the: First space to the right (English)/stringLength offset from the
                // query's end position to the right (Chinese)
                rightFinder = (language.equals("en")) ? lowerText.indexOf(" ", jStart) : jStart + stringLength;

                // Use the rightmost position for the output since the rightFinder is unable 
                // to move right anymore, and the loop will not be run
                if (rightFinder > lowerText.length() - 1 || rightFinder < 0) {
                    // Break if the output contains the entire text
                    if (jStart > lowerText.length()) {
                        break;
                    }
                    output = output + text.substring(jStart, lowerText.length());
                }

                // The rightFinder loop: Loop from the query position to the right
                // Initialize x to the end position of the current query
                // Continue looping right until we are unable to move right anymore
                for (int j = jStart; j < lowerText.length() && !(rightFinder > lowerText.length() - 1 || rightFinder < 0); j = rightFinder + 1) {
                    rightCounter++;
                    // Find the: First space to the right (English)/stringLength offset from the
                    // query's end position to the right (Chinese)
                    rightFinder = (language.equals("en")) ? lowerText.indexOf(" ", j) : j + stringLength;
                    // Use the rightmost position for the output since the rightFinder is unable
                    // to move right anymore, and the loop will not be run
                    if (rightFinder > lowerText.length() - 1 || rightFinder < 0) {
                        output = output + text.substring(j, lowerText.length());
                        break outerloop; // Break 2 loops to get to the outerloop
                    } else {
                        // If we are able to move right then combine the new right string with
                        // the existing string and continue building rightward
                        output = output + text.substring(j, rightFinder + 1);
                        // Proceed if the no. of loops matches the the required loop minus off
                        // any extra loops from spaceCount
                        if (rightCounter >= stringLoop - spaceCount) {
                            spaceCount = 0; // Reset the space count
                            // Add an ellipsis to the end of the output
                            output = (language.equals("en")) ?
                                    output.substring(0, output.length() - 1) + ELLIPSIS :
                                    output.substring(0, output.length()) + ELLIPSIS;
                            break;
                        }
                    }
                }
            }
        }
        // Set the original date string and output as an editable string
        model.setTextSpan(model.getDateString() + " " + output);
        // Highlight the queried strings with the given positions in the list
        model = highlightQuery(model, indexList, lowerCaseQuery);
        return model;
    }

    // Highlight the queried string using the query positions in the list
    private SearchModel highlightQuery(SearchModel model, List<Integer> indexList, String lowerCaseQuery) {
        int queryLength = lowerCaseQuery.length(); // Get the query length
        Spannable span = model.getTextSpan(); // Get the editable string which was set in the ellipsizeText method
        String dateString = model.getDateString().toLowerCase(); // Get the date to check for string to highlight
        // Find the date’s length to offset because the positions in the list (added in the ellipsizeText method)
        // did not include the date string as we do not wish to ellipsize the date. Thus, the date was only
        // added at the end of the ellipsizeText method and then passed to the highlightQuery method
        int offset = dateString.length() + 1;

        // Reset the color of the text to default
        ForegroundColorSpan[] spans = span.getSpans(0, span.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span1 : spans) {
            span.removeSpan(span1);
        }
        // Set the color for the text
        int color = ContextCompat.getColor(getApplicationContext(), R.color.colorAccent_Guidance);
        int indexDate = dateString.indexOf(lowerCaseQuery); // Find the query string in the date string

        // If a query is found in the date string, highlight the query string  and loop to find the next string
        for (int i = 0; i < dateString.length() && indexDate != -1; i = indexDate + 1) {
            indexDate = dateString.indexOf(lowerCaseQuery, i); // Find the next query string
            if (indexDate == -1) { // Break if no more query string is found in the date string
                break;
            } else { // Highlight the query string
                span.setSpan(new ForegroundColorSpan(color), indexDate, indexDate + queryLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // Loop through the index and highlight the query string with the offset
        for (int index : indexList) {
            span.setSpan(new ForegroundColorSpan(color), index + offset, index + queryLength + offset, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return model;
    }


    // Compare the items in the model class and set the sort order based on their IDs
    private static final Comparator<SearchModel> COMPARATOR = new Comparator<SearchModel>() {
        @Override
        public int compare(SearchModel a, SearchModel b) {
            return ((Long) a.getId()).compareTo(b.getId()); // Compare the IDs
        }
    };

    // Recycler View's Adapter to manage the view holders and bind the data
    class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {
        private final Comparator<SearchModel> mComparator;

        SearchAdapter(Comparator<SearchModel> comparator) {
            mComparator = comparator;
        }

        class SearchViewHolder extends RecyclerView.ViewHolder {
            private RelativeLayout view;

            SearchViewHolder(RelativeLayout view) {
                super(view);
                this.view = view;
            }
        }

        @Override
        public SearchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // Create a new view
            RelativeLayout v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.search_result_card, parent, false);
            return new SearchViewHolder(v);
        }

        @Override
        public void onBindViewHolder(SearchViewHolder holder, int position) {
            // Get the current model object being bind
            final SearchModel model = mSortedList.get(position);
            RelativeLayout layout = holder.view.findViewById(R.id.search_card_content);

            // When the view holder is clicked, exit the activity with animation and route back
            // to the guidance activity and pass the position (ID) selected
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    long modelID = model.getId();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("SEARCH_RESULT", modelID);
                    setResult(Activity.RESULT_OK, resultIntent); // Pass the ID back
                    // Wait for the click animation before ending the activity
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            finish();
                            overridePendingTransition(0, R.animator.slide_out); // Exit animation
                        }
                    };
                    Handler handler = new Handler();
                    handler.postDelayed(r, 200);
                }
            });

            // Add a watermark at the top right corner of each card (non-searchable)
            TextView title = holder.view.findViewById(R.id.textview_search_title);
            if (tabPosition == 0) {
                title.setText(R.string.daily_encouragement);
            } else {
                title.setText(R.string.daily_gosho);
            }
            // Add the date into the text view and set the custom font
            TextView date = holder.view.findViewById(R.id.textview_search_date);
            Typeface typeface = Typeface.createFromAsset(date.getContext()
                    .getAssets(), "fonts/beyond_the_mountains.otf");
            date.setTypeface(typeface);
            date.setText(model.getDateSpan());

            // Add the content into the text view
            TextView content = holder.view.findViewById(R.id.textview_search_content);
            content.setText(model.getContentSpan());
        }

        // Add an item to the sorted list
        public void add(SearchModel model) {
            mSortedList.add(model);
        }

        // Remove an item from the sorted list
        public void remove(SearchModel model) {
            mSortedList.remove(model);
        }

        // Add a list of items to the sorted list
        public void add(List<SearchModel> models) {
            mSortedList.addAll(models);
        }

        // Remove a list of items from the sorted list
        public void remove(List<SearchModel> models) {
            mSortedList.beginBatchedUpdates();
            for (SearchModel model : models) {
                mSortedList.remove(model);
            }
            mSortedList.endBatchedUpdates();
        }

        // Used together with the recycler view to sort (add, remove, replace, move, etc) and bind items
        private final SortedList<SearchModel> mSortedList = new SortedList<>(SearchModel.class, new SortedList.Callback<SearchModel>() {
            @Override
            // Compare 2 items and return how they should be ordered
            public int compare(SearchModel a, SearchModel b) {
                return mComparator.compare(a, b);
            }

            @Override
            // When an item is added at the given position
            public void onInserted(int position, int count) {
                notifyItemRangeInserted(position, count);
            }

            @Override
            // When an item is removed at the given position
            public void onRemoved(int position, int count) {
                notifyItemRangeRemoved(position, count);
            }

            @Override
            // When an item changes position in the list
            public void onMoved(int fromPosition, int toPosition) {
                notifyItemMoved(fromPosition, toPosition);
            }

            @Override
            // When an item is updated at the given position
            public void onChanged(int position, int count) {
                notifyItemRangeChanged(position, count);
            }

            @Override
            // Check whether two items have the same data or not
            public boolean areContentsTheSame(SearchModel oldItem, SearchModel newItem) {
                return false; // Always return false because the query text always changes
            }

            @Override
            // Decide whether two object represent the same item or not.
            public boolean areItemsTheSame(SearchModel item1, SearchModel item2) {
                return item1.getId() == item2.getId();
            }
        });

        // Remove items which are not in the list and add items which are missing in the list
        void replaceAll(List<SearchModel> models) {
            mSortedList.beginBatchedUpdates();
            for (int i = mSortedList.size() - 1; i >= 0; i--) {
                final SearchModel model = mSortedList.get(i);
                if (!models.contains(model)) {
                    mSortedList.remove(model);
                }
            }
            mSortedList.addAll(models);
            mSortedList.endBatchedUpdates();
        }

        @Override
        public int getItemCount() {
            return mSortedList.size();
        }
    }

    // Get the date, content and source in the string array after deciding if it's a leap year
    private static class ArrangeList {
        List<String> dateList, contentList, sourceList;
        private int ListLength;

        // Get the date, content and source depending if it's a leap year and if the day is after 28th Feb
        private ArrangeList(String[] dateList, String[] contentList, String[] sourceList) {
            Calendar calendar = Calendar.getInstance();
            int calendarMaxDays = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);

            this.dateList = new ArrayList<>();
            this.contentList = new ArrayList<>();
            this.sourceList = new ArrayList<>();

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
                    this.sourceList.add(sourceList[i + 1]);
                } else {
                    this.dateList.add(dateList[i]);
                    this.contentList.add(contentList[i]);
                    this.sourceList.add(sourceList[i]);
                }
            }
        }

        private List<String> getDateList() {
            return dateList;
        }

        private List<String> getContentList() {
            return contentList;
        }

        private List<String> getSourceList() {
            return sourceList;
        }
    }
}











