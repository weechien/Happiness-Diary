package com.goh.weechien.HappinessDiary;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class Bookmark extends AppCompatActivity {
    BookmarkEncouragementFrag fragEncouragement;
    BookmarkGoshoFrag fragGosho;
    ViewPagerAdapter viewPagerAdapter;
    ArrayList<String> bookmarkListDEeng, bookmarkListDEchi, bookmarkListDGeng, bookmarkListDGchi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);

        // Get the array list of bookmarks from the guidance activity
        bookmarkListDEeng = getIntent().getStringArrayListExtra("DE_ENG_BOOKMARKS");
        bookmarkListDEchi = getIntent().getStringArrayListExtra("DE_CHI_BOOKMARKS");
        bookmarkListDGeng = getIntent().getStringArrayListExtra("DG_ENG_BOOKMARKS");
        bookmarkListDGchi = getIntent().getStringArrayListExtra("DG_CHI_BOOKMARKS");

        // Create an instance of the array list if it's null
        if (bookmarkListDEeng == null) {
            bookmarkListDEeng = new ArrayList<>();
        }

        if (bookmarkListDEchi == null) {
            bookmarkListDEchi = new ArrayList<>();
        }

        if (bookmarkListDGeng == null) {
            bookmarkListDGeng = new ArrayList<>();
        }

        if (bookmarkListDGchi == null) {
            bookmarkListDGchi = new ArrayList<>();
        }

        Toolbar toolbar = findViewById(R.id.bookmark_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Add the ViewPager and attach a new adapter
        final ViewPager viewPager = findViewById(R.id.bookmark_viewpager);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), this);
        viewPager.setAdapter(viewPagerAdapter);

        // Create a parallax effect when sliding horizontally
        viewPager.setPageTransformer(false, new ParallaxPagerTransformer(R.id.image_frame));

        // Add the TabLayout, link the TabLayout with the ViewPager and set a click listener
        final TabLayout tabLayout = findViewById(R.id.bookmark_tablayout);
        tabLayout.setupWithViewPager(viewPager);

        // Iterate over all tabs and set the custom view
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            assert tab != null;

            View v = LayoutInflater.from(this).inflate(R.layout.custom_textview_bookmark, null);
            TextView tv = v.findViewById(R.id.bookmark_tablayout_custom_textview);
            if (i == 0) {
                tv.setText(getString(R.string.daily_encouragement)); // Set the title based on the tab position
            } else if (i == 1) {
                tv.setText(getString(R.string.daily_gosho)); // Set the title based on the tab position
            }

            // Setup the custom text view
            tab.setCustomView(v);
        }

        // Change the display whenever the tab changes
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }
        });
        // Enable the view pager to over scroll
        OverScrollDecoratorHelper.setUpOverScroll(viewPager);
    }

    @Override
    protected void onResume() {
        super.onResume();

        TextView textView = findViewById(R.id.toolbar_bookmark_textview);
        textView.setText(R.string.guidance_bookmark); // Title of the activity
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            setResult();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult();
    }

    private void setResult() {
        // Pass the array list back to the guidance activity
        Intent i = new Intent();
        i.putExtra("DE_ENG_BOOKMARKS", bookmarkListDEeng);
        i.putExtra("DE_CHI_BOOKMARKS", bookmarkListDEchi);
        i.putExtra("DG_ENG_BOOKMARKS", bookmarkListDGeng);
        i.putExtra("DG_CHI_BOOKMARKS", bookmarkListDGchi);
        setResult(Activity.RESULT_OK, i);
        finish();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
    }

    // View Pager Adapter to launch the fragments and name the tab layout
    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private String fragments[] = {getString(R.string.daily_encouragement),
                getString(R.string.daily_gosho)};

        private ViewPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
        }

        @Override
        public int getCount() {
            return fragments.length;
        }

        @Override
        // Launch a fragment based on the tab position
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new BookmarkEncouragementFrag();
                case 1:
                    return new BookmarkGoshoFrag();
                default:
                    return null;
            }
        }

        @Override
        // Get the fragments references which would be used to scroll to a specified card position
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment frag = (Fragment) super.instantiateItem(container, position);

            // save the appropriate reference depending on position
            switch (position) {
                case 0:
                    fragEncouragement = (BookmarkEncouragementFrag) frag;
                    break;
                case 1:
                    fragGosho = (BookmarkGoshoFrag) frag;
                    break;
            }
            return frag;
        }

        @Override
        // Set the title for the tab
        public CharSequence getPageTitle(int position) {
            return fragments[position];
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

    // Class to adjust the items in the view pager to create a parallax effect
    private class ParallaxPagerTransformer implements ViewPager.PageTransformer {
        private int id;
        private int border = 0;
        private float speed = 0.2f;

        ParallaxPagerTransformer(int id) {
            this.id = id;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void transformPage(View view, float position) {

            View parallaxView = view.findViewById(id);

            if (parallaxView != null) {
                if (position > -1 && position < 1) {
                    float width = parallaxView.getWidth();
                    parallaxView.setTranslationX(-(position * width * speed));
                    float sc = ((float) view.getWidth() - border) / view.getWidth();
                    if (position == 0) {
                        view.setScaleX(1);
                        view.setScaleY(1);
                    } else {
                        view.setScaleX(sc);
                        view.setScaleY(sc);
                    }
                }
            }
        }

        public void setBorder(int px) {
            border = px;
        }

        public void setSpeed(float speed) {
            this.speed = speed;
        }
    }
}
