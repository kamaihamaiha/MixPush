package com.mixpush.honor;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.hihonor.push.sdk.HonorPushCallback;
import com.hihonor.push.sdk.HonorPushClient;
import com.mixpush.core.BaseMixPushProvider;
import com.mixpush.core.MixPushClient;
import com.mixpush.core.MixPushHandler;
import com.mixpush.core.MixPushPlatform;
import com.mixpush.core.RegisterType;


public class HonorPushProvider extends BaseMixPushProvider {
    public static final String HONOR = "honor";
    public static String regId;

    MixPushHandler handler = MixPushClient.getInstance().getHandler();


    @Override
    public void register(Context context, RegisterType type) {
        HonorPushClient.getInstance().init(context, true);
        HonorPushClient.getInstance().getPushToken(new HonorPushCallback<String>() {
            @Override
            public void onSuccess(String s) {
                regId = s;
                if (!TextUtils.isEmpty(regId)) {
                    MixPushPlatform mixPushPlatform = new MixPushPlatform(HonorPushProvider.HONOR, regId);
                    handler.getPushReceiver().onRegisterSucceed(context, mixPushPlatform);
                }
            }

            @Override
            public void onFailure(int i, String s) {
                handler.getLogger().log(MixPushClient.TAG, "HonorPushProvider register onFailed: " + i + ", msg: " + s);
            }
        });
    }

    @Override
    public void unRegister(Context context) {
    }

    @Override
    public boolean isSupport(Context context) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        if (!manufacturer.equals(HONOR)) {
            return false;
        }
        HonorPushClient.getInstance().init(context, true);
        boolean supportHonorPush = HonorPushClient.getInstance().checkSupportHonorPush(context);
        return supportHonorPush;
    }


    @Override
    public String getPlatformName() {
        return HonorPushProvider.HONOR;
    }

    @Override
    public String getRegisterId(Context context) {
        return regId;
    }

}
