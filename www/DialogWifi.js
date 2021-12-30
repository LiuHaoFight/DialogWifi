var exec = require('cordova/exec');

exports.onConnectSocket = function (host, success, error) {
    exec(success, error, 'DialogWifi', 'onConnectSocket', [host]);
};

exports.sendDPMSet = function ( success, error) {
    exec(success, error, 'DialogWifi', 'sendDPMSet', []);
};

exports.close = function ( success, error) {
    exec(success, error, 'DialogWifi', 'close', []);
};

exports.sendSSIDPW = function (ssid, pwd, hidden, url, success, error) {
    exec(success, error, 'DialogWifi', 'sendSSIDPW', [ssid, pwd, hidden, url]);
};