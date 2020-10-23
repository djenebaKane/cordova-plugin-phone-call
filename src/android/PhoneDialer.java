package org.apache.cordova.phonedialer;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Method;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.media.AudioManager;
import android.util.Log;


public class PhoneDialer extends CordovaPlugin {
    public static final int CALL_REQ_CODE = 0;
    public static final int PERMISSION_DENIED_ERROR = 20;
    public static final String CALL_PHONE = Manifest.permission.CALL_PHONE;
    //public String isSpeakerOn = "False"; // To control the call has been made from the application
    public boolean callFromOffHook = false; // To control the change to idle state is from the app call
    public boolean callFromApp = false; // To control the call has been made from the application
    public TelephonyManager manager;
    public StatePhoneReceiver myPhoneStateListener;

    private CallbackContext callbackContext;        // The callback context from which we were invoked.
    private JSONArray executeArgs;
    private CordovaInterface icordova;
    boolean isSpeakerOn = false;
    protected void getCallPermission(int requestCode) {
        cordova.requestPermission(this, requestCode, CALL_PHONE);
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        icordova = cordova;
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        this.executeArgs = args;

        try {
            if("call".equalsIgnoreCase(action)) {
                if (cordova.hasPermission(CALL_PHONE)) {
                    callPhone(executeArgs);
                } else {
                    getCallPermission(CALL_REQ_CODE);
                }
            } else if ("dial".equalsIgnoreCase(action)) {
                dialPhone(executeArgs);
            } else if ("toggle".equalsIgnoreCase(action)){
                toggleAudio(executeArgs);
            } else if ("hangUp".equalsIgnoreCase(action)){
                hangUpCall(executeArgs);
            }else if ("isSpeakerOn".equalsIgnoreCase(action)) {
                isSpeakerOn();

            }

            return true;

        } catch (Exception e) {
            String msg = "Exception Dialing Phone Number: " + e.getMessage();
            System.err.println(msg);
            this.callbackContext.error(msg);

            return false;
        }

        // try {
        // 	String phoneNumber = args.getString(0);
        // 	Uri uri = Uri.parse("tel:"+phoneNumber);
        // 	Intent callIntent = new Intent(Intent.ACTION_CALL);
        // 	callIntent.setData(uri);
        // 	this.cordova.getActivity().startActivity(callIntent);
        // 	callbackContext.success();
        // 	return true;
        // } catch (Exception e) {
        // 	String msg = "Exception Dialing Phone Number: " + e.getMessage();
        // 	System.err.println(msg);
        // 	callbackContext.error(msg);
        // 	return false;
        // }
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
                return;
            }
        }

        switch (requestCode) {
            case CALL_REQ_CODE:
                callPhone(executeArgs);
                break;
        }
    }

    // Monitor for changes to the state of the phone
    public class StatePhoneReceiver extends PhoneStateListener {
        Context context;
        CordovaInterface cordova;
        public StatePhoneReceiver(Context context, CordovaInterface cordova) {
            this.context = context;
            this.cordova = cordova;
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Log.d("state", "s" + state);
            toggleAudio(isSpeakerOn);
            switch (state) {

                case TelephonyManager.CALL_STATE_OFFHOOK: //Call is established
                    if (callFromApp) {
                        callFromApp=false;
                        callFromOffHook=true;

                        try {
                            Thread.sleep(5000); // Delay 0,5 seconds to handlecallPhone better turning on loudspeaker
                        } catch (InterruptedException e) {
                        }


                    }
                    break;

                case TelephonyManager.CALL_STATE_IDLE: //Call is finished
                    Log.d("CALL_STATE_IDLE", "c" + callFromOffHook);
                    if (callFromOffHook) {

                        callFromOffHook=false;
                        manager.listen(myPhoneStateListener, // Remove listener
                                PhoneStateListener.LISTEN_NONE);
                    }
                    break;
            }
        }
    }


    public void killCall(Context context) {
        try {
            // Get the boring old TelephonyManager
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            // Get the getITelephony() method
            Class classTelephony = Class.forName(telephonyManager.getClass().getName());
            Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");

            // Ignore that the method is supposed to be private
            methodGetITelephony.setAccessible(true);

            // Invoke getITelephony() to get the ITelephony interface
            Object telephonyInterface = methodGetITelephony.invoke(telephonyManager);

            // Get the endCall method from ITelephony
            Class telephonyInterfaceClass =
                    Class.forName(telephonyInterface.getClass().getName());
            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");

            // Invoke endCall()
            methodEndCall.invoke(telephonyInterface);

            this.callbackContext.success();

        } catch (Exception ex) { // Many things can go wrong with reflection calls
            // Log.d(TAG,"PhoneStateReceiver **" + ex.toString());
            this.callbackContext.error("couldNotHangUpCall");
        }
    }



    private void toggleAudio(boolean speakerOn){
        AudioManager audioManager = (AudioManager) this.cordova.getActivity().getSystemService(Context.AUDIO_SERVICE);
        if (speakerOn){
            audioManager.setMode(AudioManager.MODE_IN_CALL);
        } else {
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
        audioManager.setSpeakerphoneOn(isSpeakerOn);

    }

    private void hangUpCall(JSONArray args) throws JSONException {
        //Activate loudspeaker

        killCall(this.cordova.getActivity());


    }

    private void isSpeakerOn(){

        AudioManager audioManager = (AudioManager) this.cordova.getActivity().getSystemService(Context.AUDIO_SERVICE);

        PluginResult result = new PluginResult(PluginResult.Status.OK, audioManager.isSpeakerphoneOn());
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }

    private void toggleAudio(JSONArray args) throws JSONException {
        String onSpeak = args.getString(0);
        //Activate loudspeaker
        AudioManager audioManager = (AudioManager) this.cordova.getActivity().getSystemService(Context.AUDIO_SERVICE);
        if (onSpeak.toLowerCase().equals("true")) {
            isSpeakerOn = true;
        } else {
            isSpeakerOn = false;
        }

        toggleAudio(isSpeakerOn);


    }

    private void callPhone(JSONArray args) throws JSONException {
        //toggleAudio(true);
        String number = args.getString(0);
        number = number.replaceAll("#", "%23");

        if (!number.startsWith("tel:")) {
            number = String.format("tel:%s", number);
        }

        try {
            Intent intent = new Intent(isTelephonyEnabled() ? Intent.ACTION_CALL : Intent.ACTION_VIEW);

            myPhoneStateListener = new StatePhoneReceiver(this.cordova.getContext(), this.cordova);
            manager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE); // start listening to the phone changes
            String IsSpeakerOn = args.getString(2);
            if (IsSpeakerOn.toLowerCase().equals("true")) {
                callFromApp = true;
            } else {
                callFromApp = false;
            }

            intent.setData(Uri.parse(number));

            boolean bypassAppChooser = Boolean.parseBoolean(args.getString(1));
            if (bypassAppChooser) {
                intent.setPackage(getDialerPackage(intent));
            }

            cordova.getActivity().startActivity(intent);

            this.callbackContext.success();
        }
        catch (Exception e) {
            this.callbackContext.error("CouldNotCallPhoneNumber");
        }
    }

    private void dialPhone(JSONArray args) throws JSONException {
        String number = args.getString(0);
        number = number.replaceAll("#", "%23");

        if (!number.startsWith("tel:")) {
            number = String.format("tel:%s", number);
        }

        try {
            Intent intent = new Intent(isTelephonyEnabled() ? Intent.ACTION_DIAL : Intent.ACTION_VIEW);
            intent.setData(Uri.parse(number));

            boolean bypassAppChooser = Boolean.parseBoolean(args.getString(1));
            if (bypassAppChooser) {
                intent.setPackage(getDialerPackage(intent));
            }

            this.cordova.getActivity().startActivity(intent);

            this.callbackContext.success();
        }
        catch (Exception e) {
            this.callbackContext.error("CouldNotCallPhoneNumber");
        }
    }

    private boolean isTelephonyEnabled() {
        manager = (TelephonyManager) this.cordova.getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        return manager != null && manager.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
    }

    private String getDialerPackage(Intent intent) {
        PackageManager packageManager = (PackageManager) this.cordova.getActivity().getPackageManager();
        List activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        for (int i = 0; i < activities.size(); i++) {
            if (activities.get(i).toString().toLowerCase().contains("com.android.server.telecom")) {
                return "com.android.server.telecom";
            }
            if (activities.get(i).toString().toLowerCase().contains("com.android.phone")) {
                return "com.android.phone";
            } else if (activities.get(i).toString().toLowerCase().contains("call")) {
                return activities.get(i).toString().split("[ ]")[1].split("[/]")[0];
            }
        }
        return "";
    }
}