package com.udacity.stockhawk.data;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.util.Calendar;

/**
 * Created by thib146 on 19/03/2017.
 */

/**
 * This class sets the X Axis dates in a readable format
 */
public class DayAxisValueFormatter implements IAxisValueFormatter {
    protected String[] mMonths = new String[]{
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private BarLineChartBase<?> chart;

    public DayAxisValueFormatter(BarLineChartBase<?> chart) {
        this.chart = chart;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {

        // Convert the date value to long for the setTimeInMillis function
        long valueLg = (long) value;

        // Set the date in milliseconds
        Calendar cl = Calendar.getInstance();
        cl.setTimeInMillis(valueLg);

        // Get the month and the year out of it
        String monthName = mMonths[cl.get(Calendar.MONTH) % mMonths.length];
        String yearName = "" + (cl.get(Calendar.YEAR)-2000);

        if (chart.getVisibleXRange() > (long) 1500000000) { // For a long period of time, just display the month and year

            return monthName + " " + yearName;
        } else { // For a short period of time (within 1 year), display the day and month

            // Get the day out of the date
            int dayOfMonth = cl.get(Calendar.DAY_OF_MONTH);

            String appendix = "th";

            switch (dayOfMonth) {
                case 1:
                    appendix = "st";
                    break;
                case 2:
                    appendix = "nd";
                    break;
                case 3:
                    appendix = "rd";
                    break;
                case 21:
                    appendix = "st";
                    break;
                case 22:
                    appendix = "nd";
                    break;
                case 23:
                    appendix = "rd";
                    break;
                case 31:
                    appendix = "st";
                    break;
            }

            // Return the date with the right appendix depending on the day number
            return dayOfMonth == 0 ? "" : dayOfMonth + appendix + " " + monthName;
        }
    }

}
