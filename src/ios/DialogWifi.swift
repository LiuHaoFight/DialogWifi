import Foundation
import NetworkExtension
import SystemConfiguration.CaptiveNetwork

@objc(DialogWifi) public class DialogWifi : CDVPlugin, GCDAsyncSocketDelegate  {

    var wifiConfiguration: NEHotspotConfiguration?

    var mSocket: GCDAsyncSocket!
    var mTimer: Timer?
    var timeCount: Int!
    var isCompleted = false
    var randomNo = ""
    var serialNo = ""
    
    var currentNetworkInfos: Array<NetworkInfo>? {
        get {
            return SSID.fetchNetworkInfo()
        }
    }
    
    override public func pluginInitialize() {
        timeCount = 0
    }

    override public func onAppTerminate() {
    }

    
    var connectCommand: CDVInvokedUrlCommand!
    @objc public func onConnectSocket(_ command: CDVInvokedUrlCommand) {

        self.connectCommand = command
        let host = command.argument(at: 0) as! String
        let port = command.argument(at: 1) as! UInt16
        let ssid = command.argument(at: 2) as! String
        let pw = command.argument(at: 3) as! String

        self.connectWifi(ssidS: ssid, pw: pw, host: host, port: port)
    }
    
    var disconnectCommand: CDVInvokedUrlCommand!
    @objc public func close(_ command: CDVInvokedUrlCommand) {

        self.disconnectCommand = command
        self.disconnectSocket()
    }
    
    var scanCommand: CDVInvokedUrlCommand!
    @objc public func scan(_ command: CDVInvokedUrlCommand) {
        self.scanCommand = command
        self.tcpSendReqRescan()
    }
    
    @objc public func sendDPMSet(_ command: CDVInvokedUrlCommand) {
        self.tcpSendDPMSet()
        let pluginResult = CDVPluginResult(status:CDVCommandStatus_OK, messageAs: "OK")
        self.commandDelegate?.send(pluginResult, callbackId: command.callbackId)
    }
    
    var configCommand: CDVInvokedUrlCommand!
    @objc public func sendSSIDPW(_ command: CDVInvokedUrlCommand) {
        self.configCommand = command
        let ssid = command.argument(at: 0) as! String
        let pwd = command.argument(at: 1) as! String
        let security = command.argument(at: 2) as! Int
        let hidden = command.argument(at: 3) as! Int
        let url = command.argument(at: 4) as! String
        let bind = command.argument(at: 5) as! Int
        self.tcpSendSSIDPW(ssid: ssid, pw: pwd, security: security, isHidden: hidden, serverURL: url, bind: bind)
    }
    
    // MARK: - WIFI Connection
    
