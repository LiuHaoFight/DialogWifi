package com.siemens.plugins.DialogWifi;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class DialogWifi extends CordovaPlugin {

  private static final String TAG = DialogWifi.class.getSimpleName();
  private final DialogWifiImpl mDialogWifiImpl = new DialogWifiImpl();
  private CallbackContext mConnectCallback;
  private CallbackContext mCloseCallback;
  private CallbackContext mDPMConfigCallback;
  private CallbackContext mWifiConfigCallback;

  private final DialogWifiImpl.ISocketStatusCallback mCallback = new DialogWifiImpl.ISocketStatusCallback() {
    @Override
    public void onConnectResult(boolean connected) {
      if (mConnectCallback != null) {
        if (connected) {
          mConnectCallback.success();
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
    }

    @Override
    public void onDateReport(JSONObject jsonObject) {
      if (jsonObject.has("SOCKET_TYPE")) {
        receiveSocketType(jsonObject);
      }

      if (jsonObject.has("thingName")) {
        receiveThingName(jsonObject);
      }

      if (jsonObject.has("mode")) {
        receiveMode(jsonObject);
      }

      if (jsonObject.has("SET_AP_SSID_PW")) {
        receiveSetApSSIDPW(jsonObject);
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
        this.onConnectSocket(host, callbackContext);
        return true;
      case "sendDPMSet":
        this.sendDPMSet(callbackContext);
        return true;
      case "close":
        this.onClose(callbackContext);
        return true;
      case "sendSSIDPW":
        String _ssid = args.getString(0);
        String _pwd = args.getString(1);
        int _isHidden = args.getInt(2);
        String _url = args.getString(3);
        this.sendSSIDPW(_ssid, _pwd, _isHidden, _url, callbackContext);
        return true;
    }
    return false;
  }

  /**
   * Socket Connect to AP Server after Device Wifi AP Connection Established.
   */
  public void onConnectSocket(String host, CallbackContext callbackContext) {
    Log.i(TAG, "onConnectSocket()" + host);
    mConnectCallback = callbackContext;
    mDialogWifiImpl.setHost(host);
    mDialogWifiImpl.onInitSocket(mCallback);
  }

  public void onClose(CallbackContext callbackContext) {
    Log.i(TAG, "onClose()");
    mCloseCallback = callbackContext;
    mDialogWifiImpl.onClose(mCallback);
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

  public void sendSSIDPW(String _ssid, String _pwd, int _isHidden, String _url, CallbackContext callbackContext) {
    Log.i(TAG, "sendSSIDPW");
    this.mWifiConfigCallback = callbackContext;
    final JSONObject obj = new JSONObject();
    try {
      obj.put("msgType", 1);
      obj.put("SET_AP_SSID_PW", 0);
      obj.put("ssid", _ssid);
      obj.put("pw", _pwd);
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
          mWifiConfigCallback.success();
          return;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    mWifiConfigCallback.error("");
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
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
