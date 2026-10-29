package com.mixpush.vivo;

import android.content.Context;
import android.os.Build;

import com.mixpush.core.BaseMixPushProvider;
import com.mixpush.core.MixPushPlatform;
import com.mixpush.core.RegisterType;
import com.mixpush.core.MixPushClient;
import com.mixpush.core.MixPushHandler;
import com.vivo.push.PushClient;
import com.vivo.push.PushConfig;
import com.vivo.push.listener.IPushQueryActionListener;
import com.vivo.push.util.VivoPushException;

public class VivoPushProvider extends BaseMixPushProvider {
    MixPushHandler handler = MixPushClient.getInstance().getHandler();
    public static final String VIVO = "vivo";
    public static String TAG = "v-i-v-o";
    private static volatile String regId;

    @Override
    public void register(Context context, RegisterType type) {
        handler.getLogger().log(TAG, "initialize");
        Context appContext = context.getApplicationContext();
        PushClient client = PushClient.getInstance(appContext);
        try {
            // 调用 register 前，应用应已取得隐私政策同意。
            client.initialize(new PushConfig.Builder().agreePrivacyStatement(true).build());
            client.turnOnPush(state -> {
                if (state != 0) {
                    handler.getLogger().log(TAG, "开启失败: " + state);
                    return;
                }
                handler.getLogger().log(TAG, "开启成功");
                // 部分设备不会触发 onReceiveRegId，主动异步查询作为补充。
                client.getRegId(new IPushQueryActionListener() {
                    @Override
                    public void onSuccess(String value) {
                        if (value == null || value.isEmpty()) {
                            handler.getLogger().log(TAG, "获取 regId 为空");
                            return;
                        }
                        onRegIdReceived(appContext, value);
                    }

                    @Override
                    public void onFail(Integer errorCode) {
                        handler.getLogger().log(TAG, "获取 regId 失败: " + errorCode);
                    }
                });
            });
        } catch (VivoPushException e) {
            handler.getLogger().log(TAG, "vivo 初始化失败", e);
        }
    }

    static void onRegIdReceived(Context context, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        regId = value;
        MixPushPlatform platform = new MixPushPlatform(VIVO, value);
        MixPushClient.getInstance().getHandler().getPushReceiver().onRegisterSucceed(context, platform);
    }

    @Override
    public void unRegister(Context context) {
        PushClient.getInstance(context).turnOffPush(state -> {
            if (state == 0) {
                regId = null;
            }
        });
    }

    @Override
    public String getRegisterId(Context context) {
        // 新版 SDK 仅提供异步 getRegId；注册回调会更新此缓存。
        return regId;
    }

    @Override
    public boolean isSupport(Context context) {
        if (android.os.Build.VERSION.SDK_INT < 15) {
            return false;
        }
        String brand = Build.BRAND.toLowerCase();
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        if (manufacturer.equals("vivo") || brand.contains("vivo") || brand.contains("iqoo")) {
            return PushClient.getInstance(context).isSupport();
        }
        return false;
    }

    @Override
    public String getPlatformName() {
        return VivoPushProvider.VIVO;
    }
}
