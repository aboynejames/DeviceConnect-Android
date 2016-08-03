/*
 SpheroSystemProfile.java
 Copyright (c) 2014 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.sphero.profile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.deviceconnect.android.deviceplugin.sphero.setting.SettingActivity;
import org.deviceconnect.android.profile.SystemProfile;

/**
 * System プロファイル.
 * @author NTT DOCOMO, INC.
 */
public class SpheroSystemProfile extends SystemProfile {

    @Override
    protected Class<? extends Activity> getSettingPageActivity(final Intent request, final Bundle param) {
        return SettingActivity.class;
    }

//    @Override
//    protected boolean onDeleteEvents(final Intent request, final Intent response, final String sessionKey) {
//
//        if (sessionKey == null || sessionKey.length() == 0) {
//            MessageUtils.setInvalidRequestParameterError(response);
//        } else if (EventManager.INSTANCE.removeEvents(sessionKey)) {
//            setResult(response, DConnectMessage.RESULT_OK);
//
//            Collection<DeviceInfo> devices = SpheroManager.INSTANCE.getConnectedDevices();
//            for (DeviceInfo info : devices) {
//                if (!SpheroManager.INSTANCE.hasSensorEvent(info)) {
//                    SpheroManager.INSTANCE.stopSensor(info);
//                }
//                List<Event> events = EventManager.INSTANCE.getEventList(
//                        info.getDevice().getRobot().getIdentifier(), SpheroProfile.PROFILE_NAME,
//                        SpheroProfile.INTER_COLLISION, SpheroProfile.ATTR_ON_COLLISION);
//
//                if (events.size() == 0) {
//                    SpheroManager.INSTANCE.stopCollision(info);
//                }
//            }
//        } else {
//            MessageUtils.setUnknownError(response);
//        }
//
//        return true;
//    }
}
