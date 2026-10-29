package com.mixpush.vivo;

import android.content.Context;

import com.mixpush.core.MixPushClient;
import com.mixpush.core.MixPushHandler;
import com.mixpush.core.MixPushMessage;
import com.vivo.push.model.UPSNotificationMessage;
import com.vivo.push.sdk.OpenClientPushMessageReceiver;
import com.vivo.push.subscribedialog.ChannelOperation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PushMessageReceiverImpl extends OpenClientPushMessageReceiver {
    MixPushHandler handler = MixPushClient.getInstance().getHandler();

    @Override
    public void onNotificationMessageClicked(Context context, UPSNotificationMessage message) {
        MixPushMessage pushMessage = new MixPushMessage();
        pushMessage.setPlatform(VivoPushProvider.VIVO);
//        pushMessage.setMsgId(String.valueOf(message.getMsgId()));
        pushMessage.setTitle(message.getTitle());
        pushMessage.setDescription(message.getContent());
        pushMessage.setPayload(message.getSkipContent());
        handler.getPushReceiver().onNotificationMessageClicked(context, pushMessage);
    }


    @Override
    public void onReceiveRegId(Context context, String regId) {
        VivoPushProvider.onRegIdReceived(context, regId);
    }

    @Override
    public void onSubscribeDialogResult(int operateResult, List<ChannelOperation> operations) {
        Map<String, Integer> channelOperations = new LinkedHashMap<>();
        if (operations != null) {
            for (ChannelOperation operation : operations) {
                if (operation != null && operation.getChannelId() != null) {
                    channelOperations.put(operation.getChannelId(), operation.getOperation());
                }
            }
        }
        VivoPushProvider.dispatchSubscribeDialogResult(
                operateResult, Collections.unmodifiableMap(channelOperations));
    }
}
