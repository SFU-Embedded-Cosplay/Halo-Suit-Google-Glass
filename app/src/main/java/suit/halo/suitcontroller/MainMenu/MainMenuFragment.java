package suit.halo.suitcontroller.MainMenu;

import android.app.Fragment;
import android.os.Bundle;

import suit.halo.suitcontroller.BluetoothSearch.BluetoothSearchFragment;

/**
 * Created by Michael Wong on 2/17/2018.
 */

public class MainMenuFragment extends Fragment {

    public static Fragment newInstance() {
        Bundle bundle = new Bundle();
        Fragment mainMenuFragment = new MainMenuFragment();
        mainMenuFragment.setArguments(bundle);
        return mainMenuFragment;
    }

}
