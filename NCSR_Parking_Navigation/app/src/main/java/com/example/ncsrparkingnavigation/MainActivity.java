package com.example.ncsrparkingnavigation;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private String choice = "";
    private RadioGroup radioGroup;
    private TextView resultText;
    private Button findSpot;
    private Button navBtn;
    private List<String> resultData;
    private String foundLat;
    private String foundLon;
    private String foundStreetId;

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
        findSpot = findViewById(R.id.findButton);
        navBtn = findViewById(R.id.mapButton);
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
                if(acdropdown.getText().toString().equals("")) {
                    choice = "";
                    findSpot.setEnabled(false);
                }
                else {
                    choice = acdropdown.getText().toString();
                    findSpot.setEnabled(true);
                }
            }
        });

        findSpot.setOnClickListener(view -> findAvailableSpot());
        navBtn.setOnClickListener(view -> openMapActivity());
    }

    public void openMapActivity()
    {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    @SuppressLint("StaticFieldLeak")
    public void findAvailableSpot()
    {
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
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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

                        StringBuilder csv_data = new StringBuilder();
                        StringBuilder json_data = new StringBuilder();

                        //read parking csv
                        try {
                            BufferedReader rd = new BufferedReader(new InputStreamReader(getAssets().open("parking-static.csv")));
                            String line;
                            while ((line = rd.readLine()) != null){
                                csv_data.append(line).append("\n");
                            }
                            rd.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        //convert csv to hashmap
                        HashMap<String, List<String>> csvHashmap = new HashMap<>();
                        for(String str : csv_data.toString().split("\n")){
                            String key = str.split(",")[0];
                            List<String> value;
                            if(str.split(",").length == 5)
                                value = new ArrayList<>(Arrays.asList(str.split(",")[1], str.split(",")[2],
                                                                    str.split(",")[4]));
                            else
                                value = new ArrayList<>(Arrays.asList(str.split(",")[1], str.split(",")[2], ""));
                            csvHashmap.put(key, value);
                        }

                        //read json with parking spot id's
                        try {
                            BufferedReader rd = new BufferedReader(new InputStreamReader(getAssets().open("parking-list.json")));
                            String line;
                            while ((line = rd.readLine()) != null){
                                json_data.append(line).append("\n");
                            }
                            rd.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        JSONObject jsonParser;
                        try {
                            jsonParser = new JSONObject(json_data.toString());

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        //check order
                        List<String> order1 = new ArrayList<>(Arrays.asList("Lefkippos1","Lefkippos2","Tesla","Library1","Library2"));
                        List<String> order2 = new ArrayList<>(Arrays.asList("Tesla","Lefkippos1","Lefkippos2","Library1","Library2"));
                        List<String> order3 = new ArrayList<>(Arrays.asList("Library2","Library1","Tesla","Lefkippos1","Lefkippos2"));
                        List<String> order4 = new ArrayList<>(Arrays.asList("Library1","Library2","Tesla","Lefkippos1","Lefkippos2"));

                        //check constraints and rules
                        String foundSpot = "";
                        String constraintData = constraintRadioBtn.getText().toString();
                        if(constraintData.equals(getString(R.string.rb1))){
                            if(resultData.contains("399")) {
                                foundSpot = "399";
                            }
                        }
                        else{
                            if(constraintData.equals(getString(R.string.rb2))) {
                                order1.remove("Lefkippos1");
                                order1.remove("Library2");
                                order2.remove("Lefkippos1");
                                order2.remove("Library2");
                                order3.remove("Lefkippos1");
                                order3.remove("Library2");
                                order4.remove("Lefkippos1");
                                order4.remove("Library2");
                            }

                            if(choice.equals("Lefkippos") || choice.equals("Technology Park") || choice.equals("SCio")
                                    || choice.equals("Fuelics")){

                                foundSpot = getParkingSpot(order1, jsonParser, constraintData);
                            }
                            else if(choice.equals("Tesla")){
                                foundSpot = getParkingSpot(order2, jsonParser, constraintData);
                            }
                            else if(choice.equals("Roboskel")){
                                foundSpot = getParkingSpot(order3, jsonParser, constraintData);
                            }
                            else if(choice.equals("Library") || choice.equals("Innovation Office")){
                                foundSpot = getParkingSpot(order4, jsonParser, constraintData);
                            }
                        }

                        if(foundSpot.equals("")){
                            resultText.setBackgroundColor(Color.parseColor("#ffb976"));
                            resultText.setText(getString(R.string.noVacancyMsg));
                        }
                        else{
                            resultText.setBackgroundColor(Color.parseColor("#abf5a7"));

                            if(foundSpot.equals("399")) {
                                resultText.setText(getString(R.string.motabilitySpotDesc));
                                foundStreetId = "";
                                foundLat = Objects.requireNonNull(csvHashmap.get(foundSpot)).get(0);
                                foundLon = Objects.requireNonNull(csvHashmap.get(foundSpot)).get(1);
                            }
                            else {
                                if(foundSpot.contains(",")) {
                                    foundStreetId = Objects.requireNonNull(csvHashmap.get(foundSpot.split(",")[0])).get(2) + ", "
                                            + Objects.requireNonNull(csvHashmap.get(foundSpot.split(",")[1])).get(2);
                                    resultText.setText(foundStreetId);
                                    foundLat = Objects.requireNonNull(csvHashmap.get(foundSpot.split(",")[0])).get(0);
                                    foundLon = Objects.requireNonNull(csvHashmap.get(foundSpot.split(",")[0])).get(1);
                                }
                                else {
                                    foundStreetId = Objects.requireNonNull(csvHashmap.get(foundSpot)).get(2);
                                    resultText.setText(foundStreetId);
                                    foundLat = Objects.requireNonNull(csvHashmap.get(foundSpot)).get(0);
                                    foundLon = Objects.requireNonNull(csvHashmap.get(foundSpot)).get(1);
                                }
                            }
                        }
                        resultText.setVisibility(View.VISIBLE);
                        navBtn.setEnabled(true);
                    }
                }.execute("http://83.212.75.16:8086/api/v2/query?orgID=4180de514f7a8ab2");
            }
        }
        else{
            resultText.setBackgroundColor(Color.parseColor("#ffb976"));
            resultText.setText(getString(R.string.invalidMsg));
            if(resultText.getVisibility() == View.INVISIBLE)
                resultText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        choice = parent.getItemAtPosition(pos).toString();
        Toast.makeText(getApplicationContext(), choice, Toast.LENGTH_LONG).show();
    }

    private String getParkingSpot(List<String> ruleOrder, JSONObject jsonPrsr, String constraint)
    {
        JSONArray idArray;
        String foundSpot = "";
        boolean found = false;
        for(String str : ruleOrder) {
            try {
                idArray = jsonPrsr.getJSONObject(str).getJSONArray("id");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            if(constraint.equals(getString(R.string.rb2))) {
                if(idArray.length() >= 2) {
                    for (int i = 0; i < (idArray.length() - 1); i++) {
                        try {
                            if (resultData.contains(idArray.getString(i)) && resultData.contains(idArray.getString(i + 1))) {
                                found = true;
                                foundSpot = idArray.getString(i) + "," + idArray.getString(i + 1);
                                break;
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            else{
                for (int i = 0; i < idArray.length(); i++) {
                    try {
                        if (resultData.contains(idArray.getString(i))) {
                            found = true;
                            foundSpot = idArray.getString(i);
                            break;
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if(found) break;
        }

        return foundSpot;
    }
}