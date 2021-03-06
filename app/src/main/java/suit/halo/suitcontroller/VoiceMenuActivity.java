package suit.halo.suitcontroller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.glass.eye.EyeGesture;
import com.google.android.glass.eye.EyeGestureManager;
import com.google.android.glass.eye.EyeGestureManager.Listener;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VoiceMenuActivity extends Activity implements SensorEventListener {

    private Sensor mSensor;
    private int mLastAccuracy;
    private SensorManager mSensorManager;

    private EyeGestureManager mEyeGestureManager;
    private WinkEyeGestureListener mWinkEyeGestureListener;
    private EyeGesture winkGesture = EyeGesture.WINK;

    @BindView(R.id.temp1)
    TextView temp1Text;
    @BindView(R.id.temp2)
    TextView temp2Text;
    @BindView(R.id.temp3)
    TextView temp3Text;
    @BindView(R.id.temp4)
    TextView temp4Text;

    @BindView(R.id.batt1)
    TextView batt1Text;
    @BindView(R.id.batt2)
    TextView batt2Text;
    @BindView(R.id.batt3)
    TextView batt3Text;
    @BindView(R.id.batt4)
    TextView batt4Text;

    @BindView(R.id.micLayout)
    LinearLayout blink1;
    @BindView(R.id.soundLayout)
    LinearLayout blink2;
    @BindView(R.id.headLightsLayout)
    LinearLayout blink3;
    @BindView(R.id.coolingLayout)
    LinearLayout blink4;
    @BindView(R.id.lightsLayout)
    LinearLayout blink5;

    @BindView(R.id.micButton)
    ImageView blinkOn1;
    @BindView(R.id.soundButton)
    ImageView blinkOn2;
    @BindView(R.id.headLightsButton)
    ImageView blinkOn3;
    @BindView(R.id.coolingButton)
    ImageView blinkOn4;
    @BindView(R.id.lightsButton)
    ImageView blinkOn5;

    @BindView(R.id.headIcon)
    ImageView headTemp;
    @BindView(R.id.torsoIcon)
    ImageView torsoTemp;
    @BindView(R.id.lowerIcon)
    ImageView lowerTemp;

    @BindView(R.id.bars)
    ImageView bars;

    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
    private BluetoothSocket mSocket2;

    private BluetoothAdapter mAdapter;
    private Set<BluetoothDevice> mPairedDevices;

    private Thread connectToHostThread, beagleBoneReceivingThread, batteryStateThread;

    private static final int YAW_MULTIPLIER = 10000;
    private static final int numButtons = 5;
    private static final int quadrants = 1;
    private static final int totalQuadrants = numButtons * quadrants * 2; // * 2 for spaces inbetween
    private long lastSelectedButton = -1;
    private int selectedButton = 1;
    private static final int filterWindowSize = 10;
    private BlockingQueue<Integer> unfilteredRotationData = new LinkedBlockingQueue<>(filterWindowSize);
    private int[] selectedButtonState = new int[5];

    private SoundPool soundPool;
    private int volume;
    private static final UUID insecureUUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_menu);
        Intent intent = getIntent();
        ButterKnife.bind(this);
        mEyeGestureManager = EyeGestureManager.from(this);
        mWinkEyeGestureListener = new WinkEyeGestureListener();

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        soundPool.load(this, R.raw.shield_off, 1);
        soundPool.load(this, R.raw.shield_on, 1);

    }

    private class ConnectToHostThread extends Thread {
        @Override
        public void run() {
            mDevice = BluetoothDeviceManager.getInstance().getBluetoothDevice();
            if (mDevice == null) {
                BluetoothServerSocket mmServerSocket = null;
                try {
                    mmServerSocket = mAdapter.listenUsingInsecureRfcommWithServiceRecord("Connection", insecureUUID);
                    mSocket = mmServerSocket.accept();
                    mmServerSocket.close();
                    beagleBoneReceivingThread = new BeagleBoneReceivingThread();
                    beagleBoneReceivingThread.start();
                } catch (IOException e) {
                    Log.d("VoiceMenuActivity", "failed to create listenUsingInsecureRfcommWithServiceRecord");
                }
            }
            else {
                try {
                    Method m = mDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                    mSocket = (BluetoothSocket) m.invoke(mDevice, Constants.DEVICE_CHANNEL);
                    mSocket.connect();

                    beagleBoneReceivingThread = new BeagleBoneReceivingThread();
                    beagleBoneReceivingThread.start();
                } catch (Exception e) {
                    Log.e("ConnectToHostThread", "an exception occurred with beaglebone connection", e);
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mEyeGestureManager.register(winkGesture, mWinkEyeGestureListener);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mAdapter.enable();

        try {
            mPairedDevices = mAdapter.getBondedDevices();
            for (BluetoothDevice mDevice : mPairedDevices) {
                String deviceName = mDevice.getName();
                if (deviceName.equals(Constants.DEVICE_IDENTIFIER)) {
                    this.mDevice = mDevice;
                }
            }
            connectToHostThread = new ConnectToHostThread();
            connectToHostThread.start();
            batteryStateThread = new BatteryStateThread();
            batteryStateThread.start();
        } catch (Exception e) {
            int x = 1;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this, mSensor);
        mEyeGestureManager.unregister(winkGesture, mWinkEyeGestureListener);
        beagleBoneReceivingThread.stop();
        batteryStateThread.stop();
        try {
            mSocket.close();
            mAdapter.disable();
        } catch (IOException e) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private class WinkEyeGestureListener implements Listener {

        @Override
        public void onEnableStateChange(EyeGesture eyeGesture, boolean paramBoolean) {
        }

        @Override
        public void onDetected(final EyeGesture eyeGesture) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    winkOnButton();
                }
            });
        }
    }


    private class BeagleBoneReceivingThread extends Thread {
        private byte[] bytes;
        private double temp1 = 666;
        private double temp2 = 666;
        private double temp3 = 666;
        private double temp4 = 666;

        private int batt1 = 666;
        private int batt2 = 666;
        private int batt3 = 666;
        private int batt4 = 666;

        private int dist1 = 666;

        boolean lowbat = false;

        @Override
        public void run() {
            while (true) {
                try {
                    bytes = new byte[1024];
                    int i = mSocket.getInputStream().read(bytes);
                    JSONObject jsonObject = new JSONObject(new String(bytes));
                    if (jsonObject.has("head temperature")) {
                        temp1 = jsonObject.getDouble("head temperature");
                    }
                    if (jsonObject.has("armpits temperature")) {
                        temp2 = jsonObject.getDouble("armpits temperature");
                    }
                    if (jsonObject.has("crotch temperature")) {
                        temp3 = jsonObject.getDouble("crotch temperature");
                    }
                    if (jsonObject.has("water temperature")) {
                        temp4 = jsonObject.getDouble("water temperature");
                    }
                    if (jsonObject.has("water temperature")) {
                        temp4 = jsonObject.getDouble("water temperature");
                    }

                    if (jsonObject.has("8 AH battery")) {
                        batt1 = jsonObject.getInt("8 AH battery");
                    }
                    if (jsonObject.has("2 AH battery")) {
                        batt2 = jsonObject.getInt("2 AH battery");
                    }
                    if (jsonObject.has("hud battery")) {
                        batt3 = jsonObject.getInt("hud battery");
                    }
                    if (jsonObject.has("phone battery")) {
                        batt4 = jsonObject.getInt("phone battery");
                    }
                    if (jsonObject.has("warnings")) {
                        JSONObject warnings = jsonObject.getJSONObject("warnings");
                        if (warnings.has("low 8AH battery warning") || (warnings.has("low 2AH battery warning"))) {
                            SoundMessageHandler.handleSoundMessage("low_bat", soundPool, volume);
                        }
                    }
                    if (jsonObject.has("ultrasonic")) {
                        dist1 = jsonObject.getInt("ultrasonic");
                    }

                    if (i != -1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (temp1 < 666) {
                                    temp1Text.setText(String.format("H: %.1f", temp1));
                                    if (temp1 < 25 && temp1 > 0) {
                                        headTemp.setImageDrawable(getResources().getDrawable(R.drawable.head_state));
                                    } else {
                                        headTemp.setImageDrawable(getResources().getDrawable(R.drawable.head_state_warm));
                                    }
                                }
                                if (temp2 < 666) {
                                    temp2Text.setText(String.format("A: %.1f", temp2));
                                    if (temp2 < 25 && temp2 > 0) {
                                        torsoTemp.setImageDrawable(getResources().getDrawable(R.drawable.torso_state));
                                    } else {
                                        torsoTemp.setImageDrawable(getResources().getDrawable(R.drawable.torso_state_warm));
                                    }
                                }
                                if (temp3 < 666) {
                                    temp3Text.setText(String.format("C: %.1f", temp3));
                                    if (temp3 < 25 && temp3 > 0) {
                                        lowerTemp.setImageDrawable(getResources().getDrawable(R.drawable.lower_state));
                                    } else {
                                        lowerTemp.setImageDrawable(getResources().getDrawable(R.drawable.lower_state_warm));
                                    }
                                }
                                if (temp4 < 666) {
                                    temp4Text.setText(String.format("W: %.1f", temp4));
                                }
                                if (batt1 < 666) {
                                    batt1Text.setText(String.format(" 8 AH battery: %d", batt1));
                                    if (batt1 > 75) {
                                        bars.setBackground(getResources().getDrawable(R.drawable.health_bar));
                                    } else if (batt1 > 50) {
                                        bars.setBackground(getResources().getDrawable(R.drawable.health_bar_75));
                                    } else if (batt1 > 25) {
                                        bars.setBackground(getResources().getDrawable(R.drawable.health_bar_50));
                                    } else {
                                        bars.setBackground(getResources().getDrawable(R.drawable.health_bar_25));
                                    }
                                }
                                if (batt2 < 666) {
                                    batt2Text.setText(String.format(" 2 AH battery: %d", batt2));
                                    if (batt2 > 75) {
                                        bars.setImageDrawable(getResources().getDrawable(R.drawable.energy_bar));
                                    } else if (batt2 > 50) {
                                        bars.setImageDrawable(getResources().getDrawable(R.drawable.energy_bar_75));
                                    } else if (batt2 > 25) {
                                        bars.setImageDrawable(getResources().getDrawable(R.drawable.energy_bar_50));
                                    } else {
                                        bars.setImageDrawable(getResources().getDrawable(R.drawable.energy_bar_25));
                                    }
                                }
                                if (temp4 < 666) {
                                    batt3Text.setText(String.format("  Hud battery: %d", batt3));
                                }
                                if (temp4 < 666) {
                                    batt4Text.setText(String.format("Phone battery: %d", batt4));
                                }

                            }
                        });
                    }
                } catch (Exception e) {

                }
            }
        }
    }

    private class BatteryStateThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                    Intent batteryStatus = registerReceiver(null, ifilter);
                    int charge = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

                    processCommand("sendBatteryState", charge);
                    Thread.sleep(20000, 0);
                } catch (Exception e) {
                    int x = 1;
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    public void processCommand(String command, int charge) {
        switch (command) {
            case "lights on": {
                try {
                    JSONObject j = new JSONObject();
                    j.put("lights", "on");
                    j.put("play sound", "lights");
                    mSocket.getOutputStream().write(j.toString().getBytes());
                } catch (Exception e) {

                }
            }
            break;
            case "lights off": {
                try {
                    JSONObject j = new JSONObject();
                    j.put("lights", "off");
                    j.put("play sound", "lights");
                    mSocket.getOutputStream().write(j.toString().getBytes());
                } catch (Exception e) {

                }
            }
            break;
            case "lights auto": {
                try {
                    JSONObject j = new JSONObject();
                    j.put("lights", "auto");
                    j.put("play sound", "lights");
                    mSocket.getOutputStream().write(j.toString().getBytes());
                } catch (Exception e) {

                }
            }
            break;
            case "head lights on": {
                try {
                    JSONObject j = new JSONObject();
                    j.put("head lights red", "on");
                    j.put("head lights white", "on");
                    j.put("play sound", "lights");
                    mSocket.getOutputStream().write(j.toString().getBytes());
                } catch (Exception e) {

                }
            }
            break;
            case "head lights off": {
                try {
                    JSONObject j = new JSONObject();
                    j.put("head lights red", "off");
                    j.put("head lights white", "off");
                    j.put("play sound", "lights");
                    mSocket.getOutputStream().write(j.toString().getBytes());
                } catch (Exception e) {

                }
            }
            break;
            case "cooling on": {
                try {
                    JSONObject j = new JSONObject();
                    j.put("peltier", "on");
                    j.put("water fan", "on");
                    j.put("water pump", "on");
                    j.put("head fans", "on");
                    j.put("play sound", "shield_on");
                    mSocket.getOutputStream().write(j.toString().getBytes());
                } catch (Exception e) {

                }
            }
            break;
            case "cooling off": {
                try {
                    JSONObject j = new JSONObject();
                    j.put("peltier", "off");
                    j.put("water fan", "off");
                    j.put("water pump", "off");
                    j.put("head fans", "off");
                    j.put("play sound", "shield_off");
                    mSocket.getOutputStream().write(j.toString().getBytes());
                } catch (Exception e) {

                }
            }
            break;
            case "sendBatteryState": {
                try {
                    JSONObject j = new JSONObject();
                    j.put("hud battery", charge);
                    mSocket.getOutputStream().write(j.toString().getBytes());
                } catch (Exception e) {
                    int x = 1;
                }
            }
            break;

            case "voice on": {
                try {
                    JSONObject j = new JSONObject();
                    j.put("play sound", "voice_on");
                    mSocket.getOutputStream().write(j.toString().getBytes());
                } catch (Exception e) {

                }
            }
            break;
            case "voice off": {
                try {
                    JSONObject j = new JSONObject();
                    j.put("play sound", "voice_off");
                    mSocket.getOutputStream().write(j.toString().getBytes());
                } catch (Exception e) {

                }
            }
            break;

            case "audio on": {
                try {
                    JSONObject j = new JSONObject();
                    j.put("play sound", "audio_on");
                    mSocket.getOutputStream().write(j.toString().getBytes());
                } catch (Exception e) {

                }
            }
            break;
            case "audio off": {
                try {
                    JSONObject j = new JSONObject();
                    j.put("play sound", "audio_off");
                    mSocket.getOutputStream().write(j.toString().getBytes());
                } catch (Exception e) {

                }
            }
            break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] mat = new float[9],
                orientation = new float[3];

        if (mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }

        SensorManager.getRotationMatrixFromVector(mat, event.values);
        SensorManager.getOrientation(mat, orientation);
        double yaw = (orientation[0] + Math.PI) * YAW_MULTIPLIER;
        int intYaw = (int) yaw;
        int filteredData = 0;

////
        int quadrantNum = intYaw / ((int) (Math.PI * 2 * 100000) / totalQuadrants);
        int buttonNumIndex = quadrantNum % 5;

        switch (buttonNumIndex) {
            case 0: {
                selectedButton = 1;
                break;
            }
            case 1: {
                selectedButton = 2;
                break;
            }
            case 2: {
                selectedButton = 3;
                break;
            }
            case 3: {
                selectedButton = 4;
                break;
            }
            case 4: {
                selectedButton = 5;
                break;
            }
        }
        if (selectedButton != lastSelectedButton) {
            lastSelectedButton = selectedButton;
            highlightButton(selectedButton);
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        mLastAccuracy = accuracy;
    }


    private void highlightButton(int buttonNum) {
        blink1.setBackgroundColor(Color.TRANSPARENT);
        blink2.setBackgroundColor(Color.TRANSPARENT);
        blink3.setBackgroundColor(Color.TRANSPARENT);
        blink4.setBackgroundColor(Color.TRANSPARENT);
        blink5.setBackgroundColor(Color.TRANSPARENT);

        switch (buttonNum) {
            case 1: {
                blink1.setBackgroundColor(Color.GRAY);
                break;
            }
            case 2: {
                blink2.setBackgroundColor(Color.GRAY);
                break;
            }
            case 3: {
                blink3.setBackgroundColor(Color.GRAY);
                break;
            }
            case 4: {
                blink4.setBackgroundColor(Color.GRAY);
                break;
            }
            case 5: {
                blink5.setBackgroundColor(Color.GRAY);
                break;
            }
            default: {
                int shouldntHappen = 1;
                break;
            }
        }
    }

    private void winkOnButton() {
        switch (selectedButton) {
            case 1: {
                winkOnVoiceButton();
                break;
            }
            case 2: {
                winkOnSoundButton();
                break;
            }
            case 3: {
                winkOnHeadLightsButton();
                break;
            }
            case 4: {
                winkOnCoolingButton();
                break;
            }
            case 5: {
                winkOnLightsButton();
                break;
            }
            default: {
                int shouldntHappen = 1;
                break;
            }
        }
    }

    private void winkOnVoiceButton() {
        if (selectedButtonState[0] == 0) {
            blinkOn1.setBackground(getResources().getDrawable(R.drawable.voice_off));
            selectedButtonState[0] = 1;
            processCommand("voice off", 0);
        } else {
            blinkOn1.setBackground(getResources().getDrawable(R.drawable.voice_on));
            selectedButtonState[0] = 0;
            processCommand("voice on", 0);
        }
    }

    private void winkOnSoundButton() {
        if (selectedButtonState[1] == 0) {
            blinkOn2.setBackground(getResources().getDrawable(R.drawable.audio_off));
            selectedButtonState[1] = 1;
            processCommand("audio off", 0);
        } else {
            blinkOn2.setBackground(getResources().getDrawable(R.drawable.audio_on));
            selectedButtonState[1] = 0;
            processCommand("audio on", 0);
        }
    }

    private void winkOnHeadLightsButton() {
        if (selectedButtonState[2] == 0) {
            blinkOn3.setBackground(getResources().getDrawable(R.drawable.head_lights_off));
            selectedButtonState[2] = 1;
            processCommand("head lights off", 1);
        } else {
            blinkOn3.setBackground(getResources().getDrawable(R.drawable.head_lights));
            selectedButtonState[2] = 0;
            processCommand("head lights on", 1);
        }
    }

    private void winkOnCoolingButton() {
        if (selectedButtonState[3] == 0) {
            blinkOn4.setBackground(getResources().getDrawable(R.drawable.cooling_off));
            selectedButtonState[3] = 1;
            processCommand("cooling off", 0);
        } else {
            blinkOn4.setBackground(getResources().getDrawable(R.drawable.cooling_on));
            selectedButtonState[3] = 0;
            processCommand("cooling on", 0);
        }
    }

    private void winkOnLightsButton() {
        if (selectedButtonState[4] == 0) {
            blinkOn5.setBackground(getResources().getDrawable(R.drawable.lights_auto));
            selectedButtonState[4] = 1;
            processCommand("lights auto", 0);
        } else if (selectedButtonState[4] == 1) {
            blinkOn5.setBackground(getResources().getDrawable(R.drawable.lights_off));
            selectedButtonState[4] = 2;
            processCommand("lights off", 0);
        } else {
            blinkOn5.setBackground(getResources().getDrawable(R.drawable.lights_on));
            selectedButtonState[4] = 0;
            processCommand("lights on", 0);
        }
    }
}