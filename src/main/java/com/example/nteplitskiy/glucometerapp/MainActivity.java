//epublic static void ain string args  povid mains strndf
package com.example.nteplitskiy.glucometerapp;

import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import static android.hardware.Camera.*;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Button.OnClickListener {

    private SurfaceView cSurfaceView;
    private SurfaceHolder cSurfaceHolder;
    private Camera camera;
    private Boolean cPreviewRunning = false;
    private Button captureButton;
    private JSONArray jsonArray;
    private JSONArray calibrationArray;
    private int glucoseLevel;
    private SharedPreferences configs;

    final String USERDATA = "user.json";

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                //This is the bottom navigation bar
                //this code starts new activities by calling them with their intent
                case R.id.navigation_home:
                    return true;
                case R.id.navigation_preferences:
                    Intent intent = new Intent(getBaseContext(), ConfigActivity.class);
                    intent.putExtra("json", jsonArray.toString());
                    //pass jsonArray object to the preference activity
                    startActivity(intent);
                    return true;
                case R.id.navigation_graph:
                    //mTextMessage.setText(R.string.title_graph);
                    Intent intent2 = new Intent(getBaseContext(), GraphActivity.class);
                    intent2.putExtra("userData", jsonArray.toString());
                    intent2.putExtra("hslData", calibrationArray.toString());
                    startActivity(intent2);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //keeps the screen on

        captureButton = (Button) findViewById(R.id.bt_capture);
        captureButton.setOnClickListener(this);

        cSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        //camera preview stuff
        cSurfaceHolder = cSurfaceView.getHolder();
        cSurfaceHolder.addCallback(this);
        cSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //String jsonData = loadFile("data.json");
        String jsonData = loadFile(USERDATA);
        jsonArray = parseJson(jsonData);

        navigation.bringToFront(); //navigaion bar gets hidden by camera preview. Bring it forward.
        configs = PreferenceManager.getDefaultSharedPreferences(this);

        /*if (configs.getBoolean("raw_hsl", true)) {
            String calibData = loadFile("calib.json");
            calibrationArray = parseJson(calibData);
            if (calibrationArray.length() == 0) {

            }
            Log.d("calibrationArray", "Loaded calib array");
        }*/
        hslData(); //loads hsl values from assets file to global variable
        //unsightly hack
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera = open(CameraInfo.CAMERA_FACING_BACK);
        } catch (Exception e) {
            Log.d("camera view", "failed to open camera, falling back to manual entry");
            //manual entry was never implemented
            e.printStackTrace();
            manualEntry();
        }
    }

    private void manualEntry() {
        //Do popup asking for blood glucose level
    }

    private JSONArray parseJson(String jsonString) {
        //parse string to JSON array
        JSONArray array = null;
        try {
            array = new JSONArray(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return array;
    }

    private void appendJson(long time, int glucose, String note) throws JSONException {
        //adds new item to jsonArray
        JSONObject obj = new JSONObject();
        obj.put("time", time);
        obj.put("value", glucose);
        obj.put("note", note);
        jsonArray.put(obj);
    }

    private void appendCalibration(long time, double hsl, double value, int color) throws JSONException {
        //supposed to add calibration data to calibration array, I don't remember if this works or is used
        JSONObject obj = new JSONObject();
        obj.put("time", time);
        obj.put("hsl", hsl);
        obj.put("value", value);
        obj.put("color", color);
        Log.d("hsl", obj.toString());
        calibrationArray.put(obj);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        //responsible for showing the camera live on screen
        if (cPreviewRunning) {
            camera.stopPreview();
        }
        Camera.Parameters p = camera.getParameters();
        //Camera.Size previewSize = new Camera.Size(1920, 1080);

        //p.setPreviewSize(1920, 1080);
        camera.setDisplayOrientation(90); //camera view was rotated otherwise
        if (configs.getBoolean("flash_enable", true)) {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_ON); //set if flash runs, also runs autofocus?
        }
        Camera.Size size = getBestPreviewSize(w, h); //magic code to make the picture right
        p.setPreviewSize(size.width, size.height);
        camera.setParameters(p);

        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        camera.startPreview();
        cPreviewRunning = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        cPreviewRunning = false;
        camera.release();
    }

    private void takePicture() {
        camera.takePicture(null, null, new PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try {
                    Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);
                    Intent intent1 = getIntent();
                    Uri uri = getUri(rotateBitmap(image)); //rotate image 90 degrees and save
                    //saving gives the bitmap a uri which is necessary for the crop view
                    doCrop(uri);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Bitmap rotateBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Uri getUri(Bitmap bitmap) {
        String path = null;
        try {
            path = MediaStore.Images.Media.insertImage(this.getContentResolver(), bitmap, "TempOut", "Temporary output file for URI");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("path", "" + path);
        return Uri.parse(path);
        //return path;
    }

    private void doCrop(Uri bmpUri) {
        try {
            Intent intent = new Intent("com.android.camera.action.CROP");
            //built in android crop activity
            intent.setDataAndType(bmpUri, "image/*");
            intent.putExtra("crop", "true");

            //intent.putExtra("aspectX", 1);
            //intent.putExtra("aspectY", 1);
            //intent.putExtra("outputX", 1);
            //intent.putExtra("outputY", 1);
            intent.putExtra("return-data", true);

            startActivityForResult(intent, 1);
        } catch (ActivityNotFoundException e) {
            Log.d("crop", "rest in peace, no crop found");
            Toast toast = Toast.makeText(this, "Can't crop.", Toast.LENGTH_SHORT);
            toast.show();
            e.printStackTrace();
            //app crashes on physical phone when cancel is pressed
        }
    }

    private int cAverage(Bitmap bitmap) {
        Bitmap small = Bitmap.createScaledBitmap(bitmap, 1, 1, false); //scale bitmap to 1x1 size
        return small.getPixel(0, 0); //return color of 1x1 bitmap
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("help", "does this even run?");

        if (requestCode == 1) { //if the cropping view is called
            if (data != null) {
                Bundle extras = data.getExtras();
                Bitmap bitmap = extras.getParcelable("data"); //get cropped bitmap
                if (bitmap != null) {
                    int color = cAverage(bitmap);
                    Log.d("color", "raw " + color);
                    Log.d("color", "hex (" + Color.red(color) + ", " + Color.green(color) + ", " + Color.blue(color));

                    double hsl = getHSL(color);

                    if (configs.getBoolean("raw_hsl", true)) {
                        displayConfgDialog(hsl, color); //runs hsl value dialog
                    }
                    else {
                        displayDialog(hsl); //run glucose level dialog
                    }
                }
            }
        }
    }

    private double getHSL(int color) {
        double avg = (Color.red(color) + Color.blue(color) + Color.green(color)) / 3.0;
        return avg;
    }

    private void displayDialog(final double hsl) {

        final double level = getGlucose(hsl);

        //user can click off the screen and cancel the dialog?
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_glucose, null);
        builder.setView(dialogView);
        TextView dataOut = (TextView) dialogView.findViewById(R.id.dialog_value);
        final EditText userNote = (EditText) dialogView.findViewById(R.id.dialog_input);
            dataOut.setText("" + level);

        final AlertDialog alert = builder.create();
        alert.show();


        Button okButton = (Button) dialogView.findViewById(R.id.dialog_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    try {
                        appendJson(System.currentTimeMillis(), (int) level, userNote.getText().toString());
                        Log.d("jsonArr", "" + jsonArray.toString());
                        writeFile(USERDATA, jsonArray.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    alert.dismiss();
            }
        });
    }

    private void displayConfgDialog(final double hsl, final int color) {

        final double level = getGlucose(hsl);

        //user can click off the screen and cancel the dialog?
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_glucose, null);
        builder.setView(dialogView);
        TextView dataOut = (TextView) dialogView.findViewById(R.id.dialog_value);
        final EditText userNote = (EditText) dialogView.findViewById(R.id.dialog_input);

        TextView valueIs = (TextView) dialogView.findViewById(R.id.dialog_text);
        valueIs.setText("Raw HSL value is");
        dataOut.setText("" + hsl);
        userNote.setHint("mg/dL");
        userNote.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);


        final AlertDialog alert = builder.create();
        alert.show();

        Button okButton = (Button) dialogView.findViewById(R.id.dialog_ok);
        okButton.setText("add");
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String calibdata = userNote.getText().toString();
                Double num;
                if (!calibdata.isEmpty())
                    num = Double.parseDouble(calibdata);
                else
                    num = 0.0;

                try {
                    appendCalibration(System.currentTimeMillis(), hsl, num, color);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                writeFile("lsr.json", calibrationArray.toString());
                alert.dismiss();
            }
        });
    }


    private double getGlucose(double hsl) {
        double[] formula = LSRCalc(calibrationArray);
        //take average of 3 colors for lightness?
        double m = formula[0];
        double b = formula[1];
        double level = m*hsl+b;
        Log.d("formula", "" + (Double) formula[0] + " " + formula[1] + " " + hsl);
        Log.d("formula", "" + level);
        return (level-4)*18;
    }

    private Camera.Size getBestPreviewSize(int width, int height) {
        Camera.Size result = null;
        Camera.Parameters p = camera.getParameters();
        for (Camera.Size size : p.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_capture:
                takePicture();
                break;
        }
    }

    private String loadFile(String file) {
        File inFile = new File(getFilesDir() + File.separator + file);
        if (!inFile.exists()) {
            Log.d("loadFile", "File " + file + " non extant, returning [] and hoping for the best");
            return "[]";
        }

        String string = null;
        try {
            FileInputStream inStream = new FileInputStream(inFile);
            byte[] buffer = new byte[inStream.available()];
            inStream.read(buffer);
            inStream.close();
            string = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("json", "" + file + " data " + string);
        return string;
    }

    private void writeFile(String file, String data) {
        File outFile = new File(getFilesDir() + File.separator + data);
        if (!outFile.exists()) {
            try {
                Log.d("new file", "new file creating " + file);
                outFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File writeFile = new File(getFilesDir() + File.separator + file);
        Log.d("filepath", "" + writeFile.getPath().toString());
        Log.d("value", "" + data);
        try {
            //FileWriter writer = new FileWriter(getFilesDir().toString() + File.separator.toString() + )
            FileOutputStream writer = new FileOutputStream(writeFile, false);
            writer.write(data.getBytes());
            Log.d("writer", "" + writer.toString());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hslData() {
        BufferedReader reader = null;
        String data = "[]";
        try {
            reader = new BufferedReader(new InputStreamReader(getAssets().open("lsr.json")));
            data = reader.readLine();
            reader.close();
            } catch (IOException e1) {
            e1.printStackTrace();
        }

        calibrationArray = parseJson(data);
    }

    public static double[] LSRCalc(JSONArray dataArray)
    {
        {
            //magic code, comments available in the documentation
            int n = dataArray.length();

            double sumx = 0.0, sumy =0.0;

            for (int i = 0; i<n; i++) {
                try {
                    JSONObject obj = dataArray.getJSONObject(i);
                    sumx += obj.getDouble("hsl");
                    sumy += obj.getDouble("concentration");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            Log.d("sumx", "" + sumx);
            Log.d("sumy", "" + sumy);

            double xavg = sumx/n;
            double yavg = sumy/n;

            Log.d("avgx", "" + xavg);
            Log.d("avgy", "" + yavg);

            double stdxx=0.0,stdxy=0.0;

            for (int i = 0; i<n; i++) {
                try {
                    JSONObject obj = dataArray.getJSONObject(i);
                    //stdxx += (x[i]-xavg)*(x[i]-xavg);
                    stdxx += (obj.getDouble("hsl") - xavg) * (obj.getDouble("hsl") - xavg);
                    //stdxy += (y[i]-yavg)*(x[i]-xavg);
                    stdxy += (obj.getDouble("concentration") - yavg) * (obj.getDouble("hsl") - yavg);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            Log.d("stdxy", "" + stdxx);
            Log.d("stdxx", "" + stdxy);

            double temp = stdxy/stdxx;
            Log.d("temp", "" + temp);

            double[] lsr = new double[2];
            lsr[0] = temp;
            lsr[1] = yavg - lsr[0]*xavg;

            Log.d("lsr[0]", "" + lsr[0]);
            Log.d("lsr[1]", "" + lsr[1]);

            return lsr;
        }
    }
}
