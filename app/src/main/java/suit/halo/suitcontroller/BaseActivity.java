package suit.halo.suitcontroller;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;

import java.util.List;

import butterknife.ButterKnife;
import suit.halo.suitcontroller.BluetoothSearch.BluetoothSearchFragment;

/**
 * Created by Michael Wong on 2/10/2018.
 */

public class BaseActivity extends Activity implements FragmentActivityInterface{

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.fragment_holder);
        ButterKnife.bind(this);
        goToBluetoothSearchFragment();
    }
    private void goToBluetoothSearchFragment() {
        android.support.v4.app.Fragment bluetoothFragment = BluetoothSearchFragment.newInstance();
        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.addToBackStack(BluetoothSearchFragment.class.getSimpleName());
        transaction.replace(R.id.fragment_holder, bluetoothFragment).commit();
    }


    @Override
    public boolean onKeyDown(int keycode, KeyEvent event) {
        if (keycode == KeyEvent.KEYCODE_DPAD_CENTER) {
            List<android.support.v4.app.Fragment> fragment = getSupportFragmentManager().getFragments();
            if (fragment.get(0) instanceof BluetoothSearchFragment) {
                WinkEyeGestureListener.WinkEyeGestureFragmentMethod listener = (WinkEyeGestureListener.WinkEyeGestureFragmentMethod) fragment.get(0);
                listener.onWinkFragmentMethod();
            }
            return true;
        }
        return super.onKeyDown(keycode, event);
    }
}
