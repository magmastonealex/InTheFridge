package net.magmastone.inthefrige.activities;

import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.magmastone.inthefrige.IntentIntegrator;
import net.magmastone.inthefrige.IntentResult;
import net.magmastone.inthefrige.R;
import net.magmastone.inthefrige.fragments.FridgeFragment;
import net.magmastone.inthefrige.fragments.RecFragment;
import net.magmastone.inthefrige.fragments.ScannerTab;
import net.magmastone.inthefrige.fragments.ShoppingFragment;
import net.magmastone.inthefrige.network.FRGItem;
import net.magmastone.inthefrige.network.UPCItem;
import net.magmastone.inthefrige.network.tasks.AddShoppingItemTask;
import net.magmastone.inthefrige.network.tasks.GetFRGStatusTask;
import net.magmastone.inthefrige.network.tasks.GetUPCTask;
import net.magmastone.inthefrige.network.tasks.NewUPCTask;
import net.magmastone.inthefrige.network.tasks.SetFRGStatusTask;


public class MainActivity extends ActionBarActivity implements ActionBar.TabListener, ScannerTab.ScannerInteraction,RecFragment.RecFragmentListener {
    public static final int newItemRcode=0x00302;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
           IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
           if (scanResult != null) {
               if (scanResult.getFormatName().contains("UPC")|| scanResult.getFormatName().contains("EAN")) {
                   String upc=scanResult.getContents();

                   final Context context = this.getApplicationContext();
                   final Context popContext = MainActivity.this;
                   final Activity ourActivity = this;
                   new GetUPCTask(new GetUPCTask.NetworkResults(){
                       @Override
                       public void NetworkSuccess(final UPCItem it){
                        if(it.status.equals("N")){
                            Log.d("NetworkResults", "NotFound");
                            Intent myIntent = new Intent(ourActivity, NewItemActivity.class);
                            myIntent.putExtra("UPC", it.upc); //Optional parameters
                            ourActivity.startActivityForResult(myIntent,newItemRcode);
                        }else{
                            Log.d("NetworkResults", "Found"+it.itemname);

                            new GetFRGStatusTask(new GetFRGStatusTask.NetworkResults() {
                                @Override
                                public void NetworkSuccess(FRGItem itm) {
                                    if(itm.status.equals("N") || itm.quantity==0) {
                                        Log.d("NS,FRGStatus",itm.status);
                                        launchAct();
                                    }else{
                                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                switch (which){
                                                    case DialogInterface.BUTTON_POSITIVE:

                                                        launchAct();
                                                        break;

                                                    case DialogInterface.BUTTON_NEGATIVE:

                                                        launchRem();
                                                        break;
                                                }
                                            }
                                        };

                                        AlertDialog.Builder builder = new AlertDialog.Builder(popContext);
                                        builder.setMessage("You already have "+String.valueOf(itm.quantity)+"  "+it.itemname).setPositiveButton("Add More", dialogClickListener)
                                                .setNegativeButton("Check Out", dialogClickListener).show();
                                    }
                                }
                                private void launchAct(){
                                    Intent myIntent = new Intent(ourActivity, CheckinItemActivity.class);
                                    Log.d("upctest", it.upc);
                                    myIntent.putExtra("UPC", it.upc);
                                    ourActivity.startActivity(myIntent);
                                }
                                private  void launchRem(){
                                    Intent intent = new Intent();
                                    intent.setAction("net.magmastone.NewItem");
                                    sendBroadcast(intent);
                                    new SetFRGStatusTask(new SetFRGStatusTask.NetworkResults(){

                                        @Override
                                        public void NetworkSuccess(final FRGItem it) {
                                            if(it.quantity <= 0){
                                                AlertDialog.Builder builder = new AlertDialog.Builder(popContext);

                                                DialogInterface.OnClickListener dialogClickListener2 = new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        switch (which){
                                                            case DialogInterface.BUTTON_POSITIVE:
                                                                MainActivity.this.addToList(it.upc);
                                                                Intent intent = new Intent();
                                                                intent.setAction("net.magmastone.NewItem");
                                                                sendBroadcast(intent);
                                                                break;

                                                            case DialogInterface.BUTTON_NEGATIVE:

                                                                break;
                                                        }
                                                    }
                                                };
                                                builder.setMessage("You used the last one!").setPositiveButton("Add to list", dialogClickListener2)
                                                        .setNegativeButton("Ignore", dialogClickListener2).show();
                                            }
                                        }

                                        @Override
                                        public void NetworkFailed(Exception e) {
                                            MainActivity.this.nFailed(e);
                                        }
                                    }).execute(it.upc,String.valueOf(-1));
                                }

                                @Override
                                public void NetworkFailed(Exception e) {
                                    MainActivity.this.nFailed(e);
                                }
                            }).execute(it.upc);

                        }
                       }
                       @Override
                       public void NetworkFailed(Exception e){
                           MainActivity.this.nFailed(e);
                       }
                   }).execute(upc);
               } else {
                   Context context = this.getApplicationContext();
                   CharSequence text = "Scan failed! Try again!";
                   int duration = Toast.LENGTH_SHORT;

                   Toast toast = Toast.makeText(context, text, duration);
                   toast.show();
               }
           }
        if(requestCode == newItemRcode){
            if(resultCode==RESULT_OK) {
                Log.d("ReturningResult", "AllGood");
                String itemName = intent.getStringExtra("name");
                String itemType = intent.getStringExtra("type");
                String itemUPC = intent.getStringExtra("upc");
                String itemImage = intent.getStringExtra("image");
                String itemExpiry = intent.getStringExtra("expiry");
                final Activity ourActivity = this;

                new NewUPCTask(new NewUPCTask.NetworkResults(){

                        @Override
                        public void NetworkSuccess(UPCItem it){
                            Intent myIntent = new Intent(ourActivity, CheckinItemActivity.class);
                            myIntent.putExtra("UPC", it.upc);
                            Log.d("upctest",it.upc);
                            ourActivity.startActivity(myIntent);
                        }

                        @Override
                        public void NetworkFailed(Exception e){
                            MainActivity.this.nFailed(e);
                        }
                }).execute(itemUPC, itemName, itemType, itemImage, itemExpiry);


                Log.d("ReturnedItem", itemName + " " + itemType + " " + itemUPC + " " + itemExpiry);
            }
        }

         }
    public void addToList(String upc){
        new AddShoppingItemTask(new AddShoppingItemTask.NetworkResults() {
            @Override
            public void NetworkSuccess() {

            }

            @Override
            public void NetworkFailed(Exception e) {
                MainActivity.this.nFailed(e);
            }
        }).execute(upc);
    }
    public void nFailed(Exception e){
        String failureReason = e.getClass().getName();
        if(failureReason.equals("retrofit.RetrofitError")){
            Toast.makeText(this,"Network Error. Try connecting to WiFi!",Toast.LENGTH_SHORT).show();

        }else{
            Toast.makeText(this,e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void goScan(){

        new IntentIntegrator(this).initiateScan();

    }
    public void onFragmentInteraction(String id){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(id));
        startActivity(browserIntent);
    }
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if(position==0) {
                return ScannerTab.newInstance();
            }else if (position==2) {
                return FridgeFragment.newInstance("a", "b");
            }else if(position==1){
                return RecFragment.newInstance();
            }else if (position==3) {
                return ShoppingFragment.newInstance();
            }else{
                return PlaceholderFragment.newInstance(position);
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
                case 3:
                    return getString(R.string.shoppingString).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

}
