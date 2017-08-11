package com.aylanetworks.aura.wxapi;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.aylanetworks.aura.Constants;
import com.aylanetworks.aura.R;
import com.aylanetworks.aylasdk.auth.AylaWeChatAuthProvider;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;


/**
 * Activity with no view to handle any responses or requests from WeChat. This activity can
 * never be moved or renamed or WeChat authentication won't work. Also notice that this
 * activity is being exported in the manifest. Lots of examples shows the WXEntryActivity
 * combined with an activity doing other things related to your app. However, this seemed like a poor
 * separation of functionality as well as something that's going to get ugly if you want interaction
 * with WeChat beyond authentication. Treating this activity as more of a broadcast listener keeps
 * things simple and detached from the rest of your application.
 */
public class WXEntryActivity extends Activity implements IWXAPIEventHandler {
    public static String token;

    private IWXAPI api;


    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        // Handle any communication from WeChat and then terminate activity. This class must be an activity
        // or the communication will not be received from WeChat.
        api = WXAPIFactory.createWXAPI(this, Constants.WECHAT_APP_ID, false);
        api.handleIntent(getIntent(), this);

        finish();
    }

    /**
     * Called when WeChat is initiating a request to your application. This is not used for
     * authentication.
     * @param req
     */
    @Override
    public void onReq(BaseReq req) {
    }

    /**
     * Called when WeChat is responding to a request this app initiated. Invoked by WeChat after
     * authorization has been given by the user.
     * @param resp
     */
    @Override
    public void onResp(BaseResp resp) {
        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                switch (resp.getType()) {
                    case ConstantsAPI.COMMAND_SENDAUTH:
                        SendAuth.Resp sendResp = (SendAuth.Resp) resp;
                        WXEntryActivity.token = sendResp.code;
                        AylaWeChatAuthProvider.activityDidAuthenticate(sendResp.code);
                        break;
                }
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                AylaWeChatAuthProvider.activityCancelAuth();
                Toast.makeText(this,  R.string.wechat_request_cancel, Toast.LENGTH_LONG).show();
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                Toast.makeText(this,  R.string.wechat_request_denied, Toast.LENGTH_LONG).show();
                break;
        }

    }

}