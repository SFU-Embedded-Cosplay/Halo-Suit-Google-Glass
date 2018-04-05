package suit.halo.suitcontroller.BluetoothSearch;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.glass.eye.EyeGesture;
import com.google.android.glass.eye.EyeGestureManager;
import com.google.glass.bluetooth.BluetoothAdapter;

import java.util.ArrayList;

import butterknife.BindView;
import suit.halo.suitcontroller.R;
import suit.halo.suitcontroller.WinkEyeGestureListener;

/**
 * Created by Michael Wong on 2/10/2018.
 */

public class BluetoothSearchFragment extends android.support.v4.app.Fragment implements WinkEyeGestureListener.WinkEyeGestureFragmentMethod, BluetoothSearchContract.View{

    private Context mContext;
    private EyeGestureManager mEyeGestureManager;
    private WinkEyeGestureListener mWinkEyeGestureListener;
    private EyeGesture winkGesture = EyeGesture.WINK;
    private int discoveredDeviceChoice;
    private ArrayList<BluetoothDevice> nearbyDiscoverableDevices = new ArrayList<BluetoothDevice>();
    private ArrayList<String> discoveredDeviceNameList = new ArrayList<String>();
    private final BroadcastReceiver mReceiver = getBroadcastReceiver();

    BluetoothAdapter mBluetoothAdapter;

    @BindView(R.id.deviceTextView)
    TextView discoveredDeviceText;

    public static BluetoothSearchFragment newInstance() {
        Bundle bundle = new Bundle();
        BluetoothSearchFragment bluetoothSearchFragment = new BluetoothSearchFragment();
        bluetoothSearchFragment.setArguments(bundle);
        return bluetoothSearchFragment;

    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view  = inflater.inflate(R.layout.main, container, false);
        discoveredDeviceNameList.add("Use the DemoApp");
        discoveredDeviceChoice = -1;
        mContext = container.getContext();
        mWinkEyeGestureListener = new WinkEyeGestureListener(this);
        mEyeGestureManager = EyeGestureManager.from(mContext);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(mReceiver, filter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mBluetoothAdapter.startDiscovery();
        mEyeGestureManager.register(winkGesture, mWinkEyeGestureListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBluetoothAdapter.cancelDiscovery();
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void onWinkFragmentMethod() {

    }

    private BroadcastReceiver getBroadcastReceiver() {
        return new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    nearbyDiscoverableDevices.add(device);
                    discoveredDeviceNameList.add(device.getName());
                    discoveredDeviceText.setText(String.valueOf(discoveredDeviceChoice + 1) + " out of " + String.valueOf(discoveredDeviceNameList.size()));
                }
            }
        };
    }

    @Override
    public void fragmentKeyDown() {

    }
}