    public func connectWifi(ssidS: String, pw: String, host: String, port: UInt16) {
        if (pw.isEmpty) {
            wifiConfiguration = NEHotspotConfiguration(ssid: ssidS)
        } else {
            wifiConfiguration = NEHotspotConfiguration(
                ssid: ssidS,
                passphrase: pw,
                isWEP: false)
        }
        wifiConfiguration?.joinOnce = true;
        NEHotspotConfigurationManager.shared.apply(wifiConfiguration!) { error in
            if let ssid = self.currentNetworkInfos?.first?.ssid {
                print("connected SSID: \(ssid)")
                if ssid == ssidS {
                    DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 5, execute: {
                        self.connectSocket(host:host, port:port)
                    })
                } else {
                    if (self.connectCommand != nil) {
                        let pluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: "error")
                        self.commandDelegate?.send(pluginResult, callbackId: self.connectCommand.callbackId)
                        self.connectCommand = nil
                    }

                }
            } else {
                if (self.connectCommand != nil) {
                    let pluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: "error")
                    self.commandDelegate?.send(pluginResult, callbackId: self.connectCommand.callbackId)
                    self.connectCommand = nil
                }
            }
            
        }
    }
    
    public struct NetworkInfo {
        var interface: String
        var success: Bool = false
        var ssid: String?
        var bssid: String?
    }
    
    public class SSID {
        class func fetchNetworkInfo() -> [NetworkInfo]? {
            if let interfaces: NSArray = CNCopySupportedInterfaces() {
                var networkInfos = [NetworkInfo]()
                for interface in interfaces {
                    let interfaceName = interface as! String
                    var networkInfo = NetworkInfo(interface: interfaceName,
                                                  success: false,
                                                  ssid: nil,
                                                  bssid: nil)
                    if let dict = CNCopyCurrentNetworkInfo(interfaceName as CFString) as NSDictionary? {
                        networkInfo.success = true
                        networkInfo.ssid = dict[kCNNetworkInfoKeySSID as String] as? String
                        networkInfo.bssid = dict[kCNNetworkInfoKeyBSSID as String] as? String
                    }
                    networkInfos.append(networkInfo)
                }
                return networkInfos
            }
            return nil
        }
    }
    
    // MARK: - TCP Connection
    
    public func connectSocket(host: String, port: UInt16) {
        do {
            mSocket = GCDAsyncSocket(delegate: self, delegateQueue: DispatchQueue.main)
            try mSocket.connect(toHost: host, onPort: port, withTimeout: 20)
        } catch let error {
            print(error)
            if self.connectCommand != nil {
                let pluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: "error")
                self.commandDelegate?.send(pluginResult, callbackId: self.connectCommand.callbackId)
            }
        }
    }
    
    public func disconnectSocket() {
        print("==> disconnectSocket()\n")
        if mSocket != nil {
            print("==>self.mSocket != nil\n")
            if mSocket.isConnected {
                print("==>self.mSocket.isConnected \n")
                mSocket.disconnect()
            }
            mSocket = nil
        }
    }
    
    public func socket(_ sock: GCDAsyncSocket, didAcceptNewSocket newSocket: GCDAsyncSocket) {
        print(">> didAcceptNewSocket!\n")
        mSocket = newSocket
    }
    
    public func socket(_ socket: GCDAsyncSocket, didConnectToHost host: String, port p:UInt16) {
        print(">> didConnectToHost!\n")
        let sslSettings = [
            GCDAsyncSocketManuallyEvaluateTrust: NSNumber(value: true),
            kCFStreamSSLPeerName: "www.apple.com"
        ] as? [CFString : Any]
        mSocket.startTLS(sslSettings as? [String:NSObject])
    }
    
    public func socket(_ sock: GCDAsyncSocket, didWriteDataWithTag tag: Int) {
        print(">> didWriteData");
    }
    
    public func socket(_ sock: GCDAsyncSocket, didReceive trust: SecTrust, completionHandler: @escaping (Bool) -> Void) {
        print(">> didReceiveData")
        completionHandler(true)
    }
    
    public func socket(_ sock: GCDAsyncSocket, didRead: Data, withTag tag:CLong){
        print(">> didRead!");
        do {
            let json = try JSON(data: didRead)
            print("incoming message: \(json))")
            
            if json["SOCKET_TYPE"].exists() {
                let socketType: String? = json["SOCKET_TYPE"].stringValue
                   print("socketType : \(String(describing: socketType!))")
                   if socketType == "0" || socketType == "1" {
                       mSocket.disconnect()
                   }
            }

            if json["thingName"].exists() {
                let thingName = json["thingName"].stringValue
                print("thingname : \(thingName)")
                // UserDefaults.standard.set(thingName, forKey: "thingNameKey")
            }

            if json["APList"].exists() {
                if self.scanCommand != nil {
                    print("APList : \(json["APList"].arrayValue)")
                    var apList = [NSDictionary]();
                    for item in json["APList"].arrayValue {
                        let index = item["index"].intValue
                        let SSID = item["SSID"].stringValue
                        let securityType = item["securityType"].intValue
                        let signal = item["signal"].intValue
                        let ap: NSDictionary = NSDictionary(
                           objects: [index, SSID, securityType, signal],
                           forKeys: ["index" as NSCopying, "SSID" as NSCopying, "securityType" as NSCopying, "signal" as NSCopying])
                        apList.append(ap);
                    }
                    let pluginResult = CDVPluginResult(status:CDVCommandStatus_OK, messageAs: apList)
                    self.commandDelegate?.send(pluginResult, callbackId: self.scanCommand.callbackId)
                    self.scanCommand = nil
                }
            }
            
            if json["randomNo"].exists() {
                randomNo = json["randomNo"].stringValue
                serialNo = json["SN"].stringValue
                print("randomNo : \(randomNo)")
                print("serialNo : \(serialNo)")
            }
            
            if json["RESULT_REBOOT"].exists() {
                let resultReboot:String? = json["RESULT_REBOOT"].stringValue
                print("resultReboot = \(String(describing: resultReboot!))")
                if (self.configCommand == nil) {
                    return
                }
                if resultReboot == "0"{
                    var array = [String]();
                    array.append(randomNo)
                    array.append(serialNo)
                    let pluginResult = CDVPluginResult(status:CDVCommandStatus_OK, messageAsMultipart: array)
                    self.commandDelegate?.send(pluginResult, callbackId: self.configCommand.callbackId)
                } else {
                    let pluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: "error")
                    self.commandDelegate?.send(pluginResult, callbackId: self.configCommand.callbackId)
                }
            }
            
        } catch let error {
            print(error)
        }
        
    }
    
    public func socketDidSecure(_ sock: GCDAsyncSocket) {
        print(">> socketDidSecure!")
        
        tcpSendConnected()
        
    }
    
    
    public func socketDidDisconnect(_ sock: GCDAsyncSocket, withError err: Error?) {
        print(">> didDisconnect!")
        if (!isCompleted) {
//                showToast(message: "Socket is not connected. Please retry.", seconds: 2.0)
        }
        if let error = err {
            print("Will disconnect with error: \(error)")
        } else{
            print("Success disconnect")
        }
        if mSocket != nil {
            print("==>mSocket != nil\n")
            if mSocket.isConnected {
                print("==>mSocket.isConnected \n")
                mSocket.disconnect()
            }
            mSocket = nil
        }
        if self.disconnectCommand != nil {
            let pluginResult = CDVPluginResult(status:CDVCommandStatus_OK, messageAs: "didDisconnect")
            self.commandDelegate?.send(pluginResult, callbackId: self.disconnectCommand.callbackId)
            self.disconnectCommand = nil;
        }
        if self.connectCommand != nil {
            let pluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: "didDisconnect")
            self.commandDelegate?.send(pluginResult, callbackId: self.connectCommand.callbackId)
            self.connectCommand = nil;
        }
    }
    
    
    //MARK: - Send TCP
    
