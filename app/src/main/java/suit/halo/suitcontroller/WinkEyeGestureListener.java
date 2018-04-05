package suit.halo.suitcontroller;

import android.util.Log;

import com.google.android.glass.eye.EyeGesture;
import com.google.android.glass.eye.EyeGestureManager;

/**
 * Created by Michael Wong on 2/17/2018.
 */

public class WinkEyeGestureListener implements EyeGestureManager.Listener {
    public static final String TAG  = WinkEyeGestureListener.class.getSimpleName();
    private WinkEyeGestureFragmentMethod mUsingFragment;

    public WinkEyeGestureListener(android.support.v4.app.Fragment fragment) {
        if (fragment instanceof WinkEyeGestureFragmentMethod) {
            mUsingFragment = (WinkEyeGestureFragmentMethod) fragment;
        } else {
            Log.e(TAG, fragment.getTag() + " using this Listener does not implement WinkEyeGestureFragmentMethod Interface",null );
        }
    }
    @Override
    public void onDetected(EyeGesture paramEyeGesture) {
        mUsingFragment.onWinkFragmentMethod();
    }

    @Override
    public void onEnableStateChange(EyeGesture paramEyeGesture, boolean paramBoolean) {

    }
    public interface WinkEyeGestureFragmentMethod {
        void onWinkFragmentMethod();
    }
}
