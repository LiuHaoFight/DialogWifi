package com.siemens.plugins.DialogWifi;

import android.util.Log;

import com.koushikdutta.async.AsyncSSLSocket;
import com.koushikdutta.async.AsyncSSLSocketWrapper;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class DialogWifiImpl {
  private static final String TAG = DialogWifi.class.getSimpleName();
  private boolean mIsSocketConnected = false;

  //socket
  private AsyncSSLSocket mSSLSocket = null;
  private SSLParameters sslParameters;
  public String mHost;
  public Int mPort;
  private ISocketStatusCallback mSocketStatusCallback;

  public interface ISocketStatusCallback {

    void onConnectResult(boolean connected);

    void onClosed();

    void onDateReport(JSONObject jsonObject);
  }

  public interface IWriteCallback {
    void onWriteResult(boolean result);
  }


  public void setHost(String host, Int port) {
    this.mHost = host;
    this.mPort = port;
  }

  public void write(JSONObject object, CompletedCallback callback) {
    if (mSSLSocket != null && mSSLSocket.isOpen()) {
      Log.i(TAG, "Socket is opened");
    } else {
      Log.e(TAG, "Socket is closed");
      callback.onCompleted(new Exception());
      return;
    }
    byte[] byteMsg = object.toString().getBytes(StandardCharsets.UTF_8);
    if (byteMsg == null) {
      return;
    }
    com.koushikdutta.async.Util.writeAll(mSSLSocket, byteMsg, callback);
  }

  /**
   * Socket connection initialize.
   */
  public void onInitSocket(ISocketStatusCallback callback) {
    Log.i(TAG, "onInitSocket()");
    this.mSocketStatusCallback = callback;
    if (mIsSocketConnected || mSSLSocket != null) {
      Log.e(TAG, "Socket already connected.");
      if (this.mSocketStatusCallback != null) {
        this.mSocketStatusCallback.onConnectResult(true);
      }
      return;
    }

    mIsSocketConnected = true;
    if (mSSLSocket == null || !mSSLSocket.isOpen()) {

      new Runnable() {
        @Override
        public void run() {
          AsyncServer.getDefault().connectSocket(new InetSocketAddress(mHost, mPort),
            (Exception ex, final AsyncSocket socket) -> {
              Log.i(TAG, "AP Server Connection Completed");
              if (socket != null && ex == null) {
                handleConnectCompletedWithTLS(socket);
              } else {
                Log.e(TAG, "Socket is not connected.");
                mIsSocketConnected = false;
                if (mSocketStatusCallback != null) {
                  mSocketStatusCallback.onConnectResult(false);
                }
              }
            });
        }
      }.run();
    } else {
      Log.i(TAG, "Socket is already Opened.");
    }
  }

  public void onClose(ISocketStatusCallback callback) {
    if (mSSLSocket != null) {
      mSSLSocket.close();
    }
  }

  /**
   * Socket Connect Handler Registration With TLS
   */
  private void handleConnectCompletedWithTLS(final AsyncSocket socket) {
    Log.i(TAG, "handleConnectCompletedWithTLS");
    SSLContext sslCtx;
    SSLEngine sslEng = null;
    TrustManager[] tm = null;

    try {
      tm = new TrustManager[]{createTrustMangerForAll()};
      try {
        sslParameters = SSLContext.getDefault().getDefaultSSLParameters();
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      }
      Log.d(TAG, "sslParameters.getProtocols() = " + Arrays.toString(sslParameters.getProtocols()));
      sslCtx = SSLContext.getInstance("TLSv1.2");
      sslCtx.init(null, tm, new SecureRandom());
      sslEng = sslCtx.createSSLEngine();
    } catch (Exception e) {
      Log.e(TAG, "SSLContext Init Unknown Exception Error");
      e.printStackTrace();
    }

    AsyncSSLSocketWrapper.handshake(socket, mHost, 9900,
      sslEng, tm, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER, true,
      (Exception e, AsyncSSLSocket sslSocket) -> {
        Log.i(TAG, "onHandshakeComplete");

        if (e != null || sslSocket == null) {
          Log.e(TAG, "onHandshakeCompleted Error");
          e.printStackTrace();
          mIsSocketConnected = false;
          mSSLSocket = null;
          if (mSocketStatusCallback != null) {
            mSocketStatusCallback.onConnectResult(false);
          }
          return;
        }


        mSSLSocket = sslSocket;
        sslSocket.setWriteableCallback(() -> Log.i(TAG, "Writeable CallBack"));

        //Data Callback from Device AP
        sslSocket.setDataCallback((DataEmitter emitter, ByteBufferList bb) -> {
          Log.i(TAG, "DataCallback");
          if (bb != null) {
            Log.i(TAG, "[TLS] received : " + bb.toString());
            try {
              JSONObject jsonObject = new JSONObject(bb.readString());
              if (mSocketStatusCallback != null) {
                mSocketStatusCallback.onDateReport(jsonObject);
              }
            } catch (JSONException e1) {
              e1.printStackTrace();
            }
          } else {
            Log.i(TAG, "input is null~~");
          }
        });

        //Socket close Callback
        sslSocket.setClosedCallback(ex1 -> {
          Log.i(TAG, "Socket Closed");
          if (ex1 != null) {
            Log.e(TAG, "ClosedCallback Error");
            ex1.printStackTrace();
          }
          if (mSocketStatusCallback != null) {
            mSocketStatusCallback.onClosed();
          }
        });

        sslSocket.setEndCallback(ex12 -> {
          Log.i(TAG, "Socket End");
          if (ex12 != null) {
            Log.e(TAG, "EndCallback Error");
            ex12.printStackTrace();
          }
          if (mSocketStatusCallback != null) {
            mSocketStatusCallback.onClosed();
          }
        });
        if (mSocketStatusCallback != null) {
          mSocketStatusCallback.onConnectResult(true);
        }
      });
  }

  /**
   * Create TrustManager for All Cert
   */
  private TrustManager createTrustMangerForAll() {
    return new X509TrustManager() {
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      public void checkClientTrusted(X509Certificate[] certs, String authType) {
      }

      public void checkServerTrusted(X509Certificate[] certs, String authType) {
      }
    };
  }
}
