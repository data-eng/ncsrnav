package com.example.osmntut;

import androidx.appcompat.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

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

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    private String choice = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinner = (Spinner)findViewById(R.id.destSpinner);
        ArrayAdapter<CharSequence> spinner_adapter = ArrayAdapter.createFromResource(this, R.array.ncsr_locations, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinner_adapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        choice = parent.getItemAtPosition(pos).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        choice = "";
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