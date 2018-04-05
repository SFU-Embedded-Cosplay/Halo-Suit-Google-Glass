package suit.halo.suitcontroller;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * Created by Michael Wong on 2/10/2018.
 */

public class BluetoothDeviceManager {
    //TODO add in RxJava or normal observers
    private static final String TAG = BluetoothDeviceManager.class.getSimpleName();

    private static BluetoothDeviceManager bluetoothManager = new BluetoothDeviceManager();
    private  BluetoothDevice mDevice;
    private  BluetoothSocket mSocket;

    public static BluetoothDeviceManager getInstance() {
        if (bluetoothManager == null) {
            Log.d(TAG, "getInstance: BluetoothManager is not instantiated");
        }
        return bluetoothManager;
    }

    public BluetoothDevice getBluetoothDevice() {
        return mDevice;
    }

    public void setDevice(BluetoothDevice device) {
        this.mDevice = mDevice;
    }

    public BluetoothSocket getSocket() {
        return mSocket;
    }

    public void setSocket(BluetoothSocket mSocket) {
        this.mSocket = mSocket;
    }
}

