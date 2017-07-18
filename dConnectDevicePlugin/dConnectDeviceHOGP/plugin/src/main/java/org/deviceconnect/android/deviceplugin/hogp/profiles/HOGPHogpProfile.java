package org.deviceconnect.android.deviceplugin.hogp.profiles;

import android.content.Intent;

import org.deviceconnect.android.deviceplugin.hogp.HOGPMessageService;
import org.deviceconnect.android.deviceplugin.hogp.server.HOGPServer;
import org.deviceconnect.android.message.MessageUtils;
import org.deviceconnect.android.profile.DConnectProfile;
import org.deviceconnect.android.profile.api.PostApi;
import org.deviceconnect.message.DConnectMessage;

public class HOGPHogpProfile extends DConnectProfile {

    public HOGPHogpProfile() {

        // POST /hogp/keyboard
        addApi(new PostApi() {
            @Override
            public String getAttribute() {
                return "keyboard";
            }

            @Override
            public boolean onRequest(final Intent request, final Intent response) {
                String serviceId = (String) request.getExtras().get("serviceId");
                Integer modifier = parseInteger(request, "modifier");
                Integer keyCode = parseInteger(request, "keyCode");

                HOGPServer server = getHOGPServer();
                if (server == null) {
                    MessageUtils.setIllegalDeviceStateError(response, "HOGP server is not running.");
                } else {
                    server.sendKeyDown(modifier.byteValue(), keyCode.byteValue());
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    server.sendKeyUp();
                    setResult(response, DConnectMessage.RESULT_OK);
                }
                return true;
            }
        });

        // POST /hogp/mouse
        addApi(new PostApi() {
            @Override
            public String getAttribute() {
                return "mouse";
            }

            @Override
            public boolean onRequest(final Intent request, final Intent response) {
                String serviceId = (String) request.getExtras().get("serviceId");
                Integer x = parseInteger(request, "x");
                Integer y = parseInteger(request, "y");
                Integer wheel = parseInteger(request, "wheel");
                Boolean rightButton = parseBoolean(request, "rightButton");
                Boolean leftButton = parseBoolean(request, "leftButton");
                Boolean middleButton = parseBoolean(request, "middleButton");

                HOGPServer server = getHOGPServer();
                if (server == null) {
                    MessageUtils.setIllegalDeviceStateError(response, "HOGP server is not running.");
                } else {
                    if (wheel == null) {
                        wheel = 0;
                    }

                    if (rightButton == null) {
                        rightButton = false;
                    }

                    if (leftButton == null) {
                        leftButton = false;
                    }

                    if (middleButton == null) {
                        middleButton = false;
                    }

                    server.movePointer(x, y, wheel, leftButton, rightButton, middleButton);
                    setResult(response, DConnectMessage.RESULT_OK);
                }
                return true;
            }
        });

    }

    @Override
    public String getProfileName() {
        return "hogp";
    }

    /**
     * HOGPサーバを取得します.
     * @return HOGPサーバ
     */
    private HOGPServer getHOGPServer() {
        HOGPMessageService service = (HOGPMessageService) getContext();
        return (HOGPServer) service.getHOGPServer();
    }
}