package com.example.nteplitskiy.glucometerapp;

import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.helper.GraphViewXML;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class GraphActivity extends AppCompatActivity implements Button.OnClickListener {

    private GraphView graph;
    private JSONArray jsonArray;
    private int num = 0;
    private Button fromDate;
    private Button toDate;

    private long defaultFrom;
    private long defaultTo;

    private Calendar from;
    private Calendar to;

    String jsonFile;
    SimpleDateFormat dateFormat;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    Intent intent = new Intent(getBaseContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                case R.id.navigation_preferences:
                    Intent intent2 = new Intent(getBaseContext(), ConfigActivity.class);
                    startActivity(intent2);
                    return true;
                case R.id.navigation_graph:
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        //navigation.getMenu().getItem(2).setChecked(true);

        graph = (GraphView) findViewById(R.id.graph);

        to = Calendar.getInstance();
        Log.d("what", "" + to.get(Calendar.YEAR));
        from = Calendar.getInstance();
        from.add(Calendar.MONTH, -1);

        dateFormat = new SimpleDateFormat("MM/dd/yy");


        toDate = findViewById(R.id.bt_to);
        toDate.setOnClickListener(this);
        toDate.setText("To: " + dateFormat.format(to.getTime()));
        defaultTo = to.getTimeInMillis();

        fromDate = findViewById(R.id.bt_from);
        fromDate.setOnClickListener(this);
        from.add(Calendar.MONTH, -1);
        fromDate.setText("From: " + dateFormat.format(from.getTime()));
        defaultFrom = from.getTimeInMillis();

        Intent intent = getIntent();
        jsonFile = intent.getExtras().getString("userData"); //how is config activity going to deal with this?
        //just pass the same stuff to config and pass it again?

        //jsonArray = intent.getExtras("json");

        makeArray(jsonFile);

        //fillTestValues();


        try {
            fillGraph(from.getTimeInMillis(), to.getTimeInMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeArray(jsonFile);
    }

    private void makeArray(String input) {
        Log.d("Data String", "" + input);
        try {
            jsonArray = new JSONArray(input);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private DataPoint[] loadValues(long from, long to) throws JSONException {
        //DataPoint values = new DataPoint[jsonArray.length()];
        //int num = 0;
        Log.d("loadValues", "is this thing on?");
        int startIndex = -1;
        int stopIndex = (jsonArray.length() - 1);
        JSONObject object;

        Log.d("jsonArrayLength", "" + jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            object = jsonArray.getJSONObject(i);
            if (object.getLong("time") < from) {
                Log.d("from", "continue");
                continue;
            }
            if (object.getLong("time") > to) {
                Log.d("to", "break");
                stopIndex = (i);
                break;
            }
            Log.d("parse", "" + i);
            //num++;
            if (startIndex == -1)
                startIndex = i;

        }

        DataPoint[] values = new DataPoint[stopIndex + 1 - startIndex];

        Log.d("Values", "startIndex " + startIndex + " stopIndex " + stopIndex);
        Log.d("jsonArray", "" + jsonArray.toString());
        Log.d("jsonArray object", "" + jsonArray.getJSONObject(0).toString());

        for (int i = startIndex, j = 0; i <= stopIndex; i++, j++) {

            Log.d("loadValues", "loading values " + i);

            object = jsonArray.getJSONObject(i);
            long time = object.getLong("time");
            int level = object.getInt("value");
            Date date = new Date(time);


            DataPoint point = new DataPoint(date, level);
            values[j] = point;

            Log.d("added values", "time " + point.getX() + " level " + point.getY());
        }
        return values;
    }

    private void fillGraph(long from, long to) throws JSONException {
        DataPoint[] myData = loadValues(from, to);

        //Log.d("data", "x " + myData[2].getX() + " y " + myData[2].getY());

        PointsGraphSeries<DataPoint> series = new PointsGraphSeries<>(myData);

        Log.d("series", "largest x" + series.getHighestValueX());
        Log.d("series", "smallest x" + series.getLowestValueX());
        Log.d("series", "largest y" + series.getHighestValueY());
        Log.d("series", "smallest y" + series.getLowestValueY());
        //maybe a line graph will look way better even if it is less correct


        PointsGraphSeries<DataPoint> endpoints = new PointsGraphSeries<>(new DataPoint[]{
                new DataPoint(new Date(from), 0),
                new DataPoint(new Date(to), 0),
                //new DataPoint(new Date(System.currentTimeMillis()), 100)
                //show current time
        });

        graph.removeAllSeries();
        graph.getGridLabelRenderer().reloadStyles();

        Log.d("times", "from " + from + "to " + to);

        graph.addSeries(endpoints);

        graph.addSeries(series);
        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this, DateFormat.getDateInstance(DateFormat.DATE_FIELD)));
        graph.getGridLabelRenderer().setNumHorizontalLabels(3);
        //graph.getGridLabelRenderer().setHorizontalAxisTitle("Date");
        Log.d("numlabels", "" + graph.getGridLabelRenderer().getNumHorizontalLabels());
        graph.getViewport().setMinY(50.0);
        graph.getViewport().setMaxY(300.0);
        graph.getViewport().setMinX(from);
        graph.getViewport().setMaxX(to);
        graph.getGridLabelRenderer().setHumanRounding(false);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);
    }

    private void displayDialog(final int id) {
        //DatePickerDialog.Builder builder = new DatePickerDialog.Builder(this, android.R.style.Theme_Holo_Dialog );
        DatePickerDialog.Builder builder = new DatePickerDialog.Builder(this);
        //LayoutInflater inflater = this.getLayoutInflater();
        //final View dialogView = inflater.inflate(R.layout.dialog_datepicker, null);
        final DatePicker picker = new DatePicker(this);

        //picker.init(2017, 11, 5, null);
        Log.d("aha", "" + from.get(Calendar.YEAR));
        if (id == 0) {
            picker.init(from.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH), null);
        } else {
            picker.init(to.get(Calendar.YEAR), to.get(Calendar.MONTH), to.get(Calendar.DAY_OF_MONTH), null);
        }

        picker.setSpinnersShown(true);
        picker.setCalendarViewShown(false);

        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    setCal(picker, id);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("positive button dialog", " " + picker.getDayOfMonth());
                //Log.d("positive button dialog", " " + picker.);
            }
        });

        //picker.setCalendarViewShown(false);

        builder.setView(picker);
        builder.show();
    }

    private void setCal(DatePicker picker, int id) throws JSONException {
        if (id == 0) {
            from.set(Calendar.YEAR, picker.getYear());
            from.set(Calendar.MONTH, picker.getMonth());
            from.set(Calendar.DAY_OF_MONTH, picker.getDayOfMonth());
            fromDate.setText("From: " + dateFormat.format(from.getTime()));
            fillGraph(from.getTimeInMillis(), to.getTimeInMillis());
        }
        if (id == 1) {
            to.set(Calendar.YEAR, picker.getYear());
            to.set(Calendar.MONTH, picker.getMonth());
            to.set(Calendar.DAY_OF_MONTH, picker.getDayOfMonth());
            toDate.setText("To: " + dateFormat.format(to.getTime()));
            fillGraph(from.getTimeInMillis(), to.getTimeInMillis());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_from:
                displayDialog(0);
                break;
            case R.id.bt_to:
                displayDialog(1);
                break;
            case R.id.dialog_ok:
                Log.d("dialog", "closed dialog");

        }
    }
}
