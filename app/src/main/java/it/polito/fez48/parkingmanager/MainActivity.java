package it.polito.fez48.parkingmanager;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothClassicService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus;
import com.skyfishjy.library.RippleBackground;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    ImageView iv;
    BluetoothService service;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv = (ImageView)findViewById(R.id.iv_connect);
        final RippleBackground rippleBackground=(RippleBackground)findViewById(R.id.ripple);
        BluetoothConfiguration config = new BluetoothConfiguration();
        config.context = getApplicationContext();
        config.bluetoothServiceClass = BluetoothClassicService.class; // BluetoothClassicService.class or BluetoothLeService.class
        config.bufferSize = 1024;
        config.characterDelimiter = '\n';
        config.deviceName = "EmployeeSmartphone";
        config.callListenersInMainThread = true;
        config.uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // Set null to find all devices on scan.
        //config.transport = BluetoothDevice.TRANSPORT_LE; // Only for dual-mode devices
        BluetoothService.init(config);
        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            Toast.makeText(this, "Enabling Bluetooth...", Toast.LENGTH_SHORT).show();
        }
        service = BluetoothService.getDefaultInstance();


        service.setOnScanCallback(new BluetoothService.OnBluetoothScanCallback() {
            @Override
            public void onDeviceDiscovered(BluetoothDevice device, int rssi) {
                Log.d("App", "Address: " + device.getAddress());
                Log.d("App", "Found " + (device.getName() != null ? device.getName() : "unknown"));
                if(device.getName() != null && device.getName().equals("FezBluetooth")) {
                    Log.d("App", "Found fez");
                    pairDevice(device);
                    service.connect(device);
                    service.stopScan();
                    startCommunication();
                }
            }

            @Override
            public void onStartScan() {
            }

            @Override
            public void onStopScan() {
            }


        });

        service.setOnEventCallback(new BluetoothService.OnBluetoothEventCallback() {
            @Override
            public void onDataRead(byte[] buffer, int length) {
                String s = new String(buffer, StandardCharsets.UTF_8);
                Log.d("ON_RECEIVED",s );
                Toast.makeText(getApplicationContext(), "Received: " + s, Toast.LENGTH_SHORT).show();
                if(s.contains("AUTHENTICATED")) {
                    Toast.makeText(getApplicationContext(), "Good Morning!", Toast.LENGTH_LONG).show();
                    service.disconnect();
                    rippleBackground.stopRippleAnimation();

                    //Dialog che avvisa che tutto va ok
                } else if (s.equals("ERROR")) {
                    //Dialog che dice di riprovare
                }
                else {
                    //Non è un dipendente...
                }

            }



            @Override
            public void onStatusChange(BluetoothStatus status) {

            }

            @Override
            public void onDeviceName(String deviceName) {
            }

            @Override
            public void onToast(String message) {
            }

            @Override
            public void onDataWrite(byte[] buffer) {
            }
        });

        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rippleBackground.startRippleAnimation();
                service.startScan();
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        this.registerReceiver(mPairingRequestReceiver, filter);
    }

    private final BroadcastReceiver mPairingRequestReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                try {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int pin=intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 1234);
                    //the pin in case you need to accept for an specific pin
                    Log.d("App", "Start Auto Pairing. PIN = " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 1234));
                    byte[] pinBytes;
                    pinBytes = (""+pin).getBytes("UTF-8");
                    device.setPin(pinBytes);
                    //setPairing confirmation if neeeded
                    //device.setPairingConfirmation(true);
                } catch (Exception e) {
                    Log.e("App", "Error occurs when trying to auto pair");
                    e.printStackTrace();
                }
            }
        }
    };

    private void pairDevice(BluetoothDevice device) {
        try {
            Log.d("App", "Start Pairing... with: " + device.getName());
            device.createBond();
            Log.d("App", "Pairing finished.");
        } catch (Exception e) {
            Log.e("App", e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationPermission();
        checkPhonePermission();
    }

    public boolean checkPhonePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission. READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission. READ_PHONE_STATE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Permissions")
                        .setMessage("Need to access Phone state")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.READ_PHONE_STATE},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission. READ_PHONE_STATE},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }


    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission. ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission. ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Permissions")
                        .setMessage("Need to access location due to Blueetooth Interface")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission. ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

        }
    }

    private void startCommunication() {
        TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        final String imei = "-"+tm.getDeviceId();
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        service.write(imei.getBytes("UTF-8"));
                        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                        // Vibrate for 500 milliseconds
                        v.vibrate(250);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }, 5000);
    }
}
