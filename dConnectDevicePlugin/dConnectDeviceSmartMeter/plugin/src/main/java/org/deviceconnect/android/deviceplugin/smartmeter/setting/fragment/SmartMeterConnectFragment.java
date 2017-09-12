/*
SmartMeterConnectFragment
Copyright (c) 2017 NTT DOCOMO,INC.
Released under the MIT license
http://opensource.org/licenses/mit-license.php
*/
package org.deviceconnect.android.deviceplugin.smartmeter.setting.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.deviceconnect.android.deviceplugin.smartmeter.R;
import org.deviceconnect.android.deviceplugin.smartmeter.SmartMeterMessageService;
import org.deviceconnect.android.deviceplugin.smartmeter.param.DongleConst;
import org.deviceconnect.android.deviceplugin.smartmeter.setting.SmartMeterSettingActivity;
import org.deviceconnect.android.deviceplugin.smartmeter.util.PrefUtil;

import java.util.HashMap;

/**
 * 接続画面用Fragment.
 *
 * @author NTT DOCOMO, INC.
 */
public class SmartMeterConnectFragment extends Fragment {
    /** Context. */
    private static Context mContext;
    /** Activity. */
    private static Activity mActivity;
    /** TAG. */
    private static final String TAG = "SMT_MTR_PLUGIN_SETTING";
    /** TextView. */
    private static TextView mTextViewCommment;
    /** Service Status. */
    private static int mDongleStatus;
    /** Activity Status. */
    private static int mActivityStatus;
    /** Dongleとの接続が完了した時にActivityを終了するフラグを格納するキー. */
    public static final String EXTRA_FINISH_FLAG = "flag";
    /** PrefUtil Instance. */
    private PrefUtil mPrefUtil;
    /** B Route ID EditText. */
    private EditText mBrouteId;
    /** B Route Password EditText. */
    private EditText mBroutePassword;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Root view.
        View root = inflater.inflate(R.layout.connect, container, false);

        // Context.
        mContext = getActivity().getBaseContext();
        mActivity = getActivity();

        // PrefUtil Instance.
        mPrefUtil = ((SmartMeterSettingActivity) getContext()).getPrefUtil();

        // Set status.
        mActivityStatus = DongleConst.STATUS_ACTIVITY_DISPLAY;
        mDongleStatus = DongleConst.STATUS_DONGLE_NOCONNECT;

        // Textview.
        mTextViewCommment = (TextView) root.findViewById(R.id.textViewComment);

