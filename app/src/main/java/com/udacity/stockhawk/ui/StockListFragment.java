package com.udacity.stockhawk.ui;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static com.udacity.stockhawk.data.NetworkUtils.isNetworkAvailable;

/**
 * Created by thib146 on 23/03/2017.
 */

public class StockListFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        StockAdapter.StockAdapterOnClickHandler {

    private static final int STOCK_LOADER = 0;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.recycler_view) RecyclerView stockRecyclerView;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.error) TextView error;
    private StockAdapter adapter;

    public static boolean stockRemoved = false;

    private Cursor mCursor;

    public interface OnRefresherPass {
        void onRefresherPass(SwipeRefreshLayout swipeRefreshLayout);
    }
    OnRefresherPass RefresherPass;

    public interface OnRemoveStock {
        void onRemoveStock(String symbol);
    }
    OnRemoveStock StockRemoved;

    // Boolean to store the internet connection status
    private static boolean mConnected = false;

    @Override
    public void onClick(String symbol) {
        Timber.d("Symbol clicked: %s", symbol);

        int orientation = getResources().getConfiguration().orientation;
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);

        // On Landscape mode, check if a stock was already selected
        if (orientation == ORIENTATION_LANDSCAPE || isTablet) {
            Bundle arguments = new Bundle();
            arguments.putString(DetailFragment.ARG_ITEM_ID, symbol);
            DetailFragment detailFragment = new DetailFragment();
            detailFragment.setArguments(arguments);
            getActivity().getFragmentManager().beginTransaction()
                    .replace(R.id.stock_detail_fragment, detailFragment).commit();
        } else {
            Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
            detailIntent.putExtra(Intent.EXTRA_TEXT, symbol);
            startActivity(detailIntent);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //////// Set the device language to Arabic for tests ///////
//        Locale locale = new Locale("ar", "EG");
//        Resources resources = getResources();
//        Configuration configuration = resources.getConfiguration();
//        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
//        configuration.setLocale(locale);
//        resources.updateConfiguration(configuration,displayMetrics);

        View view = inflater.inflate(R.layout.fragment_stock_list, container, false);
        ButterKnife.bind(this, view);

        adapter = new StockAdapter(getActivity(), this);
        stockRecyclerView.setAdapter(adapter);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);

        onRefresh();

        QuoteSyncJob.initialize(getActivity());

        if (!mConnected) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_network));
            error.setVisibility(View.VISIBLE);
        }

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String symbol = adapter.getSymbolAtPosition(viewHolder.getAdapterPosition());

                // Send the symbol String to the main activity for removal
                StockRemoved.onRemoveStock(symbol);
            }
        }).attachToRecyclerView(stockRecyclerView);

        // Pass the Refresher to the Main Activity for the Add Stock function
        RefresherPass.onRefresherPass(swipeRefreshLayout);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getActivity().getSupportLoaderManager().initLoader(STOCK_LOADER, null, StockListFragment.this);
    }

    /**
     * This method will change mConnected according to the internet connection
     */
    private static Handler connectionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what != 1) { // If not connected
                mConnected = false;
            } else { // If connected
                mConnected = true;
            }
        }
    };

    @Override
    public void onRefresh() {

        // Checks the internet connexion
        isNetworkAvailable(connectionHandler, 5000);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String initializedKey = getActivity().getString(R.string.pref_stocks_initialized_key);
        boolean initialized = prefs.getBoolean(initializedKey, false);

        QuoteSyncJob.syncImmediately(getActivity());

        if (!mConnected && adapter.getItemCount() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_network));
            error.setVisibility(View.VISIBLE);
        } else if (!mConnected) {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), R.string.toast_no_connectivity, Toast.LENGTH_LONG).show();
        } else if (PrefUtils.getStocks(getActivity()).size() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_stocks));
            error.setVisibility(View.VISIBLE);
        } else {
            error.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity;
        if (context instanceof Activity) {
            activity = (Activity) context; // Get the main activity when the fragment is attached
            this.RefresherPass = (OnRefresherPass) activity; // Pass the refresher
            this.StockRemoved = (OnRemoveStock) activity;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                null, null, Contract.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);

        if (data.getCount() != 0) {
            error.setVisibility(View.GONE);
        }
        adapter.setCursor(data);
        mCursor = data;
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        adapter.setCursor(null);
    }


    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (PrefUtils.getDisplayMode(getActivity())
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_percentage);
        } else {
            item.setIcon(R.drawable.ic_dollar);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            PrefUtils.toggleDisplayMode(getActivity());
            setDisplayModeMenuItemIcon(item);
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCursor != null) {
            //mCursor.close();
        }
    }

}