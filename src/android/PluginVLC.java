package cordova.plugin.vlc;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import itri.icl.k400.vlcid.app.Camera2Main;
import android.content.Intent;
import android.app.Activity;

/**
 * This class echoes a string called from JavaScript.
 */
public class PluginVLC extends CordovaPlugin {
    public static final int REQUEST_CODE = 0x071cc0de;
    CallbackContext callbackContext = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("scan")) {
            this.scan(callbackContext);
            return true;
        }
        return false;
    }

    private void scan(CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
        final CordovaPlugin that = this;
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                Intent intentScan = new Intent(that.cordova.getActivity().getBaseContext(), Camera2Main.class);
                that.cordova.startActivityForResult(that, intentScan, REQUEST_CODE);
            }
        });
        // if (message != null && message.length() > 0) {
        //     callbackContext.success(message);
        // } else {
        //     callbackContext.error("Expected one non-empty string argument.");
        // }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE && this.callbackContext != null) {
            if (resultCode == Activity.RESULT_OK) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("text", intent.getStringExtra("SCAN_RESULT"));
                    obj.put("cancelled", false);
                } catch (JSONException e) {
                    // Log.d("PluginVLC", "This should never happen");
                }
                this.callbackContext.success(obj);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("text", "");
                    obj.put("cancelled", true);
                } catch (JSONException e) {
                    // Log.d("PluginVLC", "This should never happen");
                }
                this.callbackContext.success(obj);
            } else {
                this.callbackContext.error("Unexpected error");
            }
        }
    }
}
