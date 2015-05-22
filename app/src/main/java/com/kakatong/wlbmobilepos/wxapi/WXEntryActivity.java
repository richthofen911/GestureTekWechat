package com.kakatong.wlbmobilepos.wxapi;


import com.kakatong.wlbmobilepos.Constants;
import com.kakatong.wlbmobilepos.GetFromWXActivity;
import com.kakatong.wlbmobilepos.HTTPClient;
import com.kakatong.wlbmobilepos.R;
import com.kakatong.wlbmobilepos.SendToWXActivity;
import com.kakatong.wlbmobilepos.ShowFromWXActivity;
import com.perples.recosdk.RECOBeacon;
import com.perples.recosdk.RECOBeaconManager;
import com.perples.recosdk.RECOBeaconRegion;
import com.perples.recosdk.RECOErrorCode;
import com.perples.recosdk.RECOProximity;
import com.perples.recosdk.RECORangingListener;
import com.perples.recosdk.RECOServiceConnectListener;
import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.constants.ConstantsAPI;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.modelmsg.ShowMessageFromWX;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.modelmsg.WXAppExtendObject;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

public class WXEntryActivity extends Activity implements RECOServiceConnectListener, RECORangingListener, IWXAPIEventHandler {
	
	private static final int TIMELINE_SUPPORTED_VERSION = 0x21020001;
	
	private Button btn_login;
	private ImageView iv_headimage;
	private EditText et_ip;

    private IWXAPI api;
	String urlGetToken;
	String urlHeadImage;
	String gender;
	String nickname;
	private boolean checkedIn = false;
	private boolean readyToRange = false;

	private final String APPID = "wxd562451653dc6162";
	private final String APPSECRET = "384cc80dfa0ebb75faafa589b2f819d7";
	private final String GRANTTYPE = "authorization_code";
	private String TOGETTOKEN_PREFIX = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=";
	private final String BEACONTOBEFOUND_UUID = "24DDF4118CF1440C87CDE368DAF9C93E";
	private final int BEACONTOBEFOUND_MAJOR = 54321;
	private final int BEACONTOBEFOUND_MINOR = 11111;

	protected RECOBeaconManager mRecoManager;
	protected ArrayList<RECOBeaconRegion> mRegions;

	public final boolean DISCONTINUOUS_SCAN = false;

	HTTPClient httpClient = new HTTPClient();


	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry);
		Log.e("app context: ", getApplicationContext().toString());
		mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), true, false);
		mRegions = this.generateBeaconRegion();
		mRecoManager.setRangingListener(this);
		mRecoManager.bind(this);

    	api = WXAPIFactory.createWXAPI(this, Constants.APP_ID, false);

		iv_headimage = (ImageView) findViewById(R.id.iv_headimage);
		et_ip = (EditText) findViewById(R.id.et_ip);
    	
        btn_login = (Button) findViewById(R.id.btn_login);
        btn_login.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				HTTPClient.ip = et_ip.getText().toString();
				final SendAuth.Req req = new SendAuth.Req();
				req.scope = "snsapi_userinfo";
				req.state = "getuserobject";
				api.sendReq(req);
			}
		});

        api.handleIntent(getIntent(), this);
    }
