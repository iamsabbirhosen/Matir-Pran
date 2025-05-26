package com.nextgentechnologies.matirprantestapp;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final UUID BLUETOOTH_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;

    private Button connectButton;
    private TextView moistureTextView, phTextView;

    private ProgressDialog progressDialog;
    private Thread dataReceiverThread;
    private boolean stopThread = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectButton = findViewById(R.id.connectButton);
        moistureTextView = findViewById(R.id.moistureTextView);
        phTextView = findViewById(R.id.phTextView);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        connectButton.setOnClickListener(v -> connectToBluetoothDevice());
    }

    private void connectToBluetoothDevice() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not enabled!", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        bluetoothDevice = null;

        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equalsIgnoreCase("HC-05")) {
                bluetoothDevice = device;
                break;
            }
        }

        if (bluetoothDevice == null) {
            Toast.makeText(this, "HC-05 not found in paired devices!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Connecting to Bluetooth...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(BLUETOOTH_UUID);
                bluetoothSocket.connect();

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Bluetooth connected", Toast.LENGTH_SHORT).show();
                });

                startReceivingData();

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void startReceivingData() {
        stopThread = false;

        dataReceiverThread = new Thread(() -> {
            try {
                InputStream inputStream = bluetoothSocket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytes;

                while (!stopThread) {
                    bytes = inputStream.read(buffer);
                    String data = new String(buffer, 0, bytes);

                    runOnUiThread(() -> handleBluetoothData(data));
                }

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Error reading data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });

        dataReceiverThread.start();
    }

    private void handleBlupwd
    etoothData(String data) {
        data = data.trim();

        if (data.startsWith("PH:")) {
            try {
                float phValue = Float.parseFloat(data.substring(3));
                phTextView.setText("pH: " + phValue);
            } catch (NumberFormatException ignored) {}
        } else if (data.startsWith("MOIST:")) {
            try {
                float moistureValue = Float.parseFloat(data.substring(6));
                moistureTextView.setText("Moisture: " + moistureValue + "%");
            } catch (NumberFormatException ignored) {}
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopThread = true;
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
