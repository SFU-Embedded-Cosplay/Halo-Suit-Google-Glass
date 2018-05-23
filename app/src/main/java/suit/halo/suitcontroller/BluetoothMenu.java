package suit.halo.suitcontroller;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.glass.eye.EyeGesture;
import com.google.android.glass.eye.EyeGestureManager;
import com.google.glass.bluetooth.BluetoothAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;


/**
 * Created by Michael Wong on 9/6/2017.
 */

public class BluetoothMenu extends Activity{
    public static final String intentSocketTag = "SOCKET_CONFIGURATION";
    public static final String bluetoothDeviceTag = "BLUETOOTH_TAG";
    public static final String TAG = "LOG_TAG_BLUETOOTH_MENU";

    private ArrayList<BluetoothDevice> nearbyDiscoverableDevices;
    private ArrayList<String> discoveredDeviceNameList;

    private int discoveredDeviceChoice;
    private TextView selectedDeviceNameView;
    private TextView numDevicesView;

    BluetoothAdapter mBluetoothAdapter;
    private WinkGestureListener mWinkGestureListener;
    private EyeGestureManager mEyeGestureManager;
    private EyeGesture winkGesture = EyeGesture.WINK;

    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    android.bluetooth.BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_activity);

        nearbyDiscoverableDevices = new ArrayList<BluetoothDevice>();
        discoveredDeviceNameList = new ArrayList<String>();

        // Choosing  device 0 will tell the VoiceMenuActivity to configure the bluetooth socket with the DemoApp
        discoveredDeviceNameList.add("Use the DemoApp");
        // set so you cannot change activities unless you tap at least once
        discoveredDeviceChoice = -1;
        selectedDeviceNameView = (TextView) findViewById(R.id.deviceChoice);
        numDevicesView = (TextView) findViewById(R.id.numDevice);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mEyeGestureManager = EyeGestureManager.from(this);
        mWinkGestureListener = new WinkGestureListener();

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }


    @Override
    public boolean onKeyDown(int keycode, KeyEvent event) {
        if (keycode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if(discoveredDeviceChoice >= discoveredDeviceNameList.size()-1){
                    discoveredDeviceChoice = 0;
                }
                else {
                    discoveredDeviceChoice++;
                }
            updateChoiceText();
            return true;
        }
        return super.onKeyDown(keycode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBluetoothAdapter.startDiscovery();
        mEyeGestureManager.register(winkGesture, mWinkGestureListener);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothAdapter.cancelDiscovery();
        unregisterReceiver(mReceiver);
        mEyeGestureManager.unregister(winkGesture,mWinkGestureListener);
    }

    private final BroadcastReceiver mReceiver = getBroadcastReceiver();

    private BroadcastReceiver getBroadcastReceiver() {
        return new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG, "ACTION_FOUND Device name: " + String.valueOf(device.getName()));
                    nearbyDiscoverableDevices.add(device);
                    discoveredDeviceNameList.add(device.getName());
                    numDevicesView.setText(String.valueOf(discoveredDeviceChoice + 1) + " out of " + String.valueOf(discoveredDeviceNameList.size()));
                }
            }
        };
    }

    private class WinkGestureListener implements EyeGestureManager.Listener {
        @Override
        public void onEnableStateChange(EyeGesture eyeGesture, boolean paramBoolean) {
        }
        @Override
        public void onDetected(final EyeGesture eyeGesture) {
            changeToDisplay();
        }
    }

    public void updateChoiceText() {
        try {
            selectedDeviceNameView.setText(discoveredDeviceNameList.get(discoveredDeviceChoice));
            numDevicesView.setText(String.valueOf(discoveredDeviceChoice + 1) + " out of " + String.valueOf(discoveredDeviceNameList.size()));
        } catch (Exception e) {

        }

    }

    public void changeToDisplay() {
        if(discoveredDeviceChoice != -1) {
            Toast.makeText(getApplicationContext(),"Connection started.",Toast.LENGTH_LONG).show();

            Intent intent = new Intent(this, VoiceMenuActivity.class);
            if(discoveredDeviceChoice == 0) {
                Toast.makeText(getApplicationContext(),"Chosen Demo.",Toast.LENGTH_LONG).show();
            }
            else {
                new ConnectBT().execute();
            }
            mEyeGestureManager.unregister(winkGesture, mWinkGestureListener);
            mBluetoothAdapter.cancelDiscovery();
            startActivity(intent);
        }
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null)
                {
                    myBluetooth = android.bluetooth.BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
//                    BluetoothDevice beagleBoneDevice = myBluetooth.getRemoteDevice((nearbyDiscoverableDevices.get(discoveredDeviceChoice).getAddress()));//connects to the device's address and checks if it's available
                    BluetoothDevice beagleBoneDevice = nearbyDiscoverableDevices.get(discoveredDeviceChoice);//connects to the device's address and checks if it's available
                    btSocket = beagleBoneDevice.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    android.bluetooth.BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (Exception e)
            {
                Log.e(TAG, "failed in AsyncTask" + e.getMessage(), e);
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                Toast.makeText(getApplicationContext(),"Connection Failed. Is it a SPP Bluetooth? Try again.",Toast.LENGTH_LONG).show();
                finish();
            }
            else
            {
                Toast.makeText(getApplicationContext(),"Connection Success.",Toast.LENGTH_LONG).show();
            }

        }
    }
}