        mBrouteId = (EditText)root.findViewById(R.id.input_b_route_id);
        mBrouteId.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    closeSoftwareKeyboard(v);
                }
            }
        });
        String bRouteId = mPrefUtil.getBRouteId();
        if (bRouteId != null) {
            mBrouteId.setText(bRouteId);
        }

        mBroutePassword = (EditText) root.findViewById(R.id.input_b_route_password);
        mBroutePassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    closeSoftwareKeyboard(v);
                }
            }
        });
        String bRoutePassword = mPrefUtil.getBRoutePass();
        if (bRoutePassword != null) {
            mBroutePassword.setText(bRoutePassword);
        }

        Button okButton = (Button) root.findViewById(R.id.settings_save);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String bRouteId = mBrouteId.getText().toString();
                if (bRouteId.length() == 0) {
                    Toast.makeText(getContext(), R.string.setting_error_b_route_id, Toast.LENGTH_LONG).show();
                    return;
                }
                String bRoutePassword = mBroutePassword.getText().toString();
                if (bRoutePassword.length() == 0) {
                    Toast.makeText(getContext(), R.string.setting_error_b_route_password, Toast.LENGTH_LONG).show();
                    return;
                }

                mPrefUtil.setBRouteId(bRouteId);
                mPrefUtil.setKeyBRoutePass(bRoutePassword);
                Toast.makeText(getContext(), R.string.setting_save_complete, Toast.LENGTH_LONG).show();
            }
        });
        return root;
    }

    /**
     * Close software Keyboard
     */
    private void closeSoftwareKeyboard(final View v) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public void onResume() {
        super.onResume();

        // USBの結果受信用のBroadcast Receiverを設定.
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(DongleConst.DEVICE_TO_DONGLE_OPEN_USB_RESULT);
        mIntentFilter.addAction(DongleConst.DEVICE_TO_DONGLE_CHECK_USB_RESULT);
        mIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mContext.registerReceiver(mUSBResultEvent, mIntentFilter);

        // 接続状態をチェック.
        Intent intent = new Intent(DongleConst.DEVICE_TO_DONGLE_CHECK_USB);
        mContext.sendBroadcast(intent);

        // USBデバイスのチェック.
        UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == 1027) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Serviceにメッセージを送信.
                        Intent mIntent = new Intent(mContext, SmartMeterMessageService.class);
                        mContext.startService(mIntent);

                        // USB OpenのコマンドをServiceにBroadcast.
                        Intent intent = new Intent(DongleConst.DEVICE_TO_DONGLE_OPEN_USB);
                        mContext.sendBroadcast(intent);
                    }
                });
                break;
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mUSBResultEvent != null) {
            mContext.unregisterReceiver(mUSBResultEvent);
        }
        mActivityStatus = DongleConst.STATUS_ACTIVITY_PAUSE;
    }

    /**
     * Broadcast receiver for usb event.
     */
    private BroadcastReceiver mUSBResultEvent = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            String action = intent.getAction();
            boolean bCloseFlag = intent.getBooleanExtra(EXTRA_FINISH_FLAG, false);

            switch (action) {
                case DongleConst.DEVICE_TO_DONGLE_OPEN_USB_RESULT:
                    int resultId = intent.getIntExtra("resultId", 0);
                    if (resultId == DongleConst.CAN_NOT_FIND_USB) {
                        mTextViewCommment.setText(R.string.not_found_arduino);
                    } else if (resultId == DongleConst.FAILED_OPEN_USB) {
                        mTextViewCommment.setText(R.string.failed_open_usb);
                    } else if (resultId == DongleConst.FAILED_CONNECT_DONGLE) {
                        mTextViewCommment.setText(R.string.failed_connect_dongle);
                    } else if (resultId == DongleConst.SUCCESS_CONNECT_DONGLE) {
                        mTextViewCommment.setText(R.string.success_connect_dongle);
                        String bRouteId = mBrouteId.getText().toString();
                        String bRoutePassword = mBroutePassword.getText().toString();
                        if (bRouteId.length() == 0) {
                            Toast.makeText(getContext(), R.string.setting_error_b_route_id, Toast.LENGTH_LONG).show();
                        } else if (bRoutePassword.length() == 0) {
                            Toast.makeText(getContext(), R.string.setting_error_b_route_password, Toast.LENGTH_LONG).show();
                        } else {
                            checkAndFinish(bCloseFlag);
                        }
                    }
                    break;
                case DongleConst.DEVICE_TO_DONGLE_CHECK_USB_RESULT:
                    int statusId = intent.getIntExtra("statusId", 0);
                    if (statusId == DongleConst.STATUS_DONGLE_NOCONNECT) {
                        mDongleStatus = DongleConst.STATUS_DONGLE_NOCONNECT;
                    } else if (statusId == DongleConst.STATUS_DONGLE_INIT) {
                        mDongleStatus = DongleConst.STATUS_DONGLE_INIT;
                    } else if (statusId == DongleConst.STATUS_DONGLE_RUNNING) {
                        mTextViewCommment.setText(R.string.success_connect);
                        mDongleStatus = DongleConst.STATUS_DONGLE_RUNNING;
                        checkAndFinish(bCloseFlag);
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    mTextViewCommment.setText(R.string.disconnect_usb);
                    mDongleStatus = DongleConst.STATUS_DONGLE_NOCONNECT;
                    Intent closeIntent = new Intent(DongleConst.DEVICE_TO_DONGLE_CLOSE_USB);
                    mContext.sendBroadcast(closeIntent);
                    checkAndFinish(bCloseFlag);
                    break;
            }
        }
    };

    /**
     * 終了フラグを確認して、Activityを終了します.
     */
    private void checkAndFinish(final boolean closeFlag) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (closeFlag) {
            activity.finish();
        }
    }
}