import Foundation

@objc(DialogWifi) public class DialogWifi : CDVPlugin, GCDAsyncSocketDelegate  {

    var mSocket: GCDAsyncSocket!
    var mTimer:Timer?
    var timeCount = 0
    var isCompleted = false
    
    let hostAddress = "10.0.0.1"
    let tlsHostPort: UInt16 = 9900
    
    override public func pluginInitialize() {
    }

    override public func onAppTerminate() {
    }

    
    var connectCommand: CDVInvokedUrlCommand!
    @objc public func onConnectSocket(_ command: CDVInvokedUrlCommand) {

        self.connectCommand = command
        self.connectSocket()
    }
    
    var disconnectCommand: CDVInvokedUrlCommand!
    @objc public func close(_ command: CDVInvokedUrlCommand) {

        self.disconnectCommand = command
        self.disconnectSocket()
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
        let hidden = command.argument(at: 2) as! Int
        let url = command.argument(at: 3) as! String
        self.tcpSendSSIDPW(ssid: ssid, pw: pwd, isHidden: hidden, serverURL: url)
    }
    
    // MARK: - TCP Connection
    
    public func connectSocket() {
        print("==> connectSocket()\n")
        do {
            self.mSocket = GCDAsyncSocket(delegate: self, delegateQueue: DispatchQueue.main)
            try self.mSocket.connect(toHost: hostAddress, onPort: tlsHostPort, withTimeout: 5)
            startTimer()
        } catch let error {
            print(error)
            let pluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: "error")
            self.commandDelegate?.send(pluginResult, callbackId: self.connectCommand.callbackId)
        }
    }
    
    public func disconnectSocket() {
        print("==> disconnectSocket()\n")
        if self.mSocket != nil {
            self.mSocket.disconnect()
            self.mSocket = nil
        }
    }
    
    public func socket(_ sock: GCDAsyncSocket, didAcceptNewSocket newSocket: GCDAsyncSocket) {
        print(">> didAcceptNewSocket!\n")
        mSocket = newSocket
    }
    
    public func socket(_ socket: GCDAsyncSocket, didConnectToHost host: String, port p:UInt16) {
        print(">> didConnectToHost!\n")
        stopTimer()
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
        
        stopTimer()
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
                UserDefaults.standard.set(thingName, forKey: "thingNameKey")
            }

            
            if json["RESULT_REBOOT"].exists() {
                let resultReboot:String? = json["RESULT_REBOOT"].stringValue
                print("resultReboot = \(String(describing: resultReboot!))")
                if resultReboot == "0" && self.configCommand != nil {
                    let pluginResult = CDVPluginResult(status:CDVCommandStatus_OK, messageAs: "OK")
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
        if self.disconnectCommand != nil {
            let pluginResult = CDVPluginResult(status:CDVCommandStatus_OK, messageAs: "didDisconnect")
            self.commandDelegate?.send(pluginResult, callbackId: self.disconnectCommand.callbackId)
            self.disconnectCommand = nil;
        }
        if let error = err {
            print("Will disconnect with error: \(error)")
        } else{
            print("Success disconnect")
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
        let cmdConnected: String = "{\"msgType\":0,\"CONNECTED\":0}"
        let data = cmdConnected.data(using: String.Encoding.utf8)!
        mSocket.write(data, withTimeout:10, tag: 0)
        mSocket.readData(withTimeout: -1, tag: 0)
        
    }
    
//    func tcpSendReqRescan() {
//        print("==> tcpSendReqRescan()\n")
//        let cmdReqRescan: String = "{\"msgType\":3,\"REQ_RESCAN\":0}"
//        let data = cmdReqRescan.data(using: String.Encoding.utf8)!
//        mSocket.write(data, withTimeout:10, tag: 0)
//        mSocket.readData(withTimeout: -1, tag: 0)
//
//    }
    
    func tcpSendDPMSet() {
        print("==> tcpSendDPMSet()\n")
        let cmdSetDpm: String = "{\"msgType\":5, \"REQ_SET_DPM\":0, \"sleepMode\":0, \"rtcTimer\":1740, \"useDPM\":0, \"dpmKeepAlive\":30000, \"userWakeUp\":0, \"timWakeup\":10}"
        let data = cmdSetDpm.data(using: String.Encoding.utf8)!
        mSocket.write(data, withTimeout:10, tag: 0)
        mSocket.readData(withTimeout: -1, tag: 0)
        
    }
    
    func tcpSendSSIDPW(ssid: String, pw: String, isHidden: Int, serverURL: String) {
        print("==> tcpSendSSIDPW()\n")
            //add "isHidden" in v2.3.1
        let cmdSendSSIDPW: String = "{\"msgType\":1, \"SET_AP_SSID_PW\":0, \"ssid\":\"\(ssid)\", \"pw\":\"\(pw)\", \"isHidden\":\(isHidden), \"url\":\"\(serverURL)\"}"
        //
        let data = cmdSendSSIDPW.data(using: String.Encoding.utf8)!
        mSocket.write(data, withTimeout:10, tag: 0)
        mSocket.readData(withTimeout: -1, tag: 0)
    }
    
    // MARK: - Timer
    
    func startTimer() {
        if let timer = mTimer {
            if !timer.isValid {
                mTimer = Timer.scheduledTimer(timeInterval: 1.0, target: self, selector: #selector(update), userInfo: nil, repeats: true)
            }
        } else {
            mTimer = Timer.scheduledTimer(timeInterval: 1.0, target: self, selector: #selector(update), userInfo: nil, repeats: true)
        }
        
    }
    
    @objc func update() {
        timeCount += 1
        print("timeCount = \(timeCount)")
        if (timeCount > 5) {
            if mSocket.isConnected {
                mSocket.disconnect()
            }
            self.connectSocket()
            self.stopTimer()
        }
    }
    
    func stopTimer() {
        print(">> stopTimer()")
        if let timer = mTimer {
            if (timer.isValid) {
                timer.invalidate()
                mTimer = nil
                timeCount = 0
            }
        }
    }
    
}