//    func tcpSendSocketType(type: Int) {
//        print("==> tcpSendSocketType()\n")
//        let cmdSocketType = "{\"msgType\":6,\"SOCKET_TYPE\":\(type)}"
//        let data = cmdSocketType.data(using: String.Encoding.utf8)!
//        mSocket.write(data, withTimeout:10, tag: 0)
//        mSocket.readData(withTimeout: -1, tag: 0)
//
//    }
    
    func tcpSendConnected() {
        print("==> tcpSendConnected()\n")
        if (mSocket == nil) {
            return
        }
        let cmdConnected: String = "{\"msgType\":0,\"CONNECTED\":0}"
        let data = cmdConnected.data(using: String.Encoding.utf8)!
        mSocket.write(data, withTimeout:10, tag: 0)
        mSocket.readData(withTimeout: -1, tag: 0)
        if (self.connectCommand != nil) {
            let pluginResult = CDVPluginResult(status:CDVCommandStatus_OK, messageAs: "OK")
            pluginResult?.setKeepCallbackAs(true)
            self.commandDelegate?.send(pluginResult, callbackId: self.connectCommand.callbackId)
        }
    }
    
    func tcpSendReqRescan() {
        print("==> tcpSendReqRescan()\n")
        if (mSocket == nil) {
            return
        }
        let cmdReqRescan: String = "{\"msgType\":3,\"REQ_RESCAN\":0}"
        let data = cmdReqRescan.data(using: String.Encoding.utf8)!
        mSocket.write(data, withTimeout:10, tag: 0)
        mSocket.readData(withTimeout: -1, tag: 0)

    }
    
    func tcpSendDPMSet() {
        let cmdSetDpm: String = "{\"msgType\":5, \"REQ_SET_DPM\":0, \"sleepMode\":0, \"rtcTimer\":1740, \"useDPM\":0, \"dpmKeepAlive\":30000, \"userWakeUp\":0, \"timWakeup\":10}"
        print("tcpSendDPMSet : \(cmdSetDpm)")
        if (mSocket == nil) {
            return
        }
        let data = cmdSetDpm.data(using: String.Encoding.utf8)!
        mSocket.write(data, withTimeout:10, tag: 0)
        mSocket.readData(withTimeout: -1, tag: 0)
        
    }
    
    func tcpSendSSIDPW(ssid: String, pw: String, security: Int, isHidden: Int, serverURL: String, bind: Int) {
        let cmdSendSSIDPW: String = "{\"msgType\":1, \"SET_AP_SSID_PW\":0, \"ssid\":\"\(ssid)\", \"pw\":\"\(pw)\", \"securityType\":\(security),  \"isHidden\":\(isHidden),  \"bind\":\(bind), \"url\":\"\(serverURL)\"}"
        print("tcpSendSSIDPW : \(cmdSendSSIDPW)")
        if (mSocket == nil) {
            return
        }
        let data = cmdSendSSIDPW.data(using: String.Encoding.utf8)!
        mSocket.write(data, withTimeout:10, tag: 0)
        mSocket.readData(withTimeout: -1, tag: 0)
    }
    
}