/*
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		setIntent(intent);
        api.handleIntent(intent, this);
	}
*/
	@Override
	public void onReq(BaseReq req) {
		switch (req.getType()) {
		case ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX:
			goToGetMsg();		
			break;
		case ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX:
			goToShowMsg((ShowMessageFromWX.Req) req);
			break;
		default:
			break;
		}
	}

	@Override
	public void onResp(BaseResp resp) {
		int result = 0;

		switch (resp.errCode) {
		case BaseResp.ErrCode.ERR_OK:
			SendAuth.Resp sendResp = (SendAuth.Resp) resp;
			Log.e("response authcode: ", "" + sendResp.code);
			urlGetToken = TOGETTOKEN_PREFIX + APPID + "&secret=" + APPSECRET + "&code=" + sendResp.code +
					"&grant_type=" + GRANTTYPE;
			AsyncGetToken asyncGetToken = new AsyncGetToken();
			asyncGetToken.execute();
			result = R.string.errcode_success;
			break;
		case BaseResp.ErrCode.ERR_USER_CANCEL:
			result = R.string.errcode_cancel;
			break;
		case BaseResp.ErrCode.ERR_AUTH_DENIED:
			result = R.string.errcode_deny;
			break;
		default:
			result = R.string.errcode_unknown;
			break;
		}
		
		Toast.makeText(this, result, Toast.LENGTH_LONG).show();
	}
	
	private void goToGetMsg() {
		Intent intent = new Intent(this, GetFromWXActivity.class);
		intent.putExtras(getIntent());
		startActivity(intent);
		finish();
	}
	
	private void goToShowMsg(ShowMessageFromWX.Req showReq) {
		WXMediaMessage wxMsg = showReq.message;
		WXAppExtendObject obj = (WXAppExtendObject) wxMsg.mediaObject;

		StringBuffer msg = new StringBuffer();
		msg.append("description: ");
		msg.append(wxMsg.description);
		msg.append("\n");
		msg.append("extInfo: ");
		msg.append(obj.extInfo);
		msg.append("\n");
		msg.append("filePath: ");
		msg.append(obj.filePath);

		Intent intent = new Intent(this, ShowFromWXActivity.class);
		intent.putExtra(Constants.ShowMsgActivity.STitle, wxMsg.title);
		intent.putExtra(Constants.ShowMsgActivity.SMessage, msg.toString());
		intent.putExtra(Constants.ShowMsgActivity.BAThumbData, wxMsg.thumbData);
		startActivity(intent);
		finish();
	}

	public class AsyncGetToken extends AsyncTask<String, String, String>
	{
		@Override
		protected String doInBackground(String... params)
		{
			httpClient.getRequestToken(urlGetToken);
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			AsyncGetUserInfo asyncGetUserInfo = new AsyncGetUserInfo();
			asyncGetUserInfo.execute();
		}
	}

	public class AsyncGetUserInfo extends AsyncTask<String, String, String>
	{
		@Override
		protected String doInBackground(String... params)
		{
			String urlGetUserInfo = "https://api.weixin.qq.com/sns/userinfo?access_token=" + httpClient.getToken() +
					"&openid=" + httpClient.getOpenid();
			httpClient.getRequestUserInfo(urlGetUserInfo);
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			urlHeadImage = httpClient.getHeadimage();
			nickname = httpClient.getNickname();
			gender = httpClient.getGender();
			AsyncGetUserImage asyncGetUserImage = new AsyncGetUserImage();
			asyncGetUserImage.execute();
			readyToRange = true;
			//HitServerAdd hitServerAdd = new HitServerAdd();
			//hitServerAdd.execute();
		}
	}

	public class AsyncGetUserImage extends AsyncTask<String, String, Bitmap>
	{
		@Override
		protected Bitmap doInBackground(String... params)
		{
			URL imageUrl;
			try{
				imageUrl = new URL(urlHeadImage);
				InputStream is = imageUrl.openConnection().getInputStream();
				Bitmap bitmap = BitmapFactory.decodeStream(is);
				return bitmap;
			}catch (Exception e){}

			return null;
		}

		@Override
		protected void onPostExecute(Bitmap newBitMap) {
			iv_headimage.setImageBitmap(newBitMap);
		}
	}

	@Override
	public void onServiceConnect() {
		Log.e("RangingActivity", "onServiceConnect()");
		mRecoManager.setDiscontinuousScan(this.DISCONTINUOUS_SCAN);
		this.start(mRegions);
		//Write the code when RECOBeaconManager is bound to RECOBeaconService
	}

	@Override
	public void didRangeBeaconsInRegion(Collection<RECOBeacon> recoBeacons, RECOBeaconRegion recoRegion) {
		if(!recoBeacons.isEmpty()){
			synchronized (recoBeacons){
				for(RECOBeacon recoBeacon: recoBeacons){
					if(readyToRange){
						if(recoBeacon.getProximity() == RECOProximity.RECOProximityImmediate || recoBeacon.getProximity() == RECOProximity.RECOProximityNear){
							if(!checkedIn){
								checkedIn = true;
								Toast.makeText(getApplicationContext(), "enter", Toast.LENGTH_SHORT).show();
								HitServerAdd hitServerAdd = new HitServerAdd();
								hitServerAdd.execute();
							}else{
								Log.e("checked in already", "");
							}
						}else{
							if(checkedIn){
								checkedIn = false;
								Toast.makeText(getApplicationContext(), "exit", Toast.LENGTH_SHORT).show();
								HitServerDel hitServerDel = new HitServerDel();
								hitServerDel.execute();
							}else {
								Log.e("checked out already", "");
							}
						}
					}else{
						Log.e("not ready to range", "");
					}
				}
			}
		}else {
			checkedIn = false;
			Toast.makeText(getApplicationContext(), "exit", Toast.LENGTH_SHORT).show();
			HitServerDel hitServerDel = new HitServerDel();
			hitServerDel.execute();
		}
	}

	protected void start(ArrayList<RECOBeaconRegion> regions) {
		Log.e("try to range", "");
		for(RECOBeaconRegion region : regions) {
			try {
				Log.e("a region: ", region.getProximityUuid());
				mRecoManager.startRangingBeaconsInRegion(region);
			} catch (RemoteException e) {
				Log.i("RECORangingActivity", "Remote Exception");
				e.printStackTrace();
			} catch (NullPointerException e) {
				Log.i("RECORangingActivity", "Null Pointer Exception");
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onServiceFail(RECOErrorCode errorCode) {
		//Write the code when the RECOBeaconService is failed.
		Log.e("errorcode: ", errorCode.toString());//See the RECOErrorCode in the documents.
		return;
	}

	@Override
	public void rangingBeaconsDidFailForRegion(RECOBeaconRegion region, RECOErrorCode errorCode) {
		//Write the code when the RECOBeaconService is failed to range beacons in the region.
		//See the RECOErrorCode in the documents.
		return;
	}

	private ArrayList<RECOBeaconRegion> generateBeaconRegion() {
		ArrayList<RECOBeaconRegion> regions = new ArrayList<>();

		RECOBeaconRegion recoRegion;
		//recoRegion = new RECOBeaconRegion(BEACONTOBEFOUND_UUID, "RECO Sample Region");
		recoRegion = new RECOBeaconRegion(BEACONTOBEFOUND_UUID, BEACONTOBEFOUND_MAJOR, "RECO Sample Region");
		//recoRegion = new RECOBeaconRegion(BEACONTOBEFOUND_UUID, BEACONTOBEFOUND_MAJOR, BEACONTOBEFOUND_MINOR, "RECO Sample Region");
		regions.add(recoRegion);
		Log.e("regions: ", regions.toString());
		return regions;
	}

	public class HitServerAdd extends AsyncTask<String, String, String>
	{
		@Override
		protected String doInBackground(String... params)
		{
			httpClient.postRequestAdd();
			return null;
		}
		@Override
		protected void onPostExecute(String result) {
		}
	}

	public class HitServerDel extends AsyncTask<String, String, String>
	{
		@Override
		protected String doInBackground(String... params)
		{
			httpClient.postRequestDel();
			return null;
		}
		@Override
		protected void onPostExecute(String result) {
		}
	}
}