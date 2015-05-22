package com.kakatong.wlbmobilepos;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.widget.Toast;

import com.kakatong.wlbmobilepos.wxapi.WXEntryActivity;

public class HTTPClient {

    public static String ip = "192.168.128.136";
    private String url_add = "http://" + ip + ":8888/gt2/dump.php";
    private String url_del = "http://" + ip + ":8888/gt2/storeout.php";


    private String token;
    private String openid;
    private String nickname;
    private String gender;
    private String headimage;

    public String getToken() { return token; }
    public String getOpenid() { return openid; }
    public String getNickname(){ return nickname; }
    public String getGender(){ return gender; }
    public String getHeadimage(){ return headimage; }


    public void parseForTokenAndId(String input) {
        //json = input.replace("\\", "");
        try {
            JSONObject tokenResults = new JSONObject(input);

            token = tokenResults.getString("access_token");
            openid = tokenResults.getString("openid");
            }
        catch (JSONException e) { e.printStackTrace(); }
    }

    public void parseForUserInfo(String input) {
        //json = input.replace("\\", "");
        try {
            JSONObject userinfo = new JSONObject(input);

            nickname = userinfo.getString("nickname");
            if(userinfo.getString("sex").equals("1")){
                gender = "male";
            }else {
                gender = "female";
            }
            headimage = userinfo.getString("headimgurl").replace("\\", "");
        }
        catch (JSONException e) { e.printStackTrace(); }
    }


    public String getRequestToken(String url) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(url);
        try{
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            String comeIn;
            if (entity != null){
                comeIn = EntityUtils.toString(entity, "UTF-8");
                parseForTokenAndId(comeIn);
            }
            else
                comeIn = "receiveError";
            return comeIn;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return "not received anything";
    }

    public String getRequestUserInfo(String url) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(url);
        try{
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            String comeIn;
            if (entity != null){
                comeIn = EntityUtils.toString(entity, "UTF-8");
                parseForUserInfo(comeIn);
            }
            else
                comeIn = "receiveError";
            return comeIn;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return "not received anything";
    }

    public String postRequestAdd() {
        //      CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url_add);
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
        nameValuePairs.add(new BasicNameValuePair("fbid", headimage));
        nameValuePairs.add(new BasicNameValuePair("firstName", nickname));
        nameValuePairs.add(new BasicNameValuePair("lastName", nickname));
        nameValuePairs.add(new BasicNameValuePair("gender", gender));
        try {
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            String comeIn;
            if (entity != null)
                comeIn = EntityUtils.toString(entity, "UTF-8");
            else
                comeIn = "receiveError";
            return comeIn;
        } catch (Exception e) {}
        return "not received anything";
    }

    public String postRequestDel() {
        //      CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url_del);
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
        nameValuePairs.add(new BasicNameValuePair("fbid", headimage));
        nameValuePairs.add(new BasicNameValuePair("firstName", nickname));
        nameValuePairs.add(new BasicNameValuePair("lastName", nickname));
        nameValuePairs.add(new BasicNameValuePair("gender", gender));
        try {
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            String comeIn;
            if (entity != null)
                comeIn = EntityUtils.toString(entity, "UTF-8");
            else
                comeIn = "receiveError";
            return comeIn;
        } catch (Exception e) {}
        return "not received anything";
    }
}
