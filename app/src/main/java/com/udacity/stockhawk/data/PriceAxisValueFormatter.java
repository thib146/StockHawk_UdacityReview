package com.udacity.stockhawk.data;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

/**
 * Created by thib146 on 29/03/2017.
 */

public class PriceAxisValueFormatter implements IAxisValueFormatter {

    private BarLineChartBase<?> chart;

    public PriceAxisValueFormatter(BarLineChartBase<?> chart) {
        this.chart = chart;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return "$" + String.format("%d",  (long) value);
    }
}