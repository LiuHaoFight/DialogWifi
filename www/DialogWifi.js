var exec = require('cordova/exec');

exports.onConnectSocket = function (host, port, ssid, pw, success, error) {
    exec(success, error, 'DialogWifi', 'onConnectSocket', [host, port, ssid, pw]);
};

exports.scan = function (success, error) {
    exec(success, error, 'DialogWifi', 'scan', []);
};

exports.sendDPMSet = function (success, error) {
    exec(success, error, 'DialogWifi', 'sendDPMSet', []);
};

exports.close = function (success, error) {
    exec(success, error, 'DialogWifi', 'close', []);
};

exports.sendSSIDPW = function (ssid, pwd, hidden, security_type, url, bind, success, error) {
    exec(success, error, 'DialogWifi', 'sendSSIDPW', [ssid, pwd, security_type, hidden, url, bind]);
};