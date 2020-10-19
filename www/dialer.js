cordova.define("cordova-plugin-phone-call.phonedialer", function(require, exports, module) {
    var exec = require('cordova/exec');
    var platformId = require('cordova/platform').id;
    
    module.exports = {
    
        /**
         * Call the native dialer
         *
         * @param {String, Function}      The phone number to call, The callback error function
         */
        dial: function(phnum, successCallback, errorCallback, bypassAppChooser) {
            if (phnum == null) errorCallback("empty");
            if (platformId == 'ios' || platformId == 'android') {
                exec(
                    successCallback, 
                    errorCallback, 
                    "PhoneDialer", 
                    "dial", 
                    [phnum, bypassAppChooser]
                );
            } else {
                document.location.href = "tel:" + phnum;
                if (successCallback) successCallback();
            }
        },
        hangUp: function(successCallback, errorCallback) {
            if (platformId == 'ios' || platformId == 'android') {
                exec(
                    successCallback, 
                    errorCallback, 
                    "PhoneDialer", 
                    "hangUp", 
                    []
                );
            }
        },
        toggle: function(speakerOn, successCallback, errorCallback, bypassAppChooser) {
            if (speakerOn == null) errorCallback("empty");
            if (platformId == 'ios' || platformId == 'android') {
                exec(
                    successCallback, 
                    errorCallback, 
                    "PhoneDialer", 
                    "toggle", 
                    [speakerOn, bypassAppChooser]
                );
            }
        },
        call: function(phnum, successCallback, errorCallback, speakerOn, bypassAppChooser) {
            if (phnum == null) errorCallback("empty");
            if (platformId == 'ios' || platformId == 'android') {
                exec(
                    successCallback, 
                    errorCallback, 
                    "PhoneDialer", 
                    "call", 
                    [phnum, bypassAppChooser, speakerOn]
                );
            } else {
                document.location.href = "tel:" + phnum;
                if (successCallback) successCallback();
            }
        }
    };
    });
    