package com.udacity.stockhawk.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.common.collect.Lists;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.DayAxisValueFormatter;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import org.w3c.dom.Text;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

/**
 * Created by thib146 on 08/03/2017.
 */

public class DetailFragment extends Fragment implements
        StockAdapter.StockAdapterOnClickHandler {

    @BindView(R.id.stock_title) TextView mStockTitle;
    @BindView(R.id.stock_price) TextView mStockPrice;
    @BindView(R.id.stock_change_price) TextView mStockPriceChange;
    @BindView(R.id.stock_change_percentage) TextView mStockPercentageChange;
    @BindView(R.id.no_stock_text) TextView mNoStockTextView;
    @BindView(R.id.stock_chart) LineChart mLineChart;
    @BindView(R.id.curves_tabs) TabLayout mTabLayout;

    // Used to pass the name of the Stock for the window title
    public interface OnStockNamePass {
        public void onStockNamePass(String stockName);
    }

    OnStockNamePass stockNamePass;

    private LineDataSet mDataSet;

    private String mSymbol;

    private Cursor mCursor;

    private static int COLUMN_HISTORY_INDEX = 4;

    // Key used to get the quote ID through the fragment creation
    public static final String ARG_ITEM_ID = "item_id";

    // Columns of data we want to display in the Detailed view
    public static final String[] QUOTE_PROJECTION = {
            Contract.Quote.COLUMN_SYMBOL,
            Contract.Quote.COLUMN_PRICE,
            Contract.Quote.COLUMN_ABSOLUTE_CHANGE,
            Contract.Quote.COLUMN_PERCENTAGE_CHANGE,
            Contract.Quote.COLUMN_HISTORY,
            Contract.Quote.COLUMN_COMPANY
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_stock_detail, container, false);

        ButterKnife.bind(this, view);

        // Get the intent that started the activity
        Intent intentThatStartedThatActivity = getActivity().getIntent();
        Bundle bundle = getArguments();

        if (bundle != null) { // Two-pane mode stock selection
            mSymbol = bundle.getString(DetailFragment.ARG_ITEM_ID);
        } else {
            mSymbol = intentThatStartedThatActivity.getStringExtra(Intent.EXTRA_TEXT);
        }

        // Pass the symbol to a global variable
        MainActivity.mSymbol = mSymbol;

        // Set the symbol text view
        mStockTitle.setText(mSymbol);

        // Get the History Data from our Content Provider
        String[] mSelectionArgs = {""};
        mSelectionArgs[0] = mSymbol;
        if (mSymbol != null) { // If a stock has been selected, get the data from the Content Resolver
            mCursor = getActivity().getContentResolver().query(
                    Contract.Quote.URI,
                    QUOTE_PROJECTION,
                    Contract.Quote.COLUMN_SYMBOL + " = ?",
                    mSelectionArgs,
                    null);
        } else { // If mSymbol is null, it means no Stock has been selected, so a screen must be displayed accordingly
            mStockTitle.setVisibility(View.GONE);
            mStockPrice.setVisibility(View.GONE);
            mStockPriceChange.setVisibility(View.GONE);
            mStockPercentageChange.setVisibility(View.GONE);
            mLineChart.setVisibility(View.GONE);
            mTabLayout.setVisibility(View.GONE);
            mNoStockTextView.setVisibility(View.VISIBLE);
            return view;
        }

        // If we're here, it means a stock has been selected, so we can hide the 'no stock' message
        mNoStockTextView.setVisibility(View.GONE);

        // Get all the data in the database corresponding to our current stock
        mCursor.moveToFirst();
        int indexHist = mCursor.getColumnIndex(Contract.Quote.COLUMN_HISTORY);
        String stringHistory = mCursor.getString(indexHist);

        int indexCompany = mCursor.getColumnIndex(Contract.Quote.COLUMN_COMPANY);
        String CompanyName = mCursor.getString(indexCompany);

        int indexPrice = mCursor.getColumnIndex(Contract.Quote.COLUMN_PRICE);
        String stockPrice = "$" + mCursor.getString(indexPrice);

        int indexPriceChange = mCursor.getColumnIndex(Contract.Quote.COLUMN_ABSOLUTE_CHANGE);
        float stockPriceChangeFloat = mCursor.getFloat(indexPriceChange);

        // Color and set the price change text depending on its positivity/negativity
        String stockPriceChange;
        if (stockPriceChangeFloat > 0) {
            stockPriceChange = "\u200E+" + mCursor.getString(indexPriceChange); // use \u200E caracter to keep the negative sign even in RTL mode
            mStockPriceChange.setText(stockPriceChange);
            mStockPriceChange.setTextColor(getResources().getColor(R.color.material_green_700));
        } else {
            stockPriceChange = "\u200E" + mCursor.getString(indexPriceChange); // use \u200E caracter to keep the negative sign even in RTL mode
            mStockPriceChange.setText(stockPriceChange);
            mStockPriceChange.setTextColor(getResources().getColor(R.color.material_red_700));
        }

        // Color and set the percentage change text depending on its positivity/negativity
        int indexPercentageChange = mCursor.getColumnIndex(Contract.Quote.COLUMN_PERCENTAGE_CHANGE);
        float stockPercentageChangeFloat = mCursor.getFloat(indexPercentageChange);
        String stockPercentageChange;
        if (stockPercentageChangeFloat > 0) {
            stockPercentageChange = "\u200E(+" + mCursor.getString(indexPercentageChange) + "%)"; // use \u200E caracter to keep the negative sign even in RTL mode
            mStockPercentageChange.setText(stockPercentageChange);
            mStockPercentageChange.setTextColor(getResources().getColor(R.color.material_green_700));
        } else {
            stockPercentageChange = "\u200E(" + mCursor.getString(indexPercentageChange) + "%)"; // use \u200E caracter to keep the negative sign even in RTL mode
            mStockPercentageChange.setText(stockPercentageChange);
            mStockPercentageChange.setTextColor(getResources().getColor(R.color.material_red_700));
        }

        // Set the price text view
        mStockPrice.setText(stockPrice);

        // Pass the company name to the activity to use it as a title in One Activity Mode
        stockNamePass.onStockNamePass(CompanyName);

        // Instantiate all the historical data array lists that we'll need for the graphs
        List<Entry> entriesHist = new ArrayList<Entry>();
        List<Entry> entriesOneDay = new ArrayList<Entry>();
        List<Entry> entriesFiveDay = new ArrayList<Entry>();
        List<Entry> entriesThreeMonth = new ArrayList<Entry>();
        List<Entry> entriesSixMonth = new ArrayList<Entry>();
        List<Entry> entriesOneYear = new ArrayList<Entry>();
        List<Entry> entriesFiveYear = new ArrayList<Entry>();

        // Transform the main historic data into a usable Entry List (for MPChart)
        Pattern pattern = Pattern.compile("(\\d{1,14})\\, (\\d{1,5})\\.(\\d{1,7})\\n"); // Pattern used to target each date in the data
        Matcher matcher = pattern.matcher(stringHistory);

        // Handle each piece of date data and store it in entriesHist
        while(matcher.find()){
            String strCoordX = matcher.group(1);
            String strCoordY = matcher.group(2) + "." + matcher.group(3);

            float CoordX = Float.parseFloat(strCoordX);
            float CoordY = Float.parseFloat(strCoordY);

            entriesHist.add(new Entry(CoordX, CoordY));
        }

        // *** Divide the main historic data into several specific periods ***

        // Get the current date
        Calendar clCurrent = Calendar.getInstance();
        int currentDay = clCurrent.get(Calendar.DAY_OF_MONTH);
        int currentMonth = clCurrent.get(Calendar.MONTH)+1;
        int currentYear = clCurrent.get(Calendar.YEAR);

        // Booleans used to know when each time period has been created
        boolean isOneDayListCreated = false,
                isFiveDayListCreated = false,
                isThreeMonthListCreated = false,
                isSixMonthListCreated = false,
                isOneYearListCreated = false,
                isFiveYearListCreated = false;

        // Creation of the different periods one by one
        for (int i=0; i<entriesHist.size(); i++) {
            // Get the date
            long date = (long) entriesHist.get(i).getX();

            // Set it in milliseconds
            Calendar clCurve = Calendar.getInstance();
            clCurve.setTimeInMillis(date);

            // Get the day, month and year out of it
            int curveDay = clCurve.get(Calendar.DAY_OF_MONTH);
            int curveMonth = clCurve.get(Calendar.MONTH)+1;
            int curveYear = clCurve.get(Calendar.YEAR);

            // Compare these days/months/years with the current time to make a substring of the main data every time a period is reached
            // When 1 day is reached
            if (curveDay == (currentDay - 1) && curveMonth == currentMonth && curveYear == currentYear && !isOneDayListCreated) {
                entriesOneDay = entriesHist.subList(0, i);
                isOneDayListCreated = true;
            // When 5 days are reached
            } else if (curveDay == (currentDay - 4) && (curveMonth == currentMonth || curveMonth == currentMonth-1)
                    && curveYear == currentYear && !isFiveDayListCreated) {
                entriesFiveDay = entriesHist.subList(0, i);
                isFiveDayListCreated = true;
            // Whean 3 months are reached
            } else if ((curveMonth == (currentMonth - 3) || curveMonth == (currentMonth-3+12))
                    && (curveYear == currentYear || curveYear == currentYear-1) && !isThreeMonthListCreated) {
                entriesThreeMonth = entriesHist.subList(0, i);
                isThreeMonthListCreated = true;
            // When 6 months are reached
            } else if ((curveMonth == (currentMonth - 6)  || curveMonth == (currentMonth-6+12))
                    && (curveYear == currentYear || curveYear == currentYear-1) && !isSixMonthListCreated) {
                entriesSixMonth = entriesHist.subList(0, i);
                isSixMonthListCreated = true;
            // When 1 year is reached
            } else if (curveYear == (currentYear - 1) && curveMonth == currentMonth && !isOneYearListCreated) {
                entriesOneYear = entriesHist.subList(0, i);
                isOneYearListCreated = true;
            // When 5 years are reached
            } else if (curveYear == (currentYear - 5) && !isFiveYearListCreated) {
                entriesFiveYear = entriesHist.subList(0, i);
                isFiveYearListCreated = true;
            }
        }

        // Reverse all the data for it to be readable by MPChart
        final List<Entry> entriesHistReverse = Lists.reverse(entriesHist);
        final List<Entry> entriesOneDayReverse = Lists.reverse(entriesOneDay);
        final List<Entry> entriesFiveDayReverse = Lists.reverse(entriesFiveDay);
        final List<Entry> entriesThreeMonthReverse = Lists.reverse(entriesThreeMonth);
        final List<Entry> entriesSixMonthReverse = Lists.reverse(entriesSixMonth);
        final List<Entry> entriesOneYearReverse = Lists.reverse(entriesOneYear);
        final List<Entry> entriesFiveYearReverse = Lists.reverse(entriesFiveYear);

        int tabPosition;
        boolean tabAlreadySelected = false;

        // Automatically select the first curve with data (the 1 day period is not working with this API)
        if (entriesOneDayReverse.size() != 0) {
            tabPosition = 0;
            mDataSet = new LineDataSet(entriesOneDayReverse, null);
        } else if (entriesFiveDayReverse.size() != 0) {
            tabPosition = 1;
            mDataSet = new LineDataSet(entriesFiveDayReverse, null);
        } else if (entriesThreeMonthReverse.size() != 0) {
            tabPosition = 2;
            mDataSet = new LineDataSet(entriesThreeMonthReverse, null);
        } else if (entriesSixMonthReverse.size() != 0) {
            tabPosition = 3;
            mDataSet = new LineDataSet(entriesSixMonthReverse, null);
        } else if (entriesOneYearReverse.size() != 0) {
            tabPosition = 4;
            mDataSet = new LineDataSet(entriesOneYearReverse, null);
        } else if (entriesFiveYearReverse.size() != 0) {
            tabPosition = 5;
            mDataSet = new LineDataSet(entriesFiveYearReverse, null);
        } else { // If all the periods are null, we display the whole historic data in the 1 day tab
            tabPosition = 0;
            mDataSet = new LineDataSet(entriesHistReverse, null);
        }

        // TAB LAYOUT LOGIC
        TabLayout.Tab tab = mTabLayout.getTabAt(tabPosition);
        tab.select();
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int tabPosition = tab.getPosition();
                switch (tabPosition) {
                    case 0: // 1D Tab
                        // If all the periods are null, we display the whole historical data in the tab 1D
                        if (entriesOneDayReverse.size() == 0 && entriesFiveDayReverse.size() == 0
                                && entriesThreeMonthReverse.size() == 0 && entriesSixMonthReverse.size() == 0
                                && entriesOneYearReverse.size() == 0 && entriesFiveYearReverse.size() == 0) {
                            // In case everything is null, draw the max data in the 1D tab
                            mDataSet = new LineDataSet(entriesHistReverse, "Price");
                            drawGraph(mLineChart, mDataSet);
                        } else if (entriesOneDayReverse.size() == 0) { // If there's no data, clear the chart
                            mLineChart.clear();
                        } else { // When there is data, draw the chart
                            mDataSet = new LineDataSet(entriesOneDayReverse, "Price");
                            drawGraph(mLineChart, mDataSet);
                        }
                        break;
                    case 1: // 5D Tab
                        if (entriesFiveDayReverse.size() == 0) {
                            mLineChart.clear();
                        } else {
                            mDataSet = new LineDataSet(entriesFiveDayReverse, "Price");
                            drawGraph(mLineChart, mDataSet);
                        }
                        break;
                    case 2: // 3M Tab
                        if (entriesThreeMonthReverse.size() == 0) {
                            mLineChart.clear();
                        } else {
                            mDataSet = new LineDataSet(entriesThreeMonthReverse, "Price");
                            drawGraph(mLineChart, mDataSet);
                        }
                        break;
                    case 3: // 6M Tab
                        if (entriesSixMonthReverse.size() == 0) {
                            mLineChart.clear();
                        } else {
                            mDataSet = new LineDataSet(entriesSixMonthReverse, "Price");
                            drawGraph(mLineChart, mDataSet);
                        }
                        break;
                    case 4: // 1Y Tab
                        if (entriesOneYearReverse.size() == 0) {
                            mLineChart.clear();
                        } else {
                            mDataSet = new LineDataSet(entriesOneYearReverse, "Price");
                            drawGraph(mLineChart, mDataSet);
                        }
                        break;
                    case 5: // 5Y Tab
                        if (entriesFiveYearReverse.size() == 0) {
                            mLineChart.clear();
                        } else {
                            mDataSet = new LineDataSet(entriesFiveYearReverse, "Price");
                            drawGraph(mLineChart, mDataSet);
                        }
                        break;
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }
        });

        // Draw the chart as soon as the detail view gets displayed
        drawGraph(mLineChart, mDataSet);
        return view;
    }

