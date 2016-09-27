package com.dji.simulatorDemo;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.flightcontroller.DJISimulatorInitializationData;
import dji.common.flightcontroller.DJISimulatorStateData;
import dji.common.flightcontroller.DJIVirtualStickFlightControlData;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.common.flightcontroller.DJIFlightControllerDataType;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.flightcontroller.DJISimulator;
import dji.sdk.products.DJIAircraft;
import dji.sdk.base.DJIBaseProduct;
import dji.common.error.DJIError;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getName();

    private DJIFlightController mFlightController;

    protected TextView mConnectStatusTextView;

    private Button mBtnEnableVirtualStick;
    private Button mBtnDisableVirtualStick;
    private Button mBtnTakeOff;
    private Button mBtnLand;
    private Button mBtnMove;

    private TextView mTextView;
    private Timer mSendVirtualStickDataTimer;

    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main);
        initUI();


        IntentFilter filter = new IntentFilter();
        filter.addAction(DJISimulatorApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateTitleBar();
        }
    };

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTitleBar() {
        if(mConnectStatusTextView == null) return;
        boolean ret = false;
        DJIBaseProduct product = DJISimulatorApplication.getProductInstance();
        if (product != null) {
            if(product.isConnected()) {
                mConnectStatusTextView.setText(DJISimulatorApplication.getProductInstance().getModel() + " Connected");
                ret = true;
            } else {
                if(product instanceof DJIAircraft) {
                    DJIAircraft aircraft = (DJIAircraft)product;
                    if(aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        mConnectStatusTextView.setText("only RC Connected");
                        ret = true;
                    }
                }
            }
        }

        if(!ret) {
            mConnectStatusTextView.setText("Disconnected");
        }
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        updateTitleBar();
        initFlightController();
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        if (null != mSendVirtualStickDataTimer) {

            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer = null;
        }
        super.onDestroy();
    }


    private void initFlightController() {
        DJIAircraft aircraft = DJISimulatorApplication.getAircraftInstance();

        showToast(aircraft + " AirCraft");
        if (aircraft == null || !aircraft.isConnected()) {
            showToast("Disconnected");
            mFlightController = null;
            return;
        } else {
            mFlightController = aircraft.getFlightController();
            mFlightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                @Override
                public void onResult(final DJIFlightControllerCurrentState djiFlightControllerCurrentState) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {

                            mPitch = (float) djiFlightControllerCurrentState.getAttitude().pitch;
                            mYaw = (float)djiFlightControllerCurrentState.getAttitude().yaw;
                            mRoll = (float)djiFlightControllerCurrentState.getAttitude().roll;
                            String yaw = String.format("%.2f", djiFlightControllerCurrentState.getAttitude().yaw);
                            String pitch = String.format("%.2f", djiFlightControllerCurrentState.getAttitude().pitch);
                            String roll = String.format("%.2f", djiFlightControllerCurrentState.getAttitude().roll);
                            String positionX = String.format("%.2f", djiFlightControllerCurrentState.getAircraftLocation().getAltitude());
                            String positionY = String.format("%.2f", djiFlightControllerCurrentState.getAircraftLocation().getLatitude());
                            String positionZ = String.format("%.2f", djiFlightControllerCurrentState.getAircraftLocation().getLongitude());
                            mTextView.setText("Yaw : " + yaw + ", Pitch : " + pitch + ", Roll : " + roll + "\n" + ", PosX : " + positionX +
                                    ", PosY : " + positionY +
                                    ", PosZ : " + positionZ);
                        }
                    });

                }
            });


            }

    }

    private void initUI() {

        mBtnEnableVirtualStick = (Button) findViewById(R.id.btn_enable_virtual_stick);
        mBtnDisableVirtualStick = (Button) findViewById(R.id.btn_disable_virtual_stick);
        mBtnTakeOff = (Button) findViewById(R.id.btn_take_off);
        mBtnLand = (Button) findViewById(R.id.btn_land);
        mBtnMove = (Button) findViewById(R.id.btn_move);
        mTextView = (TextView) findViewById(R.id.textview_simulator);
        mConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);

        mBtnEnableVirtualStick.setOnClickListener(this);
        mBtnDisableVirtualStick.setOnClickListener(this);
        mBtnTakeOff.setOnClickListener(this);
        mBtnLand.setOnClickListener(this);
        mBtnMove.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        initFlightController();
        showToast(mFlightController + " mFlightController ");

        switch (v.getId()) {
            case R.id.btn_enable_virtual_stick:

                if (mFlightController != null){
                    mFlightController.enableVirtualStickControlMode(
                            new DJICommonCallbacks.DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError == null) {
                                        showToast("enableVirtualStickControlMode: success");
                                    } else {
                                        showToast(djiError.getDescription());
                                    }
                                }
                            }
                    );
                }
                break;

            case R.id.btn_disable_virtual_stick:
                if (mFlightController != null){
                   mFlightController.disableVirtualStickControlMode(
                            new DJICommonCallbacks.DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.getDescription());
                                    } else {
                                        showToast("Disable Virtual Stick Success");
                                    }
                                }
                            }
                    );
                }
                break;

            case R.id.btn_take_off:
                if (mFlightController != null){
                    mFlightController.takeOff(
                            new DJICommonCallbacks.DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast("Error al despegar" + djiError.getDescription() + " " + djiError.toString());
                                    } else {
                                        showToast("Take off Success");
                                    }
                                }
                            }
                    );
                }

                break;

            case R.id.btn_land:
                if (mFlightController != null){

                    mFlightController.autoLanding(
                            new DJICommonCallbacks.DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.getDescription());
                                    } else {
                                        showToast("AutoLand Started");
                                    }
                                }
                            }
                    );

                }

                break;

            case R.id.btn_move:
                if (mFlightController != null){



                }

                break;
            default:
                break;
        }
    }

}
