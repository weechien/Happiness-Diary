package com.goh.weechien.HappinessDiary;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import java.util.ArrayList;

public class About extends AppCompatActivity implements AdapterView.OnItemClickListener {
    String version;
    int emojiCounter;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ListView listView = findViewById(R.id.about_listview);
        listView.setDivider(null); // Disable list view divider
        // Disable click effect and enable them in xml
        listView.setSelector(android.R.color.transparent);
        listView.setOnItemClickListener(this); // Setup list view click listener

        ArrayList<Item> arrayList = new ArrayList<>();

        // Get the version name of the app
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // App version
        arrayList.add(new HeaderItem(getString(R.string.app_version)));
        arrayList.add(new EntryItem(version));

        // Legal information
        arrayList.add(new HeaderItem(getString(R.string.legal_information)));
        arrayList.add(new EntryItem(getString(R.string.terms_of_service)));
        arrayList.add(new EntryItem(getString(R.string.privacy_policy)));

        // Contact us
        arrayList.add(new HeaderItem(getString(R.string.contact_us)));
        arrayList.add(new EntryItem(getString(R.string.contact_email)));

        // Credits
        arrayList.add(new HeaderItem(getString(R.string.credits)));
        arrayList.add(new EntryItem(getString(R.string.open_source_license)));

        // Set adapter
        final AboutAdapter adapter = new AboutAdapter(this, arrayList);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView textView = findViewById(R.id.toolbar_about_textview);
        textView.setText(R.string.title_activity_about); // Title of the activity
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
    }

    @Override
    // Called when an item in the list view is clicked
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String string = "";
        String emoji = "";

        if (view.findViewById(R.id.about_item_title) == null) {
            return;
        }

        // Get the string from the text view if it is not empty
        if (!((TextView) view.findViewById(R.id.about_item_title)).getText().toString().equals("")) {
            string = ((TextView) view.findViewById(R.id.about_item_title)).getText().toString();
        }

        // Version name of the app
        // Make different emojis depending on the number of clicks
        if (string.equals(version)) {
            switch (emojiCounter) {
                case 0:
                    emoji = new String(Character.toChars(0x1F623));
                    emojiCounter = 1;
                    break;
                case 1:
                    emoji = new String(Character.toChars(0x1F614));
                    emojiCounter = 2;
                    break;
                case 2:
                    emoji = new String(Character.toChars(0x1F612));
                    emojiCounter = 3;
                    break;
                case 3:
                    emoji = new String(Character.toChars(0x1F627));
                    emojiCounter = 4;
                    break;
                case 4:
                    emoji = new String(Character.toChars(0x1F62F));
                    emojiCounter = 5;
                    break;
                case 5:
                    emoji = new String(Character.toChars(0x1F62C));
                    emojiCounter = 6;
                    break;
                case 6:
                    emoji = new String(Character.toChars(0x1F60C));
                    emojiCounter = 7;
                    break;
                case 7:
                    emoji = new String(Character.toChars(0x1F604));
                    emojiCounter = 8;
                    break;
                case 8:
                    emoji = new String(Character.toChars(0x1F606));
                    emojiCounter = 9;
                    break;
                case 9:
                    emoji = new String(Character.toChars(0x1F60D));
                    emojiCounter = 0;
                    break;
            }
            Toast.makeText(this, emoji, Toast.LENGTH_SHORT).show();

            // Terms of service
        } else if (string.equals(getString(R.string.terms_of_service))) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://diary-of-happiness.firebaseapp.com/ToS/"));
            startActivity(browserIntent);

            // Privacy policy
        } else if (string.equals(getString(R.string.privacy_policy))) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://diary-of-happiness.firebaseapp.com/PrivacyPolicy/"));
            startActivity(browserIntent);

            // Contact email
        } else if (string.equals(getString(R.string.contact_email))) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri data = Uri.parse("mailto:" + getString(R.string.contact_email)
                    + "?subject=" + getString(R.string.hello_happiness_diary)
                    + "&body=" + "");
            intent.setData(data);
            startActivity(intent);

            // Open source licenses
        } else if (string.equals(getString(R.string.open_source_license))) {
            new LibsBuilder()
                    .withActivityTheme(R.style.Guidance_NoActionBar)
                    .withActivityTitle(getString(R.string.open_source_license))
                    //provide a style (optional) (LIGHT, DARK, LIGHT_DARK_TOOLBAR)
                    .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                    //start the activity
                    .start(About.this);
        }
    }

    // Interface for list view headers and items
    interface Item {
        boolean isHeader();
        String getTitle();
    }

    // List view header
    private class HeaderItem implements Item {
        private final String title;

        HeaderItem(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        @Override
        public boolean isHeader() {
            return true;
        }
    }

    //List view item
    private class EntryItem implements Item {
        public final String title;

        EntryItem(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        @Override
        public boolean isHeader() {
            return false;
        }
    }

    // List view adapter
    private class AboutAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<Item> item;

        AboutAdapter(Context context, ArrayList<Item> item) {
            this.context = context;
            this.item = item;
            //this.originalItem = item;
        }

        @Override
        public int getCount() {
            return item.size();
        }

        @Override
        public Object getItem(int position) {
            return item.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // Check if the view called is a header or item
            if (item.get(position).isHeader()) {
                // if header
                convertView = inflater.inflate(R.layout.about_list_header, parent, false);
                TextView tvSectionTitle = convertView.findViewById(R.id.about_header_title);
                tvSectionTitle.setText(item.get(position).getTitle());
            } else {
                // if item
                convertView = inflater.inflate(R.layout.about_list_item, parent, false);
                TextView tvItemTitle = convertView.findViewById(R.id.about_item_title);
                tvItemTitle.setText(item.get(position).getTitle());
            }

            return convertView;
        }
    }
}
