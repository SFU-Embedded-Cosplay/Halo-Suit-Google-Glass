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
    private ArrayList<String> deviceUUIDList;

    private int discoveredDeviceChoice;
    private TextView selectedDeviceNameView;
    private TextView numDevicesView;
    private TextView deviceUUID;

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
        deviceUUIDList = new ArrayList<String>();
        // Choosing  device 0 will tell the VoiceMenuActivity to configure the bluetooth socket with the DemoApp
        discoveredDeviceNameList.add("Use the DemoApp");
        discoveredDeviceChoice = -1;
        selectedDeviceNameView = (TextView) findViewById(R.id.deviceChoice);
        numDevicesView = (TextView) findViewById(R.id.numDevice);
        deviceUUID = (TextView) findViewById(R.id.DeviceUUID);

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
        unregisterReceiver(mReceiver);
        mEyeGestureManager.unregister(winkGesture,mWinkGestureListener);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "ACTION_FOUND Device name: " + String.valueOf(device.getName()));
                Log.d(TAG, "ACTION_FOUND Device UUID: " + device.getUuids()[0].getUuid().toString());
                nearbyDiscoverableDevices.add(device);
                discoveredDeviceNameList.add(device.getName());
                deviceUUIDList.add(device.getUuids()[0].toString());
            }
        }
    };

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
        selectedDeviceNameView.setText(discoveredDeviceNameList.get(discoveredDeviceChoice));
        deviceUUID.setText(deviceUUIDList.get(discoveredDeviceChoice));
        numDevicesView.setText(String.valueOf(discoveredDeviceChoice + 1) + " out of " + String.valueOf(discoveredDeviceNameList.size()));

    }

    public void changeToDisplay() {
        mEyeGestureManager.unregister(winkGesture, mWinkGestureListener);
        Intent intent = new Intent(this, VoiceMenuActivity.class);
        if(discoveredDeviceChoice != -1) {
            if(discoveredDeviceChoice == 0) {
                String message = "DemoApp";
                intent.putExtra(intentSocketTag, message);
                Log.d("INTENT_LOG","DemoApp in Intent");
            }
            else {
                BluetoothDevice selectedDevice = nearbyDiscoverableDevices.get(discoveredDeviceChoice);
                String message = "BeagleBone";
                Log.d("INTENT_LOG", "BeagleBone in Intent");
                intent.putExtra(intentSocketTag, message);
                intent.putExtra(bluetoothDeviceTag, selectedDevice);
            }
            startActivity(intent);
        }
    }
}
