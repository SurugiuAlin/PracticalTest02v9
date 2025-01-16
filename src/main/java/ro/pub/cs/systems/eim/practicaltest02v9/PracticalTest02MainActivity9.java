package ro.pub.cs.systems.eim.practicaltest02v9;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
//import androidx.core.view.WindowInsetsCompat.Insets;
//import com.google.android.material.edge2edge.EdgeToEdge;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

public class PracticalTest02MainActivity9 extends AppCompatActivity {

    private static final String TAG = "PracticalTest02MainActivity9";
    private static final String ACTION_DATA_BROADCAST = "ro.pub.cs.systems.eim.practicaltest02v9.DATA_BROADCAST";
    private EditText wordEditText;
    private EditText minLettersEditText;
    private Button requestButton;
    private Button openMapsButton;
    private TextView resultTextView;
    private DataReceiver dataReceiver;
    private ExecutorService executorService;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_practical_test02v9_main);

        wordEditText = findViewById(R.id.wordEditText);
        minLettersEditText = findViewById(R.id.minLettersEditText);
        requestButton = findViewById(R.id.requestButton);
        openMapsButton = findViewById(R.id.openMapsButton);
        resultTextView = findViewById(R.id.resultTextView);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String word = wordEditText.getText().toString();
                String minLetters = minLettersEditText.getText().toString();
                fetchData(word, minLetters);
            }
        });

        openMapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PracticalTest02MainActivity9.this, MapsActivity.class);
                startActivity(intent);
            }
        });

        dataReceiver = new DataReceiver();
        IntentFilter filter = new IntentFilter(ACTION_DATA_BROADCAST);
        ContextCompat.registerReceiver(this, dataReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(dataReceiver);
        executorService.shutdown();
    }

    private void fetchData(String word, String minLetters) {
        executorService.execute(() -> {
            String urlString = "http://www.anagramica.com/all/" + word;
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                String result = response.toString();
                handler.post(() -> parseAndBroadcast(result));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching data", e);
            }
        });
    }

    private void parseAndBroadcast(String jsonData) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray resultsArray = jsonObject.getJSONArray("all");
            StringBuilder filteredResults = new StringBuilder();
            for (int i = 0; i < resultsArray.length(); i++) {
                String result = resultsArray.getString(i);
                if (result.length() >= Integer.parseInt(minLettersEditText.getText().toString())) {
                    if (filteredResults.length() > 0) {
                        filteredResults.append(", ");
                    }
                    filteredResults.append(result);
                }
            }
            Log.d(TAG, "Filtered results: " + filteredResults.toString());
            Intent intent = new Intent(ACTION_DATA_BROADCAST);
            intent.putExtra("data", filteredResults.toString());
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON", e);
        }
    }

    private class DataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_DATA_BROADCAST)) {
                String data = intent.getStringExtra("data");
                resultTextView.setText(data);
            }
        }
    }
}