package com.siemens.plugins.DialogWifi;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This class echoes a string called from JavaScript.
 */
public class DialogWifi extends CordovaPlugin {

  private static final String TAG = DialogWifi.class.getSimpleName();
  private final DialogWifiImpl mDialogWifiImpl = new DialogWifiImpl();
  private CallbackContext mConnectCallback;
  private CallbackContext mCloseCallback;
  private CallbackContext mWifiListCallback;
  private CallbackContext mDPMConfigCallback;
  private CallbackContext mWifiConfigCallback;

  private String randomNo = "";
  private String serialNo = "";

  private final DialogWifiImpl.ISocketStatusCallback mCallback = new DialogWifiImpl.ISocketStatusCallback() {
    @Override
    public void onConnectResult(boolean connected) {
      if (mConnectCallback != null) {
        if (connected) {
          sendConnected();
          PluginResult result = new PluginResult(Status.OK, "");
          result.setKeepCallback(true);
          mConnectCallback.sendPluginResult(result);
        } else {
          mConnectCallback.error("");
        }
      }
    }

    @Override
    public void onClosed() {
      if (mCloseCallback != null) {
        mCloseCallback.success();
      }
      if (mConnectCallback != null) {
        mConnectCallback.error("");
      }
    }

    @Override
    public void onDateReport(JSONObject jsonObject) {
      Log.i(TAG, "== onDateReport ==" + jsonObject.toString());
      if (jsonObject.has("SOCKET_TYPE")) {
        receiveSocketType(jsonObject);
      }

      if (jsonObject.has("thingName")) {
        receiveThingName(jsonObject);
      }

      if (jsonObject.has("mode")) {
        receiveMode(jsonObject);
      }

      if (jsonObject.has("APList")) {
        receiveAPList(jsonObject);
      }

      if (jsonObject.has("SET_AP_SSID_PW")) {
        receiveSetApSSIDPW(jsonObject);
      }

      if (jsonObject.has("randomNo")) {
        receiveSerialNo(jsonObject);
      }

      if (jsonObject.has("RESULT_REBOOT")) {
        receiveRebootResult(jsonObject);
      }
    }
  };

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    switch (action) {
      case "onConnectSocket":
        String host = args.getString(0);
        int port = args.getInt(1);
        this.onConnectSocket(host, port, callbackContext);
        return true;
      case "sendDPMSet":
        this.sendDPMSet(callbackContext);
        return true;
      case "close":
        this.onClose(callbackContext);
        return true;
      case "scan":
        this.scan(callbackContext);
        return true;
      case "sendSSIDPW":
        String _ssid = args.getString(0);
        String _pwd = args.getString(1);
        int _security = args.getInt(2);
        int _isHidden = args.getInt(3);
        String _url = args.getString(4);
        this.sendSSIDPW(_ssid, _pwd, _security, _isHidden, _url, callbackContext);
        return true;
    }
    return false;
  }

  /**
   * Socket Connect to AP Server after Device Wifi AP Connection Established.
   */
  public void onConnectSocket(String host, int port, CallbackContext callbackContext) {
    Log.i(TAG, "onConnectSocket()" + host + " " + port);
    mConnectCallback = callbackContext;
    mDialogWifiImpl.setHost(host, port);
    mDialogWifiImpl.onInitSocket(mCallback);
  }

  public void onClose(CallbackContext callbackContext) {
    Log.i(TAG, "onClose()");
    mCloseCallback = callbackContext;
    mDialogWifiImpl.onClose();
  }

  public void scan(CallbackContext callbackContext) {
    Log.i(TAG, "scan()");
    mWifiListCallback = callbackContext;
    final JSONObject obj = new JSONObject();
    try {
      obj.put("msgType", 3);
      obj.put("REQ_RESCAN", 0);
    } catch (Exception e) {
      e.printStackTrace();
    }
    mDialogWifiImpl.write(obj, ex -> {
      if (ex != null) {
        Log.e(TAG, "Sending message error");
        ex.printStackTrace();
      } else {
        Log.i(TAG, "Sending message Completed");
      }
    });
  }

  public void sendConnected() {
    final JSONObject obj = new JSONObject();
    try {
      obj.put("msgType", 0);
      obj.put("CONNECTED", 0);
    } catch (Exception e) {
      e.printStackTrace();
    }
    mDialogWifiImpl.write(obj, ex -> {
      if (ex != null) {
        Log.e(TAG, "Sending message error");
        ex.printStackTrace();
      } else {
        Log.i(TAG, "Sending message Completed");
      }
    });
  }

  public void sendDPMSet(CallbackContext callbackContext) {
    Log.i(TAG, "sendDPMSet ");
    mDPMConfigCallback = callbackContext;
    final JSONObject obj = new JSONObject();
    try {
      obj.put("msgType", 5);
      obj.put("REQ_SET_DPM", 0);
      obj.put("sleepMode", 0);
      obj.put("rtcTimer", 1740);
      obj.put("useDPM", 0);
      obj.put("dpmKeepAlive", 30000);
      obj.put("userWakeUp", 0);
      obj.put("timWakeup", 10);
    } catch (Exception e) {
      e.printStackTrace();
    }

    mDialogWifiImpl.write(obj, ex -> {
      if (ex != null) {
        Log.e(TAG, "Sending message error");
        ex.printStackTrace();
      } else {
        Log.i(TAG, "Sending message Completed");
      }
    });
  }

  public void sendSSIDPW(String _ssid, String _pwd, int _security, int _isHidden, String _url,
      CallbackContext callbackContext) {
    Log.i(TAG, "sendSSIDPW");
    this.mWifiConfigCallback = callbackContext;
    final JSONObject obj = new JSONObject();
    try {
      obj.put("msgType", 1);
      obj.put("SET_AP_SSID_PW", 0);
      obj.put("ssid", _ssid);
      obj.put("pw", _pwd);
      obj.put("securityType", _security);
      obj.put("isHidden", _isHidden);
      obj.put("url", _url);
    } catch (Exception e) {
      e.printStackTrace();
    }
    mDialogWifiImpl.write(obj, ex -> {
      if (ex != null) {
        Log.e(TAG, "Sending message error");
        ex.printStackTrace();
      } else {
        Log.i(TAG, "Sending message Completed");
      }
    });
  }

  public void receiveSetApSSIDPW(JSONObject obj) {
    if (obj != null) {
      Log.i(TAG, "== receiveSetApSSIDPW ==");
      try {
        if (obj.getInt("SET_AP_SSID_PW") != -1) {
          Log.i(TAG, "SET_AP_SSID_PW = " + obj.getInt("SET_AP_SSID_PW"));
          return;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (mWifiConfigCallback != null) {
      mWifiConfigCallback.error("SET_AP_SSID_PW error");
    }
  }

  public void receiveSocketType(JSONObject obj) {
    if (obj != null) {
      Log.i(TAG, "== receiveSocketType() ==");
      try {
        Log.i(TAG, "socketType = " + obj.getInt("SOCKET_TYPE"));

      } catch (Exception e) {
        e.printStackTrace();
      }

    }
  }

  public void receiveThingName(JSONObject obj) {
    if (obj != null) {
      Log.i(TAG, "== receiveThingName() ==");
      try {
        Log.i(TAG, "thingName = " + obj.getString("thingName"));
      } catch (Exception e) {
        e.printStackTrace();
      }

    }
  }

  public void receiveMode(JSONObject obj) {
    if (obj != null) {
      Log.i(TAG, "== receiveMode() ==");
      try {
        Log.i(TAG, "mode = " + obj.getInt("mode"));
      } catch (Exception e) {
        e.printStackTrace();
      }

    }
  }

  public void receiveRebootResult(JSONObject obj) {

    if (obj != null) {
      Log.i(TAG, "== receiveRebootResult ==");
      try {
        if (obj.getInt("RESULT_REBOOT") != -1) {
          Log.i(TAG, "RESULT_REBOOT = " + obj.getInt("RESULT_REBOOT"));
          mDialogWifiImpl.onClose();
          if (mWifiConfigCallback != null) {
            List<PluginResult> resultList = new ArrayList<PluginResult>();
            PluginResult randomNoResult = new PluginResult(PluginResult.Status.OK, randomNo);
            PluginResult serialNoResult = new PluginResult(PluginResult.Status.OK, serialNo);
            resultList.add(randomNoResult);
            resultList.add(serialNoResult);
            mWifiConfigCallback.sendPluginResult(new PluginResult(PluginResult.Status.OK, resultList));
            return;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (mWifiConfigCallback != null) {
      mWifiConfigCallback.error("reboot error");
    }
  }

  public void receiveAPList(JSONObject jsonObject) {

    Log.i(TAG, "== receiveAPList() ==");
    JSONArray jsonArray;
    try {
      jsonArray = jsonObject.getJSONArray("APList");
      if (mWifiListCallback != null) {
        mWifiListCallback.success(jsonArray);
      }
    } catch (JSONException e) {
      e.printStackTrace();
      if (mWifiListCallback != null) {
        mWifiListCallback.error(e.getMessage());
      }
    }
  }

  public void receiveSerialNo(JSONObject obj) {
    if (obj != null) {
      Log.i(TAG, "== receiveSerialNo() ==");
      try {
        randomNo = obj.getString("randomNo");
        serialNo = obj.getString("SN");
        Log.i(TAG, "randomNo = " + randomNo);
        Log.i(TAG, "serialNo = " + serialNo);
      } catch (Exception e) {
        e.printStackTrace();
      }

    }
  }
}
