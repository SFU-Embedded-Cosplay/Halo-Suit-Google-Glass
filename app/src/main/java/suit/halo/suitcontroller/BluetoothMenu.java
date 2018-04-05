package suit.halo.suitcontroller;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import com.google.android.glass.eye.EyeGesture;
import com.google.android.glass.eye.EyeGestureManager;
import com.google.glass.bluetooth.BluetoothAdapter;
import java.util.ArrayList;


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
            Intent intent = new Intent(this, VoiceMenuActivity.class);
            if(discoveredDeviceChoice == 0) {
                BluetoothDeviceManager.getInstance().setDevice(null);
            }
            else {
                BluetoothDeviceManager.getInstance().setDevice(nearbyDiscoverableDevices.get(discoveredDeviceChoice));
            }
            mEyeGestureManager.unregister(winkGesture, mWinkGestureListener);
            mBluetoothAdapter.cancelDiscovery();
            startActivity(intent);
        }
    }
}
