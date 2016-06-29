/*
 LinkingBatteryProfile.java
 Copyright (c) 2016 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.linking.beacon.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.deviceconnect.android.deviceplugin.linking.BuildConfig;
import org.deviceconnect.android.deviceplugin.linking.LinkingApplication;
import org.deviceconnect.android.deviceplugin.linking.LinkingDevicePluginService;
import org.deviceconnect.android.deviceplugin.linking.beacon.LinkingBeaconManager;
import org.deviceconnect.android.deviceplugin.linking.beacon.data.BatteryData;
import org.deviceconnect.android.deviceplugin.linking.beacon.data.LinkingBeacon;
import org.deviceconnect.android.deviceplugin.linking.beacon.service.LinkingBeaconService;
import org.deviceconnect.android.event.Event;
import org.deviceconnect.android.event.EventError;
import org.deviceconnect.android.event.EventManager;
import org.deviceconnect.android.message.DConnectMessageService;
import org.deviceconnect.android.message.MessageUtils;
import org.deviceconnect.android.profile.BatteryProfile;
import org.deviceconnect.android.profile.api.DConnectApi;
import org.deviceconnect.android.profile.api.DeleteApi;
import org.deviceconnect.android.profile.api.GetApi;
import org.deviceconnect.android.profile.api.PutApi;
import org.deviceconnect.message.DConnectMessage;

import java.util.List;

public class LinkingBatteryProfile extends BatteryProfile {

    private static final String TAG = "LinkingPlugIn";
    private static final int TIMEOUT = 30;

    public LinkingBatteryProfile(final DConnectMessageService service) {
        LinkingApplication app = (LinkingApplication) service.getApplication();
        LinkingBeaconManager mgr = app.getLinkingBeaconManager();
        mgr.addOnBeaconBatteryEventListener(new LinkingBeaconManager.OnBeaconBatteryEventListener() {
            @Override
            public void onBattery(final LinkingBeacon beacon, final BatteryData battery) {
                notifyBatteryEvent(beacon, battery);
            }
        });

        addApi(mGetAll);
        addApi(mGetLevel);
        addApi(mPutOnBatteryChange);
        addApi(mDeleteOnBatteryChange);
    }

    private final DConnectApi mGetAll = new GetApi() {
        @Override
        public boolean onRequest(final Intent request, final Intent response) {
            final LinkingBeaconManager mgr = getLinkingBeaconManager();
            LinkingBeacon beacon = ((LinkingBeaconService) getService()).getLinkingBeacon();
            if (beacon == null) {
                MessageUtils.setNotSupportProfileError(response);
                return true;
            }

            mgr.addOnBeaconBatteryEventListener(new OnBeaconBatteryEventListenerImpl(beacon) {
                @Override
                public void onCleanup() {
                    mgr.removeOnBeaconBatteryEventListener(this);
                }

                @Override
                public void onTimeout() {
                    if (mCleanupFlag) {
                        return;
                    }

                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "onBattery: timeout");
                    }

                    MessageUtils.setTimeoutError(response);
                    sendResponse(response);
                }

                @Override
                public void onBattery(final LinkingBeacon beacon, final BatteryData battery) {
                    if (mCleanupFlag && !beacon.equals(mBeacon)) {
                        return;
                    }

                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "onBattery: beacon=" + beacon.getDisplayName() + " battery=" + battery);
                    }

                    setResult(response, DConnectMessage.RESULT_OK);
                    setLevel(response, battery.getLevel() / 100.0f);
                    sendResponse(response);
                    cleanup();
                }
            });
            mgr.startBeaconScan(TIMEOUT);

            return false;
        }
    };

    private final DConnectApi mGetLevel = new GetApi() {
        @Override
        public String getAttribute() {
            return ATTRIBUTE_LEVEL;
        }

        @Override
        public boolean onRequest(final Intent request, final Intent response) {
            final LinkingBeaconManager mgr = getLinkingBeaconManager();
            LinkingBeacon beacon = ((LinkingBeaconService) getService()).getLinkingBeacon();
            if (beacon == null) {
                MessageUtils.setNotSupportProfileError(response);
                return true;
            }

            mgr.addOnBeaconBatteryEventListener(new OnBeaconBatteryEventListenerImpl(beacon) {
                @Override
                public void onCleanup() {
                    mgr.removeOnBeaconBatteryEventListener(this);
                }

                @Override
                public void onTimeout() {
                    if (mCleanupFlag) {
                        return;
                    }

                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "onBattery: timeout");
                    }

                    MessageUtils.setTimeoutError(response);
                    sendResponse(response);
                }

                @Override
                public void onBattery(final LinkingBeacon beacon, final BatteryData battery) {
                    if (mCleanupFlag && !beacon.equals(mBeacon)) {
                        return;
                    }

                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "onBattery: beacon=" + beacon.getDisplayName() + " battery=" + battery);
                    }

                    setResult(response, DConnectMessage.RESULT_OK);
                    setLevel(response, battery.getLevel() / 100.0f);
                    sendResponse(response);
                    cleanup();
                }
            });
            mgr.startBeaconScan(TIMEOUT);
            return false;
        }
    };

    private final DConnectApi mPutOnBatteryChange = new PutApi() {
        @Override
        public String getAttribute() {
            return ATTRIBUTE_ON_BATTERY_CHANGE;
        }

        @Override
        public boolean onRequest(final Intent request, final Intent response) {
            EventError error = EventManager.INSTANCE.addEvent(request);
            if (error == EventError.NONE) {
                getLinkingBeaconManager().startBeaconScan();
                setResult(response, DConnectMessage.RESULT_OK);
            } else if (error == EventError.INVALID_PARAMETER) {
                MessageUtils.setInvalidRequestParameterError(response);
            } else {
                MessageUtils.setUnknownError(response);
            }
            return true;
        }
    };

    private final DConnectApi mDeleteOnBatteryChange = new DeleteApi() {
        @Override
        public String getAttribute() {
            return ATTRIBUTE_ON_BATTERY_CHANGE;
        }

        @Override
        public boolean onRequest(final Intent request, final Intent response) {
            EventError error = EventManager.INSTANCE.removeEvent(request);
            if (error == EventError.NONE) {
                if (BeaconUtil.isEmptyEvent(getLinkingBeaconManager())) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Linking Beacon Event is empty.");
                    }
                    getLinkingBeaconManager().stopBeaconScan();
                }
                setResult(response, DConnectMessage.RESULT_OK);
            } else if (error == EventError.INVALID_PARAMETER) {
                MessageUtils.setInvalidRequestParameterError(response);
            } else {
                MessageUtils.setUnknownError(response);
            }
            return true;
        }
    };

    private void notifyBatteryEvent(final LinkingBeacon beacon, final BatteryData batteryData) {
        String serviceId = beacon.getServiceId();
        List<Event> events = EventManager.INSTANCE.getEventList(serviceId,
                PROFILE_NAME, null, ATTRIBUTE_ON_BATTERY_CHANGE);
        if (events != null && events.size() > 0) {
            for (Event event : events) {
                Intent intent = EventManager.createEventMessage(event);
                setBattery(intent, createBattery(batteryData));
                sendEvent(intent, event.getAccessToken());
            }
        }
    }

    private Bundle createBattery(final BatteryData batteryData) {
        Bundle battery = new Bundle();
        setLevel(battery, batteryData.getLevel() / 100.0f);
        return battery;
    }

    private LinkingBeaconManager getLinkingBeaconManager() {
        LinkingApplication app = getLinkingApplication();
        return app.getLinkingBeaconManager();
    }

    private LinkingApplication getLinkingApplication() {
        LinkingDevicePluginService service = (LinkingDevicePluginService) getContext();
        return (LinkingApplication) service.getApplication();
    }


    private abstract class OnBeaconBatteryEventListenerImpl extends TimeoutSchedule implements
            LinkingBeaconManager.OnBeaconBatteryEventListener, Runnable {
        OnBeaconBatteryEventListenerImpl(final LinkingBeacon beacon) {
            super(beacon);
        }
    }
}
