package com.udacity.stockhawk.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import java.io.IOException;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;


public class AddStockDialog extends DialogFragment {

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.dialog_stock)
    EditText stock;

    private Activity mActivity;
    private Fragment mMainFragment;

    private String mSymbol;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View custom = inflater.inflate(R.layout.add_stock_dialog, null);

        ButterKnife.bind(this, custom);

        stock.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                addStock();
                return true;
            }
        });
        builder.setView(custom);

        builder.setMessage(getString(R.string.dialog_title));
        builder.setPositiveButton(getString(R.string.dialog_add),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mSymbol = stock.getText().toString();
                        new checkIfSymbolisValid().execute(mSymbol);
                    }
                });
        builder.setNegativeButton(getString(R.string.dialog_cancel), null);

        Dialog dialog = builder.create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity) {
            mActivity = (Activity) context; // Store the main activity in mActivity for the Add Stock function to work properly
        }
    }

    // Add a stock to the main Stock List activity
    private void addStock() {
        ((MainActivity) mActivity).addStock(stock.getText().toString());
        dismissAllowingStateLoss();
    }

    /**
     * This class checks in an async task if the symbol entered by the user is valid by using the API function "isValid()"
     */
    private class checkIfSymbolisValid extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... params) {

            // By default, the stock is invalid
            boolean isValid = false;

            try {
                Stock queryStock = YahooFinance.get(mSymbol); // Get the quote from Yahoo
                isValid = queryStock.isValid(); // Check if it is valid
            } catch (IOException exception) {
                Timber.e(exception, "Error getting stock quote");
            }
            return isValid;
        }

        @Override
        protected void onPostExecute(Boolean isValid) {
            if (isValid) { // If the stock is valid, add it to the list
                addStock();
            } else { // If the stock is invalid, display an error message in a Toast
                Toast.makeText(mActivity, R.string.toast_symbol_unknown, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
