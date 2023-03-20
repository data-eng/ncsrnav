package com.example.ncsrparkingnavigation;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private String choice = "";
    private RadioGroup radioGroup;
    private TextView resultText;
    private Button findSpot;
    private List<String> resultData;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AutoCompleteTextView acdropdown = findViewById(R.id.acDropdown);
        radioGroup = findViewById(R.id.constrGroup);
        resultText = findViewById(R.id.resultMsg);
        resultData = new ArrayList<>();
        acdropdown.setThreshold(1);
        findSpot = findViewById(R.id.mapButton);
        ArrayAdapter<CharSequence> acAdapter = ArrayAdapter.createFromResource(this, R.array.ncsr_locations, android.R.layout.select_dialog_item);
        acdropdown.setAdapter(acAdapter);
        acdropdown.setOnItemClickListener(this);
        acdropdown.setOnTouchListener((view, motionEvent) -> {
            acdropdown.showDropDown();
            return false;
        });
        acdropdown.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if(acdropdown.getText().toString().equals(""))
                    choice = "";
                else
                    findSpot.setEnabled(true);
            }
        });

        findSpot.setOnClickListener(view -> openActivity());
    }

    @SuppressLint("StaticFieldLeak")
    public void openActivity()
    {
//        Intent intent = new Intent(this, MapActivity.class);
//        startActivity(intent);
        List<String> ncsr_locations = Arrays.asList(getResources().getStringArray(R.array.ncsr_locations));
        if (ncsr_locations.contains(choice)) {
            resultText.setBackgroundColor(Color.parseColor("#abf5a7"));
            int selectedId = radioGroup.getCheckedRadioButtonId();
            RadioButton constraintRadioBtn = findViewById(selectedId);

            if(selectedId == -1) {
                resultText.setBackgroundColor(Color.parseColor("#ffb976"));
                resultText.setText(getString(R.string.invalidConstraintMsg));
            }
            else {
                resultText.setBackgroundColor(Color.parseColor("#abf5a7"));
                //data fetching
                new AsyncTask<String, Void, String>(){
                    @Override
                    protected void onPreExecute(){
                        super.onPreExecute();
                        resultText.setVisibility(View.INVISIBLE);
                        findSpot.setText(getString(R.string.loadingBtn));
                    }
                    @Override
                    protected String doInBackground(String... params) {
                        HttpURLConnection urlConnection = null;

                        String data = "from(bucket: \"gigacampus2-parking\") " +
                                "|> range(start: -5m) " +
                                "|> filter(fn: (r) => r._measurement == \"parking_status\" and r._field == \"occupied\" and r._value == 0) " +
                                "|> group( columns: [\"id\"] ) |> sort( columns: [\"_time\"], desc: false ) " +
                                "|> last() |> keep( columns: [\"id\"] ) |> group()";

                        try {
                            URL url = new URL(params[0]);
                            urlConnection = (HttpURLConnection) url.openConnection();
                            urlConnection.setRequestMethod("POST");
                            urlConnection.setRequestProperty("Authorization", "Token OpZihwsUM-5IrinGcpH0CDD2cXP9tbFWikdP6kgZlRLXjySElZwLqn5mLfHfmoR6hVtKCF-XmmeMOB20OIe8-w==");
                            urlConnection.setRequestProperty("Accept", "application/csv");
                            urlConnection.setRequestProperty("Content-Type", "application/vnd.flux");
                            urlConnection.setDoOutput(true);
                            urlConnection.setDoInput(true);
                            urlConnection.setChunkedStreamingMode(0);

                            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                            BufferedWriter writer;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                writer = new BufferedWriter(new OutputStreamWriter(
                                        out, StandardCharsets.UTF_8));
                            }
                            else
                                writer = new BufferedWriter(new OutputStreamWriter(
                                        out, "UTF-8"));
                            writer.write(data);
                            writer.flush();

                            int code = urlConnection.getResponseCode();
                            if (code !=  200) {
                                throw new IOException("Invalid response from server: " + code);
                            }

                            BufferedReader rd = new BufferedReader(new InputStreamReader(
                                    urlConnection.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = rd.readLine()) != null) {
                                sb.append(line).append("\n");
                            }
                            rd.close();
                            return sb.toString();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (urlConnection != null) {
                                urlConnection.disconnect();
                            }
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(String s){
                        super.onPostExecute(s);
                        findSpot.setText(getString(R.string.btnFunc));
                        for(String str : s.split("\n")){
                            resultData.add(str.split(",")[3]);
                        }
                        resultData.remove(0);


                    }
                }.execute("http://83.212.75.16:8086/api/v2/query?orgID=4180de514f7a8ab2");
            }
        }
        else{
            resultText.setBackgroundColor(Color.parseColor("#ffb976"));
            resultText.setText(getString(R.string.invalidMsg));
        }

        if(resultText.getVisibility() == View.INVISIBLE)
            resultText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        choice = parent.getItemAtPosition(pos).toString();
        Toast.makeText(getApplicationContext(), choice, Toast.LENGTH_LONG).show();
    }

//    private static class httpPost extends AsyncTask<Void, Void, Void> {
//        @Override
//        protected void onPreExecute(){
//            super.onPreExecute();
//        }
//
//        @Override
//        protected Void doInBackground(Void... params) {
//            HttpURLConnection urlConnection = null;
//
//            String data = "from(bucket: \"gigacampus2-parking\") " +
//                    "|> range(start: -5m) " +
//                    "|> filter(fn: (r) => r._measurement == \"parking_status\" and r._field == \"occupied\" and r._value == 0) " +
//                    "|> group( columns: [\"id\"] ) |> sort( columns: [\"_time\"], desc: false ) " +
//                    "|> last() |> keep( columns: [\"id\"] ) |> group()";
//
//            try {
//
//                URL url = new URL("http://83.212.75.16:8086/api/v2/query?orgID=4180de514f7a8ab2");
//                urlConnection = (HttpURLConnection) url.openConnection();
//                urlConnection.setRequestMethod("POST");
//                urlConnection.setRequestProperty("Authorization", "Token OpZihwsUM-5IrinGcpH0CDD2cXP9tbFWikdP6kgZlRLXjySElZwLqn5mLfHfmoR6hVtKCF-XmmeMOB20OIe8-w==");
//                urlConnection.setRequestProperty("Accept", "application/csv");
//                urlConnection.setRequestProperty("Content-Type", "application/vnd.flux");
//                urlConnection.setDoOutput(true);
//                urlConnection.setDoInput(true);
//                urlConnection.setChunkedStreamingMode(0);
//
//                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
//                BufferedWriter writer;
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
//                    writer = new BufferedWriter(new OutputStreamWriter(
//                            out, StandardCharsets.UTF_8));
//                }
//                else
//                    writer = new BufferedWriter(new OutputStreamWriter(
//                            out, "UTF-8"));
//                writer.write(data);
//                writer.flush();
//
//                int code = urlConnection.getResponseCode();
//                if (code !=  200) {
//                    throw new IOException("Invalid response from server: " + code);
//                }
//
//                BufferedReader rd = new BufferedReader(new InputStreamReader(
//                        urlConnection.getInputStream()));
//                String line;
//                while ((line = rd.readLine()) != null) {
//                    Log.i("data", line);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                if (urlConnection != null) {
//                    urlConnection.disconnect();
//                }
//            }
//
//            return null;
//        }
//    }
}