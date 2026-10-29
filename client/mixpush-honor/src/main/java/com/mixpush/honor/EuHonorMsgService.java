package com.mixpush.honor;

import com.hihonor.push.sdk.HonorMessageService;
import com.hihonor.push.sdk.HonorPushDataMsg;

public class EuHonorMsgService extends HonorMessageService {
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s); // PushToken
    }

    @Override
    public void onMessageReceived(HonorPushDataMsg honorPushDataMsg) {
        super.onMessageReceived(honorPushDataMsg); // 透传消息

    }
}
