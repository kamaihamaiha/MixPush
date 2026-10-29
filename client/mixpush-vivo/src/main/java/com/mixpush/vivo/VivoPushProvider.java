package com.mixpush.vivo;

import android.app.Activity;
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
import com.vivo.push.listener.IPushSubscribeDialogCallback;
import com.vivo.push.subscribedialog.ChannelDisplay;
import com.vivo.push.subscribedialog.ChannelOperation;
import com.vivo.push.util.VivoPushException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VivoPushProvider extends BaseMixPushProvider {
    MixPushHandler handler = MixPushClient.getInstance().getHandler();
    public static final String VIVO = "vivo";
    public static final int CHANNEL_DISPLAYED = ChannelDisplay.DISPLAYED;
    public static final int CHANNEL_ALREADY_CREATED = ChannelDisplay.NOT_DISPLAYED_CHANNEL_CREATED;
    public static final int CHANNEL_ALLOWED = ChannelOperation.OPERATION_USER_ALLOW;
    public static final int CHANNEL_REJECTED = ChannelOperation.OPERATION_USER_REJECT;
    public static final int CHANNEL_REJECTED_THRESHOLD = ChannelOperation.OPERATION_REJECT_THRESHOLD;
    public static String TAG = "v-i-v-o";
    private static volatile String regId;
    private static volatile SubscribeResultListener subscribeResultListener;

    /** resultCode=0 表示预检通过或弹窗拉起，不代表用户已授权。 */
    public interface SubscribeDialogCallback {
        void onDialogResult(int resultCode, String message, Map<String, Integer> channelDisplayStatuses);
    }

    /**
     * 在 Application 中注册，以便接收器启动时也能收到用户操作结果。
     * operateResult 是整个弹窗的操作码；channelOperations 包含每个渠道的实际授权结果。
     */
    public interface SubscribeResultListener {
        void onSubscribeDialogResult(int operateResult, Map<String, Integer> channelOperations);
    }

    public static void setSubscribeResultListener(SubscribeResultListener listener) {
        subscribeResultListener = listener;
    }

    public static void checkCanShowSubscribeDialog(Context context, List<String> channelIds,
                                                   SubscribeDialogCallback callback) {
        if (context == null || callback == null) {
            throw new IllegalArgumentException("context and callback must not be null");
        }
        PushClient.getInstance(context).checkCanShowSubscribeDialog(
                context, channelIds, wrapSubscribeDialogCallback(callback));
    }

    public static void showSubscribeDialog(Activity activity, List<String> channelIds,
                                           SubscribeDialogCallback callback) {
        if (activity == null || callback == null) {
            throw new IllegalArgumentException("activity and callback must not be null");
        }
        PushClient.getInstance(activity).showSubscribeDialog(
                activity, channelIds, wrapSubscribeDialogCallback(callback));
    }

    private static IPushSubscribeDialogCallback wrapSubscribeDialogCallback(SubscribeDialogCallback callback) {
        return (resultCode, message, displays) -> {
            Map<String, Integer> statuses = new LinkedHashMap<>();
            if (displays != null) {
                for (ChannelDisplay display : displays) {
                    if (display != null && display.getChannelId() != null) {
                        statuses.put(display.getChannelId(), display.getDisplayStatus());
                    }
                }
            }
            callback.onDialogResult(resultCode, message, Collections.unmodifiableMap(statuses));
        };
    }

    static void dispatchSubscribeDialogResult(int operateResult, Map<String, Integer> channelOperations) {
        SubscribeResultListener listener = subscribeResultListener;
        if (listener != null) {
            listener.onSubscribeDialogResult(operateResult, channelOperations);
        }
    }

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
