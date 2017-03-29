package com.udacity.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static com.udacity.stockhawk.data.NetworkUtils.isNetworkAvailable;

public class MainActivity extends AppCompatActivity implements
        StockListFragment.OnRefresherPass,
        DetailFragment.OnStockNamePass,
        StockListFragment.OnRemoveStock {

    SwipeRefreshLayout mSwipeRefreshLayout;

    public static String mSymbol;
    public static String mStockName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the device language to Arabic for tests
//        Locale locale = new Locale("ar", "EG");
//        Resources resources = getResources();
//        Configuration configuration = resources.getConfiguration();
//        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
//        configuration.setLocale(locale);
//        resources.updateConfiguration(configuration,displayMetrics);

        // Get the device's orientation and Tablet mode
        int orientation = getResources().getConfiguration().orientation;
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);

        // On Landscape mode or on Tablets, check if a stock was already selected
        if (orientation == ORIENTATION_LANDSCAPE || isTablet) {
            if (mSymbol == null) { // If no stock was selected, create a new empty stock detail Fragment, with a "no content" message
                DetailFragment detailFragment = new DetailFragment();
                getFragmentManager().beginTransaction()
                        .add(R.id.stock_detail_fragment, detailFragment).commit();
            } else { // If a stock was previously selected, get its symbol and create a Fragment with it in its arguments
                Bundle arguments = new Bundle();
                arguments.putString(DetailFragment.ARG_ITEM_ID, mSymbol);
                DetailFragment detailFragment = new DetailFragment();
                detailFragment.setArguments(arguments);
                getFragmentManager().beginTransaction()
                        .replace(R.id.stock_detail_fragment, detailFragment).commit();
            }
        }
    }

    /**
     * This function gets any new intent on the main activity. Works on first launch, but also on the onResume stage.
     * This is used to get the intent from the widget when the app is in Two-Pane mode
     *
     * @param intent the new intent received by the widget
     */
    @Override
    protected void onNewIntent(Intent intent) {
        if (intent != null) {
            setIntent(intent); // Set the activity intent if there is one
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get the intent coming from the widget
        Intent intent = getIntent();

        // Get the symbol in its Extra string
        mSymbol = intent.getStringExtra(Intent.EXTRA_TEXT);

        // If the symbol isn't null, create the fragment accordingly
        if (mSymbol != null) {
            Bundle arguments = new Bundle();
            arguments.putString(DetailFragment.ARG_ITEM_ID, mSymbol);
            DetailFragment detailFragment = new DetailFragment();
            detailFragment.setArguments(arguments);
            getFragmentManager().beginTransaction().replace(R.id.stock_detail_fragment, detailFragment).commit();
        }
    }

    // Receive the RefresherLayout from the fragment for the AddStock function
    @Override
    public void onRefresherPass(SwipeRefreshLayout swipeRefreshLayout) {
        mSwipeRefreshLayout = swipeRefreshLayout;
    }

    @Override
    public void onRemoveStock(String symbol) {
        int orientation = getResources().getConfiguration().orientation;
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);

        // Use the removeStock function to delete the item
        removeStock(symbol);

        if (mSymbol != null) {
            // If we're in Two-Pane Mode and the detailed view is currently showing the item we're deleting : clear the detail fragment
            if ((isTablet || orientation == ORIENTATION_LANDSCAPE) && mSymbol.equals(symbol)) {
                Bundle arguments = new Bundle();
                arguments.putString(DetailFragment.ARG_ITEM_ID, null);
                DetailFragment detailFragment = new DetailFragment();
                detailFragment.setArguments(arguments);
                getFragmentManager().beginTransaction().replace(R.id.stock_detail_fragment, detailFragment).commit();
            }
        }
    }

    // Receive the Stock name
    @Override
    public void onStockNamePass(String stockName) {
        mStockName = stockName;
    }

    // Check if the device is connected to the internet
    private boolean networkUp() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    // Handle the "Add" button
    public void button(@SuppressWarnings("UnusedParameters") View view) {
        new AddStockDialog().show(this.getFragmentManager(), "StockDialogFragment");
    }

    // Add a stock when the symbol is
    void addStock(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {

            if (networkUp()) {
                mSwipeRefreshLayout.setRefreshing(true);
            } else {
                String message = getResources().getString(R.string.toast_stock_added_no_connectivity);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }

            PrefUtils.removeStock(this, "GOOG");
            PrefUtils.addStock(this, symbol);
            QuoteSyncJob.syncImmediately(this);
        }
    }

    // Remove a stock
    public void removeStock(String symbol) {
        String selection = Contract.Quote.COLUMN_SYMBOL + "=?";
        String[] mSelectionArgs = {""};
        mSelectionArgs[0] = symbol;

        PrefUtils.removeStock(this, symbol);
        getContentResolver().delete(Contract.Quote.URI, selection, mSelectionArgs);
    }
}