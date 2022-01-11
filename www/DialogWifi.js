var exec = require('cordova/exec');

exports.onConnectSocket = function (host, success, error) {
    exec(success, error, 'DialogWifi', 'onConnectSocket', [host]);
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

exports.sendSSIDPW = function (ssid, pwd, hidden, security_type, url, success, error) {
    exec(success, error, 'DialogWifi', 'sendSSIDPW', [ssid, pwd, security_type, hidden, url]);
};