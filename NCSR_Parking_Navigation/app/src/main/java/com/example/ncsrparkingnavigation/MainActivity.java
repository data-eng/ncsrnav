package com.example.ncsrparkingnavigation;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
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

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private String choice = "";
    private RadioGroup radioGroup;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AutoCompleteTextView acdropdown = findViewById(R.id.acDropdown);
        acdropdown.setThreshold(1);
        Button goMap = findViewById(R.id.mapButton);
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
            }
        });

        goMap.setOnClickListener(view -> openActivity());
        radioGroup = (RadioGroup) findViewById(R.id.constrGroup);
    }

    public void onRadioButtonClicked(View view){
        int selectedId = radioGroup.getCheckedRadioButtonId();
        RadioButton constraintRadioBtn = (RadioButton) findViewById(selectedId);

        if(selectedId == -1)
            Toast.makeText(getApplicationContext(), "No constraint selected!", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(getApplicationContext(), constraintRadioBtn.getText(), Toast.LENGTH_LONG).show();
    }

    public void openActivity()
    {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        choice = parent.getItemAtPosition(pos).toString();
        Toast.makeText(getApplicationContext(), choice, Toast.LENGTH_LONG).show();
    }

    private static class httpPost extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;

            String data = "from(bucket: \"gigacampus2-parking\") " +
                    "|> range(start: -5m) " +
                    "|> filter(fn: (r) => r._measurement == \"parking_status\" and r._field == \"occupied\" and r._value == 0) " +
                    "|> group( columns: [\"id\"] ) |> sort( columns: [\"_time\"], desc: false ) " +
                    "|> last() |> keep( columns: [\"id\"] ) |> group()";

            try {

                URL url = new URL("http://83.212.75.16:8086/api/v2/query?orgID=4180de514f7a8ab2");
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
                String line;
                while ((line = rd.readLine()) != null) {
                    Log.i("data", line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return null;
        }
    }
}