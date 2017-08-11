package com.aylanetworks.aura;

import android.content.Context;
import android.text.TextUtils;

import com.aylanetworks.aura.util.PushUtils;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaPropertyTriggerApp;
import com.aylanetworks.aylasdk.model.AylaBaiduNotification;
import com.baidu.android.pushservice.PushMessageReceiver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

/**
 * Aura
 * <p>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */
public class BaiduPushMessageReceiver extends PushMessageReceiver {
    private static final String LOG_TAG = "BaiduPushMessageReceiver";

    private static String _baiduAppID;
    private static String _userId;
    private static String _channelId;

    public static String getBaiduAppID() {
        return _baiduAppID;
    }

    public static String getUserId() {
        return _userId;
    }

    public static String getChannelId() {
        return _channelId;
    }

    /**
     * After PushManager.startWork() is called, sdk would send binding request to push server, which
     * is asynchronised process. The result of the binding request is returned via onBind.
     *
     * If you need single broadcast push, need to upload user_id and channel_id to application server
     * and call server interface to push messages to single device or user with the given channel_id
     * and user_id
     * */
    @Override
    public void onBind(
            Context context
            , int errorCode
            , String appId
            , String userId
            , String channelId
            , String requestId ) {
        StringBuilder sb = new StringBuilder();
        sb.append("onBind errorCode=").append(errorCode)
                .append(" appid=").append(appId)
                .append(" userId=").append(userId)
                .append(" channelId=").append(channelId)
                .append(" requestId=").append(requestId);

        AylaLog.d(LOG_TAG, sb.toString());

        // If binding successfully, set binding flag to reduce unnecessary binding reqeust
        if (errorCode == 0) {
            PushUtils.setBind(context, true);
            BaiduPushMessageReceiver._baiduAppID = appId;
            BaiduPushMessageReceiver._userId = userId;
            BaiduPushMessageReceiver._channelId = channelId;
            //TODO: Guess we can persist the info.
        }

        // When pushing messages to Baidu_Push Service via server side interface,
        // like REST or SDK, you will need to send channel_id and access_token (or app_id
        // and user_id) to app server. If you use Baidu_Push Developer Console to send
        // messages, this is not necessary.
        // TODO: implement your own logic.
    }

    /**
     * callback for PushManager.stopWork().
     * */
    @Override
    public void onUnbind(
            Context context
            , int errorCode
            , String requestId ) {
        StringBuilder sb = new StringBuilder();
        sb.append("onUnbind errorCode=").append(errorCode)
                .append(" requestId = ").append(requestId);
        AylaLog.d(LOG_TAG, sb.toString());

        // unbind successfully, set unbind flag
        if (errorCode == 0) {
            PushUtils.setBind(context, false);
        }
        //TODO: your own logic
    }

    /**
     * Callback to receive transparent transmission message.
     * @param context
     * @param message
     * @param customContent
     * */
    @Override
    public void onMessage(Context context, String message, String customContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("transparent transmission message:\"").append(message)
                .append("\" customContent:").append(customContent);
        AylaLog.d(LOG_TAG, sb.toString());

        // Note that customContent can only be set on app Server, which is Ayla Server for MCA.
        handleBaiduMessage(context, message, customContent);
    }


    private void handleBaiduMessage(
            Context context
            , final String message
            , final String customContent) {
        if (context == null || TextUtils.isEmpty(message)) {
            return;
        }

        Gson gson = new GsonBuilder().create();
        try {
            AylaBaiduNotification baiduNotify
                    = gson.fromJson(message, AylaBaiduNotification.class);
            if (baiduNotify == null) {
                AylaLog.e(LOG_TAG, "BaiduNotify parsing error.");
                return;
            }
            AylaPropertyTriggerApp.AylaBaiduMessage baiduMsg
                    = gson.fromJson(
                    baiduNotify.getDescription(),
                    AylaPropertyTriggerApp.AylaBaiduMessage.class);
            if (baiduMsg == null) {
                AylaLog.e(LOG_TAG, "BaiduMsg parsing error.");
                return;
            }
            switch (baiduMsg.getMsgType()) {
                case 0: // for normal message
                    PushUtils.sendNotification(context, baiduMsg.getMsg(), baiduMsg.getSound());
                    break;
                default:
                    AylaLog.d(LOG_TAG, "onMessage, unhandled case, message:" + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }
    }


    /**
     * Callback to receive notification clicks. Note that app can not retrieve notification content
     * via the interface before the pushed notification is clicked by users.
     *
     * @param context
     * @param title
     * @param description
     * @param customContent
     * */
    @Override
    public void onNotificationClicked(
            Context context
            , String title
            , String description
            , String customContent ) {
        StringBuilder sb = new StringBuilder();
        sb.append("clicked notification title=\"").append(title)
                .append("\" description=\"").append(description)
                .append("\" customContent=").append(customContent);

        AylaLog.d(LOG_TAG, sb.toString());

        //TODO: customized content extraction from customContent, and implement your own logic.
    }


    /**
     * Not used for now, need to override, or gradle would report error.
     * */
    @Override
    public void onSetTags(Context context, int errorCode,
                          List<String> sucessTags, List<String> failTags, String requestId) {
        String responseString = "onSetTags errorCode=" + errorCode
                + " sucessTags=" + sucessTags + " failTags=" + failTags
                + " requestId=" + requestId;

        AylaLog.d(LOG_TAG, responseString);
    }


    /**
     * Not used for now, need to override, or gradle would report error.
     * */
    @Override
    public void onDelTags(Context context, int errorCode,
                          List<String> sucessTags, List<String> failTags, String requestId) {
        String responseString = "onDelTags errorCode=" + errorCode
                + " sucessTags=" + sucessTags + " failTags=" + failTags
                + " requestId=" + requestId;

        AylaLog.d(LOG_TAG, responseString);
    }


    /**
     * Not used for now, need to override, or gradle would report error.
     * */
    @Override
    public void onListTags(Context context, int errorCode, List<String> tags,
                           String requestId) {
        String responseString = "onListTags errorCode=" + errorCode + " tags="
                + tags;
        AylaLog.d(LOG_TAG, responseString);
    }

    /**
     * Not used for now, need to override, or gradle would report error.
     * */
    @Override
    public void onNotificationArrived(Context arg0, String arg1, String arg2,
                                      String arg3) {
        // TODO Auto-generated method stub
    }
}