//    @Override
//    public void onRemoveStock(Boolean stockRemoved) {
//        if (stockRemoved) {
//            mStockTitle.setVisibility(View.GONE);
//            mStockPrice.setVisibility(View.GONE);
//            mStockPriceChange.setVisibility(View.GONE);
//            mStockPercentageChange.setVisibility(View.GONE);
//            mLineChart.setVisibility(View.GONE);
//            mTabLayout.setVisibility(View.GONE);
//            mNoStockTextView.setVisibility(View.VISIBLE);
//            mSymbol = null;
//            StockListFragment.stockRemoved = false;
//        }
//    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity;
        Fragment mainFragment;
        if (context instanceof Activity) { // If we're in One Activity Mode
            activity = (Activity) context; // Get the main activity when the fragment is attached
            this.stockNamePass = (OnStockNamePass) activity; // Pass the company name to use it for the detail activity window title
        } else { // If we're in two-pane mode
            mainFragment = getFragmentManager().findFragmentById(R.id.stock_list_fragment);
            this.stockNamePass = (OnStockNamePass) mainFragment;
        }
    }

    /**
     * This function draws the chart with the MPChart plugin
     *
     * @param lineChart the chart object
     * @param dataSet the data to be drawn
     */
    public void drawGraph (LineChart lineChart, LineDataSet dataSet) {

        // Curve color and fill color
        dataSet.setColor(Color.GRAY);
        dataSet.setFillColor(Color.GREEN);
        dataSet.setFillAlpha(100);
        dataSet.setDrawFilled(true);
        dataSet.setDrawCircles(false);

        LineData lineData = new LineData(dataSet);

        // Formatting of the dates so that they appear in "May 16" style
        IAxisValueFormatter xAxisFormatter = new DayAxisValueFormatter(lineChart);

        // X Axis options
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(true);
        xAxis.setValueFormatter(xAxisFormatter);

        // Left axis disabled
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setEnabled(false);

        // Right axis options
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        rightAxis.setTextSize(10f);
        rightAxis.setTextColor(Color.WHITE);
        rightAxis.setDrawAxisLine(true);
        rightAxis.setDrawGridLines(true);

        // General chart options
        lineChart.setData(lineData);
        lineChart.setTouchEnabled(false);
        lineChart.setDescription(null);
        lineChart.setDrawBorders(true);
        lineChart.setBorderColor(Color.WHITE);
        lineChart.setBorderWidth(1);
        lineChart.setMaxVisibleValueCount(3);
        lineChart.getLegend().setEnabled(false);
        lineChart.invalidate(); // refresh
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onClick(String itemId) {

    }
}